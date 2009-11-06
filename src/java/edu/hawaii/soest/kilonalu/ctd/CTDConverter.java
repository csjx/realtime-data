/**
 *  Copyright: 2009 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a converted  sample of data produced by
 *             a Seabird Seacat 19plus CTD profiler as described in
 *            the SBE 19plus SEACAT Profiler User's Manual 
 *            (Manual Version #010, 01/02/03 ).  The converter is intended to 
 *            convert raw frequencies and voltages as described in OUTPUTFORMAT 
 *            2 in the manual.  It uses formulas stated in the SBE19plus
 *            calibration sheets for converting temperature counts to degrees
 *            celsius, conductivity frequencies to Siemens/m, and pressure counts
 *            to decibars.
 *
 *   Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate:  $
 * $LastChangedBy:  $
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
package edu.hawaii.soest.kilonalu.ctd;

import edu.hawaii.soest.kilonalu.ctd.CTDParser;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.RealMatrix;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents converted CTD data values.  The class takes a 
 *  CTDParser class as input, which stores decimal data values for raw 
 *  frequencies and voltages, and metadata fields used to convert those voltages
 *  to usable engineering units.
 */
public class CTDConverter {
    
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
  static Logger logger = Logger.getLogger(CTDConverter.class);
        
  /**
   * The instance of the CTDParser object that holds the original data and
   * metadata values.
   */
   private CTDParser ctdParser;

  /**
   * The original matrix of raw frequency and voltage values (counts, Hz, V ...)
   */
   private RealMatrix dataValuesMatrix;

  /**
   * The converted matrix of values in engineering units (degrees, S/m, decibars ...)
   */
   private RealMatrix convertedDataValuesMatrix;

  /**
   *  Constructor:  Builds all of the components of the CTD data object from
   *  the CTDParser object being passed in.  The data string must contain the results 
   *  of the 'DS' command and the 'DCAL' command in order to inform the parser
   *  which data fields should be expected, and which CTD output format to expect.
   *  The data observations should follow the output of the 'DS' and 'DCAL' commands.
   *
   *  @param dataString The CTDParser object that contains the data and metadata  
   *                    from the instrument
   */
  public CTDConverter(CTDParser ctdParser) {
    
    // set the data values matrix
    this.dataValuesMatrix = ctdParser.getConvertedMatrix();  
    
    // create an equally-sized array for the converted values
    this.convertedDataValuesMatrix = 
      new Array2DRowRealMatrix(this.dataValuesMatrix.getRowDimension(),
                               this.dataValuesMatrix.getColumnDimension());
    
    // set the CTD parser object field
    this.ctdParser = ctdParser;                           
    
  }
}                                               
