/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert an ADCP PD0 binary data source into RBNB Data Turbine
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
package edu.hawaii.soest.kilonalu.adcp;

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
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to harvest an PD0 data stream (from an RDI 1200kHz 
 * Workhorse ADCP) over a TCP socket connection with a Digi PortServer 
 * serial2ip converter host. The data stream is then converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server.  This class extends 
 * org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each ensemble of  
 * data produced by the ADCP instrument is prepended by a number of header 
 * bytes that provide critical metadata needed to parse the subsequent bytes 
 * of data in the ensemble. The header of each ensemble begins with a 0x7F 
 * byte (the HEADER_ID), followed immediately by another 0x7F 
 * (the DATA_SOURCE_ID). Once the header is found, the offset to the next
 * header id of the following ensemble can be determined.  Each ensemble is
 * packaged into an RBNB channel map, and sent to the RBNB Data Turbine.
 */
public class ADCPSource extends RBNBSource {

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
  private int DEFAULT_BUFFER_SIZE = 1048576; // 1 MB

  /**
   * The size of the ByteBuffer used to beffer the TCP stream from the 
   * instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /*
   *  A default RBNB channel name for the given source instrument
   */  
  private String DEFAULT_RBNB_CHANNEL = "BinaryPD0EnsembleData";

  /**
   * The name of the RBNB channel for this data stream
   */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /*
   *  A default source IP address for the given source instrument
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "192.168.1.200";  

  /**
   * The domain name or IP address of the host machine that this Source 
   * represents and from which the data will stream. 
   */
  private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

  /*
   *  A default source TCP port for the given source instrument
   */  
  private final int DEFAULT_SOURCE_HOST_PORT  = 2104;

  /**
   * The TCP port to connect to on the Source host machine 
   */
  private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;

  /**
   * The number of bytes in an ensemble to the Number of Data Types field,
   * as defined by the RDI PD0 binary ensemble format
   */
  private int NUMBER_OF_DATA_TYPES_OFFSET = 6;

  /**
   * The number of bytes in the ensemble as each byte is read from the stream
   */
  private int ensembleByteCount = 0;
    
  /**
   * The flag indicating whether not the header is verified.  The value will be
   * set to true if the identifier for the first data type (0x0000, i.e. the
   * Fixed Leader ID) is found at the exact number of bytes from the Header ID
   * (0x7F7F) that is stated in the header's offset for Data Type One.  This is
   * used to avoid re-starting the Ensemble parsing when a random 0x7F7F is found
   * in the data stream (which is fairly frequent).
   */
  private boolean headerIsVerified = false;
  
  // the number of bytes in the ensemble to validate
  /**
   * The checksum calculated for each ensemble as the data are read from the stream
   */
  int ensembleChecksum = 0;

  /**
   * The number of data types in the ensemble as stated in the ensemble header
   */
  int numberOfDataTypes = 0;                                    
  
  /**
   * The byte offset location for the first data type in the the ensemble, where
   * byte 1 == the first 0x7F of the ensemble Header ID
   */
  int dataTypeOneOffset = 0;
  /**
   * The Logger instance used to log system messages 
   */
  private static Logger logger = Logger.getLogger(ADCPSource.class);

  //private int DEFAULT_CACHE_FRAME_SIZE =   100000; // ~100MB for 1K Ensembles
  //private int DEFAULT_ARCHIVE_FRAME_SIZE = 1000000; // ~1GB for 1K Ensembles
  protected int state = 0;
  
  private int ensembleBytes = 0;
  
  private boolean readyToStream = false;
  
  private Thread streamingThread;
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
  // private static ByteBuffer HEADER_ID = ByteBuffer.wrap(new byte[]{0x7f7f});
    
  /**
   * Constructor - create an empty instance of the ADCPSource object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  public ADCPSource() {
  }

  /**
   * Constructor - create an instance of the ADCPSource object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port.  This constructor will use default values for the
   * archive mode, archive frame size, and cache frame size. 
   *
   * @param sourceHostName  the name or IP address of the source instrument
   * @param sourceHostPort  the TCP port of the source host instrument
   * @param serverName      the name or IP address of the RBNB server connection
   * @param serverPort      the TCP port of the RBNB server
   */
  public ADCPSource(String sourceHostName, String sourceHostPort, 
                    String serverName, String serverPort) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
  }

  /**
   * Constructor - create an instance of the ADCPSource object, using the
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
  public ADCPSource(String sourceHostName, String sourceHostPort, 
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
      
      // Create a buffer that will store the ensemble bytes as they are read
      ByteBuffer ensembleBuffer = ByteBuffer.allocate(getBufferSize());
      
      // create a byte buffer to store bytes from the TCP stream
      ByteBuffer buffer = ByteBuffer.allocateDirect(getBufferSize());
      
      // add a channel of data that will be pushed to the server.  
      // Each ensemble will be sent to the Data Turbine as an rbnb frame.
      ChannelMap rbnbChannelMap = new ChannelMap();
      int channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
            
      // while there are bytes to read from the socket ...
      while ( socket.read(buffer) != -1 || buffer.position() > 0) {
        // prepare the buffer for reading
        buffer.flip();          
    
        // while there are unread bytes in the ByteBuffer
        while ( buffer.hasRemaining() ) {
          byteOne = buffer.get();
    
          // Use a State Machine to process the byte stream.
          // Start building an rbnb frame for the entire ensemble, first by 
          // inserting a timestamp into the channelMap.  This time is merely
          // the time of insert into the data turbine, not the time of
          // observations of the measurements.  That time should be parsed out
          // of the ensemble in the Sink client code
    
          System.out.print("\rProcessed byte # " +
                           ensembleByteCount +
                           " " +
                           new String(Hex.encodeHex((new byte[]{byteOne}))) +
                           " - log msg is: "
                           );
        
          switch( state ) {
    
            case 0: // find ensemble header id
              if ( byteOne == 0x7F && byteTwo == 0x7F ) {
                ensembleByteCount++; // add Header ID
                ensembleChecksum += (byteTwo & 0xFF);
                ensembleByteCount++; // add Data Source ID
                ensembleChecksum += (byteOne & 0xFF);

                state = 1;
                break;
    
              }else {
                break;
                
              }
    
            case 1: // find the Ensemble Length (LSB)
              ensembleByteCount++; // add Ensemble Byte Count (LSB)
              ensembleChecksum += (byteOne & 0xFF);
  
              state = 2;
              break;
    
            case 2: // find the Ensemble Length (MSB)
              ensembleByteCount++; // add Ensemble Byte Count (MSB)
              ensembleChecksum += (byteOne & 0xFF);
  
              int upperEnsembleByte = (byteOne & 0xFF) << 8;
              int lowerEnsembleByte = (byteTwo  & 0xFF);
              ensembleBytes = upperEnsembleByte + lowerEnsembleByte;
              logger.debug("Number of Bytes in the Ensemble: " +
                           ensembleBytes);

              if ( ensembleBuffer.remaining() > 0 ) {
                
                ensembleBuffer.put(byteFour);
                ensembleBuffer.put(byteThree);
                ensembleBuffer.put(byteTwo);
                ensembleBuffer.put(byteOne);
              } else {
                
                ensembleBuffer.compact();
                ensembleBuffer.put(byteFour);
                ensembleBuffer.put(byteThree);
                ensembleBuffer.put(byteTwo);
                ensembleBuffer.put(byteOne);
              }
    
              state = 3;
              break;
            
            // verify that the header is real, not a random 0x7F7F
            case 3: // find the number of data types in the ensemble
              
              // set the numberOfDataTypes byte
              if ( ensembleByteCount == NUMBER_OF_DATA_TYPES_OFFSET - 1 ) {
                ensembleByteCount++;
                ensembleChecksum += (byteOne & 0xFF);
                numberOfDataTypes = (byteOne & 0xFF);
                // calculate the number of bytes to the Fixed Leader ID
                dataTypeOneOffset = 6 + (2 * numberOfDataTypes);
                
                if ( ensembleBuffer.remaining() > 0 ) {
                  ensembleBuffer.put(byteOne);
                  
                } else {
                  ensembleBuffer.compact();
                  ensembleBuffer.put(byteOne);
                  
                }
                state = 4;
                break;
              
              } else {
                ensembleByteCount++;
                ensembleChecksum += (byteOne & 0xFF);
                
                if ( ensembleBuffer.remaining() > 0 ) {
                  ensembleBuffer.put(byteOne);
                  
                } else {
                  ensembleBuffer.compact();
                  ensembleBuffer.put(byteOne);
                  
                }
               
                break;
              }
              
            case 4:  // find the offset to data type #1 and verify the header ID
              if ( (ensembleByteCount == dataTypeOneOffset + 1) &&
                   byteOne == 0x00 && byteTwo == 0x00) {
                ensembleByteCount++;
                ensembleChecksum += (byteOne & 0xFF);
                // we are confident that the previous sequence of 0x7F7F is truly
                // an headerID and not a random occurrence in the stream because
                // we have identified the Fixed Leader ID (0x0000) the correct
                // number of bytes beyond the 0x7F7F
                headerIsVerified = true;
                
                if ( ensembleBuffer.remaining() > 0 ) {
                  ensembleBuffer.put(byteOne);
                  
                } else {
                  ensembleBuffer.compact();
                  ensembleBuffer.put(byteOne);
                  
                }
               
                state = 5;
                break;
              
              } else {
                
                if ( ensembleByteCount > dataTypeOneOffset + 1 ) {
                  // We've hit a random 0x7F7F byte sequence that is not a true
                  // ensemble header id.  Reset the processing and look for the 
                  // next 0x7F7F sequence in the stream
                  ensembleByteCount = 0;
                  ensembleChecksum  = 0;
                  dataTypeOneOffset = 0;
                  numberOfDataTypes = 0;                                    
                  headerIsVerified  = false;
                  ensembleBuffer.clear();
                  rbnbChannelMap.Clear();
                  channelIndex      = rbnbChannelMap.Add(getRBNBChannelName());

                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 0;
                  
                  if ( ensembleBuffer.remaining() > 0 ) {
                    ensembleBuffer.put(byteOne);
                    
                  } else {
                    ensembleBuffer.compact();
                    ensembleBuffer.put(byteOne);
                    
                  }
                  
                  break;
                  
                } else {
                  // We are still parsing bytes between the purported header ID
                  // and fixed leader ID. Keep parsing until we hit the fixed
                  // leader ID, or until we are greater than the dataTypeOneOffset
                  // stated value.
                  ensembleByteCount++;
                  ensembleChecksum += (byteOne & 0xFF);
                  
                  if ( ensembleBuffer.remaining() > 0 ) {
                    ensembleBuffer.put(byteOne);
                    
                  } else {
                    ensembleBuffer.compact();
                    ensembleBuffer.put(byteOne);
                    
                  }
                  
                  break;
                  }
                
              }

            case 5: // read the rest of the bytes to the next Header ID 
    
              // if we've made it to the next ensemble's header id, prepare to
              // flush the data.  Also check that the calculated byte count 
              // is greater than the recorded byte count in case of finding an
              // arbitrary 0x7f 0x7f sequence in the data stream
              if ( byteOne == 0x7F && byteTwo == 0x7F &&
                   ( ensembleByteCount == ensembleBytes + 3 ) && 
                   headerIsVerified ) {
    
                // remove the last bytes from the count (byteOne and byteTwo)
                ensembleByteCount -= 1;
    
                // remove the last three bytes from the checksum: 
                // the two checksum bytes are not included, and the two 0x7f 
                //bytes belong to the next ensemble, and one of them was 
                // previously added. Reset the buffer position due to this too.
                //ensembleChecksum -= (byteOne   & 0xFF);
                ensembleChecksum -= (byteTwo   & 0xFF);
                ensembleChecksum -= (byteThree & 0xFF);
                ensembleChecksum -= (byteFour  & 0xFF);
                // We are consistently 1 byte over in the checksum.  Trim it.  We need to
                // troubleshoot why this is. CSJ 12/18/2007
                ensembleChecksum = ensembleChecksum - 1;
    
                // jockey byteThree into LSB, byteFour into MSB
                int upperChecksumByte = (byteThree & 0xFF) << 8;
                int lowerChecksumByte = (byteFour & 0xFF);
                int trueChecksum = upperChecksumByte + lowerChecksumByte;
    
                if ( ensembleBuffer.remaining() > 0 ) {
                  ensembleBuffer.put((byte)lowerChecksumByte);
                  ensembleBuffer.put((byte)(upperChecksumByte >> 8));
                } else {
                  ensembleBuffer.compact();
                  ensembleBuffer.put((byte)lowerChecksumByte);
                  ensembleBuffer.put((byte)(upperChecksumByte >> 8));
                }
    
                // check if the calculated checksum (modulo 65535) is equal
                // to the true checksum; if so, flush to the data turbine
                // Also, if the checksums are off by 1 byte, also flush the
                // data.  We need to troubleshoot this bug CSJ 06/11/2008
                if ( ( (ensembleChecksum % 65535) == trueChecksum ) ||
                     ( (ensembleChecksum + 1 ) % 65535 == trueChecksum ) ||
                     ( (ensembleChecksum - 1 ) % 65535 == trueChecksum ) ) {
    
                  // extract just the length of the ensemble bytes out of the
                  // ensemble buffer, and place it in the channel map as a 
                  // byte array.  Then, send it to the data turbine.
                  byte[] ensembleArray = new byte[ensembleByteCount];
                  ensembleBuffer.flip();
                  ensembleBuffer.get(ensembleArray);
    
                  // send the ensemble to the data turbine
                  rbnbChannelMap.PutTimeAuto("server");
                  rbnbChannelMap.PutDataAsByteArray(channelIndex, ensembleArray);
                  getSource().Flush(rbnbChannelMap);
                  logger.debug(
                    "flushed: "   + ensembleByteCount          + " "    +
                    "ens cksum: " + ensembleChecksum           + "\t\t" +
                    "ens pos: "   + ensembleBuffer.position()  + "\t"   +
                    "ens rem: "   + ensembleBuffer.remaining() + "\t"   +
                    "buf pos: "   + buffer.position()          + "\t"   +
                    "buf rem: "   + buffer.remaining()         + "\t"   +
                    "state: "     + state
                  );
                  logger.info("Sent ADCP ensemble to the data turbine.");
    
                  // only clear all four bytes if we are not one or two bytes 
                  // from the end of the byte buffer (i.e. the header id 
                  // is split or is all in the previous buffer)
                  if ( byteOne == 0x7f && byteTwo == 0x7f &&
                       ensembleByteCount > ensembleBytes &&
                       buffer.position() == 0) {
                    byteThree = 0x00;
                    byteFour = 0x00;
                    logger.debug("Cleared ONLY b3, b4.");
                  } else if ( byteOne == 0x7f && 
                              ensembleByteCount > ensembleBytes &&
                              buffer.position() == 1 ) {
                      buffer.position(buffer.position() -1);
                      byteTwo = 0x00;
                      byteThree = 0x00;
                      byteFour = 0x00;
                      logger.debug("Cleared ONLY b2, b3, b4.");
    
                  } else {
                    byteOne = 0x00;
                    byteTwo = 0x00;
                    byteThree = 0x00;
                    byteFour = 0x00;                      
                    logger.debug("Cleared ALL b1, b2, b3, b4.");
                  }
    
                  //rewind the position to before the next ensemble's header id
                  if ( buffer.position() >= 2 ) {
                    buffer.position(buffer.position() - 2);
                    logger.debug("Moved position back two, now: " +
                                 buffer.position());
                  }
    
    
                  ensembleBuffer.clear();
                  ensembleByteCount = 0;
                  ensembleBytes = 0;
                  ensembleChecksum = 0;
                  state = 0;
                  break;
    
                } else {
    
                  // The checksums don't match, move on 
                  logger.info("not equal: " +
                               "calc chksum: " + 
                               (ensembleChecksum % 65535) +
                               "\tens chksum: " + trueChecksum +
                               "\tbuf pos: " + buffer.position() + 
                               "\tbuf rem: " + buffer.remaining() +
                               "\tens pos: " + ensembleBuffer.position() + 
                               "\tens rem: " + ensembleBuffer.remaining() +
                               "\tstate: " + state);
    
                  rbnbChannelMap.Clear();
                  channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
                  ensembleBuffer.clear();
                  ensembleByteCount = 0;
                  ensembleChecksum = 0;
                  ensembleBuffer.clear();
                  state = 0;
                  break;
                }                  
    
              } else {
    
                // still in the middle of the ensemble, keep adding bytes
                ensembleByteCount++; // add each byte found
                ensembleChecksum += (byteOne & 0xFF);
    
                if ( ensembleBuffer.remaining() > 0 ) {
                  ensembleBuffer.put(byteOne);
                  
                } else {
                  ensembleBuffer.compact();
                  ensembleBuffer.put(byteOne);
                  
                }
                
                break;
              }                
          }
          // shift the bytes in the FIFO window
          byteFour = byteThree;
          byteThree = byteTwo;
          byteTwo = byteOne;

          logger.debug("remaining:\t" + buffer.remaining() +
                       "\tstate:\t" + state +
                       "\tens byte count:\t" + ensembleByteCount +
                       "\tens bytes:\t" + ensembleBytes +
                       "\tver:\t" + headerIsVerified +
                       "\tbyte value:\t" + 
                       new String(Hex.encodeHex((new byte[]{byteOne}))));
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
  }
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
   * A method that returns the domain name or IP address of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   */
  public String getHostName(){
    return this.sourceHostName;
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
    
    // Set up a simple logger that logs to the console
    BasicConfigurator.configure();
    
    logger.info("ADCPSource.main() called.");
    
    try {
      // create a new instance of the ADCPSource object, and parse the command 
      // line arguments as settings for this instance
      final ADCPSource adcpSource = new ADCPSource();
      
      // parse the commandline arguments to configure the connection, then 
      // start the streaming connection between the source and the RBNB server.
      if ( adcpSource.parseArgs(args) ) {
        adcpSource.start();
      }
      
      // Handle ctrl-c's and other abrupt death signals to the process
      Runtime.getRuntime().addShutdownHook(new Thread() {
        // stop the streaming process
        public void run() {
          adcpSource.stop();
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
    options.addOption("H", true, "Source host name or IP *" + getHostName());
    options.addOption("P", true, "Source host port number *" + getHostPort());    
    options.addOption("C", true, "RBNB source channel name *" + getRBNBChannelName());
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
