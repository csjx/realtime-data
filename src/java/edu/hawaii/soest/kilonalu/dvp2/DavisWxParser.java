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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

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
public class DavisWxParser {
    
  /*
   *  A field that stores the binary LOOP packet input as a ByteBuffer
   */
  private ByteBuffer packetBuffer = ByteBuffer.allocate(99);
  
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
    
    this.packetBuffer = packetBuffer;
    // prepare the buffer for reading
    this.packetBuffer.flip();
    
    // Each of the LOOP packet fields get set upon instantiation of the parser:
    
    // set the loopID field from offset 0-2
    this.loopID.put(this.packetBuffer.get());
    this.loopID.put(this.packetBuffer.get());
    this.loopID.put(this.packetBuffer.get());
    
    // set the barTrend field from offset 3
    this.barTrend.put(this.packetBuffer.get());
    
    // set the packetType field from offset 4
    this.packetType.put(this.packetBuffer.get());
    
    // set the nextRecord field from offset 5-6
    this.nextRecord.put(this.packetBuffer.get());
    this.nextRecord.put(this.packetBuffer.get());
    
    // set the barometer field from offset 7-8
    this.barometer.put(this.packetBuffer.get());
    this.barometer.put(this.packetBuffer.get());
    
    // set the insideTemperature field from offset 9-10
    this.insideTemperature.put(this.packetBuffer.get());
    this.insideTemperature.put(this.packetBuffer.get());
    
    // set the insideHumidity field from offset 11
    this.insideHumidity.put(this.packetBuffer.get());
    
    // set the outsideTemperature field from offset 12-13
    this.outsideTemperature.put(this.packetBuffer.get());
    this.outsideTemperature.put(this.packetBuffer.get());
    
    // set the windSpeed field from offset 14
    this.windSpeed.put(this.packetBuffer.get());
    
    // set the tenMinuteAverageWindSpeed field from offset 15
    this.tenMinuteAverageWindSpeed.put(this.packetBuffer.get());
    
    // set the windDirection field from offset 16-17
    this.windDirection.put(this.packetBuffer.get());
    this.windDirection.put(this.packetBuffer.get());
    
    // set the extraTemperatures field from offset 18-24
    this.extraTemperatures.put(this.packetBuffer.get());
    this.extraTemperatures.put(this.packetBuffer.get());
    this.extraTemperatures.put(this.packetBuffer.get());
    this.extraTemperatures.put(this.packetBuffer.get());
    this.extraTemperatures.put(this.packetBuffer.get());
    this.extraTemperatures.put(this.packetBuffer.get());
    this.extraTemperatures.put(this.packetBuffer.get());
    
    // set the soilTemperatures field from offset 25-28
    this.soilTemperatures.put(this.packetBuffer.get());
    this.soilTemperatures.put(this.packetBuffer.get());
    this.soilTemperatures.put(this.packetBuffer.get());
    this.soilTemperatures.put(this.packetBuffer.get());
    
    // set the leafTemperatures field from offset 29-32
    this.leafTemperatures.put(this.packetBuffer.get());
    this.leafTemperatures.put(this.packetBuffer.get());
    this.leafTemperatures.put(this.packetBuffer.get());
    this.leafTemperatures.put(this.packetBuffer.get());
    
    // set the outsideHumidity field from offset 33
    this.outsideHumidity.put(this.packetBuffer.get());
    
    // set the extraHumidities field from offset 34-40
    this.extraHumidities.put(this.packetBuffer.get());
    this.extraHumidities.put(this.packetBuffer.get());
    this.extraHumidities.put(this.packetBuffer.get());
    this.extraHumidities.put(this.packetBuffer.get());
    this.extraHumidities.put(this.packetBuffer.get());
    this.extraHumidities.put(this.packetBuffer.get());
    this.extraHumidities.put(this.packetBuffer.get());
    
    // set the rainRate field from offset 41-42
    this.rainRate.put(this.packetBuffer.get());
    this.rainRate.put(this.packetBuffer.get());
    
    // set the uvRadiation field from offset 43
    this.uvRadiation.put(this.packetBuffer.get());
    
    // set the solarRadiation field from offset 44-45
    this.solarRadiation.put(this.packetBuffer.get());
    this.solarRadiation.put(this.packetBuffer.get());
    
    // set the stormRain field from offset 46-47
    this.stormRain.put(this.packetBuffer.get());
    this.stormRain.put(this.packetBuffer.get());
    
    // set the currentStormStartDate field from offset 48-49
    this.currentStormStartDate.put(this.packetBuffer.get());
    this.currentStormStartDate.put(this.packetBuffer.get());
    
    // set the dailyRain field from offset 50-51
    this.dailyRain.put(this.packetBuffer.get());
    this.dailyRain.put(this.packetBuffer.get());
    
    // set the monthlyRain field from offset 52-53
    this.monthlyRain.put(this.packetBuffer.get());
    this.monthlyRain.put(this.packetBuffer.get());
    
    // set the yearlyRain field from offset 54-55
    this.yearlyRain.put(this.packetBuffer.get());
    this.yearlyRain.put(this.packetBuffer.get());
    
    // set the dailyEvapoTranspiration field from offset 56-57
    this.dailyEvapoTranspiration.put(this.packetBuffer.get());
    this.dailyEvapoTranspiration.put(this.packetBuffer.get());
    
    // set the monthlyEvapoTranspiration field from offset 58-59
    this.monthlyEvapoTranspiration.put(this.packetBuffer.get());
    this.monthlyEvapoTranspiration.put(this.packetBuffer.get());
    
    // set the yearlyEvapoTranspiration field from offset 60-61
    this.yearlyEvapoTranspiration.put(this.packetBuffer.get());
    this.yearlyEvapoTranspiration.put(this.packetBuffer.get());
    
    // set the soilMoistures field from offset 62-65
    this.soilMoistures.put(this.packetBuffer.get());
    this.soilMoistures.put(this.packetBuffer.get());
    this.soilMoistures.put(this.packetBuffer.get());
    this.soilMoistures.put(this.packetBuffer.get());
    
    // set the leafWetnesses field from offset 66-69
    this.leafWetnesses.put(this.packetBuffer.get());
    this.leafWetnesses.put(this.packetBuffer.get());
    this.leafWetnesses.put(this.packetBuffer.get());
    this.leafWetnesses.put(this.packetBuffer.get());
    
    // set the insideAlarm field from offset 70
    this.insideAlarm.put(this.packetBuffer.get());
    
    // set the rainAlarm field from offset 71
    this.rainAlarm.put(this.packetBuffer.get());
    
    // set the outsideAlarms field from offset 72-73
    this.outsideAlarms.put(this.packetBuffer.get());
    this.outsideAlarms.put(this.packetBuffer.get());
    
    // set the extraTemperatureHumidityAlarms field from offset 74-81
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    this.extraTemperatureHumidityAlarms.put(this.packetBuffer.get());
    
    // set the soilLeafAlarms field from offset 82-85
    this.soilLeafAlarms.put(this.packetBuffer.get());
    this.soilLeafAlarms.put(this.packetBuffer.get());
    this.soilLeafAlarms.put(this.packetBuffer.get());
    this.soilLeafAlarms.put(this.packetBuffer.get());
    
    // set the transmitterBatteryStatus field from offset 86
    this.transmitterBatteryStatus.put(this.packetBuffer.get());
    
    // set the consoleBatteryVoltage field from offset 87-88
    this.consoleBatteryVoltage.put(this.packetBuffer.get());
    this.consoleBatteryVoltage.put(this.packetBuffer.get());
    
    // set the forecastIconValues field from offset 89
    this.forecastIconValues.put(this.packetBuffer.get());
    
    // set the forecastRuleNumber field from offset 90
    this.forecastRuleNumber.put(this.packetBuffer.get());
    
    // set the timeOfSunrise field from offset 91-92
    this.timeOfSunrise.put(this.packetBuffer.get());
    this.timeOfSunrise.put(this.packetBuffer.get());
    
    // set the timeOfSunset field from offset 93-94
    this.timeOfSunset.put(this.packetBuffer.get());
    this.timeOfSunset.put(this.packetBuffer.get());
    
    // set the recordDelimiter field from offset 95-96
    this.recordDelimiter.put(this.packetBuffer.get());
    this.recordDelimiter.put(this.packetBuffer.get());
    
    // set the crcChecksum field from offset 97-98
    this.crcChecksum.put(this.packetBuffer.get());
    this.crcChecksum.put(this.packetBuffer.get());
    
  }
  
  /*
   *  The main entrypoint method.  THis takes a single argument that is the 
   *  path to a file containing a single binary LOOP packet of data.
   */
  public static void main(String[] args){
    
    // Ensure we have a path to the binary file
    if (args.length != 1) {
      System.out.println("Please provide the path to a file containing a binary LOOP packet.");
      System.exit(1);
    } else {
      try {
        // open and read the file
        File packetFile = new File(args[0]);
        FileInputStream fis     = new FileInputStream(packetFile);
        FileChannel fileChannel = fis.getChannel();
        ByteBuffer inBuffer     = ByteBuffer.allocateDirect(8192);
        ByteBuffer packetBuffer = ByteBuffer.allocateDirect(8192);

        while( fileChannel.read(inBuffer) != -1 || inBuffer.position() > 0) {
          inBuffer.flip();
          packetBuffer.put(inBuffer.get());
          inBuffer.compact();
        }
        fileChannel.close();
        fis.close();
        packetBuffer.put(inBuffer.get());
        
        // create an instance of the parser, and report the field contents after parsing
        DavisWxParser davisWxParser = new DavisWxParser(packetBuffer);
        
        System.out.println("loopID:                         " + davisWxParser.getLoopID());
        System.out.println("barTrend:                       " + davisWxParser.getBarTrend());
        System.out.println("barTrendAsString:               " + davisWxParser.getBarTrendAsString());
        System.out.println("packetType:                     " + davisWxParser.getPacketType());
        System.out.println("nextRecord:                     " + davisWxParser.getNextRecord());
        System.out.println("barometer:                      " + davisWxParser.getBarometer());
        System.out.println("insideTemperature:              " + davisWxParser.getInsideTemperature());
        System.out.println("insideHumidity:                 " + davisWxParser.getInsideHumidity());
        System.out.println("outsideTemperature:             " + davisWxParser.getOutsideTemperature());
        System.out.println("windSpeed:                      " + davisWxParser.getWindSpeed());
        System.out.println("tenMinuteAverageWindSpeed:      " + davisWxParser.getTenMinuteAverageWindSpeed());
        System.out.println("windDirection:                  " + davisWxParser.getWindDirection());
        System.out.println("extraTemperatures:              " + Arrays.toString(davisWxParser.getExtraTemperatures()));
        System.out.println("soilTemperatures:               " + Arrays.toString(davisWxParser.getSoilTemperatures()));
        System.out.println("leafTemperatures:               " + Arrays.toString(davisWxParser.getLeafTemperatures()));
        System.out.println("outsideHumidity:                " + davisWxParser.getOutsideHumidity());
        System.out.println("extraHumidities:                " + Arrays.toString(davisWxParser.getExtraHumidities()));
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
        System.out.println("soilMoistures:                  " + Arrays.toString(davisWxParser.getSoilMoistures()));
        System.out.println("leafWetnesses:                  " + Arrays.toString(davisWxParser.getLeafWetnesses()));
        System.out.println("insideAlarm:                    " + davisWxParser.getInsideAlarm());
        System.out.println("rainAlarm:                      " + davisWxParser.getRainAlarm());
        System.out.println("outsideAlarms:                  " + davisWxParser.getOutsideAlarms());
        System.out.println("extraTemperatureHumidityAlarms: " + davisWxParser.getExtraTemperatureHumidityAlarms());
        System.out.println("soilLeafAlarms:                 " + davisWxParser.getSoilLeafAlarms());
        System.out.println("transmitterBatteryStatus:       " + davisWxParser.getTransmitterBatteryStatus());
        System.out.println("consoleBatteryVoltage:          " + davisWxParser.getConsoleBatteryVoltage());
        System.out.println("forecastIconValues:             " + davisWxParser.getForecastAsString());
        System.out.println("forecastRuleNumber:             " + davisWxParser.getForecastRuleNumberAsString());
        System.out.println("timeOfSunrise:                  " + davisWxParser.getTimeOfSunrise());
        System.out.println("timeOfSunset:                   " + davisWxParser.getTimeOfSunset());
        System.out.println("recordDelimiter:                " + davisWxParser.getRecordDelimiterAsHexString());
        System.out.println("crcChecksum:                    " + davisWxParser.getCrcChecksum());

      } catch ( java.io.FileNotFoundException fnfe){
        fnfe.printStackTrace();

      } catch (java.io.IOException ioe){
        ioe.printStackTrace();

      }
      
    }
  }
  
  /**
   * get the value from the loopID field
   *
   * @return loopID - the loopID as a String
   */
  public String getLoopID(){
    this.loopID.flip();
    
    String loopString;
    try {
      loopString =              new String(new byte[]{this.loopID.get()});
      loopString = loopString + new String(new byte[]{this.loopID.get()});
      loopString = loopString + new String(new byte[]{this.loopID.get()});
      
      //loopString = new String(this.loopID.order(ByteOrder.LITTLE_ENDIAN).array(), "US-ASCII");
    } catch (Exception e) {
      e.printStackTrace();
      loopString = "BAD";
    }
    return loopString;
  }
  
  /**
   * get the value from the barTrend field
   *
   * @return barTrend - the barTrend as an integer
   */
  public int getBarTrend(){
    this.barTrend.flip();
    return (int) this.barTrend.get();
  }
  
  /**
   * get the interpreted value from the barTrend field as a String
   *
   * @return barTrend - the barTrend as a String
   */
  public String getBarTrendAsString(){
    this.barTrend.flip();
    String barTrendString = "Trend not available";
    int barTrend = this.barTrend.get();
    
    if ( barTrend == -60 ) {
      barTrendString = "Falling Rapidly";
    } else if ( barTrend == -20 ) {
      barTrendString = "Falling Slowly";
    } else if ( barTrend ==   0 ) {
      barTrendString = "Steady";
    } else if ( barTrend ==  20 ) {
      barTrendString = "Rising Slowly";
    } else if ( barTrend ==  60 ) {
      barTrendString = "Rising Rapidly";
    } else if ( barTrend ==  80 ) {
      barTrendString = "P";
    }
    return barTrendString;
  }
  
  /**
   * get the value from the packetType field
   *
   * @return packetType - the packetType as an integer
   */
  public int getPacketType(){
    this.packetType.flip();
    return (int) this.packetType.get();
  }
  
  /**
   * get the value from the nextRecord field
   *
   * @return nextRecord - the nextRecord as an integer
   */
  public int getNextRecord(){
    this.nextRecord.flip();
    return (int) this.nextRecord.order(ByteOrder.LITTLE_ENDIAN).getShort();
  }
  
  /**
   * get the value from the barometer field
   *
   * @return barometer - the barometer as a double
   */
  public double getBarometer(){
    this.barometer.flip();
    return (double) (this.barometer.order(ByteOrder.LITTLE_ENDIAN).getShort())/1000;
  }
  
  /**
   * get the value from the insideTemperature field
   *
   * @return insideTemperature - the insideTemperature as a double
   */
  public double getInsideTemperature(){
    this.insideTemperature.flip();
    return (double) (this.insideTemperature.order(ByteOrder.LITTLE_ENDIAN).getShort())/10;
  }
  
  /**
   * get the value from the insideHumidity field
   *
   * @return insideHumidity - the insideHumidity as an integer
   */
  public int getInsideHumidity(){
    this.insideHumidity.flip();
    return (int) (this.insideHumidity.get());
  }
  
  /**
   * get the value from the outsideTemperature field
   *
   * @return outsideTemperature - the outsideTemperature as a double
   */
  public double getOutsideTemperature(){
    this.outsideTemperature.flip();
    return (double) (this.outsideTemperature.order(ByteOrder.LITTLE_ENDIAN).getShort())/10;
  }
  
  /**
   * get the value from the windSpeed field
   *
   * @return windSpeed - the windSpeed as an integer
   */
  public int getWindSpeed(){
    this.windSpeed.flip();
    return (int) (this.windSpeed.get());
  }
  
  /**
   * get the value from the tenMinuteAverageWindSpeed field
   *
   * @return tenMinuteAverageWindSpeed - the tenMinuteAverageWindSpeed as an integer
   */
  public int getTenMinuteAverageWindSpeed(){
    this.tenMinuteAverageWindSpeed.flip();
    return (int) (this.tenMinuteAverageWindSpeed.get());
  }
  
  /**
   * get the value from the windDirection field
   *
   * @return windDirection - the windDirection as an integer
   */
  public int getWindDirection(){
    this.windDirection.flip();
    return (int) (this.windDirection.get());
  }
  
  /**
   * get the value from the extraTemperatures field
   *
   * @return extraTemperatures - the extraTemperatures as an double array
   */
  public double[] getExtraTemperatures(){
    this.extraTemperatures.flip();
    
    // add each of the temperature values to a double array
    double[] extraTemperatures = new double[7];
    for (int i = 0; i < extraTemperatures.length; i++ ) {
      extraTemperatures[i] = (double) (this.extraTemperatures.get() - 90.0 )/10;
    }
    return extraTemperatures;
  }
  
  /**
   * get the value from the soilTemperatures field
   *
   * @return soilTemperatures - the soilTemperatures as an double array
   */
  public double[] getSoilTemperatures(){
    this.soilTemperatures.flip();
    
    // add each of the temperature values to a double array
    double[] soilTemperatures = new double[4];
    for (int i = 0; i < soilTemperatures.length; i++ ) {
      soilTemperatures[i] = (double) (this.soilTemperatures.get() - 90.0 )/10;
    }
    return soilTemperatures;
  }
  
  /**
   * get the value from the leafTemperatures field
   *
   * @return leafTemperatures - the leafTemperatures as an double array
   */
  public double[] getLeafTemperatures(){
    this.leafTemperatures.flip();
    
    // add each of the temperature values to a double array
    double[] leafTemperatures = new double[4];
    for (int i = 0; i < leafTemperatures.length; i++ ) {
      leafTemperatures[i] = (double) (this.leafTemperatures.get() - 90.0 )/10;
    }
    return leafTemperatures;
  }
  
  /**
   * get the value from the outsideHumidity field
   *
   * @return outsideHumidity - the outsideHumidity as an integer
   */
  public int getOutsideHumidity(){
    this.outsideHumidity.flip();
    return (int) (this.outsideHumidity.get());
  }
  
  /**
   * get the value from the extraHumidities field
   *
   * @return extraHumidities - the extraHumidities as an double array
   */
  public int[] getExtraHumidities(){
    this.extraHumidities.flip();
    
    // add each of the humidity values to a double array
    int[] extraHumidities = new int[7];
    for (int i = 0; i < extraHumidities.length; i++ ) {
      extraHumidities[i] = (int) this.extraHumidities.get();
    }
    return extraHumidities;
  }
  
  /**
   * get the value from the rainRate field
   *
   * @return rainRate - the rainRate as a double
   */
  public double getRainRate(){
    this.rainRate.flip();
    return (double) (this.rainRate.order(ByteOrder.LITTLE_ENDIAN).getShort())/100;
  }
  
  /**
   * get the value from the uvRadiation field
   *
   * @return uvRadiation - the uvRadiation as an integer
   */
  public int getUvRadiation(){
    this.uvRadiation.flip();
    return (int) (this.uvRadiation.get());
  }
  
  /**
   * get the value from the solarRadiation field
   *
   * @return solarRadiation - the solarRadiation as a double
   */
  public double getSolarRadiation(){
    this.solarRadiation.flip();
    return (double) (this.solarRadiation.order(ByteOrder.LITTLE_ENDIAN).getShort());
  }
  
  /**
   * get the value from the stormRain field
   *
   * @return stormRain - the stormRain as an integer
   */
  public double getStormRain(){
    this.stormRain.flip();
    return (double) (this.stormRain.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the currentStormStartDate field
   *
   * @return currentStormStartDate - the currentStormStartDate as a String
   */
  public String getCurrentStormStartDate(){
    this.currentStormStartDate.flip();
    int currentStormStartDate = (int)
      this.currentStormStartDate.order(ByteOrder.LITTLE_ENDIAN).getShort();
      
    int month = currentStormStartDate;
        month = (month >> 11); // clear bits 0-11
    String monthString = (new Integer(month)).toString();
    
    int day   = currentStormStartDate;
        day   = (day << 21) >> 27; // clear bits 16-32, then 0-11
        String dayString = (new Integer(day)).toString();
          
    int year  = currentStormStartDate;
        year  = (year << 25) >> 25; // clear bits 7-32
    int centuryYear = 2000 + (int) year;
          String centuryYearString = (new Integer(centuryYear)).toString();
    return( monthString + "-" + dayString + "-" + centuryYearString );
  }
  
  /**
   * get the value from the dailyRain field
   *
   * @return dailyRain - the dailyRain as a double
   */
  public double getDailyRain(){
    this.dailyRain.flip();
    return (int) (this.dailyRain.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the monthlyRain field
   *
   * @return monthlyRain - the monthlyRain as a double
   */
  public double getMonthlyRain(){
    this.monthlyRain.flip();
    return (double) (this.monthlyRain.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the yearlyRain field
   *
   * @return yearlyRain - the yearlyRain as a double
   */
  public double getYearlyRain(){
    this.yearlyRain.flip();
    return (double) (this.yearlyRain.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the dailyEvapoTranspiration field
   *
   * @return dailyEvapoTranspiration - the dailyEvapoTranspiration as a double
   */
  public double getDailyEvapoTranspiration(){
    this.dailyEvapoTranspiration.flip();
    return (double) (this.dailyEvapoTranspiration.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the monthlyEvapoTranspiration field
   *
   * @return monthlyEvapoTranspiration - the monthlyEvapoTranspiration as a double
   */
  public double getMonthlyEvapoTranspiration(){
    this.monthlyEvapoTranspiration.flip();
    return (double) (this.monthlyEvapoTranspiration.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the yearlyEvapoTranspiration field
   *
   * @return yearlyEvapoTranspiration - the yearlyEvapoTranspiration as a double
   */
  public double getYearlyEvapoTranspiration(){
    this.yearlyEvapoTranspiration.flip();
    return (double) (this.yearlyEvapoTranspiration.order(ByteOrder.LITTLE_ENDIAN).getShort()/100);
  }
  
  /**
   * get the value from the soilMoistures field
   *
   * @return soilTemperatures - the soilMoistures values as a double array
   */
  public double[] getSoilMoistures(){
    this.soilMoistures.flip();
    
    // add each of the soilMoistures values to a double array
    double[] soilMoistures = new double[4];
    for (int i = 0; i < soilMoistures.length; i++ ) {
      soilMoistures[i] = (double) this.soilMoistures.get();
    }
    return soilMoistures;
  }
  
  /**
   * get the value from the leafWetnesses field
   *
   * @return leafWetnesses - the leafWetnesses values as a double array
   */
  public double[] getLeafWetnesses(){
    this.leafWetnesses.flip();
    
    // add each of the leafWetnesses values to a double array
    double[] leafWetnesses = new double[4];
    for (int i = 0; i < leafWetnesses.length; i++ ) {
      leafWetnesses[i] = (double) this.leafWetnesses.get();
    }
    return leafWetnesses;
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
   *
   * @return transmitterBatteryStatus - the transmitterBatteryStatus as an integer
   */
  public int getTransmitterBatteryStatus(){
    this.transmitterBatteryStatus.flip();
    return (int) (this.transmitterBatteryStatus.get());
  }
  
  /**
   * get the value from the consoleBatteryVoltage field
   *
   * @return consoleBatteryVoltage - the consoleBatteryVoltage as a double
   */
  public double getConsoleBatteryVoltage(){
    this.consoleBatteryVoltage.flip();
    double consoleBatteryVoltage = (double)
      this.consoleBatteryVoltage.order(ByteOrder.LITTLE_ENDIAN).getShort();
    
    consoleBatteryVoltage = ((consoleBatteryVoltage * 300.0f)/512.0f)/100.0f; // from the instrument guide
    return consoleBatteryVoltage;
  }
  
  /**
   * get the value from the forecastIconValues field
   *
   * @return forecastIconValueString - the forecastIconValue as a String
   */
  public String getForecastAsString(){
    this.forecastIconValues.flip();
    byte forecastIconValue = this.forecastIconValues.get();
    String forecastIconValueString = "Forecast not available";
    
    // from the instrument guide:
    if(  forecastIconValue == 0x08 ) {
      forecastIconValueString = "Mostly Clear";
      
    } else if ( forecastIconValue == 0x06 ) {
      forecastIconValueString = "Partially Cloudy";
      
    } else if ( forecastIconValue == 0x02 ) {
      forecastIconValueString = "Mostly Cloudy";
      
    } else if ( forecastIconValue == 0x03 ) {
      forecastIconValueString = "Mostly Cloudy, Rain within 12 hours";
      
    } else if ( forecastIconValue == 0x12 ) {
      forecastIconValueString = "Mostly Cloudy, Snow within 12 hours";
      
    } else if ( forecastIconValue == 0x13 ) {
      forecastIconValueString = "Mostly Cloudy, Rain or Snow within 12 hours";
      
    } else if ( forecastIconValue == 0x07 ) {
      forecastIconValueString = "Partially Cloudy, Rain within 12 hours";
      
    } else if ( forecastIconValue == 0x16 ) {
      forecastIconValueString = "Partially Cloudy, Snow within 12 hours";
      
    } else if ( forecastIconValue == 0x17 ) {
      forecastIconValueString = "Partially Cloudy, Rain or Snow within 12 hours";
      
    }
    return forecastIconValueString;
    
  }
  
  /**
   * get the value from the forecastRuleNumber field. The forecast rule
   * numbers are mapped to specific forecast strings set by Davis Instruments.
   * However, these aren't documented in the Serial Protocol Guide.  The strings
   * were taken from the PERL module documentation at 
   * http://www.cpan.org/authors/id/S/ST/STSANDER/vanprod-doc.html
   *
   * @return forecastRuleNumberAsString - the forecastRuleNumberAsString as a string
   */
  public String getForecastRuleNumberAsString(){
    this.forecastRuleNumber.flip();
    int forecastRuleNumber = (int) this.forecastRuleNumber.get();
    System.out.println("RULE NUMBER:" + forecastRuleNumber);
    
    String forecastRuleNumberAsString = "No forecast rule string available";
    Map<Integer, String> forecastRuleMap = new HashMap<Integer,String>();
    
    forecastRuleMap.put(  0, "Mostly clear and cooler.");
    forecastRuleMap.put(  1, "Mostly clear with little temperature change.");
    forecastRuleMap.put(  2, "Mostly clear for 12 hours with little temperature change.");
    forecastRuleMap.put(  3, "Mostly clear for 12 to 24 hours and cooler.");
    forecastRuleMap.put(  4, "Mostly clear with little temperature change.");
    forecastRuleMap.put(  5, "Partly cloudy and cooler.");
    forecastRuleMap.put(  6, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(  7, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(  8, "Mostly clear and warmer.");
    forecastRuleMap.put(  9, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 10, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 11, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 12, "Increasing clouds and warmer. Precipitation possible within 24 to 48 hours.");
    forecastRuleMap.put( 13, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 14, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 15, "Increasing clouds with little temperature change. Precipitation possible within 24 hours.");
    forecastRuleMap.put( 16, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 17, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 18, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 19, "Increasing clouds with little temperature change. Precipitation possible within 12 hours.");
    forecastRuleMap.put( 20, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 21, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 22, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 23, "Increasing clouds and warmer. Precipitation possible within 24 hours.");
    forecastRuleMap.put( 24, "Mostly clear and warmer. Increasing winds.");
    forecastRuleMap.put( 25, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 26, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 27, "Increasing clouds and warmer. Precipitation possible within 12 hours. Increasing winds.");
    forecastRuleMap.put( 28, "Mostly clear and warmer. Increasing winds.");
    forecastRuleMap.put( 29, "Increasing clouds and warmer.");
    forecastRuleMap.put( 30, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 31, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 32, "Increasing clouds and warmer. Precipitation possible within 12 hours. Increasing winds.");
    forecastRuleMap.put( 33, "Mostly clear and warmer. Increasing winds.");
    forecastRuleMap.put( 34, "Increasing clouds and warmer.");
    forecastRuleMap.put( 35, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 36, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 37, "Increasing clouds and warmer. Precipitation possible within 12 hours. Increasing winds.");
    forecastRuleMap.put( 38, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 39, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 40, "Mostly clear and warmer. Precipitation possible within 48 hours.");
    forecastRuleMap.put( 41, "Mostly clear and warmer.");
    forecastRuleMap.put( 42, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 43, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 44, "Increasing clouds with little temperature change. Precipitation possible within 24 to 48 hours.");
    forecastRuleMap.put( 45, "Increasing clouds with little temperature change.");
    forecastRuleMap.put( 46, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 47, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 48, "Increasing clouds and warmer. Precipitation possible within 12 to 24 hours.");
    forecastRuleMap.put( 49, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 50, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 51, "Increasing clouds and warmer. Precipitation possible within 12 to 24 hours. Windy.");
    forecastRuleMap.put( 52, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 53, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 54, "Increasing clouds and warmer. Precipitation possible within 12 to 24 hours. Windy.");
    forecastRuleMap.put( 55, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 56, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 57, "Increasing clouds and warmer. Precipitation possible within 6 to 12 hours.");
    forecastRuleMap.put( 58, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 59, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 60, "Increasing clouds and warmer. Precipitation possible within 6 to 12 hours. Windy.");
    forecastRuleMap.put( 61, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 62, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 63, "Increasing clouds and warmer. Precipitation possible within 12 to 24 hours. Windy.");
    forecastRuleMap.put( 64, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 65, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 66, "Increasing clouds and warmer. Precipitation possible within 12 hours.");
    forecastRuleMap.put( 67, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 68, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 69, "Increasing clouds and warmer. Precipitation likley.");
    forecastRuleMap.put( 70, "Clearing and cooler. Precipitation ending within 6 hours.");
    forecastRuleMap.put( 71, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 72, "Clearing and cooler. Precipitation ending within 6 hours.");
    forecastRuleMap.put( 73, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 74, "Clearing and cooler. Precipitation ending within 6 hours.");
    forecastRuleMap.put( 75, "Partly cloudy and cooler.");
    forecastRuleMap.put( 76, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 77, "Mostly clear and cooler.");
    forecastRuleMap.put( 78, "Clearing and cooler. Precipitation ending within 6 hours.");
    forecastRuleMap.put( 79, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 80, "Clearing and cooler. Precipitation ending within 6 hours.");
    forecastRuleMap.put( 81, "Mostly clear and cooler.");
    forecastRuleMap.put( 82, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 83, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 84, "Increasing clouds with little temperature change. Precipitation possible within 24 hours.");
    forecastRuleMap.put( 85, "Mostly cloudy and cooler. Precipitation continuing.");
    forecastRuleMap.put( 86, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 87, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 88, "Mostly cloudy and cooler. Precipitation likely.");
    forecastRuleMap.put( 89, "Mostly cloudy with little temperature change. Precipitation continuing.");
    forecastRuleMap.put( 90, "Mostly cloudy with little temperature change. Precipitation likely.");
    forecastRuleMap.put( 91, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 92, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 93, "Increasing clouds and cooler. Precipitation possible and windy within 6 hours.");
    forecastRuleMap.put( 94, "Increasing clouds with little temperature change. Precipitation possible and windy within 6 hours.");
    forecastRuleMap.put( 95, "Mostly cloudy and cooler. Precipitation continuing. Increasing winds.");
    forecastRuleMap.put( 96, "Partly cloudy with little temperature change.");
    forecastRuleMap.put( 97, "Mostly clear with little temperature change.");
    forecastRuleMap.put( 98, "Mostly cloudy and cooler. Precipitation likely. Increasing winds.");
    forecastRuleMap.put( 99, "Mostly cloudy with little temperature change. Precipitation continuing. Increasing winds.");
    forecastRuleMap.put(100, "Mostly cloudy with little temperature change. Precipitation likely. Increasing winds.");
    forecastRuleMap.put(101, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(102, "Mostly clear with little temperature change.");
    forecastRuleMap.put(103, "Increasing clouds and cooler. Precipitation possible within 12 to 24 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(104, "Increasing clouds with little temperature change. Precipitation possible within 12 to 24 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(105, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(106, "Mostly clear with little temperature change.");
    forecastRuleMap.put(107, "Increasing clouds and cooler. Precipitation possible within 6 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(108, "Increasing clouds with little temperature change. Precipitation possible within 6 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(109, "Mostly cloudy and cooler. Precipitation ending within 12 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(110, "Mostly cloudy and cooler. Possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(111, "Mostly cloudy with little temperature change. Precipitation ending within 12 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(112, "Mostly cloudy with little temperature change. Possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(113, "Mostly cloudy and cooler. Precipitation ending within 12 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(114, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(115, "Mostly clear with little temperature change.");
    forecastRuleMap.put(116, "Mostly cloudy and cooler. Precipitation possible within 24 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(117, "Mostly cloudy with little temperature change. Precipitation ending within 12 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(118, "Mostly cloudy with little temperature change. Precipitation possible within 24 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(119, "Clearing, cooler and windy. Precipitation ending within 6 hours.");
    forecastRuleMap.put(120, "Clearing, cooler and windy.");
    forecastRuleMap.put(121, "Mostly cloudy and cooler. Precipitation ending within 6 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(122, "Mostly cloudy and cooler. Windy with possible wind shift o the W, NW, or N.");
    forecastRuleMap.put(123, "Clearing, cooler and windy.");
    forecastRuleMap.put(124, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(125, "Mostly clear with little temperature change.");
    forecastRuleMap.put(126, "Mostly cloudy with little temperature change. Precipitation possible within 12 hours. Windy.");
    forecastRuleMap.put(127, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(128, "Mostly clear with little temperature change.");
    forecastRuleMap.put(129, "Increasing clouds and cooler. Precipitation possible within 12 hours, possibly heavy at times. Windy.");
    forecastRuleMap.put(130, "Mostly cloudy and cooler. Precipitation ending within 6 hours. Windy.");
    forecastRuleMap.put(131, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(132, "Mostly clear with little temperature change.");
    forecastRuleMap.put(133, "Mostly cloudy and cooler. Precipitation possible within 12 hours. Windy.");
    forecastRuleMap.put(134, "Mostly cloudy and cooler. Precipitation ending in 12 to 24 hours.");
    forecastRuleMap.put(135, "Mostly cloudy and cooler.");
    forecastRuleMap.put(136, "Mostly cloudy and cooler. Precipitation continuing, possible heavy at times. Windy.");
    forecastRuleMap.put(137, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(138, "Mostly clear with little temperature change.");
    forecastRuleMap.put(139, "Mostly cloudy and cooler. Precipitation possible within 6 to 12 hours. Windy.");
    forecastRuleMap.put(140, "Mostly cloudy with little temperature change. Precipitation continuing, possibly heavy at times. Windy.");
    forecastRuleMap.put(141, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(142, "Mostly clear with little temperature change.");
    forecastRuleMap.put(143, "Mostly cloudy with little temperature change. Precipitation possible within 6 to 12 hours. Windy.");
    forecastRuleMap.put(144, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(145, "Mostly clear with little temperature change.");
    forecastRuleMap.put(146, "Increasing clouds with little temperature change. Precipitation possible within 12 hours, possibly heavy at times. Windy.");
    forecastRuleMap.put(147, "Mostly cloudy and cooler. Windy.");
    forecastRuleMap.put(148, "Mostly cloudy and cooler. Precipitation continuing, possibly heavy at times. Windy.");
    forecastRuleMap.put(149, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(150, "Mostly clear with little temperature change.");
    forecastRuleMap.put(151, "Mostly cloudy and cooler. Precipitation likely, possibly heavy at times. Windy.");
    forecastRuleMap.put(152, "Mostly cloudy with little temperature change. Precipitation continuing, possibly heavy at times. Windy.");
    forecastRuleMap.put(153, "Mostly cloudy with little temperature change. Precipitation likely, possibly heavy at times. Windy.");
    forecastRuleMap.put(154, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(155, "Mostly clear with little temperature change.");
    forecastRuleMap.put(156, "Increasing clouds and cooler. Precipitation possible within 6 hours. Windy.");
    forecastRuleMap.put(157, "Increasing clouds with little temperature change. Precipitation possible within 6 hours. Windy");
    forecastRuleMap.put(158, "Increasing clouds and cooler. Precipitation continuing. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(159, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(160, "Mostly clear with little temperature change.");
    forecastRuleMap.put(161, "Mostly cloudy and cooler. Precipitation likely. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(162, "Mostly cloudy with little temperature change. Precipitation continuing. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(163, "Mostly cloudy with little temperature change. Precipitation likely. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(164, "Increasing clouds and cooler. Precipitation possible within 6 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(165, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(166, "Mostly clear with little temperature change.");
    forecastRuleMap.put(167, "Increasing clouds and cooler. Precipitation possible within 6 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(168, "Increasing clouds with little temperature change. Precipitation possible within 6 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(169, "Increasing clouds with little temperature change. Precipitation possible within 6 hours possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(170, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(171, "Mostly clear with little temperature change.");
    forecastRuleMap.put(172, "Increasing clouds and cooler. Precipitation possible within 6 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(173, "Increasing clouds with little temperature change. Precipitation possible within 6 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(174, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(175, "Mostly clear with little temperature change.");
    forecastRuleMap.put(176, "Increasing clouds and cooler. Precipitation possible within 12 to 24 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(177, "Increasing clouds with little temperature change. Precipitation possible within 12 to 24 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(178, "Mostly cloudy and cooler. Precipitation possibly heavy at times and ending within 12 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(179, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(180, "Mostly clear with little temperature change.");
    forecastRuleMap.put(181, "Mostly cloudy and cooler. Precipitation possible within 6 to 12 hours, possibly heavy at times. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(182, "Mostly cloudy with little temperature change. Precipitation ending within 12 hours. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(183, "Mostly cloudy with little temperature change. Precipitation possible within 6 to 12 hours, possibly heavy at times. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(184, "Mostly cloudy and cooler. Precipitation continuing.");
    forecastRuleMap.put(185, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(186, "Mostly clear with little temperature change.");
    forecastRuleMap.put(187, "Mostly cloudy and cooler. Precipitation likely. Windy with possible wind shift to the W, NW, or N.");
    forecastRuleMap.put(188, "Mostly cloudy with little temperature change. Precipitation continuing.");
    forecastRuleMap.put(189, "Mostly cloudy with little temperature change. Precipitation likely.");
    forecastRuleMap.put(190, "Partly cloudy with little temperature change.");
    forecastRuleMap.put(191, "Mostly clear with little temperature change.");
    forecastRuleMap.put(192, "Mostly cloudy and cooler. Precipitation possible within 12 hours, possibly heavy at times. Windy.");
    forecastRuleMap.put(193, "FORECAST REQUIRES 3 HOURS OF RECENT DATA");
    forecastRuleMap.put(194, "Mostly clear and cooler.");
    forecastRuleMap.put(195, "Mostly clear and cooler.");
    forecastRuleMap.put(196, "Mostly clear and cooler.");
    
    forecastRuleNumberAsString = forecastRuleMap.get(forecastRuleNumber);
    return forecastRuleNumberAsString;               
  }
  
  /**
   * get the value from the timeOfSunrise field
   *
   * @return timeOfSunrise - the timeOfSunrise value as a String
   */
  public String getTimeOfSunrise(){
    this.timeOfSunrise.flip();
    short timeOfSunrise = this.timeOfSunrise.order(ByteOrder.LITTLE_ENDIAN).getShort();
    double timeOfSunriseAsDouble = (double) timeOfSunrise;
    timeOfSunriseAsDouble        = timeOfSunriseAsDouble/100;
    
    String hour                  = "";
    String minute                = "";
    String timeOfSunriseString   = "";
    
    // use the modulo to get the fraction and integer of the time
    double fraction = timeOfSunriseAsDouble % 1;
    double integral = timeOfSunriseAsDouble - fraction;
           fraction = fraction * 100;
    int integralInt = (new Double(integral)).intValue();
    
    //convert the exponent to a minute string
    hour = String.format("%02d", (Object) integralInt);
    
    //convert the exponent to a minute string
    minute = String.format("%02d", (Object) Math.round(fraction));
    
    timeOfSunriseString = hour + ":" + minute;
    return timeOfSunriseString;
  }
  
  /**
   * get the value from the timeOfSunset field
   *
   * @return timeOfSunset - the timeOfSunset value as a String
   */
  public String getTimeOfSunset(){
    this.timeOfSunset.flip();
    short timeOfSunset = this.timeOfSunset.order(ByteOrder.LITTLE_ENDIAN).getShort();
    double timeOfSunsetAsDouble = (double) timeOfSunset;
    timeOfSunsetAsDouble        = timeOfSunsetAsDouble/100;
    
    String hour                 = "";
    String minute               = "";
    String timeOfSunsetString   = "";
    
    // use the modulo to get the fraction and integer of the time
    double fraction = timeOfSunsetAsDouble % 1;
    double integral = timeOfSunsetAsDouble - fraction;
           fraction = fraction * 100;
    int integralInt = (new Double(integral)).intValue();
    
    //convert the exponent to a minute string
    hour = String.format("%02d", (Object) integralInt);
    
    //convert the exponent to a minute string
    minute = String.format("%02d", (Object) Math.round(fraction));
    
    timeOfSunsetString = hour + ":" + minute;
    return timeOfSunsetString;
  }
  
  /**
   * get the value from the recordDelimiter field
   *
   * @return recordDelimiter - the recordDelimiter as a Hex encoded String
   */
  public String getRecordDelimiterAsHexString(){
    this.recordDelimiter.flip();
    String delim1 = new String(Hex.encodeHex(new byte[]{this.recordDelimiter.get()}));
    String delim2 = new String(Hex.encodeHex(new byte[]{this.recordDelimiter.get()}));
    
    String recordDelimiter = delim1 + delim2;
    return recordDelimiter;
  }
  
  /**
   * get the value from the crcChecksum field
   */
  public ByteBuffer getCrcChecksum(){
    return this.crcChecksum;
  }
                                                
}                                               
