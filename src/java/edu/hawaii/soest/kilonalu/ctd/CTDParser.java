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

import edu.ucsb.nceas.utilities.XMLUtilities;

import java.io.IOException;
import java.io.StringReader;

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

import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

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
    
  /*  The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /*  The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /**  The Logger instance used to log system messages  */
  static Logger logger = Logger.getLogger(CTDParser.class);
  
  /*  A field that stores the data file string input as a String */
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
  
  /*  A field used to keep track of the current data variable offset */
   private int currentOffset;
   
  /**  A field that stores the metadata field delimiter as a String */
  public final String METADATA_FIELD_DELIMITER = ",";
  
  /**  A field that stores the primary name/value pair delimiter as a String */
  public final String PRIMARY_PAIR_DELIMITER = ":";
  
  /**  A field that stores the primary name/value pair delimiter as a String */
  public final String SECONDARY_PAIR_DELIMITER = "=";

  /*  A field that stores the CTD synchronization mode as a String */
  private String synchronizationMode;
  
  /**  A field that stores the CTD synchronization mode key as a String */
  public final String SYNCHRONIZATION_MODE = "SyncMode";

  /*  A field that stores the CTD sampling mode as a String */
  private String samplingMode;
  
  /**  A field that stores the CTD sampling mode key as a String */
  public final String SAMPLING_MODE = "mode";

  /*  A field that stores the CTD first sample time as a String */
  private String firstSampleTime;
  
  /**  A field that stores the CTD first sample time key as a String */
  public final String FIRST_SAMPLE_TIME = "First Sample Time";

  /**  A field that stores the default record delimiter in a String. */
  public final String DEFAULT_RECORD_DELIMITER = "\r\n";
  
  /*  A field that stores the record delimiter in a String. */
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
  
  /**  A field that stores the file name as a String */
  private String fileName;
  
  /**  A field that stores the file name key as a String */
  public final String FILE_NAME = "FileName";
  
  /*  A field that stores the temperature sensor serial number as a String */
  private String temperatureSerialNumber;
  
  /**  A field that stores the temperature sensor serial number key as a String */
  public final String TEMPERATURE_SERIAL_NUMBER = "Temperature SN";
  
  /*  A field that stores the conductivity sensor serial number as a String */
  private String conductivitySerialNumber;
  
  /**  A field that stores the conductivity sensor serial number key as a String */
  public final String CONDUCTIVITY_SERIAL_NUMBER = "Conductivity SN";
  
  /*  A field that stores the System UpLoad Time as a String */
  private String systemUpLoadTime;
  
  /**  A field that stores the System UpLoad Time key as a String */
  public final String SYSTEM_UPLOAD_TIME = "System UpLoad Time";
  
  /*  A field that stores the cruise information as a String */
  private String cruiseInformation;
  
  /**  A field that stores the cruise information key as a String */
  public final String CRUISE_INFORMATION = "Cruise";
  
  /*  A field that stores the station information as a String */
  private String stationInformation;
  
  /**  A field that stores the station information key as a String */
  public final String STATION_INFORMATION = "Station";
  
  /*  A field that stores the ship information as a String */
  private String shipInformation;
  
  /**  A field that stores the ship information key as a String */
  public final String SHIP_INFORMATION = "Ship";
  
  /*  A field that stores the chief scientist information as a String */
  private String chiefScientist;
  
  /**  A field that stores the chief scientist information key as a String */
  public final String CHIEF_SCIENTIST = "Chief_Scientist";
  
  /*  A field that stores the organization information as a String */
  private String organization;
  
  /**  A field that stores the organization information key as a String */
  public final String ORGANIZATION = "Organization";
  
  /*  A field that stores the area of operation information as a String */
  private String areaOfOperation;
  
  /**  A field that stores the area of operation information key as a String */
  public final String AREA_OF_OPERATION = "Area_of_Operation";
  
  /*  A field that stores the instrument package information as a String */
  private String instrumentPackage;
  
  /**  A field that stores the instrument package information key as a String */
  public final String INSTRUMENT_PACKAGE = "Package";
  
  /*  A field that stores the mooring number as a String */
  private String mooringNumber;
  
  /**  A field that stores the mooring number key as a String */
  public final String MOORING_NUMBER = "Mooring_Number";
  
  /*  A field that stores the instrument latitude as a String */
  private String instrumentLatitude;
  
  /**  A field that stores the instrument latitude key as a String */
  public final String INSTRUMENT_LATITUDE = "Latitude";
  
  /*  A field that stores the instrument longitude as a String */
  private String instrumentLongitude;
  
  /**  A field that stores the instrument longitude key as a String */
  public final String INSTRUMENT_LONGITUDE = "Longitude";
  
  /*  A field that stores the instrument depth sounding as a String */
  private String depthSounding;
  
  /**  A field that stores the instrument depth sounding key as a String */
  public final String DEPTH_SOUNDING = "Sounding";
  
  /*  A field that stores the instrument profile number as a String */
  private String profileNumber;
  
  /**  A field that stores the instrument profile number key as a String */
  public final String PROFILE_NUMBER = "Profile_Number";
  
  /*  A field that stores the instrument profile direction as a String */
  private String profileDirection;
  
  /**  A field that stores the instrument profile direction key as a String */
  public final String PROFILE_DIRECTION = "Profile_Direction";
  
  /*  A field that stores the instrument deployment notes as a String */
  private String deploymentNotes;
  
  /**  A field that stores the instrument deployment notes key as a String */
  public final String DEPLOYMENT_NOTES = "Notes";
  
  /*  A field that stores the instrument device type as a String */
  private String deviceType;
  
  /**  A field that stores the instrument device type key as a String */
  public final String DEVICE_TYPE = "DeviceType";
  
  /*  A field that stores the instrument serial number as a String */
  private String instrumentSerialNumber;
  
  /**  A field that stores the instrument serial number key as a String */
  public final String INSTRUMENT_SERIAL_NUMBER = "InstrumentSerialNumber";
  
  /*  A field that stores the instrument date time as a String */
  private String instrumentDateTime;
  
  /**  A field that stores the instrument date time key as a String */
  public final String INSTRUMENT_DATE_TIME = "DateTime";
  
  /*  A field that stores the number of instrument events as a String */
  private String numberOfInstrumentEvents;
  
  /**  A field that stores the number of instrument events key as a String */
  public final String NUMBER_OF_INSTRUMENT_EVENTS = "EventSummary/@numEvents";
  
  /*  A field that stores the pressure installed value as a String */
  private String pressureInstalled;
  
  /**  A field that stores the pressure installed key as a String */
  public final String PRESSURE_INSTALLED = "PressureInstalled";
  
  /*  A field that stores the pump installed value as a String */
  private String pumpInstalled;
  
  /**  A field that stores the pump installed key as a String */
  public final String PUMP_INSTALLED = "PumpInstalled";
  
  /*  A field that stores the main battery voltage as a String */
  private String mainBatteryVoltage;
  
  /**  A field that stores the main battery voltage key as a String */
  public final String MAIN_BATTERY_VOLTAGE = "vbatt";
  
  /*  A field that stores the lithium battery voltage as a String */
  private String lithiumBatteryVoltage;
  
  /**  A field that stores the lithium battery voltage key as a String */
  public final String LITHIUM_BATTERY_VOLTAGE = "vlith";
  
  /*  A field that stores the operating current as a String */
  private String operatingCurrent;
  
  /**  A field that stores the operating current key as a String */
  public final String OPERATING_CURRENT = "ioper";
  
  /*  A field that stores the pump current as a String */
  private String pumpCurrent;
  
  /**  A field that stores the pump current key as a String */
  public final String PUMP_CURRENT = "ipump";
  
  /*  A field that stores the channel 0 and 1 external voltage currents as a String */
  private String channels01ExternalCurrent;
  
  /**  A field that stores the channel 0 and 1 external voltage currents key as a String */
  public final String CHANNELS_01_EXTERNAL_CURRENT = "iext01";
  
  /*  A field that stores the channel 2 and 3 external voltage currents as a String */
  private String channels23ExternalCurrent;
  
  /**  A field that stores the channel 2 and 3 external voltage currents key as a String */
  public final String CHANNELS_23_EXTERNAL_CURRENT = "iext23";
  
  /*  A field that stores the logging status as a String */
  private String loggingStatus;
  
  /**  A field that stores the logging status key as a String */
  public final String LOGGING_STATUS = "status";
  
  /*  A field that stores the number of scans to average as a String */
  private String numberOfScansToAverage;
  
  /**  A field that stores the number of scans to average key as a String */
  public final String NUMBER_OF_SCANS_TO_AVERAGE = "number of scans to average";
  
  /*  A field that stores the number of bytes in memory as a String */
  private String numberOfBytes;
  
  /**  A field that stores the number of samples key as a String */
  public final String NUMBER_OF_BYTES = "MemorySummary/Bytes";
  
  /*  A field that stores the number of samples as a String */
  private String numberOfSamples;
  
  /**  A field that stores the number of samples key as a String */
  public final String NUMBER_OF_SAMPLES = "samples";
  
  /*  A field that stores the number of available samples as a String */
  private String numberOfAvailableSamples;
  
  /**  A field that stores the number of available samples key as a String */
  public final String NUMBER_OF_AVAILABLE_SAMPLES = "free";
  
  /*  A field that stores the sample length in bytes as a String */
  private String sampleByteLength;
  
  /**  A field that stores the sample length in bytes key as a String */
  public final String SAMPLE_BYTE_LENGTH = "MemorySummary/SampleLength";
  
  /*  A field that stores the sample interval as a String */
  private String sampleInterval;
  
  /**  A field that stores the sample interval key as a String */
  public final String SAMPLE_INTERVAL = "sample interval";
  
  /*  A field that stores the number of measurements per sample as a String */
  private String measurementsPerSample;
  
  /**  A field that stores the number of measurements per sample key as a String */
  public final String MEASUREMENTS_PER_SAMPLE = "number of measurements per sample";
  
  /*  A field that stores the output salinity state as a String */
  private String outputSalinity;
  
  /**  A field that stores the output salinity state key as a String */
  public final String OUTPUT_SALINITY = "OutputSalinity";
  
  /*  A field that stores the output sound velocity state as a String */
  private String outputSoundVelocity;
  
  /**  A field that stores the output salinity state key as a String */
  public final String OUTPUT_SOUND_VELOCITY = "OutputSV";
  
  /*  A field that stores the transmit real-time state as a String */
  private String transmitRealtime;
  
  /**  A field that stores the transmit real-time state key as a String */
  public final String TRANSMIT_REALTIME = "transmit real-time";
  
  /*  A field that stores the number of casts as a String */
  private String numberOfCasts;
  
  /**  A field that stores the number of casts key as a String */
  public final String NUMBER_OF_CASTS = "casts";
  
  /*  A field that stores the minimum conductivity frequency as a String */
  private String minimumConductivityFrequency;
  
  /**  A field that stores the minimum conductivity frequency key as a String */
  public final String MINIMUM_CONDUCTIVITY_FREQUENCY = "minimum cond freq";
  
  /*  A field that stores the pump delay as a String */
  private String pumpDelay;
  
  /**  A field that stores the pump delay key as a String */
  public final String PUMP_DELAY = "pump delay";
  
  /*  A field that stores the automatic logging state as a String */
  private String automaticLogging;
  
  /**  A field that stores the automatic logging state key as a String */
  public final String AUTOMATIC_LOGGING = "autorun";
  
  /**  A field that stores the autonomous sampling state as a String */
  private String autonomousSampling;
  
  /**  A field that stores the autonomous sampling state key as a String */
  public final String AUTONOMOUS_SAMPLING = "AutonomousSampling";
  
  /*  A field that stores the ignore magnetic switch state as a String */
  private String ignoreMagneticSwitch;
  
  /**  A field that stores the ignore magnetic switch state key as a String */
  public final String IGNORE_MAGNETIC_SWITCH = "ignore magnetic switch";
  
  /*  A field that stores the battery type as a String */
  private String batteryType;
  
  /**  A field that stores the battery type key as a String */
  public final String BATTERY_TYPE = "battery type";
  
  /*  A field that stores the battery cutoff as a String */
  private String batteryCutoff;
  
  /**  A field that stores the battery cutoff key as a String */
  public final String BATTERY_CUTOFF = "battery cutoff";
  
  /*  A field that stores the pressure sensor type as a String */
  private String pressureSensorType;
  
  /**  A field that stores the pressure sensor type key as a String */
  public final String PRESSURE_SENSOR_TYPE = "pressure sensor";
  
  /*  A field that stores the pressure sensor range as a String */
  private String pressureSensorRange;
  
  /**  A field that stores the pressure sensor range key as a String */
  public final String PRESSURE_SENSOR_RANGE = "range";
  
  /*  A field that stores the SBE38 temperature sensor state as a String */
  private String sbe38TemperatureSensor;
  
  /**  A field that stores the SBE38 temperature sensor state key as a String */
  public final String SBE38_TEMPERATURE_SENSOR = "SBE 38";
  
  /*  A field that stores the gas tension device state as a String */
  private String gasTensionDevice;
  
  /**  A field that stores the gas tension device state key as a String */
  public final String GAS_TENSION_DEVICE = "Gas Tension Device";
  
  /*  A field that stores the external voltage channel 0 state as a String */
  private String externalVoltageChannelZero;
  
  /**  A field that stores the external voltage channel 0 state key as a String */
  public final String EXTERNAL_VOLTAGE_CHANNEL_ZERO = "Ext Volt 0";
  
  /*  A field that stores the external voltage channel 1 state as a String */
  private String externalVoltageChannelOne;
  
  /**  A field that stores the external voltage channel 1 state key as a String */
  public final String EXTERNAL_VOLTAGE_CHANNEL_ONE = "Ext Volt 1";
  
  /*  A field that stores the external voltage channel 2 state as a String */
  private String externalVoltageChannelTwo;
  
  /**  A field that stores the external voltage channel 2 state key as a String */
  public final String EXTERNAL_VOLTAGE_CHANNEL_TWO = "Ext Volt 2";
  
  /*  A field that stores the external voltage channel 3 state as a String */
  private String externalVoltageChannelThree;
  
  /**  A field that stores the external voltage channel 3 state key as a String */
  public final String EXTERNAL_VOLTAGE_CHANNEL_THREE = "Ext Volt 3";
  
  /*  A field that stores the echo commands state as a String */
  private String echoCommands;
  
  /**  A field that stores the echo commands state key as a String */
  public final String ECHO_COMMANDS = "echo commands";
  
  /*  A field that stores the output format as a String */
  private String outputFormat;
  
  /**  A field that stores the output format key as a String */
  public final String OUTPUT_FORMAT = "output format";
  
  /*  A field that stores the temperature calibration format as a String */
  private String temperatureCalibrationFormat;
  
  /**  A field that stores the temperature calibration format key as a String */
  public final String TEMPERATURE_CALIBRATION_FORMAT = "@format";
  
  /*  A field that stores the temperature calibration date as a String */
  private String temperatureCalibrationDate;
  
  /**  A field that stores the temperature calibration date key as a String */
  public final String TEMPERATURE_CALIBRATION_DATE = "temperature";
  
  /*  A field that stores the temperature coefficient TA0 as a String */
  private String temperatureCoefficientTA0;
  
  /**  A field that stores the temperature coefficient TA0 key as a String */
  public final String TEMPERATURE_COEFFICIENT_TA0 = "TA0";
  
  /*  A field that stores the temperature coefficient TA1 as a String */
  private String temperatureCoefficientTA1;
  
  /**  A field that stores the temperature coefficient TA1 key as a String */
  public final String TEMPERATURE_COEFFICIENT_TA1 = "TA1";
  
  /*  A field that stores the temperature coefficient TA2 as a String */
  private String temperatureCoefficientTA2;
  
  /**  A field that stores the temperature coefficient TA2 key as a String */
  public final String TEMPERATURE_COEFFICIENT_TA2 = "TA2";
  
  /*  A field that stores the temperature coefficient TA3 as a String */
  private String temperatureCoefficientTA3;
  
  /**  A field that stores the temperature coefficient TA3 key as a String */
  public final String TEMPERATURE_COEFFICIENT_TA3 = "TA3";
  
  /*  A field that stores the temperature offset coefficient as a String */
  private String temperatureOffsetCoefficient;
  
  /**  A field that stores the temperature offset coefficient key as a String */
  public final String TEMPERATURE_OFFSET_COEFFICIENT = "TOFFSET";
  
  /*  A field that stores the conductivity calibration format as a String */
  private String conductivityCalibrationFormat;
  
  /**  A field that stores the conductivity calibration format key as a String */
  public final String CONDUCTIVITY_CALIBRATION_FORMAT = "@format";
  
  /*  A field that stores the conductivity calibration date as a String */
  private String conductivityCalibrationDate;
  
  /**  A field that stores the conductivity calibration date key as a String */
  public final String CONDUCTIVITY_CALIBRATION_DATE = "conductivity";
  
  /*  A field that stores the conductivity coefficient G as a String */
  private String conductivityCoefficientG;
  
  /**  A field that stores the conductivity coefficient G key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_G = "G";
  
  /*  A field that stores the conductivity coefficient H as a String */
  private String conductivityCoefficientH;
  
  /**  A field that stores the conductivity coefficient H key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_H = "H";
  
  /*  A field that stores the conductivity coefficient I as a String */
  private String conductivityCoefficientI;
  
  /**  A field that stores the conductivity coefficient I key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_I = "I";
  
  /*  A field that stores the conductivity coefficient J as a String */
  private String conductivityCoefficientJ;
  
  /**  A field that stores the conductivity coefficient J key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_J = "J";
  
  /*  A field that stores the conductivity coefficient CF0 as a String */
  private String conductivityCoefficientCF0;
  
  /**  A field that stores the conductivity coefficient CF0 key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_CF0 = "CF0";
  
  /*  A field that stores the conductivity coefficient CPCOR as a String */
  private String conductivityCoefficientCPCOR;
  
  /**  A field that stores the conductivity coefficient CPCOR key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_CPCOR = "CPCOR";
  
  /*  A field that stores the conductivity coefficient CTCOR as a String */
  private String conductivityCoefficientCTCOR;
  
  /**  A field that stores the conductivity coefficient CTCOR key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_CTCOR = "CTCOR";
  
  /*  A field that stores the conductivity coefficient CSLOPE as a String */
  private String conductivityCoefficientCSLOPE;
  
  /**  A field that stores the conductivity coefficient CSLOPE key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_CSLOPE = "CSLOPE";
  
  /*  A field that stores the conductivity coefficient WBOTC as a String */
  private String conductivityCoefficientWBOTC;
  
  /**  A field that stores the conductivity coefficient WBOTC key as a String */
  public final String CONDUCTIVITY_COEFFICIENT_WBOTC = "WBOTC";
  
  /*  A field that stores the pressure serial number as a String */
  private String pressureSerialNumber;
  
  /**  A field that stores the pressure serial number key as a String */
  public final String PRESSURE_SERIAL_NUMBER = "pressure S/N";
  
  /*  A field that stores the pressure calibration date as a String */
  private String pressureCalibrationDate;
  
  /**  A field that stores the pressure calibration date key as a String */
  public final String PRESSURE_CALIBRATION_DATE = "pressure";
  
  /*  A field that stores the pressure calibration format as a String */
  private String pressureCalibrationFormat;
  
  /**  A field that stores the pressure calibration format key as a String */
  public final String PRESSURE_CALIBRATION_FORMAT = "@format";
  
  /*  A field that stores the pressure coefficient PA0 as a String */
  private String pressureCoefficientPA0;
  
  /**  A field that stores the pressure coefficient PA0 key as a String */
  public final String PRESSURE_COEFFICIENT_PA0 = "PA0";
  
  /*  A field that stores the pressure coefficient PA1 as a String */
  private String pressureCoefficientPA1;
  
  /**  A field that stores the pressure coefficient PA1 key as a String */
  public final String PRESSURE_COEFFICIENT_PA1 = "PA1";
  
  /*  A field that stores the pressure coefficient PA2 as a String */
  private String pressureCoefficientPA2;
  
  /**  A field that stores the pressure coefficient PA2 key as a String */
  public final String PRESSURE_COEFFICIENT_PA2 = "PA2";
  
  /*  A field that stores the pressure coefficient PTCA0 as a String */
  private String pressureCoefficientPTCA0;
  
  /**  A field that stores the pressure coefficient PTCA0 key as a String */
  public final String PRESSURE_COEFFICIENT_PTCA0 = "PTCA0";
  
  /*  A field that stores the pressure coefficient PTCA1 as a String */
  private String pressureCoefficientPTCA1;
  
  /**  A field that stores the pressure coefficient PTCA1 key as a String */
  public final String PRESSURE_COEFFICIENT_PTCA1 = "PTCA1";
  
  /*  A field that stores the pressure coefficient PTCA2 as a String */
  private String pressureCoefficientPTCA2;
  
  /**  A field that stores the pressure coefficient PTCA2 key as a String */
  public final String PRESSURE_COEFFICIENT_PTCA2 = "PTCA2";
  
  /*  A field that stores the pressure coefficient PTCB0 as a String */
  private String pressureCoefficientPTCB0;
  
  /**  A field that stores the pressure coefficient PTCB0 key as a String */
  public final String PRESSURE_COEFFICIENT_PTCB0 = "PTCB0";
  
  /*  A field that stores the pressure coefficient PTCB1 as a String */
  private String pressureCoefficientPTCB1;
  
  /**  A field that stores the pressure coefficient PTCB1 key as a String */
  public final String PRESSURE_COEFFICIENT_PTCB1 = "PTCB1";
  
  /*  A field that stores the pressure coefficient PTCB2 as a String */
  private String pressureCoefficientPTCB2;
  
  /**  A field that stores the pressure coefficient PTCB2 key as a String */
  public final String PRESSURE_COEFFICIENT_PTCB2 = "PTCB2";
  
  /*  A field that stores the pressure coefficient PTEMPA0 as a String */
  private String pressureCoefficientPTEMPA0;
  
  /**  A field that stores the pressure coefficient PTEMPA0 key as a String */
  public final String PRESSURE_COEFFICIENT_PTEMPA0 = "PTEMPA0";
  
  /*  A field that stores the pressure coefficient PTEMPA1 as a String */
  private String pressureCoefficientPTEMPA1;
  
  /**  A field that stores the pressure coefficient PTEMPA1 key as a String */
  public final String PRESSURE_COEFFICIENT_PTEMPA1 = "PTEMPA1";
  
  /*  A field that stores the pressure coefficient PTEMPA2 as a String */
  private String pressureCoefficientPTEMPA2;
  
  /**  A field that stores the pressure coefficient PTEMPA2 key as a String */
  public final String PRESSURE_COEFFICIENT_PTEMPA2 = "PTEMPA2";
  
  /*  A field that stores the pressure offset coefficient as a String */
  private String pressureOffsetCoefficient;
  
  /**  A field that stores the pressure offset coefficient key as a String */
  public final String PRESSURE_OFFSET_COEFFICIENT = "POFFSET";

  // TODO: add voltage offset fields.  What are they used for?
  // * volt 0: offset = -4.678210e-02, slope = 1.248624e+00
  // * volt 1: offset = -4.696105e-02, slope = 1.248782e+00
  // * volt 2: offset = -4.683263e-02, slope = 1.249537e+00
  // * volt 3: offset = -4.670842e-02, slope = 1.249841e+00
  // * EXTFREQSF = 1.000012e+00
   
  /*  A boolean field indicating if a pressure sensor is present on the instrument */
  private boolean hasPressure = false;
  
  /*  A boolean field indicating if a pump is present on the instrument */
  private boolean hasPump = false;
  
  /*  A boolean field indicating if the pressure sensor is a strain gauge sensor */
  private boolean hasStrainGaugePressure = false;
  
  /*  A boolean field indicating if external voltage channel zero is sampling */
  private boolean hasVoltageChannelZero = false;
  
  /*  A boolean field indicating if external voltage channel one is sampling */
  private boolean hasVoltageChannelOne = false;
  
  /*  A boolean field indicating if external voltage channel two is sampling */
  private boolean hasVoltageChannelTwo = false;
  
  /*  A boolean field indicating if external voltage channel three is sampling */
  private boolean hasVoltageChannelThree = false;
  
  /*  A boolean field indicating if there is an SBE38 temperature sensor */
  private boolean hasSBE38TemperatureSensor = false;
  
  /*  A boolean field indicating if there is a gas tension device */
  private boolean hasGasTensionDevice = false;

  /*  A field that stores the raw temperature field name as a string */
  public final String RAW_TEMPERATURE_FIELD_NAME = "temperatureCounts";  
  
  /*  A field that stores the raw conductivity field name as a string */
  public final String RAW_CONDUCTIVITY_FIELD_NAME = "conductivityFrequency";  
  
  /*  A field that stores the raw pressure field name as a string */
  public final String RAW_PRESSURE_FIELD_NAME = "pressureCounts";  
  
  /*  A field that stores the raw pressure temperature compensation field name as a string */
  public final String RAW_PRESSURE_TEMP_COMP_FIELD_NAME = "pressureTemperatureCompensationCounts";  
  
  /*  A field that stores the raw voltage channel zero field name as a string */
  public final String RAW_VOLTAGE_CHANNEL_ZERO_FIELD_NAME = "voltageChannelZero";  
  
  /*  A field that stores the raw voltage channel one field name as a string */
  public final String RAW_VOLTAGE_CHANNEL_ONE_FIELD_NAME = "voltageChannelOne";  
  
  /*  A field that stores the raw voltage channel two field name as a string */
  public final String RAW_VOLTAGE_CHANNEL_TWO_FIELD_NAME = "voltageChannelTwo";  
  
  /*  A field that stores the raw voltage channel zero field name as a string */
  public final String RAW_VOLTAGE_CHANNEL_THREE_FIELD_NAME = "voltageChannelThree";  
  
  /** An XML document object used to access CTD metadata reported in XML syntax */
  private Document xmlMetadata;
  
  /** 
   * The date format for the timestamp applied to the TChain sample 04 Aug 2008 09:15:01
   */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /**  The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
    
  /**
   *  Constructor:  Builds an empty CTDParser object that can be populated manually.
   *  This can be used to gradually build the CTDParser object from independent sections
   *  of metadata gethered from the CTD using commands such as GetCD, GetSD, GetCC, GetEC,
   *  and GetHD.  These commands are available in the Seabird firmare > 3.0f.
   */
  public CTDParser() {
    this.metadataValuesMap = new TreeMap<String, String>();
    this.dataValuesMap     = new TreeMap<Integer, String>();
    
  }
  
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
      
      // handle converted engineering data
      if ( this.outputFormat.equals("converted engineering") ) {
      
        // TODO: process engineering data
      }
      
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
  public void setMetadata() throws ParseException {
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
  
  /*
   *  A method used to parse the various XML metadata formats from the instrument.
   *  This method parses the output of the following instrument commands:
   *  GetCD, GetSD, GetCC, GetEC, and GetHD.
   *
   * @param xmlString - the CTD's XML output string containing the metadata values
   */
  public void setMetadata(String xmlString) throws ParseException {
    logger.debug("CTDParser.parseXMLMetadata() called.");
    
    try {
      // create an XML Document object from the instrument XML string
      XMLUtilities xmlUtil   = new XMLUtilities();
      StringReader xmlReader = new StringReader(xmlString);
      this.xmlMetadata = xmlUtil.getXMLReaderAsDOMDocument(xmlReader);
      logger.debug(xmlUtil.getDOMTreeAsXPathMap(xmlMetadata.getDocumentElement()));
      
      // set the configuration metadata fields
      if ( this.xmlMetadata.getDocumentElement().getTagName()
           .equals("ConfigurationData") ) {
        
        // Extract the metadata fields from the XML and populate the fields
        
        // set the device type field
        this.deviceType = 
          xmlUtil.getAttributeNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/@" + this.DEVICE_TYPE).getNodeValue().trim();
        logger.info("Device type is: " + this.deviceType);
        
        // set the instrument serial number field
        this.instrumentSerialNumber = 
          xmlUtil.getAttributeNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/@" + this.INSTRUMENT_SERIAL_NUMBER).getNodeValue().trim();
        logger.info("Instrument serial number is: " + this.instrumentSerialNumber);
        
        // set the pressure installed field
        this.pressureInstalled = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/" + this.PRESSURE_INSTALLED)
          .getFirstChild().getNodeValue().trim();
        logger.info("Pressure installed is: " + this.pressureInstalled);
          
        if (this.pressureInstalled.equals("yes") ) {
          this.hasPressure = true;
          
        }

        // set the pump installed field
        this.pumpInstalled = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/" + this.PUMP_INSTALLED)
          .getFirstChild().getNodeValue().trim();
        logger.info("Pump installed is: " + this.pumpInstalled);
        
        if (this.pumpInstalled.equals("yes") ) {
          this.hasPump = true;
        
        }
      
        // set the minimum conductivity frequency field
        this.minimumConductivityFrequency = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/" + this.MINIMUM_CONDUCTIVITY_FREQUENCY)
          .getFirstChild().getNodeValue().trim();
        logger.info("Minimum conductivity frequency is: " + this.minimumConductivityFrequency);
        
        // set the output format field
        this.outputFormat = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/SampleDataFormat")
          .getFirstChild().getNodeValue().trim();
        logger.info("Output format is: " + this.outputFormat);
        
        // set the output salinity state field
        this.outputSalinity = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/" + this.OUTPUT_SALINITY)
          .getFirstChild().getNodeValue().trim();
        logger.info("Output salinity state is: " + this.outputSalinity);
        
        // set the output sound velocity state field
        this.outputSoundVelocity = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/" + this.OUTPUT_SOUND_VELOCITY)
          .getFirstChild().getNodeValue().trim();
        logger.info("Output sound velocity state is: " + this.outputSoundVelocity);
        
        // set the output transmit real time state field
        this.transmitRealtime = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/TxRealTime")
          .getFirstChild().getNodeValue().trim();
        logger.info("Transmit real time state is: " + this.transmitRealtime);
        
        if (this.transmitRealtime.equals("yes") ) {
          
          // set the sampling mode to moored since profile mode doesn't support this
          this.samplingMode = "moored";
          logger.info("Sampling mode is: " + this.samplingMode);
          
        }
        
        // set the sample interval field
        this.sampleInterval = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/SampleInterval")
          .getFirstChild().getNodeValue().trim();
        logger.info("Sample interval is: " + this.sampleInterval);
        
        // set the synchronization mode state field
        this.synchronizationMode = 
          xmlUtil.getNodeWithXPath(xmlMetadata, 
          "//ConfigurationData/SyncMode")
          .getFirstChild().getNodeValue().trim();
        logger.info("Synchronization mode state state is: " + this.synchronizationMode);
        
      // add the status metadata fields to the metadataValuesMap
      } else if ( this.xmlMetadata.getDocumentElement().getTagName()
                  .equals("StatusData") ) {
         
         this.deviceType = 
           xmlUtil.getAttributeNodeWithXPath(xmlMetadata, 
           "//StatusData/@" + this.DEVICE_TYPE).getNodeValue().trim();
         logger.info("Device type is: " + this.deviceType);

         // set the instrument serial number field
         this.instrumentSerialNumber = 
           xmlUtil.getAttributeNodeWithXPath(xmlMetadata, 
           "//StatusData/@" + this.INSTRUMENT_SERIAL_NUMBER).getNodeValue().trim();
         logger.info("Instrument serial number is: " + this.instrumentSerialNumber);

         // set the instrument date time field
         this.instrumentDateTime = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.INSTRUMENT_DATE_TIME)
           .getFirstChild().getNodeValue().trim();
         logger.info("Instrument date time is: " + this.instrumentDateTime);
         
         // set the number of instrument events field
         this.numberOfInstrumentEvents = 
           xmlUtil.getAttributeNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.NUMBER_OF_INSTRUMENT_EVENTS)
           .getFirstChild().getNodeValue().trim();
         logger.info("Number of instrument events is: " + 
                     this.numberOfInstrumentEvents);
         
         // set the main battery voltage field
         this.mainBatteryVoltage = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/Power/vMain")
           .getFirstChild().getNodeValue().trim();
         logger.info("Main battery voltage is: " + this.mainBatteryVoltage);
      
         // set the lithium battery voltage field
         this.lithiumBatteryVoltage = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/Power/vLith")
           .getFirstChild().getNodeValue().trim();
         logger.info("Lithium battery voltage is: " + this.lithiumBatteryVoltage);
      
         // set the number of bytes in memory field
         this.numberOfBytes = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.NUMBER_OF_BYTES)
           .getFirstChild().getNodeValue().trim();
         logger.info("Number of bytes in memory is: " + this.numberOfBytes);
      
         // set the number of samples in memory field
         this.numberOfSamples = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.NUMBER_OF_SAMPLES)
           .getFirstChild().getNodeValue().trim();
         logger.info("Number of samples in memory is: " + this.numberOfSamples);
      
         // set the number of available samples in memory field
         this.numberOfAvailableSamples = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.NUMBER_OF_AVAILABLE_SAMPLES)
           .getFirstChild().getNodeValue().trim();
         logger.info("Number of available samples in memory is: " + 
                     this.numberOfAvailableSamples);
      
         // set the number of bytes per sample in memory field
         this.sampleByteLength = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.SAMPLE_BYTE_LENGTH)
           .getFirstChild().getNodeValue().trim();
         logger.info("Number of bytes per sample in memory is: " + 
                     this.sampleByteLength);
      
         // set the autonomous sampling state field
         this.autonomousSampling = 
           xmlUtil.getNodeWithXPath(xmlMetadata, 
           "//StatusData/" + this.AUTONOMOUS_SAMPLING)
           .getFirstChild().getNodeValue().trim();
         logger.info("Autonomous sampling state is: " + this.autonomousSampling);
      
      // add the calibration metadata fields to the metadataValuesMap
      } else if ( this.xmlMetadata.getDocumentElement().getTagName()
                    .equals("CalibrationCoefficients") ) {
        
        NodeList calibrationList = 
          xmlUtil.getNodeListWithXPath(this.xmlMetadata.getDocumentElement(), 
                                       "//CalibrationCoefficients/Calibration");
        
        // iterate through each calibration and set the metadata fields
        for (int itemNumber = 0; 
                 itemNumber < calibrationList.getLength(); 
                 itemNumber++ ) {
          Node calibrationNode = calibrationList.item(itemNumber);
          String idString = 
            xmlUtil.getAttributeNodeWithXPath(calibrationNode, "@id")
              .getNodeValue().trim();
          
          if ( idString.equals("Temperature") ) {
            
            // set the temperature calibration format field
            this.temperatureCalibrationFormat = 
              xmlUtil.getAttributeNodeWithXPath(calibrationNode, 
                this.TEMPERATURE_CALIBRATION_FORMAT).getNodeValue().trim();
            logger.info("Temperature calibration format is: " + 
                        this.temperatureCalibrationFormat);
            
            // set the temperature serial number field
            this.temperatureSerialNumber = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "SerialNum").getFirstChild().getNodeValue().trim();
            logger.info("Temperature serial number is: " + 
                        this.temperatureSerialNumber);
            
            // set the temperature calibration date field
            this.temperatureCalibrationDate = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "CalDate").getFirstChild().getNodeValue().trim();
            logger.info("Temperature calibration date is: " + 
                        this.temperatureCalibrationDate);
            
            // set the temperature calibration A0 field
            this.temperatureCoefficientTA0 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "A0").getFirstChild().getNodeValue().trim();
            logger.info("Temperature calibration A0 is: " + 
                        this.temperatureCoefficientTA0);
            
            // set the temperature calibration A1 field
            this.temperatureCoefficientTA1 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "A1").getFirstChild().getNodeValue().trim();
            logger.info("Temperature calibration A1 is: " + 
                        this.temperatureCoefficientTA1);
            
            // set the temperature calibration A2 field
            this.temperatureCoefficientTA2 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "A2").getFirstChild().getNodeValue().trim();
            logger.info("Temperature calibration A2 is: " + 
                        this.temperatureCoefficientTA2);
            
            // set the temperature calibration A3 field
            this.temperatureCoefficientTA3 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "A3").getFirstChild().getNodeValue().trim();
            logger.info("Temperature calibration A3 is: " + 
                        this.temperatureCoefficientTA3);
            
          } else if ( idString.equals("Conductivity") ) {
            
            // set the conductivity calibration format field
            this.conductivityCalibrationFormat = 
              xmlUtil.getAttributeNodeWithXPath(calibrationNode, 
                this.CONDUCTIVITY_CALIBRATION_FORMAT).getNodeValue().trim();
            logger.info("Conductivity calibration format is: " + 
                        this.conductivityCalibrationFormat);
            
            // set the conductivity serial number field
            this.conductivitySerialNumber = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "SerialNum").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity serial number is: " + 
                        this.conductivitySerialNumber);
            
            // set the conductivity calibration date field
            this.conductivityCalibrationDate = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "CalDate").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration date is: " + 
                        this.conductivityCalibrationDate);
            
            // set the conductivity calibration G field
            this.conductivityCoefficientG = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "G").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration G is: " + 
                        this.conductivityCoefficientG);
            
            // set the conductivity calibration H field
            this.conductivityCoefficientH = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "H").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration H is: " + 
                        this.conductivityCoefficientH);
            
            // set the conductivity calibration I field
            this.conductivityCoefficientI = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "I").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration I is: " + 
                        this.conductivityCoefficientI);
            
            // set the conductivity calibration J field
            this.conductivityCoefficientJ = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "J").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration J is: " + 
                        this.conductivityCoefficientJ);
            
            // set the conductivity calibration PCOR field
            this.conductivityCoefficientCPCOR = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PCOR").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration PCOR is: " + 
                        this.conductivityCoefficientCPCOR);

            // set the conductivity calibration TCOR field
            this.conductivityCoefficientCTCOR = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "TCOR").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration TCOR is: " + 
                        this.conductivityCoefficientCTCOR);
            
            // set the conductivity calibration WBOTC field
            this.conductivityCoefficientWBOTC = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "WBOTC").getFirstChild().getNodeValue().trim();
            logger.info("Conductivity calibration WBOTC is: " + 
                        this.conductivityCoefficientWBOTC);

          } else if ( idString.equals("Pressure") ) {
            
            // set the pressure calibration format field
            this.pressureCalibrationFormat = 
              xmlUtil.getAttributeNodeWithXPath(calibrationNode, 
                this.PRESSURE_CALIBRATION_FORMAT).getNodeValue().trim();
            logger.info("Pressure calibration format is: " + 
                        this.pressureCalibrationFormat);
            
            // set the pressure serial number field
            this.pressureSerialNumber = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "SerialNum").getFirstChild().getNodeValue().trim();
            logger.info("Pressure serial number is: " + 
                        this.pressureSerialNumber);
            
            // set the pressure calibration date field
            this.pressureCalibrationDate = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "CalDate").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration date is: " + 
                        this.pressureCalibrationDate);
            
            // set the pressure calibration PA0 field
            this.pressureCoefficientPA0 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PA0").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PA0 is: " + 
                        this.pressureCoefficientPA0);
            
            // set the pressure calibration PA1 field
            this.pressureCoefficientPA1 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PA1").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PA1 is: " + 
                        this.pressureCoefficientPA1);
            
            // set the pressure calibration PA2 field
            this.pressureCoefficientPA2 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PA2").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PA2 is: " + 
                        this.pressureCoefficientPA2);
            
            // set the pressure calibration PTCA0 field
            this.pressureCoefficientPTCA0 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTCA0").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTCA0 is: " + 
                        this.pressureCoefficientPTCA0);
            
            // set the pressure calibration PTCA1 field
            this.pressureCoefficientPTCA1 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTCA1").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTCA1 is: " + 
                        this.pressureCoefficientPTCA1);

            // set the pressure calibration PTCA2 field
            this.pressureCoefficientPTCA2 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTCA2").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTCA2 is: " + 
                        this.pressureCoefficientPTCA2);
            
            // set the pressure calibration PTCB0 field
            this.pressureCoefficientPTCB0 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTCB0").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTCB0 is: " + 
                        this.pressureCoefficientPTCB0);
            
            // set the pressure calibration PTCB1 field
            this.pressureCoefficientPTCB1 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTCB1").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTCB1 is: " + 
                        this.pressureCoefficientPTCB1);
            
            // set the pressure calibration PTCB2 field
            this.pressureCoefficientPTCB2 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTCB2").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTCB2 is: " + 
                        this.pressureCoefficientPTCB2);

            // set the pressure calibration PTEMPA0 field
            this.pressureCoefficientPTEMPA0 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTEMPA0").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTEMPA0 is: " + 
                        this.pressureCoefficientPTEMPA0);
            
            // set the pressure calibration PTEMPA1 field
            this.pressureCoefficientPTEMPA1 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTEMPA1").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTEMPA1 is: " + 
                        this.pressureCoefficientPTEMPA1);
            
            // set the pressure calibration PTEMPA2 field
            this.pressureCoefficientPTEMPA2 = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PTEMPA2").getFirstChild().getNodeValue().trim();
            logger.info("Pressure calibration PTEMPA2 is: " + 
                        this.pressureCoefficientPTEMPA2);

            // set the pressure calibration POFFSET field
            this.pressureOffsetCoefficient = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "POFFSET").getFirstChild().getNodeValue().trim();
            logger.info("Pressure offset calibration is: " + 
                        this.pressureOffsetCoefficient);
            
            // set the pressure sensor range field
            this.pressureSensorRange = 
              xmlUtil.getNodeWithXPath(calibrationNode, 
                "PRANGE").getFirstChild().getNodeValue().trim();
            logger.info("Pressure sensor range is: " + 
                        this.pressureSensorRange);

          } else {
            throw new ParseException(
              "There was an error parsing the XML string. "   +
              "The calibration coefficient was not recognized. " +
              "The identifier was: " + idString, 0);
            
          }
        }
        
      // add the event metadata fields to the metadataValuesMap
      } else if ( this.xmlMetadata.getDocumentElement().getTagName().equals("EventCounters") ) {
        
      // add the hardware metadata fields to the metadataValuesMap
      } else if ( this.xmlMetadata.getDocumentElement().getTagName().equals("HardwareData") ) {
        
      } else {
        throw new ParseException("The XML metadata is not recognized.", 0);
      }
    
    } catch ( IOException ioe ) {
      logger.info("There was an error reading the XML metadata. " +
        "The error message was: " + ioe.getMessage());
      throw new ParseException(ioe.getMessage(), 0);
      
    } catch ( TransformerException te ) {
      logger.info("There was an error creating the XML configuration. " +
        "The error message was: " + te.getMessage());
        throw new ParseException(te.getMessage(), 0);
      
    }
    
  }                                                  
  
  /**¬
   * A method that returns the SamplingMode field
   */
  public String getSamplingMode() {
    return this.samplingMode;
  }
  
  /**
   * A method that returns the FirstSampleTime field
   */
  public String getFirstSampleTime() {
    return this.firstSampleTime;
  }
  
  /**
   * A method that returns the FileName field
   */
  public String getFileName() {
    return this.fileName;
  }
  
  /**
   * A method that returns the TemperatureSerialNumber field
   */
  public String getTemperatureSerialNumber() {
    return this.temperatureSerialNumber;
  }
  
  /**
   * A method that returns the ConductivitySerialNumber field
   */
  public String getConductivitySerialNumber() {
    return this.conductivitySerialNumber;
  }
  
  /**
   * A method that returns the SystemUpLoadTime field
   */
  public String getSystemUpLoadTime() {
    return this.systemUpLoadTime;
  }
  
  /**
   * A method that returns the CruiseInformation field
   */
  public String getCruiseInformation() {
    return this.cruiseInformation;
  }
  
  /**
   * A method that returns the StationInformation field
   */
  public String getStationInformation() {
    return this.stationInformation;
  }
  
  /**
   * A method that returns the ShipInformation field
   */
  public String getShipInformation() {
    return this.shipInformation;
  }
  
  /**
   * A method that returns the ChiefScientist field
   */
  public String getChiefScientist() {
    return this.chiefScientist;
  }
  
  /**
   * A method that returns the Organization field
   */
  public String getOrganization() {
    return this.organization;
  }
  
  /**
   * A method that returns the AreaOfOperation field
   */
  public String getAreaOfOperation() {
    return this.areaOfOperation;
  }
  
  /**
   * A method that returns the InstrumentPackage field
   */
  public String getInstrumentPackage() {
    return this.instrumentPackage;
  }
  
  /**
   * A method that returns the MooringNumber field
   */
  public String getMooringNumber() {
    return this.mooringNumber;
  }
  
  /**
   * A method that returns the InstrumentLatitude field
   */
  public String getInstrumentLatitude() {
    return this.instrumentLatitude;
  }
  
  /**
   * A method that returns the InstrumentLongitude field
   */
  public String getInstrumentLongitude() {
    return this.instrumentLongitude;
  }
  
  /**
   * A method that returns the DepthSounding field
   */
  public String getDepthSounding() {
    return this.depthSounding;
  }
  
  /**
   * A method that returns the ProfileNumber field
   */
  public String getProfileNumber() {
    return this.profileNumber;
  }
  
  /**
   * A method that returns the ProfileDirection field
   */
  public String getProfileDirection() {
    return this.profileDirection;
  }
  
  /**
   * A method that returns the DeploymentNotes field
   */
  public String getDeploymentNotes() {
    return this.deploymentNotes;
  }
  
  /**
   * A method that returns the MainBatteryVoltage field
   */
  public String getMainBatteryVoltage() {
    return this.mainBatteryVoltage;
  }
  
  /**
   * A method that returns the LithiumBatteryVoltage field
   */
  public String getLithiumBatteryVoltage() {
    return this.lithiumBatteryVoltage;
  }
  
  /**
   * A method that returns the OperatingCurrent field
   */
  public String getOperatingCurrent() {
    return this.operatingCurrent;
  }
  
  /**
   * A method that returns the PumpCurrent field
   */
  public String getPumpCurrent() {
    return this.pumpCurrent;
  }
  
  /**
   * A method that returns the Channels01ExternalCurrent field
   */
  public String getChannels01ExternalCurrent() {
    return this.channels01ExternalCurrent;
  }
  
  /**
   * A method that returns the Channels23ExternalCurrent field
   */
  public String getChannels23ExternalCurrent() {
    return this.channels23ExternalCurrent;
  }
  
  /**
   * A method that returns the LoggingStatus field
   */
  public String getLoggingStatus() {
    return this.loggingStatus;
  }
  
  /**
   * A method that returns the NumberOfScansToAverage field
   */
  public String getNumberOfScansToAverage() {
    return this.numberOfScansToAverage;
  }
  
  /**
   * A method that returns the NumberOfSamples field
   */
  public String getNumberOfSamples() {
    return this.numberOfSamples;
  }
  
  /**
   * A method that returns the NumberOfAvailableSamples field
   */
  public String getNumberOfAvailableSamples() {
    return this.numberOfAvailableSamples;
  }
  
  /**
   * A method that returns the SampleInterval field
   */
  public String getSampleInterval() {
    return this.sampleInterval;
  }
  
  /**
   * A method that returns the MeasurementsPerSample field
   */
  public String getMeasurementsPerSample() {
    return this.measurementsPerSample;
  }
  
  /**
   * A method that returns the TransmitRealtime field
   */
  public String getTransmitRealtime() {
    return this.transmitRealtime;
  }
  
  /**
   * A method that returns the NumberOfCasts field
   */
  public String getNumberOfCasts() {
    return this.numberOfCasts;
  }
  
  /**
   * A method that returns the MinimumConductivityFrequency field
   */
  public String getMinimumConductivityFrequency() {
    return this.minimumConductivityFrequency;
  }
  
  /**
   * A method that returns the PumpDelay field
   */
  public String getPumpDelay() {
    return this.pumpDelay;
  }
  
  /**
   * A method that returns the AutomaticLogging field
   */
  public String getAutomaticLogging() {
    return this.automaticLogging;
  }
  
  /**
   * A method that returns the IgnoreMagneticSwitch field
   */
  public String getIgnoreMagneticSwitch() {
    return this.ignoreMagneticSwitch;
  }
  
  /**
   * A method that returns the BatteryType field
   */
  public String getBatteryType() {
    return this.batteryType;
  }
  
  /**
   * A method that returns the BatteryCutoff field
   */
  public String getBatteryCutoff() {
    return this.batteryCutoff;
  }
  
  /**
   * A method that returns the PressureSensorType field
   */
  public String getPressureSensorType() {
    return this.pressureSensorType;
  }
  
  /**
   * A method that returns the PressureSensorRange field
   */
  public String getPressureSensorRange() {
    return this.pressureSensorRange;
  }
  
  /**
   * A method that returns the Sbe38TemperatureSensor field
   */
  public String getSbe38TemperatureSensor() {
    return this.sbe38TemperatureSensor;
  }
  
  /**
   * A method that returns the GasTensionDevice field
   */
  public String getGasTensionDevice() {
    return this.gasTensionDevice;
  }
  
  /**
   * A method that returns the ExternalVoltageChannelZero field
   */
  public String getExternalVoltageChannelZero() {
    return this.externalVoltageChannelZero;
  }
  
  /**
   * A method that returns the ExternalVoltageChannelOne field
   */
  public String getExternalVoltageChannelOne() {
    return this.externalVoltageChannelOne;
  }
  
  /**
   * A method that returns the ExternalVoltageChannelTwo field
   */
  public String getExternalVoltageChannelTwo() {
    return this.externalVoltageChannelTwo;
  }
  
  /**
   * A method that returns the ExternalVoltageChannelThree field
   */
  public String getExternalVoltageChannelThree() {
    return this.externalVoltageChannelThree;
  }
  
  /**
   * A method that returns the EchoCommands field
   */
  public String getEchoCommands() {
    return this.echoCommands;
  }
  
  /**
   * A method that returns the OutputFormat field
   */
  public String getOutputFormat() {
    return this.outputFormat;
  }
  
  /**
   * A method that returns the TemperatureCalibrationDate field
   */
  public String getTemperatureCalibrationDate() {
    return this.temperatureCalibrationDate;
  }
  
  /**
   * A method that returns the TemperatureCoefficientTA0 field
   */
  public double getTemperatureCoefficientTA0() {
    return new Double(this.temperatureCoefficientTA0).doubleValue();
  }
  
  /**
   * A method that returns the TemperatureCoefficientTA1 field
   */
  public double getTemperatureCoefficientTA1() {
    return new Double(this.temperatureCoefficientTA1).doubleValue();
  }
  
  /**
   * A method that returns the TemperatureCoefficientTA2 field
   */
  public double getTemperatureCoefficientTA2() {
    return new Double(this.temperatureCoefficientTA2).doubleValue();
  }
  
  /**
   * A method that returns the TemperatureCoefficientTA3 field
   */
  public double getTemperatureCoefficientTA3() {
    return new Double(this.temperatureCoefficientTA3).doubleValue();
  }
  
  /**
   * A method that returns the TemperatureOffsetCoefficient field
   */
  public double getTemperatureOffsetCoefficient() {
    return new Double(this.temperatureOffsetCoefficient).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCalibrationDate field
   */
  public String getConductivityCalibrationDate() {
    return this.conductivityCalibrationDate;
  }
  
  /**
   * A method that returns the ConductivityCoefficientG field
   */
  public double getConductivityCoefficientG() {
    return new Double(this.conductivityCoefficientG).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientH field
   */
  public double getConductivityCoefficientH() {
    return new Double(this.conductivityCoefficientH).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientI field
   */
  public double getConductivityCoefficientI() {
    return new Double(this.conductivityCoefficientI).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientJ field
   */
  public double getConductivityCoefficientJ() {
    return new Double(this.conductivityCoefficientJ).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientCF0 field
   */
  public double getConductivityCoefficientCF0() {
    return new Double(this.conductivityCoefficientCF0).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientCPCOR field
   */
  public double getConductivityCoefficientCPCOR() {
    return new Double(this.conductivityCoefficientCPCOR).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientCTCOR field
   */
  public double getConductivityCoefficientCTCOR() {
    return new Double(this.conductivityCoefficientCTCOR).doubleValue();
  }
  
  /**
   * A method that returns the ConductivityCoefficientCSLOPE field
   */
  public double getConductivityCoefficientCSLOPE() {
    return new Double(this.conductivityCoefficientCSLOPE).doubleValue();
  }
  
  /**
   * A method that returns the PressureSerialNumber field
   */
  public int getPressureSerialNumber() {
    return new Integer(this.pressureSerialNumber).intValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPA0 field
   */
  public double getPressureCoefficientPA0() {
    return new Double(this.pressureCoefficientPA0).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPA1 field
   */
  public double getPressureCoefficientPA1() {
    return new Double(this.pressureCoefficientPA1).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPA2 field
   */
  public double getPressureCoefficientPA2() {
    return new Double(this.pressureCoefficientPA2).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTCA0 field
   */
  public double getPressureCoefficientPTCA0() {
    return new Double(this.pressureCoefficientPTCA0).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTCA1 field
   */
  public double getPressureCoefficientPTCA1() {
    return new Double(this.pressureCoefficientPTCA1).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTCA2 field
   */
  public double getPressureCoefficientPTCA2() {
    return new Double(this.pressureCoefficientPTCA2).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTCB0 field
   */
  public double getPressureCoefficientPTCB0() {
    return new Double(this.pressureCoefficientPTCB0).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTCB1 field
   */
  public double getPressureCoefficientPTCB1() {
    return new Double(this.pressureCoefficientPTCB1).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTCB2 field
   */
  public double getPressureCoefficientPTCB2() {
    return new Double(this.pressureCoefficientPTCB2).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTEMPA0 field
   */
  public double getPressureCoefficientPTEMPA0() {
    return new Double(this.pressureCoefficientPTEMPA0).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTEMPA1 field
   */
  public double getPressureCoefficientPTEMPA1() {
    return new Double(this.pressureCoefficientPTEMPA1).doubleValue();
  }
  
  /**
   * A method that returns the PressureCoefficientPTEMPA2 field
   */
  public double getPressureCoefficientPTEMPA2() {
    return new Double(this.pressureCoefficientPTEMPA2).doubleValue();
  }
  
  /**
   * A method that returns the PressureOffsetCoefficient field
   */
  public double getPressureOffsetCoefficient() {
    return new Double(this.pressureOffsetCoefficient).doubleValue();
  }
  
  /**
   * The main entry point method
   */
  public static void main(String[] args) {
    
    String file = 
      "* Sea-Bird SBE19plus Data File:\r\n" +
      "* FileName = sh__0002.hex\r\n" +
      "* Software Version \r\n" +
      "* Temperature SN =  5251\r\n" +
      "* Conductivity SN =  5251\r\n" +
      "* System UpLoad Time = Sep 23 2009 11:29:01\r\n" +
      "** Cruise: Sea Engineering C&C Project\r\n" +
      "** Station: Kilo Nalu, 20meter[D[D[D[D[D meter site \r\n" +
      "** Ship: Huki Pau, Sea Engineering\r\n" +
      "** Chief_Scientist: McManus\r\n" +
      "** Organization: UH\r\n" +
      "** Area_of_Operation: Offshore Kewalo Basin\r\n" +
      "** Package: SH2\r\n" +
      "** Mooring_Number: N/A\r\n" +
      "** Latitude: \r\n" +
      "** Longitude: \r\n" +
      "** Sounding: 20.6 m\r\n" +
      "** Profile_Number: 2\r\n" +
      "** Profile_Direction: up\r\n" +
      "** Notes: \r\n" +
      "* ds\r\n" +
      "* SeacatPlus V 1.6b  SERIAL NO. 5251    23 Sep 2009  11:29:02\r\n" +
      "* vbatt = 12.0, vlith =  8.4, ioper =  61.8 ma, ipump = 134.7 ma, \r\n" +
      "* iext01 =   5.4 ma\r\n" +
      "* iext23 =  77.8 ma\r\n" +
      "* \r\n" +
      "* status = not logging\r\n" +
      "* number of scans to average = 1\r\n" +
      "* samples = 493446, free = 1, casts = 1\r\n" +
      "* mode = profile, minimum cond freq = 3000, pump delay = 20 sec\r\n" +
      "* autorun = no, ignore magnetic switch = yes\r\n" +
      "* battery type = alkaline, battery cutoff =  7.3 volts\r\n" +
      "* pressure sensor = strain gauge, range = 508.0\r\n" +
      "* SBE 38 = no, Gas Tension Device = no\r\n" +
      "* Ext Volt 0 = yes, Ext Volt 1 = no, Ext Volt 2 = yes, Ext Volt 3 = yes\r\n" +
      "* echo commands = yes\r\n" +
      "* output format = raw HEX\r\n" +
      "* S>\r\n" +
      "* dcal\r\n" +
      "* SeacatPlus V 1.6b  SERIAL NO. 5251    23 Sep 2009  11:29:08\r\n" +
      "* temperature:  09-oct-07\r\n" +
      "*     TA0 = 1.276108e-03\r\n" +
      "*     TA1 = 2.615414e-04\r\n" +
      "*     TA2 = -1.590756e-07\r\n" +
      "*     TA3 = 1.496275e-07\r\n" +
      "*     TOFFSET = 0.000000e+00\r\n" +
      "* conductivity:  09-oct-07\r\n" +
      "*     G = -1.016034e+00\r\n" +
      "*     H = 1.583915e-01\r\n" +
      "*     I = -5.990283e-04\r\n" +
      "*     J = 7.154628e-05\r\n" +
      "*     CF0 = 2.541437e+03\r\n" +
      "*     CPCOR = -9.570000e-08\r\n" +
      "*     CTCOR = 3.250000e-06\r\n" +
      "*     CSLOPE = 1.000000e+00\r\n" +
      "* pressure S/N = 2458922, range = 508 psia:  04-oct-07\r\n" +
      "*     PA0 = 3.174352e-01\r\n" +
      "*     PA1 = 1.542191e-03\r\n" +
      "*     PA2 = 6.554083e-12\r\n" +
      "*     PTCA0 = 5.251525e+05\r\n" +
      "*     PTCA1 = 1.352052e+01\r\n" +
      "*     PTCA2 = -1.455316e-01\r\n" +
      "*     PTCB0 = 2.558650e+01\r\n" +
      "*     PTCB1 = -1.500000e-03\r\n" +
      "*     PTCB2 = 0.000000e+00\r\n" +
      "*     PTEMPA0 = -5.795848e+01\r\n" +
      "*     PTEMPA1 = 5.427466e+01\r\n" +
      "*     PTEMPA2 = -5.522354e-01\r\n" +
      "*     POFFSET = 0.000000e+00\r\n" +
      "* volt 0: offset = -4.678210e-02, slope = 1.248624e+00\r\n" +
      "* volt 1: offset = -4.696105e-02, slope = 1.248782e+00\r\n" +
      "* volt 2: offset = -4.683263e-02, slope = 1.249537e+00\r\n" +
      "* volt 3: offset = -4.670842e-02, slope = 1.249841e+00\r\n" +
      "*     EXTFREQSF = 1.000012e+00\r\n" +
      "* S>\r\n" +
      "** First Sample Time: 23 Sep 2009  11:29:15\r\n" +
      "*END*\r\n" +
      "03B7DA1909A2086F85510B6E950574D157\r\n" +
      "03B7DB1909A3086FC9510C6E9A05B2E8FD\r\n" +
      "03B7DF1909A9087032510B6EB3059DFCB5\r\n" +
      "03B7E51909A9087090510B6F1B05C7E0FD\r\n" +
      "03B7E61909A30870AD510C6FEE05A5E723\r\n" +
      "03B7E51909A308708D510B709D05A2F4CF\r\n" +
      "03B7E31909A308705F510C70D305C9E81B\r\n" +
      "03B7E11909A9087039510B70A905E5E808\r\n" +
      "03B7DC19099D087027510B708805BCF033\r\n" +
      "03B7D91909A9087038510C703405D4EAF3\r\n" +
      "03B7DC1909A9087077510C6FA80593E975\r\n" +
      "03B7DE1909A30870CD510D6F15055CEDE5\r\n" +
      "03B7E11909A3087137510B6EA40573EC14\r\n" +
      "03B7E11909A908719C510E6E1505A9EAA6\r\n" +
      "03B7E31909AF0871D7510C6D98058CECCF\r\n" +
      "03B7E21909A30871D6510C6D4E0566EC65\r\n" +
      "03B7E21909A908719E510B6CDD0586EB59\r\n" +
      "03B7E11909AC087153510C6C6A05D1EC54\r\n" +
      "03B7DE1909AF08711D510E6BFE059BEC44\r\n" +
      "03B7D91909A3087112510D6BE005AFEBAA\r\n" +
      "03B7D91909A3087141510E6BE9056AEC19\r\n" +
      "03B7E01909A308718C510B6C000580EC27\r\n" +
      "03B7E11909A30871B3510C6C040577EBAD\r\n" +
      "03B7E21909A3087159510D6C180565EBC6\r\n" +
      "03B7E21909A30870AA510D6C410570EBD8\r\n" +
      "03B7E21909A9086FDB510E6C8305A3EBB3\r\n" +
      "03B7DF1909A9086F5E510B6D7905B2EBF1\r\n" +
      "03B7DB1909A3086F3B510E6EA9056DEBFC\r\n" +
      "03B7DA1909A3086F5A510D6FB30565EBE1\r\n" +
      "03B7E019099D086FA9510D701D056DEBF5\r\n" +
      "03B7E219099D08700D510F708505C1EBF2\r\n" +
      "03B7E319099D08704E510E711B05BFEBDE\r\n" +
      "03B7E219099D08704C510D71D7055FEBCC\r\n" +
      "03B7E31909A308702C510E72320573EBD9\r\n" +
      "03B7E219099D087017510E71EB05B1EBD8\r\n" +
      "03B7E41909A3087014510D713405A0EBCE\r\n" +
      "03B7E0190992087021510E702E059CEBD4\r\n" +
      "03B7E119099708703D510E6EFD057EEBDB\r\n" +
      "03B7E4190997087069510D6DDE0594EBCF\r\n" +
      "03B7E41909970870A3510D6CFD05AFEBC6\r\n" +
      "03B7E81909970870D6510E6C6605A6EBCC\r\n" +
      "03B7E819099D0870DA510F6C10057CEBD6\r\n" +
      "03B7EA19099D0870B7510F6BB5057CEBD5\r\n" +
      "03B7E819099708708E510F6B5805B6EBCC\r\n" +
      "03B7E719099B087086510D6AAF058BEBC3\r\n" +
      "03B7E919098C0870B3510E6A230569EA83\r\n" +
      "03B7E819098C087108510E69A40575EAA4\r\n" +
      "03B7EE190992087170510F69BC0585EB83\r\n" +
      "03B7F11909920871DB510D6A4D059FEA8B\r\n" +
      "03B7F419099D087219510F6AF0056DEAEC\r\n" +
      "03B7F219099708721C510E6B4F0558EBAD\r\n" +
      "03B7F41909920871C9510F6B5E0576EBCF\r\n" +
      "03B7F21909AF087149510D6B5D05A2EBEC\r\n" +
      "03B7EE1909A30870D3510D6C4F05CAEBED\r\n" +
      "03B7EA1909A3087091510E6DEF05CCEBFF\r\n" +
      "03B7E819099D087075510D6F6505C0EBEF\r\n" +
      "03B7E619099D08707F510E703C05EBEBAD\r\n" +
      "03B7E519099D087092510E706905DAEBE2\r\n" +
      "03B7E61909A3087086510F702E0629EBFB\r\n" +
      "03B7E819099D087043510E6FAD060AEBF3\r\n" +
      "03B7E519099D086FCE51106F3F059DEBF8\r\n" +
      "03B7E11909A9086F4E51106EE40596EBF8\r\n" +
      "03B7DE190992086EED510F6E7B05B2EC00\r\n" +
      "03B7DE190992086EC7510F6DF405E2EC02\r\n" +
      "03B7DA190997086EE4510D6D7005A1EC06\r\n" +
      "03B7DD190992086F3851106CF4056AEC07\r\n" +
      "03B7E419098C086FCC510F6C9A056EEBFD\r\n" +
      "03B7E719098C087082510F6D0D057EEC0E\r\n" +
      "03B7EE1909970870D0510E6EB10552EBFB\r\n" +
      "03B7F1190997087098511070530552EBFF\r\n" +
      "03B7ED1909A608701E510E715905B2EC08\r\n" +
      "03B7EB19099D086FD2510E72540581EBF5\r\n" +
      "03B7EA190997086FC8511173370568EBE8\r\n" +
      "03B7E5190992086FF0510F73AA0577EC08\r\n" +
      "03B7ED19098608702C510F73A10581EC26\r\n" +
      "03B7EE19098608706B5110736A0564EC17\r\n" +
      "03B7EF19098608709D510D72DF059FEC0F\r\n" +
      "03B7F01909970870B6510F723B056FEBFB\r\n" +
      "03B7F21909970870CC5110716305C8EB5C\r\n" +
      "03B7F31909970870E8510F706005D4EB45\r\n" +
      "03B7F019099208711751106F4505B4EBC4\r\n" +
      "03B7F0190997087161510F6E230581EBD9\r\n" +
      "03B7F11909C00871B751106D32057AEBEC\r\n" +
      "03B7E31909CC0871F7510F6CF305A9EBD5\r\n" +
      "03B7D91909A30871FD510F6E7305CAEBD2\r\n" +
      "03B7E01909520871C1510F710E05AEEB74\r\n" +
      "03B7FB19096308716E510F73B205A2EB62\r\n" +
      "03B81019094C08712D511075E705C5EB0A\r\n" +
      "03B81E19094C0871155111779005A2EB7B\r\n" +
      "03B82419096908711C511178C80591EB6D\r\n" +
      "03B82219099D087120511179C60597EB65\r\n" +
      "03B80D1909BA0870E651107AA1059AEB85\r\n" +
      "03B7F71909DD08706F51117B5E05A3EB97\r\n" +
      "03B7DE190A29086FF151107BDC0596EBA0\r\n" +
      "03B7B7190A69086F9651117C2A059BEBA2\r\n" +
      "03B78C190A69086F83510F7C6605ABEBA5\r\n" +
      "03B76C190A69086F9F510F7CC405A8EB5D\r\n" +
      "03B75E190A74086FE2510F7D3505A2EB43\r\n" +
      "03B758190A5108703551107DA205A6EB4E\r\n" +
      "03B75B190A1208706151107DC905CAEB82\r\n" +
      "03B772190A2308703A51117DD505BBEB94\r\n" +
      "03B781190A4C086FDF51117DDB05ACEB79\r\n" +
      "03B779190A7A086F97510F7DF605B3EB75\r\n" +
      "03B766190A86086F8551107E0005B4EB6E\r\n" +
      "03B753190A74086F9F51117DF3059AEB2B\r\n" +
      "03B74F190A69086FE2510F7DF5059CEB50\r\n" +
      "03B750190A7408703D510F7DFF05ABEB63\r\n" +
      "03B750190A690870A051107E0C0595EB39\r\n" +
      "03B754190A4C0870DB510E7E2705AFEB46\r\n" +
      "03B75D190A400870F051107E3305ADEB54\r\n" +
      "03B76B190A340870E0510E7E30058AEB63\r\n" +
      "03B776190A230870CD510F7E2105A3EB6D\r\n" +
      "03B7831909F40870D851107E1505AEEBA6\r\n" +
      "03B7941909EF0870F651137E100596EB97\r\n" +
      "03B7A71909E308711851117E1D059FEBBB\r\n" +
      "03B7AF1909EF08712651107E3B0587EBA8\r\n" +
      "03B7B41909EF08712351107E580592EBAC\r\n" +
      "03B7B51909EF08711E51117E7E058DEB88\r\n" +
      "03B7B4190A0C08711F51107E8B057BEB8D\r\n" +
      "03B7AC190A1708711951117E900589EBB3\r\n" +
      "03B7A2190A170870F851127E730584EBB2\r\n" +
      "03B798190A1D0870B8510F7E590590EBB5\r\n" +
      "03B797190A2C08707351117E250571EB84\r\n" +
      "03B78F190A3408704551117E0D0573EB99\r\n" +
      "03B78A190A4608703B51107DFD058CEAF6\r\n" +
      "03B77C190A91087045510F7E150589EAED\r\n" +
      "03B759190A5D08705051117E3A057CEB52\r\n" +
      "03B751190A9108704951117E550584EBA0\r\n" +
      "03B74A190A9708703151107E68059CEBAC\r\n" +
      "03B73D190A8008702051117E750587EBCB\r\n" +
      "03B73D190A6908702651107E7D0578EBCE\r\n" +
      "03B748190A5108703951107E880595EBB9\r\n" +
      "03B756190A4608704A51117E990595EBC7\r\n" +
      "03B760190A3408705051117EC2059AEBE0\r\n" +
      "03B771190A2F08705A51117ED1057DEBB0\r\n" +
      "03B77C190A3408707A51127ED60591EBC0\r\n" +
      "03B77D190A3A0870AF51107EDD056AEBBE\r\n" +
      "03B77C190A400870ED510F7ED60576EB95\r\n" +
      "03B77A190A46087126510E7EC6058AEB88\r\n" +
      "03B778190A3408714151127EC80588EB90\r\n" +
      "03B77B190A2F08712551107EBF0588EB95\r\n" +
      "03B780190A230870E651107E9D0585EBE0\r\n" +
      "03B786190A3408709E51117E7D059DEBDA\r\n" +
      "03B786190A3408706F51117E55057AEBD1\r\n" +
      "03B784190A2308705B51117E3F0594EBD5\r\n" +
      "03B788190A1708704751107E5F0589EBB2\r\n" +
      "03B78E190A1508703451127E5B0587EB98\r\n" +
      "03B792190A2308703351107E650573EBD0\r\n" +
      "03B794190A3408705C51117E7B0563EBCC\r\n" +
      "03B78F190A2F08708D51117E8D0585EBC2\r\n" +
      "03B788190A230870A351107EAE0587EBBE\r\n" +
      "03B78A190A1208708651107EBE059EEBA2\r\n" +
      "03B791190A1208705F51127EB4057CEB8F\r\n" +
      "03B794190A1D08706051117E870591EB96\r\n" +
      "03B795190A2F08708C51137E530585EBA8\r\n" +
      "03B78C190A400870CC510F7E5A0580EBAE\r\n" +
      "03B783190A3408711451117E82056DEBB0\r\n" +
      "03B781190A2F08714051137EAF058DEB65\r\n" +
      "03B784190A2908713551117EB7057CEB91\r\n" +
      "03B785190A0C08710651107EC70596EB9D\r\n" +
      "03B7901909F40870D551117EC2057FEBB6\r\n" +
      "03B79E190A0C0870B851107EBA057BEBB9\r\n" +
      "03B79F190A290870B751137EC8058EEBD5\r\n" +
      "03B799190A4C0870C551117ED7058BEBD1\r\n" +
      "03B786190A3A0870BB51127EF7058AEBC8\r\n" +
      "03B782190A6908709451127EED0582EB92\r\n" +
      "03B771190A9708706551117EBF057DEB8F\r\n" +
      "03B75A190AA908705151137E8E0578EB87\r\n" +
      "03B73F190AAF08705D51117E55057CEBC1\r\n" +
      "03B730190AAF08707A51127E600584EBB7\r\n" +
      "03B72B190A8C08708351107E8D0586EB78\r\n" +
      "03B735190A4D08706751127EB50587EB75\r\n" +
      "03B74A190A2308703051137EC70579EB63\r\n" +
      "03B76A190A1708700151117E970587EB8F\r\n" +
      "03B780190A29086FF8510F7E610582EBB6\r\n" +
      "03B786190A6308701351107E4A057EEB8B\r\n" +
      "03B77D190A5108704251107E34058CEB86\r\n" +
      "03B770190A3408705C51107E23057FEB95\r\n" +
      "03B774190A2F08705951127E20057BEBCB\r\n" +
      "03B779190A2308705151117E2005B6EBBF\r\n" +
      "03B7841909FA08705B51137E1B0589EB9D\r\n" +
      "03B7921909C608707D51127E09058CEB9D\r\n" +
      "03B7AF1909E90870A751117E22058DEB84\r\n" +
      "03B7B9190A0C0870CA51137E580580EBA7\r\n" +
      "03B7B0190A060870E351127E940570EB8D\r\n" +
      "03B7AB1909FA0870F451137EAF057CEBCF\r\n" +
      "03B7AC1909D70870F951117EC00590EBF1\r\n" +
      "03B7B519098C08710751117EB60569EBDD\r\n" +
      "03B7D019098C08712151117ECD0588EBBD\r\n" +
      "03B7EA19097508713751117EDA0586EB92\r\n" +
      "03B7FB19092908713051117ED50591EBD1\r\n" +
      "03B81E1908E908710551147EA9057DEBD2\r\n" +
      "03B84B1908CC0870CB51127E6A058EEBBB\r\n" +
      "03B86F1908E90870A851117E300583EBC7\r\n" +
      "03B87C19090C087096510F7E090583EBA7\r\n" +
      "03B87519095708707F51107E130596EB9C\r\n" +
      "03B8521909AF08705B51137E4D057FEBAA\r\n" +
      "03B8261909C208702C51117E850589EBB5\r\n" +
      "03B7FD190A2F086FF651137EC30595EBD1\r\n" +
      "03B7CE190A5D086FB451127EE60589EBC0\r\n" +
      "03B79A190A57086F7951107F16057BEBA1\r\n" +
      "03B77A190A91086F4051117F320590EB9D\r\n" +
      "03B75A190A5D086F0851117F33059FEB70\r\n" +
      "03B758190A4C086ECB51117F2A0588EB74\r\n" +
      "03B760190A51086E8151127F340588EB5B\r\n" +
      "03B761190A5D086E2C51147F0E0589EB3F\r\n" +
      "03B75F190ABA086DDD51107EEE0585EB34\r\n" +
      "03B745190B17086D8551117ED80582EAF6\r\n" +
      "03B713190AFA086D2A51137EEF0574EA76\r\n" +
      "03B6F7190B74086CCD51127EFF058BEA57\r\n" +
      "03B6CA190CDD086C6D51127EEA0582EA61\r\n" +
      "03B647190D1D086C1051127ED60586EA8E\r\n" +
      "03B5C9190D06086BB051127EED0587EA7D\r\n" +
      "03B58C190C80086B4951107EF10571EA85\r\n" +
      "03B593190C2F086AE351127EF10589EAA8\r\n" +
      "03B5BD190CEF086A7E51117EF2057AEAB4\r\n" +
      "03B5A4190E63086A1451127EF20570EAC2\r\n" +
      "03B523190E3A0869A751117EDE0577EAFB\r\n" +
      "03B4BD190F1208694051117EE00591EB0E\r\n" +
      "03B453190F770868D451127ECF05A0EB24\r\n" +
      "03B3F319100708686B51117EAA062AEAFE\r\n" +
      "03B39019103508680551117E8E0597EB65\r\n" +
      "03B33E19104708679851127E790586EB9C\r\n" +
      "03B30E19105208672F51117E520575EB76\r\n" +
      "03B2EF1910BB0866C451127E5B0582EB81\r\n" +
      "03B2C41910DE08665451117E610580EBA4\r\n" +
      "03B29B1910F00865DF51117E790597EBA8\r\n" +
      "03B28219116408656851127E87058CEBCD\r\n" +
      "03B2501911BC0864F451127E960579EB9F\r\n" +
      "03B2101911A408648151117E910589EBC1\r\n" +
      "03B1F019117008640D51127E9B0599EBDD\r\n" +
      "03B1EF19114108639851117E8B059AEBEA\r\n" +
      "03B20419112A08632651117E840588EBEA\r\n" +
      "03B21B1911760862B051137E7F0599EBF7\r\n" +
      "03B21519118A08624151117E6805B3EBE1\r\n" +
      "03B2011911300861D351127E560588EC07\r\n" +
      "03B20E19128E08615651117E3205A0EC1A\r\n" +
      "03B1CD1913A50860D551127E060582EC18\r\n" +
      "03B12F19142608606151107DF50590EC29\r\n" +
      "03B09D191466085FEF51137DF90589EC10\r\n" +
      "03B03419148F085F7951127E0F05A2EC36\r\n" +
      "03AFEA191567085F0051127E2D0588EC43\r\n" +
      "03AF8F19168B085E8351137E3C0590EC41\r\n" +
      "03AEF619173A085E0A51127E400589EC10\r\n" +
      "03AE6019177A085D9C51127E55057FEC1B\r\n" +
      "03ADEB1917AE085D3051117E730592EC36\r\n" +
      "03AD9D1917F0085CBC51137E7C0589EC37\r\n" +
      "03AD61191823085C4A51117E83059EEC68\r\n" +
      "03AD2D191858085BD451127E7D0599EC5F\r\n" +
      "03AD001918EA085B5751137E7A0593EC44\r\n" +
      "03ACC0191959085AE351127E810599EC3F\r\n" +
      "03AC751919C8085A7E51127E670594EC4B\r\n" +
      "03AC24191A1A085A0F51147E3B059BEC72\r\n" +
      "03ABD9191A3D08599F51127E2405ACEC68\r\n" +
      "03ABA1191A4908592851137E250589EC7D\r\n" +
      "03AB7E191A490858AC51127E320590EC81\r\n" +
      "03AB6D191A4D08582D51107E4F05A1EC99\r\n" +
      "03AB64191A540857B151147E5C059BEC97\r\n" +
      "03AB5F191A0808574251147E6E058AEC94\r\n" +
      "03AB6A191A8F0856CD51137E5E0598ECA8\r\n" +
      "03AB5B191AFE08565451117E4D059CECC2\r\n" +
      "03AB26191B440855D751127E1C0593ECD5\r\n" +
      "03AAED191B7E08555A51117E070587ECBA\r\n" +
      "03AAB4191BAD0854D951137DEF058CECB8\r\n" +
      "03AA8A191C8B08546751137DF0058DECBA\r\n" +
      "03AA34191D840853F851137DE90587EC81\r\n" +
      "03A9B0191DC108538A51137DDA0573ECA6\r\n" +
      "03A942191DF608531151127DEA058FECC1\r\n" +
      "03A8FA191E4808529651117DFC0593ECA4\r\n" +
      "03A8BD191E9408521F51127E200577EC90\r\n" +
      "03A880191ECF0851AE51117E4D0575EC81\r\n" +
      "03A84E191F0908513751107E78058EEC8B\r\n" +
      "03A821191F2C0850D051127E8D0575EC9E\r\n" +
      "03A7FB191F6108507051127E7A057CEC8F\r\n" +
      "03A7DD191FB008500651117E770561EC72\r\n" +
      "03A7B3192045084F9751127E5B0555EC7F\r\n" +
      "03A77419210C084F2551137E4D055FEC8D\r\n" +
      "03A7121921CE084EB351127E3F0550EC8D\r\n" +
      "03A69D192266084E4151137E3C054BECA5\r\n" +
      "03A6261922F9084DDA51137E35054FEC98\r\n" +
      "03A5BA192350084D7D51137E250552ECA7\r\n" +
      "03A55F1923B3084D1A51127E270556ECA4\r\n" +
      "03A50F1924B6084CAC51137E32055EECA4\r\n" +
      "03A4A8192628084C3C51127E330545ECA2\r\n" +
      "03A3F81926EF084BC951147E27053CEC6C\r\n" +
      "03A34919276A084B5151107E1A053AEC68\r\n" +
      "03A2C0192793084AE751137E14053FEC52\r\n" +
      "03A2681927C8084A8651127DFC0543EC2A\r\n" +
      "03A237192769084A2351107DD00546EC1D\r\n" +
      "03A22F1927410849B451137DA10565EC18\r\n" +
      "03A23819274708494151147D6A0558EBEF\r\n" +
      "03A23C19291D0848CC51127D32053DEBFC\r\n" +
      "03A1CE19291708485151137CED0536EBDF\r\n" +
      "03A16A1928720847E951137CD60549EBB7\r\n" +
      "03A15D19288208477F51127CC10541EB7D\r\n" +
      "03A17119269708471751157C96054CEB61\r\n" +
      "03A1E31928260846AA51127C4F0549EB72\r\n" +
      "03A1F119295708464351127BFF0536EB8C\r\n" +
      "03A190192A480845CD51127BC30540EB66\r\n" +
      "03A101192B1008454F51137BAC0547EB53\r\n" +
      "03A06D192B480844E451137B9F0551EB17\r\n" +
      "039FFF192B8B08447B51127B95054CEACE\r\n" +
      "039FB1192BAF08440B51127B95054CEACD\r\n" +
      "039F7B192BD208439D51127B9B0548EAC0\r\n" +
      "039F54192BC608432951127B830563EA82\r\n" +
      "039F40192C3C0842AF51147B7B0567EA74\r\n" +
      "039F1C192C8308423751137B84054EEA12\r\n" +
      "039EEE192CE00841C251147B740572EA21\r\n" +
      "039EB9192D1B08415351137B550566EA2D\r\n" +
      "039E88192D500840DD51127B4A0568E9E2\r\n" +
      "039E5C192D5C08406A51137B6A0577E9FF\r\n" +
      "039E41192D7308400051137B8D0586E9EF\r\n" +
      "039E2B192D75083F9C51117B9E0591E9DC\r\n" +
      "039E1E192DA2083F3551127BB005B1E9F6\r\n" +
      "039E0A192DCB083EBC51137BD405C0E9E5\r\n" +
      "039DF3192DDD083E4B51147BE805CDE9AE\r\n" +
      "039DDF192DE3083DDE51127C0305D2E97F\r\n" +
      "039DD4192DF7083D6F51147C0105EEE982\r\n" +
      "039DC8192E24083D0551137C300614E941\r\n" +
      "039DB5192E76083C9D51147C1A0608E930\r\n" +
      "039D94192E93083C3151137C180619E901\r\n" +
      "039D75192E99083BC151137BDF061AE8FC\r\n" +
      "039D62192E99083B4C51137BA80627E8F5\r\n" +
      "039D55192EA0083ADB51137B740623E8F4\r\n" +
      "039D4F192E93083A6E51137B4A0648E8BB\r\n" +
      "039D50192E5E083A0151127B38062BE8F8\r\n" +
      "039D5B192E8208399E51147B130653E8F7\r\n" +
      "039D5F192EA508393B51127AFC065AE922\r\n" +
      "039D56192EDA0838E051137AE10641E8DC\r\n" +
      "039D3B192F9808387051137AC1065FE8EB\r\n" +
      "039CFC19304108380451127A970688E906\r\n" +
      "039C9619316708379551137A990665E8CC\r\n" +
      "039C0A1931D708371F51137AAF064BE8AF\r\n" +
      "039B891932700836AC51147ABE0660E8A3\r\n" +
      "039B1A19325F08363951127AD60656E83B\r\n" +
      "039AD41932700835C451137AD50665E838\r\n" +
      "039AB31932A508354B51157AC50649E854\r\n" +
      "039A981932E60834D951127AB50660E85B\r\n" +
      "039A7619332108347451137A9C0649E716\r\n" +
      "039A5319331F08341051147A87062CE662\r\n" +
      "039A3B1933440833AF51137A87063BE735\r\n" +
      "039A2B19335608334F51137AB9064EE54D\r\n" +
      "039A1B19335C0832D251127AAE0634E748\r\n" +
      "039A1019335608324451137A9C061DE7EB\r\n" +
      "039A081933510831BD51137A6A0615E797\r\n" +
      "039A0519336708315051137A4E0640E5FE\r\n" +
      "039A0119336208311851127A57062EE602\r\n" +
      "0399FF1932E008312251137A86061FE6E9\r\n" +
      "039A1D19329F08317351127AD305F7E238\r\n" +
      "039A4B1932AE0831D951137AF3060DE185\r\n" +
      "039A681931B40831FB51117AE205F6E3F0\r\n" +
      "039A901932000831EF51137AC105EFE576\r\n" +
      "039ABF1931C50831E551117A9C05EBE227\r\n" +
      "039AE91931730831E951137A520608E5A5\r\n" +
      "039B191931C10831DF51137A2405EBE3C3\r\n" +
      "039B261932240831BD51137A1A05CCD963\r\n" +
      "039B091932BD08319151137A2205E2DFBA\r\n" +
      "039ACB1932FD08319051147A460602DEC6\r\n" +
      "039A8B19330F0831A851147A8D05FCE398\r\n" +
      "039A6019330E0831BB51137AD605E9DF3A\r\n" +
      "039A491933320831AD51157AEF05D1E03D\r\n" +
      "039A3A19332D08319451137AFC05D3E01E\r\n" +
      "039A311933090831A251137AF005CBDF6F\r\n" +
      "039A3619329F0831C051147AF805D8E1A9\r\n" +
      "039A561932590831CF51137AFF05CDE24F\r\n" +
      "039A851931FA0831BF51157B1C05CBDDDE\r\n" +
      "039AB81932240831AC51157B3805CCE03E\r\n" +
      "039AD41932180831AA51127B3805CBDF14\r\n" +
      "039ADF19321E0831C451137B2405B9E2B7\r\n" +
      "039AE51932040831E851137AFD05B2E2FF\r\n" +
      "039AEA1931D708320151167AFF05A8E322\r\n" +
      "039AFD1931A208320551157B14059CE5B3\r\n" +
      "039B171931DD08320051147B3405B6E6C0\r\n" +
      "039B2019325908320351167B6A05B2E5E4\r\n" +
      "039AFC19323208320851167B950597E804\r\n" +
      "039AE51932180831FE51157BB0059DE6E8\r\n" +
      "039AE61932240831E451147B940585E718\r\n" +
      "039AE61932B70831C651147B5D058EE88E\r\n" +
      "039AC11932F80831A551167B330596E8A9\r\n" +
      "039A8B1932D008317C51147B2B05A4E899\r\n" +
      "039A7819326A08314451137B4105A3E618\r\n" +
      "039A8919324D08313951167B610585DD1C\r\n" +
      "039AA519326A08316F51147B7005A0E285\r\n" +
      "039AB11932AB0831C351157B7D05A5DD05\r\n" +
      "039AA019329D0831F751147B93059BE50B\r\n" +
      "039A9619327008320551157B8B058FDE93\r\n" +
      "039A9F19325E0831FE51157B720590DA65\r\n" +
      "039AAC1932B70831F151157B450582DE2F\r\n" +
      "039A9F1932F20831E651137B1C0593DEDA\r\n" +
      "039A7C1933400831D851167AF7059FE1F3\r\n";
    
    try {
      BasicConfigurator.configure();
      CTDParser ctdParser = new CTDParser(file);
      
    } catch ( ParseException pe ) {
      System.out.println("There was a parse exception:" + pe.getMessage());
      System.exit(0);
    }
  }
  
  /**
   *  A method that returns the converted data matrix as a RealMatrix
   */
  public RealMatrix getConvertedMatrix() {
    return this.convertedDataValuesMatrix;
      
  } 
  
  /**
   *  A method that returns the converted data matrix as a double array
   */
  public double[][] getConvertedMatrixAsArray() {
    return this.convertedDataValuesMatrix.getData();
      
  } 
  
  /**
   *  A method that returns the ordered data variable names as a List
   */
  public List<String> getDataVariableNames() {
    return this.dataVariableNames;
      
  } 
  
  /**
   *  A method that returns the ordered data variable units as a List
   */
  public List<String> getDataVariableUnits() {
    return this.dataVariableUnits;
      
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
