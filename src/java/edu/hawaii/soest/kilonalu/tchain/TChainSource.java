/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Seacat ASCII data source into RBNB Data Turbine
 *             frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL$
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
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
package edu.hawaii.soest.kilonalu.tchain;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import java.lang.StringBuffer;

import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to harvest a decimal ASCII data stream from a TChain 
 * temperature logger) over a TCP socket connection to a
 * serial2ip converter host. The data stream is then converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server.  This class extends 
 * org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each sample of data
 * is a space delimited string of values, and that each sample is terminated
 * by a newline character (\n).
 *
 * Each line of data is parsed by an instance of the SeabirdParser code, 
 * contributed to the Open Source Data Turbine (OSDT) initiative, found at
 * http://dataturbine.org.
 */
public class TChainSource extends RBNBSource {

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

  /*
   * The default size of the ByteBuffer used to beffer the TCP stream from the
   * source instrument.
   */  
  private int DEFAULT_BUFFER_SIZE = 8096; // 8K

  /**
   * The size of the ByteBuffer used to beffer the TCP stream from the 
   * instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /*
   *  A default RBNB channel name for the given source instrument
   */  
  private String DEFAULT_RBNB_CHANNEL = "DecimalASCIISampleData";

  /**
   * The name of the RBNB channel for this data stream
   */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /*
   *  A default source IP address for the given source instrument
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "localhost";  

  /**
   * The domain name or IP address of the host machine that this Source 
   * represents and from which the data will stream. 
   */
  private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

  /*
   *  A default source TCP port for the given source instrument
   */  
  private final int DEFAULT_SOURCE_HOST_PORT  = 2101;

  /**
   * The TCP port to connect to on the Source host machine 
   */
  private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;

  /**
   * The number of bytes in the ensemble as each byte is read from the stream
   */
  private int sampleByteCount = 0;
  
  /**
   * The default log configuration file location
   */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /**
   * The log configuration file location
   */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;

  /**
   * The Logger instance used to log system messages 
   */
  private static Logger logger = Logger.getLogger(TChainSource.class);

  protected int state = 0;
  
  private boolean readyToStream = false;
  
  private Thread streamingThread;

  /**
   * The default field delimiter string used to separate data fields in the sample
   */
  private String DEFAULT_FIELD_DELIMITER = "  ";

  /**
   * The field delimiter string used to separate data fields in the sample
   */
  private String fieldDelimiter = DEFAULT_FIELD_DELIMITER;

  /**
   * The default first byte of the sample delimiter (i.e for Windows
   * line endings (\r\n) the byte is 0x0D (the \r)
   */
  private final byte DEFAULT_FIRST_DELIMITER_BYTE = 0x0A;

  /**
   * The default first byte of the sample delimiter (i.e for Windows
   * line endings (\r\n) the byte is 0x0D (the \r)
   */
  private final byte DEFAULT_SECOND_DELIMITER_BYTE = 0x0D;

  /**
   * The first byte of the sample delimiter (i.e for Windows
   * line endings (\r\n) the byte is 0x0D (the \r)
   */
  private byte firstDelimiterByte = DEFAULT_FIRST_DELIMITER_BYTE;

  /**
   * The default first byte of the sample delimiter (i.e for Windows
   * line endings (\r\n) the byte is 0x0D (the \r)
   */
  private byte secondDelimiterByte = DEFAULT_SECOND_DELIMITER_BYTE;

  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
    
  /**
   * Constructor - create an empty instance of the TChainSource object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  
   /** 
    * The date format for the timestamp applied to the TChain sample 04 Aug 2008 09:15:01
    */
   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /**
   * The timezone used for the sample date
   */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
    
  public TChainSource() {
  }

  /**
   * Constructor - create an instance of the TChainSource object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port.  This constructor will use default values for the
   * archive mode, archive frame size, and cache frame size. 
   *
   * @param sourceHostName  the name or IP address of the source instrument
   * @param sourceHostPort  the TCP port of the source host instrument
   * @param serverName      the name or IP address of the RBNB server connection
   * @param serverPort      the TCP port of the RBNB server
   */
  public TChainSource(String sourceHostName, String sourceHostPort, 
                      String serverName, String serverPort) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
  }

  /**
   * Constructor - create an instance of the TChainSource object, using the
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
  public TChainSource(String sourceHostName, String sourceHostPort, 
                      String serverName, String serverPort, 
                      String archiveMode, int archiveFrameSize, 
                      int cacheFrameSize, String rbnbClientName) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
    setArchiveMode(archiveMode);
    setArchiveSize(archiveFrameSize);
    setCacheSize(cacheFrameSize);
    setRBNBClientName(rbnbClientName);
  }

  /**
   * A method that executes the streaming of data from the source to the RBNB
   * server after all configuration of settings, connections to hosts, and
   * thread initiatizing occurs.  This method contains the detailed code for 
   * streaming the data and interpreting the stream.
   */
  protected boolean execute() {
    logger.debug("TChainSource.execute() called.");
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
      boolean failed = false;
    
      SocketChannel socket = getSocketConnection();
    
    // while data are being sent, read them into the buffer
    try {
      // create four byte placeholders used to evaluate up to a four-byte 
      // window.  The FIFO layout looks like:
      //           -------------------------
      //   in ---> | One | Two |Three|Four |  ---> out
      //           -------------------------
      byte byteOne   = 0x00,   // set initial placeholder values
           byteTwo   = 0x00,
           byteThree = 0x00,
           byteFour  = 0x00;
      
      // Create a buffer that will store the sample bytes as they are read
      ByteBuffer sampleBuffer = ByteBuffer.allocate(getBufferSize());
      
      // create a byte buffer to store bytes from the TCP stream
      ByteBuffer buffer = ByteBuffer.allocateDirect(getBufferSize());
      
      // add a channel of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.
      ChannelMap rbnbChannelMap = new ChannelMap();
            
      // while there are bytes to read from the socket ...
      while ( socket.read(buffer) != -1 || buffer.position() > 0) {

        // prepare the buffer for reading
        buffer.flip();          
    
        // while there are unread bytes in the ByteBuffer
        while ( buffer.hasRemaining() ) {
          byteOne = buffer.get();
         logger.debug("char: " + (char) byteOne                                   + "\t" + 
                      "b1: " + new String(Hex.encodeHex((new byte[]{byteOne})))   + "\t" + 
                      "b2: " + new String(Hex.encodeHex((new byte[]{byteTwo})))   + "\t" + 
                      "b3: " + new String(Hex.encodeHex((new byte[]{byteThree}))) + "\t" + 
                      "b4: " + new String(Hex.encodeHex((new byte[]{byteFour})))  + "\t" +
                      "sample pos: "   + sampleBuffer.position()                  + "\t" +
                      "sample rem: "   + sampleBuffer.remaining()                 + "\t" +
                      "sample cnt: "   + sampleByteCount                          + "\t" +
                      "buffer pos: "   + buffer.position()                        + "\t" +
                      "buffer rem: "   + buffer.remaining()                       + "\t" +
                      "state: "        + state
         );
          
          // Use a State Machine to process the byte stream.
          // Start building an rbnb frame for the entire sample, first by 
          // inserting a timestamp into the channelMap.  This time is merely
          // the time of insert into the data turbine, not the time of
          // observations of the measurements.  That time should be parsed out
          // of the sample in the Sink client code
    
          switch( state ) {
    
            case 0:
              
              // sample line ending is '\r\n' (carraige return, newline)
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == this.firstDelimiterByte && 
                   byteTwo == this.secondDelimiterByte ) {
                // we've found the end of a sample, move on
                state = 1;
                break;
    
              } else {
                break;                
              }
            
            case 1: // read the rest of the bytes to the next EOL characters
              
              // sample line is terminated by record delimiter bytes (usually \r\n or \n)
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == this.firstDelimiterByte && 
                   byteTwo == this.secondDelimiterByte ) {
                
                // rewind the sample to overwrite the line ending so we can add
                // in the timestamp (then add the line ending)
                sampleBuffer.position(sampleBuffer.position() - 1);
                --sampleByteCount;

                // add the delimiter to the end of the sample.
                byte[] delimiterAsBytes = getFieldDelimiter().getBytes("US-ASCII");
                
                for ( byte delim : delimiterAsBytes ) {
                  sampleBuffer.put(delim);
                  sampleByteCount++;
                }
                
                // then add a timestamp to the end of the sample
                DATE_FORMAT.setTimeZone(TZ);
                byte[] sampleDateAsBytes = DATE_FORMAT.format(new Date()).getBytes("US-ASCII");
                for ( byte b : sampleDateAsBytes ) {
                  sampleBuffer.put(b);
                  sampleByteCount++;
                }
                
                // add the last two bytes found (usually \r\n) to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                  sampleByteCount++;
                  sampleBuffer.put(byteTwo);
                  sampleByteCount++;
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  sampleByteCount++;
                  sampleBuffer.put(byteTwo);
                  sampleByteCount++;
                  
                }                
                
                // extract just the length of the sample bytes out of the
                // sample buffer, and place it in the channel map as a 
                // byte array.  Then, send it to the data turbine.
                byte[] sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                sampleBuffer.get(sampleArray);
                
                // send the sample to the data turbine
                rbnbChannelMap.PutTimeAuto("server");
                String sampleString = new String(sampleArray, "US-ASCII");
                int channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
                rbnbChannelMap.PutMime(channelIndex, "text/plain");
                rbnbChannelMap.PutDataAsString(channelIndex, sampleString);
                getSource().Flush(rbnbChannelMap);
                logger.info("Sample: " + 
                            sampleString.substring(0, sampleString.length() - 2) + 
                            " sent data to the DataTurbine. ");
                                            
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  sampleBuffer.clear();
                  sampleByteCount = 0;
                  rbnbChannelMap.Clear();                      
                  logger.debug("Cleared b1,b2,b3,b4. Cleared sampleBuffer. Cleared rbnbChannelMap.");
                  //state = 0;
                  
              } else { // not 0x0D20

                // still in the middle of the sample, keep adding bytes
                sampleByteCount++; // add each byte found

                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                } else {
                  sampleBuffer.compact();
                  logger.debug("Compacting sampleBuffer ...");
                  sampleBuffer.put(byteOne);
                  
                }
                
                break;
              } // end if for 0x0D20 EOL
          
          } // end switch statement
          
          // shift the bytes in the FIFO window
          byteFour = byteThree;
          byteThree = byteTwo;
          byteTwo = byteOne;

        } //end while (more unread bytes)
    
        // prepare the buffer to read in more bytes from the stream
        buffer.compact();
    
    
      } // end while (more socket bytes to read)
      socket.close();
        
    } catch ( IOException e ) {
      // handle exceptions
      // In the event of an i/o exception, log the exception, and allow execute()
      // to return false, which will prompt a retry.
      failed = true;
      e.printStackTrace();
      return !failed;
    } catch ( SAPIException sapie ) {
      // In the event of an RBNB communication  exception, log the exception, 
      // and allow execute() to return false, which will prompt a retry.
      failed = true;
      sapie.printStackTrace();
      return !failed;
    }
    
    return !failed;
  } // end if (  !isConnected() ) 
  
   /**
   * A method used to the TCP socket of the remote source host for communication
   * @param host       the name or IP address of the host to connect to for the
   *                   socket connection (reading)
   * @param portNumber the number of the TCP port to connect to (i.e. 2604)
   */
  protected SocketChannel getSocketConnection() {
    
    
    String host = getHostName();
    int portNumber = new Integer(getHostPort()).intValue();
    SocketChannel dataSocket = null;
    
    try {  
      
      // create the socket channel connection to the data source via the 
      // converter serial2IP converter      
      dataSocket = SocketChannel.open();
      Socket tcpSocket = dataSocket.socket();
      tcpSocket.setTcpNoDelay(true);
      dataSocket.connect( new InetSocketAddress(host, portNumber));
      
      // if the connection to the source fails, also disconnect from the RBNB
      // server and return null
      if ( !dataSocket.isConnected()) {
        dataSocket.close();
        disconnect();
        dataSocket = null;
      }      
    }  catch ( UnknownHostException ukhe ) {
      System.err.println("Unable to look up host: " + host + "\n");
      disconnect();
      dataSocket = null;
    } catch (IOException nioe ) {
      System.err.println("Couldn't get I/O connection to: " + host);
      disconnect();
      dataSocket = null;
    } catch (Exception e) {
      disconnect();
      dataSocket = null;            
    }
    return dataSocket;
    
  }

  
  /**
   * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   */
   public int getBufferSize() {
     return this.bufferSize;
   }
   
  /**
   * A method that gets the field delimiter string, which is used to append
   * a timestamp at the end of the sample using the correct delimiter.
   *
   * @return delimiter  the delimiter as a string of characters
   */
   public String getFieldDelimiter() {
     return this.fieldDelimiter;
   }
  
  /**
   * A method that returns the domain name or IP address of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   */
  public String getHostName(){
    return this.sourceHostName;
  }

  /**
   * A method that gets the log configuration file location
   *
   * @return logConfigurationFile  the log configuration file location
   */
  public String getLogConfigurationFile() {
    return this.logConfigurationFile;
  }
  
  /**
   * A method that returns the name of the RBNB channel that contains the 
   * streaming data from this instrument
   */
  public String getRBNBChannelName(){
    return this.rbnbChannelName;
  }

  /**
   * A method that returns the TCP port of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   */
  public int getHostPort(){
    return this.sourceHostPort;
  }

  /**
   * A method that returns the versioning info for this file.  In this case, 
   * it returns a String that includes the Subversion LastChangedDate, 
   * LastChangedBy, LastChangedRevision, and HeadURL fields.
   */

  public String getCVSVersionString(){
    return (
    "$LastChangedDate$" +
    "$LastChangedBy$" +
    "$LastChangedRevision$" +
    "$HeadURL$"
    );
  }

  /**
   * A method that returns true if the RBNB connection is established
   * and if the data streaming Thread has been started
   */
  public boolean isRunning() {
    // return the connection status and the thread status
    return ( isConnected() && readyToStream );
  }
  
  /**
   * The main method for running the code
   * @ param args[] the command line list of string arguments, none are needed
   */

  public static void main (String args[]) {
    
    try {
      // create a new instance of the TChainSource object, and parse the command 
      // line arguments as settings for this instance
      final TChainSource tChainSource = new TChainSource();
      
      // parse the commandline arguments to configure the connection, then 
      // start the streaming connection between the source and the RBNB server.
      if ( tChainSource.parseArgs(args) ) {

        // Set up a simple logger that logs to the console
        PropertyConfigurator.configure(tChainSource.getLogConfigurationFile());
        logger.info("TChainSource.main() called.");
    
        tChainSource.start();
      }
      
      // Handle ctrl-c's and other abrupt death signals to the process
      Runtime.getRuntime().addShutdownHook(new Thread() {
        // stop the streaming process
        public void run() {
          tChainSource.stop();
        }
      }
      );
      
    } catch ( Exception e ) {
      logger.info("Error in main(): " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /*
   * A method that runs the data streaming work performed by the execute()
   * by handling execution problems and continuously trying to re-execute after 
   * a specified retry interval for the thread.
   */
  private void runWork() {
    
    // handle execution problems by retrying if execute() fails
    boolean retry = true;
    
    while ( retry ) {
      
      // connect to the RBNB server
      if ( connect() ) {
        // run the data streaming code
        retry = !execute();
      }
      
      disconnect();
      
      if ( retry ) {
        try {
          Thread.sleep(RETRY_INTERVAL);
        } catch ( Exception e ){
          logger.info("There was an execution problem. Retrying. Message is: " +
          e.getMessage());
        }
      }
    }
    // stop the streaming when we are done
    stop();
  }

  /**
   * A method that sets the command line arguments for this class.  This method 
   * calls the <code>RBNBSource.setBaseArgs()</code> method.
   * 
   * @param command  The CommandLine object being passed in from the command
   */
  protected boolean setArgs(CommandLine command) {
    
    // first set the base arguments that are included on the command line
    if ( !setBaseArgs(command)) {
      return false;
    }
    
    // add command line arguments here
    
    // handle the -H option
    if ( command.hasOption("H") ) {
      String hostName = command.getOptionValue("H");
      if ( hostName != null ) {
        setHostName(hostName);
      }
    }

    // handle the -P option, test if it's an integer
    if ( command.hasOption("P") ) {
      String hostPort = command.getOptionValue("P");
      if ( hostPort != null ) {
        try {
          setHostPort(Integer.parseInt(hostPort));
          
        } catch ( NumberFormatException nfe ){
          logger.info("Error: Enter a numeric value for the host port. " +
                             hostPort + " is not a valid number.");
          return false;
        }
      }
    }

    // handle the -C option
    if ( command.hasOption("C") ) {
      String channelName = command.getOptionValue("C");
      if ( channelName != null ) {
        setChannelName(channelName);
      }
    }

    // handle the -d option
    if ( command.hasOption("d") ) {
      String delimiter = command.getOptionValue("d");
      if ( delimiter != null ) {
        setFieldDelimiter(delimiter);
      }
    }

    // handle the -l option
    if ( command.hasOption("l") ) {
      String lineEnding = command.getOptionValue("l");
      if ( lineEnding != null ) {
        try {
         
          String[] endingBytes = lineEnding.split(",");

          if ( endingBytes.length == 2 ) {
           byte[] firstByte   = Hex.decodeHex(endingBytes[0].trim().toCharArray());      
           byte[] secondByte  = Hex.decodeHex(endingBytes[1].trim().toCharArray());      
           setFirstDelimiterByte(firstByte[0]);
           setSecondDelimiterByte(secondByte[0]);

          } else if ( endingBytes.length == 1 ) {
           byte[] firstByte2  = Hex.decodeHex(endingBytes[0].trim().toCharArray());      
           setFirstDelimiterByte(firstByte2[0]);

          } else {
            throw new Exception("The number of delimiter bytes must be one or two.");

          }

        } catch (org.apache.commons.codec.DecoderException de ) {
          logger.info("Please set the record delimiter (-l) with a one or two byte" +
                       " comma-sparated sequence in hexadecimal format, such as 0D,0A" +
                       " (which represents a Windows line ending \\r\\n).");

        } catch (java.lang.Exception e ) {
          logger.info("Please set the record delimiter (-l) with a one or two byte" +
                       " comma-sparated sequence in hexadecimal format, such as 0D,0A" +
                       " (which represents a Windows line ending \\r\\n).");
        }
      }
    }

    return true;
  }

  /**
   * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   *
   * @param bufferSize  the size, in bytes, of the ByteBuffer
   */
  public void setBuffersize(int bufferSize) {
    this.bufferSize = bufferSize;
  }
  
  /**
   * A method that sets the field delimiter string, which is used to append
   * a timestamp at the end of the sample using the correct delimiter.
   *
   * @param delimiter  the delimiter as a string of characters
   */
   public void setFieldDelimiter(String delimiter) {
     this.fieldDelimiter = delimiter;
   }

  /**
   * A method that sets the first delimiter byte of the record delimiter.  For
   * instance, a Windows line ending is 0x0A,0x0D (\r\n), and this method
   * sets the 0x0A as the first delimiter byte for evauluating the data stream.
   *
   * @param delimiterByte  the first delimiter byte character
   */
   public void setFirstDelimiterByte(byte delimiterByte) {
     this.firstDelimiterByte = delimiterByte;
   }

  /**
   * A method that sets the second delimiter byte of the record delimiter.  For
   * instance, a Windows line ending is 0x0A,0x0D (\r\n), and this method
   * sets the 0x0A as the second delimiter byte for evauluating the data stream.
   *
   * @param delimiterByte  the second delimiter byte character
   */
   public void setSecondDelimiterByte(byte delimiterByte) {
     this.secondDelimiterByte = delimiterByte;
   }

  /**
   * A method that sets the log configuration file name
   *
   * @param logConfigurationFile  the log configuration file name
   */
  public void setLogConfigurationFile(String logConfigurationFile) {
    this.logConfigurationFile = logConfigurationFile;
  }
  
  /**
   * A method that sets the RBNB channel name of the source instrument's data
   * stream
   *
   * @param channelName  the name of the RBNB channel being streamed
   */
  public void setChannelName(String channelName) {
    this.rbnbChannelName = channelName;
  }

  /**
   * A method that sets the domain name or IP address of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   *
   * @param hostName  the domain name or IP address of the source instrument
   */
  public void setHostName(String hostName) {
    this.sourceHostName = hostName;
  }

  /**
   * A method that sets the TCP port of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   *
   * @param hostPort  the TCP port of the source instrument
   */
  public void setHostPort(int hostPort) {
    this.sourceHostPort = hostPort;
  }

  /**
   * A method that sets the command line options for this class.  This method 
   * calls the <code>RBNBSource.setBaseOptions()</code> method in order to set
   * properties such as the sourceHostName, sourceHostPort, serverName, and
   * serverPort.
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
    options.addOption("H", true, "Source host name or IP (defaults to " + getHostName() + ").");
    options.addOption("P", true, "Source host port number (defaults to " + getHostPort() + ").");    
    options.addOption("C", true, "RBNB source channel name (defaults to " + getRBNBChannelName() + ").");
    options.addOption("d", true, "The field delimiter string (characters between columns)" +
                                 "(defaults to two space characters \"  \".");
    options.addOption("l", true, "The line ending characters (record delimiter) as a comma" +
                                 " separated sequence of one or two bytes in hexadecimal format" +
                                 " (i.e. \"0D,0A\" represents a Windows line ending \\r\\n)");
    //options.addOption("M", true, "RBNB archive mode *" + getArchiveMode());    
                      
    return options;
  }

  /**
   * A method that starts the streaming of data from the source instrument to
   * the RBNB server via an established TCP connection.  
   */
  public boolean start() {
    
    // return false if the streaming is running
    if ( isRunning() ) {
      return false;
    }
    
    // reset the connection to the RBNB server
    if ( isConnected() ) {
      disconnect();
    }
    connect();
    
    // return false if the connection fails
    if ( !isConnected() ) {
      return false;
    }
    
    // begin the streaming thread to the source
    startThread();
    
    return true;  
  }

  /**
   * A method that creates and starts a new Thread with a run() method that 
   * begins processing the data streaming from the source instrument.
   */
  private void startThread() {
    
    // build the runnable class and implement the run() method
    Runnable runner = new Runnable() {
      public void run() {
        runWork();
      }
    };
    
    // build the Thread and start it, indicating that it has been started
    readyToStream = true;
    streamingThread = new Thread(runner, "StreamingThread");
    streamingThread.start();     
  }

  /**
   * A method that stops the streaming of data between the source instrument and
   * the RBNB server.  
   */ 
  public boolean stop() {
    
    // return false if the thread is not running
    if ( !isRunning() ) {
      return false;
    }
    
    // stop the thread and disconnect from the RBNB server
    stopThread();
    disconnect();
    return true;
  }

  /**
   * A method that interrupts the thread created in startThread()
   */
  private void stopThread() {
    // set the streaming status to false and stop the Thread
    readyToStream = false;
    streamingThread.interrupt();
  }

}
