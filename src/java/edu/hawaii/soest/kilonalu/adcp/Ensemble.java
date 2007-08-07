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
   *  A byte sum variable used to hold the cumulative sum of the bytes read from
   *  the byte stream for this ensemble.  As each byte of the ensemble is read,
   *  the byte value should be added to this variable.  Once all of the bytes
   *  are read, this byte sum should be compared with the checksum found at the
   *  end of the stream, and the checksum should equal the modulo 65535 value
   *  of this byte sum.
   */
  private double ensembleByteSum;

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
    this.ensembleHeader = new EnsembleHeader(ensembleBuffer, this);     
    
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
    
    // Finally, set the last two fields of the Ensemble
    byte[] twoBytes = new byte[2];
    ensembleBuffer.get(twoBytes);
    setReservedBIT(twoBytes);
    addToByteSum(twoBytes);
    ensembleBuffer.get(twoBytes);
    setChecksum(twoBytes);
    addToByteSum(twoBytes);
  
  }

  /**
   * A method that adds the given values of the given byte array to the
   * ensembleByteSum field.  Each byte is added individually in order for the 
   * ensembleByteSum to eventually be compared to the checksum stated in the
   * data stream.
   */
   protected void addToByteSum(byte[] byteArray) {
     
     // iterate through the bytes and add them to ensembleByteSum
     for ( int i : byteArray ) ensembleByteSum += byteArray[i];
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

   /**
    * A method that returns the Ensemble baseFrequencyIndex field contents 
    * as an int.
    */
   public int getBaseFrequencyIndex() {
     return ensembleFixedLeader.getBaseFrequencyIndex().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble beamAngle field contents 
    * as an int.
    */
    public int getBeamAngle() {
      return ensembleFixedLeader.getBeamAngle().order(
           ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble binOneDistance field contents 
    * as an int.
    */
   public int getBinOneDistance() {
     return ensembleFixedLeader.getBinOneDistance().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble blankAfterTransmit field contents 
    * as an int.
    */
   public int getBlankAfterTransmit() {
     return ensembleFixedLeader.getBlankAfterTransmit().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble coordinate transform methods used
    * as an int.  The following show the return values and their meaning:
    * 1 - none, beam coordinates used.
    * 2 - instrument coordinates used.
    * 3 - ship coordinates used.
    * 4 - earth coordinates used.
    */
   public int getCoordinateTransformParams() {
     int coordTransform = 
       ensembleFixedLeader.getCoordinateTransformParams().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     coordTransform = (coordTransform >> 3) << 3; // clear the first 7 bits
     coordTransform = (coordTransform << 27) >> 27; // clear all but bits 4,5
     
     int returnValue = 0;
                                
     // define the coordinate transformations from the manual
     final int BEAM = 0;        // ---00--- = NO TRANSFORMATION (BEAM COORDS) 
     final int INSTRUMENT = 8;  // ---01--- = INSTRUMENT COORDINATES 
     final int SHIP = 16;       // ---10--- = SHIP COORDINATES 
     final int EARTH = 24;      // ---11--- = EARTH COORDINATES 
     
     //find the coordinate transformation setting
     switch ( coordTransform ) {
       case BEAM:
         returnValue = 0;
         break;
       case INSTRUMENT:
         returnValue = 1;
         break;
       case SHIP:
         returnValue = 3;
         break;
       case EARTH:
         returnValue = 4;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns whether or not tilts (pitch and roll) are used in 
    * coordinate transforms, as an int.  A return of 0 indicates tilts are not
    * used, and a return of 1 indicates that tilts are used. 
    */
   public int getTransformTiltsSetting() {
     int coordTransform = 
       ensembleFixedLeader.getCoordinateTransformParams().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     coordTransform = (coordTransform >> 2) << 2; // clear the first 2 bits
     coordTransform = (coordTransform << 29) >> 29; // clear all but bit 3
     
     int returnValue = 0;
                                
     // define the tilt settings from the manual
     final int TILTS_NOT_USED = 0;  // -----0-- = TILTS (PITCH AND ROLL) USED 
     final int TILTS_USED     = 4;  // -----1-- = TILTS (PITCH AND ROLL) USED 
     
     //find the coordinate transformation setting
     switch ( coordTransform ) {
       case TILTS_NOT_USED:
         returnValue = 0;
         break;
       case TILTS_USED:
         returnValue = 1;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns whether or not 3-beam solutions are used in 
    * coordinate transforms, as an int.  A return of 0 indicates 3-beams solutions
    * are not used, and a return of 1 indicates that 3-beam solutions are used. 
    */
   public int getTransformThreeBeamSetting() {
     int coordTransform = 
       ensembleFixedLeader.getCoordinateTransformParams().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     coordTransform = (coordTransform >> 1) << 1;   // clear bit 1
     coordTransform = (coordTransform << 30) >> 30; // clear all but bit 2
     
     int returnValue = 0;
                                
     // define the tilt settings from the manual
     final int THREE_NOT_USED = 0;  // ------0- = 3-BEAM SOLUTION NOT USED 
     final int THREE_USED     = 2;  // ------1- = 3-BEAM SOLUTION USED 
     
     //find the coordinate transformation setting
     switch ( coordTransform ) {
       case THREE_NOT_USED:
         returnValue = 0;
         break;
       case THREE_USED:
         returnValue = 1;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns whether or not 3-beam solutions are used in 
    * coordinate transforms, as an int.  A return of 0 indicates 3-beams solutions
    * are not used, and a return of 1 indicates that 3-beam solutions are used. 
    */
   public int getTransformBinMappingSetting() {
     int coordTransform = 
       ensembleFixedLeader.getCoordinateTransformParams().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     coordTransform = (coordTransform << 31) >> 31; // clear all but bit 1
     
     int returnValue = 0;
                                
     // define the tilt settings from the manual
     final int BIN_MAP_NOT_USED = 0;  // -------0 = BIN MAPPING NOT USED 
     final int BIN_MAP_USED     = 1;  // -------1 = BIN MAPPING USED 
     
     //find the coordinate transformation setting
     switch ( coordTransform ) {
       case BIN_MAP_NOT_USED:
         returnValue = 0;
         break;
       case BIN_MAP_USED:
         returnValue = 1;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the Ensemble cpuFirmwareRevision field contents 
    * as an int.
    */
   public int getCpuFirmwareRevision() {
     return ensembleFixedLeader.getCpuFirmwareRevision().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble headerID field contents 
    * as an int.
    */
   public int getCpuFirmwareVersion() {
     return ensembleFixedLeader.getCpuFirmwareVersion().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble cpuFirmwareVersion field contents 
    * as an double.
    */
   public double getCpuBoardSerialNumber() {
     return ensembleFixedLeader.getCpuBoardSerialNumber().order(
          ByteOrder.LITTLE_ENDIAN).getDouble();
   }

   /**
    * A method that returns the Ensemble depthCellLength field contents 
    * as an int.
    */
   public int getDepthCellLength() {
     return ensembleFixedLeader.getDepthCellLength().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble errorVelocityThreshold field contents 
    * as an int.
    */
   public int getErrorVelocityThreshold() {
     return ensembleFixedLeader.getErrorVelocityThreshold().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble falseTargetThreshold field contents 
    * as an int.
    */
   public int getFalseTargetThreshold() {
     return ensembleFixedLeader.getFalseTargetThreshold().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble fixedLeaderID field contents 
    * as an int.
    */
   public int getFixedLeaderID() {
     return ensembleFixedLeader.getFixedLeaderID().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble fixedLeaderSpare field contents 
    * as an int.
    */
   public int getFixedLeaderSpare() {
     return ensembleFixedLeader.getFixedLeaderSpare().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble headingAlignment field contents 
    * as an float.
    */
   public float getHeadingAlignment() {
     return ensembleFixedLeader.getHeadingAlignment().order(
          ByteOrder.LITTLE_ENDIAN).getFloat();
   }

   /**
    * A method that returns the Ensemble headingBias field contents 
    * as an float.
    */
   public float getHeadingBias() {
     return ensembleFixedLeader.getHeadingBias().order(
          ByteOrder.LITTLE_ENDIAN).getFloat();
   }

   /**
    * A method that returns the Ensemble lagLength field contents 
    * as an int.
    */
   public int getLagLength() {
     return ensembleFixedLeader.getLagLength().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble lowCorrelationThreshold field contents 
    * as an int.
    */
   public int getLowCorrelationThreshold() {
     return ensembleFixedLeader.getLowCorrelationThreshold().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble numberOfBeams field contents 
    * as an int.
    */
   public int getNumberOfBeams() {
     return ensembleFixedLeader.getNumberOfBeams().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble numberOfCells field contents 
    * as an int.
    */
   public int getNumberOfCells() {
     return ensembleFixedLeader.getNumberOfCells().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }


   /**
    * A method that returns the Ensemble numberOfCodeRepetitions field contents 
    * as an int.
    */
   public int getNumberOfCodeRepetitions() {
     return ensembleFixedLeader.getNumberOfCodeRepetitions().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble pdRealOrSimulatedFlag field contents 
    * as an int.
    */
   public int getPdRealOrSimulatedFlag() {
     return ensembleFixedLeader.getPdRealOrSimulatedFlag().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble percentGoodMinimum field contents 
    * as an int.
    */
   public int getPercentGoodMinimum() {
     return ensembleFixedLeader.getPercentGoodMinimum().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble pingHundredths field contents 
    * as an int.
    */
   public int getPingHundredths() {
     return ensembleFixedLeader.getPingHundredths().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble pingMinutes field contents 
    * as an int.
    */
   public int getPingMinutes() {
     return ensembleFixedLeader.getPingMinutes().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble pingSeconds field contents 
    * as an int.
    */
   public int getPingSeconds() {
     return ensembleFixedLeader.getPingSeconds().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble pingsPerEnsemble field contents 
    * as an int.
    */
   public int getPingsPerEnsemble() {
     return ensembleFixedLeader.getPingsPerEnsemble().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble profilingMode field contents 
    * as an int.
    */
   public int getProfilingMode() {
     return ensembleFixedLeader.getProfilingMode().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble referenceLayerEnd field contents 
    * as an int.
    */
   public int getReferenceLayerEnd() {
     return ensembleFixedLeader.getReferenceLayerEnd().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble referenceLayerStart field contents 
    * as an int.
    */
    public int getReferenceLayerStart() {
      return ensembleFixedLeader.getReferenceLayerStart().order(
           ByteOrder.LITTLE_ENDIAN).getInt();
    }

   /**
    * A method that returns the Ensemble sensorAvailability field contents 
    * as an int.
    */
   public int getSensorAvailability() {
     return ensembleFixedLeader.getSensorAvailability().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble sensorSource field contents 
    * as an int.
    */
   public int getSensorSource() {
     return ensembleFixedLeader.getSensorSource().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - speed of sound is not calculated from depth, salinity, and temperature
    * 1 - speed of sound is calculated from depth, salinity, and temperature
    */
   public int getSensorSpeedOfSoundSetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting >> 6) << 6; // clear the first 6 bits
     sensorSetting = (sensorSetting << 25) >> 25; // clear all but bit 7
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int NO_SOS_CALC = 0; // -0------ = DOES NOT CALCULATE SPEED OF SOUND 
     final int SOS_CALC    = 64;// -1------ = CALCULATES SPEED OF SOUND 
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case NO_SOS_CALC:
         returnValue = 0;
         break;
       case SOS_CALC:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - depth sensor is not used for calculations
    * 1 - depth sensor is used for calculations
    */
   public int getSensorDepthSetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting >> 5) << 5; // clear the first 5 bits
     sensorSetting = (sensorSetting << 26) >> 26; // clear all but bit 6
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int DEPTH_NOT_USED = 0; // --0----- = DOES NOT USE DEPTH TO CALCULATE
     final int DEPTH_USED    = 32; // --1----- = USES DEPTH TO CALCULATE
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case DEPTH_NOT_USED:
         returnValue = 0;
         break;
       case DEPTH_USED:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - heading is not used from transducer heading sensor
    * 1 - heading is used from transducer heading sensor
    */
   public int getSensorHeadingSetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting >> 4) << 4; // clear the first 4 bits
     sensorSetting = (sensorSetting << 27) >> 27; // clear all but bit 5
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int HEADING_NOT_USED = 0; // ---0---- = DOES NOT USE HEADING SENSOR
     final int HEADING_USED    = 16; // ---1---- = USES HEADING SENSOR
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case HEADING_NOT_USED:
         returnValue = 0;
         break;
       case HEADING_USED:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - pitch is not used from transducer pitch sensor
    * 1 - pitch is used from transducer pitch sensor
    */
   public int getSensorPitchSetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting >> 3) << 3; // clear the first 3 bits
     sensorSetting = (sensorSetting << 28) >> 28; // clear all but bit 4
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int PITCH_NOT_USED = 0; // ----0--- = DOES NOT USE PITCH SENSOR
     final int PITCH_USED     = 8; // ----1--- = USES PITCH SENSOR
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case PITCH_NOT_USED:
         returnValue = 0;
         break;
       case PITCH_USED:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - roll is not used from transducer roll sensor
    * 1 - roll is used from transducer roll sensor
    */
   public int getSensorRollSetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting >> 2) << 2; // clear the first 2 bits
     sensorSetting = (sensorSetting << 29) >> 29; // clear all but bit 3
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int ROLL_NOT_USED = 0; // -----0-- = DOES NOT USE ROLL SENSOR
     final int ROLL_USED     = 4; // -----1-- = USES ROLL SENSOR
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case ROLL_NOT_USED:
         returnValue = 0;
         break;
       case ROLL_USED:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - salinity is not used from conductivity sensor
    * 1 - salinity is used from conductivity sensor
    */
   public int getSensorSalinitySetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting >> 1) << 1; // clear the first bit
     sensorSetting = (sensorSetting << 30) >> 30; // clear all but bit 2
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int SALINITY_NOT_USED = 0; // ------0- = DOES NOT USE SALINITY
     final int SALINITY_USED     = 2; // ------1- = USES SALINITY
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case SALINITY_NOT_USED:
         returnValue = 0;
         break;
       case SALINITY_USED:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - temperature is not used from transducer sensor
    * 1 - temperature is used from transducer sensor
    */
   public int getSensorTemperatureSetting() {
     int sensorSetting = 
       ensembleFixedLeader.getSensorSource().order(
         ByteOrder.LITTLE_ENDIAN).getInt();
     sensorSetting = (sensorSetting << 31) >> 31; // clear all but bit 1
     
     int returnValue = 0;
                                
     // define the sensor source settings from the manual
     final int TEMPERATURE_NOT_USED = 0; // ------0- = DOES NOT USE TEMPERATURE
     final int TEMPERATURE_USED     = 2; // ------1- = USES TEMPERATURE
     
     //find the sensor source setting
     switch ( sensorSetting ) {
       case TEMPERATURE_NOT_USED:
         returnValue = 0;
         break;
       case TEMPERATURE_USED:
         returnValue = 1;
         break;
     }
     // reset the sensorSource ByteBuffer for other get methods
     ensembleFixedLeader.getSensorSource().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the Ensemble serialNumber field contents 
    * as an int.
    */
   public int getSerialNumber() {
     return ensembleFixedLeader.getSerialNumber().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble signalProcessingMode field contents 
    * as an int.
    */
   public int getSignalProcessingMode() {
     return ensembleFixedLeader.getSignalProcessingMode().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble systemBandwidth field contents 
    * as an int.
    */
   public int getSystemBandwidth() {
     return ensembleFixedLeader.getSystemBandwidth().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble systemConfiguration field contents 
    * as an int.
    */
   public int getSystemConfiguration() {
     return ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the beam pattern component of the Ensemble 
    * systemConfiguration field contents as an int.  A return value of 0
    * indicates a concave beam pattern, while a return value of 1 indicates
    * a convex beam pattern.
    */
   public int getBeamPattern() {
     int systemConfig = ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
     int returnValue = 0;
     
     systemConfig = (systemConfig >> 3) << 3; // clear the first 3 bits
     systemConfig = (systemConfig << 28) >> 28; // clear all but bit 4
     
     // define the patterns from the manual
     final int CONCAVE = 0;  //  - - - - 0 - - -  CONCAVE BEAM PATTERN
     final int CONVEX  = 8;  //  - - - - 1 - - -  CONVEX BEAM PATTERN 
     
     //find the beam pattern
     switch ( systemConfig ) {
       case CONCAVE:
         returnValue = 0;
         break;
       case CONVEX:
         returnValue = 1;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the direction of the beam from the 
    * systemConfiguration field contents as an int.  A return value of 0
    * indicates a down facing beam, while a return value of 1 
    * indicates an up facing beam.
    */
   public int getBeamDirection() {
     int systemConfig = ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
     int returnValue = 0;
     
     systemConfig = (systemConfig >> 7) << 7; // clear the first 7 bits
     systemConfig = (systemConfig << 24) >> 24; // clear all but bit 8
     
     // define the directions from the manual
     final int DOWN = 0;  //  0 - - - - - - -  DOWN FACING BEAM
     final int UP   = 128; //  1 - - - - - - -  UP-FACING BEAM 
     
     //find the beam direction
     switch ( systemConfig ) {
       case DOWN:
         returnValue = 0;
         break;
       case UP:
         returnValue = 1;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the attachement of the transducer from the 
    * systemConfiguration field contents as an int.  A return value of 0
    * indicates the transducer is not attached, while a return value of 1 
    * indicates the transducer is attached.
    */
   public int getTransducerAttachment() {
     int systemConfig = ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
     int returnValue = 0;
     
     systemConfig = (systemConfig >> 6) << 6; // clear the first 6 bits
     systemConfig = (systemConfig << 25) >> 25; // clear all but bit 7
     
     // define the attachemnt from the manual
     final int NOT_ATTACHED = 0;  //  - 0 - - - - - -  XDCR HD NOT ATTACHED
     final int ATTACHED     = 64; //  - 1 - - - - - -  XDCR HD ATTACHED 
     
     //find the beam pattern
     switch ( systemConfig ) {
       case NOT_ATTACHED:
         returnValue = 0;
         break;
       case ATTACHED:
         returnValue = 1;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the frequency component of the Ensemble 
    * systemConfiguration field contents as an int.  The return values
    * are in kHz, e.g. 1200 kHz.
    */
   public int getSystemFrequency() {
     int systemConfig = ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
     int returnValue = 0;
     
     systemConfig = (systemConfig << 29) >> 29; // clear all but first 3 bits
     
     // define the frequencies from the manual
     final int SEVENTY_FIVE   = 0;  //  - - - - - 0 0 0   75-kHz SYSTEM
     final int ONE_H_FIFTY    = 1;  //  - - - - - 0 0 1  150-kHz SYSTEM
     final int THREE_HUNDRED  = 2;  //  - - - - - 0 1 0  300-kHz SYSTEM
     final int SIX_HUNDRED    = 3;  //  - - - - - 0 1 1  600-kHz SYSTEM
     final int TWELVE_HUNDRED = 4;  //  - - - - - 1 0 0 1200-kHz SYSTEM
     final int TWENTY_FOUR_H  = 5;  //  - - - - - 1 0 1 2400-kHz SYSTEM
     
     //find the frequency
     switch ( systemConfig ) {
       case SEVENTY_FIVE   :
         returnValue = 75;
         break;
       case ONE_H_FIFTY    :
         returnValue = 150;
         break;
       case THREE_HUNDRED  :
         returnValue = 300;
         break;
       case SIX_HUNDRED    :
         returnValue = 600;
         break;
       case TWELVE_HUNDRED :
         returnValue = 1200;
         break;
       case TWENTY_FOUR_H  :
         returnValue = 2400;       
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }
   
   /**
    * A method that returns the sensor configuration component of the Ensemble 
    * systemConfiguration field contents as an int.  A return value of 1
    * indicates sensor config #1, a return value of 2 indicates sensor 
    * config #2, and a return value of 3 indicates sensor config 3.
    */
   public int getSensorConfiguration() {
     int systemConfig = ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
     int returnValue = 0;
     
     systemConfig = (systemConfig >> 4) << 4; // clear the first 4 bits
     systemConfig = (systemConfig << 26) >> 26; // clear all but bits 5,6
     
     // define the configs from the manual
     final int SENSOR_CONFIG_1 = 0;    //  - - 0 0 - - - -  SENSOR CONFIG #1
     final int SENSOR_CONFIG_2 = 16;   //  - - 0 1 - - - -  SENSOR CONFIG #2 
     final int SENSOR_CONFIG_3 = 32;   //  - - 1 0 - - - -  SENSOR CONFIG #3 
     
     //find the beam pattern
     switch ( systemConfig ) {
       case SENSOR_CONFIG_1:
         returnValue = 1;
         break;
       case SENSOR_CONFIG_2:
         returnValue = 2;
         break;
       case SENSOR_CONFIG_3:
         returnValue = 3;
         break;
     }
     // reset the systemConfiguration ByteBuffer for other get methods
     ensembleFixedLeader.getSystemConfiguration().rewind();
     
     return returnValue;
   }

   /**
    * A method that returns the Ensemble systemPower field contents 
    * as an int.
    */
   public int getSystemPower() {
     return ensembleFixedLeader.getSystemPower().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble transmitLagDistance field contents 
    * as an int.
    */
   public int getTransmitLagDistance() {
     return ensembleFixedLeader.getTransmitLagDistance().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble transmitPulseLength field contents 
    * as an int.
    */
   public int getTransmitPulseLength() {
     return ensembleFixedLeader.getTransmitPulseLength().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that determines whether or not the Ensemble is valid by
    * comparing the cumulative byte sum value with the stated checksum
    * value in the ensemble byte stream.  Returns true if it is valid.
    */
   public boolean isValid() {
     boolean isValid = false;
     
     if (  ensembleByteSum % 65535 == 
          (checksum.order(ByteOrder.LITTLE_ENDIAN).getDouble()) ) {
       isValid = true;
     }
     return isValid;
   }

   /**
    * A method that sets the checksum field from the given
    * byte array. The byteArray argument must be 2-bytes in size.
    *
    * @param byteArray  the 2-byte array that contains the checksum bytes
    */
   private void setChecksum(byte[] byteArray) {
     this.checksum.put(byteArray);
   }

   /**
    * A method that sets the reservedBIT field from the given
    * byte array. The byteArray argument must be 2-bytes in size.
    *
    * @param byteArray  the 2-byte array that contains the checksum bytes
    */
   private void setReservedBIT(byte[] byteArray) {
     this.reservedBIT.put(byteArray);
   }

}