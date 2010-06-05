/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a Satlantic STOR-X data logger sample
 *             from a binary data file.
 *
 *   Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy: $
 * $LastChangedRevision: $
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
package edu.hawaii.soest.kilonalu.ctd;

import edu.hawaii.soest.kilonalu.ctd.StorXParser;

import java.io.File; 
import java.io.FileInputStream; 

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 *  A class that represents a single binary data file from a Satlantic 
 *  STOR-X data logger.  At the moment, the parser treats the binary format
 *  as simple lines starting with "SAT" and ending with "\r\n", ignoring
 * other structured content (since we don't have a format specification).
 */
public class StorXParser {
    
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(StorXParser.class);

  /* A field that stores the binary UPD packet data input as a ByteBuffer */
  private ByteBuffer fileBuffer = ByteBuffer.allocate(256);
  
  /* An array list used to store lines of a file as ByteBuffers*/
  private ArrayList<String> lineStringArrayList = new ArrayList<String>();
  
  /* The ASCII data sample strings */
  private ArrayList<String> sampleStrings = new ArrayList<String>();
  
  /* The dates corresponding to each sample */
  private ArrayList<Date> sampleDates = new ArrayList<Date>();
  
  /* The processing state during data parsing */
  private int state = 0;
  
   /* The date format for the timestamp applied to the sample */
   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /* The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
  
  /**
   *  Constructor: Creates a StorXParser instance that parses a single binary
   *  data file.
   *
   *  @param fileBuffer  the binary data file as a ByteBuffer
   */
  public StorXParser(ByteBuffer fileBuffer) {
    
    this.fileBuffer = fileBuffer;
    
    // parse the buffer
    parse(this.fileBuffer);
    
  }

  /**
   * A method that gets the log configuration file location
   *
   * @return logConfigurationFile  the log configuration file location
   */
  public String getLogConfigurationFile() {
    return this.logConfigurationFile;
  }
  
  /**
   * A method that sets the log configuration file name
   *
   * @param logConfigurationFile  the log configuration file name
   */
  public void setLogConfigurationFile(String logConfigurationFile) {
    this.logConfigurationFile = logConfigurationFile;
  }
  
  /**
   * A method that returns the data sample strings as a String array
   *
   * @return sampleStrings -  the data sample strings array
   */
  public String[] getSampleStrings() {
    return this.sampleStrings.toArray(new String[this.sampleStrings.size()]);
      
  }
  
  /**
   * A method that returns the data sample dates as a Date array
   *
   * @return sampleDates -  the data sample dates array
   */
  public Date[] getSampleDates() {
    return this.sampleDates.toArray(new Date[this.sampleDates.size()]);
      
  }
  
  /**
   * A method used to parse the binary STOR-X file.  Currently, this only
   * parses out ASCII sample data strings.
   *
   * @param fileBuffer - the binary data file as a ByteBuffer
   */ 
  public void parse(ByteBuffer fileBuffer) {
    
    logger.debug("StorXParser.parse() called.");
    
    try {
      
      // Create a buffer that will store a single line of the file
      ByteBuffer lineBuffer = ByteBuffer.allocate(1024);
      
      // create four byte placeholders used to evaluate up to a four-byte 
      // window.  The FIFO layout looks like:
      //           -------------------------
      //   in ---> | One | Two |Three|Four |  ---> out
      //           -------------------------
      byte byteOne   = 0x00,   // set initial placeholder values
           byteTwo   = 0x00,
           byteThree = 0x00,
           byteFour  = 0x00;
      
      int lineByteCount = 0; // keep track of bytes per line
      
      fileBuffer.position(0);
      fileBuffer.limit(fileBuffer.capacity());
      
      while ( fileBuffer.hasRemaining() ) {
          
        // load the next byte into the FIFO window
        byteOne = fileBuffer.get();
        
        // show the byte stream coming in
        //logger.debug("b1: " + new String(Hex.encodeHex(new byte[]{byteOne}))   + "\t" +
        //             "b2: " + new String(Hex.encodeHex(new byte[]{byteTwo}))   + "\t" +
        //             "b3: " + new String(Hex.encodeHex(new byte[]{byteThree})) + "\t" +
        //             "b4: " + new String(Hex.encodeHex(new byte[]{byteFour}))
        //             );
        
        // evaluate the bytes, separate the file line by line (SAT ...\r\n)
        switch (this.state) {
          
          case 0: // find a line beginning (SAT) 53 41 54
            
            if ( byteOne == 0x54 && byteTwo == 0x41 && byteThree == 0x53 ) {
              
              // found a line, add the beginning to the line buffer 
              lineBuffer.put(byteThree);
              lineBuffer.put(byteTwo);
              lineBuffer.put(byteOne);
              
              lineByteCount = lineByteCount + 3;
              
              this.state = 1;
              break;
            
            } else {
              break;
              
            }
          
          case 1: // find a line ending (\r\n)
            
            if ( byteOne == 0x0A && byteTwo == 0x0D ) {
              
              // we have a line ending. store the line in the arrayList
              lineBuffer.put(byteOne);
              lineByteCount++;
              lineBuffer.flip();
              // create a true copy of the byte array subset
              byte[] lineArray = lineBuffer.array();
              byte[] lineCopy  = new byte[lineByteCount];
              System.arraycopy(lineArray, 0, lineCopy, 0, lineByteCount);
              String currentLine = new String(lineCopy, "US-ASCII");
              this.lineStringArrayList.add(currentLine);
              
              // rest the line buffer for the next line
              lineBuffer.clear();
              lineByteCount = 0;
              this.state = 0; 
              break;
              
            } else {
              
              // no full line yet, keep adding bytes
              lineBuffer.put(byteOne);
              lineByteCount++;
              break;
              
            }
          
        } // end switch()
        
        // shift the bytes in the FIFO window
        byteFour  = byteThree;
        byteThree = byteTwo;
        byteTwo   = byteOne;
        
      } // end while()
      
      if ( this.lineStringArrayList.size() > 0 ) {
        // now the file has been parsed into lines.  Evaluate each line, find
        // the data lines, and add them to the sample strings array      
        for (Iterator iterator = this.lineStringArrayList.iterator(); iterator.hasNext();) {
          
          // prepare the line for reading
          String lineString = (String) iterator.next();
          
          if ( lineString.matches("SATSBE[0-9]{4}.*\t.*[0-9][0-9]\r\n") ) {

            logger.debug("LINE: " + lineString);

            // we've found a line of data. Store it in the sampleStrings list
            logger.debug("This is a data line in the file. Processing it.");
            
            // find the beginning of the data string and subset the string
            int index = lineString.indexOf(" ");
            String sampleString = lineString.substring(index, lineString.length());
            this.sampleStrings.add(sampleString);
            
          } else {
            // go to the next line
            // logger.debug("This is not a data line in the file. Skipping it.");

          }

        } // end for()

        // now the sample strings have been extracted. Parse each string to
        // extract the date column value. Note: this has hard-coded information
        // about the data string. Needs to be abstracted, but we have little to
        // no automated metadata to allow automated string parsing.
        for (Iterator sIterator = this.sampleStrings.iterator(); sIterator.hasNext();) {

          String sample = (String) sIterator.next();
          String[] columns      = sample.trim().split(",");
          String   dateString   = columns[columns.length - 1]; // last field

          DATE_FORMAT.setTimeZone(TZ);
          Date sampleDate = DATE_FORMAT.parse(dateString);
          this.sampleDates.add(sampleDate);

        } // end for()
        
      } else {
        logger.debug("There are no lines in the line buffer to parse.");
        
      } // end if()
      
    } catch (Exception e) {
      logger.debug("Failed to parse the data file.  The error message was:" +
                   e.getMessage());
      e.printStackTrace();
      
    }    
        
  }
                                                
}                                               
