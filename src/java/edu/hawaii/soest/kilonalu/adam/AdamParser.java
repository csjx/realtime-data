/**
 *  Copyright: 2009 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a single LOOP sample of data produced by
 *            a Davis Scientific Vantage Pro 2 Weather station as described in
 *            the Davis Vantage Serial Protocol document (Vantage Pro and 
 *            Vantage Pro2 Serial Support 2.2 - 01-25-2005)
 *   Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy: $
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
package edu.hawaii.soest.kilonalu.adam;

import edu.hawaii.soest.kilonalu.adam.AdamParser;

import java.io.File; 
import java.io.FileInputStream; 

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents a single Rev "B" sample of data produced by
 *  an Davis Scientific Vantage Pro 2 Weather station in the
 *  default LOOP format.  This class includes fields for each of the stated
 *  byte fields in the LOOP format documentation, along with getter and setter
 *  methods for accessing those fields.
 * 
 *  Note: LOOP Multi-byte binary values are generally stored and sent least 
 *  significant byte first. Negative numbers use 2's complement notation. 
 *  CRC values are sent and received most significant byte first.
 */
public class AdamParser {
    
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
  private static Logger logger = Logger.getLogger(AdamParser.class);
  
  /**
   *  The voltage sense range for the ADAM logger (+/- 10 volts)
   */
  private int voltageSenseRange = 20; 

  /**
   *  The voltage full range for the ADAM logger (16-bit == 65536)
   */
  private int voltageFullRange = 65536;
  
  /*
   *  A field that stores the binary UPD packet data input as a ByteBuffer
   */
  private ByteBuffer packetBuffer = ByteBuffer.allocate(256);
  
  /**
   *  A field that stores the header of the ADAM data.  Documentation on this
   *  is scarce, but it comprises the first 22 bytes of data in the UDP payload,
   *  with bytes 9 - 22 usually zeros.
   */
  private ByteBuffer packetHeader = ByteBuffer.allocate(22);
  
  /**
   *  A field that stores channel zero ADAM data voltage (two bytes).
   */
  private ByteBuffer channelZero = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores channel one ADAM data voltage (two bytes).
   */
  private ByteBuffer channelOne = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel two ADAM data voltage (two bytes).
   */
  private ByteBuffer channelTwo = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel three ADAM data voltage (two bytes).
   */
  private ByteBuffer channelThree = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel four ADAM data voltage (two bytes).
   */
  private ByteBuffer channelFour = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel five ADAM data voltage (two bytes).
   */
  private ByteBuffer channelFive = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel six ADAM data voltage (two bytes).
   */
  private ByteBuffer channelSix = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel seven ADAM data voltage (two bytes).
   */
  private ByteBuffer channelSeven = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel averaged ADAM data voltage (two bytes).
   */
  private ByteBuffer channelAverage = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores channel zero maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelZeroMax = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores channel one maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelOneMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel two maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelTwoMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel three maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelThreeMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel four maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelFourMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel five maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelFiveMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel six maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelSixMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel seven maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelSevenMax = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel averaged maximum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelAverageMax = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores channel zero minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelZeroMin = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores channel one minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelOneMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel two minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelTwoMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel three minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelThreeMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel four minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelFourMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel five minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelFiveMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel six minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelSixMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel seven minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelSevenMin = ByteBuffer.allocate(2);

  /**
   *  A field that stores channel averaged minimum ADAM data voltage (two bytes).
   */
  private ByteBuffer channelAverageMin = ByteBuffer.allocate(2);
  
  
  /**
   *  Constructor:  Builds all of the components of the ADAM data packet from
   *  the ByteBuffer data being passed in.
   *
   *  @param packetBuffer  the ByteBuffer that contains the binary ADAM packet data
   */
  public AdamParser(ByteBuffer packetBuffer) {
    
    this.packetBuffer = packetBuffer;
    // prepare the buffer for reading
    this.packetBuffer.flip();
    byte[] twentyTwoBytes = new byte[22];
    byte[] twoBytes = new byte[2];
    
    // Each of the ADAM packet data fields get set upon instantiation of the parser:
    
    // set the packetHeader field
    this.packetHeader.put(this.packetBuffer.get(twentyTwoBytes));
    
    // set the channelZero field
    this.channelZero.put(this.packetBuffer.get(twoBytes));
    
    // set the channelOne field
    this.channelOne.put(this.packetBuffer.get(twoBytes));
    
    // set the channelTwo field
    this.channelTwo.put(this.packetBuffer.get(twoBytes));
    
    // set the channelThree field
    this.channelThree.put(this.packetBuffer.get(twoBytes));
    
    // set the channelFour field
    this.channelFour.put(this.packetBuffer.get(twoBytes));
    
    // set the channelFive field
    this.channelFive.put(this.packetBuffer.get(twoBytes));
    
    // set the channelSix field
    this.channelSix.put(this.packetBuffer.get(twoBytes));
    
    // set the channelSeven field
    this.channelSeven.put(this.packetBuffer.get(twoBytes));
    
    // set the channelAverage field
    this.channelAverage.put(this.packetBuffer.get(twoBytes));
    
    
    // set the channelZeroMax field
    this.channelZeroMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelOneMax field
    this.channelOneMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelTwoMax field
    this.channelTwoMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelThreeMax field
    this.channelThreeMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelFourMax field
    this.channelFourMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelFiveMax field
    this.channelFiveMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelSixMax field
    this.channelSixMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelSevenMax field
    this.channelSevenMax.put(this.packetBuffer.get(twoBytes));
    
    // set the channelAverageMax field
    this.channelAverageMax.put(this.packetBuffer.get(twoBytes));
    
    
    // set the channelZeroMin field
    this.channelZeroMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelOneMin field
    this.channelOneMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelTwoMin field
    this.channelTwoMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelThreeMin field
    this.channelThreeMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelFourMin field
    this.channelFourMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelFiveMin field
    this.channelFiveMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelSixMin field
    this.channelSixMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelSevenMin field
    this.channelSevenMin.put(this.packetBuffer.get(twoBytes));
    
    // set the channelAverageMin field
    this.channelAverageMin.put(this.packetBuffer.get(twoBytes));
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
                                                
}                                               
