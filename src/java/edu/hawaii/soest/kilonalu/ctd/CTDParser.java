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

  /*
   * A field that stores the raw temperature field name as a string
   */
  public final String RAW_TEMPERATURE_FIELD_NAME = "temperatureCounts";  
  
  /*
   * A field that stores the raw conductivity field name as a string
   */
  public final String RAW_CONDUCTIVITY_FIELD_NAME = "conductivityFrequency";  
  
  /*
   * A field that stores the raw pressure field name as a string
   */
  public final String RAW_PRESSURE_FIELD_NAME = "pressureCounts";  
  
  /*
   * A field that stores the raw pressure temperature compensation field name as a string
   */
  public final String RAW_PRESSURE_TEMP_COMP_FIELD_NAME = "pressureTemperatureCompensationCounts";  
  
  /*
   * A field that stores the raw voltage channel zero field name as a string
   */
  public final String RAW_VOLTAGE_CHANNEL_ZERO_FIELD_NAME = "voltageChannelZero";  
  
  /*
   * A field that stores the raw voltage channel one field name as a string
   */
  public final String RAW_VOLTAGE_CHANNEL_ONE_FIELD_NAME = "voltageChannelOne";  
  
  /*
   * A field that stores the raw voltage channel two field name as a string
   */
  public final String RAW_VOLTAGE_CHANNEL_TWO_FIELD_NAME = "voltageChannelTwo";  
  
  /*
   * A field that stores the raw voltage channel zero field name as a string
   */
  public final String RAW_VOLTAGE_CHANNEL_THREE_FIELD_NAME = "voltageChannelThree";  
  
   /** 
    * The date format for the timestamp applied to the TChain sample 04 Aug 2008 09:15:01
    */
   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /**
   * The timezone used for the sample date
   */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
    
  /**
   *  Constructor:  Builds all of the components of the CTD data object from
   *  the String data being passed in.  The data string must contain the results 
   *  of the 'DS' command and the 'DCAL' command in order to inform the parser
   *  which data fields should be expected, and which CTD output format to expect.
   *  The data observations should follow the output of the 'DS' and 'DCAL' commands.
   *
   *  @param dataString The String that contains the data and metadata output 
   *                    from the instrument
   */
  public CTDParser(String dataString) throws ParseException{
    
    // Prepare the string for parsing.  The data file is split into metadata
    // and data sections, and each section is then tokenized into lines. 
    // The metadata section is tokenized into field pairs 
    // (e.g. battery type = alkaline) based on the "," delimiter, and the pairs 
    // are split based on "=" and ":" delimiters and placed into a SortedMap for
    // later retrieval.  The data section is split into its component 
    // observations, based on the presence/absence of certain data or voltages.
    this.dataString = dataString;
    this.metadataValuesMap = new TreeMap<String, String>();
    this.dataValuesMap     = new TreeMap<Integer, String>();
    
    try {
      // Parse the data input string.  Two sorted hashmaps are populated in the
      // class, one for metadata name/value pairs, and one for data observation
      // lines.  The latter is then converted into a common data structure after
      // transformations are applied using the calibration coefficients.
      parse();
      
      // Set the individual metadata fields found in the DS and DCAL output
      setMetadata();
      
      // set the data structures from the incoming text lines (Hex or decimal ...)
      setData();
      
    } catch (ParseException pe) {
      throw pe;
      
    }
    
  }
  
  /*
   *  A method used to convert hex data to their raw output units based on per
   * variable conversions found in the SBE19plus user manual under the 
   * OUTPUTFORMAT = 0 section (raw Hex).   
   */
  private double convert(double value, String variableName) {
    
    double returnValue = 0d;
    
    // temperature has no conversion
    if ( variableName.equals(this.RAW_TEMPERATURE_FIELD_NAME) ) {
      returnValue = value;
    
    // conductivity    
    } else if ( variableName.equals(this.RAW_CONDUCTIVITY_FIELD_NAME) ) {
      returnValue = value / 256d;
    
    // pressure    
    } else if ( variableName.equals(this.RAW_PRESSURE_FIELD_NAME) ) {
      returnValue = value;
    
    // voltages    
    } else if ( variableName.equals(this.RAW_PRESSURE_TEMP_COMP_FIELD_NAME)   ||
                variableName.equals(this.RAW_VOLTAGE_CHANNEL_ZERO_FIELD_NAME) || 
                variableName.equals(this.RAW_VOLTAGE_CHANNEL_ONE_FIELD_NAME)  || 
                variableName.equals(this.RAW_VOLTAGE_CHANNEL_TWO_FIELD_NAME)  || 
                variableName.equals(this.RAW_VOLTAGE_CHANNEL_THREE_FIELD_NAME)) {
      returnValue = value / 13107d;
                
    }
    return returnValue;  
  }

  /*
   *  A method used to parse the input data string. The data file is split 
   *  into metadata and data sections, and each section is then tokenized into 
   *  lines.  The metadata section is tokenized into field pairs 
   *  (e.g. battery type = alkaline) based on the "," delimiter, and the pairs 
   *  are split based on "=" and ":" delimiters and placed into a SortedMap for
   *  later retrieval.  The data section is split into its component 
   *  observations, based on the presence/absence of certain data or voltages.
   */
  private void parse() throws ParseException {
    logger.debug("CTDParser.parse() called.");
        
    // create the two sections
    String[] sections = this.dataString.split(metadataDelimiter);
    String[] nameValueArray = {"", ""};  //used later for splitting name/value pairs
    
    if ( sections.length > 1) {
      this.metadataString     = sections[0];  
      this.observationsString = sections[1];
      
      // tokenize the file into lines
      StringTokenizer lineTokenizer = 
        new StringTokenizer(this.metadataString, this.recordDelimiter);
     
      while( lineTokenizer.hasMoreTokens() ) {

        String line = lineTokenizer.nextToken();
        StringTokenizer fieldTokenizer = 
          new StringTokenizer(line, this.METADATA_FIELD_DELIMITER);

        // tokenize the lines into field pairs
        while ( fieldTokenizer.hasMoreTokens() ) {
          
          // remove leading "*" characters and trim whitespace
          String nameValuePair = 
            fieldTokenizer.nextToken().replaceAll("^\\**", "").trim();

          // check for pairs delimited by a colon
          if ( nameValuePair.indexOf(this.PRIMARY_PAIR_DELIMITER) > 0 ) {
            nameValueArray = nameValuePair.split(this.PRIMARY_PAIR_DELIMITER, 2);

            // add the pair to the metadata map
            if ( nameValueArray.length > 1) {
              this.metadataValuesMap.put(nameValueArray[0].trim(), nameValueArray[1].trim());

            // otherwise add an empty pair to the metadata map
            } else {
              this.metadataValuesMap.put(nameValueArray[0].trim(), "");

            }  

          // check for pairs delimited by an equals sign
          } else if ( nameValuePair.indexOf(this.SECONDARY_PAIR_DELIMITER) > 0  ) {
            nameValueArray = nameValuePair.split(this.SECONDARY_PAIR_DELIMITER, 2);
            
            // add the pair to the metadata map
            if ( nameValueArray.length > 1) {
              this.metadataValuesMap.put(nameValueArray[0].trim(), nameValueArray[1].trim());
              
            // otherwise add an empty pair to the metadata map
            } else {
              this.metadataValuesMap.put(nameValueArray[0].trim(), "");

            }  

          // otherwise add an empty pair to the metadata map
          } else {
            this.metadataValuesMap.put(nameValuePair.trim(), "");

          } //if   
                 
        } //while
        
      } //while
      
      StringTokenizer dataTokenizer = 
        new StringTokenizer(observationsString, this.recordDelimiter);
      
      // tokenize the lines into observations strings, place them in sequential
      // order into the 
      while ( dataTokenizer.hasMoreTokens() ) {
        String dataLine = dataTokenizer.nextToken();
        //logger.debug("|" + dataLine + "|");
        this.dataValuesMap.put(dataValuesMap.size() + 1, dataLine);  

      }
        
    } else {
      
      throw new ParseException(
      "Parsing of the CTD data input failed. The header " + 
      "and data sections do not appear to be delimited "  +
      "correctly.  Please be sure that the output of the" +
      "'DS' and 'DCAL' commands are followed by "         +
      "'*END*\\r\\n' and then the data observation lines.", 0);
    }
    
  }                                                  

  /*
   *  A method used to set the data structure based on the sampling mode,
   *  data output format, and pertinent metadata fields.
   */
  private void setData() throws ParseException {
    logger.debug("CTDParser.setData() called.");
      
    // build the list of data variable names and offsets
  
    // handle profile mode
    if ( this.samplingMode.equals("profile") ) {
  
      // handle the raw frquencies and voltages in Hex OUTPUTFORMAT (0)
      if ( this.outputFormat.equals("raw HEX") ) {
        this.dataVariableNames = new ArrayList<String>();
        this.dataVariableNames.add(this.RAW_TEMPERATURE_FIELD_NAME);
        this.dataVariableNames.add(this.RAW_CONDUCTIVITY_FIELD_NAME);
        
        this.dataVariableUnits = new ArrayList<String>();
        this.dataVariableUnits.add("counts");
        this.dataVariableUnits.add("Hz");
        
        this.currentOffset = 6;
        this.dataVariableOffsets = new ArrayList<Integer>();
        this.dataVariableOffsets.add(currentOffset);
        this.currentOffset = currentOffset + 6;
        this.dataVariableOffsets.add(currentOffset);
  
        // Is pressure present?
        if ( this.hasPressure ) {
          this.dataVariableNames.add(this.RAW_PRESSURE_FIELD_NAME);
          this.dataVariableUnits.add("counts");
          this.currentOffset = this.currentOffset + 6;
          this.dataVariableOffsets.add(this.currentOffset);
  
          // And is it a strain gauge sensor?
          if ( this.hasStrainGaugePressure ) {
            dataVariableNames.add(this.RAW_PRESSURE_TEMP_COMP_FIELD_NAME);
            dataVariableUnits.add("counts");
            currentOffset = currentOffset + 4;
            dataVariableOffsets.add(currentOffset);
  
          }
  
        } else {
          logger.info("There is no pressure sensor.");
        }
  
        // Is there a channel zero voltage present?
        if ( this.hasVoltageChannelZero ) {
          this.dataVariableNames.add(this.RAW_VOLTAGE_CHANNEL_ZERO_FIELD_NAME);
          this.dataVariableUnits.add("V");
          this.currentOffset = this.currentOffset + 4;
          this.dataVariableOffsets.add(this.currentOffset);
  
        }
  
        // Is there a channel one voltage present?
        if ( this.hasVoltageChannelOne ) {
          this.dataVariableNames.add(this.RAW_VOLTAGE_CHANNEL_ONE_FIELD_NAME);
          this.dataVariableUnits.add("V");
          this.currentOffset = this.currentOffset + 4;
          this.dataVariableOffsets.add(this.currentOffset);
  
        }
  
        // Is there a channel two voltage present?
        if ( this.hasVoltageChannelTwo ) {
          this.dataVariableNames.add(this.RAW_VOLTAGE_CHANNEL_TWO_FIELD_NAME);
          this.dataVariableUnits.add("V");
          this.currentOffset = this.currentOffset + 4;
          this.dataVariableOffsets.add(this.currentOffset);
  
        }
  
        // Is there a channel three voltage present?
        if ( this.hasVoltageChannelThree ) {
          this.dataVariableNames.add(this.RAW_VOLTAGE_CHANNEL_THREE_FIELD_NAME);
          this.dataVariableUnits.add("V");
          this.currentOffset = this.currentOffset + 4;
          this.dataVariableOffsets.add(this.currentOffset);
  
        }
        
        /*
         * @todo - handle SBE38 and/or gasTensionDevice data
         */
          
        // We now know the data variable names, units, and corresponding
        // character offsets for each Hex data string found in the 
        // dataValuesMap.  Build a raw matrix from the dataValuesMap by only
        // applying output factors.  Conversion to useful variable units
        // will happen in the calling source driver since voltage channel
        // semantics are unknown to the parser
        int beginIndex       = 0;
        int endIndex         = 0;
        int offsetIndex      = 0;
        String hexSubstring  = "";
        String hexDataString = "";
        Hex decoder          = new Hex();
        double value         = 0d;
        convertedDataValuesMatrix = 
          new Array2DRowRealMatrix(this.dataValuesMap.size() - 1, dataVariableOffsets.size());
  
        for ( int rowIndex = 1; rowIndex < this.dataValuesMap.size(); rowIndex++ ) {
          hexDataString = this.dataValuesMap.get(rowIndex);
          logger.debug(rowIndex + ") hexDataString is: " + hexDataString);
          
          for ( offsetIndex = 0; offsetIndex < dataVariableOffsets.size(); offsetIndex++ ) {
            endIndex = dataVariableOffsets.get(offsetIndex);
            hexSubstring = hexDataString.substring(beginIndex, endIndex);
            
            try {
              // convert the hex characters to bytes
              byte[] hexAsBytes = decoder.decodeHex(hexSubstring.toCharArray());
                  
              BigInteger bigInteger = new BigInteger(hexAsBytes);
              int intValue = bigInteger.intValue();
              
              // the hex values are either 2 or 3 bytes long (AABBCC or AABB)
              // BigInteger fills in missing bits with 0xFF. Remove them.  This
              // is only a problem with large bytes that cause the value to 
              // become negative.
              if ( hexAsBytes.length < 3 ) {
                intValue = (intValue & 0x0000FFFF);
                
              } else {
                intValue = (intValue & 0x00FFFFFF);
                
              }
              value = new Integer(intValue).doubleValue();
              
              // convert the value based on the CTD User manual conversion using
              // the corresponding data variable name to determine which conversion
              double convertedValue = convert(value, dataVariableNames.get(offsetIndex));                                           
              
              convertedDataValuesMatrix.setEntry(rowIndex - 1, offsetIndex, convertedValue);
              logger.debug("\t"                               + 
                           dataVariableNames.get(offsetIndex) + 
                           " is:\t"                           + 
                           value                              + 
                           "\tConverted: "                    + 
                           convertedValue);
              // set the beginIndex to start at the endIndex
              beginIndex = endIndex;  
            
            } catch ( DecoderException de ){
              logger.debug("Could not decode the Hex string: " + hexSubstring); 
            }
            
          } // for
          
          // reset the offsetIndex for the next hexDataString
          offsetIndex = 0;
          beginIndex  = 0;
        } // for
                 
      // handle the engineering units in Hex OUTPUTFORMAT (1)
      } else if ( this.outputFormat.equals("converted Hex") ) {
        
        /*
         * @todo - handle OUTPUTFORMAT (1)
         */
      
      // handle the raw frquencies and voltages in decimal OUTPUTFORMAT (2)
      } else if ( this.outputFormat.equals("raw decimal") ) {
      
        /*
         * @todo - handle OUTPUTFORMAT (2)
         */
      
      // handle the engineering units in decimal OUTPUTFORMAT (3)
      } else if ( this.outputFormat.equals("converted decimal") ) { 
      
        /*
         * @todo - handle OUTPUTFORMAT (3)
         */
      }
    
    // handle moored mode
    } else if ( this.samplingMode.equals("moored") ) {
  
    } else {
      throw new ParseException("There was an error parsing the data string. "  +
                               "The sampling mode is not recognized.", 0);
  
    }
    
  }
  
  /*
   *  A method used to set each of the metadata fields found in the output of
   *  the DS and DCAL commands.  The method handles both profile and moored
   *  modes, and builds metadata fields based on the data output format.
   */
  private void setMetadata() throws ParseException {
    logger.debug("CTDParser.setMetadata() called.");
    
    // Are we in profile or moored mode?
    if ( this.SAMPLING_MODE != null ) {
      this.samplingMode = this.metadataValuesMap.get(this.SAMPLING_MODE);
      logger.info("Sampling mode is: " + this.samplingMode);
  
    } else {
      throw new ParseException("There was an error parsing the data string. "  +
                               "The sampling mode is not stated correctly in " +
                               "the metadata from the DS command. Please "     +
                               "check the output.", 0);
  
    }
  
    // Determine the output format
    if ( this.OUTPUT_FORMAT != null ) {
      this.outputFormat = this.metadataValuesMap.get(this.OUTPUT_FORMAT);
      logger.info("Data output format is: " + this.outputFormat);
  
    } else {
      throw new ParseException("There was an error parsing the data string. "  +
                               "The output format is not stated correctly in " +
                               "the metadata from the DS command. Please "     +
                               "check the output.", 0);
  
    }
  
    // Is there a pressure sensor?  If so, what type?
    if ( this.PRESSURE_SENSOR_TYPE != null ) {
      this.pressureSensorType = 
        this.metadataValuesMap.get(this.PRESSURE_SENSOR_TYPE).trim();
      this.hasPressure = true;
  
      if ( this.pressureSensorType.equals("strain gauge") ) {
        this.hasStrainGaugePressure = true;
  
      }
  
      if ( this.PRESSURE_SENSOR_RANGE != null ) {
        this.pressureSensorRange = 
          this.metadataValuesMap.get(this.PRESSURE_SENSOR_RANGE).trim();
      }
  
    } else {
      logger.info("There is no pressure sensor.");
  
    }
  
    // Determine if there are external voltages to read
  
    // channel 0
    if ( this.EXTERNAL_VOLTAGE_CHANNEL_ZERO != null ) {
      this.externalVoltageChannelZero = 
        this.metadataValuesMap.get(this.EXTERNAL_VOLTAGE_CHANNEL_ZERO).trim();
      if ( this.externalVoltageChannelZero.equals("yes") ) {
        this.hasVoltageChannelZero = true;
        logger.info("There is a channel 0 voltage.");
  
      } else {
        logger.info("There is no channel 0 voltage.");
  
      }
    }
  
    // channel 1
    if ( this.EXTERNAL_VOLTAGE_CHANNEL_ONE != null ) {
      this.externalVoltageChannelOne = 
        this.metadataValuesMap.get(this.EXTERNAL_VOLTAGE_CHANNEL_ONE).trim();
      if ( this.externalVoltageChannelOne.equals("yes") ) {
        this.hasVoltageChannelOne = true;
        logger.info("There is a channel 1 voltage.");
  
      } else {
        logger.info("There is no channel 1 voltage.");
  
      }
    }
  
    // channel 2
    if ( this.EXTERNAL_VOLTAGE_CHANNEL_TWO != null ) {
      this.externalVoltageChannelTwo = 
        this.metadataValuesMap.get(this.EXTERNAL_VOLTAGE_CHANNEL_TWO).trim();
      if ( this.externalVoltageChannelTwo.equals("yes") ) {
        this.hasVoltageChannelTwo = true;
        logger.info("There is a channel 2 voltage.");
  
      } else {
        logger.info("There is no channel 2 voltage.");
  
      }
    }
  
    // channel 3
    if ( this.EXTERNAL_VOLTAGE_CHANNEL_THREE != null ) {
      this.externalVoltageChannelThree = 
        this.metadataValuesMap.get(this.EXTERNAL_VOLTAGE_CHANNEL_THREE).trim();
      if ( this.externalVoltageChannelThree.equals("yes") ) {
        this.hasVoltageChannelThree = true;
        logger.info("There is a channel 3 voltage.");
  
      } else {
        logger.info("There is no channel 3 voltage.");
  
      }
    }
  
    // Determine if there is an SBE38 secondary temperature to read
    if ( this.SBE38_TEMPERATURE_SENSOR != null ) {
      this.sbe38TemperatureSensor = 
        this.metadataValuesMap.get(this.SBE38_TEMPERATURE_SENSOR).trim();
      if ( this.sbe38TemperatureSensor.equals("yes") ) {
        this.hasSBE38TemperatureSensor = true;
  
      }
    }
  
    // Determine if there is a gas tension device to read
    if ( this.GAS_TENSION_DEVICE != null ) {
      this.gasTensionDevice = 
        this.metadataValuesMap.get(this.GAS_TENSION_DEVICE).trim();
      if ( this.gasTensionDevice.equals("yes") ) {
        this.hasGasTensionDevice = true;
  
      }
    }
  
    // set each of the metadata fields
    
    // set the first sample time field
    if ( this.FIRST_SAMPLE_TIME != null ) {
      this.firstSampleTime = this.metadataValuesMap.get(this.FIRST_SAMPLE_TIME);
      logger.info("First sample time is: " + this.firstSampleTime);
  
    }
    
    // set the file name time field
    if ( this.FILE_NAME != null ) {
      this.fileName = this.metadataValuesMap.get(this.FILE_NAME);
      logger.info("File name is: " + this.fileName);
  
    }
    
    // set the temperature serial number field
    if ( this.TEMPERATURE_SERIAL_NUMBER != null ) {
      this.temperatureSerialNumber = this.metadataValuesMap.get(this.TEMPERATURE_SERIAL_NUMBER);
      logger.info("Temperature serial number is: " + this.temperatureSerialNumber);
  
    }
    
    // set the conductivity serial number field
    if ( this.CONDUCTIVITY_SERIAL_NUMBER != null ) {
      this.conductivitySerialNumber = this.metadataValuesMap.get(this.CONDUCTIVITY_SERIAL_NUMBER);
      logger.info("Conductivity serial number is: " + this.conductivitySerialNumber);
  
    }
    
    // set the system upload time field
    if ( this.SYSTEM_UPLOAD_TIME != null ) {
      this.systemUpLoadTime = this.metadataValuesMap.get(this.SYSTEM_UPLOAD_TIME);
      logger.info("System upload time is: " + this.systemUpLoadTime);
  
    }
    
    // set the cruise information field
    if ( this.CRUISE_INFORMATION != null ) {
      this.cruiseInformation = this.metadataValuesMap.get(this.CRUISE_INFORMATION);
      logger.info("Cruise information is: " + this.cruiseInformation);
  
    }
    
    // set the station information field
    if ( this.STATION_INFORMATION != null ) {
      this.stationInformation = this.metadataValuesMap.get(this.STATION_INFORMATION);
      logger.info("Station information is: " + this.stationInformation);
  
    }
    
    // set the ship information field
    if ( this.SHIP_INFORMATION != null ) {
      this.shipInformation = this.metadataValuesMap.get(this.SHIP_INFORMATION);
      logger.info("Ship information is: " + this.shipInformation);
  
    }
    
    // set the chief scientist field
    if ( this.CHIEF_SCIENTIST != null ) {
      this.chiefScientist = this.metadataValuesMap.get(this.CHIEF_SCIENTIST);
      logger.info("Chief scientist is: " + this.chiefScientist);
  
    }
    
    // set the organization field
    if ( this.ORGANIZATION != null ) {
      this.organization = this.metadataValuesMap.get(this.ORGANIZATION);
      logger.info("Organization is: " + this.organization);
  
    }
    
    // set the area of operation field
    if ( this.AREA_OF_OPERATION != null ) {
      this.areaOfOperation = this.metadataValuesMap.get(this.AREA_OF_OPERATION);
      logger.info("Area of operation is: " + this.areaOfOperation);
  
    }
    
    // set the instrument package field
    if ( this.INSTRUMENT_PACKAGE != null ) {
      this.instrumentPackage = this.metadataValuesMap.get(this.INSTRUMENT_PACKAGE);
      logger.info("Instrument package is: " + this.instrumentPackage);
  
    }
    
    // set the mooring number field
    if ( this.MOORING_NUMBER != null ) {
      this.mooringNumber = this.metadataValuesMap.get(this.MOORING_NUMBER);
      logger.info("Mooring number is: " + this.mooringNumber);
  
    }
    
    // set the instrument latitude field
    if ( this.INSTRUMENT_LATITUDE != null ) {
      this.instrumentLatitude = this.metadataValuesMap.get(this.INSTRUMENT_LATITUDE);
      logger.info("Instrument latitude is: " + this.instrumentLatitude);
  
    }
    
    // set the instrument longitude field
    if ( this.INSTRUMENT_LONGITUDE != null ) {
      this.instrumentLongitude = this.metadataValuesMap.get(this.INSTRUMENT_LONGITUDE);
      logger.info("Instrument longitude is: " + this.instrumentLongitude);
  
    }
    
    // set the depth sounding field
    if ( this.DEPTH_SOUNDING != null ) {
      this.depthSounding = this.metadataValuesMap.get(this.DEPTH_SOUNDING);
      logger.info("Depth sounding is: " + this.depthSounding);
  
    }
    
    // set the profile number field
    if ( this.PROFILE_NUMBER != null ) {
      this.profileNumber = this.metadataValuesMap.get(this.PROFILE_NUMBER);
      logger.info("Profile number is: " + this.profileNumber);
  
    }
    
    // set the profile direction field
    if ( this.PROFILE_DIRECTION != null ) {
      this.profileDirection = this.metadataValuesMap.get(this.PROFILE_DIRECTION);
      logger.info("Profile direction is: " + this.profileDirection);
  
    }
    
    // set the deployment notes field
    if ( this.DEPLOYMENT_NOTES != null ) {
      this.deploymentNotes = this.metadataValuesMap.get(this.DEPLOYMENT_NOTES);
      logger.info("Deployment notes are: " + this.deploymentNotes);
  
    }
    
    // set the main battery voltage field
    if ( this.MAIN_BATTERY_VOLTAGE != null ) {
      this.mainBatteryVoltage = this.metadataValuesMap.get(this.MAIN_BATTERY_VOLTAGE);
      logger.info("Main battery voltage is: " + this.mainBatteryVoltage);
  
    }
    
    // set the lithium battery voltage field
    if ( this.LITHIUM_BATTERY_VOLTAGE != null ) {
      this.lithiumBatteryVoltage = 
        this.metadataValuesMap.get(this.LITHIUM_BATTERY_VOLTAGE);
      logger.info("Lithium battery voltage is: " + this.lithiumBatteryVoltage);
  
    }
    
    // set the operating current field
    if ( this.OPERATING_CURRENT != null ) {
      this.operatingCurrent = this.metadataValuesMap.get(this.OPERATING_CURRENT);
      logger.info("Operating current is: " + this.operatingCurrent);
  
    }
    
    // set the pump current field
    if ( this.PUMP_CURRENT != null ) {
      this.pumpCurrent = this.metadataValuesMap.get(this.PUMP_CURRENT);
      logger.info("Pump current is: " + this.pumpCurrent);
  
    }
    
    // set the channels 0 and 1 external current field
    if ( this.CHANNELS_01_EXTERNAL_CURRENT != null ) {
      this.channels01ExternalCurrent = 
      this.metadataValuesMap.get(this.CHANNELS_01_EXTERNAL_CURRENT);
      logger.info("Channels 0 and 1 external current is: " + 
                  this.channels01ExternalCurrent);
  
    }
    
    // set the channels 2 and 3 external current field
    if ( this.CHANNELS_23_EXTERNAL_CURRENT != null ) {
      this.channels23ExternalCurrent = 
      this.metadataValuesMap.get(this.CHANNELS_23_EXTERNAL_CURRENT);
      logger.info("Channels 2 and 3 external current is: " + 
                  this.channels23ExternalCurrent);
  
    }
    
    // set the logging status field
    if ( this.LOGGING_STATUS != null ) {
      this.loggingStatus = this.metadataValuesMap.get(this.LOGGING_STATUS);
      logger.info("Logging status is: " + this.loggingStatus);
  
    }
    
    // set the number of scans to average field
    if ( this.NUMBER_OF_SCANS_TO_AVERAGE != null ) {
      this.numberOfScansToAverage = 
        this.metadataValuesMap.get(this.NUMBER_OF_SCANS_TO_AVERAGE);
      logger.info("Number of scans to average is: " + 
                  this.numberOfScansToAverage);
  
    }
    
    // set the number of samples field
    if ( this.NUMBER_OF_SAMPLES != null ) {
      this.numberOfSamples = this.metadataValuesMap.get(this.NUMBER_OF_SAMPLES);
      logger.info("Number of samples is: " + this.numberOfSamples);
  
    }
    
    // set the number of available samples field
    if ( this.NUMBER_OF_AVAILABLE_SAMPLES != null ) {
      this.numberOfAvailableSamples = 
        this.metadataValuesMap.get(this.NUMBER_OF_AVAILABLE_SAMPLES);
      logger.info("Number of available samples is: " + 
                  this.numberOfAvailableSamples);
  
    }
    
    // set the sample interval field
    if ( this.SAMPLE_INTERVAL != null ) {
      this.sampleInterval = this.metadataValuesMap.get(this.SAMPLE_INTERVAL);
      logger.info("Sample interval is: " + this.sampleInterval);
  
    }
    
    // set the measurements per sample field
    if ( this.MEASUREMENTS_PER_SAMPLE != null ) {
      this.measurementsPerSample = this.metadataValuesMap.get(this.MEASUREMENTS_PER_SAMPLE);
      logger.info("Measurements per sample is: " + this.measurementsPerSample);
  
    }
    
    // set the transmit real time field
    if ( this.TRANSMIT_REALTIME != null ) {
      this.transmitRealtime = this.metadataValuesMap.get(this.TRANSMIT_REALTIME);
      logger.info("Transmit real time state is: " + this.transmitRealtime);
  
    }
    
    // set the number of casts field
    if ( this.NUMBER_OF_CASTS != null ) {
      this.numberOfCasts = this.metadataValuesMap.get(this.NUMBER_OF_CASTS);
      logger.info("Number of casts is: " + this.numberOfCasts);
  
    }
    
    // set the minimum conductivity frequency field
    if ( this.MINIMUM_CONDUCTIVITY_FREQUENCY != null ) {
      this.minimumConductivityFrequency = 
        this.metadataValuesMap.get(this.MINIMUM_CONDUCTIVITY_FREQUENCY);
      logger.info("Minimum conductivity frequency is: " + 
                  this.minimumConductivityFrequency);
  
    }
    
    // set the pump delay field
    if ( this.PUMP_DELAY != null ) {
      this.pumpDelay = this.metadataValuesMap.get(this.PUMP_DELAY);
      logger.info("Pump delay is: " + this.pumpDelay);
  
    }
    
    // set the automatic logging field
    if ( this.AUTOMATIC_LOGGING != null ) {
      this.automaticLogging = this.metadataValuesMap.get(this.AUTOMATIC_LOGGING);
      logger.info("Automatic logging is: " + this.automaticLogging);
  
    }
    
    // set the ignore magnetic switch field
    if ( this.IGNORE_MAGNETIC_SWITCH != null ) {
      this.ignoreMagneticSwitch = this.metadataValuesMap.get(this.IGNORE_MAGNETIC_SWITCH);
      logger.info("Ignore magnetic switch is: " + this.ignoreMagneticSwitch);
  
    }
    
    // set the battery type field
    if ( this.BATTERY_TYPE != null ) {
      this.batteryType = this.metadataValuesMap.get(this.BATTERY_TYPE);
      logger.info("Battery type is: " + this.batteryType);
  
    }
    
    // set the echo commands field
    if ( this.ECHO_COMMANDS != null ) {
      this.echoCommands = this.metadataValuesMap.get(this.ECHO_COMMANDS);
      logger.info("Echo commands state is: " + this.echoCommands);
  
    }
    
    // set the temperature calibration date field
    if ( this.TEMPERATURE_CALIBRATION_DATE != null ) {
      this.temperatureCalibrationDate = 
        this.metadataValuesMap.get(this.TEMPERATURE_CALIBRATION_DATE);
      logger.info("Temperature calibration date is: " + 
                  this.temperatureCalibrationDate);
  
    }
    
    // set the temperature coefficient TA0 field
    if ( this.TEMPERATURE_COEFFICIENT_TA0 != null ) {
      this.temperatureCoefficientTA0 = 
        this.metadataValuesMap.get(this.TEMPERATURE_COEFFICIENT_TA0);
      logger.info("Temperature coefficient TA0 is: " + 
                  this.temperatureCoefficientTA0);
  
    }
  
    // set the temperature coefficient TA1 field
    if ( this.TEMPERATURE_COEFFICIENT_TA1 != null ) {
      this.temperatureCoefficientTA1 = 
        this.metadataValuesMap.get(this.TEMPERATURE_COEFFICIENT_TA1);
      logger.info("Temperature coefficient TA1 is: " + 
                  this.temperatureCoefficientTA1);
  
    }
    
    // set the temperature coefficient TA2 field
    if ( this.TEMPERATURE_COEFFICIENT_TA2 != null ) {
      this.temperatureCoefficientTA2 = 
        this.metadataValuesMap.get(this.TEMPERATURE_COEFFICIENT_TA2);
      logger.info("Temperature coefficient TA2 is: " + 
                  this.temperatureCoefficientTA2);
  
    }
    
    // set the temperature coefficient TA3 field
    if ( this.TEMPERATURE_COEFFICIENT_TA3 != null ) {
      this.temperatureCoefficientTA3 = 
        this.metadataValuesMap.get(this.TEMPERATURE_COEFFICIENT_TA3);
      logger.info("Temperature coefficient TA3 is: " + 
                  this.temperatureCoefficientTA3);
  
    }
    
    // set the temperature offset coefficient field
    if ( this.TEMPERATURE_OFFSET_COEFFICIENT != null ) {
      this.temperatureOffsetCoefficient = 
        this.metadataValuesMap.get(this.TEMPERATURE_OFFSET_COEFFICIENT);
      logger.info("Temperature offset coefficient is: " + 
                  this.temperatureOffsetCoefficient);
  
    }
    
    // set the conductivity calibration date field
    if ( this.CONDUCTIVITY_CALIBRATION_DATE != null ) {
      this.conductivityCalibrationDate = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_CALIBRATION_DATE);
      logger.info("Conductivity calibration date is: " + 
                  this.conductivityCalibrationDate);
  
    }
    
    // set the conductivity coefficient G field
    if ( this.CONDUCTIVITY_COEFFICIENT_G != null ) {
      this.conductivityCoefficientG = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_G);
      logger.info("Conductivity coefficient G is: " + 
                  this.conductivityCoefficientG);
  
    }
  
    // set the conductivity coefficient H field
    if ( this.CONDUCTIVITY_COEFFICIENT_H != null ) {
      this.conductivityCoefficientH = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_H);
      logger.info("Conductivity coefficient H is: " + 
                  this.conductivityCoefficientH);
  
    }
    
    // set the conductivity coefficient I field
    if ( this.CONDUCTIVITY_COEFFICIENT_I != null ) {
      this.conductivityCoefficientI = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_I);
      logger.info("Conductivity coefficient I is: " + 
                  this.conductivityCoefficientI);
  
    }
    
    // set the conductivity coefficient J field
    if ( this.CONDUCTIVITY_COEFFICIENT_J != null ) {
      this.conductivityCoefficientJ = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_J);
      logger.info("Conductivity coefficient J is: " + 
                  this.conductivityCoefficientJ);
  
    }
    
    // set the conductivity coefficient CF0 field
    if ( this.CONDUCTIVITY_COEFFICIENT_CF0 != null ) {
      this.conductivityCoefficientCF0 = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_CF0);
      logger.info("Conductivity coefficient CF0 is: " + 
                  this.conductivityCoefficientCF0);
  
    }
  
    // set the conductivity coefficient CPCOR field
    if ( this.CONDUCTIVITY_COEFFICIENT_CPCOR != null ) {
      this.conductivityCoefficientCPCOR = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_CPCOR);
      logger.info("Conductivity coefficient CPCOR is: " + 
                  this.conductivityCoefficientCPCOR);
  
    }
    
    // set the conductivity coefficient CTCOR field
    if ( this.CONDUCTIVITY_COEFFICIENT_CTCOR != null ) {
      this.conductivityCoefficientCTCOR = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_CTCOR);
      logger.info("Conductivity coefficient CTCOR is: " + 
                  this.conductivityCoefficientCTCOR);
  
    }
    
    // set the conductivity coefficient CSLOPE field
    if ( this.CONDUCTIVITY_COEFFICIENT_CSLOPE != null ) {
      this.conductivityCoefficientCSLOPE = 
        this.metadataValuesMap.get(this.CONDUCTIVITY_COEFFICIENT_CSLOPE);
      logger.info("Conductivity coefficient CSLOPE is: " + 
                  this.conductivityCoefficientCSLOPE);
  
    }
         
    // set the pressure serial number field
    if ( this.PRESSURE_SERIAL_NUMBER != null ) {
      this.pressureSerialNumber = this.metadataValuesMap.get(this.PRESSURE_SERIAL_NUMBER);
      logger.info("Pressure serial number is: " + this.pressureSerialNumber);
  
    }
    
    // set the pressure coefficient PA0 field
    if ( this.PRESSURE_COEFFICIENT_PA0 != null ) {
      this.pressureCoefficientPA0 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PA0);
      logger.info("Pressure coefficient PA0 is: " + 
                  this.pressureCoefficientPA0);
  
    }
    
    // set the pressure coefficient PA1 field
    if ( this.PRESSURE_COEFFICIENT_PA1 != null ) {
      this.pressureCoefficientPA1 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PA1);
      logger.info("Pressure coefficient PA1 is: " + 
                  this.pressureCoefficientPA1);
  
    }
    
    // set the pressure coefficient PA2 field
    if ( this.PRESSURE_COEFFICIENT_PA2 != null ) {
      this.pressureCoefficientPA2 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PA2);
      logger.info("Pressure coefficient PA2 is: " + 
                  this.pressureCoefficientPA2);
  
    }
    
    // set the pressure coefficient PTCA0 field
    if ( this.PRESSURE_COEFFICIENT_PTCA0 != null ) {
      this.pressureCoefficientPTCA0 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTCA0);
      logger.info("Pressure coefficient PTCA0 is: " + 
                  this.pressureCoefficientPTCA0);
  
    }
    
    // set the pressure coefficient PTCA1 field
    if ( this.PRESSURE_COEFFICIENT_PTCA1 != null ) {
      this.pressureCoefficientPTCA1 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTCA1);
      logger.info("Pressure coefficient PTCA1 is: " + 
                  this.pressureCoefficientPTCA1);
  
    }
    
    // set the pressure coefficient PTCA2 field
    if ( this.PRESSURE_COEFFICIENT_PTCA2 != null ) {
      this.pressureCoefficientPTCA2 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTCA2);
      logger.info("Pressure coefficient PTCA2 is: " + 
                  this.pressureCoefficientPTCA2);
  
    }
    
    // set the pressure coefficient PTCB0 field
    if ( this.PRESSURE_COEFFICIENT_PTCB0 != null ) {
      this.pressureCoefficientPTCB0 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTCB0);
      logger.info("Pressure coefficient PTCB0 is: " + 
                  this.pressureCoefficientPTCB0);
  
    }
    
    // set the pressure coefficient PTCB1 field
    if ( this.PRESSURE_COEFFICIENT_PTCB1 != null ) {
      this.pressureCoefficientPTCB1 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTCB1);
      logger.info("Pressure coefficient PTCB1 is: " + 
                  this.pressureCoefficientPTCB1);
  
    }
    
    // set the pressure coefficient PTCB2 field
    if ( this.PRESSURE_COEFFICIENT_PTCB2 != null ) {
      this.pressureCoefficientPTCB2 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTCB2);
      logger.info("Pressure coefficient PTCB2 is: " + 
                  this.pressureCoefficientPTCB2);
  
    }
    
    // set the pressure coefficient PTEMPA0 field
    if ( this.PRESSURE_COEFFICIENT_PTEMPA0 != null ) {
      this.pressureCoefficientPTEMPA0 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTEMPA0);
      logger.info("Pressure coefficient PTEMPA0 is: " + 
                  this.pressureCoefficientPTEMPA0);
  
    }
    
    // set the pressure coefficient PTEMPA1 field
    if ( this.PRESSURE_COEFFICIENT_PTEMPA1 != null ) {
      this.pressureCoefficientPTEMPA1 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTEMPA1);
      logger.info("Pressure coefficient PTEMPA1 is: " + 
                  this.pressureCoefficientPTEMPA1);
  
    }
    
    // set the pressure coefficient PTEMPA2 field
    if ( this.PRESSURE_COEFFICIENT_PTEMPA2 != null ) {
      this.pressureCoefficientPTEMPA2 = 
        this.metadataValuesMap.get(this.PRESSURE_COEFFICIENT_PTEMPA2);
      logger.info("Pressure coefficient PTEMPA2 is: " + 
                  this.pressureCoefficientPTEMPA2);
  
    }
    
    // set the pressure offset coefficient field
    if ( this.PRESSURE_OFFSET_COEFFICIENT != null ) {
      this.pressureOffsetCoefficient = 
        this.metadataValuesMap.get(this.PRESSURE_OFFSET_COEFFICIENT);
      logger.info("Pressure offset coefficient is: " + 
                  this.pressureOffsetCoefficient);
  
    }
           
  }  
}                                               
