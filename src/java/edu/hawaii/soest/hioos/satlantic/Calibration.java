/**
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
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.dhmp.util.HierarchicalMap;
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
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";
  
  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(Calibration.class);
  
  /* The URL of the calibration file */
  private String calibrationURL;
  
  /* An array list of the calibration file lines */
  private ArrayList<String> calibrationLines;
  
  /* 
   * A hierarchical map of the information found in the calibration file. 
   * The map structure is:
   * /calibrations/calibration/type        - the sensor calibration type (String)
   * /calibrations/calibration/id          - the sensor calibration id (String)
   * /calibrations/calibration/units       - the sensor calibration units (String)
   * /calibrations/calibration/fieldLength - the sensor calibration field length (Integer)
   * /calibrations/calibration/dataType    - the sensor calibration data type (String)
   * /calibrations/calibration/coeffLines  - the sensor calibration coefficient lines (Integer)
   * /calibrations/calibration/fitType     - the sensor calibration fit type (String)
   * /calibrations/calibration/coefficient - a sensor calibration coefficient (Double)
   *
   * Calibration definitions can repeat, and are loaded into the structure in
   * the order they are listed in the .cal or .tdf file that has been parsed.
   */
  private BasicHierarchicalMap calibrationsMap;
  
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
   * A method that parses a Satlantic calibration or telemetry definition file.
   */
  public boolean parse(String calibrationURL) throws ParseException {
    
    this.calibrationURL  = calibrationURL;
    this.calibrationsMap = new BasicHierarchicalMap();
    
    int count       = 0;     // track lines in the file
    int coeffCount  = 0;     // track current number of coefficient lines
    boolean success = false; // track parsing success
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
        logger.debug("The current line is: " + line);
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
          this.calibrationMap.put("type", type);
          this.calibrationMap.put("id", id);
          this.calibrationMap.put("units", units);
          this.calibrationMap.put("fieldLength", fieldLength);
          this.calibrationMap.put("dataType", dataType);
          this.calibrationMap.put("coeffLines", coeffLines);
          this.calibrationMap.put("fitType", fitType);
          
          logger.debug(this.calibrationMap.toXMLString(1000));
          
          // add the map if there are no more coefficient lines
          if ( coeffCount == 0 ) {
            this.calibrationsMap.add("/calibrations/calibration", this.calibrationMap.clone());
            this.calibrationMap.removeAll("*");
            
          }
        
        // handle coefficient lines
        } else if ( line.matches("^[0-9].*") ){
          
          logger.debug("MARK 1");
          
          parts = line.split(" ");
          
          // extract each coefficient and add it to the map
          for ( int j=0; j < parts.length; j++ ) {
            
            coefficient = new Double(parts[j]);
            this.calibrationMap.add("coefficient", coefficient);
            
          }
          coeffCount--;
          
          // add the map if there are no more coefficient lines
          if ( coeffCount == 0 ) {
            this.calibrationsMap.add("/calibrations/calibration", this.calibrationMap.clone());
            this.calibrationMap.removeAll("*");
            
          }
          
        // handle comments and blank lines
        } else if ( line.matches("^#.*") || line.matches("^$") ) {
          logger.debug("Skipping blank or commented calibration file lines.");
          
        } // end if()
        
      } // end for()
      
    } catch ( NumberFormatException nfe ) {
      throw new ParseException("There was a problem parsing"    +
                               " the calibration line string: " +
                               nfe.getMessage(), 0);
    
    } catch (IOException ioe) {
      throw new ParseException("There was a problem reading the calibration file " +
                               "from " + this.calibrationURL + ": " + ioe.getMessage(), 0);
      
    } // end try/catch
    
    logger.debug(this.calibrationsMap.toXMLString(1000));
    
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
