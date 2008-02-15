/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents the ensemble Correlation Profile 
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
 *  A class that represents the Correlation Profile of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format. 
 */
public final class EnsembleCorrelationProfile {
  
  /**
   *  A field that stores the Correlation Profile ID (2-bytes) in a ByteBuffer
   */
  private ByteBuffer correlationProfileID           = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the Correlation Profile array data in a ByteBuffer
   */
  private ByteBuffer correlationProfile;
  
  /**
   *  Constructor.  This method populates the Correlation Profile fields from 
   *  the given ByteBuffer of data passed in as an argument, based on metadata 
   *  found in the EnsembleHeader.
   *
   * @param ensembleBuffer the ByteBuffer that contains the binary ensemble data
   * @param ensembleHeader the ensembleHeader that contains the ensemble header data
   */
  public EnsembleCorrelationProfile( ByteBuffer ensembleBuffer, 
                                     Ensemble ensemble ) {
    System.out.println("EnsembleCorrelationProfile called.");
    // prepare the ensemble buffer for reading
    ensembleBuffer.flip();
    ensembleBuffer.limit(ensembleBuffer.capacity());
    
    // position the cursor at the correct offset given the sequential location
    // of the fixed leader in the data stream.
    int typeNumber = 
      ensemble.getDataTypeNumber( EnsembleDataType.CORRELATION_PROFILE );
    int offset = ensemble.getDataTypeOffset( typeNumber );
    ensembleBuffer.position( offset );
    
    int numberOfBeams = ensemble.getNumberOfBeams();    
    int numberOfCells = ensemble.getNumberOfCells();
    int correlationProfileLength = numberOfBeams * numberOfCells;
    
    // set the size of the Correlation Profile ByteBuffer (1-byte cells)
    correlationProfile = ByteBuffer.allocate( correlationProfileLength );
    
    // define the temporary arrays for passing bytes
    byte[] oneByte = new byte[1];
    byte[] twoBytes = new byte[2];
    
    ensembleBuffer.get(twoBytes);
    setCorrelationProfileID(twoBytes);
    ensemble.addToByteSum(twoBytes);
    
    // iterate through the bytes for each beam * number of depth cell bins
    for ( int i=1; i <= correlationProfileLength; i++ ) {
      ensembleBuffer.get(oneByte);
      setCorrelationProfile(oneByte);
      ensemble.addToByteSum(oneByte);
    }
  }
  
  /**
   * A method that returns the Ensemble Correlation Profile ID field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getCorrelationProfile(){
    this.correlationProfile.limit(this.correlationProfile.capacity());
    this.correlationProfile.position(0);
    return this.correlationProfile;
  }
  
  /**
   * A method that returns the Ensemble Correlation Profile ID field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getcorrelationProfileID(){
    this.correlationProfileID.limit(this.correlationProfileID.capacity());
    this.correlationProfileID.position(0);
    return this.correlationProfileID;
  }
  
  /**
   * A method that sets the Ensemble Correlation Profile ID field contents 
   * with the given byte array.
   */
  private void setCorrelationProfile(byte[] byteArray){
    this.correlationProfile.put(byteArray);
  }
  
  /**
   * A method that sets the Ensemble Correlation Profile ID field contents 
   * with the given byte array.
   */
  private void setCorrelationProfileID(byte[] byteArray){
    this.correlationProfileID.put(byteArray);
  }

}