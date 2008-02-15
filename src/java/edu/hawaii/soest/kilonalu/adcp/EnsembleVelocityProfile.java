/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents the ensemble Velocity Profile 
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
/**
 *  A class that represents the Velocity Profile of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format. 
 */
public final class EnsembleVelocityProfile {

  /**
   *  A field that stores the Velocity Profile ID (2-bytes) in a ByteBuffer
   */
  private ByteBuffer velocityProfileID              = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the Velocity Profile array data in a ByteBuffer
   */
  private ByteBuffer velocityProfile;
  
  /**
   *  Constructor.  This method populates the Velocity Profile fields from 
   *  the given ByteBuffer of data passed in as an argument, based on metadata 
   *  found in the EnsembleHeader.
   *
   * @param ensembleBuffer the ByteBuffer that contains the binary ensemble data
   * @param ensembleHeader the ensembleHeader that contains the ensemble header data
   */
  public EnsembleVelocityProfile( ByteBuffer ensembleBuffer, 
                                  Ensemble ensemble ) {
    
    // prepare the ensemble buffer for reading
    ensembleBuffer.flip();
    ensembleBuffer.limit(ensembleBuffer.capacity());
    
    // position the cursor at the correct offset given the sequential location
    // of the fixed leader in the data stream.
    int typeNumber = 
      ensemble.getDataTypeNumber( EnsembleDataType.VELOCITY_PROFILE );
    int offset = ensemble.getDataTypeOffset( typeNumber );
    ensembleBuffer.position( offset );
    
    int numberOfBeams = ensemble.getNumberOfBeams();    
    int numberOfCells = ensemble.getNumberOfCells();
    int velocityProfileLength = numberOfBeams * numberOfCells;
    
    // set the size of the Velocity Profile ByteBuffer (2-byte cells)
    velocityProfile = ByteBuffer.allocate( velocityProfileLength * 2 );
    
    // define the temporary array for passing bytes
    byte[] twoBytes = new byte[2];
    
    ensembleBuffer.get(twoBytes);
    setVelocityProfileID(twoBytes);
    ensemble.addToByteSum(twoBytes);
    
    // iterate through the bytes for each beam * number of depth cell bins
    for ( int i=1; i <= velocityProfileLength; i++ ) {
      ensembleBuffer.get(twoBytes);
      setVelocityProfile(twoBytes);
      ensemble.addToByteSum(twoBytes);
    }
  }
  
  /**
   * A method that returns the Ensemble Velocity Profile ID field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getVelocityProfile(){
    this.velocityProfile.limit(this.velocityProfile.capacity());
    this.velocityProfile.position(0);
    return this.velocityProfile;
  }
  
  /**
   * A method that returns the Ensemble Velocity Profile ID field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getVelocityProfileID(){
    this.velocityProfileID.limit(this.velocityProfileID.capacity());
    this.velocityProfileID.position(0);
    return this.velocityProfileID;
  }
  
  /**
   * A method that sets the Ensemble Velocity Profile ID field contents 
   * with the given byte array.
   */
  private void setVelocityProfile(byte[] byteArray){
    this.velocityProfile.put(byteArray);
  }
  
  /**
   * A method that sets the Ensemble Velocity Profile ID field contents 
   * with the given byte array.
   */
  private void setVelocityProfileID(byte[] byteArray){
    this.velocityProfileID.put(byteArray);
  }
  
}