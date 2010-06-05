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
  
}                                               
