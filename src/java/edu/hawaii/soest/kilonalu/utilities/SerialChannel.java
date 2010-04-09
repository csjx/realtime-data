/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To provide a Java NIO channel for serial communication
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
package edu.hawaii.soest.kilonalu.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 */
public class SerialChannel implements ByteChannel {
    
  /* The input stream underlying serial communication reads */
  private InputStream in;
  
  /* The output stream underlying serial communication writes */
  private OutputStream out;
  
  /* The buffered reader underlying serial communication reads */
  private BufferedReader serialReader;
  
  /* The buffered writer underlying serial communication writes */
  private BufferedWriter serialWriter;
  
  /* The output channel used for serial communication writes */
  private WritableByteChannel writeChannel;
  
  /** The default serial port name used for serial communications */
  private static String DEFAULT_SERIAL_PORT = "/dev/ttyUSB0";
  
  /* The  serial port name used for serial communications */
  private String serialPortName = DEFAULT_SERIAL_PORT;
  
  /* The communications port identifier for the given serial port */
  private CommPortIdentifier portIdentifier;
  
  /* The serial port object representing the serial port */
  private SerialPort serialPort;
  
  /* A boolean field indicating the status of the serial connection */
  private boolean isOpen = false;
   
   /* The default log configuration file location */
   private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

   /* The log configuration file location */
   private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;

   /* The Logger instance used to log system messages */
   private static Logger logger = Logger.getLogger(SerialChannel.class);
  
  /**
   * Constructor: creates an empty SerialChannel object
   *
   * @param portName - the name of the communications port
   */
  public SerialChannel(String portName) {
    setSerialPort(portName);
    try {
      // establish the serial connection
      open();
      
    } catch (IOException ioe ) {
      logger.debug("Couldn't open the serial port. " +
                   "Error message is " + ioe.getMessage());
    }
        
  }
  
  private void open() throws IOException {
    try {
      
      this.portIdentifier = 
        CommPortIdentifier.getPortIdentifier(this.serialPortName);
      
      if ( portIdentifier.isCurrentlyOwned() ) {
        logger.debug("Error: Port is currently in use.");
        throw new IOException("Error: Serial port is currently in use.");
      
      } else {
        
        CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
        
        if ( commPort instanceof SerialPort ) {
          this.serialPort = (SerialPort) commPort;
          this.serialPort.setSerialPortParams(
           9600,
           SerialPort.DATABITS_8,
           SerialPort.STOPBITS_1,
           SerialPort.PARITY_NONE);
           
           this.in           = serialPort.getInputStream();
           this.serialReader = new BufferedReader(new InputStreamReader(this.in));
           this.out          = serialPort.getOutputStream();
           this.serialWriter = new BufferedWriter(new OutputStreamWriter(this.out));
           this.writeChannel = Channels.newChannel(this.out); // CSJ - not needed?
           this.isOpen       = true;
           
        } else {
          logger.debug("Error: Only serial ports are handled.");
          throw new IOException("Error opening the the port. " +
                                "The port is not a serial port.  Please " +
                                "provide a valid serial port name.");
                                
        }
      }
    
    } catch (  UnsupportedCommOperationException ucoe ) {
      this.serialPort.close();
      throw new IOException(ucoe.getMessage());
      
    } catch ( PortInUseException piue ) {
      throw new IOException(piue.getMessage());
    
    } catch ( NoSuchPortException nspe) {
      throw new IOException(nspe.getMessage());

    } catch ( IOException ioe) {
      throw ioe;
    }
  }
  
  /**
   * A method used to read bytes from the serial port
   *
   * @param readBuffer - the ByteBuffer used to store the bytes read
   * @return count - the number of bytes read as an integer
   */
  public int read(ByteBuffer readBuffer) throws IOException {

    //byte[] buffer = new byte[8192];
    StringBuffer buffer = new StringBuffer();
    int          count  = 0;
    String       line   = null;
    
    try {
      line = this.serialReader.readLine();

      // only append the line read if it is not null, has a length
      // greater than zero, and isn't just a single null character.
      if ( line != null && 
           line.length() > 0 && 
           !line.equals(new String(new byte[]{0x00})) ) {
        buffer.append(line);
        buffer.append("\r\n"); //add termination bytes back into the line
        
        // filter null characters out of the string before putting the
        // bytes into the ByteBuffer.
        byte[] lineAsBytes = buffer.toString().getBytes();
        for (int i = 0; i < lineAsBytes.length; i++) {
          if ( lineAsBytes[i] != 0x00 ) {
            readBuffer.put(lineAsBytes[i]);
            count++;  
          }
        }
        
      } else {
        count = 0;
        
      }

    } catch ( BufferOverflowException bofe ) {
      logger.info("There a problem reading from the serial port: " +
                  bofe.getMessage());
      this.serialPort.close();
      throw new IOException(bofe.getMessage());
    
    } catch ( ReadOnlyBufferException robe ) {
      logger.info("There a problem reading from the serial port: " +
                  robe.getMessage());
      this.serialPort.close();
      throw new IOException(robe.getMessage());
      
    }
    return count;
    
  }
  
  /**
   * A method used to get the status of the serial port connection
   *
   * @return isOpen - true if the serial port is open
   */
  public boolean isOpen() {
    return this.isOpen;
  }
  
  /**
   * A method used to close the serial port connection
   */ 
  public void close() throws IOException {
      try {
        this.serialPort.close();
        this.isOpen = false;
         
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }      
  }
  
  /**
   * A method used to write bytes to the serial port
   *
   * @param  writeBuffer - The ByteBuffer with the bytes to write
   * @return count       - The number of bytes written to the serial port
   */
  public int write(ByteBuffer writeBuffer) throws IOException    {
    
    int count = 0;

    try {
      logger.debug("writeBuffer: " + writeBuffer.toString());
      logger.debug("writeBuffer rem: " + writeBuffer.remaining());
      byte[] stringArray = new byte[writeBuffer.limit()];
      writeBuffer.flip();

      while( writeBuffer.hasRemaining() ) {
        stringArray[count] = writeBuffer.get();
        count++;
      }

      String writeString = new String(stringArray, "US-ASCII");
      logger.debug("writeString: " + writeString );

      this.serialWriter.write(writeString, 0, writeString.length());
      //count = writeString.length();
      this.serialWriter.flush();
 
    } catch ( BufferUnderflowException bufe ) {
      logger.debug("There was an underflow problem:");
      bufe.printStackTrace();
      throw new IOException(bufe.getMessage());
      
    } catch ( IOException ioe ) {
      logger.debug("There was an IO problem:");
      ioe.printStackTrace();
      throw ioe;
      
    }
    return count;  
  }
    
  /**
   * A method that returns the name of the serial port used for serial
   * communication.  This defaults to "/dev/ttyS0".  On Windows, this
   * should be set to the appropriate comm port (e.g. COM1).  For use with 
   * FTDI serial-to-USB adapters on Linux, use "/dev/ttyUSB0".
   *
   * @return serialPort - the name of the serial port
   */
  public String getSerialPort() {
    return this.serialPortName;
    
  }
  
  /**
   * A method used to set the name of the serial port used for serial
   * communication.
   */
  private void setSerialPort(String serialPortName) {
    this.serialPortName = serialPortName;
      
  }
    
}
