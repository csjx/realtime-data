/**
 *  Copyright: 2009 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a single LOOP sample of data produced by
 *            a Davis Scientific Vantage Pro 2 Weather station as described in
 *            the Davis Vantage Serial Protocol document (Vantage Pro and 
 *            Vantage Pro2 Serial Support 2.2 - 01-25-2005)
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
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j2.properties";

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
    
    // Each of the ADAM packet data fields get set upon instantiation of the parser:
    
    // set the packetHeader field
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    this.packetHeader.put(this.packetBuffer.get());
    
    // set the channelZero field
    this.channelZero.put(this.packetBuffer.get());
    this.channelZero.put(this.packetBuffer.get());
    
    // set the channelOne field
    this.channelOne.put(this.packetBuffer.get());
    this.channelOne.put(this.packetBuffer.get());
    
    // set the channelTwo field
    this.channelTwo.put(this.packetBuffer.get());
    this.channelTwo.put(this.packetBuffer.get());
    
    // set the channelThree field
    this.channelThree.put(this.packetBuffer.get());
    this.channelThree.put(this.packetBuffer.get());
    
    // set the channelFour field
    this.channelFour.put(this.packetBuffer.get());
    this.channelFour.put(this.packetBuffer.get());
    
    // set the channelFive field
    this.channelFive.put(this.packetBuffer.get());
    this.channelFive.put(this.packetBuffer.get());
    
    // set the channelSix field
    this.channelSix.put(this.packetBuffer.get());
    this.channelSix.put(this.packetBuffer.get());
    
    // set the channelSeven field
    this.channelSeven.put(this.packetBuffer.get());
    this.channelSeven.put(this.packetBuffer.get());
    
    // set the channelAverage field
    this.channelAverage.put(this.packetBuffer.get());
    this.channelAverage.put(this.packetBuffer.get());
    
    
    // set the channelZeroMax field
    this.channelZeroMax.put(this.packetBuffer.get());
    this.channelZeroMax.put(this.packetBuffer.get());
    
    // set the channelOneMax field
    this.channelOneMax.put(this.packetBuffer.get());
    this.channelOneMax.put(this.packetBuffer.get());
    
    // set the channelTwoMax field
    this.channelTwoMax.put(this.packetBuffer.get());
    this.channelTwoMax.put(this.packetBuffer.get());
    
    // set the channelThreeMax field
    this.channelThreeMax.put(this.packetBuffer.get());
    this.channelThreeMax.put(this.packetBuffer.get());
    
    // set the channelFourMax field
    this.channelFourMax.put(this.packetBuffer.get());
    this.channelFourMax.put(this.packetBuffer.get());
    
    // set the channelFiveMax field
    this.channelFiveMax.put(this.packetBuffer.get());
    this.channelFiveMax.put(this.packetBuffer.get());
    
    // set the channelSixMax field
    this.channelSixMax.put(this.packetBuffer.get());
    this.channelSixMax.put(this.packetBuffer.get());
    
    // set the channelSevenMax field
    this.channelSevenMax.put(this.packetBuffer.get());
    this.channelSevenMax.put(this.packetBuffer.get());
    
    // set the channelAverageMax field
    this.channelAverageMax.put(this.packetBuffer.get());
    this.channelAverageMax.put(this.packetBuffer.get());
    
    
    // set the channelZeroMin field
    this.channelZeroMin.put(this.packetBuffer.get());
    this.channelZeroMin.put(this.packetBuffer.get());
    
    // set the channelOneMin field
    this.channelOneMin.put(this.packetBuffer.get());
    this.channelOneMin.put(this.packetBuffer.get());
    
    // set the channelTwoMin field
    this.channelTwoMin.put(this.packetBuffer.get());
    this.channelTwoMin.put(this.packetBuffer.get());
    
    // set the channelThreeMin field
    this.channelThreeMin.put(this.packetBuffer.get());
    this.channelThreeMin.put(this.packetBuffer.get());
    
    // set the channelFourMin field
    this.channelFourMin.put(this.packetBuffer.get());
    this.channelFourMin.put(this.packetBuffer.get());
    
    // set the channelFiveMin field
    this.channelFiveMin.put(this.packetBuffer.get());
    this.channelFiveMin.put(this.packetBuffer.get());
    
    // set the channelSixMin field
    this.channelSixMin.put(this.packetBuffer.get());
    this.channelSixMin.put(this.packetBuffer.get());
    
    // set the channelSevenMin field
    this.channelSevenMin.put(this.packetBuffer.get());
    this.channelSevenMin.put(this.packetBuffer.get());
    
    // set the channelAverageMin field
    this.channelAverageMin.put(this.packetBuffer.get());
    this.channelAverageMin.put(this.packetBuffer.get());
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
    this.packetHeader.flip();
    return this.packetHeader;
  }
  
  /**
   * A method that gets the channel zero data as a converted decimal float
   *
   * @return channelZero - the 2 bytes of the channelZero data as a float
   */
  public float getChannelZero() {
    this.channelZero.flip();    
    int channelData = (int) this.channelZero.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel one data as a converted decimal float
   *
   * @return channelOne - the 2 bytes of the channelOne data as a float
   */
  public float getChannelOne() {
    this.channelOne.flip();
    int channelData = (int) this.channelOne.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel two data as a converted decimal float
   *
   * @return channelTwo - the 2 bytes of the channelTwo data as a float
   */
  public float getChannelTwo() {
    this.channelTwo.flip();
    int channelData = (int) this.channelTwo.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel three data as a converted decimal float
   *
   * @return channelThree - the 2 bytes of the channelThree data as a float
   */
  public float getChannelThree() {
    this.channelThree.flip();
    int channelData = (int) this.channelThree.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel four data as a converted decimal float
   *
   * @return channelFour - the 2 bytes of the channelFour data as a float
   */
  public float getChannelFour() {
    this.channelFour.flip();
    int channelData = (int) this.channelFour.order(ByteOrder.BIG_ENDIAN).getShort();
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel five data as a converted decimal float
   *
   * @return channelFive - the 2 bytes of the channelFive data as a float
   */
  public float getChannelFive() {
    this.channelFive.flip();
    int channelData = (int) this.channelFive.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel six data as a converted decimal float
   *
   * @return channelSix - the 2 bytes of the channelSix data as a float
   */
  public float getChannelSix() {
    this.channelSix.flip();
    int channelData = (int) this.channelSix.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel seven data as a converted decimal float
   *
   * @return channelSeven - the 2 bytes of the channelSeven data as a float
   */
  public float getChannelSeven() {
    this.channelSeven.flip();
    int channelData = (int) this.channelSeven.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel average data as a converted decimal float
   *
   * @return channelAverage - the 2 bytes of the channelAverage data as a float
   */
  public float getChannelAverage() {
    this.channelAverage.flip();
    int channelData = (int) this.channelAverage.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel zero maximum data as a converted decimal float
   *
   * @return channelZeroMax  - the 2 bytes of the channelZeroMax data as a float
   */
  public float getChannelZeroMax() {
    this.channelZeroMax.flip();
    int channelData = (int) this.channelZeroMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel one data maximum as a converted decimal float
   *
   * @return channelOneMax - the 2 bytes of the channelOneMax data as a float
   */
  public float getChannelOneMax() {
    this.channelOneMax.flip();
    int channelData = (int) this.channelOneMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel two data maximum as a converted decimal float
   *
   * @return channelTwoMax - the 2 bytes of the channelTwoMax data as a float
   */
  public float getChannelTwoMax() {
    this.channelTwoMax.flip();
    int channelData = (int) this.channelTwoMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel three data maximum as a converted decimal float
   *
   * @return channelThreeMax - the 2 bytes of the channelThreeMax data as a float
   */
  public float getChannelThreeMax() {
    this.channelThreeMax.flip();
    int channelData = (int) this.channelThreeMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel four data maximum as a converted decimal float
   *
   * @return channelFourMax - the 2 bytes of the channelFourMax data as a float
   */
  public float getChannelFourMax() {
    this.channelFourMax.flip();
    int channelData = (int) this.channelFourMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel five data maximum as a converted decimal float
   *
   * @return channelFiveMax - the 2 bytes of the channelFiveMax data as a float
   */
  public float getChannelFiveMax() {
    this.channelFiveMax.flip();
    int channelData = (int) this.channelFiveMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel six data maximum as a converted decimal float
   *
   * @return channelSixMax - the 2 bytes of the channelSixMax data as a float
   */
  public float getChannelSixMax() {
    this.channelSixMax.flip();
    int channelData = (int) this.channelSixMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel seven data maximum as a converted decimal float
   *
   * @return channelSevenMax - the 2 bytes of the channelSevenMax data as a float
   */
  public float getChannelSevenMax() {
    this.channelSevenMax.flip();
    int channelData = (int) this.channelSevenMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel average maximum data as a converted decimal float
   *
   * @return channelAverageMax - the 2 bytes of the channelAverageMax data as a float
   */
  public float getChannelAverageMax() {
    this.channelAverageMax.flip();
    int channelData = (int) this.channelAverageMax.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel zero mimimum data as a converted decimal float
   *
   * @return channelZeroMin  - the 2 bytes of the channelZeroMin data as a float
   */
  public float getChannelZeroMin() {
    this.channelZeroMin.flip();
    int channelData = (int) this.channelZeroMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel one data mimimum as a converted decimal float
   *
   * @return channelOneMin - the 2 bytes of the channelOneMin data as a float
   */
  public float getChannelOneMin() {
    this.channelOneMin.flip();
    int channelData = (int) this.channelOneMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel two data mimimum as a converted decimal float
   *
   * @return channelTwoMin - the 2 bytes of the channelTwoMin data as a float
   */
  public float getChannelTwoMin() {
    this.channelTwoMin.flip();
    int channelData = (int) this.channelTwoMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel three data mimimum as a converted decimal float
   *
   * @return channelThreeMin - the 2 bytes of the channelThreeMin data as a float
   */
  public float getChannelThreeMin() {
    this.channelThreeMin.flip();
    int channelData = (int) this.channelThreeMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel four data mimimum as a converted decimal float
   *
   * @return channelFourMin - the 2 bytes of the channelFourMin data as a float
   */
  public float getChannelFourMin() {
    this.channelFourMin.flip();
    int channelData = (int) this.channelFourMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel five data mimimum as a converted decimal float
   *
   * @return channelFiveMin - the 2 bytes of the channelFiveMin data as a float
   */
  public float getChannelFiveMin() {
    this.channelFiveMin.flip();
    int channelData = (int) this.channelFiveMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel six data mimimum as a converted decimal float
   *
   * @return channelSixMin - the 2 bytes of the channelSixMin data as a float
   */
  public float getChannelSixMin() {
    this.channelSixMin.flip();
    int channelData = (int) this.channelSixMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel seven data mimimum as a converted decimal float
   *
   * @return channelSevenMin - the 2 bytes of the channelSevenMin data as a float
   */
  public float getChannelSevenMin() {
    this.channelSevenMin.flip();
    int channelData = (int) this.channelSevenMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /**
   * A method that gets the channel average mimimum data as a converted decimal float
   *
   * @return channelAverageMin - the 2 bytes of the channelAverageMin data as a float
   */
  public float getChannelAverageMin() {
    this.channelAverageMin.flip();
    int channelData = (int) this.channelAverageMin.order(ByteOrder.BIG_ENDIAN).getShort();
    
    return getVoltage(channelData);
  }
  
  /*
   * A method that applies a conversion to raw ADAM module channel data to
   * produce a voltage for the channel
   *
   * @return voltage - the converted decimal voltage as a float
   */
  private float getVoltage(int channelData) {    
    channelData = (channelData & 0x000000000000FFFF);
    float voltage =
    ( this.voltageSenseRange * ( channelData / this.voltageFullRange ) ) -
      ( this.voltageSenseRange/2 );
    return voltage;
    
  }
}                                               
