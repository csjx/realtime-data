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
  private float voltageSenseRange = 20f; 

  /**
   *  The voltage full range for the ADAM logger (16-bit == 65536)
   */
  private float voltageFullRange = 65536f;
  
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
                                                
  /**
   * A method that gets the packetHeader data
   *
   * @return packetHeader - the 22 bytes of the packetHeader field as a ByteBuffer
   */
  public ByteBuffer getPacketHeader() {
    return this.packetHeader;
  }
  
  /**
   * A method that gets the channel zero data as a converted decimal float
   *
   * @return channelZero - the 2 bytes of the channelZero data as a float
   */
  public float getChannelZero() {
    float channelData = 
      (float) this.channelZero.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel one data as a converted decimal float
   *
   * @return channelOne - the 2 bytes of the channelOne data as a float
   */
  public float getChannelOne() {
    float channelData = 
      (float) this.channelOne.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel two data as a converted decimal float
   *
   * @return channelTwo - the 2 bytes of the channelTwo data as a float
   */
  public float getChannelTwo() {
    float channelData = 
      (float) this.channelTwo.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel three data as a converted decimal float
   *
   * @return channelThree - the 2 bytes of the channelThree data as a float
   */
  public float getChannelThree() {
    float channelData = 
      (float) this.channelThree.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel four data as a converted decimal float
   *
   * @return channelFour - the 2 bytes of the channelFour data as a float
   */
  public float getChannelFour() {
    float channelData = 
      (float) this.channelFour.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel five data as a converted decimal float
   *
   * @return channelFive - the 2 bytes of the channelFive data as a float
   */
  public float getChannelFive() {
    float channelData = 
      (float) this.channelFive.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel six data as a converted decimal float
   *
   * @return channelSix - the 2 bytes of the channelSix data as a float
   */
  public float getChannelSix() {
    float channelData = 
      (float) this.channelSix.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel seven data as a converted decimal float
   *
   * @return channelSeven - the 2 bytes of the channelSeven data as a float
   */
  public float getChannelSeven() {
    float channelData = 
      (float) this.channelSeven.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel average data as a converted decimal float
   *
   * @return channelAverage - the 2 bytes of the channelAverage data as a float
   */
  public float getChannelAverage() {
    float channelData = 
      (float) this.channelAverage.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel zero maximum data as a converted decimal float
   *
   * @return channelZeroMax  - the 2 bytes of the channelZeroMax data as a float
   */
  public float getChannelZeroMax() {
    float channelData = 
      (float) this.channelZeroMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel one data maximum as a converted decimal float
   *
   * @return channelOneMax - the 2 bytes of the channelOneMax data as a float
   */
  public float getChannelOneMax() {
    float channelData = 
      (float) this.channelOneMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel two data maximum as a converted decimal float
   *
   * @return channelTwoMax - the 2 bytes of the channelTwoMax data as a float
   */
  public float getChannelTwoMax() {
    float channelData = 
      (float) this.channelTwoMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel three data maximum as a converted decimal float
   *
   * @return channelThreeMax - the 2 bytes of the channelThreeMax data as a float
   */
  public float getChannelThreeMax() {
    float channelData = 
      (float) this.channelThreeMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel four data maximum as a converted decimal float
   *
   * @return channelFourMax - the 2 bytes of the channelFourMax data as a float
   */
  public float getChannelFourMax() {
    float channelData = 
      (float) this.channelFourMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel five data maximum as a converted decimal float
   *
   * @return channelFiveMax - the 2 bytes of the channelFiveMax data as a float
   */
  public float getChannelFiveMax() {
    float channelData = 
      (float) this.channelFiveMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel six data maximum as a converted decimal float
   *
   * @return channelSixMax - the 2 bytes of the channelSixMax data as a float
   */
  public float getChannelSixMax() {
    float channelData = 
      (float) this.channelSixMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel seven data maximum as a converted decimal float
   *
   * @return channelSevenMax - the 2 bytes of the channelSevenMax data as a float
   */
  public float getChannelSevenMax() {
    float channelData = 
      (float) this.channelSevenMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel average maximum data as a converted decimal float
   *
   * @return channelAverageMax - the 2 bytes of the channelAverageMax data as a float
   */
  public float getChannelAverageMax() {
    float channelData = 
      (float) this.channelAverageMax.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel zero mimimum data as a converted decimal float
   *
   * @return channelZeroMin  - the 2 bytes of the channelZeroMin data as a float
   */
  public float getChannelZeroMin() {
    float channelData = 
      (float) this.channelZeroMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel one data mimimum as a converted decimal float
   *
   * @return channelOneMin - the 2 bytes of the channelOneMin data as a float
   */
  public float getChannelOneMin() {
    float channelData = 
      (float) this.channelOneMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel two data mimimum as a converted decimal float
   *
   * @return channelTwoMin - the 2 bytes of the channelTwoMin data as a float
   */
  public float getChannelTwoMin() {
    float channelData = 
      (float) this.channelTwoMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel three data mimimum as a converted decimal float
   *
   * @return channelThreeMin - the 2 bytes of the channelThreeMin data as a float
   */
  public float getChannelThreeMin() {
    float channelData = 
      (float) this.channelThreeMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel four data mimimum as a converted decimal float
   *
   * @return channelFourMin - the 2 bytes of the channelFourMin data as a float
   */
  public float getChannelFourMin() {
    float channelData = 
      (float) this.channelFourMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel five data mimimum as a converted decimal float
   *
   * @return channelFiveMin - the 2 bytes of the channelFiveMin data as a float
   */
  public float getChannelFiveMin() {
    float channelData = 
      (float) this.channelFiveMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel six data mimimum as a converted decimal float
   *
   * @return channelSixMin - the 2 bytes of the channelSixMin data as a float
   */
  public float getChannelSixMin() {
    float channelData = 
      (float) this.channelSixMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel seven data mimimum as a converted decimal float
   *
   * @return channelSevenMin - the 2 bytes of the channelSevenMin data as a float
   */
  public float getChannelSevenMin() {
    float channelData = 
      (float) this.channelSevenMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel average mimimum data as a converted decimal float
   *
   * @return channelAverageMin - the 2 bytes of the channelAverageMin data as a float
   */
  public float getChannelAverageMin() {
    float channelData = 
      (float) this.channelAverageMin.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /*
   * A method that applies a conversion to raw ADAM module channel data to
   * produce a voltage for the channel
   *
   * @return voltage - the converted decimal voltage as a float
   */
  private float getVoltage(float channelData) {
    float voltage = 
    ( this.voltageSenseRange * ( channelData / this.voltageFullRange ) -
      this.voltageSenseRange/2 );
    return voltage;
    
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
}                                               
