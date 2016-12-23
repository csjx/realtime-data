/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that provides properties and methods 
 *             for a simple instrument driver streaming data from a
 *             text-based file.
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
package edu.hawaii.soest.pacioos.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rbnb.sapi.SAPIException;

/**
 * @author cjones
 *
 */
public class FileTextSource extends SimpleTextSource {

    private static final Log log = LogFactory.getLog(FileTextSource.class);

    /* The full path to the data file with the incoming data being appended */
    private String dataFilePath;

    /* The buffered reader data representing the data file stream  */
    private BufferedReader fileReader;
    

    /**
     * constructor: create an instance of the SerialTextSource 
     * @param xmlConfig 
     * @throws ConfigurationException 
     */
    public FileTextSource(XMLConfiguration xmlConfig) throws ConfigurationException {
        super(xmlConfig);
        
    }
    
    /* (non-Javadoc)
     * @see edu.hawaii.soest.pacioos.text.SimpleTextSource#execute()
     */
        protected boolean execute() {
            log.debug("FileTextSource.execute() called.");
            boolean failed = false; // indicates overall success of execute()
            
            // do not execute the stream if there is no connection
            if ( !isConnected() ) return false;
            
            try {
                
                // open the data file for monitoring
                FileReader reader = new FileReader(new File(getDataFilePath()));
                this.fileReader = new BufferedReader(reader);
                
                // add channels of data that will be pushed to the server.    
                // Each sample will be sent to the Data Turbine as an rbnb frame.    
                int channelIndex = 0;
                
                long lastSampleTimeAsSecondsSinceEpoch = getLastSampleTime();
                
                // poll the data file for new lines of data and insert them into the RBNB
                while (true) {
                    String line = fileReader.readLine();
                    
                    if (line == null ) {
                        Thread.currentThread().sleep(this.pollInterval);
                        
                    } else {
                        
                        // test the line for the expected data pattern
                        boolean valid = this.validateSample(line);
                        
                        // if the line matches the data pattern, insert it into the DataTurbine
                        if ( valid ) {
                            log.debug("This line matches the data line pattern: " + line);
                            
                            Date sampleDate = getSampleDate(line);
                                                    
                            if ( this.dateFormats == null || this.dateFields == null ) {
                                log.warn("Using the default datetime format and field for sample data. " +
                                            "Use the -f and -d options to explicitly set date time fields.");
                            }
                            log.debug("Sample date is: " + sampleDate.toString());
                            
                            // convert the sample date to seconds since the epoch
                            long sampleTimeAsSecondsSinceEpoch = (sampleDate.getTime()/1000L);
                            
                            // only insert samples newer than the last sample seen at startup 
                            // and that are not in the future (> 1 hour since the CTD clock
                            // may have drifted)
                            Calendar currentCal = Calendar.getInstance();
                            this.tz = TimeZone.getTimeZone(this.timezone);
                            currentCal.setTimeZone(this.tz);
                            currentCal.add(Calendar.HOUR, 1);
                            Date currentDate = currentCal.getTime();
                            
                            if ( lastSampleTimeAsSecondsSinceEpoch < sampleTimeAsSecondsSinceEpoch &&
                                 sampleTimeAsSecondsSinceEpoch < currentDate.getTime()/1000L ) {
                                
                                int numberOfChannelsFlushed = 0;
                                try {
                                    //insert into the DataTurbine
                                    numberOfChannelsFlushed = this.sendSample(line);
                                
                                } catch ( SAPIException sapie ) {
                                    // reconnect if an exception is thrown on Flush()
	                                log.error("Error while flushing the source: " + sapie.getMessage());
	                                failed = true;
                
                                }
                                
                                // reset the last sample time to the sample just inserted
                                lastSampleTimeAsSecondsSinceEpoch = sampleTimeAsSecondsSinceEpoch;
                                log.debug("Last sample time is now: " + 
                                		(new Date(lastSampleTimeAsSecondsSinceEpoch * 1000L).toString())
                                                        );
                                log.info(getRBNBClientName() + " Sample sent to the DataTurbine: " + line.trim());
                                
                            } else {
                                log.info("The current line is earlier than the last entry " +
	                                "in the Data Turbine or is a date in the future. " +
	                                "Skipping it. The line was: " + line);
                                
                            }
                            
                        } else {
                            log.info("The current line doesn't match an expected " +
                                "data line pattern. Skipping it.    The line was: " + line);
                                                    
                        }
                    }    // end if()
                } // end while
            
	        } catch ( ParseException pe ) {
	            log.info("There was a problem parsing the sample date string. The message was: " + pe.getMessage());
	            failed = true;
	            return !failed;
	            
	        } catch ( SAPIException sapie ) {
	            log.info("There was a problem communicating with the DataTurbine. The message was: " + sapie.getMessage());
	            failed = true;
	            return !failed;
	            
	        } catch ( InterruptedException ie ) {
	            log.info("There was a problem while polling the data file. The message was: " + ie.getMessage());
	            failed = true;
	            return !failed;
	            
	        } catch ( IOException ioe ) {
	            log.info("There was a problem opening the data file. The message was: " + ioe.getMessage());
	            failed = true;
	            return !failed;
	            
	        } finally {
	            try {
	                this.fileReader.close();
	                
	            } catch (IOException ioe2) {
	                log.error("There was a problem closing the data file. The message was: " + ioe2.getMessage());
	                
	            }

	        }
        
    }
  
    /* (non-Javadoc)
     * @see org.nees.rbnb.RBNBBase#setArgs(org.apache.commons.cli.CommandLine)
     */
    @Override
    protected boolean setArgs(CommandLine cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Return the path to the data file as a string
     * 
     * @return dataFilePath - the path to the data file
     */
    public String getDataFilePath() {
        return dataFilePath;
    }

    /**
     * Set the full path to the data file that is receiving incoming data
     * 
     * @param dataFilePath  the path to the data file
     */
    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        
    }

}
