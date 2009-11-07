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

  /*
   *  A method used to apply all of the conversion formulas to the data matrix
   *  The converted matrix is populated with the new values.
   */
  public void convert() {
    
    // For the following conversions, each variable's data vector position
    // is determined by the position of the variable name as they are listed
    // in the ctdParser.dataVariableNames list.
    
    // convert the temperature vector 
    if ( this.ctdParser.getDataVariableNames().indexOf(
           ctdParser.RAW_TEMPERATURE_FIELD_NAME) >= 0 ) {
      int temperatureVariableIndex = this.ctdParser.getDataVariableNames().indexOf(
                                       this.ctdParser.RAW_TEMPERATURE_FIELD_NAME);
      convertTemperature(temperatureVariableIndex);    
    
    }
    
    // convert the pressure vector (relies on pressure-temp compensation)
    if ( (this.ctdParser.getDataVariableNames().indexOf(
           this.ctdParser.RAW_PRESSURE_FIELD_NAME) >= 0) &&
          ( this.ctdParser.getDataVariableNames().indexOf(
                  this.ctdParser.RAW_PRESSURE_TEMP_COMP_FIELD_NAME) >= 0) ) {
      int pressureVariableIndex = 
        this.ctdParser.getDataVariableNames().indexOf(
          this.ctdParser.RAW_PRESSURE_FIELD_NAME);
      int pressureTempCompVariableIndex = 
        this.ctdParser.getDataVariableNames().indexOf(
          this.ctdParser.RAW_PRESSURE_TEMP_COMP_FIELD_NAME);
      convertPressure(pressureVariableIndex, pressureTempCompVariableIndex);    
  
    }
    
    // convert the conductivity vector (relies on converted temp and pressure)
    if ( (this.ctdParser.getDataVariableNames().indexOf(
           this.ctdParser.RAW_CONDUCTIVITY_FIELD_NAME) >= 0) &&
         (this.ctdParser.getDataVariableNames().indexOf(
           this.ctdParser.RAW_TEMPERATURE_FIELD_NAME) >= 0) &&
         (this.ctdParser.getDataVariableNames().indexOf(
           this.ctdParser.RAW_PRESSURE_FIELD_NAME) >= 0) ) {
      
      int conductivityVariableIndex = this.ctdParser.getDataVariableNames().indexOf(
                                        this.ctdParser.RAW_CONDUCTIVITY_FIELD_NAME);
      
      int temperatureVariableIndex  = this.ctdParser.getDataVariableNames().indexOf(
                                        this.ctdParser.RAW_TEMPERATURE_FIELD_NAME);
      
      int pressureVariableIndex     = this.ctdParser.getDataVariableNames().indexOf(
                                        this.ctdParser.RAW_PRESSURE_FIELD_NAME);
        
      convertConductivity(conductivityVariableIndex, 
                          temperatureVariableIndex, 
                          pressureVariableIndex);    
  
    }
    
    System.out.println("Converted Data Matrix is: " + convertedDataValuesMatrix.toString());
    
    // TODO: convert voltage channels based on the metadata from the data
    // type being collected on each channel
    
  }    
  
  /*
   *  A method used to apply the temperature conversion formula to the data 
   *  matrix. The converted matrix is populated with the new values in the given
   *  vector position.
   */
  private void convertTemperature(int temperatureVectorIndex) {
    
    // define the constants from the temperature conversion formulas
    double MV_CONSTANT_ONE  = 524288d;
    double MV_CONSTANT_TWO  = 1.6e+007;
                            
    double R_CONSTANT_ONE   = 2.900e+009;
    double R_CONSTANT_TWO   = 1.024e+008;
    double R_CONSTANT_THREE = 2.048e+004;
    double R_CONSTANT_FOUR  = 2.0e+005;
    
    double T_CONSTANT_ONE   = 273.15d;
    
    //get the calibration coefficients from the CTDParser instance
    double temperatureCoefficientTA0 = this.ctdParser.getTemperatureCoefficientTA0(); 
    double temperatureCoefficientTA1 = this.ctdParser.getTemperatureCoefficientTA1(); 
    double temperatureCoefficientTA2 = this.ctdParser.getTemperatureCoefficientTA2(); 
    double temperatureCoefficientTA3 = this.ctdParser.getTemperatureCoefficientTA3(); 
    
    ArrayRealVector temperatureVector = (ArrayRealVector)
      this.dataValuesMatrix.getColumnVector(temperatureVectorIndex);
    
    ArrayRealVector convertedTemperatureVector = (ArrayRealVector)
      new ArrayRealVector(temperatureVector.getDimension());
      
    // iterate through the temperature values and apply the conversion
    for( int count = 0; count < temperatureVector.getDimension(); count++ ) {
      
      // calculate the temperature in degrees C from the calibration sheet formula
      // note: the mv, r, and tDegreesC variables come from the calibration sheet
      double countValue = temperatureVector.getEntry(count);
      double mv         = (countValue - MV_CONSTANT_ONE)/MV_CONSTANT_TWO;
      
      double r = 
      (mv * R_CONSTANT_ONE + R_CONSTANT_TWO)/(R_CONSTANT_THREE - mv * R_CONSTANT_FOUR);
      
      double tDegreesC = 
        1/(
            temperatureCoefficientTA0 + 
            temperatureCoefficientTA1 * (Math.log(r)) +
            temperatureCoefficientTA2 * (Math.pow(Math.log(r), 2)) + 
            temperatureCoefficientTA3 * (Math.pow(Math.log(r), 3)) 
          ) - T_CONSTANT_ONE;
      
      convertedTemperatureVector.setEntry(count, tDegreesC);
            
    } 
    
    // set the new vector in the converted values matrix
    this.convertedDataValuesMatrix.setColumnVector(temperatureVectorIndex, 
                                                   convertedTemperatureVector);
           
  }
  
  /*
   *  A method used to apply the conductivity conversion formula to the data 
   *  matrix. The converted matrix is populated with the new values in the given
   *  vector position.
   */
  private void convertConductivity(int conductivityVectorIndex, 
                                   int temperatureVectorIndex, 
                                   int pressureVectorIndex) {
    
    // define the constants from the conductivity conversion formulas
    double F_CONSTANT_ONE  = 1000d;
        
    //get the calibration coefficients from the CTDParser instance
    double conductivityCoefficientG      = this.ctdParser.getConductivityCoefficientG(); 
    double conductivityCoefficientH      = this.ctdParser.getConductivityCoefficientH(); 
    double conductivityCoefficientI      = this.ctdParser.getConductivityCoefficientI(); 
    double conductivityCoefficientJ      = this.ctdParser.getConductivityCoefficientJ(); 
    double conductivityCoefficientCF0    = this.ctdParser.getConductivityCoefficientCF0(); 
    double conductivityCoefficientCPCOR  = this.ctdParser.getConductivityCoefficientCPCOR(); 
    double conductivityCoefficientCTCOR  = this.ctdParser.getConductivityCoefficientCTCOR(); 
    double conductivityCoefficientCSLOPE = this.ctdParser.getConductivityCoefficientCSLOPE(); 
    
    ArrayRealVector conductivityVector = (ArrayRealVector)
      this.dataValuesMatrix.getColumnVector(conductivityVectorIndex);
    
    ArrayRealVector convertedConductivityVector = (ArrayRealVector)
      new ArrayRealVector(conductivityVector.getDimension());
    
    ArrayRealVector convertedTemperatureVector = (ArrayRealVector)
      this.convertedDataValuesMatrix.getColumnVector(temperatureVectorIndex);
    
    ArrayRealVector convertedPressureVector = (ArrayRealVector)
      this.convertedDataValuesMatrix.getColumnVector(pressureVectorIndex);
    
    // iterate through the temperature values and apply the conversion
    for( int count = 0; count < conductivityVector.getDimension(); count++ ) {
      
      // calculate the conductivity in Siemens/m from the calibration sheet formula
      double frequencyValue = conductivityVector.getEntry(count);
      double tDegreesC      = convertedTemperatureVector.getEntry(count);
      double pressureDB     = convertedPressureVector.getEntry(count);
      double f              = frequencyValue/F_CONSTANT_ONE;
      double cSiemensPerM = (
                              conductivityCoefficientG      +
                              (conductivityCoefficientH     *
                                Math.pow(f, 2))             +
                              (conductivityCoefficientI     *
                                Math.pow(f, 3))             +
                              (conductivityCoefficientJ     *
                                Math.pow(f, 4))             
                            )                               /
                            (                               
                              1                             +
                              (conductivityCoefficientCTCOR *
                               tDegreesC)                   +
                              (conductivityCoefficientCPCOR *
                               pressureDB)
                            );
      convertedConductivityVector.setEntry(count, cSiemensPerM);
            
    } 
    
    // set the new vector in the converted values matrix
    this.convertedDataValuesMatrix.setColumnVector(conductivityVectorIndex, 
                                                   convertedConductivityVector);      
       
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
