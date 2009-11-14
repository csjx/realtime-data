/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Seacat ASCII data source into RBNB Data Turbine
 *             frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy: cjones $
 * $LastChangedRevision:  $
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

import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to harvest a compressed hexadecimal ASCII data file 
 * from a Brooke Ocean Seahorse profiler equipped with a Seacat SBE19plus CTD. 
 * Communication is over a TCP socket connection with an Iridium satellite modem
 * and a serial2ip converter host. The data samples are then converted into RBNB
 * frames and pushed into the RBNB DataTurbine real time server.  This class  
 * extends org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each sample (scan) 
 * of data is a Hex-encoded string of values, and that each sample is terminated
 * by a newline character (\n).  It is also assumed that the sample rate is 4Hz,
 * or a sample is taken every 15 seconds.
 *
 */
public class SeahorseSource extends RBNBSource {

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
  private String DEFAULT_RBNB_CHANNEL = "HexadecimalASCIISampleData";

  /**
   * The name of the RBNB channel for this data stream
   */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /*
   *  A default source IP address for the given source instrument
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "127.0.0.1";  

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

  /*
   * The socket channel used to establish TCP communication with the instrument
   */
  private SocketChannel socketChannel;
  
  /**
   * The number of bytes in the ensemble as each byte is read from the stream
   */
  private int resultByteCount = 0;
  
  /**
   * The command prefix used to send commands to the microcontroller
   */ 
  private String MODEM_COMMAND_PREFIX = "AT";

  /**
   * The command suffix used to send commands to the microcontroller
   */ 
  private final String MODEM_COMMAND_SUFFIX = "\r";

  /**
   * The command used to get the network registration status from the Iridium modem
   */ 
  private final String REGISTRATION_STATUS_COMMAND = "+CREG?";

  /**
   * The command used to get the signal strength from the Iridium modem
   */ 
  private final String SIGNAL_STRENGTH_COMMAND = "+CSQ";

  /**
   * The command used to answer a RING call from the Iridium modem
   */ 
  private final String ANSWER_COMMAND = "A";

  /**
   * The command used to acknowledge the connection from the instrument
   */ 
  private final String ACKNOWLEDGE_COMMAND = "ACK";

  /**
   *The command used to get the ID from the instrument
   */ 
  private final String ID_COMMAND = "GID";

  /**
   *The platform ID of the instrument (i.e. the SeahHorse identifier, not the CTD)
   */ 
  private  String platformID = "";
  
  /**
   *The command used to get the battery voltage from the instrument
   */ 
  private final String BATTERY_VOLTAGE_COMMAND = "GBV";
  
  /**
   *The command used to get the GPRMC data string from the instrument
   */ 
  private final String GPRMC_COMMAND = "GPS";

  /**
   *The command used to get the name of the file to be downloaded from the instrument
   */ 
  private final String FILENAME_COMMAND = "GFN";

  /**
   *The command used to get the remaining number of blocks (bytes) from the instrument
   */ 
  private final String NUMBER_OF_BLOCKS_COMMAND = "GNB";

  /**
   *The remaining number of blocks (bytes) to download from the instrument
   */ 
  private int numberOfBlocks = 0;
  
  /**
   *The command used to transfer blocks (bytes) from the instrument
   */ 
  private final String TRANSFER_BLOCKS_COMMAND = "TXB";

  /**
   *The command used to disconnect (hang up) with the Iridium modem
   */ 
  private final String HANGUP_COMMAND = "H0";

  /**
   * The command used to close the transfer session with the instrument
   */ 
  private final String CLOSE_TRANSFER_SESSION_COMMAND = "REL";

  /**
   *The command used to escape to command mode with the Iridium modem
   */ 
  private final String ESCAPE_SEQUENCE_COMMAND = "+++";

  /**
   * The okay status string expected from the instrument
   */ 
  private final String OKAY_STATUS = "OK";
  
  /**
   * The signal strength string expected from Iridium modem
   */ 
  private final String SIGNAL_STRENGTH = "+CSQ:";
  
  /**
   * The signal strength threshold string needed from Iridium modem (0 - 5)
   */ 
  private final int SIGNAL_THRESHOLD = 3;
  
  /**
   * The registration status string expected from the instrument
   */ 
  private final String REGISTRATION_STATUS = "+CREG:";
  
  /**
   * The call ring string expected from the instrument
   */ 
  private final String CALL_RING = "RING";
  
  /**
   * The connect rate string expected from the instrument
   */ 
  private final String CONNECT_RATE = "CONNECT 19200";
  
  /**
   * The ready status string expected from the instrument
   */ 
  private final String READY_STATUS = "READY";
  
  /**
   * The file name prefix string expected from the instrument
   */ 
  private final String FILENAME_PREFIX = "FILE=";
    
  /**
   * The file name to be downloaded from the instrument
   */ 
  private String fileNameToDownload = "";
  
  /**
   * The prefix string expected at the beginning of the data file name
   */ 
  private final String DATA_FILE_PREFIX = "SH__";
  
  /**
   * The prefix string expected at the beginning of the cast file name
   */ 
  private final String CAST_FILE_PREFIX = "CAST";
  
  /**
   * The blocksize prefix string expected from the instrument
   */ 
  private final String BLOCKSIZE_PREFIX = "BLOCKSIZE=";
  
  /**
   * The transfer complete string expected from the instrument
   */ 
  private final String TRANSFER_COMPLETE = "DONE";
  
  /**
   * The end of files string expected from the instrument
   */ 
  private final String END_OF_FILES = "NONE";
  
  /**
   * The session closed string expected from the instrument
   */ 
  private final String SESSION_CLOSED = "BYE";
  
  /**
   *The command sent to the instrument
   */ 
  private String command;  
  
  /*
   * A boolean field indicating if a command has been sent to the instrument
   */
  private boolean sentCommand = false;
  
  /*
   * The instance of the CTDParser object used to parse the CTD
   * data file and retrieve each of the data fields
   */
   private CTDParser ctdParser = null;
   
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
  private static Logger logger = Logger.getLogger(SeahorseSource.class);

  protected int state = 0;
  
  private boolean readyToStream = false;
  
  private Thread streamingThread;

}
