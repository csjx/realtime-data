/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a Satlantic STOR-X data logger sample
 *             from a binary data file.
 *
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
package edu.hawaii.soest.hioos.storx;

import edu.hawaii.soest.hioos.storx.StorXParser;
import edu.hawaii.soest.hioos.isus.ISUSFrame;
import edu.hawaii.soest.kilonalu.ctd.CTDFrame;


import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dhmp.util.HierarchicalMap;
import org.dhmp.util.BasicHierarchicalMap;

/**
 *  A class that represents a single binary data file from a Satlantic 
 *  STOR-X data logger.  The parser treats the binary format as a series of
 *  frames as defined by Satlantic's binary file format.  It parses each frame
 *  out of the binary data based on the frame type and creates a frame map that
 *  enables access to the individual fields of data.  It cuurently supports
 *  the parsing of native StorX logger frames and Seabird CTD ASCII frames.
 */
public class StorXParser {
    
  /* The Logger instance used to log system messages */
  static Log logger = LogFactory.getLog(StorXParser.class);

  /* A field that stores the binary UPD packet data input as a ByteBuffer */
  private ByteBuffer fileBuffer = ByteBuffer.allocate(8192);
  
  /* 
   * The hierarchical map of data frames and their parsed poperties. 
   * The map structure is:
   * frames/frame/rawFrame          - (ByteBuffer of the entire binary frame)
   * frames/frame/id                - (String: 'SATSTX')
   * frames/frame/type              - (String: 'SBE')
   * frames/frame/serialNumber      - (String: '0207')
   * frames/frame/date              - (Date object)
   * frames/frame/parsedFrameObject - (StorXFrame, ISUSFrame, CTDFrame objects)
  */
  private BasicHierarchicalMap framesMap = new BasicHierarchicalMap();
  
  /* A Stor-X header size in bytes as an integer (null-filled at the end) */
  private final int STOR_X_HEADER_SIZE = 128;
  
  /* A Stor-X frame size in bytes as an integer */
  private final int STOR_X_FRAME_SIZE = 35;
  
  /* A Stor-X header ID as a String */
  private final String STOR_X_HEADER_ID = "SATHDR";
  
  /* A Stor-X frame ID as a String */
  private final String STOR_X_FRAME_ID = "SATSTX";
  
  /* A Seabird SBE CTD sensor frame ID as a String */
  private final String SBE_CTD_FRAME_ID = "SATSBE";
  
  /* An ISUS nitrate sensor dark binary frame ID as a String */
  private final String ISUS_DARK_FRAME_ID = "SATNDB";
  
  /* An ISUS nitrate sensor light binary frame ID as a String */
  private final String ISUS_LIGHT_FRAME_ID = "SATNLB";
  
  /* An ISUS frame size in bytes as an integer */
  private final int ISUS_FRAME_SIZE = 610;
  
  /* The processing state during data parsing */
  private int state = 0;
  
  /* The date format for the timestamp applied to a SBE CTD sample */
  private static final SimpleDateFormat DATE_FORMAT = 
    new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /* The date format for the timestamp applied to a StorX frame (Julian day)*/
  private static final SimpleDateFormat FRAME_DATE_FORMAT = 
    new SimpleDateFormat("yyyyDDDHHmmssSSS");
 
  /* The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
  
  /**
   *  Constructor: Creates an empty StorXParser instance 
   */
  public StorXParser() {
    
  }
  
  /**
   *  Constructor: Creates a StorXParser instance that parses a single binary
   *  data file.
   *
   *  @param fileBuffer  the binary data file as a ByteBuffer
   */
  public StorXParser(ByteBuffer fileBuffer) {
    
    // parse the buffer
    try {
      
      parse(fileBuffer);
      
    } catch (Exception e) {
      e.printStackTrace();
      
    }
      
  }

  /**
   * A method that returns the frames map as a hierarchical map
   *
   * @return framesMap -  the frames stored in the hierarchical map
   */
  public HierarchicalMap getFramesMap() {
    return this.framesMap;
      
  }
  
  /**
   * Parses the binary STOR-X file.  The binary file format is a sequence of
   * 'frames' that all begin with 'SAT'.  The parser creates a list with the
   * individual frames.  Some frames are StorX frames (SATSTX), some are from 
   * external sensors (ISUS: 'SATNLB', 'SATNDB'; SBE CTD: 'SATSBE')
   *
   * @param fileBuffer - the binary data file as a ByteBuffer
   */ 
  public void parse(ByteBuffer fileBuffer) throws Exception {
    
    logger.debug("StorXParser.parse() called.");
    
    this.fileBuffer = fileBuffer;
    //logger.debug(this.fileBuffer.toString());
    
    try {
      
      // Create a buffer that will store a single frame of the file
      ByteBuffer frameBuffer = ByteBuffer.allocate(1024);
      
      // create four byte placeholders used to evaluate up to a four-byte 
      // window.  The FIFO layout looks like:
      //           ---------------------------
      //   in ---> | Four | Three | Two | One |  ---> out
      //           ---------------------------
      byte byteOne   = 0x00,   // set initial placeholder values
           byteTwo   = 0x00,
           byteThree = 0x00,
           byteFour  = 0x00;
      
      int frameByteCount = 0; // keep track of bytes per frame
      int frameCount = 0; // keep track of frames
      
      this.fileBuffer.position(0);
      this.fileBuffer.limit(this.fileBuffer.capacity());
      
      while ( this.fileBuffer.hasRemaining() ) {
        
        // load the next byte into the FIFO window
        byteOne = fileBuffer.get();
        
        // show the byte stream coming in
        //logger.debug("b1: " + new String(Hex.encodeHex(new byte[]{byteOne}))   + "\t" +
        //             "b2: " + new String(Hex.encodeHex(new byte[]{byteTwo}))   + "\t" +
        //             "b3: " + new String(Hex.encodeHex(new byte[]{byteThree})) + "\t" +
        //             "b4: " + new String(Hex.encodeHex(new byte[]{byteFour}))  + "\t" +
        //             "st: " + Integer.toString(this.state)                     + "\t" +
        //             "po: " + this.fileBuffer.position()                       + "\t" +
        //             "cp: " + this.fileBuffer.capacity()
        //             );
        
        // evaluate the bytes, separate the file frame by frame (SAT ...)
        switch (this.state) {
          
          case 0: // find a frame beginning (SAT) 53 41 54
            
            if ( byteOne == 0x54 && byteTwo == 0x41 && byteThree == 0x53 ) {
              
              // found a line, add the beginning to the line buffer 
              frameBuffer.put(byteThree);
              frameBuffer.put(byteTwo);
              frameBuffer.put(byteOne);
              
              frameByteCount = frameByteCount + 3;
              
              this.state = 1;
              break;
            
            } else {
              break;
              
            }
          
          case 1: // find the next frame beginning (SAT) 53 41 54
            
            if ( ( byteOne == 0x54 && byteTwo == 0x41 && byteThree == 0x53 ) || 
                 fileBuffer.position() == fileBuffer.capacity() ) {
              
              // we have a line ending. store the line in the arrayList
              frameBuffer.put(byteOne);
              frameByteCount++;
              frameBuffer.flip();
              byte[] frameArray = frameBuffer.array();
              ByteBuffer currentFrameBuffer;
              
              if ( fileBuffer.position() == fileBuffer.capacity() ) {
                
                // create a true copy of the byte array subset (no trailing 'SAT')
                byte[] frameCopy  = new byte[frameByteCount];
                System.arraycopy(frameArray, 0, frameCopy, 0, frameByteCount);
                currentFrameBuffer = ByteBuffer.wrap(frameCopy);
                
              } else {
                
                // create a true copy of the byte array subset (less the 'SAT')
                byte[] frameCopy  = new byte[frameByteCount - 3];
                System.arraycopy(frameArray, 0, frameCopy, 0, frameByteCount - 3);
                currentFrameBuffer = ByteBuffer.wrap(frameCopy);
                
              }
              
              // parse the current frame and add it to the frameMap
              
              frameCount++;
              
              // create a map to store frames as they are encountered
              BasicHierarchicalMap frameMap = new BasicHierarchicalMap();
              
              // peek at the first six header bytes as a string
              byte[] sixBytes  = new byte[6];
              currentFrameBuffer.get(sixBytes);
              currentFrameBuffer.position(0);
              String frameHeader = new String(sixBytes, "US-ASCII");
              
              // determine the frame type based on the header
              if ( frameHeader.matches(this.STOR_X_HEADER_ID) ) {
                frameMap.put("rawFrame", currentFrameBuffer);
                frameMap.put("id", frameHeader);
                frameMap.put("type", frameHeader.substring(3,6));
                frameMap.put("serialNumber", null);
                frameMap.put("date", null);
                String headerString = new String(currentFrameBuffer.array());
                // trim trailing null characters and line endings
                int nullIndex = headerString.indexOf(0);
                headerString = headerString.substring(0, nullIndex).trim();
                frameMap.put("parsedFrameObject", headerString);
                
                // Add the frame to the frames map
                this.framesMap.add("/frames/frame", 
                                   (BasicHierarchicalMap) frameMap.clone());
                
                frameMap.removeAll("frame");
                currentFrameBuffer.clear();
                
              } else if ( frameHeader.matches(this.STOR_X_FRAME_ID) ) {
                
                // test if the frame is complete
                if ( currentFrameBuffer.capacity() == this.STOR_X_FRAME_SIZE ) {
                  
                  // convert the frame buffer to a StorXFrame
                  StorXFrame storXFrame = new StorXFrame(currentFrameBuffer);
                  
                  frameMap.put("rawFrame", currentFrameBuffer);
                  frameMap.put("id", frameHeader);
                  frameMap.put("type", frameHeader.substring(3,6));
                  frameMap.put("serialNumber", storXFrame.getSerialNumber());
                  frameMap.put("date", parseTimestamp(storXFrame.getTimestamp()));
                  frameMap.put("parsedFrameObject", storXFrame);
    
                  // Add the frame to the frames map
                  this.framesMap.add("/frames/frame", 
                                     (BasicHierarchicalMap) frameMap.clone());
    
                  frameMap.removeAll("frame");
                  currentFrameBuffer.clear();
                  
                } else {
                  logger.debug(frameHeader + " frame " + frameCount + 
                               " length is " + currentFrameBuffer.capacity() +
                               " not " + this.STOR_X_FRAME_SIZE);
                }
                
              } else if ( frameHeader.matches(this.SBE_CTD_FRAME_ID) ) {
                
                // convert the frame buffer to a CTDFrame
                CTDFrame ctdFrame = new CTDFrame(currentFrameBuffer);
                
                // add in a sample if it matches a general data sample pattern
                if ( ctdFrame.getSample().matches(" [0-9].*[0-9]\r\n")) {
                  
                  // extract the sample bytes from the frame
                  frameMap.put("rawFrame", currentFrameBuffer);
                  frameMap.put("id", frameHeader);
                  frameMap.put("type", frameHeader.substring(3,6));
                  frameMap.put("serialNumber", ctdFrame.getSerialNumber());
                  frameMap.put("date", parseTimestamp(ctdFrame.getTimestamp()));
                  frameMap.put("parsedFrameObject", ctdFrame);

                  // Add the frame to the frames map
                  this.framesMap.add("/frames/frame", 
                                     (BasicHierarchicalMap) frameMap.clone());
                  
                } else {
                  logger.debug("This CTD frame is not a data sample." +
                              " Skipping it. The string is: " +
                              ctdFrame.getSample());
                }
                
                frameMap.removeAll("frame");
                currentFrameBuffer.clear();
                
              } else if ( frameHeader.matches(this.ISUS_DARK_FRAME_ID) ) {
                
                // test if the frame is complete
                if ( currentFrameBuffer.capacity() == this.ISUS_FRAME_SIZE ) {
                  
                  // convert the frame buffer to a ISUSFrame
                  ISUSFrame isusFrame = new ISUSFrame(currentFrameBuffer);
                  
                  frameMap.put("rawFrame", currentFrameBuffer);
                  frameMap.put("id", frameHeader);
                  frameMap.put("type", frameHeader.substring(3,6));
                  frameMap.put("serialNumber", isusFrame.getSerialNumber());
                  frameMap.put("date", parseTimestamp(isusFrame.getTimestamp()));
                  frameMap.put("parsedFrameObject", isusFrame);
    
                  // Add the frame to the frames map
                  this.framesMap.add("/frames/frame", 
                                     (BasicHierarchicalMap) frameMap.clone());
    
                  frameMap.removeAll("frame");
                  currentFrameBuffer.clear();
                  
                } else {
                  logger.debug(frameHeader + " frame " + frameCount + 
                               " length is " + currentFrameBuffer.capacity() +
                               " not " + this.ISUS_FRAME_SIZE);
                }
                
                currentFrameBuffer.clear();
                
              } else if ( frameHeader.matches(this.ISUS_LIGHT_FRAME_ID) ) {
                
                // test if the frame is complete
                if ( currentFrameBuffer.capacity() == this.ISUS_FRAME_SIZE ) {
                  
                  // convert the frame buffer to a ISUSFrame
                  ISUSFrame isusFrame = new ISUSFrame(currentFrameBuffer);
                  
                  frameMap.put("rawFrame", currentFrameBuffer);
                  frameMap.put("id", frameHeader);
                  frameMap.put("type", frameHeader.substring(3,6));
                  frameMap.put("serialNumber", isusFrame.getSerialNumber());
                  frameMap.put("date", parseTimestamp(isusFrame.getTimestamp()));
                  frameMap.put("parsedFrameObject", isusFrame);
    
                  // Add the frame to the frames map
                  this.framesMap.add("/frames/frame", 
                                     (BasicHierarchicalMap) frameMap.clone());
    
                  frameMap.removeAll("frame");
                  currentFrameBuffer.clear();
                  
                } else {
                  logger.debug(frameHeader + " frame " + frameCount + 
                               " length is " + currentFrameBuffer.capacity() +
                               " not " + this.ISUS_FRAME_SIZE);
                }
                
                currentFrameBuffer.clear();
                
              } else {
                logger.info("The current frame type is not recognized. " +
                            "Discarding it.  The header was: " + frameHeader);
                currentFrameBuffer.clear();
                            
              }
              
              // reset the frame buffer for the next frame, but add the 'SAT'
              // bytes already encountered
              frameBuffer.clear();
              frameByteCount = 0;
              this.fileBuffer.position(this.fileBuffer.position() - 3);
              this.state = 0; 
              break;
              
            } else {
              
              // no full line yet, keep adding bytes
              frameBuffer.put(byteOne);
              frameByteCount++;
              break;
              
            }
            
        } // end switch()
        
        // shift the bytes in the FIFO window
        byteFour  = byteThree;
        byteThree = byteTwo;
        byteTwo   = byteOne;
        
      } // end while()
      
      logger.debug(this.framesMap.toXMLString(1000));
      
    } catch (Exception e) {
      logger.debug("Failed to parse the data file.  The error message was:" +
                   e.getMessage());
      e.printStackTrace();
      
    }    
        
  }
  
  /**
   * Parses the binary STOR-X timestamp. The timestamp format is
   * YYYYDDD from the first 3 bytes, and HHMMSS.SSS from the last four:
   * Example:
   * 1E AC CC = 2010316 (year 2010, julian day 316)
   * 09 9D 3E 20 = 16:13:00.000 (4:13 pm)
   * @param timestamp - the timestamp to parse as a byte array
   * @return date - the timestamp as a Date object
   */ 
  public Date parseTimestamp(byte[] timestamp) {
    
    Date convertedDate = new Date(0L); // initialize to the epoch
    
    try {
      ByteBuffer timestampBuffer = ByteBuffer.wrap(timestamp);
      
      // convert the year and day bytes
      int yearAndJulianDay =
      ((timestampBuffer.get() & 0xFF) << 16) |
      ((timestampBuffer.get() & 0xFF) <<  8) |
      ((timestampBuffer.get() & 0xFF));
      
      String yearAndJulianDayString = new Integer(yearAndJulianDay).toString();
      
      // convert the hour, minute, second, millis bytes
      int hourMinuteSecondMillis =
      timestampBuffer.getInt();
      String hourMinuteSecondMillisString = 
        String.format("%09d", hourMinuteSecondMillis);
      
      // concatenate the strings to get the timestamp
      String timestampString = yearAndJulianDayString + hourMinuteSecondMillisString;
      
      // convert to a Date object
      FRAME_DATE_FORMAT.setTimeZone(TZ);
      convertedDate = 
        FRAME_DATE_FORMAT.parse(timestampString, new ParsePosition(0));
      
    } catch (BufferUnderflowException bue) {
      
      logger.debug("There was a problem reading the timestamp. " +
                   "The error message was: " + bue.getMessage());
      
    } catch (NullPointerException npe) {
      
      logger.debug("There was a problem converting the timestamp. " +
                   "The error message was: " + npe.getMessage());
      
    } finally {
      
      return convertedDate;
      
    }
    
  }

}
