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
package edu.hawaii.soest.kilonalu.isus;

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
  
}
