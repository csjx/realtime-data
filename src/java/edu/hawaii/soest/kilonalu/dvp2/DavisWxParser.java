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
package edu.hawaii.soest.kilonalu.dvp2;

import java.io.File; 
import java.io.FileInputStream; 

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import java.util.HashMap;

import org.apache.commons.codec.binary.Hex;

/**
 *  A class that represents a single Rev "B" sample of data produced by
 *  an Davis Scientific Vantage Pro 2 Weather station in the
 *  default LOOP format.  This class includes fields for each of the stated
 *  byte fields in the LOOP format documentation, along with getter and setter
 *  methods for accessing those fields.
 */
public class DavisWxParser {
    
  /**
   *  A field that stores the LOOP packet identifier (three bytes) in a ByteBuffer
   *  which contains the three letters 'LOO'.  The 'P' slot is now used in Rev 'B'
   *  packets to store the barometric trend.
   */
  private ByteBuffer loopID = ByteBuffer.allocate(3);
  
  /**
   *  A field that stores the signed byte that indicates the current 3-hour 
   *  barometer trend.  It is one of these values:
   *
   *  -60 = Falling Rapidly  = 196 (as an unsigned byte) 
   *  -20 = Falling Slowly   = 236 (as an unsigned byte) 
   *    0 = Steady 
   *   20 = Rising Slowly 
   *   60 = Rising Rapidly 
   *   80 = ASCII "P" = Rev A firmware, no trend info is available 
   *  Any other value means that the Vantage does not have the 3 
   *  hours of bar data needed to determine the bar trend. 
   
   *  */
  private ByteBuffer barTrend = ByteBuffer.allocate(1);

  /**
   *  A field that stores the LOOP packet type (one byte) in a ByteBuffer
   *  which contains the value zero.  In the future Davis may define new LOOP 
   *  packet formats and assign a different value to this field.  
   */
  private ByteBuffer packetType = ByteBuffer.allocate(1);

  /**
   *  A field that stores the next record value (two bytes) in a ByteBuffer
   *  which is the location in the archive memory where the next data packet 
   *  will be written. This can be monitored to detect when a new record is 
   *  created. 
   */
  private ByteBuffer nextRecord = ByteBuffer.allocate(2);

  /**
   *  A field that stores the current barometer value (two bytes) in a ByteBuffer.
   *  Units are (in Hg / 1000). The barometric value should be between 20 
   *  inches and 32.5 inches in Vantage Pro and between 20 inches and 32.5 
   *  inches in both Vantatge Pro Vantage Pro2.  Values outside these ranges 
   *  will not be logged.
   */
  private ByteBuffer barometer = ByteBuffer.allocate(2);

  /**
   *  A field that stores the inside temperature value (two bytes) in a ByteBuffer.
   *  The value is sent as 10th of a degree in F.  For example, 795 is 
   *  returned for 79.5°F.
   */
  private ByteBuffer insideTemperature = ByteBuffer.allocate(2);

  /**
   *  A field that stores the inside humidity value (one byte) in a ByteBuffer.
   *  This is the relative humidity in %, such as 50 is returned for 50%.
   */
  private ByteBuffer insideHumidity = ByteBuffer.allocate(1);

  /**
   *  A field that stores the outside temperature value (two bytes) in a ByteBuffer.
   *  The value is sent as 10th of a degree in F.  For example, 795 is 
   *  returned for 79.5°F.
   */
  private ByteBuffer outsideTemperature = ByteBuffer.allocate(2);

  /**
   *  A field that stores the wind speed value (one byte) in a ByteBuffer.
   *  It is a byte unsigned value in mph.  If the wind speed is dashed 
   *  because it lost synchronization with the radio or due to some 
   *  other reason, the wind speed is forced to be 0. 
   */
  private ByteBuffer windSpeed = ByteBuffer.allocate(1);

  /**
   *  A field that stores the ten minute averagewind speed value (one byte) in a ByteBuffer.
   *  It is a byte unsigned value in mph.
   */
  private ByteBuffer tenMinuteAverageWindSpeed = ByteBuffer.allocate(1);

  /**
   *  A field that stores the wind direction value (two bytes) in a ByteBuffer.
   *  It is a two byte unsigned value from 0 to 360 degrees.  (0° is 
   *  North, 90° is East, 180° is South and 270° is West.) 
   */
  private ByteBuffer windDirection = ByteBuffer.allocate(2);

  /**
   *  A field that stores seven extra temperature values (seven bytes) in a ByteBuffer.
   *  This field supports seven extra temperature stations. 
   *  Each byte is one extra temperature value in whole degrees F with 
   *  an offset of 90 degrees.  For example, a value of 0 = -90°F ; a 
   *  value of 100 = 10°F ; and a value of 169 = 79°F.
   */
  private ByteBuffer extraTemperatures = ByteBuffer.allocate(7);

  /**
   *  A field that stores soil temperature values (four bytes) in a ByteBuffer.
   *  This field supports four soil temperature sensors.
   *  Each byte is one temperature value in whole degrees F with 
   *  an offset of 90 degrees.  For example, a value of 0 = -90°F ; a 
   *  value of 100 = 10°F ; and a value of 169 = 79°F.
   */
  private ByteBuffer soilTemperatures = ByteBuffer.allocate(4);

  /**
   *  A field that stores leaf temperature values (four bytes) in a ByteBuffer.
   *  This field supports four soil temperature sensors.
   *  Each byte is one temperature value in whole degrees F with 
   *  an offset of 90 degrees.  For example, a value of 0 = -90°F ; a 
   *  value of 100 = 10°F ; and a value of 169 = 79°F.
   */
  private ByteBuffer leafTemperatures = ByteBuffer.allocate(4);

  /**
   *  A field that stores the outside humidity value (one byte) in a ByteBuffer.
   *  This is the relative humidity in %, such as 50 is returned for 50%.
   */
  private ByteBuffer outsideHumidity = ByteBuffer.allocate(1);

  /**
   *  A field that stores seven extra humidity values (seven bytes) in a ByteBuffer.
   *  Relative humidity in % for extra seven humidity stations.  
   */
  private ByteBuffer extraHumidities = ByteBuffer.allocate(7);

  /**
   *  A field that stores the rain rate value (two bytes) in a ByteBuffer.
   *  This value is sent as 100 of a inch per hour.  For example, 256 
   *  represent 2.56 inches/hour. 
   */
  private ByteBuffer rainRate = ByteBuffer.allocate(2);

  /**
   *  A field that stores the UV radiation value (one byte) in a ByteBuffer.
   *  The unit is in UV index.
   */
  private ByteBuffer uvRadiation = ByteBuffer.allocate(1);

  /**
   *  A field that stores the solar radiation value (two bytes) in a ByteBuffer.
   *  The unit is in watt/meter^2.
   */
  private ByteBuffer solarRadiation = ByteBuffer.allocate(2);

  /**
   *  A field that stores the storm rain value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer stormRain = ByteBuffer.allocate(2);

  /**
   *  A field that stores the current storm start date (two bytes) in a ByteBuffer.
   *  Bit 15 to bit 12 is the month, bit 11 to bit 7 is the day and bit 6 to 
   *  bit 0 is the year offseted by 2000.
   */
  private ByteBuffer currentStormStartDate = ByteBuffer.allocate(2);

  /**
   *  A field that stores the daily rain value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer dailyRain = ByteBuffer.allocate(2);

  /**
   *  A field that stores the monthly rain value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer monthlyRain = ByteBuffer.allocate(2);

  /**
   *  A field that stores the yearly rain value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer yearlyRain = ByteBuffer.allocate(2);

  /**
   *  A field that stores the daily evapotranspiration value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer dailyEvapoTranspiration = ByteBuffer.allocate(2);

  /**
   *  A field that stores the monthly evapotranspiration value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer monthlyEvapoTranspiration = ByteBuffer.allocate(2);

  /**
   *  A field that stores the yearly evapotranspiration value (two bytes) in a ByteBuffer.
   *  The storm is stored as 100th of an inch. 
   */
  private ByteBuffer yearlyEvapoTranspiration = ByteBuffer.allocate(2);

  /**
   *  A field that stores soil moisture values (four bytes) in a ByteBuffer.
   *  The unit is in centibar.  It supports four soil sensors. 
   */
  private ByteBuffer soilMoistures = ByteBuffer.allocate(4);

  /**
   *  A field that stores leaf wetness values (four bytes) in a ByteBuffer.
   *  This is a scale number from 0 to 15 with 0 meaning very dry and 
   *  15 meaning very wet.  It supports four leaf sensors. 
   */
  private ByteBuffer leafWetnesses = ByteBuffer.allocate(4);

  /**
   *  A field that stores the currently active inside alarm value (one byte) in a ByteBuffer.
   *  Field                         Bit #  
   *  Inside Alarms                 Currently active inside alarms.  
   *  Falling bar trend alarm       0  
   *  Rising bar trend alarm        1  
   *  Low inside temp alarm         2  
   *  High inside temp alarm        3  
   *  Low inside hum alarm          4  
   *  High inside hum alarm         5  
   *  Time alarm                    6  
   */
  private ByteBuffer insideAlarm = ByteBuffer.allocate(1);

  /**
   *  A field that stores the currently active rain alarm value (one byte) in a ByteBuffer.
   *  Field                       Bit #  
   *  Rain Alarms                 Currently active rain alarms.  
   *  High rain rate alarm        0  
   *  15 min rain alarm           1  Flash flood alarm
   *  24 hr rain alarm            2  
   *  storm total rain alarm      3  
   *  daily ET  alarm             4  
   */
  private ByteBuffer rainAlarm = ByteBuffer.allocate(1);

  /**
   *  A field that stores the currently outside alarm values (two bytes) in a ByteBuffer.
   *  Field                       Bit #  of Byte 1
   *  Low outside temp alarm      0  
   *  High outside temp alarm     1  
   *  Wind speed alarm            2  
   *  10 min avg speed alarm      3  
   *  Low dewpoint alarm          4  
   *  High dewpoint alarm         5  
   *  High heat alarm             6  
   *  Low wind chill alarm        7  
   *  Field                       Bit #  of Byte 2
   *  High THSW alarm             0  
   *  High solar rad alarm        1  
   *  High UV alarm               2  
   *  UV Dose alarm               3  
   */
  private ByteBuffer outsideAlarms = ByteBuffer.allocate(2);

  /**
   *  A field that stores the extra temperature and humidity alarm values 
   * (eight bytes) in a ByteBuffer.
   *  Field                       Bit #  of Byte 1
   *  Low Humidity alarm          1  Currently active outside humidity alarms.
   *  Low Humidity alarm          2  
   *  High Humidity alarm         3  
   *  Field                       Bit #  of Byte 2-8:
   *    Each byte contains four alarm bits (0 – 3) for a single extra 
   *    Temp/Hum station. Bits (4 – 7) are not used and reserved for 
   *    future use. 
   *    Use the temperature and humidity sensor numbers, as 
   *    described in Section XIII.4 to locate which byte contains the 
   *    appropriate alarm bits. In particular, the humidity and 
   *    temperature alarms for a single station will be found in 
   *    different bytes.
   */
  private ByteBuffer extraTemperatureHumidityAlarms = ByteBuffer.allocate(8);

  /**
   *  A field that stores the soil and leaf alarm values 
   * (four bytes) in a ByteBuffer.
   *  Field                       Bit #  of Byte 1
   *  Low leaf wetness X alarm    0  
   *  High leaf wetness X alarm   1  
   *  Low soil moisture X alarm   2  
   *  High soil moisture X alarm  3  
   *  Low leaf temp X alarm       4  
   *  High leaf temp X alarm      5  
   *  Low soil temp X alarm       6  
   *  High soil temp X alarm      7  
   */
  private ByteBuffer soilLeafAlarms = ByteBuffer.allocate(4);

  /**
   *  A field that stores the transmitter battery status value (one byte) in a ByteBuffer.
   */
  private ByteBuffer transmitterBatteryStatus = ByteBuffer.allocate(1);

  /**
   *  A field that stores the console battery voltage value (two bytes) in a ByteBuffer.
   *  Voltage = ((Data * 300)/512)/100.0
   */
  private ByteBuffer consoleBatteryVoltage = ByteBuffer.allocate(2);

  /**
   *  A field that stores the forecast icon values (one byte) in a ByteBuffer.
   *  Forecast Icons      Bit maps for forecast icons on the console screen. 
   *  Rain                0  
   *  Cloud               1  
   *  Partly Cloudy       2  
   *  Sun                 3  
   *  Snow                4 
   * 
   *  Forecast Icon Values 
   *  
   *  Value Decimal Value Hex  Segments Shown          Forecast 
   *  8             0x08       Sun Mostly              Clear 
   *  6             0x06       Partial Sun + Cloud     Partially Cloudy 
   *  2             0x02       Cloud Mostly            Cloudy 
   *  3             0x03       Cloud + Rain            Mostly Cloudy, Rain within 12 hours 
   *  18            0x12       Cloud + Snow            Mostly Cloudy, Snow within 12 hours 
   *  19            0x13       Cloud + Rain + Snow     Mostly Cloudy, Rain or Snow within 12 hours 
   *  7             0x07       Partial Sun + Cloud +   Partially Cloudy, Rain within 12 hours
   *                           Rain  
   *  22            0x16       Partial Sun + Cloud +   Partially Cloudy, Snow within 12 hours 
   *                           Snow 
   *  23            0x17       Partial Sun + Cloud +   Partially Cloudy, Rain or Snow within 12 hours
   *                           Rain + Snow 
   */
  private ByteBuffer forecastIconValues = ByteBuffer.allocate(1);

  /**
   *  A field that stores the forecast rule number value (one byte) in a ByteBuffer.
   */
  private ByteBuffer forecastRuleNumber = ByteBuffer.allocate(1);

  /**
   *  A field that stores the time of sunrise (two bytes) in a ByteBuffer.
   *  The time is stored as hour * 100 + min. 
   */
  private ByteBuffer timeOfSunrise = ByteBuffer.allocate(2);

  /**
   *  A field that stores the time of sunset (two bytes) in a ByteBuffer.
   *  The time is stored as hour * 100 + min. 
   */
  private ByteBuffer timeOfSunset = ByteBuffer.allocate(2);

  /**
   *  A field that stores record delimiter (two bytes) in a ByteBuffer.
   *  The values are "\n" <LF> = 0x0A and then "\r" <CR> = 0x0D 
   */
  private ByteBuffer recordDelimiter = ByteBuffer.allocate(2);

  /**
   *  A field that stores CRC checksum value (two bytes) in a ByteBuffer.
   *  The CRC checking used by the WeatherLink is based on the CRC-CCITT 
   *  standard. The heart of the method involves a CRC-accumulator that 
   *  uses the following formula on each successive data byte. After all 
   *  the data bytes have been "accumulated", there will be a two byte CRC 
   *  checksum that will get processed in the same manner as the data bytes. 
   *  If there has been no transmission error, then the final CRC-accumulator 
   *  value will be 0 (assuming it was set to zero before accumulating data). 
      
   *  In the following code, "crc" is the crc accumulator (16 bits or 2 bytes), 
   *  "data" is the data or CRC checksum byte to be accumulated, and "crc_table" 
   *  is the table of CRC values found in the CCITT.h header file. The operator 
   *  "^" is an exclusive-or (XOR), ">> 8" shifts the data right by one byte 
   *  (divides by 256), and "<< 8" shifts the data left by one byte 
   *  (multiplies by 256).
   *
   *  crc = crc_table [(crc >> 8) ^ data] ^ (crc << 8);
   * 
   */
  private ByteBuffer crcChecksum = ByteBuffer.allocate(2);

  
  /**
   *  Constructor:  Builds all of the components of the LOOP packet from
   *  the ByteBuffer data being passed in.
   *
   *  @param packetBuffer  the ByteBuffer that contains the binary LOOP packet data
   */
  public DavisWxParser(ByteBuffer packetBuffer) {
    
    // define temporary byte arrays for reading from the packetBuffer input
    byte[] oneByte  = new byte[1];
    byte[] twoBytes = new byte[2];
    byte[] threeBytes = new byte[3];
    
    // prepare the buffer for reading
    packetBuffer.flip();
    
    // set the loopID field from offset 0-2
    this.loopID.put(packetBuffer.get());
    this.loopID.put(packetBuffer.get());
    this.loopID.put(packetBuffer.get());
    
    // set the barTrend field from offset 3
    this.barTrend.put(packetBuffer.get());
    
    // set the packetType field from offset 4
    this.packetType.put(packetBuffer.get());
    
    // set the nextRecord field from offset 5-6
    this.nextRecord.put(packetBuffer.get());
    this.nextRecord.put(packetBuffer.get());
    
    // set the barometer field from offset 7-8
    this.barometer.put(packetBuffer.get());
    this.barometer.put(packetBuffer.get());
    
    // set the insideTemperature field from offset 9-10
    this.insideTemperature.put(packetBuffer.get());
    this.insideTemperature.put(packetBuffer.get());
    
    // set the insideHumidity field from offset 11
    this.insideHumidity.put(packetBuffer.get());
    
    // set the outsideTemperature field from offset 12-13
    this.outsideTemperature.put(packetBuffer.get());
    this.outsideTemperature.put(packetBuffer.get());
    
    // set the windSpeed field from offset 14
    this.windSpeed.put(packetBuffer.get());
    
    // set the tenMinuteAverageWindSpeed field from offset 15
    this.tenMinuteAverageWindSpeed.put(packetBuffer.get());
    
    // set the windDirection field from offset 16-17
    this.windDirection.put(packetBuffer.get());
    this.windDirection.put(packetBuffer.get());
    
    // set the extraTemperatures field from offset 18-24
    this.extraTemperatures.put(packetBuffer.get());
    this.extraTemperatures.put(packetBuffer.get());
    this.extraTemperatures.put(packetBuffer.get());
    this.extraTemperatures.put(packetBuffer.get());
    this.extraTemperatures.put(packetBuffer.get());
    this.extraTemperatures.put(packetBuffer.get());
    this.extraTemperatures.put(packetBuffer.get());
    
    // set the soilTemperatures field from offset 25-28
    this.soilTemperatures.put(packetBuffer.get());
    this.soilTemperatures.put(packetBuffer.get());
    this.soilTemperatures.put(packetBuffer.get());
    this.soilTemperatures.put(packetBuffer.get());
    
    // set the leafTemperatures field from offset 29-32
    this.leafTemperatures.put(packetBuffer.get());
    this.leafTemperatures.put(packetBuffer.get());
    this.leafTemperatures.put(packetBuffer.get());
    this.leafTemperatures.put(packetBuffer.get());
    
    // set the outsideHumidity field from offset 33
    this.outsideHumidity.put(packetBuffer.get());
    
    // set the extraHumidities field from offset 34-40
    this.extraHumidities.put(packetBuffer.get());
    this.extraHumidities.put(packetBuffer.get());
    this.extraHumidities.put(packetBuffer.get());
    this.extraHumidities.put(packetBuffer.get());
    this.extraHumidities.put(packetBuffer.get());
    this.extraHumidities.put(packetBuffer.get());
    this.extraHumidities.put(packetBuffer.get());
    
    // set the rainRate field from offset 41-42
    this.rainRate.put(packetBuffer.get());
    this.rainRate.put(packetBuffer.get());
    
    // set the uvRadiation field from offset 43
    this.uvRadiation.put(packetBuffer.get());
    
    // set the solarRadiation field from offset 44-45
    this.solarRadiation.put(packetBuffer.get());
    this.solarRadiation.put(packetBuffer.get());
    
    // set the stormRain field from offset 46-47
    this.stormRain.put(packetBuffer.get());
    this.stormRain.put(packetBuffer.get());
    
    // set the currentStormStartDate field from offset 48-49
    this.currentStormStartDate.put(packetBuffer.get());
    this.currentStormStartDate.put(packetBuffer.get());
    
    // set the dailyRain field from offset 50-51
    this.dailyRain.put(packetBuffer.get());
    this.dailyRain.put(packetBuffer.get());
    
    // set the monthlyRain field from offset 52-53
    this.monthlyRain.put(packetBuffer.get());
    this.monthlyRain.put(packetBuffer.get());
    
    // set the yearlyRain field from offset 53-54
    this.yearlyRain.put(packetBuffer.get());
    this.yearlyRain.put(packetBuffer.get());
    
    // set the dailyEvapoTranspiration field from offset 55-56
    this.dailyEvapoTranspiration.put(packetBuffer.get());
    this.dailyEvapoTranspiration.put(packetBuffer.get());
    
    // set the monthlyEvapoTranspiration field from offset 57-58
    this.monthlyEvapoTranspiration.put(packetBuffer.get());
    this.monthlyEvapoTranspiration.put(packetBuffer.get());
    
    // set the yearlyEvapoTranspiration field from offset 59-60
    this.yearlyEvapoTranspiration.put(packetBuffer.get());
    this.yearlyEvapoTranspiration.put(packetBuffer.get());
    
    // set the soilMoistures field from offset 61-62
    this.soilMoistures.put(packetBuffer.get());
    this.soilMoistures.put(packetBuffer.get());
    
    // set the leafWetnesses field from offset 66-69
    this.leafWetnesses.put(packetBuffer.get());
    this.leafWetnesses.put(packetBuffer.get());
    this.leafWetnesses.put(packetBuffer.get());
    this.leafWetnesses.put(packetBuffer.get());
    
    // set the insideAlarm field from offset 70
    this.insideAlarm.put(packetBuffer.get());
    
    // set the rainAlarm field from offset 71
    this.rainAlarm.put(packetBuffer.get());
    
    // set the outsideAlarms field from offset 72-73
    this.outsideAlarms.put(packetBuffer.get());
    this.outsideAlarms.put(packetBuffer.get());
    
    // set the extraTemperatureHumidityAlarms field from offset 74-81
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(packetBuffer.get());
    
    // set the soilLeafAlarms field from offset 82-85
    this.soilLeafAlarms.put(packetBuffer.get());
    this.soilLeafAlarms.put(packetBuffer.get());
    this.soilLeafAlarms.put(packetBuffer.get());
    this.soilLeafAlarms.put(packetBuffer.get());
    
    // set the transmitterBatteryStatus field from offset 86
    this.transmitterBatteryStatus.put(packetBuffer.get());
    
    // set the consoleBatteryVoltage field from offset 87-88
    this.consoleBatteryVoltage.put(packetBuffer.get());
    this.consoleBatteryVoltage.put(packetBuffer.get());
    
    // set the forecastIconValues field from offset 89
    this.forecastIconValues.put(packetBuffer.get());
    
    // set the forecastRuleNumber field from offset 90
    this.forecastRuleNumber.put(packetBuffer.get());
    
    // set the timeOfSunrise field from offset 91-92
    this.timeOfSunrise.put(packetBuffer.get());
    this.timeOfSunrise.put(packetBuffer.get());
    
    // set the timeOfSunset field from offset 93-94
    this.timeOfSunset.put(packetBuffer.get());
    this.timeOfSunset.put(packetBuffer.get());
    
    // set the recordDelimiter field from offset 95-96
    this.recordDelimiter.put(packetBuffer.get());
    
    // set the crcChecksum field from offset 97-98
    this.crcChecksum.put(packetBuffer.get());
    this.crcChecksum.put(packetBuffer.get());
    
  }
   
  // 4C 4F 4F EC 00 1E 00 CC 74 47 03 33 FF 02 05 04 50 00 FF FF FF FF FF FF FF 
  // FF FF FF FF FF FF FF FF 49 FF FF FF FF FF FF FF 00 00 00 00 00 00 00 FF FF 
  // 00 00 01 00 15 06 04 00 33 00 A2 24 FF FF FF FF FF FF FF 00 00 00 00 00 00 
  // 00 00 00 00 00 00 00 00 00 00 00 00 1F 03 06 2D 25 02 77 07 0A 0D EA 68
  
  public static void main(String[] args){
    try {
      File packetFile = new File("/Users/cjones/wxtest.bin");
      FileInputStream fis     = new FileInputStream(packetFile);
      FileChannel fileChannel = fis.getChannel();
      ByteBuffer inBuffer     = ByteBuffer.allocateDirect(8192);
      ByteBuffer packetBuffer = ByteBuffer.allocateDirect(8192);
      
      while( fileChannel.read(inBuffer) != -1 || inBuffer.position() > 0) {
        inBuffer.flip();
        packetBuffer.put(inBuffer.get());
        inBuffer.compact();
      }

      packetBuffer.put(inBuffer.get());
      
      //while (packetBuffer.hasRemaining() ) {
      //  System.out.println(new String(Hex.encodeHex(new byte[]{packetBuffer.get()})));
      //}
      
      DavisWxParser davisWxParser = new DavisWxParser(packetBuffer);
      System.out.println("loopID:                         " + davisWxParser.getLoopID());
      System.out.println("barTrend:                       " + davisWxParser.getBarTrend());
      System.out.println("packetType:                     " + davisWxParser.getPacketType());
      System.out.println("nextRecord:                     " + davisWxParser.getNextRecord());
      System.out.println("barometer:                      " + davisWxParser.getBarometer());
      System.out.println("insideTemperature:              " + davisWxParser.getInsideTemperature());
      System.out.println("insideHumidity:                 " + davisWxParser.getInsideHumidity());
      System.out.println("outsideTemperature:             " + davisWxParser.getOutsideTemperature());
      System.out.println("windSpeed:                      " + davisWxParser.getWindSpeed());
      System.out.println("tenMinuteAverageWindSpeed:      " + davisWxParser.getTenMinuteAverageWindSpeed());
      System.out.println("windDirection:                  " + davisWxParser.getWindDirection());
      System.out.println("extraTemperatures:              " + davisWxParser.getExtraTemperatures());
      System.out.println("soilTemperatures:               " + davisWxParser.getSoilTemperatures());
      System.out.println("leafTemperatures:               " + davisWxParser.getLeafTemperatures());
      System.out.println("outsideHumidity:                " + davisWxParser.getOutsideHumidity());
      System.out.println("extraHumidities:                " + davisWxParser.getExtraHumidities());
      System.out.println("rainRate:                       " + davisWxParser.getRainRate());
      System.out.println("uvRadiation:                    " + davisWxParser.getUvRadiation());
      System.out.println("solarRadiation:                 " + davisWxParser.getSolarRadiation());
      System.out.println("stormRain:                      " + davisWxParser.getStormRain());
      System.out.println("currentStormStartDate:          " + davisWxParser.getCurrentStormStartDate());
      System.out.println("dailyRain:                      " + davisWxParser.getDailyRain());
      System.out.println("monthlyRain:                    " + davisWxParser.getMonthlyRain());
      System.out.println("yearlyRain:                     " + davisWxParser.getYearlyRain());
      System.out.println("dailyEvapoTranspiration:        " + davisWxParser.getDailyEvapoTranspiration());
      System.out.println("monthlyEvapoTranspiration:      " + davisWxParser.getMonthlyEvapoTranspiration());
      System.out.println("yearlyEvapoTranspiration:       " + davisWxParser.getYearlyEvapoTranspiration());
      System.out.println("soilMoistures:                  " + davisWxParser.getSoilMoistures());
      System.out.println("leafWetnesses:                  " + davisWxParser.getLeafWetnesses());
      System.out.println("insideAlarm:                    " + davisWxParser.getInsideAlarm());
      System.out.println("rainAlarm:                      " + davisWxParser.getRainAlarm());
      System.out.println("outsideAlarms:                  " + davisWxParser.getOutsideAlarms());
      System.out.println("extraTemperatureHumidityAlarms: " + davisWxParser.getExtraTemperatureHumidityAlarms());
      System.out.println("soilLeafAlarms:                 " + davisWxParser.getSoilLeafAlarms());
      System.out.println("transmitterBatteryStatus:       " + davisWxParser.getTransmitterBatteryStatus());
      System.out.println("consoleBatteryVoltage:          " + davisWxParser.getConsoleBatteryVoltage());
      System.out.println("forecastIconValues:             " + davisWxParser.getForecastIconValues());
      System.out.println("forecastRuleNumber:             " + davisWxParser.getForecastRuleNumber());
      System.out.println("timeOfSunrise:                  " + davisWxParser.getTimeOfSunrise());
      System.out.println("timeOfSunset:                   " + davisWxParser.getTimeOfSunset());
      System.out.println("recordDelimiter:                " + davisWxParser.getRecordDelimiter());
      System.out.println("crcChecksum:                    " + davisWxParser.getCrcChecksum());
      
    } catch ( java.io.FileNotFoundException fnfe){
      fnfe.printStackTrace();
      
    } catch (java.io.IOException ioe){
      ioe.printStackTrace();
      
    }
  }
  
  /**
   * get the value from the loopID field
   */
  public String getLoopID(){
    this.loopID.flip();
    
    String loopString;
    try {
      loopString =              new String(Hex.encodeHex(new byte[]{this.loopID.get()}));
      loopString = loopString + new String(Hex.encodeHex(new byte[]{this.loopID.get()}));
      loopString = loopString + new String(Hex.encodeHex(new byte[]{this.loopID.get()}));
      
      //loopString = new String(this.loopID.order(ByteOrder.LITTLE_ENDIAN).array(), "US-ASCII");
    } catch (Exception e) {
      e.printStackTrace();
      loopString = "BAD";
    }
    return loopString;
  }
  
  /**
   * get the value from the barTrend field
   */
  public int getBarTrend(){
    this.barTrend.flip();
    return (int) this.barTrend.get();
  }
  
  /**
   * get the value from the packetType field
   */
  public ByteBuffer getPacketType(){
    return this.packetType;
  }
  
  /**
   * get the value from the nextRecord field
   */
  public ByteBuffer getNextRecord(){
    return this.nextRecord;
  }
  
  /**
   * get the value from the barometer field
   */
  public ByteBuffer getBarometer(){
    return this.barometer;
  }
  
  /**
   * get the value from the insideTemperature field
   */
  public ByteBuffer getInsideTemperature(){
    return this.insideTemperature;
  }
  
  /**
   * get the value from the insideHumidity field
   */
  public ByteBuffer getInsideHumidity(){
    return this.insideHumidity;
  }
  
  /**
   * get the value from the outsideTemperature field
   */
  public ByteBuffer getOutsideTemperature(){
    return this.outsideTemperature;
  }
  
  /**
   * get the value from the windSpeed field
   */
  public ByteBuffer getWindSpeed(){
    return this.windSpeed;
  }
  
  /**
   * get the value from the tenMinuteAverageWindspeed field
   */
  public ByteBuffer getTenMinuteAverageWindSpeed(){
    return this.tenMinuteAverageWindSpeed;
  }
  
  /**
   * get the value from the windDirection field
   */
  public ByteBuffer getWindDirection(){
    return this.windDirection;
  }
  
  /**
   * get the value from the extraTemperatures field
   */
  public ByteBuffer getExtraTemperatures(){
    return this.extraTemperatures;
  }
  
  /**
   * get the value from the soilTemperatures field
   */
  public ByteBuffer getSoilTemperatures(){
    return this.soilTemperatures;
  }
  
  /**
   * get the value from the leafTemperatures field
   */
  public ByteBuffer getLeafTemperatures(){
    return this.leafTemperatures;
  }
  
  /**
   * get the value from the outsideHumidity field
   */
  public ByteBuffer getOutsideHumidity(){
    return this.outsideHumidity;
  }
  
  /**
   * get the value from the extraHumidities field
   */
  public ByteBuffer getExtraHumidities(){
    return this.extraHumidities;
  }
  
  /**
   * get the value from the rainRate field
   */
  public ByteBuffer getRainRate(){
    return this.rainRate;
  }
  
  /**
   * get the value from the uvRadiation field
   */
  public ByteBuffer getUvRadiation(){
    return this.uvRadiation;
  }
  
  /**
   * get the value from the solarRadiation field
   */
  public ByteBuffer getSolarRadiation(){
    return this.solarRadiation;
  }
  
  /**
   * get the value from the stormRain field
   */
  public ByteBuffer getStormRain(){
    return this.stormRain;
  }
  
  /**
   * get the value from the currentStormStartDate field
   */
  public ByteBuffer getCurrentStormStartDate(){
    return this.currentStormStartDate;
  }
  
  /**
   * get the value from the dailyRain field
   */
  public ByteBuffer getDailyRain(){
    return this.dailyRain;
  }
  
  /**
   * get the value from the monthlyRain field
   */
  public ByteBuffer getMonthlyRain(){
    return this.monthlyRain;
  }
  
  /**
   * get the value from the yearlyRain field
   */
  public ByteBuffer getYearlyRain(){
    return this.yearlyRain;
  }
  
  /**
   * get the value from the dailyEvapoTranspiration field
   */
  public ByteBuffer getDailyEvapoTranspiration(){
    return this.dailyEvapoTranspiration;
  }
  
  /**
   * get the value from the monthlyEvapoTranspiration field
   */
  public ByteBuffer getMonthlyEvapoTranspiration(){
    return this.monthlyEvapoTranspiration;
  }
  
  /**
   * get the value from the yearlyEvapoTranspiration field
   */
  public ByteBuffer getYearlyEvapoTranspiration(){
    return this.yearlyEvapoTranspiration;
  }
  
  /**
   * get the value from the soilMoistures field
   */
  public ByteBuffer getSoilMoistures(){
    return this.soilMoistures;
  }
  
  /**
   * get the value from the leafWetnesses field
   */
  public ByteBuffer getLeafWetnesses(){
    return this.leafWetnesses;
  }
  
  /**
   * get the value from the insideAlarm field
   */
  public ByteBuffer getInsideAlarm(){
    return this.insideAlarm;
  }
  
  /**
   * get the value from the rainAlarm field
   */
  public ByteBuffer getRainAlarm(){
    return this.rainAlarm;
  }
  
  /**
   * get the value from the outsideAlarm field
   */
  public ByteBuffer getOutsideAlarms(){
    return this.outsideAlarms;
  }
  
  /**
   * get the value from the extraTemperatureHumidityAlarms field
   */
  public ByteBuffer getExtraTemperatureHumidityAlarms(){
    return this.extraTemperatureHumidityAlarms;
  }
  
  /**
   * get the value from the soilLeafAlarms field
   */
  public ByteBuffer getSoilLeafAlarms(){
    return this.soilLeafAlarms;
  }
  
  /**
   * get the value from the transmitterBatteryStatus field
   */
  public ByteBuffer getTransmitterBatteryStatus(){
    return this.transmitterBatteryStatus;
  }
  
  /**
   * get the value from the consoleBatteryStatus field
   */
  public ByteBuffer getConsoleBatteryVoltage(){
    return this.consoleBatteryVoltage;
  }
  
  /**
   * get the value from the forecastIconValues field
   */
  public ByteBuffer getForecastIconValues(){
    return this.forecastIconValues;
  }
  
  /**
   * get the value from the forecastIconRuleNumber field
   */
  public ByteBuffer getForecastRuleNumber(){
    return this.forecastRuleNumber;
  }
  
  /**
   * get the value from the timeOfSunrise field
   */
  public ByteBuffer getTimeOfSunrise(){
    return this.timeOfSunrise;
  }
  
  /**
   * get the value from the timeOfSunset field
   */
  public ByteBuffer getTimeOfSunset(){
    return this.timeOfSunset;
  }
  
  /**
   * get the value from the recordDelimiter field
   */
  public ByteBuffer getRecordDelimiter(){
    return this.recordDelimiter;
  }
  
  /**
   * get the value from the crcChecksum field
   */
  public ByteBuffer getCrcChecksum(){
    return this.crcChecksum;
  }
                                                
}                                               
