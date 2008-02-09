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
   *  A field that stores the default Variable Leader ID (0x8000)
   */
  private static final ShortBuffer DEFAULT_FIXED_LEADER_ID = 
    ShortBuffer.wrap( new short[]{0x80,0x00} );

  /**
   *  A field that stores the Variable Leader ID (2-bytes) in a ByteBuffer
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
    
  }
}