/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a single SBE CTD sample in a
 *             Satlantic STOR-X frame from a binary data file.
 *
 *   Authors: Christopher Jones
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
package edu.hawaii.soest.kilonalu.ctd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.io.UnsupportedEncodingException;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.io.IOUtils;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents a single binary frame from a Satlantic 
 *  STOR-X data file.  
 */
public class CTDFrame {
    
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(CTDFrame.class);

  /* A Seabird SBE CTD sensor frame ID as a String */
  private final String SBE_CTD_FRAME_ID = "SATSBE";
  
  /* A field that stores the binary frame input as a ByteBuffer */
  private ByteBuffer ctdFrame = ByteBuffer.allocate(8192);
  
  /* A StorX frame header field as a byte buffer*/
  private ByteBuffer header = ByteBuffer.allocate(6);
  
  /* A StorX frame serial number field as a byte buffer */
  private ByteBuffer serialNumber = ByteBuffer.allocate(4);
  
   /* A CTD sample field as a byte buffer */
  private ByteBuffer sample = ByteBuffer.allocate(256);
  
  /* A StorX frame timestamp field as a byte buffer */
  private ByteBuffer timestamp = ByteBuffer.allocate(7);
  
  /**
   *  Constructor: Creates an empty CTDFrame instance 
   */
  public CTDFrame() {
    
  }
  
  /**
   *  Constructor: Creates a CTDFrame instance that parses a single binary
   *  frame.
   *
   *  @param frameBuffer  the binary data frame as a ByteBuffer
   */
  public CTDFrame(ByteBuffer frameBuffer) {
    
    // store the frame and parse the individual fields out of it
    this.ctdFrame = frameBuffer;
    
    byte[] sixBytes  = new byte[6];
    byte[] fourBytes = new byte[4];
    
    // six bytes for the header
    this.ctdFrame.get(sixBytes);
    this.header.put(sixBytes);
    
    // four bytes for the serial number
    this.ctdFrame.get(fourBytes);
    this.serialNumber.put(fourBytes);
    
    // move to the timestamp field and extract it
    this.ctdFrame.position(this.ctdFrame.capacity() - 7);
    int endSampleIndex = this.ctdFrame.position();
    
    // seven bytes for the timestamp
    this.ctdFrame.get(sixBytes);
    this.timestamp.put(sixBytes);
    this.timestamp.put(this.ctdFrame.get());
    
    // reset the position to extract the CTD sample (or message)
    this.ctdFrame.position(SBE_CTD_FRAME_ID.length() + 5);
    int sampleLength = endSampleIndex - this.ctdFrame.position();
    byte[] sampleBytes = new byte[sampleLength];
    this.ctdFrame.get(sampleBytes);
    this.sample.put(sampleBytes);
    
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
   * A method that sets the log configuration file name
   *
   * @param logConfigurationFile  the log configuration file name
   */
  public void setLogConfigurationFile(String logConfigurationFile) {
    this.logConfigurationFile = logConfigurationFile;
  }
  
  /* 
   * Get the frame header field as a String
   * @return frameHeader - the frame header as a String
   */
  public String getHeader() {

    this.header.flip();
    
    try {
      return new String(this.header.array(), "US-ASCII");
      
    } catch (UnsupportedEncodingException uee) {
      logger.debug("The string encoding was not recognized: " +
                   uee.getMessage());
      return null;
    }

  }

  /* 
   * Get the frame serial number as a String
   * @return serialNumber - the serial number as a String
   */
  public String getSerialNumber() {
  
    try {
      return new String(this.serialNumber.array(), "US-ASCII");
      
    } catch (UnsupportedEncodingException uee) {
      logger.debug("The string encoding was not recognized: " +
                   uee.getMessage());
      return null;
    }
  
  }

  /** 
   * Get the CTD sample field as a String
   * @return sample - the sample as a String
   */
  public String getSample() {

    this.sample.flip();
    String sampleString;
    
    try {
      sampleString = new String(this.sample.array(), "US-ASCII");
      // strip leading command and trailing null characters
      int spaceIndex = sampleString.indexOf(" ");
      int nullIndex = sampleString.indexOf(0);
      
      if ( spaceIndex > 0 ) {
        // has leading and trailing
        return sampleString.substring(spaceIndex, nullIndex);
    
      } else if ( spaceIndex == 0 ){
        // has trailing
        return sampleString.substring(0, nullIndex);
        
      } else {
        return sampleString;
        
      }
      
    } catch (UnsupportedEncodingException uee) {
      logger.debug("The string encoding was not recognized: " +
                   uee.getMessage());
      return null;
    }


  }

  /**
   * Get the frame timestamp field as a byte array. The timestamp format is
   * YYYYDDD from the first 3 bytes, and HHMMSS.SSS from the last four:
   * Example:
   * 1E AC CC = 2010316 (year 2010, julian day 316)
   * 09 9D 3E 20 = 16:13:00.000 (4:13 pm)
   * @return timestamp - the frame timestamp as a byte array
   */
  public byte[] getTimestamp() {

    this.timestamp.flip();
    return this.timestamp.array();

  }

}                                               
