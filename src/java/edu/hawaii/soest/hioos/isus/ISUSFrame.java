/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a Satlantic ISUS V3 data sample
 *             from a binary StorX data file.
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
package edu.hawaii.soest.hioos.isus;

import java.io.UnsupportedEncodingException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents a single (full) binary frame from a Satlantic 
 * ISUS V3 nitrate concentration instrument.  The class represents both light
 * and dark frames, and provides access to the individual fields of the binary
 * data sample as described in the ISUS V3 Operation Manual.
 */
public class ISUSFrame {
  
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(ISUSFrame.class);
  
  /* A ISUS frame size in bytes as an integer */
  private final int ISUS_FRAME_SIZE = 8192;
  
  /* The date format for the timestamp applied to a ISUS frame (Julian day)*/
  private static final SimpleDateFormat FRAME_DATE_FORMAT = 
    new SimpleDateFormat("yyyyDDDHHmmss");
 
  /* The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
  
  /* A ISUS frame as a ByteBuffer */
  private ByteBuffer isusFrame = ByteBuffer.allocate(ISUS_FRAME_SIZE);
  
  /*  AS10  The frame header or synchronization string starts with 'SAT' 
   *  for a Satlantic instrument, followed by three characters identifying 
   * the frame type. The last four characters are the instrument serial number.
   */
  private ByteBuffer header = ByteBuffer.allocate(10);

  /* BS4  The date field denotes the date at the time of the sample, using 
   * the year and Julian day. The format is YYYYDDD.
   */
  private ByteBuffer sampleDate = ByteBuffer.allocate(4);

  /*  BD8  The time field gives the GMT/UTC time of the sample in decimal 
   * hours of the day.
   */
  private ByteBuffer sampleTime = ByteBuffer.allocate(8);

  /*  BF4  The Nitrate concentration as calculated by the ISUS in μMol/L */
  private ByteBuffer nitrogenConcentration = ByteBuffer.allocate(4);

  /*  BF4  The auxiliary 1 fitting result of the ISUS is reported. */
  private ByteBuffer auxConcentration1 = ByteBuffer.allocate(4);

  /*  BF4  The auxiliary 2 fitting result of the ISUS is reported. */
  private ByteBuffer auxConcentration2 = ByteBuffer.allocate(4);

  /*  BF4  The auxiliary 3 fitting result of the ISUS is reported. */
  private ByteBuffer auxConcentration3 = ByteBuffer.allocate(4);

  /*  BF4  The Root Mean Square Error of the ISUS’ concentration calculation 
   *  is given, in ASCII frames to 6 decimal places. 
   */
  private ByteBuffer rmsError = ByteBuffer.allocate(4);

  /*  BF4  The temperature inside the ISUS housing is given indegrees Celsius; 
   * in ASCII frames to 2 decimal places. */
  private ByteBuffer insideTemperature = ByteBuffer.allocate(4);

  /*  BF4  The temperature of the spectrometer is given in degreesCelsius */
  private ByteBuffer spectrometerTemperature = ByteBuffer.allocate(4);

  /*  BF4  The temperature of the lamp is given in degrees Celsius */
  private ByteBuffer lampTemperature = ByteBuffer.allocate(4);

  /*  BU4  The lamp on-time of the current data acquisition in seconds. */
  private ByteBuffer lampTime = ByteBuffer.allocate(4);

  /*  BF4  The humidity inside the instrument, given in percent. 
   *  Increasing values of humidity indicate a slow leak. */
  private ByteBuffer humidity = ByteBuffer.allocate(4);

  /*  BF4  The voltage of the lamp power supply. */
  private ByteBuffer lampVoltage12 = ByteBuffer.allocate(4);

  /*  BF4  The voltage of the internal analog power supply. */
  private ByteBuffer internalPowerVoltage5 = ByteBuffer.allocate(4);

  /*  BF4  The voltage of the main internal supply. */
  private ByteBuffer mainPowerVoltage = ByteBuffer.allocate(4);

  /*  BF4  The average Reference Channel measurement during the sample time */
  private ByteBuffer referenceAverage = ByteBuffer.allocate(4);

  /*  BF4  The variance of the Reference Channel measurements */
  private ByteBuffer referenceVariance = ByteBuffer.allocate(4);

  /*  BF4  The Sea-Water Dark calculation in spectrometer counts. */
  private ByteBuffer seaWaterDarkCounts = ByteBuffer.allocate(4);

  /*  BF4  The average value of all spectrometer channels */
  private ByteBuffer averageWavelength = ByteBuffer.allocate(4);

  /*  BU2  The spectrometer counts of the channel wavelengths (256 total) */
  private ByteBuffer channelWavelengths = ByteBuffer.allocate(2 * 256);

  /*  BU1  Binary frames only: A check sum validates binary frames. */
  private ByteBuffer checksum = ByteBuffer.allocate(1);

  /* A ISUS frame timestamp as a ByteBuffer*/
  private ByteBuffer timestamp = ByteBuffer.allocate(7);
  
  public ISUSFrame(ByteBuffer isusFrame) {
    
    this.isusFrame = isusFrame;
    
    // parse each of the fields from the incoming byte buffer
    byte[] twoBytes   = new byte[2];
    byte[] sixBytes   = new byte[6];
    byte[] sevenBytes = new byte[7];
    byte[] fiveTwelveBytes = new byte[512];
    
    try {
      
      // set the header field
      this.isusFrame.get(sixBytes);
      this.header.put(sixBytes);
      this.isusFrame.get(twoBytes);
      this.header.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.header.put(twoBytes);

      // set the sample date field
      this.isusFrame.get(twoBytes);
      this.sampleDate.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.sampleDate.put(twoBytes);

      // set the sample time field
      this.isusFrame.get(sixBytes);
      this.sampleTime.put(sixBytes);
      this.isusFrame.get(twoBytes);
      this.sampleTime.put(twoBytes);

      // set the nitrogen concentration field
      this.isusFrame.get(twoBytes);
      this.nitrogenConcentration.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.nitrogenConcentration.put(twoBytes);

      // set the first auxillary concentration field
      this.isusFrame.get(twoBytes);
      this.auxConcentration1.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.auxConcentration1.put(twoBytes);

      // set the second auxillary concentration field
      this.isusFrame.get(twoBytes);
      this.auxConcentration2.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.auxConcentration2.put(twoBytes);

      // set the third auxillary concentration field
      this.isusFrame.get(twoBytes);
      this.auxConcentration3.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.auxConcentration3.put(twoBytes);

      // set the root mean square error field
      this.isusFrame.get(twoBytes);
      this.rmsError.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.rmsError.put(twoBytes);

      // set the inside temperature field
      this.isusFrame.get(twoBytes);
      this.insideTemperature.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.insideTemperature.put(twoBytes);

      // set the spectrometer temperature field
      this.isusFrame.get(twoBytes);
      this.spectrometerTemperature.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.spectrometerTemperature.put(twoBytes);

      // set the lamp temperature field
      this.isusFrame.get(twoBytes);
      this.lampTemperature.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.lampTemperature.put(twoBytes);

      // set the lamp time field
      this.isusFrame.get(twoBytes);
      this.lampTime.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.lampTime.put(twoBytes);

      // set the humdity field
      this.isusFrame.get(twoBytes);
      this.humidity.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.humidity.put(twoBytes);

      // set the lamp voltage12 field
      this.isusFrame.get(twoBytes);
      this.lampVoltage12.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.lampVoltage12.put(twoBytes);

      // set the internal power voltage5 field
      this.isusFrame.get(twoBytes);
      this.internalPowerVoltage5.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.internalPowerVoltage5.put(twoBytes);

      // set the main power voltage field
      this.isusFrame.get(twoBytes);
      this.mainPowerVoltage.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.mainPowerVoltage.put(twoBytes);

      // set the reference average field
      this.isusFrame.get(twoBytes);
      this.referenceAverage.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.referenceAverage.put(twoBytes);

      // set the reference variance field
      this.isusFrame.get(twoBytes);
      this.referenceVariance.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.referenceVariance.put(twoBytes);

      // set the sea water dark counts field
      this.isusFrame.get(twoBytes);
      this.seaWaterDarkCounts.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.seaWaterDarkCounts.put(twoBytes);

      // set the average wavelength field
      this.isusFrame.get(twoBytes);
      this.averageWavelength.put(twoBytes);
      this.isusFrame.get(twoBytes);
      this.averageWavelength.put(twoBytes);

      // set the channel wavelengths field
      this.isusFrame.get(fiveTwelveBytes);
      this.channelWavelengths.put(fiveTwelveBytes);

      // set the checksum field
      this.checksum.put(this.isusFrame.get());

      // set the timestamp field
      this.isusFrame.get(sixBytes);
      this.timestamp.put(sixBytes);
      this.timestamp.put(this.isusFrame.get());
      
      logger.debug(this.isusFrame.toString());
      System.out.println("-------------------------------------------------------");
      
    } catch ( BufferUnderflowException bue ) {
      
      bue.printStackTrace();
      
    }
  }
  
  /* 
   * AS10 The frame header or synchronization string starts with SAT 
   * for a Satlantic instrument, followed by threecharacters identifying 
   * the frame type. The last four characters are the instrument serial number.
   */
  public String getHeader() {
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
   * @return frameSerialNumber - the serial number as a String
   */
  public String getSerialNumber() {

    try {
      
      byte[] fourBytes = new byte[4];
      this.header.flip().position(6);
      this.header.get(fourBytes);
      this.header.flip();
      return new String(fourBytes, "US-ASCII");
      
    } catch (UnsupportedEncodingException uee) {
      logger.debug("The string encoding was not recognized: " +
                   uee.getMessage());
      return null;
    }


  }
  
  /* 
   * BS4 The date field denotes the date at the time of the sample, using 
   * the year and Julian day. The format is YYYYDDD. 
   */
  public byte[] getSampleDate() {
    return this.sampleDate.array();
  }

  /* 
   * BD8 The time field gives the GMT/UTC time of the sample in decimal 
   * hours of the day. 
   */
  public byte[] getSampleTime() {
    return this.sampleTime.array();
  }

  /*  in ASCII frames to 2 decimal places. */
  public float getNitrogenConcentration() {
    return this.nitrogenConcentration.getFloat();
  }

  /* BF4 The first auxiliary fitting result of the ISUS is reported. */
  public float getAuxConcentration1() {
    return this.auxConcentration1.getFloat();
  }

  /* BF4 The second auxiliary fitting result of the ISUS is reported. */
  public float getAuxConcentration2() {
    return this.auxConcentration2.getFloat();
  }

  /* BF4 The first auxiliary fitting result of the ISUS is reported. */
  public float getAuxConcentration3() {
    return this.auxConcentration3.getFloat();
  }

  /* BF4 The Root Mean Square Error of the ISUS’ concentration calculation is given, in ASCII frames to 6 decimal places. */
  public float getRmsError() {
    return this.rmsError.getFloat();
  }

  /* The temperature inside the housing in degrees Celcius. */
  public float getInsideTemperature() {
    return this.insideTemperature.getFloat();
  }

  /* The temperature of the spectrometer in degrees Celcius. */
  public float getSpectrometerTemperature() {
    return this.spectrometerTemperature.getFloat();
  }

  /* The temperature of the lamp in degrees Celcius. */
  public float getLampTemperature() {
    return this.lampTemperature.getFloat();
  }

  /* BU4 The lamp on-time of the current data acquisition inseconds. */
  public float getLampTime() {
    return this.lampTime.getFloat();
  }

  /* BF4 The humidity inside the instrument, given in percent. Increasing values of humidity indicate a slow leak. */
  public float getHumidity() {
    return this.humidity.getFloat();
  }

  /* BF4 The voltage of the lamp power supply. */
  public float getLampVoltage12() {
    return this.lampVoltage12.getFloat();
  }

  /* BF4 The voltage of the internal analog power supply. */
  public float getInternalPowerVoltage5() {
    return this.internalPowerVoltage5.getFloat();
  }

  /* BF4 The voltage of the main internal supply. */
  public float getMainPowerVoltage() {
    return this.mainPowerVoltage.getFloat();
  }

  /* BF4 The average Reference Channel measurement during thesample time, in ASCII mode to 2 decimal places. */
  public float getReferenceAverage() {
    return this.referenceAverage.getFloat();
  }

  /* BF4 The variance of the Reference Channel measurements, inASCII mode to 2 decimal places. */
  public float getReferenceVariance() {
    return this.referenceVariance.getFloat();
  }

  /* BF4 An AF formatted field representing the Sea-Water Darkcalculation (to 2 decimal places), in spectrometer counts. */
  public float getSeaWaterDarkCounts() {
    return this.seaWaterDarkCounts.getFloat();
  }

  /* BF4 An AF formatted field representing the average value of all spectrometer channels, to 2 decimal places. */
  public float getAverageWavelength() {
    return this.averageWavelength.getFloat();
  }

  /* BU2 The counts of the given channel wavelength of thespectrometer. */
  public int getChannelWavelengthCounts(int wavelength) {
    
    int position = (wavelength * 2) - 2;
    short counts = this.channelWavelengths.getShort(position);
    
    return new Short(counts).intValue();
  }

  /* BU1 Binary frames only: A check sum validates binaryframes. Satlantic’s software rejects invalid frames. */
  public float getChecksum() {
    return this.checksum.getFloat();
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
