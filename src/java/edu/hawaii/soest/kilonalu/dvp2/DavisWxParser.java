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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.HashMap;

/**
 *  A class that represents a single Rev "B" sample of data produced by
 *  an Davis Scientific Vantage Pro 2 Weather station in the
 *  default LOOP format.  This class includes fields for each of the stated
 *  byte fields in the LOOP format documentation, along with getter and setter
 *  methods for accessing those fields.
 */
public class DavisWxParser {
    
  /**
   *  A field that stores the LOOP packet byte count
   */
  private int packetByteCount = 0;
  
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
    
  }
   
}
