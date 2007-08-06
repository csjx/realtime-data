/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a single ensemble of data produced by
 *            an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *            default PD0 format as described in RDI's "Workhorse Commands and 
 *            Output Data Format" manual, P/N 957-6156-00 (March 2005)
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
package edu.hawaii.soest.kilonalu.adcp;

import edu.hawaii.soest.kilonalu.adcp.EnsembleBottomTrack;
import edu.hawaii.soest.kilonalu.adcp.EnsembleCorrelationProfile;
import edu.hawaii.soest.kilonalu.adcp.EnsembleDataType;
import edu.hawaii.soest.kilonalu.adcp.EnsembleEchoIntensityProfile;
import edu.hawaii.soest.kilonalu.adcp.EnsembleFixedLeader;
import edu.hawaii.soest.kilonalu.adcp.EnsembleHeader;
import edu.hawaii.soest.kilonalu.adcp.EnsembleMicroCAT;
import edu.hawaii.soest.kilonalu.adcp.EnsemblePercentGoodProfile;
import edu.hawaii.soest.kilonalu.adcp.EnsembleStatusProfile;
import edu.hawaii.soest.kilonalu.adcp.EnsembleVariableLeader;
import edu.hawaii.soest.kilonalu.adcp.EnsembleVelocityProfile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.HashMap;
/**
 *  A class that represents a single ensemble of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format. 
 */
public class Ensemble {
    
  /**
   *  A field that stores the RDI reserved BIT data (two bytes) in a ByteBuffer
   *  which contains two bytes of data for internal RDI use.
   */
  private ByteBuffer reservedBIT = ByteBuffer.allocate(2);
  
  /**
   *  A field that stores the Ensemble checksum data (two bytes) in a ByteBuffer
   *  which is used to verify the size of the Ensemble in bytes.  The value of the
   *  checksum is a modulo 65535 number, and must be evaluated as such.
   */
  private ByteBuffer checksum = ByteBuffer.allocate(2);

  /*
   *  An instance of the Header component of the Ensemble
   */
  private EnsembleHeader ensembleHeader;

  /*
   *  An instance of the Fixed Leader component of the Ensemble
   */
  private EnsembleFixedLeader ensembleFixedLeader;

  /*
   *  An instance of the Variable Leader component of the Ensemble
   */
  private EnsembleVariableLeader ensembleVariableLeader;

  /*
   *  An instance of the Velocity Profile component of the Ensemble
   */
  private EnsembleVelocityProfile ensembleVelocityProfile;
  
  /*
   *  An instance of the Correlation Profile component of the Ensemble
   */
  private EnsembleCorrelationProfile ensembleCorrelationProfile;
  
  /*
   *  An instance of the Echo Intensity Profile component of the Ensemble
   */
  private EnsembleEchoIntensityProfile ensembleEchoIntensityProfile;
  
  /*
   *  An instance of the Percent Good Profile component of the Ensemble
   */
  private EnsemblePercentGoodProfile ensemblePercentGoodProfile;
  
  /*
   *  An instance of the Staus Profile component of the Ensemble
   */
  private EnsembleStatusProfile ensembleStatusProfile;
  
  /*
   *  An instance of the Bottom Track Data component of the Ensemble
   */
  private EnsembleBottomTrack ensembleBottomTrack;
  
  /*
   *  An instance of the MicroCAT component of the Ensemble
   */
  private EnsembleMicroCAT ensembleMicroCAT;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  Velocity Profile Data.  The default is false.
   */
  private boolean hasVelocityProfile = false;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  Correlation Profile Data.  The default is false.
   */
  private boolean hasCorrelationProfile = false;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  Echo Intensity Profile Data.  The default is false.
   */
  private boolean hasEchoIntensityProfile = false;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  Percent Good Profile Data.  The default is false.
   */
  private boolean hasPercentGoodProfile = false;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  Status Profile Data.  The default is false.
   */
  private boolean hasStatusProfile = false;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  Bottom Track Data.  The default is false.
   */
  private boolean hasBottomTrackData = false;

  /*
   *  A boolean value indicating whether or not this Ensemble contains
   *  MicroCAT Data.  The default is false.
   */
  private boolean hasMicroCATData = false;

  /**
   *  A hashmap used to store a mapping between the Data Type # found in
   *  the ensemble and the Data Type ID constant.  For instance, the map 
   *  would indicate that Data Type #1 == Fixed Leader, Data Type #2 ==
   *  Variable Leader, Data Type #3 = Velocity Profile, etc.
   */
   private HashMap<EnsembleDataType, Integer> dataTypeMap = 
     new HashMap<EnsembleDataType, Integer>();


  /**
   *  Constructor:  Builds all of the components of the Ensemble from
   *  the ByteBuffer data being passed in.
   *
   *  @param ensembleBuffer  the ByteBuffer that contains the binary ensemble data
   */
  public Ensemble(ByteBuffer ensembleBuffer) {
    
    // first, build the EnsembleHeader to be used in subsequent parsing
    this.ensembleHeader = new EnsembleHeader(ensembleBuffer);     
    
    // create the components of the ensemble based on the metadata content
    // of the EnsembleHeader
    this.ensembleFixedLeader = 
      new EnsembleFixedLeader(ensembleBuffer, this);     
    this.ensembleVariableLeader = 
      new EnsembleVariableLeader(ensembleBuffer, this);     
    
    // build each of the collected data types
    if ( getNumberOfDataTypes() > 2 ) {
      
      // identify which data types are present in the Ensemble based
      // on the Data Type ID
      findDataTypes(ensembleBuffer);
      
      
      // build 'em if we got 'em
      if ( hasVelocityProfile ){
        this.ensembleVelocityProfile = 
          new EnsembleVelocityProfile(ensembleBuffer, this);
      }
      
      if ( hasCorrelationProfile ) {
        this.ensembleCorrelationProfile = 
          new EnsembleCorrelationProfile(ensembleBuffer, this);
      }
      
      if ( hasEchoIntensityProfile ) {
        this.ensembleEchoIntensityProfile = 
          new EnsembleEchoIntensityProfile(ensembleBuffer, this);
      }

      if ( hasPercentGoodProfile ) {
        this.ensemblePercentGoodProfile = 
          new EnsemblePercentGoodProfile(ensembleBuffer, this);
      }

      if ( hasStatusProfile ) {
        this.ensembleStatusProfile = 
          new EnsembleStatusProfile(ensembleBuffer, this);
      }

      if ( hasBottomTrackData ) {
        this.ensembleBottomTrack = 
          new EnsembleBottomTrack(ensembleBuffer, this);
      }

      if ( hasMicroCATData ) {
        this.ensembleMicroCAT = 
          new EnsembleMicroCAT(ensembleBuffer, this);
      }
    }
    
  }

  /**
   *  A method that identifies the types of data that are collected in the
   *  given ensemble ByteBuffer.  This method sets the has<DataType> boolean
   *  fields to true if it finds the Data Type ID at one of the Data Type 
   *  Offsets in the EnsembleHeader.  It also registers the particular DataType
   *  into the dataTypeMap with the Data Type #, so that we know that, for
   *  instance, the Velocity Profile data type is Data Type #3 in the ensemble.
   */
   private void findDataTypes(ByteBuffer ensembleBuffer ) {
     
       // search the ensemble byte buffer for the above IDs at each of the 
       // data type offset locations, and mark them present or not present
       ensembleBuffer.flip();
       
       for (int i = 1; i < getNumberOfDataTypes(); i++) {
         ensembleBuffer.position( getDataTypeOffset(i) + 1 );
         
         // once the cursor is in place, read the 2-byte ID marker, and test
         // it against the constant enumerated data types
         EnsembleDataType dataType = getDataType(
           ensembleBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt());
         
         // for each dataType ID marker found in the ensemble, cache it's 
         // sequence location and data type in a HashMap for later processing.
         // also, set boolean existence values for optional data types 
         switch ( dataType ) {
           
           case HEADER:
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case FIXED_LEADER:
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case VARIABLE_LEADER:
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case VELOCITY_PROFILE:
             hasVelocityProfile = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case CORRELATION_PROFILE:
             hasCorrelationProfile = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case ECHOINTENSITY_PROFILE:
             hasEchoIntensityProfile = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case PERCENTGOOD_PROFILE:
             hasPercentGoodProfile = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case STATUS_PROFILE:
             hasStatusProfile = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case BOTTOMTRACK_DATA:
             hasBottomTrackData = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
           case MICROCAT_DATA:
             hasMicroCATData = true;
             dataTypeMap.put(dataType, new Integer(i));
             break;
         }
         // prepare the buffer for rereading of the next dataType
         ensembleBuffer.rewind();
       }
   }

   /**
    * A method that returns the Ensemble checksum field contents 
    * as a int.
    */
   public int getChecksum(){
     return this.checksum.order(ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble headerID field contents 
    * as a int.
    */
   public int getHeaderID(){
     return ensembleHeader.getHeaderID().order(
       ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble headerSpare field contents 
    * as a int.
    */
   public int getHeaderSpare(){
     return ensembleHeader.getHeaderSpare().order(
       ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    *  A method that returns the Data Type given the 2-byte Data Type ID.  
    *  For instance, The offset for Data Type #1 (FixedLeader) will be returned
    *  by calling this method with a dataTypeNumber argument of 1.
    *
    * @param dataTypeID  the 2-byte ID of the Data Type being requested
    */
    public EnsembleDataType getDataType(int dataTypeID) {
      
      EnsembleDataType returnType = null;
      
      // compare the given dataTypeID
      if ( dataTypeID == 0x7f7f ) {
        returnType = EnsembleDataType.HEADER;
      }
      if ( dataTypeID == 0x0000 ) {
        returnType = EnsembleDataType.FIXED_LEADER;
      }
      if ( dataTypeID == 0x0080 ) {
        returnType = EnsembleDataType.VARIABLE_LEADER;
      }
      if ( dataTypeID == 0x0100 ) {
        returnType = EnsembleDataType.VELOCITY_PROFILE;
      }
      if ( dataTypeID == 0x0200 ) {
        returnType = EnsembleDataType.CORRELATION_PROFILE;
      }
      if ( dataTypeID == 0x0300 ) {
        returnType = EnsembleDataType.ECHOINTENSITY_PROFILE;
      }
      if ( dataTypeID ==0x0400 ) {
        returnType = EnsembleDataType.PERCENTGOOD_PROFILE;
      }
      if ( dataTypeID ==0x0500 ) {
        returnType = EnsembleDataType.STATUS_PROFILE;
      }
      if ( dataTypeID ==0x0600 ) {
        returnType = EnsembleDataType.BOTTOMTRACK_DATA;
      }
      if ( dataTypeID == 0x0800 ) {
        returnType = EnsembleDataType.MICROCAT_DATA;
      }
      return returnType;
    }

   /**
    *  A method that returns the Data Type Number from the order of data types
    *  in the ensemble as they have been indexed in the dataTypeMap.  The map
    *  contains the sequential location of each data type.
    *
    * @param ensembleDataType  the enumerated type from the EnsembleDataType enum
    */
    public int getDataTypeNumber( EnsembleDataType ensembleDataType ) {
      return this.dataTypeMap.get(ensembleDataType);
    }
   /**
    *  A method that returns the offset for the given Data Type number.  
    *  For instance, The offset for Data Type #1 (FixedLeader) will be returned
    *  by calling this method with a dataTypeNumber argument of 1.
    *
    * @param dataTypeNumber  the number of the Data Type desired (as an int)
    */
    public int getDataTypeOffset (int dataTypeNumber) {
     return ensembleHeader.getDataTypeOffset(dataTypeNumber).order(
         ByteOrder.LITTLE_ENDIAN).getInt();
    }

   /**
    * A method that returns the numberOfBytesInEnsemble field contents 
    * as a int.
    */
   public int getNumberOfBytesInEnsemble() {   
     return ensembleHeader.getNumberOfBytesInEnsemble().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the numberOfDataTypes field contents 
    * as a int.
    */
   public int getNumberOfDataTypes() {
     return ensembleHeader.getNumberOfDataTypes().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble reserved bit field contents 
    * as a int.  This is really just for RDI internal use.
    */
   public int getReservedBIT(){
     return this.reservedBIT.order(ByteOrder.LITTLE_ENDIAN).getInt();
   }


}