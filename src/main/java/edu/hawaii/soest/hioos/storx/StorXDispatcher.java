/*
 *  Copyright: 2016 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Seacat ASCII data source into RBNB Data Turbine
 *             frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.hawaii.soest.hioos.storx;


import edu.hawaii.soest.hioos.isus.ISUSSource;
import edu.hawaii.soest.kilonalu.ctd.CTDSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.util.MimeMessageParser;
import org.dhmp.util.BasicHierarchicalMap;
import org.nees.rbnb.RBNBSource;

import javax.activation.DataSource;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class used to harvest a decimal ASCII data from a Seacat 16plus CTD) from
 * an emails created by a Satlantic STOR-X log. The data are converted into
 * RBNB frames and pushed into the RBNB DataTurbine real time server. This class
 * extends org.nees.rbnb.RBNBSource, which in turn extends
 * org.nees.rbnb.RBNBBase, and therefore follows the API conventions found in
 * the org.nees.rbnb code.
 * <p>
 * The parsing of the data stream relies on the premise that each email contains
 * the text/plain body and an application/octet-stream attachement with the
 * STOR-X binary data format with the embedded ASCII data strings. The strings
 * are parsed out of the binary file using 'SAT' as line beginnings and '\r\n'
 * as endings. Since we don't have a binary format specification for the
 * attachment, other structures in the file are ignored for the time being.
 */
public class StorXDispatcher extends RBNBSource {

    /*
     * A default archive mode for the given source connection to the RBNB
     * server. Valid modes include 'append', 'create', 'load' and 'none'.
     */
    private final String DEFAULT_ARCHIVE_MODE = "append";

    /*
     * The mode in which the source interacts with the RBNB archive. Valid modes
     * include 'append', 'create', 'load' and 'none', however, Kilo Nalu
     * instruments should append to an archive, which will create one if none
     * exist.
     *
     * @see setArchiveMode()
     *
     * @see getArchiveMode()
     */
    private String archiveMode = DEFAULT_ARCHIVE_MODE;

    /* The default size of the ByteBuffer for data from the instrument */
    private int DEFAULT_BUFFER_SIZE = 524288; // 512K

    /* The size of the ByteBuffer for data from the instrument */
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    /* A default RBNB channel name for the given source instrument */
    private String DEFAULT_RBNB_CHANNEL = "DecimalASCIISampleData";

    /* The name of the RBNB channel for this data stream */
    private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;

    /* A default source address for the given source email server */
    private final String DEFAULT_SOURCE_HOST_NAME = "mail.gmail.com";

    /* A source address for the given source email server */
    private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

    /* The default IP address or DNS name of the RBNB server */
    private static final String DEFAULT_SERVER_NAME = "127.0.0.1";

    /* The default TCP port of the RBNB server */
    private static final int DEFAULT_SERVER_PORT = 3333;

    /* The IP address or DNS name of the RBNB server */
    private String serverName = DEFAULT_SERVER_NAME;

    /* The default TCP port of the RBNB server */
    private int serverPort = DEFAULT_SERVER_PORT;

    /* The IMAP session used to connect to the email server */
    private Session mailSession;

    /* The IMAP store email server from the email connection */
    private Store mailStore;

    /* The address and port string for the RBNB server */
    private String server = serverName + ":" + serverPort;

    /*
     * The number of bytes in the ensemble as each byte is read from the stream
     */
    private int sampleByteCount = 0;

    /* The Logger instance used to log system messages */
    private static Log log = LogFactory.getLog(StorXDispatcher.class);

    /* The XML configuration file location for the list of sensor properties */
    private String xmlConfigurationFile = "lib/email.account.properties.xml";

    /* The XML configuration object with the list of sensor properties */
    private XMLConfiguration xmlConfiguration;

    /* The state used to track the data processing */
    protected int state = 0;

    /* A hash map that contains sensor serial number to RBNB Source mappings */
    private HashMap<String, Object> sourceMap;

    /* The instance of the StorX Parser class used to parse StorX output */
    private StorXParser storXParser;

    /**
     * the start time for data
     */
    private double startTime = 0.0;

    /* the end time for data export */
    private double endTime = Double.MAX_VALUE;

    /* The execute interval used to periodically fetch data (in milliseconds) */
    private long executeInterval = 240000; // 3 minutes

    /* a flag to indicate if we are connected to the RBNB server or not */
    private boolean connected = false;

    /*
     * An internal Thread setting used to specify how long, in milliseconds, the
     * execution of the data streaming Thread should wait before re-executing
     *
     * @see execute()
     */
    private final int RETRY_INTERVAL = 5000;

    /**
     * Constructor - create an empty instance of the StorXDispatcher object,
     * using default values for the RBNB server name and port, source instrument
     * name and port, archive mode, archive frame size, and cache frame size.
     */
    public StorXDispatcher() {
    }

    /**
     * Constructor - create an instance of the StorXDispatcher object, using the
     * argument values for the source instrument name and port, and the RBNB
     * server name and port. This constructor will use default values for the
     * archive mode, archive frame size, and cache frame size.
     *
     * @param sourceHostName the name or IP address of the source instrument
     * @param sourceHostPort the TCP port of the source host instrument
     * @param serverName     the name or IP address of the RBNB server connection
     * @param serverPort     the TCP port of the RBNB server
     */
    public StorXDispatcher(String sourceHostName, String sourceHostPort, String serverName, String serverPort) {

        setHostName(sourceHostName);
        setServerName(serverName);
        setServerPort(Integer.parseInt(serverPort));
    }

    /**
     * Constructor - create an instance of the StorXDispatcher object, using the
     * argument values for the source instrument name and port, and the RBNB
     * server name and port, the archive mode, archive frame size, and cache
     * frame size. A frame is created at each call to flush() to an RBNB server,
     * and so the frame sizes below are relative to the number of bytes of data
     * loaded in the ChannelMap that is flushed to the RBNB server.
     *
     * @param sourceHostName   the name or IP address of the source instrument
     * @param sourceHostPort   the TCP port of the source host instrument
     * @param serverName       the name or IP address of the RBNB server
     * @param serverPort       the TCP port of the RBNB server
     * @param archiveMode      the RBNB archive mode: append, load, create, none
     * @param archiveFrameSize the size, in frames, for the RBNB server to archive
     * @param cacheFrameSize   the size, in frames, for the RBNB server to cache
     * @param rbnbClientName   the unique name of the source RBNB client
     */
    public StorXDispatcher(String sourceHostName, String sourceHostPort, String serverName, String serverPort,
                           String archiveMode, int archiveFrameSize, int cacheFrameSize, String rbnbClientName) {

        setHostName(sourceHostName);
        setServerName(serverName);
        setServerPort(Integer.parseInt(serverPort));
        setArchiveMode(archiveMode);
        setArchiveSize(archiveFrameSize);
        setCacheSize(cacheFrameSize);
        setRBNBClientName(rbnbClientName);
    }

    /**
     * A method that executes the reading of data from the email account to the
     * RBNB server after all configuration of settings, connections to hosts,
     * and thread initiatizing occurs. This method contains the detailed code
     * for reading the data and interpreting the data files.
     *
     * @return failed True if the execution failed
     */
    protected boolean execute() {
        log.debug("StorXDispatcher.execute() called.");
        boolean failed = true; // indicates overall success of execute()
        boolean messageProcessed = false; // indicates per message success

        // declare the account properties that will be pulled from the
        // email.account.properties.xml file
        String accountName = "";
        String server = "";
        String username = "";
        String password = "";
        String protocol = "";
        String dataMailbox = "";
        String processedMailbox = "";
        String prefetch = "";

        // fetch data from each sensor in the account list
        List accountList = this.xmlConfiguration.getList("account.accountName");

        for (Iterator aIterator = accountList.iterator(); aIterator.hasNext(); ) {

            int aIndex = accountList.indexOf(aIterator.next());

            // populate the email connection variables from the xml properties
            // file
            accountName = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").accountName");
            server = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").server");
            username = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").username");
            password = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").password");
            protocol = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").protocol");
            dataMailbox = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").dataMailbox");
            processedMailbox = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").processedMailbox");
            prefetch = (String) this.xmlConfiguration.getProperty("account(" + aIndex + ").prefetch");

            log.debug("\n\nACCOUNT DETAILS: \n" +
                "accountName     : " + accountName + "\n" +
                "server          : " + server + "\n" +
                "username        : " + username + "\n" +
                "password        : " + password + "\n" +
                "protocol        : " + protocol + "\n" +
                "dataMailbox     : " + dataMailbox + "\n" +
                "processedMailbox: " + processedMailbox + "\n" +
                "prefetch        : " + prefetch + "\n");

            // get a connection to the mail server
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", protocol);
            props.setProperty("mail.imaps.partialfetch", prefetch);

            try {

                // create the imaps mail session
                this.mailSession = Session.getDefaultInstance(props, null);
                this.mailStore = mailSession.getStore(protocol);

            } catch (NoSuchProviderException nspe) {

                try {
                    // pause for 10 seconds
                    log.debug(
                        "There was a problem connecting to the IMAP server. " + "Waiting 10 seconds to retry.");
                    Thread.sleep(10000L);
                    this.mailStore = mailSession.getStore(protocol);

                } catch (NoSuchProviderException nspe2) {

                    log.debug("There was an error connecting to the mail server. The " + "message was: "
                        + nspe2.getMessage());
                    nspe2.printStackTrace();
                    failed = true;
                    return !failed;

                } catch (InterruptedException ie) {

                    log.debug("The thread was interrupted: " + ie.getMessage());
                    failed = true;
                    return !failed;

                }

            }

            try {

                this.mailStore.connect(server, username, password);

                // get folder references for the inbox and processed data box
                Folder inbox = mailStore.getFolder(dataMailbox);
                inbox.open(Folder.READ_WRITE);

                Folder processed = this.mailStore.getFolder(processedMailbox);
                processed.open(Folder.READ_WRITE);

                Message[] msgs;
                while (!inbox.isOpen()) {
                    inbox.open(Folder.READ_WRITE);

                }
                msgs = inbox.getMessages();

                List<Message> messages = new ArrayList<Message>();
                Collections.addAll(messages, msgs);

                // sort the messages found in the inbox by date sent
                Collections.sort(messages, new Comparator<Message>() {

                    public int compare(Message message1, Message message2) {
                        int value = 0;
                        try {
                            value = message1.getSentDate().compareTo(message2.getSentDate());
                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }
                        return value;

                    }


                });

                log.debug("Number of messages: " + messages.size());
                for (Message message : messages) {

                    // Copy the message to ensure we have the full attachment
                    MimeMessage mimeMessage = (MimeMessage) message;
                    MimeMessage copiedMessage = new MimeMessage(mimeMessage);

                    // determine the sensor serial number for this message
                    String messageSubject = copiedMessage.getSubject();
                    Date sentDate = copiedMessage.getSentDate();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");

                    // The subfolder of the processed mail folder (e.g. 2016-12);
                    String destinationFolder = formatter.format(sentDate);
                    log.debug("Message date: " + sentDate +
                        "\tNumber: " + copiedMessage.getMessageNumber());
                    String[] subjectParts = messageSubject.split("\\s");
                    String logSerialNumber = "SerialNumber";
                    if (subjectParts.length > 1) {
                        logSerialNumber = subjectParts[2];

                    }

                    // Do we have a data attachment? If not, there's no data to
                    // process
                    if (copiedMessage.isMimeType("multipart/mixed")) {

                        log.debug("Message size: " + copiedMessage.getSize());

                        MimeMessageParser parser = new MimeMessageParser(copiedMessage);
                        try {
                            parser.parse();

                        } catch (Exception e) {
                            log.error("Failed to parse the MIME message: " + e.getMessage());
                            continue;
                        }
                        ByteBuffer messageAttachment = ByteBuffer.allocate(256); // init only

                        log.debug("Has attachments: " + parser.hasAttachments());
                        for (DataSource dataSource : parser.getAttachmentList()) {
                            if (StringUtils.isNotBlank(dataSource.getName())) {
                                log.debug("Attachment: " +
                                    dataSource.getName() + ", " +
                                    dataSource.getContentType());

                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                IOUtils.copy(dataSource.getInputStream(), outputStream);
                                messageAttachment = ByteBuffer.wrap(outputStream.toByteArray());

                            }
                        }

                        // We now have the attachment and serial number. Parse the attachment 
                        // for the data components, look up the storXSource based on the serial 
                        // number, and push the data to the DataTurbine

                        // parse the binary attachment
                        StorXParser storXParser = new StorXParser(messageAttachment);

                        // iterate through the parsed framesMap and handle each
                        // frame
                        // based on its instrument type
                        BasicHierarchicalMap framesMap = (BasicHierarchicalMap) storXParser.getFramesMap();

                        Collection frameCollection = framesMap.getAll("/frames/frame");
                        Iterator framesIterator = frameCollection.iterator();

                        while (framesIterator.hasNext()) {

                            BasicHierarchicalMap frameMap = (BasicHierarchicalMap) framesIterator.next();

                            // log.debug(frameMap.toXMLString(1000));

                            String frameType = (String) frameMap.get("type");
                            String sensorSerialNumber = (String) frameMap.get("serialNumber");

                            // handle each instrument type
                            if (frameType.equals("HDR")) {
                                log.debug("This is a header frame. Skipping it.");

                            } else if (frameType.equals("STX")) {

                                try {

                                    // handle StorXSource
                                    StorXSource source = (StorXSource) sourceMap.get(sensorSerialNumber);
                                    // process the data using the StorXSource
                                    // driver
                                    messageProcessed = source.process(this.xmlConfiguration, frameMap);

                                } catch (ClassCastException cce) {

                                }

                            } else if (frameType.equals("SBE")) {

                                try {

                                    // handle CTDSource
                                    CTDSource source = (CTDSource) sourceMap.get(sensorSerialNumber);

                                    // process the data using the CTDSource
                                    // driver
                                    messageProcessed = source.process(this.xmlConfiguration, frameMap);

                                } catch (ClassCastException cce) {

                                }

                            } else if (frameType.equals("NLB")) {

                                try {

                                    // handle ISUSSource
                                    ISUSSource source = (ISUSSource) sourceMap.get(sensorSerialNumber);
                                    // process the data using the ISUSSource
                                    // driver
                                    messageProcessed = source.process(this.xmlConfiguration, frameMap);

                                } catch (ClassCastException cce) {

                                }

                            } else if (frameType.equals("NDB")) {

                                try {

                                    // handle ISUSSource
                                    ISUSSource source = (ISUSSource) sourceMap.get(sensorSerialNumber);
                                    // process the data using the ISUSSource
                                    // driver
                                    messageProcessed = source.process(this.xmlConfiguration, frameMap);

                                } catch (ClassCastException cce) {

                                }

                            } else {

                                log.debug("The frame type " + frameType + " is not recognized. Skipping it.");
                            }

                        } // end while()

                        if (this.sourceMap.get(logSerialNumber) != null) {

                            // Note: Use message (not copiedMessage) when setting flags 

                            if (!messageProcessed) {
                                log.info("Failed to process message: " + "Message Number: "
                                    + message.getMessageNumber() + "  " + "Logger Serial:" + logSerialNumber);
                                // leave it in the inbox, flagged as seen (read)
                                message.setFlag(Flags.Flag.SEEN, true);
                                log.debug("Saw message " + message.getMessageNumber());

                            } else {

                                // message processed successfully. Create a by-month sub folder if it doesn't exist
                                // Copy the message and flag it deleted
                                Folder destination = processed.getFolder(destinationFolder);
                                boolean created = destination.create(Folder.HOLDS_MESSAGES);
                                inbox.copyMessages(new Message[]{message}, destination);
                                message.setFlag(Flags.Flag.DELETED, true);
                                log.debug("Deleted message " + message.getMessageNumber());
                            } // end if()

                        } else {
                            log.debug("There is no configuration information for " + "the log serial number "
                                + logSerialNumber + ". Please add the configuration to the "
                                + "email.account.properties.xml configuration file.");

                        } // end if()

                    } else {
                        log.debug("This is not a data email since there is no "
                            + "attachment. Skipping it. Subject: " + messageSubject);

                    } // end if()

                } // end for()

                // expunge messages and close the mail server store once we're
                // done
                inbox.expunge();
                this.mailStore.close();

            } catch (MessagingException me) {
                try {
                    this.mailStore.close();

                } catch (MessagingException me2) {
                    failed = true;
                    return !failed;

                }
                log.info("There was an error reading the mail message. The " + "message was: " + me.getMessage());
                me.printStackTrace();
                failed = true;
                return !failed;

            } catch (IOException me) {
                try {
                    this.mailStore.close();

                } catch (MessagingException me3) {
                    failed = true;
                    return !failed;

                }
                log.info(
                    "There was an I/O error reading the message part. The " + "message was: " + me.getMessage());
                me.printStackTrace();
                failed = true;
                return !failed;

            } catch (IllegalStateException ese) {
                try {
                    this.mailStore.close();

                } catch (MessagingException me4) {
                    failed = true;
                    return !failed;

                }
                log.info("There was an error reading messages from the folder. The " + "message was: "
                    + ese.getMessage());
                failed = true;
                return !failed;

            } finally {

                try {
                    this.mailStore.close();

                } catch (MessagingException me2) {
                    log.debug("Couldn't close the mail store: " + me2.getMessage());

                }

            }

        }

        return !failed;
    }

    /**
     * A method used to connect each of the StorXSource drivers to the
     * DataTurbine. There is a one driver for each sensor stated in the xml
     * configuration file, and the primary key for each sensor is the sensor
     * serial number.
     */
    protected boolean connect() {
        log.debug("StorXDispatcher.execute() called.");

        if (isConnected()) {
            return true;
        }

        try {

            // Create a list of sensors from the properties file, and iterate
            // through
            // the list, creating an RBNB Source object for each sensor listed.
            // Store
            // these objects in a HashMap for later referral.

            this.sourceMap = new HashMap<String, Object>();

            // the sensor properties to be pulled from each account's sensor
            // list.
            String logName = "";
            String logSerialNumber = "";

            String sourceName = "";
            String sourceType = "";
            String serialNumber = "";
            String description = "";
            String cacheSize = "";
            String archiveSize = "";
            String archiveChannel = "";

            // iterate through each account
            List accountList = xmlConfiguration.getList("account.accountName");

            for (Iterator aIterator = accountList.iterator(); aIterator.hasNext(); ) {

                int aIndex = accountList.indexOf(aIterator.next());

                // evaluate each log listed in the
                // email.account.properties.xml file
                List logList = xmlConfiguration.getList("account.log.logName");

                for (Iterator gIterator = logList.iterator(); gIterator.hasNext(); ) {

                    int gIndex = logList.indexOf(gIterator.next());

                    logName = (String) this.xmlConfiguration
                        .getProperty("account(" + aIndex + ").log(" + gIndex + ").logName");
                    logSerialNumber = (String) this.xmlConfiguration
                        .getProperty("account(" + aIndex + ").log(" + gIndex + ").logSerialNumber");

                    // evaluate each log listed in the
                    // email.account.properties.xml file
                    List sensorList = xmlConfiguration.getList("account.log.sensor.name");

                    for (Iterator sIterator = sensorList.iterator(); sIterator.hasNext(); ) {

                        // get each property value of the sensor
                        int sIndex = sensorList.indexOf(sIterator.next());

                        sourceName = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").name");
                        sourceType = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").type");
                        serialNumber = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").serialNumber");
                        description = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").description");
                        cacheSize = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").cacheSize");
                        archiveSize = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").archiveSize");
                        archiveChannel = (String) this.xmlConfiguration.getProperty(
                            "account(" + aIndex + ").log(" + gIndex + ").sensor(" + sIndex + ").archiveChannel");

                        // test for all of the critical source information
                        if (sourceName != null && sourceType != null && cacheSize != null && archiveSize != null
                            && archiveChannel != null) {

                            // test which type of source to create the RBNB
                            // Source
                            if (sourceType.equals("StorXSource")) {

                                // given the properties, create a StorXSource
                                // object
                                StorXSource storXSource = new StorXSource(this.serverName,
                                    (new Integer(this.serverPort)).toString(), this.archiveMode,
                                    (new Integer(archiveSize).intValue()), (new Integer(cacheSize).intValue()),
                                    sourceName);
                                storXSource.startConnection();
                                sourceMap.put(serialNumber, storXSource);

                            } else if (sourceType.equals("CTDSource")) {

                                // given the properties, create a CTDSource
                                // object
                                CTDSource ctdSource = new CTDSource(this.serverName,
                                    (new Integer(this.serverPort)).toString(), this.archiveMode,
                                    (new Integer(archiveSize).intValue()), (new Integer(cacheSize).intValue()),
                                    sourceName);
                                ctdSource.startConnection();
                                sourceMap.put(serialNumber, ctdSource);

                            } else if (sourceType.equals("ISUSSource")) {

                                // given the properties, create an ISUSSource
                                // object
                                ISUSSource isusSource = new ISUSSource(this.serverName,
                                    (new Integer(this.serverPort)).toString(), this.archiveMode,
                                    (new Integer(archiveSize).intValue()), (new Integer(cacheSize).intValue()),
                                    sourceName);
                                isusSource.startConnection();
                                sourceMap.put(serialNumber, isusSource);

                            } // end if()

                        } // end if()

                    } // end for()

                } // end for()

            } // end for()

            log.debug(this.sourceMap.toString());

            return true;

        } catch (Exception e) {
            log.debug("Failed to connect. Message: " + e.getMessage());
            return false;

        }

    }

    /**
     * A method that sets the size, in bytes, of the ByteBuffer used in
     * streaming data from a source instrument via a TCP connection
     *
     * @return bufferSize - the size of the buffer
     */
    public int getBufferSize() {
        return this.bufferSize;
    }

    /**
     * A method that returns the domain name or IP address of the email source
     * (i.e. the IMAP mail server)
     *
     * @return sourceHostName - the name of the source host
     */
    public String getHostName() {
        return this.sourceHostName;
    }

    /**
     * A method that returns the name of the RBNB channel that contains the
     * streaming data from this instrument
     *
     * @return rbnbChannelName - the name of the RBNB channel
     */
    public String getRBNBChannelName() {
        return this.rbnbChannelName;
    }

    /**
     * A method that returns the versioning info for this file. In this case, it
     * returns a String that includes the Subversion LastChangedDate,
     * LastChangedBy, LastChangedRevision, and HeadURL fields.
     *
     * @return cvsVersion - the CVS version information
     */
    public String getCVSVersionString() {
        return ("$LastChangedDate$" + "$LastChangedBy$" + "$LastChangedRevision$" + "$HeadURL$");
    }

    /**
     * The main method for running the code @ param args[] the command line list
     * of string arguments, none are needed
     *
     * @param args - the arguments array
     */
    public static void main(String[] args) {

        try {
            // create a new instance of the StorXDispatcher object, and parse
            // the command
            // line arguments as settings for this instance
            final StorXDispatcher storXDispatcher = new StorXDispatcher();

            // Handle ctrl-c's and other abrupt death signals to the process
            Runtime.getRuntime().addShutdownHook(new Thread() {
                                                     // stop the streaming process
                                                     public void run() {
                                                         Collection sourceCollection = storXDispatcher.sourceMap.values();
                                                         for (Iterator iterator = sourceCollection.iterator(); iterator.hasNext(); ) {

                                                             Object sourceObject = iterator.next();

                                                             try {

                                                                 // disconnect StorX sources
                                                                 StorXSource source = (StorXSource) sourceObject;
                                                                 log.info("Disconnecting source: " + source.getRBNBClientName());
                                                                 source.stopConnection();

                                                             } catch (java.lang.ClassCastException cce) {

                                                                 // disconnect ISUS sources
                                                                 try {
                                                                     ISUSSource source = (ISUSSource) sourceObject;
                                                                     log.info("Disconnecting source: " + source.getRBNBClientName());
                                                                     source.stopConnection();

                                                                 } catch (java.lang.ClassCastException cce2) {

                                                                     // disconnect CTD sources
                                                                     CTDSource source = (CTDSource) sourceObject;
                                                                     log.info("Disconnecting source: " + source.getRBNBClientName());
                                                                     source.stopConnection();

                                                                 } // end try/catch

                                                             } // end try/catch

                                                         } // end for()

                                                     } // end run()

                                                 } // end new Thread()

            ); // end addShutDownHook()

            // parse the commandline arguments to configure the connection, then
            // start the streaming connection between the source and the RBNB
            // server.
            if (storXDispatcher.parseArgs(args) && storXDispatcher.parseConfiguration()) {

                // establish the individual source connections with the RBNB
                if (storXDispatcher.connect()) {

                    // fetch data on a schedule
                    TimerTask fetchData = new TimerTask() {
                        public void run() {
                            log.debug("TimerTask.run() called.");
                            storXDispatcher.execute();
                        }
                    };

                    Timer executeTimer = new Timer("Execute Timer");
                    // run the fetchData timer task at the default interval
                    executeTimer.scheduleAtFixedRate(fetchData, new Date(), storXDispatcher.executeInterval);

                } else {
                    log.info("Could not establish a connection to the DataTurbine. Exiting.");
                    System.exit(0);
                }

            }

        } catch (Exception e) {
            log.info("Error in main(): " + e.getMessage());
            e.printStackTrace();

        }
    }

    /**
     * A method that sets the size, in bytes, of the ByteBuffer used in
     * streaming data from a source instrument via a TCP connection
     *
     * @param bufferSize the size, in bytes, of the ByteBuffer
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * A method that sets the RBNB channel name of the source instrument's data
     * stream
     *
     * @param channelName the name of the RBNB channel being streamed
     */
    public void setChannelName(String channelName) {
        this.rbnbChannelName = channelName;
    }

    /**
     * A method that sets the domain name or IP address of the source instrument
     * (i.e. the serial-to-IP converter to which it is attached)
     *
     * @param hostName the domain name or IP address of the source instrument
     */
    public void setHostName(String hostName) {
        this.sourceHostName = hostName;
    }

    /**
     * A method that sets the domain name or IP address of the RBNB server
     *
     * @param serverName the domain name or IP address of the RBNB server
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * A method that sets the TCP port of the RBNB server
     *
     * @param serverPort the TCP port of the RBNB server
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;

    }

    /*
     * A method used to get the sensor configuration properties for each of the
     * listed CTD sensors
     *
     * @return failed  true if the parsing doesn't succeed
     */
    private boolean parseConfiguration() {

        boolean failed = true;

        try {
            // create an XML Configuration object from the sensor XML file
            File xmlConfigFile = new File(this.xmlConfigurationFile);
            this.xmlConfiguration = new XMLConfiguration(xmlConfigFile);
            failed = false;

        } catch (NullPointerException npe) {
            log.info("There was an error reading the XML configuration file. " + "The error message was: "
                + npe.getMessage());

        } catch (ConfigurationException ce) {
            log.info("There was an error creating the XML configuration. " + "The error message was: "
                + ce.getMessage());

        }
        return !failed;

    }

    /**
     * A method that sets the command line arguments for this class. This method
     * calls the <code>RBNBSource.setBaseArgs()</code> method.
     *
     * @param command The CommandLine object being passed in from the command
     * @return argumentsSet  True if the arguments are set
     */
    protected boolean setArgs(CommandLine command) {

        // first set the base arguments that are included on the command line
        if (!setBaseArgs(command)) {
            return false;
        }

        // add command line arguments here

        // handle the -H option
        if (command.hasOption("H")) {
            String hostName = command.getOptionValue("H");
            if (hostName != null) {
                setHostName(hostName);
            }
        }

        // handle the -C option
        if (command.hasOption("C")) {
            String channelName = command.getOptionValue("C");
            if (channelName != null) {
                setChannelName(channelName);
            }
        }

        // handle the -s option
        if (command.hasOption('s')) {
            String serverName = command.getOptionValue('s');
            if (serverName != null)
                setServerName(serverName);
        }

        // handle the -p option
        if (command.hasOption('p')) {
            String serverPort = command.getOptionValue('p');
            if (serverPort != null) {
                try {
                    setServerPort(Integer.parseInt(serverPort));

                } catch (NumberFormatException nf) {
                    System.out.println(
                        "Please enter a numeric value for -p (server port). " + serverPort + " is not valid.");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * A method that sets the command line options for this class. This method
     * calls the <code>RBNBSource.setBaseOptions()</code> method in order to set
     * properties such as the sourceHostName, sourceHostPort, serverName, and
     * serverPort.
     *
     * @return options  The command line options being set
     */
    protected Options setOptions() {
        Options options = setBaseOptions(new Options());

        // Note:
        // Command line options already provided by RBNBBase include:
        // -h "Print help"
        // -s "RBNB Server Hostname"
        // -p "RBNB Server Port Number"
        // -S "RBNB Source Name"
        // -v "Print Version information"

        // Command line options already provided by RBNBSource include:
        // -z "Cache size"
        // -Z "Archive size"

        // add command line options here
        options.addOption("H", true, "Source host name or IP e.g. " + getHostName());
        options.addOption("C", true, "RBNB source channel name e.g. " + getRBNBChannelName());
        options.addOption("s", true, "RBNB Server Hostname");
        options.addOption("p", true, "RBNB Server Port Number");

        return options;
    }

}
