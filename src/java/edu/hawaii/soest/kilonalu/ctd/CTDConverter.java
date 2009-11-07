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
  
  /*
   *  A method used to apply the temperature conversion formula to the data 
   *  matrix. The converted matrix is populated with the new values in the given
   *  vector position.
   */
  private void convertPressure(int pressureVectorIndex, int pressureTempCompIndex) {
  
    double P_CONTSTANT_ONE = 14.7d;     // atmospheric pressure influence
    double P_CONTSTANT_TWO = 0.689476d; // psia to decibars factor
    
    //get the calibration coefficients from the CTDParser instance
    double pressureCoefficientPA0     = this.ctdParser.getPressureCoefficientPA0(); 
    double pressureCoefficientPA1     = this.ctdParser.getPressureCoefficientPA1(); 
    double pressureCoefficientPA2     = this.ctdParser.getPressureCoefficientPA2(); 
    double pressureCoefficientPTCA0   = this.ctdParser.getPressureCoefficientPTCA0(); 
    double pressureCoefficientPTCA1   = this.ctdParser.getPressureCoefficientPTCA1(); 
    double pressureCoefficientPTCA2   = this.ctdParser.getPressureCoefficientPTCA2(); 
    double pressureCoefficientPTCB0   = this.ctdParser.getPressureCoefficientPTCB0(); 
    double pressureCoefficientPTCB1   = this.ctdParser.getPressureCoefficientPTCB1(); 
    double pressureCoefficientPTCB2   = this.ctdParser.getPressureCoefficientPTCB2(); 
    double pressureCoefficientPTEMPA0 = this.ctdParser.getPressureCoefficientPTEMPA0(); 
    double pressureCoefficientPTEMPA1 = this.ctdParser.getPressureCoefficientPTEMPA1(); 
    double pressureCoefficientPTEMPA2 = this.ctdParser.getPressureCoefficientPTEMPA2(); 
    double pressureOffsetCoefficient  = this.ctdParser.getPressureOffsetCoefficient(); 
    
    // convert raw values ...
    ArrayRealVector pressureVector = (ArrayRealVector)
      this.dataValuesMatrix.getColumnVector(pressureVectorIndex);
    
    ArrayRealVector pressureTempCompVector = (ArrayRealVector)
      this.dataValuesMatrix.getColumnVector(pressureTempCompIndex);
    
    // to engineering values
    ArrayRealVector convertedPressureVector = (ArrayRealVector)
      new ArrayRealVector(pressureVector.getDimension());
      
    // iterate through the pressure values and apply the conversion
    for( int count = 0; count < pressureVector.getDimension(); count++ ) {
      
      // calculate the pressure in decibars from the calibration sheet formula
      double pressureValue  = pressureVector.getEntry(count);
      double ptCompValue    = pressureTempCompVector.getEntry(count);
      
      double t = pressureCoefficientPTEMPA0                + 
                (pressureCoefficientPTEMPA1 * ptCompValue) +
                (pressureCoefficientPTEMPA2 * Math.pow(ptCompValue, 2));
              
      double x = pressureValue -
                 pressureCoefficientPTCA0 -
                 pressureCoefficientPTCA1 * t -
                 pressureCoefficientPTCA2 * Math.pow(t, 2);
                 
      double n = x *
                 pressureCoefficientPTCB0 /
                 (
                   pressureCoefficientPTCB0 + 
                   pressureCoefficientPTCB1 *
                   t                        +
                   pressureCoefficientPTCB2 *
                   Math.pow(t, 2)
                 );
      double pressurePSIA = pressureCoefficientPA0 +
                            pressureCoefficientPA1 *
                            n                      +
                            pressureCoefficientPA2 *
                            Math.pow(n, 2);
                 
      // pressure (decibels) = [pressure (psia) - 14.7] * 0.689476 from the CTD manual
      double pressureDB = (pressurePSIA - P_CONTSTANT_ONE) * P_CONTSTANT_TWO;
      convertedPressureVector.setEntry(count, pressureDB);
      
    }
    
    // set the new vector in the converted values matrix
    this.convertedDataValuesMatrix.setColumnVector(pressureVectorIndex, convertedPressureVector);
        
  }
  
  /**
   *  The main method
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
      CTDConverter ctdConverter = new CTDConverter(ctdParser);
      ctdConverter.convert();
      
    } catch ( ParseException pe ) {
      System.out.println("There was a parse exception:" + pe.getMessage());
      System.exit(0);
    }
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
