/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents the ensemble header of data produced by
 *             an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *             default PD0 format as described in RDI's "Workhorse Commands and 
 *             Output Data Format" manual, P/N 957-6156-00 (March 2005)
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/**
 *  A class that represents the ensemble header of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format. 
 */
public class EnsembleHeader {

  /**
   *  A field that stores the Ensemble Data Source ID (one byte) in a ByteBuffer
   */
  private ByteBuffer dataSourceID = ByteBuffer.allocate(1);

  /**
   *  A field that stores the binary byte address offsets where data types
   * 1 to n are stored  (in a ByteBuffer).  Each address offset is 
   * represented by a 2-byte sequence.  For instance, the offset for 
   * Data Type #1 (FixedLeader) is the first two bytes of this ByteBuffer.
   * These two bytes are in Little Endian order, and must be read as such.
   * Add 1 to this address location to get the binary byte location of the
   * beginning of Data Type #1 (FixedLeader). Repeat this to read 
   * Data Type #2 (VariableLeader), Data Type #3 (VelocityProfile), etc... 
   */
  private ByteBuffer dataTypeOffsets;

  /**
   *  A field that stores the default Ensemble Header ID ( 0x7F7F)
   */
   private static final ByteBuffer DEFAULT_HEADER_ID = 
     ByteBuffer.wrap( new byte[]{0x7F,0x7F} );

  /**
   *  A field that stores the Ensemble Header ID (2-bytes) in a ByteBuffer
   */
  private ByteBuffer headerID = DEFAULT_HEADER_ID;
  
  /**
   *  A field that stores the Ensemble Header Spare (one byte) in a ByteBuffer
   *  This field is reserved for RDI internal use.
   */
  private ByteBuffer headerSpare = ByteBuffer.allocate(1);

  /**
   *  A field that stores the number of bytes from the start of the
   *  current ensemble up to, but not including, the 2-byte checksum, in a 
   *  ByteBuffer that has Little Endian order (same as the data stream)
   */
  private ByteBuffer numberOfBytesInEnsemble = ByteBuffer.allocate(2);

  /**
   *  A field that stores the number of data types selected for collection.
   */
  private ByteBuffer numberOfDataTypes = ByteBuffer.allocate(1);



  /**
   *  Constructor.  This method populates the Header fields from the given
   *  ByteBuffer of data passed in as an argument.
   *
   * @param ensembleBuffer the ByteBuffer that contains the binary ensemble data
   */
  public EnsembleHeader( ByteBuffer ensembleBuffer, Ensemble ensemble ) {
    
    // prepare the ensemble buffer for reading
    ensembleBuffer.flip();
    
    // define the temporary arrays for passing bytes
    byte[] oneByte  = new byte[1];
    byte[] twoBytes = new byte[2];
    
    // set each of the Ensemble Header fields n the order that they are 
    // read from the byte stream
    ensembleBuffer.get(twoBytes);
    setHeaderID(twoBytes);
    ensemble.addToByteSum(twoBytes);
    
    ensembleBuffer.get(twoBytes);
    setNumberOfBytesInEnsemble(twoBytes);
    ensemble.addToByteSum(twoBytes);
    
    ensembleBuffer.get(oneByte);
    setHeaderSpare(oneByte);
    ensemble.addToByteSum(oneByte);
    
    ensembleBuffer.get(oneByte); 
    setNumberOfDataTypes(oneByte);
    ensemble.addToByteSum(oneByte);
    
    byte[] offsetBytes = new byte[( getNumberOfDataTypes().getInt() * 2 ) ];
    ensembleBuffer.get(offsetBytes);
    setDataTypeOffsets(offsetBytes);
    ensemble.addToByteSum(offsetBytes);
    
    // set the dataTypeOffsets ByteBuffer size
    dataTypeOffsets = ByteBuffer.allocate( (getNumberOfDataTypes().getInt() * 2 ) );
  }

  /**
   * A method that returns the Ensemble headerID field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getHeaderID(){
    return this.headerID;
  }

  /**
   * A method that returns the Ensemble headerSpare field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getHeaderSpare(){
    return this.headerSpare;
  }

  /**
   *  A method that returns the offset for the given Data Type number.  
   *  For instance, The offset for Data Type #1 (FixedLeader) will be returned
   *  by calling this method with a dataTypeNumber argument of 1.
   *
   * @param dataTypeNumber  the number of the Data Type desired (as a ByteBuffer)
   */
   protected ByteBuffer getDataTypeOffset (int dataTypeNumber) {
     
     // prepare the ByteBuffer for reading
     dataTypeOffsets.flip();
     
     // read the offset by setting the position based on the desired type
     dataTypeOffsets.position( ( (dataTypeNumber * 2 ) - 2 ) );
     ByteBuffer dataTypeOffset = dataTypeOffsets.get( new byte[2]);
     return dataTypeOffset;       
   }
   
  /**
   * A method that returns the numberOfBytesInEnsemble field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getNumberOfBytesInEnsemble(){   
    return this.numberOfBytesInEnsemble;
  }

  /**
   * A method that returns the numberOfDataTypes field contents 
   * as a ByteBuffer.
   */
  protected ByteBuffer getNumberOfDataTypes(){   
    return this.numberOfDataTypes;
  }

  /**
   * A method that sets the dataTypesOffsets field from the given 
   * byte array. The byteArray argument must be equal to the numberOfDataTypes
   * field times 2 (since each offset takes 2 bytes).
   *
   * @param byteArray  the 2-byte array that contains the header ID
   */
  private void setDataTypeOffsets( byte[] byteArray ) {
    this.numberOfBytesInEnsemble.put(byteArray);
  }

  /**
   * A method that sets the Ensemble Header ID field from the given 
   * byte array. The byteArray argument must be 2 bytes in size.
   *
   * @param byteArray  the 2-byte array that contains the header ID
   */
  private void setHeaderID( byte[] byteArray ) {
    this.numberOfBytesInEnsemble.put(byteArray);
  }

  /**
   * A method that sets the Header Spare field from the given 
   * byte array. The byteArray argument must be 1 bytes in size.
   *
   * @param byteArray  the 1-byte array that contains the header spare
   */
  private void setHeaderSpare( byte[] byteArray ) {
    this.headerSpare.put(byteArray);
  }

  /**
   * A method that sets the numberOfBytesInEnsemble field from the given
   * byte array. The byteArray argument must be 2-bytes in size.
   *
   * @param byteArray  the 2-byte byte array that contains the header bytes
   */
  private void setNumberOfBytesInEnsemble( byte[] byteArray ) {
    this.numberOfBytesInEnsemble.put(byteArray);
  }

  /**
   * A method that sets the numberOfDataTypes field from the given
   * byte array. The byteArray argument must be 1-byte in size.
   *
   * @param byteArray  the 1-byte byte array that contains the header bytes
   */
  private void setNumberOfDataTypes( byte[] byteArray ) {
    this.numberOfDataTypes.put(byteArray);
  }
}