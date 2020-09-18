/*
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.HashMap;

/**
 *  A class that represents a single ensemble of data produced by
 *  an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *  default PD0 format.  This class is built from a series of subcomponents
 *  that model each of the sections of data found in an RDI PD0 ensemble. They
 *  include:
 *
 * <ul>
 *   <li> EnsembleHeader (always output)</li>
 *   <li> EnsembleFixedLeader (always output)</li>
 *   <li> EnsembleHeader (always output)</li>
 *   <li> EnsembleVelocityProfile (optionally output)</li>
 *   <li> EnsembleCorrelationProfile (optionally output)</li>
 *   <li> EnsembleEchoIntensityProfile (optionally output)</li>
 *   <li> EnsemblePercentGoodProfile (optionally output)</li>
 *   <li> EnsembleBottomTrack (optionally output) [this is unfinished]</li>
 *   <li> EnsembleMicroCAT (optionally output) [this is unfinished]</li>
 *   <li> EnsembleStatusProfile (optionally output) [this is unfinished]</li>
 * </ul>
 *
 *  Depending on the settings found in the Header, Fixed Leader, and Variable
 *  Leader, the remaining data objects may or may not be present.  the Ensemble
 *  class tests for their existence, and if found, creates the subcomponents.
 */
public class Ensemble {

    private Log logger = LogFactory.getLog(Ensemble.class);

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

  /**
   *  A field that stores the Ensemble byte count
   */
  private int ensembleByteCount = 0;

  
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
    
      // identify which data types are present in the Ensemble based
      // on the Data Type ID
      findDataTypes(ensembleBuffer);
      
      // create the components of the ensemble based on the metadata content
      // of the EnsembleHeader
      this.ensembleFixedLeader = 
        new EnsembleFixedLeader(ensembleBuffer, this);     
      this.ensembleVariableLeader = 
        new EnsembleVariableLeader(ensembleBuffer, this);     
      
    // build each of the collected data types
    if ( getNumberOfDataTypes() > 2 ) {

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
    //addToByteSum(twoBytes); // don't add checksum bytes to ensembleChecksum
  }

  /**
   * A method that adds the given values of the given byte array to the
   * ensembleByteSum field.  Each byte is added individually in order for the 
   * ensembleByteSum to eventually be compared to the checksum stated in the
   * data stream.
   * 
   * @param byteArray The byte array to be added
   */
   protected void addToByteSum(byte[] byteArray) {
     
     // iterate through the bytes and add them to ensembleByteSum
     for ( byte i : byteArray ) {
       ensembleByteSum += (i & 0xFF);
       ensembleByteCount++;
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
       ensembleBuffer.limit(ensembleBuffer.capacity());
       ensembleBuffer.position(0);
       
       for (int i = 1; i <= getNumberOfDataTypes(); i++) {
         int offset = getDataTypeOffset(i);
         ensembleBuffer.position( offset );
         // once the cursor is in place, read the 2-byte ID marker, and test
         // it against the constant enumerated data types
         EnsembleDataType dataType = getDataType(
           (int) ensembleBuffer.order(ByteOrder.LITTLE_ENDIAN).getShort());
         
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
    * 
    * @return csum The checksum of the Ensemble
    */
   public int getChecksum(){
     this.checksum.limit(this.checksum.capacity());
     this.checksum.position(0);
     int csum = 0;
     byte[] b = new byte[2];
     b[0] = this.checksum.get();
     b[1] = this.checksum.get();
     
     csum |= b[1] & 0xFF;
     csum <<= 8;
     csum |= b[0] & 0xFF;
     return csum;
   }

   /**
    * A method that returns the Ensemble headerID field contents 
    * as a int.
    * 
    * @return headerID The header ID
    */
   public int getHeaderID(){
     return (int) ensembleHeader.getHeaderID().order(
       ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble headerSpare field contents 
    * as a int.
    * 
    * @return headerSpare The header spare
    */
   public int getHeaderSpare(){
     return (int) ensembleHeader.getHeaderSpare().get();
   }

   /**
    *  A method that returns the Data Type given the 2-byte Data Type ID.  
    *  For instance, The offset for Data Type #1 (FixedLeader) will be returned
    *  by calling this method with a dataTypeNumber argument of 1.
    *
    * @param dataTypeID  the 2-byte ID of the Data Type being requested
    * 
    * @return returnType The Ensemble data type
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
      if ( dataTypeID == 0x0400 ) {
        returnType = EnsembleDataType.PERCENTGOOD_PROFILE;
      }
      if ( dataTypeID == 0x0500 ) {
        returnType = EnsembleDataType.STATUS_PROFILE;
      }
      if ( dataTypeID == 0x0600 ) {
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
    * @return dataTypeNumber The number of the data type
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
    * 
    * @return dataTypeOffset The offset for the data type
    */
    public int getDataTypeOffset (int dataTypeNumber) {
      ByteBuffer dataTypeOffset = ensembleHeader.getDataTypeOffset(dataTypeNumber);
      return (int) dataTypeOffset.order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

   /**
    * A method that returns the numberOfBytesInEnsemble field contents 
    * as a int.
    * 
    * @return bytes The number of bytes in the Ensemble
    */
   public int getNumberOfBytesInEnsemble() {   
     return (int) ensembleHeader.getNumberOfBytesInEnsemble().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the numberOfDataTypes field contents 
    * as a int.
    * 
    * @return numberOfDataTypes The number of data types
    */
   public int getNumberOfDataTypes() {
     ByteBuffer numberOfDataTypes = ensembleHeader.getNumberOfDataTypes();
     numberOfDataTypes.limit(numberOfDataTypes.capacity());
     numberOfDataTypes.position(0);
     return (int) numberOfDataTypes.get();
   }

   /**
    * A method that returns the Ensemble reserved bit field contents 
    * as a int.  This is really just for RDI internal use.
    * 
    * @return reservedBIT The reserved bit
    */
   public int getReservedBIT(){
     this.reservedBIT.limit(this.reservedBIT.capacity());
     this.reservedBIT.position(0);
     return (int) this.reservedBIT.order(ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble baseFrequencyIndex field contents 
    * as an int.
    * 
    * @return baseFrquencyIndex The base frequency index
    */
   public int getBaseFrequencyIndex() {
     return (int) ensembleFixedLeader.getBaseFrequencyIndex().order(
          ByteOrder.LITTLE_ENDIAN).get();
   }

   /**
    * A method that returns the Ensemble beamAngle field contents 
    * as an int.
    * 
    * @return beamAngle The beam angle
    */
    public int getBeamAngle() {
      return ensembleFixedLeader.getBeamAngle().get();
   }

   /**
    * A method that returns the Ensemble binOneDistance field contents 
    * as an int.
    * 
    * @return binOneDistance The distance to the first bin
    */
   public int getBinOneDistance() {
     return (int) ensembleFixedLeader.getBinOneDistance().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble blankAfterTransmit field contents 
    * as an int.
    * 
    * @return blankAfterTransmit The blank after transmit field
    */
   public int getBlankAfterTransmit() {
     return (int) ensembleFixedLeader.getBlankAfterTransmit().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble coordinate transform methods used
    * as an int.  The following show the return values and their meaning:
    * 1 - none, beam coordinates used.
    * 2 - instrument coordinates used.
    * 3 - ship coordinates used.
    * 4 - earth coordinates used.
    * 
    * @return returnValue The coordinate transform parameters
    */
   public int getCoordinateTransformParams() {
     int coordTransform = 
       (int) ensembleFixedLeader.getCoordinateTransformParams().get();
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
    * 
    * @return returnValue The coordinate transform tilt settings
    */
   public int getTransformTiltsSetting() {
     int coordTransform = 
       (int) ensembleFixedLeader.getCoordinateTransformParams().get();
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
    * 
    * @return returnValue The 3-beam solution setting
    */
   public int getTransformThreeBeamSetting() {
     int coordTransform = 
       (int) ensembleFixedLeader.getCoordinateTransformParams().get();
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
    * A method that returns the bin mapping settings
    * 
    * @return returnValue  The bin mapping settings
    */
   public int getTransformBinMappingSetting() {
     int coordTransform = 
       (int) ensembleFixedLeader.getCoordinateTransformParams().get();
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
    * 
    * @return firmwareRevision The revision of the firmware
    */
   public int getCpuFirmwareRevision() {
     return (int) ensembleFixedLeader.getCpuFirmwareRevision().get();
   }

   /**
    * A method that returns the Ensemble headerID field contents 
    * as an int.
    * 
    * @return cpufirmwareRevision The CPU firmware revision
    */
   public int getCpuFirmwareVersion() {
     return (int) ensembleFixedLeader.getCpuFirmwareVersion().get();
   }

   /**
    * A method that returns the Ensemble CpuBoardSerialNumber field contents 
    * as an double.
    * 
    * @return cpuBoardSerialNumber The CPU board serial number
    */
   public double getCpuBoardSerialNumber() {
     return ensembleFixedLeader.getCpuBoardSerialNumber().order(
          ByteOrder.LITTLE_ENDIAN).getDouble();
   }

   /**
    * A method that returns the Ensemble depthCellLength field contents 
    * as an int.
    * 
    * @return depthCellLength The depth cell length
    */
   public int getDepthCellLength() {
     return (int) ensembleFixedLeader.getDepthCellLength().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble errorVelocityThreshold field contents 
    * as an int.
    * 
    * @return errorVelocityThreshold The error velocity threshold
    */
   public int getErrorVelocityThreshold() {
     return (int) ensembleFixedLeader.getErrorVelocityThreshold().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble falseTargetThreshold field contents 
    * as an int.
    * 
    * @return falseTargetThreshold The false target threshold
    */
   public int getFalseTargetThreshold() {
     return (int) ensembleFixedLeader.getFalseTargetThreshold().get();
   }

   /**
    * A method that returns the Ensemble fixedLeaderID field contents 
    * as an int.
    * 
    * @return fixedLeaderID The fixed leader ID
    */
   public int getFixedLeaderID() {
     return (int) ensembleFixedLeader.getFixedLeaderID().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble fixedLeaderSpare field contents 
    * as an int.
    * 
    * @return fixedLeaderSpare The fixed leader spare
    */
   public int getFixedLeaderSpare() {
     return (int) ensembleFixedLeader.getFixedLeaderSpare().get();
   }

   /**
    * A method that returns the Ensemble headingAlignment field contents 
    * as an float.
    * 
    * @return headingAlignment The heading alignment
    */
   public float getHeadingAlignment() {
     return ensembleFixedLeader.getHeadingAlignment().order(
          ByteOrder.LITTLE_ENDIAN).getFloat();
   }

   /**
    * A method that returns the Ensemble headingBias field contents 
    * as an float.
    * 
    * @return headingBias The heading bias
    */
   public float getHeadingBias() {
     return ensembleFixedLeader.getHeadingBias().order(
          ByteOrder.LITTLE_ENDIAN).getFloat();
   }

   /**
    * A method that returns the Ensemble lagLength field contents 
    * as an int.
    * 
    * @return lagLength The lag length
    */
   public int getLagLength() {
     return (int) ensembleFixedLeader.getLagLength().get();
   }

   /**
    * A method that returns the Ensemble lowCorrelationThreshold field contents 
    * as an int.
    * 
    * @return lowCorrelationThreshold The low correlation threshold
    */
   public int getLowCorrelationThreshold() {
     return (int) ensembleFixedLeader.getLowCorrelationThreshold().get();
   }

   /**
    * A method that returns the Ensemble numberOfBeams field contents 
    * as an int.
    * 
    * @return numberOfBeams The number of beams
    */
   public int getNumberOfBeams() {
     return ensembleFixedLeader.getNumberOfBeams().get();
   }

   /**
    * A method that returns the Ensemble numberOfCells field contents 
    * as an int.
    * 
    * @return numberOfCells The number of cells
    */
   public int getNumberOfCells() {
     return (int) ensembleFixedLeader.getNumberOfCells().get();
   }


   /**
    * A method that returns the Ensemble numberOfCodeRepetitions field contents 
    * as an int.
    * 
    * @return numberOfCodeRepetitions The number of code repetitions
    */
   public int getNumberOfCodeRepetitions() {
     return (int) ensembleFixedLeader.getNumberOfCodeRepetitions().get();
   }

   /**
    * A method that returns the Ensemble pdRealOrSimulatedFlag field contents 
    * as an int.
    * 
    * @return pdRealOrSimulatedFlag The PD real or simulated flag
    */
   public int getPdRealOrSimulatedFlag() {
     return (int) ensembleFixedLeader.getPdRealOrSimulatedFlag().get();
   }

   /**
    * A method that returns the Ensemble percentGoodMinimum field contents 
    * as an int.
    * 
    * @return percentGoodMinimum The percent good minimum
    */
   public int getPercentGoodMinimum() {
     return (int) ensembleFixedLeader.getPercentGoodMinimum().get();
   }

   /**
    * A method that returns the Ensemble pingHundredths field contents 
    * as an int.
    * 
    * @return pingHundredths The ping hundredths
    */
   public int getPingHundredths() {
     return (int) ensembleFixedLeader.getPingHundredths().get();
   }

   /**
    * A method that returns the Ensemble pingMinutes field contents 
    * as an int.
    * 
    * @return pingMinutes The ping minutes
    */
   public int getPingMinutes() {
     return (int) ensembleFixedLeader.getPingMinutes().get();
   }

   /**
    * A method that returns the Ensemble pingSeconds field contents 
    * as an int.
    * 
    * @return pingSeconds The ping seconds
    */
   public int getPingSeconds() {
     return (int) ensembleFixedLeader.getPingSeconds().get();
   }

   /**
    * A method that returns the Ensemble pingsPerEnsemble field contents 
    * as an int.
    * 
    * @return pingsPerEnsemble The pings per ensemble
    */
   public int getPingsPerEnsemble() {
     return (int) ensembleFixedLeader.getPingsPerEnsemble().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble profilingMode field contents 
    * as an int.
    * 
    * @return profilingMode The profiling mode
    */
   public int getProfilingMode() {
     return (int) ensembleFixedLeader.getProfilingMode().get();
   }

   /**
    * A method that returns the Ensemble referenceLayerEnd field contents 
    * as an int.
    * 
    * @return referenceLayerEnd The reference layer end
    */
   public int getReferenceLayerEnd() {
     return (int) ensembleFixedLeader.getReferenceLayerEnd().get();
   }

   /**
    * A method that returns the Ensemble referenceLayerStart field contents 
    * as an int.
    * 
    * @return referenceLayerStart The reference layer start
    */
    public int getReferenceLayerStart() {
      return (int) ensembleFixedLeader.getReferenceLayerStart().get();
    }

   /**
    * A method that returns the Ensemble sensorAvailability field contents 
    * as an int.
    * 
    * @return sensorAvailability The sensor availability
    */
   public int getSensorAvailability() {
     return (int) ensembleFixedLeader.getSensorAvailability().get();
   }

   /**
    * A method that returns the Ensemble sensorSource field contents 
    * as an int.
    * 
    * @return sensorSource The sensor source
    */
   public int getSensorSource() {
     return (int) ensembleFixedLeader.getSensorSource().get();
   }

   /**
    * A method that returns the source of environmental sensor data.  In this
    * case, the return values are as follows:
    * 0 - speed of sound is not calculated from depth, salinity, and temperature
    * 1 - speed of sound is calculated from depth, salinity, and temperature
    * 
    * @return speedOfSoundSetting The speed of sound setting
    */
   public int getSensorSpeedOfSoundSetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * 
    * @return sensorDepthSetting The sensor depth setting
    */
   public int getSensorDepthSetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * 
    * @return sensorHeading The sensor heading
    */
   public int getSensorHeadingSetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * 
    * @return sensorPitch The sensor pitch
    */
   public int getSensorPitchSetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * 
    * @return sensorRoll The sensor roll
    */
   public int getSensorRollSetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * 
    * @return sensorSalinity The sensor salinity setting
    */
   public int getSensorSalinitySetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * 
    * @return sensorTemperature The sensor temperature setting
    */
   public int getSensorTemperatureSetting() {
     int sensorSetting = (int) ensembleFixedLeader.getSensorSource().get();
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
    * as an int. Remus only, spare for other Workhorse ADCPs
    * 
    * @return serialNumber The sensor serial number
    */
   public int getSerialNumber() {
     return ensembleFixedLeader.getSerialNumber().order(
          ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble signalProcessingMode field contents 
    * as an int.
    * 
    * @return signalProcessingMode The signal processing mode
    */
   public int getSignalProcessingMode() {
     return (int) ensembleFixedLeader.getSignalProcessingMode().get();
   }

   /**
    * A method that returns the Ensemble systemBandwidth field contents 
    * as an int.
    * 
    * @return systemBandwidth The system bandwidth
    */
   public int getSystemBandwidth() {
     return (int) ensembleFixedLeader.getSystemBandwidth().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble systemConfiguration field contents 
    * as an int.
    * 
    * @return systemConfiguration The system configuration
    */
   public int getSystemConfiguration() {
     return (int) ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the beam pattern component of the Ensemble 
    * systemConfiguration field contents as an int.  A return value of 0
    * indicates a concave beam pattern, while a return value of 1 indicates
    * a convex beam pattern.
    * 
    * @return beamPattern The beam pattern
    */
   public int getBeamPattern() {
     int systemConfig = (int) ensembleFixedLeader.getSystemConfiguration().order(
           ByteOrder.LITTLE_ENDIAN).getShort();
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
    * 
    * @return beamDirection The beam direction
    */
   public int getBeamDirection() {
     int systemConfig = (int) ensembleFixedLeader.getSystemConfiguration().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
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
    * 
    * @return transducerAttachement The transducer attachement
    */
   public int getTransducerAttachment() {
     int systemConfig = (int) ensembleFixedLeader.getSystemConfiguration().order(
           ByteOrder.LITTLE_ENDIAN).getShort();
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
    * 
    * @return systemFequency The system frequency
    */
   public int getSystemFrequency() {
     int systemConfig = (int) ensembleFixedLeader.getSystemConfiguration().order(
           ByteOrder.LITTLE_ENDIAN).getShort();
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
    * 
    * @return sensorConfiguration The sensor configuration
    */
   public int getSensorConfiguration() {
     int systemConfig = (int) ensembleFixedLeader.getSystemConfiguration().order(
           ByteOrder.LITTLE_ENDIAN).getShort();
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
    * 
    * @return systemPower The system power
    */
   public int getSystemPower() {
     return (int) ensembleFixedLeader.getSystemPower().get();
   }

   /**
    * A method that returns the Ensemble transmitLagDistance field contents 
    * as an int.
    * 
    * @return transmitLagDistance The transmit lag distance
    */
   public int getTransmitLagDistance() {
     return (int) ensembleFixedLeader.getTransmitLagDistance().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble transmitPulseLength field contents 
    * as an int.
    * 
    * @return transmitPulseLength The transmit pulse length
    */
   public int getTransmitPulseLength() {
     return (int) ensembleFixedLeader.getTransmitPulseLength().order(
          ByteOrder.LITTLE_ENDIAN).getShort();
   }

   // CSJ review the getShort() call below !!!!!!!
   
   /**
    * A method that returns the Ensemble Variable Leader ID field contents 
    * as a short.
    * 
    * @return variableLeaderID The variable leader ID
    */
   public int getVariableLeaderID(){
     // do we need to reverse the byte order of the incoming short?? CSJ
     return (int) ensembleVariableLeader.getVariableLeaderID().get();
     
   }

   /**
    * A method that returns the Ensemble Number field contents 
    * as an int.
    * 
    * @return ensembleNumber The ensemble number
    */
   public int getEnsembleNumber(){
     return (int) ensembleVariableLeader.getEnsembleNumber().order(
            ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Year field contents 
    * as an int.
    * 
    * @return realTimeClockYear The realtime clock year
    */
   public int getRealTimeClockYear(){
     return (int) ensembleVariableLeader.getRealTimeClockYear().get();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Month field contents 
    * as an int.
    * 
    * @return realTimeClockMonth The realtime clock month
    */
   public int getRealTimeClockMonth(){
     return (int) ensembleVariableLeader.getRealTimeClockMonth().get();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Day field contents 
    * as an int.
    * 
    * @return realTimeClockDay The realtime clock day
    */
   public int getRealTimeClockDay(){
     return (int) ensembleVariableLeader.getRealTimeClockDay().get();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Hour field contents 
    * as an int.
    * 
    * @return realTimeClockHour The realtime clock hour
    */
   public int getRealTimeClockHour(){
     return (int) ensembleVariableLeader.getRealTimeClockHour().get();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Minute field contents 
    * as an int.
    * 
    * @return realTimeClockminute The realtime clock minute
    */
   public int getRealTimeClockMinute(){
     return (int) ensembleVariableLeader.getRealTimeClockMinute().get();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Second field contents 
    * as an int.
    * 
    * @return realTimeClockSecond The realtime clock second
    */
   public int getRealTimeClockSecond(){
     return (int) ensembleVariableLeader.getRealTimeClockSecond().get();
   }

   /**
    * A method that returns the Ensemble Real Time Clock Hundredths field contents 
    * as an int.
    * 
    * @return realTimeClockHundredths The realtime clock hundredths
    */
   public int getRealTimeClockHundredths(){
     return (int) ensembleVariableLeader.getRealTimeClockHundredths().get();
   }

   /**
    * A method that returns the Ensemble Number Increment field contents 
    * as an int.
    * 
    * @return numberIncrement The number increment
    */
   public int getEnsembleNumberIncrement(){
     return (int) ensembleVariableLeader.getEnsembleNumberIncrement().get();
   }

   /**
    * A method that returns the Ensemble Built In Test Result field contents 
    * as a short.
    * 
    * @return testResult The built in test result
    */
   public short getBuiltInTestResult(){
     return ensembleVariableLeader.getBuiltInTestResult().order(
                     ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble Speed of Sound field contents 
    * as an int expressed in meters/s.
    * 
    * @return speedOfSound The speed of sound
    */
   public int getSpeedOfSound(){
     return (int) ensembleVariableLeader.getSpeedOfSound().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble Depth of Transducer field contents 
    * as a float expressed in meters.
    * 
    * @return depthOfTransducer The depth of the transducer
    */
   public float getDepthOfTransducer(){
     short value = ensembleVariableLeader.getDepthOfTransducer().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
     int exp = (value/10)*10;
     int valueAsInt = (int) value;
     int man = valueAsInt - exp;
     float mantissa = (float) man;
     float exponent = (float) exp;
     float returnValue = (exponent/10) + (mantissa/10);
     return returnValue;
   }

   /**
    * A method that returns the Ensemble Heading field contents 
    * as an int expressed in degrees (0.00 - 360.00).
    * 
    * @return heading the ensemble heading
    */
   public float getHeading(){
     short value = ensembleVariableLeader.getHeading().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
     int exp = (value/100)*100;
     int valueAsInt = (int) value;
     int man = valueAsInt - exp;
     float mantissa = (float) man;
     float exponent = (float) exp;
     float returnValue = (exponent/100) + (mantissa/100);
     return returnValue;
   }

   /**
    * A method that returns the Ensemble Pitch field contents 
    * as an int expressed in degrees (+/- 20.00).
    * 
    * @return pitch The ensemble pitch
    */
   public float getPitch(){
     short value = ensembleVariableLeader.getPitch().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
     int exp = (value/100)*100;
     int valueAsInt = (int) value;
     int man = valueAsInt - exp;
     float mantissa = (float) man;
     float exponent = (float) exp;
     float returnValue = (exponent/100) + (mantissa/100);
     return returnValue;
   }

   /**
    * A method that returns the Ensemble Heading field contents 
    * as an int expressed in degrees (+/- 20.00).
    * 
    * @return roll The ensemble roll
    */
   public float getRoll(){
     short value = ensembleVariableLeader.getRoll().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
     int exp = (value/100)*100;
     int valueAsInt = (int) value;
     int man = valueAsInt - exp;
     float mantissa = (float) man;
     float exponent = (float) exp;
     float returnValue = (exponent/100) + (mantissa/100);
     return returnValue;
   }

   /**
    * A method that returns the Ensemble Salinity field contents 
    * as a float expressed in parts per thousand.
    * 
    * @return salinity The salinity
    */
   public int getSalinity(){
     short value = ensembleVariableLeader.getSalinity().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
     return (int) value;
   }

   /**
    * A method that returns the Ensemble Temperature field contents 
    * as a float expressed in degrees Celcius.
    * 
    * @return temperature The temperature
    */
   public float getTemperature(){
     short value = ensembleVariableLeader.getTemperature().order(
                   ByteOrder.LITTLE_ENDIAN).getShort();
     int exp = (value/100)*100;
     int valueAsInt = (int) value;
     int man = valueAsInt - exp;
     float mantissa = (float) man;
     float exponent = (float) exp;
     float returnValue = (exponent/100) + (mantissa/100);
     return returnValue;
   }

   /**
    * A method that returns the Ensemble Minimum Pre-ping Wait Minutes field 
    * contents as an int.
    * 
    * @return minimumPrePingWaitMinutes The minimum pre ping wait minutes
    */
   public int getMinPrePingWaitMinutes(){
     return (int) ensembleVariableLeader.getMinPrePingWaitMinutes().get();
   }

   /**
    * A method that returns the Ensemble Minimum Pre-ping Wait Seconds field 
    * contents as an int.
    * 
    * @return minimumPrePingWaitSeconds The minimum pre ping wait seconds
    */
   public int getMinPrePingWaitSeconds(){
     return (int) ensembleVariableLeader.getMinPrePingWaitSeconds().get();
   }

   /**
    * A method that returns the Ensemble Minimum Pre-ping Wait Hundredths field 
    * contents as an int.
    * 
    * @return minimumPrePingWaitHundredths The minimum pre ping wait hundredths
    */
   public int getMinPrePingWaitHundredths(){
     return (int) ensembleVariableLeader.getMinPrePingWaitHundredths().get();
   }

   /**
    * A method that returns the Ensemble Heading Standard Deviation field 
    * contents as an int.
    * 
    * @return headingStandardDeviation The heading standard deviation
    */
   public float getHeadingStandardDeviation(){
     return (float) ensembleVariableLeader.getHeadingStandardDeviation().get();
   }

   /**
    * A method that returns the Ensemble Pitch Standard Deviation field 
    * contents as an int.
    * 
    * @return pitchStandardDeviation The pitch standard deviation
    */
   public float getPitchStandardDeviation(){
     return (float) ensembleVariableLeader.getPitchStandardDeviation().get();
   }

   /**
    * A method that returns the Ensemble Roll Standard Deviation field 
    * contents as an int.
    * 
    * @return rollStandardDeviation The roll standard deviation
    */
   public float getRollStandardDeviation(){
     return (float) ensembleVariableLeader.getRollStandardDeviation().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Zero field 
    * contents as a byte.
    * 
    * @return adcChannelZero The ADC channel zero 
    */
   public byte getADCChannelZero(){
     return ensembleVariableLeader.getADCChannelZero().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel One field 
    * contents as a byte.
    * 
    * @return adcChannelOne The ADC channel one 
    */
   public byte getADCChannelOne(){
     return ensembleVariableLeader.getADCChannelOne().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Two field 
    * contents as a byte.
    * 
    * @return adcChannelTwo The ADC channel two 
    */
   public byte getADCChannelTwo(){
     return ensembleVariableLeader.getADCChannelTwo().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Three field 
    * contents as a byte.
    * 
    * @return adcChannelThree The ADC channel three 
    */
   public byte getADCChannelThree(){
     return ensembleVariableLeader.getADCChannelThree().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Four field 
    * contents as a byte.
    * 
    * @return adcChannelFour The ADC channel four 
    */
   public byte getADCChannelFour(){
     return ensembleVariableLeader.getADCChannelFour().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Five field 
    * contents as a byte.
    * 
    * @return adcChannelFive The ADC channel five 
    */
   public byte getADCChannelFive(){
     return ensembleVariableLeader.getADCChannelFive().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Six field 
    * contents as a byte.
    * 
    * @return adcChannelSix The ADC channel six 
    */
   public byte getADCChannelSix(){
     return ensembleVariableLeader.getADCChannelSix().get();
   }

   /**
    * A method that returns the Ensemble ADC Channel Seven field 
    * contents as a byte.
    * 
    * @return adcChannelSeven The ADC channel seven 
    */
   public byte getADCChannelSeven(){
     return ensembleVariableLeader.getADCChannelSeven().get();
   }

   /**
    * A method that returns the Ensemble Error Status Word field 
    * contents as an int.
    * 
    * @return errorStatusWord The error status word
    */
   public int getErrorStatusWord(){
     return ensembleVariableLeader.getErrorStatusWord().order(
                                     ByteOrder.LITTLE_ENDIAN).getInt();
   }

   /**
    * A method that returns the Ensemble Spare Field One field 
    * contents as an int.
    * 
    * @return spareFieldOne The spare field one 
    */
   public int getSpareFieldOne(){
     return (int) ensembleVariableLeader.getSpareFieldOne().order(
                                     ByteOrder.LITTLE_ENDIAN).getShort();
   }

   /**
    * A method that returns the Ensemble Pressure field 
    * contents as a float expressed in deca-pascals.
    * 
    * @return pressure The pressure
    */
   public float getPressure(){
     int value = ensembleVariableLeader.getPressure().order(
                   ByteOrder.LITTLE_ENDIAN).getInt();
     int exp = (value/10000)*10000;
     int valueAsLong = value;
     int man = valueAsLong - exp;
     float mantissa = (float) man;
     float exponent = (float) exp;
     float returnValue = (exponent/10000) + (mantissa/10000);
     return returnValue;
   }

   /**
    * A method that returns the Ensemble Pressure Variance field 
    * contents as a float expressed in deca-pascals.
    * 
    * @return pressureVariance The pressure variance
    */
   public float getPressureVariance(){
     return ensembleVariableLeader.getPressureVariance().order(
                                     ByteOrder.LITTLE_ENDIAN).getFloat();
   }

   /**
    * A method that returns the Ensemble Spare Field Two field 
    * contents as an int.
    * 
    * @return spareFieldTwo The spare field two 
    */
   public int getSpareFieldTwo(){
     return (int) ensembleVariableLeader.getSpareFieldTwo().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Century 
    * field contents as an int. This field represents the century of observation,
    * and can be used with the Real Time Y2K-compliant Clock Year field to get 
    * the full year as YYYY.
    * 
    * @return realTimeY2KClockCentury The realtime Y2K clock century
    */
   public int getRealTimeY2KClockCentury(){
     return (int) ensembleVariableLeader.getRealTimeY2KClockCentury().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Year 
    * field contents as an int. This field represents the year of observation.
    * 
    * @return realTimeY2KClockYear The realtime Y2K clock year
    */
     public int getRealTimeY2KClockYear(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockYear().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Month 
    * field contents as an int. This field represents the month of observation.
    */
     public int getRealTimeY2KClockMonth(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockMonth().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Day 
    * field contents as an int. This field represents the day of observation.
    */
     public int getRealTimeY2KClockDay(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockDay().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Hour 
    * field contents as an int. This field represents the hour of observation.
    */
     public int getRealTimeY2KClockHour(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockHour().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Minute 
    * field contents as an int. This field represents the minute of observation.
    */
     public int getRealTimeY2KClockMinute(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockMinute().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Second 
    * field contents as an int. This field represents the second of observation.
    */
     public int getRealTimeY2KClockSecond(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockSecond().get();
   }

   /**
    * A method that returns the Ensemble Real Time Y2K-compliant Clock Hundredths 
    * field contents as an int. This field represents the fractional seconds of observation.
    */
     public int getRealTimeY2KClockHundredths(){
       return (int) ensembleVariableLeader.getRealTimeY2KClockHundredths().get();
   }
   
   /**
    * A method that returns the all of velocity measurements for a given beam as
    * a double array.  The array is returned in depth-bin order, with the 
    * the first array member being the first bin closest to the transducer head.
    */
   //public float[] getVelocitiesByBeam(int beamNumber){
   //  
   //// create a map to store velocity arrays by beam     
   //float[] beamArray = new float[getNumberOfBeams()];
   //float[] velocitiesArray = new float[getNumberOfDepthCells()];
   //float[][] velocitiesByBeamMap;
   // 
   //// prep the velocity profile buffer for reading
   //ByteBuffer velocitiesBuffer = ensembleVelocityProfile.getVelocityProfile();
   //velocitiesBuffer.limit(velocitiesBuffer.capacity());
   //velocitiesBuffer.position(0);
   //
   //// build the map
   //for (int i=0; i < getNumberOfDepthCells(); i) ) {
   //  for (int j=0; j < getNumberOfBeams(); j++) {
   //    short value = velocitiesBuffer.order(ByteOrder.LITTLE_ENDIAN).getShort();         
   //    int exp = (int) (value/100)*100;
   //    int valueAsInt = (int) value;
   //    int man = valueAsInt - exp;
   //    float mantissa = (float) man;
   //    float exponent = (float) exp;
   //    float floatValue = (exponent/100) + (mantissa/100);
   //             
   //  }
   //  //velocititesByBeamMap.put();
   //}
   //
   ////return velocitiesByBeamMap.get(new Integer(beamNumber));
   //}

   /**
    * A method that returns the all velocity measurements for a given depth cell as
    * a double array.  The array is returned in beam order, with the 
    * the first array member being the velocity of beam 1.
    */
   //public float[] getVelocitiesByDepthCell(int depthCell){
   //}
   
   /**
    * A method that determines whether or not the Ensemble is valid by
    * comparing the cumulative byte sum value with the stated checksum
    * value in the ensemble byte stream.  Returns true if it is valid.
    */
   public boolean isValid() {
     boolean isValid = false;
     if (  ensembleByteSum % 65535 == getChecksum() ) {
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
