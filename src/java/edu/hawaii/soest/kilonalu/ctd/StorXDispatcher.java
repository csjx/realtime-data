/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Seacat ASCII data source into RBNB Data Turbine
 *             frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy:  $
 * $LastChangedRevision: $
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
package edu.hawaii.soest.kilonalu.ctd;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import edu.hawaii.soest.kilonalu.ctd.StorXParser;
import edu.hawaii.soest.kilonalu.ctd.StorXSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.InterruptedException;
import java.lang.StringBuffer;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TimeZone;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.commons.lang.exception.NestableException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A class used to harvest a decimal ASCII data from a Seacat 
 * 16plus CTD) from an emails created by a Satlantic STOR-X logger. 
 * The data are converted into RBNB frames and pushed into the RBNB DataTurbine 
 * real time server.  This class extends org.nees.rbnb.RBNBSource, which in 
 * turn extends org.nees.rbnb.RBNBBase, and therefore follows the API 
 * conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each email
 * contains the text/plain body and an application/octet-stream attachement
 * with the STOR-X binary data format with the embedded ASCII data strings.
 * The strings are parsed out of the binary file using 'SAT' as line
 * beginnings and '\r\n' as endings. Since we don't have a binary format 
 * specification for the attachment, other structures in the file are ignored
 * for the time being.
 *
 */
public class StorXDispatcher extends RBNBSource {

  /*
   *  A default archive mode for the given source connection to the RBNB server.
   * Valid modes include 'append', 'create', 'load' and 'none'.
   */
  private final String DEFAULT_ARCHIVE_MODE = "append";
  
  /*
   * The mode in which the source interacts with the RBNB archive. Valid modes 
   * include 'append', 'create', 'load' and 'none', however, Kilo Nalu 
   * instruments should append to an archive, which will create one if none 
   * exist.
   *
   * @see setArchiveMode()
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
  private static final String DEFAULT_SERVER_NAME = "localhost";
  
  /* The default TCP port of the RBNB server */
  private static final int DEFAULT_SERVER_PORT = 3333;
  
  /* The IP address or DNS name of the RBNB server */
  private String serverName = DEFAULT_SERVER_NAME;
  
  /* The default TCP port of the RBNB server */
  private int serverPort = DEFAULT_SERVER_PORT;
  
  /* The address and port string for the RBNB server */
  private String server = serverName + ":" + serverPort;
  
  /* The number of bytes in the ensemble as each byte is read from the stream */
  private int sampleByteCount = 0;
  
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(StorXDispatcher.class);

  
  /* The XML configuration file location for the list of sensor properties */
  private String xmlConfigurationFile = "lib/email.account.properties.xml";
  
  /* The XML configuration object with the list of sensor properties */
  private XMLConfiguration xmlConfiguration;
  
  /* The state used to track the data processing */
  protected int state = 0;
  
  /* A hash map that contains sensor serial number to RBNB Source mappings */
  private HashMap<String, StorXSource> sourceMap;
  
  /* The instance of the StorX Parser class used to parse StorX output */
  private StorXParser storXParser;

  /** the start time for data  */
  private double startTime = 0.0;

  /* the end time for data export */
  private double endTime = Double.MAX_VALUE;
  
  /* The execute interval used to periodically fetch data (in seconds) */
  private int executeInterval = 12000;
  
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
   * Constructor - create an empty instance of the StorXDispatcher object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  public StorXDispatcher() {
  }

  /**
   * Constructor - create an instance of the StorXDispatcher object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port.  This constructor will use default values for the
   * archive mode, archive frame size, and cache frame size. 
   *
   * @param sourceHostName  the name or IP address of the source instrument
   * @param sourceHostPort  the TCP port of the source host instrument
   * @param serverName      the name or IP address of the RBNB server connection
   * @param serverPort      the TCP port of the RBNB server
   */
  public StorXDispatcher(String sourceHostName, String sourceHostPort, 
                         String serverName, String serverPort) {
    
    setHostName(sourceHostName);
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
  }

  /**
   * Constructor - create an instance of the StorXDispatcher object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port, the archive mode, archive frame size, and cache 
   * frame size.  A frame is created at each call to flush() to an RBNB server,
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
  public StorXDispatcher(String sourceHostName, String sourceHostPort, 
                         String serverName, String serverPort, 
                         String archiveMode, int archiveFrameSize, 
                         int cacheFrameSize, String rbnbClientName) {
    
    setHostName(sourceHostName);
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
    setArchiveMode(archiveMode);
    setArchiveSize(archiveFrameSize);
    setCacheSize(cacheFrameSize);
    setRBNBClientName(rbnbClientName);
  }

}
