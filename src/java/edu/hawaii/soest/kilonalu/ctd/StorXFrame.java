/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a single Satlantic STOR-X frame
 *             from a binary data file.
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

import org.apache.commons.io.IOUtils;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents a single binary frame from a Satlantic 
 *  STOR-X data file.  
 */
public class StorXFrame {
    
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(StorXFrame.class);

  /* A field that stores the binary frame input as a ByteBuffer */
  private ByteBuffer storXFrame = ByteBuffer.allocate(8192);
  
  /* A StorX frame header field as a byte buffer*/
  private ByteBuffer header = ByteBuffer.allocate(6);
  
  /* A StorX frame serial number field as a byte buffer */
  private ByteBuffer serialNumber = ByteBuffer.allocate(4);
  
  /* A StorX frame analog channel one field as a byte buffer */
  private ByteBuffer analogChannelOne = ByteBuffer.allocate(2);
  
  /* A StorX frame analog channel two field as a byte buffer */
  private ByteBuffer analogChannelTwo = ByteBuffer.allocate(2);
  
  /* A StorX frame analog channel three field as a byte buffer */
  private ByteBuffer analogChannelThree = ByteBuffer.allocate(2);
  
  /* A StorX frame analog channel four field as a byte buffer */
  private ByteBuffer analogChannelFour = ByteBuffer.allocate(2);
  
  /* A StorX frame analog channel five field as a byte buffer */
  private ByteBuffer analogChannelFive = ByteBuffer.allocate(2);
  
  /* A StorX frame analog channel six field as a byte buffer */
  private ByteBuffer analogChannelSix = ByteBuffer.allocate(2);
  
  /* A StorX frame analog channel seven field as a byte buffer */
  private ByteBuffer analogChannelSeven = ByteBuffer.allocate(2);
  
  /* A StorX frame internal voltage field as a byte buffer */
  private ByteBuffer internalVoltage = ByteBuffer.allocate(2);
  
   /* A StorX frame terminator field as a byte buffer */
  private ByteBuffer terminator = ByteBuffer.allocate(2);
  
  /* A StorX frame timestamp field as a byte buffer */
  private ByteBuffer timestamp = ByteBuffer.allocate(7);
  
  /**
   *  Constructor: Creates an empty StorXFrame instance 
   */
  public StorXFrame() {
    
  }
  
  /**
   *  Constructor: Creates a StorXFrame instance that parses a single binary
   *  frame.
   *
   *  @param frameBuffer  the binary data frame as a ByteBuffer
   */
  public StorXFrame(ByteBuffer frameBuffer) {
    
    // store the frame and parse the individual fields out of it
    this.storXFrame = frameBuffer;
    
    byte[] sixBytes  = new byte[6];
    byte[] fourBytes = new byte[4];
    byte[] twoBytes  = new byte[2];
    
    // six bytes for the header
    this.storXFrame.get(sixBytes);
    this.header.put(sixBytes);
    
    // four bytes for the serial number
    this.storXFrame.get(fourBytes);
    this.serialNumber.put(fourBytes);
    
    // two bytes for the analog channel one
    this.storXFrame.get(twoBytes);
    this.analogChannelOne.put(twoBytes);
    
    // two bytes for the analog channel two
    this.storXFrame.get(twoBytes);
    this.analogChannelTwo.put(twoBytes);
    
    // two bytes for the analog channel three
    this.storXFrame.get(twoBytes);
    this.analogChannelThree.put(twoBytes);
    
    // two bytes for the analog channel four
    this.storXFrame.get(twoBytes);
    this.analogChannelFour.put(twoBytes);
    
    // two bytes for the analog channel five
    this.storXFrame.get(twoBytes);
    this.analogChannelFive.put(twoBytes);
    
    // two bytes for the analog channel six
    this.storXFrame.get(twoBytes);
    this.analogChannelSix.put(twoBytes);
    
    // two bytes for the analog channel seven
    this.storXFrame.get(twoBytes);
    this.analogChannelSeven.put(twoBytes);
    
    // two bytes for the internal voltage
    this.storXFrame.get(twoBytes);
    this.internalVoltage.put(twoBytes);
    
    // two bytes for the terminator
    this.storXFrame.get(twoBytes);
    this.terminator.put(twoBytes);
    
    // seven bytes for the timestamp
    this.storXFrame.get(sixBytes);
    this.timestamp.put(sixBytes);
    this.timestamp.put(this.storXFrame.get());
    
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
   * Get the frame analog channel one field as a byte array
   * @return analogChannelone - the analog channel one as a byte array
   */
  public byte[] getAnalogChannelOne() {

    return this.analogChannelOne.array();

  }

  /** 
   * Get the frame analog channel two field as a byte array
   * @return analogChannelTwo - the analog channel two as a byte array
   */
  public byte[] getAnalogChannelTwo() {

    return this.analogChannelTwo.array();

  }

  /** 
   * Get the frame analog channel three field as a byte array
   * @return analogChannelThree - the analog channel three as a byte array
   */
  public byte[] getAnalogChannelThree() {

    return this.analogChannelThree.array();

  }

  /** 
   * Get the frame analog channel four field as a byte array
   * @return analogChannelFour - the analog channel four as a byte array
   */
  public byte[] getAnalogChannelFour() {

    return this.analogChannelFour.array();

  }

  /** 
   * Get the frame analog channel five field as a byte array
   * @return analogChannelFive - the analog channel five as a byte array
   */
  public byte[] getAnalogChannelFive() {

    return this.analogChannelFive.array();

  }

  /** 
   * Get the frame analog channel six field as a byte array
   * @return analogChannelSix - the analog channel six as a byte array
   */
  public byte[] getAnalogChannelSix() {

    return this.analogChannelSix.array();

  }

  /** 
   * Get the frame analog channel seven field as a byte array
   * @return analogChannelSeven - the analog channel seven as a byte array
   */
  public byte[] getAnalogChannelSeven() {

    return this.analogChannelSeven.array();

  }

  /**
   * Get the frame internal voltage field as a float
   * @return internalVoltage - the internal voltage as a float
   */
  public float getInternalVoltage() {

    this.internalVoltage.flip();
    return this.internalVoltage.getFloat();

  }

  /** 
   * Get the frame terminator field as a String
   * @return terminator - the terminator as a String
   */
  public String getTerminator() {

    this.terminator.flip();
    try {
      return new String(this.terminator.array(), "US-ASCII");
      
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
