/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents the ensemble Variable Leader 
 *             component of data produced by an RDI 1200kHz Workhorse Acoustic 
 *             Doppler Current Profiler in the default PD0 format as described 
 *             in RDI's "Workhorse Commands and Output Data Format" manual, 
 *             P/N 957-6156-00 (March 2005)
 *    Authors: Christopher Jones
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
package edu.hawaii.soest.kilonalu.adcp;

import edu.hawaii.soest.kilonalu.adcp.Ensemble;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
/**
 *  A class that represents the Variable Leader of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format. 
 */
public final class EnsembleVariableLeader {

  /**
   *  A field that stores the Variable Leader ID (2-bytes) in a ShortBuffer
   */
  private ShortBuffer variableLeaderID              = ShortBuffer.allocate(1);
  
  /**
   *  A field that stores the Ensemble Number in a ByteBuffer
   * 
   *  Scaling:  LSD = 1 ensemble; Range = 1 to 65,535 ensembles
   *  
   *  NOTE:  The first ensemble collected is #1.  At “rollover,” we 
   *  have the following sequence: 
   *  1 = ENSEMBLE NUMBER 1 
   *  ...
   *  65535 = ENSEMBLE NUMBER 65,535 | ENSEMBLE 
   *  0 = ENSEMBLE NUMBER 65,536 | #MSB FIELD 
   *  1 = ENSEMBLE NUMBER 65,537 | (BYTE 12) INCR.
   */
  private ByteBuffer ensembleNumber                = ByteBuffer.allocate(2);

  /**
   *  A field that stores Workhorse Real Time Clock year value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockYear             = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Real Time Clock month value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockMonth            = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Real Time Clock day value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockDay              = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Real Time Clock hour value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockHour             = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Real Time Clock minute value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockMinute           = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Real Time Clock second value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockSecond           = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Real Time Clock hundredths value. The clock does
   *  account for leap years.
   */
  private ByteBuffer realTimeClockHundredths       = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the number of times the Ensemble Number 
   *  field has 'rolled over' in a ByteBuffer.  This allows ensembles up to
   *  16,777,215.
   */
  private ByteBuffer ensembleNumberIncrement       = ByteBuffer.allocate(1);

  /**
   *  A field that stores the Built In Test result in a ByteBuffer.  A zero
   *  code indicates a successful BIT result.  The following table shows the 
   *  possible error codes:
   *
   *  BYTE 13   BYTE 14  (BYTE 14 RESERVED FOR FUTURE USE)
   *  1xxxxxxx  xxxxxxxx = RESERVED
   *  x1xxxxxx  xxxxxxxx = RESERVED
   *  xx1xxxxx  xxxxxxxx = RESERVED
   *  xxx1xxxx  xxxxxxxx = DEMOD 1 ERROR
   *  xxxx1xxx  xxxxxxxx = DEMOD 0 ERROR
   *  xxxxx1xx  xxxxxxxx = RESERVED
   *  xxxxxx1x  xxxxxxxx = TIMING CARD ERROR
   *  xxxxxxx1  xxxxxxxx = RESERVED
   */
  private ByteBuffer builtInTestResult             = ByteBuffer.allocate(2);

  /**
   *  A field that stores the speed of sound in a ByteBuffer.
   *  Scaling:  LSD = 1 meter per second; Range = 1400 to 1600 m/s
   */
  private ByteBuffer speedOfSound                  = ByteBuffer.allocate(2);

  /**
   *  A field that stores the depth of the transducer in a ByteBuffer.
   *  Scaling:  LSD = 1 decimeter; Range = 1 to 9999 decimeters
   */
  private ByteBuffer depthOfTransducer             = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the ADCP heading angle in a ByteBuffer.
   *  Scaling:  LSD = 0.01 degree; Range = 000.00 to 359.99 degrees
   */
  private ByteBuffer heading                       = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the ADCP pitch angle in a ByteBuffer.
   *  Positive values mean that Beam #3 is spatially higher than Beam #4.
   *  Scaling:  LSD = 0.01 degree; Range = -20.00 to +20.00 degrees
   */
  private ByteBuffer pitch                         = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the ADCP roll angle in a ByteBuffer.
   *  For up-facing Workhorses, positive values mean that Beam 
   *  #2 is spatially higher than Beam #1.  For down-facing Workhorses, 
   *  positive values mean that Beam #1 is spatially higher than Beam #2.
   *  Scaling:  LSD = 0.01 degree; Range = -20.00 to +20.00 degrees  
   */
  private ByteBuffer roll                          = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the salinity value of the water at the
   *  transducer head in a ByteBuffer.
   *  Scaling:  LSD = 1 part per thousand; Range = 0 to 40 ppt
   */
  private ByteBuffer salinity                      = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the temperature value of the water at the 
   *  transducer head in a ByteBuffer.
   *  Scaling:  LSD = 0.01 degree; Range = -5.00 to +40.00 degrees C
   */
  private ByteBuffer temperature                   = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the minimum pre-ping wait time minutes in a ByteBuffer.
   */
  private ByteBuffer minPrePingWaitMinutes         = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the minimum pre-ping wait time seconds in a ByteBuffer.
   */
  private ByteBuffer minPrePingWaitSeconds         = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the minimum pre-ping wait time hundredths in a ByteBuffer.
   */
  private ByteBuffer minPrePingWaitHundredths      = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the heading standard deviation value in a ByteBuffer.
   */
  private ByteBuffer headingStandardDeviation      = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the pitch standard deviation value in a ByteBuffer.
   */
  private ByteBuffer pitchStandardDeviation        = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the roll standard deviation value in a ByteBuffer.
   */
  private ByteBuffer rollStandardDeviation         = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the Analog to Digital Converter Channel 0 output
   *  values in a ByteBuffer.
   *  The ADC sequentially samples one of the eight channels per ping group.  
   *  These fields are zeroed at the beginning of the deployment and updated each 
   *  ensemble at the rate of one channel per ping group.  
   *  For example, if the ping group size is 5, then: 
   *    END OF ENSEMBLE No.     CHANNELS UPDATED 
   *           Start            All channels = 0 
   *             1              0, 1, 2, 3, 4 
   *             2              5, 6, 7, 0, 1 
   *             3              2, 3, 4, 5, 6 
   *             4              7, 0, 8, 2, 3 
   *             ...                   ... 
   *  Here is the description for each channel: 
   *  CHANNEL  DESCRIPTION  
   *  0        XMIT CURRENT       
   *  1        XMIT VOLTAGE       
   *  2        AMBIENT TEMP 
   *  3        PRESSURE (+) 
   *  4        PRESSURE (-)   
   *  5        ATTITUDE TEMP
   *  6        ATTITUDE  
   *  7        CONTAMINATION SENSOR 
   *  
   */
  private ByteBuffer adcChannelZero               = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the Analog to Digital Converter Channel 1 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelOne                = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the Analog to Digital Converter Channel 2 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelTwo                  = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the Analog to Digital Converter Channel 3 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelThree              = ByteBuffer.allocate(1);

  /**
   *  A field that stores the Analog to Digital Converter Channel 4 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelFour               = ByteBuffer.allocate(1);

  /**
   *  A field that stores the Analog to Digital Converter Channel 5 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelFive               = ByteBuffer.allocate(1);

  /**
   *  A field that stores the Analog to Digital Converter Channel 6 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelSix                = ByteBuffer.allocate(1);

  /**
   *  A field that stores the Analog to Digital Converter Channel 7 output
   *  values in a ByteBuffer.
   *
   * @see adcChannelZero
   */
  private ByteBuffer adcChannelSeven              = ByteBuffer.allocate(1);

  /**
   *  A field that stores the Error status word in a ByteBuffer.  The ESW is 
   *  cleared (set to zero) between each ensemble. 
   *  Note that each number above represents one bit set – they 
   *  may occur in combinations.  For example, if the long word 
   *  value is 0000C000 (hexadecimal), then it indicates that both 
   *  a cold wake-up (0004000) and an unknown wake-up 
   *  (00008000) occurred.
   *  The following table describes in each byte of the 32 bit field.
   *
   *  Low 16 BITS LSB  
   *  BITS 07 06 05 04 03 02 01 00 
   *       x  x  x  x  x  x  x  1  Bus Error exception 
   *       x  x  x  x  x  x  1  x  Address Error exception 
   *       x  x  x  x  x  1  x  x  Illegal Instruction exception 
   *       x  x  x  x  1  x  x  x  Zero Divide exception 
   *       x  x  x  1  x  x  x  x  Emulator exception 
   *       x  x  1  x  x  x  x  x  Unassigned exception 
   *       x  1  x  x  x  x  x  x  Watchdog restart occurred 
   *       1  x  x  x  x  x  x  x  Battery Saver power  
   *  Low 16 BITS MSB  
   *  BITS 15 14 13 12 11 10 09 08 
   *       x  x  x  x  x  x  x  1  Pinging 
   *       x  x  x  x  x  x  1  x  Not Used 
   *       x  x  x  x  x  1  x  x  Not Used 
   *       x  x  x  x  1  x  x  x  Not Used 
   *       x  x  x  1  x  x  x  x  Not Used 
   *       x  x  1  x  x  x  x  x  Not Used 
   *       x  1  x  x  x  x  x  x  Cold Wakeup occurred 
   *       1  x  x  x  x  x  x  x  Unknown Wakeup occurred 
   *  High 16 BITS LSB  
   *  BITS 24 23 22 21 20 19 18 17 
   *       x  x  x  x  x  x  x  1  Clock Read error occurred 
   *       x  x  x  x  x  x  1  x  Unexpected alarm 
   *       x  x  x  x  x  1  x  x  Clock jump forward 
   *       x  x  x  x  1  x  x  x  Clock jump backward 
   *       x  x  x  1  x  x  x  x  Not Used 
   *       x  x  1  x  x  x  x  x  Not Used 
   *       x  1  x  x  x  x  x  x  Not Used      
   *       1  x  x  x  x  x  x  x  Not Used
   *
   *  High 16 BITS MSB  
   *  BITS 32 31 30 29 28 27 26 25 
   *       x  x  x  x  x  x  x  1  Not Used 
   *       x  x  x  x  x  x  1  x  Not Used 
   *       x  x  x  x  x  1  x  x  Not Used 
   *       x  x  x  x  1  x  x  x  Power Fail (Unrecorded) 
   *       x  x  x  1  x  x  x  x  Spurious level 4 intr (DSP) 
   *       x  x  1  x  x  x  x  x  Spurious level 5 intr (UART) 
   *       x  1  x  x  x  x  x  x  Spurious level 6 intr (CLOCK) 
   *       1  x  x  x  x  x  x  x  Level 7 interrupt occurred 
   *       1  x  x  x  x  x  x  x  Level 7 interrupt occurred
   */
  private ByteBuffer errorStatusWord                  = ByteBuffer.allocate(4);

  /**
   *  A field that is spare for RDI use in a ByteBuffer.
   */
  private ByteBuffer spareFieldOne                    = ByteBuffer.allocate(2);

  /**
   *  A field that stores the pressure of the water at the tranducer head
   *  relative to one atmosphere (sea level) in a ByteBuffer. Units are 
   *  decapascals. Scaling: LSD=1 deca-pascal; Range=0 to 4,294,967,295 
   *  decapascals.
   */
  private ByteBuffer pressure                         = ByteBuffer.allocate(4);

  /**
   *  A field that stores the pressure variance (deviation about the mean) 
   *  Units are decapascals. Scaling: LSD=1 deca-pascal; Range=0 to 4,294,967,295 
   *  decapascals.
   */
  private ByteBuffer pressureVariance                 = ByteBuffer.allocate(4);

  /**
   *  A field that is a spare for RDI use in a ByteBuffer.
   */
  private ByteBuffer spareFieldTwo                    = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock century value. 
   *  The clock does account for leap years.
   */
  private ByteBuffer realTimeY2KClockCentury          = ByteBuffer.allocate(1);
  
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock year value. 
   *  The clock does account for leap years.
   */
  private ByteBuffer realTimeY2KClockYear             = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock month value. 
   *  The clock does account for leap years.
   */
    private ByteBuffer realTimeY2KClockMonth          = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock day value. 
   *  The clock does account for leap years.
   */
    private ByteBuffer realTimeY2KClockDay            = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock hour value. 
   *  The clock does account for leap years.
   */
    private ByteBuffer realTimeY2KClockHour           = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock minute value. 
   *  The clock does account for leap years.
   */
    private ByteBuffer realTimeY2KClockMinute         = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock second value. 
   *  The clock does account for leap years.
   */
    private ByteBuffer realTimeY2KClockSecond         = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores Workhorse Y2K-compliant Real Time Clock hundredths value. 
   *  The clock does account for leap years.
   */
    private ByteBuffer realTimeY2KClockHundredths     = ByteBuffer.allocate(1);
  
  
  /**
   *  Constructor.  This method populates the Variable Leader fields from 
   *  the given ByteBuffer of data passed in as an argument, based on metadata 
   *  found in the EnsembleHeader.
   *
   * @param ensembleBuffer the ByteBuffer that contains the binary ensemble data
   * @param ensembleHeader the ensembleHeader that contains the ensemble header data
   */
  public EnsembleVariableLeader( ByteBuffer ensembleBuffer, 
                                 Ensemble ensemble ) {
    
    // prepare the ensemble buffer for reading
    ensembleBuffer.flip();
    ensembleBuffer.limit(ensembleBuffer.capacity());
    
    // position the cursor at the correct offset given the sequential location
    // of the fixed leader in the data stream.
    int typeNumber = 
      ensemble.getDataTypeNumber( EnsembleDataType.VARIABLE_LEADER );
    int offset = ensemble.getDataTypeOffset( typeNumber );
    ensembleBuffer.position( offset );
    
    // define the temporary arrays for passing bytes
    byte[] oneByte  = new byte[1];
    byte[] twoBytes = new byte[2];
    
    // set all of the FixedLeader fields in the order that they are read from 
    // the byte stream
    short shortValue = ensembleBuffer.getShort();
    setVariableLeaderID(shortValue);
    //ensembleBuffer.get(twoBytes);
    twoBytes[0] = (byte) ((shortValue << 8) >> 8);
    twoBytes[1] = (byte) (shortValue >> 8);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setEnsembleNumber(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(oneByte);
    setRealTimeClockYear(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeClockMonth(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeClockDay(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeClockHour(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeClockMinute(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeClockSecond(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeClockHundredths(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setEnsembleNumberIncrement(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(twoBytes);
    setBuiltInTestResult(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setSpeedOfSound(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setDepthOfTransducer(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setHeading(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setPitch(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setRoll(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setSalinity(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setTemperature(twoBytes);
    ensemble.addToByteSum(twoBytes);
    ensembleBuffer.get(oneByte);
    setMinPrePingWaitMinutes(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setMinPrePingWaitSeconds(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setMinPrePingWaitHundredths(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setHeadingStandardDeviation(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setPitchStandardDeviation(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRollStandardDeviation(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelZero(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelOne(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelTwo(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelThree(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelFour(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelFive(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelSix(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setADCChannelSeven(oneByte);
    ensemble.addToByteSum(oneByte);
    byte[] errorStatusWord = new byte[4];
    ensembleBuffer.get(errorStatusWord);
    setErrorStatusWord(errorStatusWord);
    ensemble.addToByteSum(errorStatusWord);
    ensembleBuffer.get(twoBytes);
    setSpareFieldOne(twoBytes);
    ensemble.addToByteSum(twoBytes);
    byte[] pressureArray = new byte[4];
    ensembleBuffer.get(pressureArray);
    setPressure(pressureArray);
    ensemble.addToByteSum(pressureArray);
    byte[] pressureVarianceArray = new byte[4];
    ensembleBuffer.get(pressureVarianceArray);
    setPressureVariance(pressureVarianceArray);
    ensemble.addToByteSum(pressureVarianceArray);
    ensembleBuffer.get(oneByte);
    setSpareFieldTwo(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockCentury(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockYear(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockMonth(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockDay(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockHour(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockMinute(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockSecond(oneByte);
    ensemble.addToByteSum(oneByte);
    ensembleBuffer.get(oneByte);
    setRealTimeY2KClockHundredths(oneByte);
    ensemble.addToByteSum(oneByte);    
  }
  
  /**
   * A method that returns the Ensemble Variable Leader ID field contents 
   * as a ByteBuffer.
   */
  protected ShortBuffer getVariableLeaderID(){
    this.variableLeaderID.limit(this.variableLeaderID.capacity());
    this.variableLeaderID.position(0);
    return this.variableLeaderID;
  }
  
  /**
   * A method that returns the Ensemble Number field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getEnsembleNumber(){
    this.ensembleNumber.limit(this.ensembleNumber.capacity());
    this.ensembleNumber.position(0);
    return this.ensembleNumber;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Year field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockYear(){
    this.realTimeClockYear.limit(this.realTimeClockYear.capacity());
    this.realTimeClockYear.position(0);
    return this.realTimeClockYear;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Month field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockMonth(){
    this.realTimeClockMonth.limit(this.realTimeClockMonth.capacity());
    this.realTimeClockMonth.position(0);
    return this.realTimeClockMonth;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Day field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockDay(){
    this.realTimeClockDay.limit(this.realTimeClockDay.capacity());
    this.realTimeClockDay.position(0);
    return this.realTimeClockDay;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Hour field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockHour(){
    this.realTimeClockHour.limit(this.realTimeClockHour.capacity());
    this.realTimeClockHour.position(0);
    return this.realTimeClockHour;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Minute field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockMinute(){
    this.realTimeClockMinute.limit(this.realTimeClockMinute.capacity());
    this.realTimeClockMinute.position(0);
    return this.realTimeClockMinute;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Second field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockSecond(){
    this.realTimeClockSecond.limit(this.realTimeClockSecond.capacity());
    this.realTimeClockSecond.position(0);
    return this.realTimeClockSecond;
  }
    
  /**
   * A method that returns the Ensemble Real Time Clock Hundredths field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeClockHundredths(){
    this.realTimeClockHundredths.limit(this.realTimeClockHundredths.capacity());
    this.realTimeClockHundredths.position(0);
    return this.realTimeClockHundredths;
  }
    
  /**
   * A method that returns the Ensemble Number Increment field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getEnsembleNumberIncrement(){
    this.ensembleNumberIncrement.limit(this.ensembleNumberIncrement.capacity());
    this.ensembleNumberIncrement.position(0);
    return this.ensembleNumberIncrement;
  }
    
  /**
   * A method that returns the Ensemble Built In Test Result field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getBuiltInTestResult(){
    this.builtInTestResult.limit(this.builtInTestResult.capacity());
    this.builtInTestResult.position(0);
    return this.builtInTestResult;
  }
    
  /**
   * A method that returns the Ensemble Speed of Sound field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getSpeedOfSound(){
    this.speedOfSound.limit(this.speedOfSound.capacity());
    this.speedOfSound.position(0);
    return this.speedOfSound;
  }
    
  /**
   * A method that returns the Ensemble Depth of Transducer field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getDepthOfTransducer(){
    this.depthOfTransducer.limit(this.depthOfTransducer.capacity());
    this.depthOfTransducer.position(0);
    return this.depthOfTransducer;
  }
    
  /**
   * A method that returns the Ensemble Heading field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getHeading(){
    this.heading.limit(this.heading.capacity());
    this.heading.position(0);
    return this.heading;
  }
    
  /**
   * A method that returns the Ensemble Pitch field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getPitch(){
    this.pitch.limit(this.pitch.capacity());
    this.pitch.position(0);
    return this.pitch;
  }
    
  /**
   * A method that returns the Ensemble Heading field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getRoll(){
    this.roll.limit(this.roll.capacity());
    this.roll.position(0);
    return this.roll;
  }
    
  /**
   * A method that returns the Ensemble Salinity field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getSalinity(){
    this.salinity.limit(this.salinity.capacity());
    this.salinity.position(0);
    return this.salinity;
  }
    
  /**
   * A method that returns the Ensemble Temperature field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getTemperature(){
    this.temperature.limit(this.temperature.capacity());
    this.temperature.position(0);
    return this.temperature;
  }
    
  /**
   * A method that returns the Ensemble Minimum Pre-ping Wait Minutes field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getMinPrePingWaitMinutes(){
    this.minPrePingWaitMinutes.limit(this.minPrePingWaitMinutes.capacity());
    this.minPrePingWaitMinutes.position(0);
    return this.minPrePingWaitMinutes;
  }
    
  /**
   * A method that returns the Ensemble Minimum Pre-ping Wait Seconds field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getMinPrePingWaitSeconds(){
    this.minPrePingWaitSeconds.limit(this.minPrePingWaitSeconds.capacity());
    this.minPrePingWaitSeconds.position(0);
    return this.minPrePingWaitSeconds;
  }
    
  /**
   * A method that returns the Ensemble Minimum Pre-ping Wait Hundredths field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getMinPrePingWaitHundredths(){
    this.minPrePingWaitHundredths.limit(this.minPrePingWaitHundredths.capacity());
    this.minPrePingWaitHundredths.position(0);
    return this.minPrePingWaitHundredths;
  }
    
  /**
   * A method that returns the Ensemble Heading Standard Deviation field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getHeadingStandardDeviation(){
    this.headingStandardDeviation.limit(this.headingStandardDeviation.capacity());
    this.headingStandardDeviation.position(0);
    return this.headingStandardDeviation;
  }
    
  /**
   * A method that returns the Ensemble Pitch Standard Deviation field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getPitchStandardDeviation(){
    this.pitchStandardDeviation.limit(this.pitchStandardDeviation.capacity());
    this.pitchStandardDeviation.position(0);
    return this.pitchStandardDeviation;
  }
    
  /**
   * A method that returns the Ensemble Roll Standard Deviation field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getRollStandardDeviation(){
    this.rollStandardDeviation.limit(this.rollStandardDeviation.capacity());
    this.rollStandardDeviation.position(0);
    return this.rollStandardDeviation;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Zero field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelZero(){
    this.adcChannelZero.limit(this.adcChannelZero.capacity());
    this.adcChannelZero.position(0);
    return this.adcChannelZero;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel One field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelOne(){
    this.adcChannelOne.limit(this.adcChannelOne.capacity());
    this.adcChannelOne.position(0);
    return this.adcChannelOne;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Two field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelTwo(){
    this.adcChannelTwo.limit(this.adcChannelTwo.capacity());
    this.adcChannelTwo.position(0);
    return this.adcChannelTwo;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Three field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelThree(){
    this.adcChannelThree.limit(this.adcChannelThree.capacity());
    this.adcChannelThree.position(0);
    return this.adcChannelThree;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Four field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelFour(){
    this.adcChannelFour.limit(this.adcChannelFour.capacity());
    this.adcChannelFour.position(0);
    return this.adcChannelFour;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Five field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelFive(){
    this.adcChannelFive.limit(this.adcChannelFive.capacity());
    this.adcChannelFive.position(0);
    return this.adcChannelFive;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Six field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelSix(){
    this.adcChannelSix.limit(this.adcChannelSix.capacity());
    this.adcChannelSix.position(0);
    return this.adcChannelSix;
  }
    
  /**
   * A method that returns the Ensemble ADC Channel Seven field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getADCChannelSeven(){
    this.adcChannelSeven.limit(this.adcChannelSeven.capacity());
    this.adcChannelSeven.position(0);
    return this.adcChannelSeven;
  }
    
  /**
   * A method that returns the Ensemble Error Status Word field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getErrorStatusWord(){
    this.errorStatusWord.limit(this.errorStatusWord.capacity());
    this.errorStatusWord.position(0);
    return this.errorStatusWord;
  }
    
  /**
   * A method that returns the Ensemble Spare Field One field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getSpareFieldOne(){
    this.spareFieldOne.limit(this.spareFieldOne.capacity());
    this.spareFieldOne.position(0);
    return this.spareFieldOne;
  }
    
  /**
   * A method that returns the Ensemble Pressure field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getPressure(){
    this.pressure.limit(this.pressure.capacity());
    this.pressure.position(0);
    return this.pressure;
  }
    
  /**
   * A method that returns the Ensemble Pressure Variance field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getPressureVariance(){
    this.pressureVariance.limit(this.pressureVariance.capacity());
    this.pressureVariance.position(0);
    return this.pressureVariance;
  }
    
  /**
   * A method that returns the Ensemble Spare Field TWo field 
   * contents as a ByteBuffer.
   */
  protected ByteBuffer getSpareFieldTwo(){
    this.spareFieldTwo.limit(this.spareFieldTwo.capacity());
    this.spareFieldTwo.position(0);
    return this.spareFieldTwo;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Century 
   * field contents as a ByteBuffer.
   */
  protected ByteBuffer getRealTimeY2KClockCentury(){
    this.realTimeY2KClockCentury.limit(this.realTimeY2KClockCentury.capacity());
    this.realTimeY2KClockCentury.position(0);
    return this.realTimeY2KClockCentury;
  }
  
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Year 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockYear(){
      this.realTimeY2KClockYear.limit(this.realTimeY2KClockYear.capacity());
      this.realTimeY2KClockYear.position(0);
      return this.realTimeY2KClockYear;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Month 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockMonth(){
      this.realTimeY2KClockMonth.limit(this.realTimeY2KClockMonth.capacity());
      this.realTimeY2KClockMonth.position(0);
      return this.realTimeY2KClockMonth;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Day 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockDay(){
      this.realTimeY2KClockDay.limit(this.realTimeY2KClockDay.capacity());
      this.realTimeY2KClockDay.position(0);
      return this.realTimeY2KClockDay;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Hour 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockHour(){
      this.realTimeY2KClockHour.limit(this.realTimeY2KClockHour.capacity());
      this.realTimeY2KClockHour.position(0);
      return this.realTimeY2KClockHour;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Minute 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockMinute(){
      this.realTimeY2KClockMinute.limit(this.realTimeY2KClockMinute.capacity());
      this.realTimeY2KClockMinute.position(0);
      return this.realTimeY2KClockMinute;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Second 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockSecond(){
      this.realTimeY2KClockSecond.limit(this.realTimeY2KClockSecond.capacity());
      this.realTimeY2KClockSecond.position(0);
      return this.realTimeY2KClockSecond;
  }
    
  /**
   * A method that returns the Ensemble Real Time Y2K-compliant Clock Hundredths 
   * field contents as a ByteBuffer.
   */
    protected ByteBuffer getRealTimeY2KClockHundredths(){
      this.realTimeY2KClockHundredths.limit(this.realTimeY2KClockHundredths.capacity());
      this.realTimeY2KClockHundredths.position(0);
      return this.realTimeY2KClockHundredths;
  }
  
  /**
   * A method that sets the Ensemble Variable Leader ID field contents 
   * with the given byte array.
   */
  private void setVariableLeaderID(short shortValue){
    this.variableLeaderID.put(shortValue);
  }
  
  /**
   * A method that sets the Ensemble Number field contents 
   * with the given byte array.
   */
  private void setEnsembleNumber(byte[] byteArray){
    this.ensembleNumber.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Year field contents 
   * with the given byte array.
   */
  private void setRealTimeClockYear(byte[] byteArray){
    this.realTimeClockYear.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Month field contents 
   * with the given byte array.
   */
  private void setRealTimeClockMonth(byte[] byteArray){
    this.realTimeClockMonth.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Day field contents 
   * with the given byte array.
   */
  private void setRealTimeClockDay(byte[] byteArray){
    this.realTimeClockDay.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Hour field contents 
   * with the given byte array.
   */
  private void setRealTimeClockHour(byte[] byteArray){
    this.realTimeClockHour.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Minute field contents 
   * with the given byte array.
   */
  private void setRealTimeClockMinute(byte[] byteArray){
    this.realTimeClockMinute.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Second field contents 
   * with the given byte array.
   */
  private void setRealTimeClockSecond(byte[] byteArray){
    this.realTimeClockSecond.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Clock Hundredths field contents 
   * with the given byte array.
   */
  private void setRealTimeClockHundredths(byte[] byteArray){
    this.realTimeClockHundredths.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Number Increment field contents 
   * with the given byte array.
   */
  private void setEnsembleNumberIncrement(byte[] byteArray){
    this.ensembleNumberIncrement.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Built In Test Result field contents 
   * with the given byte array.
   */
  private void setBuiltInTestResult(byte[] byteArray){
    this.builtInTestResult.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Speed of Sound field contents 
   * with the given byte array.
   */
  private void setSpeedOfSound(byte[] byteArray){
    this.speedOfSound.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Depth of Transducer field contents 
   * with the given byte array.
   */
  private void setDepthOfTransducer(byte[] byteArray){
    this.depthOfTransducer.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Heading field contents 
   * with the given byte array.
   */
  private void setHeading(byte[] byteArray){
    this.heading.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Pitch field contents 
   * with the given byte array.
   */
  private void setPitch(byte[] byteArray){
    this.pitch.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Heading field contents 
   * with the given byte array.
   */
  private void setRoll(byte[] byteArray){
    this.roll.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Salinity field contents 
   * with the given byte array.
   */
  private void setSalinity(byte[] byteArray){
    this.salinity.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Temperature field contents 
   * with the given byte array.
   */
  private void setTemperature(byte[] byteArray){
    this.temperature.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Minimum Pre-ping Wait Minutes field 
   * contents as a ByteBuffer.
   */
  private void setMinPrePingWaitMinutes(byte[] byteArray){
    this.minPrePingWaitMinutes.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Minimum Pre-ping Wait Seconds field 
   * contents as a ByteBuffer.
   */
  private void setMinPrePingWaitSeconds(byte[] byteArray){
    this.minPrePingWaitSeconds.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Minimum Pre-ping Wait Hundredths field 
   * contents as a ByteBuffer.
   */
  private void setMinPrePingWaitHundredths(byte[] byteArray){
    this.minPrePingWaitHundredths.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Heading Standard Deviation field 
   * contents as a ByteBuffer.
   */
  private void setHeadingStandardDeviation(byte[] byteArray){
    this.headingStandardDeviation.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Pitch Standard Deviation field 
   * contents as a ByteBuffer.
   */
  private void setPitchStandardDeviation(byte[] byteArray){
    this.pitchStandardDeviation.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Roll Standard Deviation field 
   * contents as a ByteBuffer.
   */
  private void setRollStandardDeviation(byte[] byteArray){
    this.rollStandardDeviation.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Zero field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelZero(byte[] byteArray){
    this.adcChannelZero.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel One field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelOne(byte[] byteArray){
    this.adcChannelOne.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Two field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelTwo(byte[] byteArray){
    this.adcChannelTwo.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Three field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelThree(byte[] byteArray){
    this.adcChannelThree.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Four field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelFour(byte[] byteArray){
    this.adcChannelFour.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Five field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelFive(byte[] byteArray){
    this.adcChannelFive.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Six field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelSix(byte[] byteArray){
    this.adcChannelSix.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble ADC Channel Seven field 
   * contents as a ByteBuffer.
   */
  private void setADCChannelSeven(byte[] byteArray){
    this.adcChannelSeven.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Error Status Word field 
   * contents as a ByteBuffer.
   */
  private void setErrorStatusWord(byte[] byteArray){
    this.errorStatusWord.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Spare Field One field 
   * contents as a ByteBuffer.
   */
  private void setSpareFieldOne(byte[] byteArray){
    this.spareFieldOne.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Pressure field 
   * contents as a ByteBuffer.
   */
  private void setPressure(byte[] byteArray){
    this.pressure.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Pressure Variance field 
   * contents as a ByteBuffer.
   */
  private void setPressureVariance(byte[] byteArray){
    this.pressureVariance.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Spare Field TWo field 
   * contents as a ByteBuffer.
   */
  private void setSpareFieldTwo(byte[] byteArray){
    this.spareFieldTwo.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Century 
   * field contents as a ByteBuffer.
   */
  private void setRealTimeY2KClockCentury(byte[] byteArray){
    this.realTimeY2KClockCentury.put(byteArray);
  }
  
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Year 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockYear(byte[] byteArray){
    this.realTimeY2KClockYear.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Month 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockMonth(byte[] byteArray){
    this.realTimeY2KClockMonth.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Day 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockDay(byte[] byteArray){
    this.realTimeY2KClockDay.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Hour 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockHour(byte[] byteArray){
    this.realTimeY2KClockHour.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Minute 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockMinute(byte[] byteArray){
    this.realTimeY2KClockMinute.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Second 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockSecond(byte[] byteArray){
    this.realTimeY2KClockSecond.put(byteArray);
  }
    
  /**
   * A method that sets the Ensemble Real Time Y2K-compliant Clock Hundredths 
   * field contents as a ByteBuffer.
   */
    private void setRealTimeY2KClockHundredths(byte[] byteArray){
    this.realTimeY2KClockHundredths.put(byteArray);
  }
  
}