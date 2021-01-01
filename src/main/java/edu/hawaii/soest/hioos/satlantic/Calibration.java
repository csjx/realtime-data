/*
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a set of Satlantic instrument 
 *            calibration coefficients and methods to apply those 
 *            coefficients to observed data values.
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
package edu.hawaii.soest.hioos.satlantic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dhmp.util.BasicHierarchicalMap;

/*
 * A calibration object that models the Satlantic 'Instrument File Standard'
 * (SATDN-00134) version 6.1 02/04/2010.  This document describes the 
 * standardized fields found in a Satlantic calibration (.cal) or telemetry
 * definition (.tdf) file and how to interpret them.  This class provides
 * methods to parse these calibration files and to apply a series of 
 * algorithms to produce calibrated values from raw observed values.
 */
public class Calibration {
  
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j2.properties";
  
  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Log logger = LogFactory.getLog(Calibration.class);
  
  /* The URL of the calibration file */
  private String calibrationURL;
  
  /* An array list of the calibration file lines */
  private ArrayList<String> calibrationLines;
  
  /* 
   * A hash map of the information found in the calibration file, using the
   * concatenated type and id as the key. Each value is a hierarchical map. 
   * The map structure is:
   * /calibration/type        - the sensor calibration type (String)
   * /calibration/id          - the sensor calibration id (String)
   * /calibration/units       - the sensor calibration units (String)
   * /calibration/fieldLength - the sensor calibration field length (Integer)
   * /calibration/dataType    - the sensor calibration data type (String)
   * /calibration/coeffLines  - the sensor calibration coefficient lines (Integer)
   * /calibration/fitType     - the sensor calibration fit type (String)
   * /calibration/coefficient - a sensor calibration coefficient (Double)
   *
   */
  private HashMap<String, BasicHierarchicalMap> calibrationsMap;
  
  /**
   * A single node in the calibrationsMap used to build the full map above
   */
  private BasicHierarchicalMap calibrationMap;
   
  /**
   * Constructor: Creates an empty Calibration instance
   */
  public Calibration() {
    
  }
  
  /**
   * A method that applies a Satlantic calibration with no coefficients.
   *
   * @param observedValue - the observed value being calibrated
   * @param fitType - the type of calibration to be applied as a String
   * @return returnValue - The value of the calibration when applied
   */
  public String apply(String observedValue, String fitType) {
    
    String returnValue = "";
    
    try {
      
      CalibrationType calibrationType = CalibrationType.valueOf(fitType);
      
      // apply the appropriate calibration based on fit type
      switch(calibrationType) {
        
        case GPSTIME:
          //returnValue = applyGPSTime(observedValue, coefficients);
          break;
          
        case GPSPOS:
          //returnValue = applyGPSPos(observedValue, coefficients);
          break;
          
        case GPSHEMI:
          //returnValue = applyGPSHemi(observedValue, coefficients);
          break;
          
        case GPSMODE:
          //returnValue = applyGPSMode(observedValue, coefficients);
          break;
          
        case GPSSTATUS:
          //returnValue = applyGPSStatus(observedValue, coefficients);
          break;
          
        case DDMM:
          //returnValue = applyDDMM(observedValue, coefficients);
          break;
          
        case HHMMSS:
          //returnValue = applyHHMMSS(observedValue, coefficients);
          break;
          
        case DDMMYY:
          //returnValue = applyDDMMYY(observedValue, coefficients);
          break;
          
        case TIME2:
          //returnValue = applyTIME2(observedValue, coefficients);
          break;
          
        case COUNT:
          returnValue = observedValue;
          break;
          
        case NONE:
          returnValue = observedValue;
          break;
          
        case DELIMITER:
          //returnValue = applyDelimiter(observedValue, coefficients);
          break;
          
      }
      
    } catch (NullPointerException npe) {
      throw new IllegalArgumentException("The fit type was null. " +
                                         "Couldn't apply a calibration.");
      
    } catch (IllegalArgumentException iae) {
      throw iae;
    }
    
    return returnValue;
  }
  
  /**
   * A method that applies a Satlantic calibration to an observed value 
   * using the given coefficients.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param isImmersed - A boolean stating if the sensor is immersed during sampling
   * @param variable - the name of the measurement variable as a String
   * 
   * @return returnValue - the value of the applied calibration
   */
  public double apply(double observedValue, boolean isImmersed, String variable)
                      throws IllegalArgumentException {
    
    double returnValue = 0d;
    
    double[] coefficients = getCoefficients(variable);
    
    String fitType = getFitType(variable);
    fitType = fitType.toUpperCase();
    
    try {
      
      CalibrationType calibrationType = CalibrationType.valueOf(fitType);
      
      // apply the appropriate calibration based on fit type
      switch(calibrationType) {
        
        case OPTIC1:
          returnValue = applyOptic1(observedValue, isImmersed, coefficients);
          break;
          
        case OPTIC2:
          returnValue = applyOptic2(observedValue, isImmersed, coefficients);
          break;
          
        case OPTIC3:
          returnValue = applyOptic3(observedValue, isImmersed, coefficients);
          break;
          
        case THERM1:
          returnValue = applyTherm1(observedValue, coefficients);
          break;
          
        case POW10:
          returnValue = applyPow10(observedValue, isImmersed, coefficients);
          break;
          
        case POLYU:
          returnValue = applyPolyU(observedValue, coefficients);
          break;
          
        case POLYF:
          returnValue = applyPolyF(observedValue, coefficients);
          break;
        
        case COUNT:
          returnValue = observedValue;
          break;
        
        case NONE:
          returnValue = observedValue;
          break;
        
      }
      
      logger.debug("applied calibrationType " + calibrationType.toString() +
                   ", produced " + returnValue   +
                   "\t\tfrom "      + observedValue +
                   "\t\tfor "       + variable);
      
    } catch (NullPointerException npe) {
      throw new IllegalArgumentException("The fit type was null. " +
                                         "Couldn't apply a calibration.");
      
    } catch (IllegalArgumentException iae) {
      throw iae;
    }
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic OPTIC1 calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param isImmersed - A boolean stating if the sensor is immersed during sampling
   * @param coefficients - an array of coefficient values
   * 
   * @return returnValue - the value of the applied optic calibration 
   */
  private double applyOptic1(double observedValue, boolean isImmersed,
                             double[] coefficients)
                             throws IllegalArgumentException {
    
    double returnValue = 0d;
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic OPTIC2 calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param isImmersed - A boolean stating if the sensor is immersed during sampling
   * @param coefficients - an array of coefficient values
   * 
   * @return returnValue - the value of the applied optic calibration
   */
  private double applyOptic2(double observedValue, boolean isImmersed,
                             double[] coefficients)
                             throws IllegalArgumentException {
    
    double returnValue = 0d;
    
    if ( coefficients.length != 3 ) {
      throw new IllegalArgumentException("The OPTIC2 callibration requires " +
                                         "exactly three coefficients, not "  +
                                         coefficients.length);
    }
    
    double a0 = coefficients[0];
    double a1 = coefficients[1];
    double im = coefficients[2];
    
    // apply wet vs. dry calibrations
    if ( isImmersed ) {
      returnValue = im * a1 * (observedValue - a0);
      
    } else {
      returnValue = 1.0d * a1 * (observedValue - a0);
      
    }
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic OPTIC3 calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param isImmersed - A boolean stating if the sensor is immersed during sampling
   * @param coefficients - an array of coefficient values
   *    
   * @return returnValue - the value of the applied optic calibration
   */
  private double applyOptic3(double observedValue, boolean isImmersed,
                             double[] coefficients)
                             throws IllegalArgumentException {
    
    double returnValue = 0d;
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic THERM1 calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param coefficients - an array of coefficient values
   * 
   * @return returnValue - the value of the applied therm1 calibration
   */
  private double applyTherm1(double observedValue, double[] coefficients)
                             throws IllegalArgumentException {
    
    Double returnValue = null;
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic POW10 calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param isImmersed - A boolean stating if the sensor is immersed during sampling
   * @param coefficients - an array of coefficient values
   * 
   * @return returnValue - the value of the applied Pow10 calibration
   */
  private double applyPow10(double observedValue, boolean isImmersed,
                            double[] coefficients)
                            throws IllegalArgumentException {
    
    double returnValue = 0d;
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic POLYU calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param coefficients - an array of coefficient values
   * 
   * @return returnValue - the value of the applied PolyU calibration
   */
  private double applyPolyU(double observedValue, double[] coefficients)
                            throws IllegalArgumentException {
    
    double returnValue = 0d;
    double power = 0d;
    
    if ( !(coefficients.length > 0) ) {
      throw new IllegalArgumentException("The POLYU callibration requires " +
                                         "at least one coefficient, not "  +
                                         coefficients.length);
    }
    
    for (double coefficient : coefficients) {
      
      // apply the unfactored polynomial calibration
      returnValue = returnValue + (coefficient * Math.pow(observedValue, power));
      power++;
      
    }
    
    return returnValue;
  }
  
  /*
   * A method that applies a Satlantic POLYF calibration to an observed value
   * using the given coefficients. See Satlantic's Instrument File Standard,
   * (SAT-DN-00134) Version 6.1 (E) February 4, 2010.
   *
   * @param observedValue - the value from the sensor to be calibrated as a Double
   * @param coefficients - an array of coefficient values
   * 
   * @return returnValue - the value of the applied PolyF calibration
   */
  private double applyPolyF(double observedValue, double[] coefficients)
                            throws IllegalArgumentException {
    
    double returnValue = 0d;
    int count = 0;
    
    if ( !(coefficients.length > 0) ) {
      throw new IllegalArgumentException("The POLYU callibration requires " +
                                         "at least one coefficient, not "  +
                                         coefficients.length);
    }
    
    for (double coefficient : coefficients) {
      
      if ( count == 0 ) {
        returnValue = coefficient;
        
      } else {
        // apply the factored polynomial calibration
        returnValue = returnValue * (observedValue - coefficient);
        
      }
      count++;
      
    }
    
    return returnValue;
  }
  
  /*
   * Returns the array of calibration coefficients for a given variable
   *
   * @param variable - the variable name
   * @return coefficients - the calibration coefficients
   */
  private double[] getCoefficients(String variable) {
    
    BasicHierarchicalMap calibrationMap = this.calibrationsMap.get(variable);
    
    Collection collection = calibrationMap.getAll("calibration/coefficient");
    double[] coefficients = new double[collection.size()];
    int count = 0;
    
    for (Object item : collection) {
      Double coefficient = (Double) item;
      coefficients[count] = coefficient.doubleValue();
      count ++;
      
    }
    
    return coefficients;
  }
  
  /*
   *  Returns the type of calibration fit for a given variable
   *
   *  @param variable - the variable name
   *  @return fitType - the type of the calibration fit
   */
  private String getFitType(String variable) {
    
    String fitType = "NULL";
    
    BasicHierarchicalMap calibrationMap = this.calibrationsMap.get(variable);
    fitType = (String) calibrationMap.get("calibration/fitType");
    
    return fitType;
  }
  
  /**
   *  Returns the names of the variables from the calibrations map
   *
   * @return variableNames - the set of variable names a a List of Strings
   */
  public List<String> getVariableNames() {
    
    List<String> variableNames = new ArrayList<String>();
    
    for(String name : this.calibrationsMap.keySet()) {
      variableNames.add(name);
    }
    return variableNames;
    
  }
  
  /**
   * A method that parses a Satlantic calibration or telemetry definition file.
   * 
   * @param calibrationURL the URL of the calibration or telementry definition file
   * @return parsed - True if the file is parsed
   * @throws ParseException - parse exception
   */
  public boolean parse(String calibrationURL) throws ParseException {
    
    this.calibrationURL  = calibrationURL;
    this.calibrationsMap = new HashMap<String, BasicHierarchicalMap>();
    
    int count       = 0;     // track lines in the file
    int coeffCount  = 0;     // track current number of coefficient lines
    boolean success = false; // track parsing success
    String key      = "";    // unique identifier 
    String line;             // current line being parsed
    String[] parts;          // array of current line fields
    String type;             // placeholder calibration field
    String id;               // placeholder calibration field
    String units;            // placeholder calibration field
    Integer fieldLength;     // placeholder calibration field
    String dataType;         // placeholder calibration field
    Integer coeffLines;      // placeholder calibration field
    String fitType;          // placeholder calibration field
    Double coefficient;      // placeholder calibration field
    
    try {
    
      // open the calibration URL for reading
      URL calibrationFile = new URL(this.calibrationURL);
      BufferedReader inputReader = new BufferedReader(
                                   new InputStreamReader(
                                   calibrationFile.openStream()));
      
      this.calibrationLines = new ArrayList<String>();
      // and read each line
      while ((line = inputReader.readLine()) != null) {
        
        // extract each line of the file
        this.calibrationLines.add(line);
        count++;
        
      } // end while()
      
      inputReader.close();
      
      // parse non-empty lines or those without comments
      for (Iterator cIterator = calibrationLines.iterator(); cIterator.hasNext();) {
        
        line = ((String) cIterator.next()).trim();
        
        // handle sensor definition lines
        if ( line.matches("^[A-Za-z].*") ) {
          
          parts       = line.split(" ");
          key         = parts[1].toUpperCase().equals("NONE") ? 
                          parts[0] : parts[0] + "_" + parts[1];
          type        = parts[0];
          id          = parts[1];
          units       = parts[2].replaceAll("'", "");
          fieldLength = new Integer(parts[3]);
          dataType    = parts[4];
          coeffLines  = new Integer(parts[5]);
          fitType     = parts[6];
          
          if ( coeffCount == 0 ) {
            // create a new calibration entry
            this.calibrationMap = new BasicHierarchicalMap();
            coeffCount = coeffLines;
          }
          
          // populate the map
          this.calibrationMap.put("calibration/type", type);
          this.calibrationMap.put("calibration/id", id);
          this.calibrationMap.put("calibration/units", units);
          this.calibrationMap.put("calibration/fieldLength", fieldLength);
          this.calibrationMap.put("calibration/dataType", dataType);
          this.calibrationMap.put("calibration/coeffLines", coeffLines);
          this.calibrationMap.put("calibration/fitType", fitType);
          
          //logger.debug(this.calibrationMap.toXMLString(1000));
          
          // add the map if there are no more coefficient lines
          if ( coeffCount == 0 ) {
            this.calibrationsMap.put(key, (BasicHierarchicalMap) this.calibrationMap.clone());
            this.calibrationMap.removeAll("*");
            
          }
        
        // handle coefficient lines
        } else if ( line.matches("^[0-9].*") ){
          
          parts = line.split(" ");
          
          // extract each coefficient and add it to the map
          for ( int j=0; j < parts.length; j++ ) {
            
            coefficient = new Double(parts[j]);
            this.calibrationMap.add("calibration/coefficient", coefficient);
            
          }
          coeffCount--;
          
          // add the map if there are no more coefficient lines
          if ( coeffCount == 0 ) {
            this.calibrationsMap.put(key, (BasicHierarchicalMap) this.calibrationMap.clone());
            this.calibrationMap.removeAll("*");
            
          }
          
        // handle comments and blank lines
        } else if ( line.matches("^#.*") || line.matches("^$") ) {
          //logger.debug("Skipping blank or commented calibration file lines.");
          
        } // end if()
        
      } // end for()
      
      success = true;
      
    } catch ( NumberFormatException nfe ) {
      throw new ParseException("There was a problem parsing"    +
                               " the calibration line string: " +
                               nfe.getMessage(), 0);
    
    } catch (IOException ioe) {
      throw new ParseException("There was a problem reading the calibration file " +
                               "from " + this.calibrationURL + ": " + ioe.getMessage(), 0);
      
    } // end try/catch
    
    //logger.debug(this.calibrationsMap.toString());
    
    return success;
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
