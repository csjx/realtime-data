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

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

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

  
  /*
   * Constructor: Creates an empty Calibration instance
   */
  public Calibration() {
    
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
