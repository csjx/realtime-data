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
  
  /**
   *  A field that stores the instrument deployment notes key as a String
   */
  public final String DEPLOYMENT_NOTES = "Notes";
  
  /*
   *  A field that stores the main battery voltage as a String
   */
  private String mainBatteryVoltage;
  
  /**
   *  A field that stores the main battery voltage key as a String
   */
  public final String MAIN_BATTERY_VOLTAGE = "vbatt";
  
  /*
   *  A field that stores the lithium battery voltage as a String
   */
  private String lithiumBatteryVoltage;
  
  /**
   *  A field that stores the lithium battery voltage key as a String
   */
  public final String LITHIUM_BATTERY_VOLTAGE = "vlith";
  
  /*
   *  A field that stores the operating current as a String
   */
  private String operatingCurrent;
  
  /**
   *  A field that stores the operating current key as a String
   */
  public final String OPERATING_CURRENT = "ioper";
  
  /*
   *  A field that stores the pump current as a String
   */
  private String pumpCurrent;
  
  /**
   *  A field that stores the pump current key as a String
   */
  public final String PUMP_CURRENT = "ipump";
  
  /*
   *  A field that stores the channel 0 and 1 external voltage currents as a String
   */
  private String channels01ExternalCurrent;
  
  /**
   *  A field that stores the channel 0 and 1 external voltage currents key as a String
   */
  public final String CHANNELS_01_EXTERNAL_CURRENT = "iext01";
  
  /*
   *  A field that stores the channel 2 and 3 external voltage currents as a String
   */
  private String channels23ExternalCurrent;
  
  /**
   *  A field that stores the channel 2 and 3 external voltage currents key as a String
   */
  public final String CHANNELS_23_EXTERNAL_CURRENT = "iext23";
  
  /*
   *  A field that stores the logging status as a String
   */
  private String loggingStatus;
  
  /**
   *  A field that stores the logging status key as a String
   */
  public final String LOGGING_STATUS = "status";
  
  /*
   *  A field that stores the number of scans to average as a String
   */
  private String numberOfScansToAverage;
  
  /**
   *  A field that stores the number of scans to average key as a String
   */
  public final String NUMBER_OF_SCANS_TO_AVERAGE = "number of scans to average";
  
  /*
   *  A field that stores the number of samples as a String
   */
  private String numberOfSamples;
  
  /**
   *  A field that stores the number of samples key as a String
   */
  public final String NUMBER_OF_SAMPLES = "samples";
  
  /*
   *  A field that stores the number of available samples as a String
   */
  private String numberOfAvailableSamples;
  
  /**
   *  A field that stores the number of available samples key as a String
   */
  public final String NUMBER_OF_AVAILABLE_SAMPLES = "free";
  
  /*
   *  A field that stores the sample interval as a String
   */
  private String sampleInterval;
  
  /**
   *  A field that stores the sample interval key as a String
   */
  public final String SAMPLE_INTERVAL = "sample interval";
  
  /*
   *  A field that stores the number of measurements per sample as a String
   */
  private String measurementsPerSample;
  
  /**
   *  A field that stores the number of measurements per sample key as a String
   */
  public final String MEASUREMENTS_PER_SAMPLE = "number of measurements per sample";
  
  /*
   *  A field that stores the transmit real-time state as a String
   */
  private String transmitRealtime;
  
  /**
   *  A field that stores the transmit real-time state key as a String
   */
  public final String TRANSMIT_REALTIME = "transmit real-time";
  
  /*
   *  A field that stores the number of casts as a String
   */
  private String numberOfCasts;
  
  /**
   *  A field that stores the number of casts key as a String
   */
  public final String NUMBER_OF_CASTS = "casts";
  
  /*
   *  A field that stores the minimum conductivity frequency as a String
   */
  private String minimumConductivityFrequency;
  
  /**
   *  A field that stores the minimum conductivity frequency key as a String
   */
  public final String MINIMUM_CONDUCTIVITY_FREQUENCY = "minimum cond freq";
  
  /*
   *  A field that stores the pump delay as a String
   */
  private String pumpDelay;
  
  /**
   *  A field that stores the pump delay key as a String
   */
  public final String PUMP_DELAY = "pump delay";
  
  /*
   *  A field that stores the automatic logging state as a String
   */
  private String automaticLogging;
  
  /**
   *  A field that stores the automatic logging state key as a String
   */
  public final String AUTOMATIC_LOGGING = "autorun";
  
  /*
   *  A field that stores the ignore magnetic switch state as a String
   */
  private String ignoreMagneticSwitch;
  
  /**
   *  A field that stores the ignore magnetic switch state key as a String
   */
  public final String IGNORE_MAGNETIC_SWITCH = "ignore magnetic switch";
  
  /*
   *  A field that stores the battery type as a String
   */
  private String batteryType;
  
  /**
   *  A field that stores the battery type key as a String
   */
  public final String BATTERY_TYPE = "battery type";
  
  /*
   *  A field that stores the battery cutoff as a String
   */
  private String batteryCutoff;
  
  /**
   *  A field that stores the battery cutoff key as a String
   */
  public final String BATTERY_CUTOFF = "battery cutoff";
  
  /*
   *  A field that stores the pressure sensor type as a String
   */
  private String pressureSensorType;
  
  /**
   *  A field that stores the pressure sensor type key as a String
   */
  public final String PRESSURE_SENSOR_TYPE = "pressure sensor";
  
  /*
   *  A field that stores the pressure sensor range as a String
   */
  private String pressureSensorRange;
  
  /**
   *  A field that stores the pressure sensor range key as a String
   */
  public final String PRESSURE_SENSOR_RANGE = "range";
  
  /*
   *  A field that stores the SBE38 temperature sensor state as a String
   */
  private String sbe38TemperatureSensor;
  
  /**
   *  A field that stores the SBE38 temperature sensor state key as a String
   */
  public final String SBE38_TEMPERATURE_SENSOR = "SBE 38";
  
  /*
   *  A field that stores the gas tension device state as a String
   */
  private String gasTensionDevice;
  
  /**
   *  A field that stores the gas tension device state key as a String
   */
  public final String GAS_TENSION_DEVICE = "Gas Tension Device";
  
  /*
   *  A field that stores the external voltage channel 0 state as a String
   */
  private String externalVoltageChannelZero;
  
  /**
   *  A field that stores the external voltage channel 0 state key as a String
   */
  public final String EXTERNAL_VOLTAGE_CHANNEL_ZERO = "Ext Volt 0";
  
  /*
   *  A field that stores the external voltage channel 1 state as a String
   */
  private String externalVoltageChannelOne;
  
  /**
   *  A field that stores the external voltage channel 1 state key as a String
   */
  public final String EXTERNAL_VOLTAGE_CHANNEL_ONE = "Ext Volt 1";
  
  /*
   *  A field that stores the external voltage channel 2 state as a String
   */
  private String externalVoltageChannelTwo;
  
  /**
   *  A field that stores the external voltage channel 2 state key as a String
   */
  public final String EXTERNAL_VOLTAGE_CHANNEL_TWO = "Ext Volt 2";
  
  /*
   *  A field that stores the external voltage channel 3 state as a String
   */
  private String externalVoltageChannelThree;
  
  /**
   *  A field that stores the external voltage channel 3 state key as a String
   */
  public final String EXTERNAL_VOLTAGE_CHANNEL_THREE = "Ext Volt 3";
  
  /*
   *  A field that stores the echo commands state as a String
   */
  private String echoCommands;
  
  /**
   *  A field that stores the echo commands state key as a String
   */
  public final String ECHO_COMMANDS = "echo commands";
  
  /*
   *  A field that stores the output format as a String
   */
  private String outputFormat;
  
  /**
   *  A field that stores the output format key as a String
   */
  public final String OUTPUT_FORMAT = "output format";
  
  /*
   *  A field that stores the temperature calibration date as a String
   */
  private String temperatureCalibrationDate;
  
  /**
   *  A field that stores the temperature calibration date key as a String
   */
  public final String TEMPERATURE_CALIBRATION_DATE = "temperature";
  
  /*
   *  A field that stores the temperature coefficient TA0 as a String
   */
  private String temperatureCoefficientTA0;
  
  /**
   *  A field that stores the temperature coefficient TA0 key as a String
   */
  public final String TEMPERATURE_COEFFICIENT_TA0 = "TA0";
  
  /*
   *  A field that stores the temperature coefficient TA1 as a String
   */
  private String temperatureCoefficientTA1;
  
  /**
   *  A field that stores the temperature coefficient TA1 key as a String
   */
  public final String TEMPERATURE_COEFFICIENT_TA1 = "TA1";
  
  /*
   *  A field that stores the temperature coefficient TA2 as a String
   */
  private String temperatureCoefficientTA2;
  
  /**
   *  A field that stores the temperature coefficient TA2 key as a String
   */
  public final String TEMPERATURE_COEFFICIENT_TA2 = "TA2";
  
  /*
   *  A field that stores the temperature coefficient TA3 as a String
   */
  private String temperatureCoefficientTA3;
  
  /**
   *  A field that stores the temperature coefficient TA3 key as a String
   */
  public final String TEMPERATURE_COEFFICIENT_TA3 = "TA3";
  
  /*
   *  A field that stores the temperature offset coefficient as a String
   */
  private String temperatureOffsetCoefficient;
  
  /**
   *  A field that stores the temperature offset coefficient key as a String
   */
  public final String TEMPERATURE_OFFSET_COEFFICIENT = "TOFFSET";
  
  /*
   *  A field that stores the conductivity calibration date as a String
   */
  private String conductivityCalibrationDate;
  
  /**
   *  A field that stores the conductivity calibration date key as a String
   */
  public final String CONDUCTIVITY_CALIBRATION_DATE = "conductivity";
  
  /*
   *  A field that stores the conductivity coefficient G as a String
   */
  private String conductivityCoefficientG;
  
  /**
   *  A field that stores the conductivity coefficient G key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_G = "G";
  
  /*
   *  A field that stores the conductivity coefficient H as a String
   */
  private String conductivityCoefficientH;
  
  /**
   *  A field that stores the conductivity coefficient H key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_H = "H";
  
  /*
   *  A field that stores the conductivity coefficient I as a String
   */
  private String conductivityCoefficientI;
  
  /**
   *  A field that stores the conductivity coefficient I key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_I = "I";
  
  /*
   *  A field that stores the conductivity coefficient J as a String
   */
  private String conductivityCoefficientJ;
  
  /**
   *  A field that stores the conductivity coefficient J key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_J = "J";
  
  /*
   *  A field that stores the conductivity coefficient CF0 as a String
   */
  private String conductivityCoefficientCF0;
  
  /**
   *  A field that stores the conductivity coefficient CF0 key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_CF0 = "CF0";
  
  /*
   *  A field that stores the conductivity coefficient CPCOR as a String
   */
  private String conductivityCoefficientCPCOR;
  
  /**
   *  A field that stores the conductivity coefficient CPCOR key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_CPCOR = "CPCOR";
  
  /*
   *  A field that stores the conductivity coefficient CTCOR as a String
   */
  private String conductivityCoefficientCTCOR;
  
  /**
   *  A field that stores the conductivity coefficient CTCOR key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_CTCOR = "CTCOR";
  
  /*
   *  A field that stores the conductivity coefficient CSLOPE as a String
   */
  private String conductivityCoefficientCSLOPE;
  
  /**
   *  A field that stores the conductivity coefficient CSLOPE key as a String
   */
  public final String CONDUCTIVITY_COEFFICIENT_CSLOPE = "CSLOPE";
  
  /*
   *  A field that stores the pressure serial number as a String
   */
  private String pressureSerialNumber;
  
  /**
   *  A field that stores the pressure serial number key as a String
   */
  public final String PRESSURE_SERIAL_NUMBER = "pressure S/N";
  
  /*
   *  A field that stores the pressure coefficient PA0 as a String
   */
  private String pressureCoefficientPA0;
  
  /**
   *  A field that stores the pressure coefficient PA0 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PA0 = "PA0";
  
  /*
   *  A field that stores the pressure coefficient PA1 as a String
   */
  private String pressureCoefficientPA1;
  
  /**
   *  A field that stores the pressure coefficient PA1 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PA1 = "PA1";
  
  /*
   *  A field that stores the pressure coefficient PA2 as a String
   */
  private String pressureCoefficientPA2;
  
  /**
   *  A field that stores the pressure coefficient PA2 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PA2 = "PA2";
  
  /*
   *  A field that stores the pressure coefficient PTCA0 as a String
   */
  private String pressureCoefficientPTCA0;
  
  /**
   *  A field that stores the pressure coefficient PTCA0 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTCA0 = "PTCA0";
  
  /*
   *  A field that stores the pressure coefficient PTCA1 as a String
   */
  private String pressureCoefficientPTCA1;
  
  /**
   *  A field that stores the pressure coefficient PTCA1 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTCA1 = "PTCA1";
  
  /*
   *  A field that stores the pressure coefficient PTCA2 as a String
   */
  private String pressureCoefficientPTCA2;
  
  /**
   *  A field that stores the pressure coefficient PTCA2 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTCA2 = "PTCA2";
  
  /*
   *  A field that stores the pressure coefficient PTCB0 as a String
   */
  private String pressureCoefficientPTCB0;
  
  /**
   *  A field that stores the pressure coefficient PTCB0 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTCB0 = "PTCB0";
  
  /*
   *  A field that stores the pressure coefficient PTCB1 as a String
   */
  private String pressureCoefficientPTCB1;
  
  /**
   *  A field that stores the pressure coefficient PTCB1 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTCB1 = "PTCB1";
  
  /*
   *  A field that stores the pressure coefficient PTCB2 as a String
   */
  private String pressureCoefficientPTCB2;
  
  /**
   *  A field that stores the pressure coefficient PTCB2 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTCB2 = "PTCB2";
  
  /*
   *  A field that stores the pressure coefficient PTEMPA0 as a String
   */
  private String pressureCoefficientPTEMPA0;
  
  /**
   *  A field that stores the pressure coefficient PTEMPA0 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTEMPA0 = "PTEMPA0";
  
  /*
   *  A field that stores the pressure coefficient PTEMPA1 as a String
   */
  private String pressureCoefficientPTEMPA1;
  
  /**
   *  A field that stores the pressure coefficient PTEMPA1 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTEMPA1 = "PTEMPA1";
  
  /*
   *  A field that stores the pressure coefficient PTEMPA2 as a String
   */
  private String pressureCoefficientPTEMPA2;
  
  /**
   *  A field that stores the pressure coefficient PTEMPA2 key as a String
   */
  public final String PRESSURE_COEFFICIENT_PTEMPA2 = "PTEMPA2";
  
  /*
   *  A field that stores the pressure offset coefficient as a String
   */
  private String pressureOffsetCoefficient;
  
  /**
   *  A field that stores the pressure offset coefficient key as a String
   */
  public final String PRESSURE_OFFSET_COEFFICIENT = "POFFSET";

  // TODO: add voltage offset fields.  What are they used for?
  // * volt 0: offset = -4.678210e-02, slope = 1.248624e+00
  // * volt 1: offset = -4.696105e-02, slope = 1.248782e+00
  // * volt 2: offset = -4.683263e-02, slope = 1.249537e+00
  // * volt 3: offset = -4.670842e-02, slope = 1.249841e+00
  // * EXTFREQSF = 1.000012e+00
   
  /*
   * A boolean field indicating if a pressure sensor is present on the instrument
   */
  private boolean hasPressure = false;
  
  /*
   * A boolean field indicating if the pressure sensor is a strain gauge sensor
   */
  private boolean hasStrainGaugePressure = false;
  
  /*
   * A boolean field indicating if external voltage channel zero is sampling
   */
  private boolean hasVoltageChannelZero = false;
  
  /*
   * A boolean field indicating if external voltage channel one is sampling
   */
  private boolean hasVoltageChannelOne = false;
  
  /*
   * A boolean field indicating if external voltage channel two is sampling
   */
  private boolean hasVoltageChannelTwo = false;
  
  /*
   * A boolean field indicating if external voltage channel three is sampling
   */
  private boolean hasVoltageChannelThree = false;
  
  /*
   * A boolean field indicating if there is an SBE38 temperature sensor
   */
  private boolean hasSBE38TemperatureSensor = false;
  
  /*
   * A boolean field indicating if there is a gas tension device
   */
  private boolean hasGasTensionDevice = false;
  
}                                               
