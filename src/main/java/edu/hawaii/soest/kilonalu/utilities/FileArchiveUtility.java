/*
 * 
 * Copyright (c) 2005-2007 University at Buffalo
 * Copyright (c) 2005-2007 NEES Cyberinfrastructure Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * $URL$
 * $Revision$
 * $Date$
 * $Author$
 */
package edu.hawaii.soest.kilonalu.utilities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A utility class that provides methods used in archiving data from an RBNB
 * DataTurbine to a directory of files.  This class is based on Jason Hanley's
 * ArchiveUtility class found in the NEES software library, and it retains the 
 * the NEES copyright.
 *
 * @author Jason Hanley
 * @author Christopher Jones
 */
public class FileArchiveUtility {

  /**
   * The Logger instance used to log system messages 
   */
  private static Log log = LogFactory.getLog(FileArchiveUtility.class);
  
  /** The date format for the command **/
  private static final SimpleDateFormat COMMAND = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  /** The date format for the file being created **/
  private static final SimpleDateFormat FILE = new SimpleDateFormat("yyyyMMddHHmmss");
  /** The date format for the year of the data **/
  private static final SimpleDateFormat YEAR = new SimpleDateFormat("yyyy");
  /** The date format for the month of the data **/
  private static final SimpleDateFormat MONTH = new SimpleDateFormat("MM");
  /** The date format for the day of the data **/
  private static final SimpleDateFormat DAY = new SimpleDateFormat("dd");
  /** The date format for the hour of the data **/
  private static final SimpleDateFormat HOUR = new SimpleDateFormat("HH");
  /** The date format for the minute of the data **/
  private static final SimpleDateFormat MIN = new SimpleDateFormat("mm");
  /** The time zone for the data **/
  private static final TimeZone TZ = TimeZone.getTimeZone("GMT");

  /** The file name extension to be used for writing data files to disk **/
  public static SimpleDateFormat fileDepthFormat = MIN; 
  

  static
  {
    COMMAND.setTimeZone(TZ);
    FILE.setTimeZone(TZ);
    YEAR.setTimeZone(TZ);
    MONTH.setTimeZone(TZ);
    DAY.setTimeZone(TZ);
    HOUR.setTimeZone(TZ);
    MIN.setTimeZone(TZ);
  }

//  ------------------------  Utility Methods --------------------------------

  /**
   * Create a File object based on the time the file is being archived, along
   * with a file prefix string and an extension.  The depth of the directory 
   * structure that is created depends on the SimpleDateFormat passed in as an 
   * argument.  If format equals the yyyy, MM, dd, HH, or mm formats defined as
   * static constants in this class, the directory will be created to the given 
   * depth.  The depth defaults to mm (minutes).
   */
  public static File makePathFromTime(File base, long time, String filePrefix,
                                      SimpleDateFormat format, String extension) {
    log.trace("FileArchiveUtility.makePathFromTime() called.");
    Date t = new Date(time);
    String path;
    
    if ( format.toPattern().equals(YEAR.toPattern()) ) {
      path =
      YEAR.format(t) + File.separator +
      filePrefix + 
      FILE.format(new Date(time)) + 
      extension;
    }  else if ( format.toPattern().equals(MONTH.toPattern()) ) {
      path =
      YEAR.format(t) + File.separator + 
      MONTH.format(t) + File.separator + 
      filePrefix + 
      FILE.format(new Date(time)) + 
      extension;
    }  else if ( format.toPattern().equals(DAY.toPattern()) ) {
      path =
      YEAR.format(t) + File.separator + 
      MONTH.format(t) + File.separator + 
      DAY.format(t) + File.separator + 
      filePrefix + 
      FILE.format(new Date(time)) + 
      extension;
    }  else if ( format.toPattern().equals(HOUR.toPattern()) ) {
      path =
      YEAR.format(t) + File.separator + 
      MONTH.format(t) + File.separator + 
      DAY.format(t) + File.separator + 
      HOUR.format(t) + File.separator + 
      filePrefix + 
      FILE.format(new Date(time)) + 
      extension;
    }  else if ( format.toPattern().equals(MIN.toPattern()) ) {
      path =
      YEAR.format(t) + File.separator + 
      MONTH.format(t) + File.separator + 
      DAY.format(t) + File.separator + 
      HOUR.format(t) + File.separator + 
      MIN.format(t) + File.separator + 
      filePrefix + 
      FILE.format(new Date(time)) + 
      extension;
    } else {
      path =
      YEAR.format(t) + File.separator + 
      MONTH.format(t) + File.separator + 
      DAY.format(t) + File.separator + 
      HOUR.format(t) + File.separator + 
      MIN.format(t) + File.separator + 
      filePrefix + 
      FILE.format(new Date(time)) + 
      extension;
    }
    File ret = new File(base, path);
    return ret;
  }

  /**
   * A method that returns the date string for the calling command
   *
   * @returns SimpleDateFormat COMMAND
   */
  public static SimpleDateFormat getCommandFormat()
  {
    return COMMAND;
  }
  
  /**
   * A method that confirms whether or not the correct directory path 
   * was created for archiving data files.
   */ 
  public static boolean confirmCreateDirPath(File testPath)
  {
    if (testPath.exists()) return true;
    return testPath.mkdirs();
  }

}