/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

    
public class SerialWriter implements Runnable {
  
  /* The output stream underlying serial communication writes */
  private OutputStream out;
  
  /* The buffered writer underlying serial communication writes */
  private BufferedWriter serialWriter;
    
  /* The write buffer underlying serial communication writes */
  private ByteBuffer writeBuffer = ByteBuffer.allocate(64);
  
  /* The number of bytes written to the serial port */
  private int bytesWritten = 0;
  
  /* A boolean field indicating if a write operation is complete */
  private boolean isComplete = false;
  
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j2.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;

  /* The Logger instance used to log system messages */
  private static Log log = LogFactory.getLog(SerialWriter.class);
 
  /**
   * The constructor - creates a new SerialWriter object given an OutputStream
   *                   to write to
   *
   * @param out - the OutputStream used for writing
   */
  public SerialWriter(OutputStream out) {
    this.serialWriter = new BufferedWriter(new OutputStreamWriter(out));
    this.writeBuffer.position(0);
    this.writeBuffer.limit(0);
    log.debug("writeBuffer at init: " + this.writeBuffer.toString());
    
  }
  
  /**
   * A method that implements the Runnable interface.  While running, looks for
   * bytes to be written in the writeBuffer ByteBuffer, and writes them to the 
   * OutputStream.
   */
  public void run() {
    log.debug("SerialWriter.run() called.");
    try {

      while ( this.writeBuffer.position() != -1 ) {
        while ( this.writeBuffer.hasRemaining()) {
          this.serialWriter.write(this.writeBuffer.get());
          this.bytesWritten++;
        }
        
        // flush the stream and indicate that all bytes were written
        if ( this.bytesWritten > 0 ) {
          this.serialWriter.flush();
          this.isComplete = true; 
        }
      }
      
    } catch (IOException e) {
      log.debug("There was an error writing to the serial port: " + e.getMessage());
      
    }
  }
  
  /**
   * A method used to write bytes to the serial port
   *
   * @param  writeBuffer - The ByteBuffer with the bytes to write
   * @return count       - The number of bytes written to the serial port
   */
  public int write(ByteBuffer writeBuffer) throws IOException {

    log.debug("SerialWriter.write() called.");
    try {
 
      // duplicate the incoming writeBuffer when it is not locked
      synchronized(writeBuffer) {

        this.writeBuffer = writeBuffer.duplicate();
        log.debug("SerialWriter.write(): duplicated the ByteBuffer.");
      }
      
      this.writeBuffer.flip();
      
      while ( ! this.isComplete ) {
        log.debug("SerialWriter.write(): waiting for write to complete.");
        // do nothing.  wait for the thread to write the data
      }
      // copy and reset the bytesWritten, reset the isComplete state
      int count = this.bytesWritten;
      this.bytesWritten = 0;
      this.isComplete = false;
      return count;

      
    } catch ( BufferUnderflowException bufe ) {
      //log.debug("There was an underflow problem:");
      bufe.printStackTrace();
      throw new IOException(bufe.getMessage());
            
    }
  }
}
