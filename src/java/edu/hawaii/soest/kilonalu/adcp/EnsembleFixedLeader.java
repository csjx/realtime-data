/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents the ensemble Fixed Leader 
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

import edu.hawaii.soest.kilonalu.adcp.EnsembleDataType;
import edu.hawaii.soest.kilonalu.adcp.Ensemble;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 *  A class that represents the Fixed Leader of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format. 
 */
public final class EnsembleFixedLeader {

  /**
   *  A field that contains the base frequency index for Navigator only,
   *  and is considered a spare for the Workhorse.
   */
  private ByteBuffer baseFrequencyIndex         = ByteBuffer.allocate(1);

  /**
   *  A field that contains the beam angle (H-ADCP only). This byte is 
   *  considered a spare for all other ADCPs.
   */
  private ByteBuffer beamAngle                  = ByteBuffer.allocate(1);

  /**
   *  A field that contains the distance to the middle of the first depth
   *  cell (bin).  This distance is a function of depth cell length, 
   *  the profiling mode, the blank after transmit distance.
   */
  private ByteBuffer binOneDistance             = ByteBuffer.allocate(2);
  
  /**
   *  A field that contains the blanking distance used by the Workhorse 
   * to allow the transmit circuits time to recover before the receive 
   * cycle begins.
   */
  private ByteBuffer blankAfterTransmit         = ByteBuffer.allocate(2);
  
  /**
   *  A field that contains the coordinate transformation processing 
   *  parameters.  These firmware switches indicate how the Workhorse 
   *  collected data.  
   *  See the manual for interpreting each bit. 
   */
  private ByteBuffer coordinateTransformParams  = ByteBuffer.allocate(1);
  
  /**
   *  A field that contains the revision number of the CPU firmware.
   */
  private ByteBuffer cpuFirmwareRevision        = ByteBuffer.allocate(1);
  
  /**
   *  A field that contains the version number of the CPU firmware.
   */
  private ByteBuffer cpuFirmwareVersion         = ByteBuffer.allocate(1);
  
  /**
   *  A field that contains the serial number of the CPU board.
   */
  private ByteBuffer cpuBoardSerialNumber       = ByteBuffer.allocate(8);
  
  /**
   *  A field that contains the length of one depth cell.
   */
  private ByteBuffer depthCellLength            = ByteBuffer.allocate(2);
  
  /**
   * A field that contains the actual threshold value used to flag 
   * water-current data as good or bad.  If the error velocity value 
   * exceeds this threshold, the Workhorse flags all four beams of the 
   * affected bin as bad.
   */
  private ByteBuffer errorVelocityThreshold     = ByteBuffer.allocate(2);
  
  /**
   *  A field that contains the threshold value used to reject data 
   * received from a false target, usually fish.
   */
  private ByteBuffer falseTargetThreshold       = ByteBuffer.allocate(1);
  
  /**
   *  A field that stores the default Fixed Leader ID (0x0000)
   */
  private static final ByteBuffer DEFAULT_FIXED_LEADER_ID = 
    ByteBuffer.wrap( new byte[]{0x00,0x00} );

  /**
   *  A field that stores the Fixed Leader ID (2-bytes) in a ByteBuffer
   */
  private ByteBuffer fixedLeaderID              = ByteBuffer.allocate(2);

  /**
   *  A field that stores the Fixed Leader Spare (1-byte) in a ByteBuffer
   *  This field is reserved for RDI internal use.
   */
  private ByteBuffer fixedLeaderSpare           = ByteBuffer.allocate(1);
  
  /**
   *  A field that contains a correction factor for physical 
   *  heading misalignment.
   */
  private ByteBuffer headingAlignment           = ByteBuffer.allocate(2);
  
  /**
   *  A field that contains a correction factor for electrical/magnetic 
   *  heading bias.  
   */
  private ByteBuffer headingBias                = ByteBuffer.allocate(2);
  
    /**
     * A field stores the lag length, which is the time period between 
     * sound pulses.
     */
  private ByteBuffer lagLength                  = ByteBuffer.allocate(1);
  
  /**
   * A field stores Contains the minimum threshold of correlation that 
   * water-profile data can have to be considered good data.
   */
  private ByteBuffer lowCorrelationThreshold    = ByteBuffer.allocate(1);
  
  /**
   * A field contains the number of beams used to calculate velocity data,
   * not the number of physical beams. The Workhorse needs only three 
   * beams to calculate water-current velocities.
   */
  private ByteBuffer numberOfBeams              = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the number of depth cells over which the 
   * Workhorse collects data. 
   */
  private ByteBuffer numberOfCells              = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the number of code repetitions in the transmit pulse.
   */
  private ByteBuffer numberOfCodeRepetitions    = ByteBuffer.allocate(1);
  
  /**
   *  A field that indicates whether the PD0 data are real or simulated.
   *  The default is real data(0).
   */
  private ByteBuffer pdRealOrSimulatedFlag      = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the minimum percentage of water-profiling pings 
   * in an ensemble that must be considered good to output velocity data.
   */
  private ByteBuffer percentGoodMinimum         = ByteBuffer.allocate(1);
  
  /**
   * pingMinutes, pingSeconds, and pingHundredths are fields that contains 
   * the amount of time between ping groups in the ensemble.
   */
  private ByteBuffer pingHundredths             = ByteBuffer.allocate(1);
  
  /**
   * pingMinutes, pingSeconds, and pingHundredths are fields that contains 
   * the amount of time between ping groups in the ensemble.
   */
  private ByteBuffer pingMinutes                = ByteBuffer.allocate(1);
  
  /**
   * pingMinutes, pingSeconds, and pingHundredths are fields that contains 
   * the amount of time between ping groups in the ensemble.
   */
  private ByteBuffer pingSeconds                = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the number of pings averaged together 
   * during a data ensemble.
   */
  private ByteBuffer pingsPerEnsemble           = ByteBuffer.allocate(2);
  
  /**
   * A field that contains the profiling mode of the ADCP
   */
  private ByteBuffer profilingMode              = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the ending depth cell used for water 
   * reference layer averaging.  See the manual for details.
   */
  private ByteBuffer referenceLayerEnd          = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the starting depth cell used for water 
   * reference layer averaging.  See the manual for details.
   */
  private ByteBuffer referenceLayerStart        = ByteBuffer.allocate(1);

  /**
   * A field that reflects which sensors are available.  The bit pattern 
   * is the same as listed for the sensorSource field.
   */
  private ByteBuffer sensorAvailability         = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the selected source of environmental sensor data.
   *  See the manual for interpreting each bit. 
   */
  private ByteBuffer sensorSource               = ByteBuffer.allocate(1);
  
  /**
   *  A field that contains the instrument serial number (REMUS only),
   *  and is considered a spare for all other Workhorse ADCPs.
   */
  private ByteBuffer serialNumber               = ByteBuffer.allocate(4);
  
  /**
   * A field that contains the Signal Processing Mode.  
   * This field will always be set to 1. 
   */
  private ByteBuffer signalProcessingMode       = ByteBuffer.allocate(1);
  
  /**
   * A field that contains the WB-command settings.  See the manual for details. 
   */
  private ByteBuffer systemBandwidth            = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the Workhorse hardware configuration.  
   *  See the manual for interpreting each bit. 
   */
  private ByteBuffer systemConfiguration        = ByteBuffer.allocate(2);
  
  /**
    * A field that contains the CQ-command settings.  See the manual for details. 
   */
  private ByteBuffer systemPower                = ByteBuffer.allocate(1);
  
  /**
   * A field that contains contains the distance between pulse repetitions.
   */
  private ByteBuffer transmitLagDistance        = ByteBuffer.allocate(2);
  
  /**
   * A field that contains the length of the transmit pulse.
   */
  private ByteBuffer transmitPulseLength        = ByteBuffer.allocate(2);
  
  
  
  
  /**
   *  Constructor.  This method populates the Fixed Leader fields from 
   *  the given ByteBuffer of data passed in as an argument, based on metadata 
   *  found in the EnsembleHeader.
   *
   * @param ensembleBuffer the ByteBuffer that contains the binary ensemble data
   * @param ensemble  the parent ensemble for this fixed leader
   */
  public EnsembleFixedLeader( ByteBuffer ensembleBuffer, 
                              Ensemble ensemble ) {
    
    // prepare the ensemble buffer for reading
    ensembleBuffer.flip();
    
  }
  
  /**
   * A method that returns the Ensemble baseFrequencyIndex field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getBaseFrequencyIndex() {
    return this.baseFrequencyIndex;
  }
  
  /**
   * A method that returns the Ensemble beamAngle field contents 
   * as a ByteBuffer.
   */
   protected ByteBuffer getBeamAngle() {
    return this.beamAngle;
  }
  
  /**
   * A method that returns the Ensemble binOneDistance field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getBinOneDistance() {
    return this.binOneDistance;
  }
  
  /**
   * A method that returns the Ensemble blankAfterTransmit field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getBlankAfterTransmit() {
    return this.blankAfterTransmit;
  }
  
  /**
   * A method that returns the Ensemble coordinateTransformParams field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getCoordinateTransformParams() {
    return this.coordinateTransformParams;
  }
  
  /**
   * A method that returns the Ensemble cpuFirmwareRevision field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getCpuFirmwareRevision() {
    return this.cpuFirmwareRevision;
  }
  
  /**
   * A method that returns the Ensemble headerID field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getCpuFirmwareVersion() {
    return this.cpuFirmwareVersion;
  }
  
  /**
   * A method that returns the Ensemble cpuFirmwareVersion field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getCpuBoardSerialNumber() {
    return this.cpuBoardSerialNumber;
  }
  
}