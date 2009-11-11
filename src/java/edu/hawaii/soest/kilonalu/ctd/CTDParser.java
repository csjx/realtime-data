/**
 *  Copyright: 2009 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a data sample of data produced by
 *             a Seabird Seacat 19plus CTD profiler as described in
 *            the SBE 19plus SEACAT Profiler User's Manual 
 *            (Manual Version #010, 01/02/03 ).  The parser is intended to parse
 *            one of the four OUTPUTFORMATs described in the manual.
 *
 *   Authors: Christopher Jones
 *
 * $HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/ctd/CTDParser.java $
 * $LastChangedDate: 2009-06-23 19:29:51 -0600 (Tue, 23 Jun 2009) $
 * $LastChangedBy: cjones $
 * $LastChangedRevision: 404 $
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
import java.math.BigInteger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents a sample of data produced by
 *  a Seabird Seacat 19plus CTD Profiler.  The parser is intended to parse one
 *  of the four OUTPUTFORMATs described in the manual.  Therefore, the parser
 *  will handle both Hex and Decimal data formats, as well as data from both 
 *  profile and moored modes. This class includes fields for each of the stated
 *   fields in the sample format documentation, along with getter and setter
 *  methods for accessing those fields.
 */
public class CTDParser {
    
  /*
   * The default log configuration file location
   */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /*
   * The log configuration file location
   */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /**
   * The Logger instance used to log system messages 
   */
  static Logger logger = Logger.getLogger(CTDParser.class);
  
  /*
   *  A field that stores the data file string input as a String
   */
  private String dataString = "";
  
  /*
   *  A field that stores the metadata string input as a String.  This will be
   *  the results of the DS and DCAL commands
   */
  private String metadataString = "";
  
  /*
   *  A field that stores the data file string input as a String. This will be
   *  the actually data observations in the given OUTPUTFORMAT (Hex, decimal, etc.)
   */
  private String observationsString = "";
  
  /*
   *  A field that stores the metadata values as a SortedMap.  Each metadata key
   *  corresponds to the value given by the instrument.
   */
  private SortedMap<String,String> metadataValuesMap;
  
  /*
   *  A field that stores the data values (lines) as a SortedMap.  Each string 
   *  represents the data observation values for a given CTD scan.
   */
  private SortedMap<Integer,String> dataValuesMap;
  
  /*
   *  A field that stores the converted data values as a RealMatrix.  Each row 
   *  represents the data observation values for a given CTD scan.
   */
  private RealMatrix convertedDataValuesMatrix;
  
  /*
   *  A field that stores the ordered data variable names as a List.
   *  Each name represents a single data variable taken in a given scan.
   */
  private List<String> dataVariableNames;
  
  /*
   *  A field that stores the ordered data variable units as a List.
   *  Each unit represents a single data variable unit taken in a given scan.
   */
  private List<String> dataVariableUnits;
  
  /*
   *  A field that stores the ordered data variable offsets as a List. This
   *  vector is used to parse raw Hex strings of data from the CTD.
   *  Each offset represents the ending Hex character for the variable.  The
   *  offsets can be seen as:
   *
   *       6     12     18   22   26   30   34   38
   *  A1B2C3 D4E5F6 A7B8C9 D0E1 F2A3 B4C5 D6E7 F8A9
   *
   *  where the first variable (temperature) is taken from characters 1-6, the 
   *  second (conductivity) is 7-12, etc.
   
   */
  private List<Integer> dataVariableOffsets;
  
  /*
   *  A field used to keep track of the current data variable offset
   */
   private int currentOffset;
   
  /**
   *  A field that stores the metadata field delimiter as a String
   */
  public final String METADATA_FIELD_DELIMITER = ",";
  
  /**
   *  A field that stores the primary name/value pair delimiter as a String
   */
  public final String PRIMARY_PAIR_DELIMITER = ":";
  
  /**
   *  A field that stores the primary name/value pair delimiter as a String
   */
  public final String SECONDARY_PAIR_DELIMITER = "=";

  /*
   *  A field that stores the CTD sampling mode as a String
   */
  private String samplingMode;
  
  /**
   *  A field that stores the CTD sampling mode key as a String
   */
  public final String SAMPLING_MODE = "mode";

  /*
   *  A field that stores the CTD first sample time as a String
   */
  private String firstSampleTime;
  
  /**
   *  A field that stores the CTD first sample time key as a String
   */
  public final String FIRST_SAMPLE_TIME = "First Sample Time";

  /**
   *  A field that stores the default record delimiter in a String.
   */
  public final String DEFAULT_RECORD_DELIMITER = "\r\n";
  
  /*
   *  A field that stores the record delimiter in a String.
   */
  private String recordDelimiter = DEFAULT_RECORD_DELIMITER;
  
  /**
   *  A field that stores the default metadata delimiter pattern in a String. 
   *  Usually "\\*END\\*\r\n"
   */
  public final String DEFAULT_METADATA_DELIMITER = "\\*END\\*\r\n";
  
  /*
   *  A field that stores the metadata delimiter pattern in a String. 
   *  Usually "\\*END\\*\r\n"
   */
  private String metadataDelimiter = DEFAULT_METADATA_DELIMITER;
  
  /**
   *  A field that stores the file name as a String
   */
  private String fileName;
  
  /**
   *  A field that stores the file name key as a String
   */
  public final String FILE_NAME = "FileName";
  
  /*
   *  A field that stores the temperature sensor serial number as a String
   */
  private String temperatureSerialNumber;
  
  /**
   *  A field that stores the temperature sensor serial number key as a String
   */
  public final String TEMPERATURE_SERIAL_NUMBER = "Temperature SN";
  
  /*
   *  A field that stores the conductivity sensor serial number as a String
   */
  private String conductivitySerialNumber;
  
  /**
   *  A field that stores the conductivity sensor serial number key as a String
   */
  public final String CONDUCTIVITY_SERIAL_NUMBER = "Conductivity SN";
  
  /*
   *  A field that stores the System UpLoad Time as a String
   */
  private String systemUpLoadTime;
  
  /**
   *  A field that stores the System UpLoad Time key as a String
   */
  public final String SYSTEM_UPLOAD_TIME = "System UpLoad Time";
  
  /*
   *  A field that stores the cruise information as a String
   */
  private String cruiseInformation;
  
  /**
   *  A field that stores the cruise information key as a String
   */
  public final String CRUISE_INFORMATION = "Cruise";
  
  /*
   *  A field that stores the station information as a String
   */
  private String stationInformation;
  
  /**
   *  A field that stores the station information key as a String
   */
  public final String STATION_INFORMATION = "Station";
  
  /*
   *  A field that stores the ship information as a String
   */
  private String shipInformation;
  
  /**
   *  A field that stores the ship information key as a String
   */
  public final String SHIP_INFORMATION = "Ship";
  
  /*
   *  A field that stores the chief scientist information as a String
   */
  private String chiefScientist;
  
  /**
   *  A field that stores the chief scientist information key as a String
   */
  public final String CHIEF_SCIENTIST = "Chief_Scientist";
  
  /*
   *  A field that stores the organization information as a String
   */
  private String organization;
  
  /**
   *  A field that stores the organization information key as a String
   */
  public final String ORGANIZATION = "Organization";
  
  /*
   *  A field that stores the area of operation information as a String
   */
  private String areaOfOperation;
  
  /**
   *  A field that stores the area of operation information key as a String
   */
  public final String AREA_OF_OPERATION = "Area_of_Operation";
  
  /*
   *  A field that stores the instrument package information as a String
   */
  private String instrumentPackage;
  
  /**
   *  A field that stores the instrument package information key as a String
   */
  public final String INSTRUMENT_PACKAGE = "Package";
  
  /*
   *  A field that stores the mooring number as a String
   */
  private String mooringNumber;
  
  /**
   *  A field that stores the mooring number key as a String
   */
  public final String MOORING_NUMBER = "Mooring_Number";
  
  /*
   *  A field that stores the instrument latitude as a String
   */
  private String instrumentLatitude;
  
  /**
   *  A field that stores the instrument latitude key as a String
   */
  public final String INSTRUMENT_LATITUDE = "Latitude";
  
  /*
   *  A field that stores the instrument longitude as a String
   */
  private String instrumentLongitude;
  
  /**
   *  A field that stores the instrument longitude key as a String
   */
  public final String INSTRUMENT_LONGITUDE = "Longitude";
  
  /*
   *  A field that stores the instrument depth sounding as a String
   */
  private String depthSounding;
  
  /**
   *  A field that stores the instrument depth sounding key as a String
   */
  public final String DEPTH_SOUNDING = "Sounding";
  
  /*
   *  A field that stores the instrument profile number as a String
   */
  private String profileNumber;
  
  /**
   *  A field that stores the instrument profile number key as a String
   */
  public final String PROFILE_NUMBER = "Profile_Number";
  
  /*
   *  A field that stores the instrument profile direction as a String
   */
  private String profileDirection;
  
  /**
   *  A field that stores the instrument profile direction key as a String
   */
  public final String PROFILE_DIRECTION = "Profile_Direction";
  
  /*
   *  A field that stores the instrument deployment notes as a String
   */
  private String deploymentNotes;
  
}                                               
