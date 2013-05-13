/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that provides properties and methods common 
 *             to all simple text-based source drivers within this
 *             package.
 *
 *   Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy:  $
 * $LastChangedRevision:  $
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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nees.rbnb.RBNBSource;

/**
 * a class that represents a simple text-based instrument as a Source driver
 * for an RBNB DataTurbine streaming server.
 * 
 * @author cjones
 *
 */
public abstract class SimpleTextSource extends RBNBSource {

    private final Log log = LogFactory.getLog(SimpleTextSource.class);
    
    /*
     * The mode in which the source interacts with the RBNB archive. Valid modes 
     * include 'append', 'create', 'load' and 'none'.
     *
     * @see setArchiveMode()
     * @see getArchiveMode()
     */
    private String archiveMode;

    /* The name of the RBNB channel for this data stream */
    private String rbnbChannelName;
    
    /* The IP address or DNS name of the RBNB server */
    private String serverName;
    
    /* The default TCP port of the RBNB server */
    private int serverPort;
    
    /* The address and port string for the RBNB server */
    private String server = serverName + ":" + serverPort;

    /* The driver connection type (file, socket, or serial) */
	private String connectionType;

    /* The delimiter separating variables in the sample line */
    private String delimiter;

    /* The default date format for the timestamp in the data sample string */
    private SimpleDateFormat defaultDateFormat;
    
    /* A list of date format patterns to be applied to designated date/time fields */
    private List<String> dateFormats = null;

    /* A one-based list of Integers corresponding to the observation date/time field indices */
    private List<Integer> dateFields = null;
    
    /* The instance of TimeZone to use when parsing dates */
    private TimeZone tz;

    /* The pattern used to identify data lines */
    private Pattern dataPattern;
    
    /* The timezone that the data samples are taken in as a string (UTC, HST, etc.) */
    private String timezone;
    
    /* The state used to track the data processing */
    private int state = 0;
      
    /* A boolean indicating if we are ready to stream (connected) */
    private boolean readyToStream = false;
    
    /* The thread used for streaming data */
    private Thread streamingThread;
    
    /* The polling interval (millis) used to check for new lines in the data file */
    private int pollInterval;
    
    /*
     * An internal Thread setting used to specify how long, in milliseconds, the
     * execution of the data streaming Thread should wait before re-executing
     * 
     * @see execute()
     */
    private int retryInterval = 5000;

    /* The identifier of the instrument (e.g. NS01) */
	private String identifier;

    /**
     * Constructor: create an instance of the simple SimpleTextSource
     */
    public SimpleTextSource() {
    	
    	// Pull the general configuration from the properties file
    	try {
			Configuration config   = new PropertiesConfiguration("pacioos.properties");
			this.archiveMode       = config.getString("pacioos.textsource.archive_mode");
			this.rbnbChannelName   = config.getString("pacioos.textsource.rbnb_channel");
			this.serverName        = config.getString("pacioos.textsource.server_name ");
			this.delimiter         = config.getString("pacioos.textsource.delimiter");
			this.pollInterval      = config.getInt("pacioos.textsource.poll_interval");
			this.retryInterval     = config.getInt("pacioos.textsource.retry_interval");
			this.defaultDateFormat = new SimpleDateFormat(
			    config.getString("pacioos.textsource.default_date_format"));

		} catch (ConfigurationException e) {
			log.error("Couldn't configure the text source driver. The error was: " + e.getMessage());
			
			if ( log.isDebugEnabled() ) {
				e.printStackTrace();
			}
			System.exit(1);
			
		}
    }
    
    /**
     * Set the name of the RBNB DataTurbine channel for this text source
     * 
     * @param channelName  the name of the channel
     */
    public void setChannelName(String channelName) {
    	
    }
    
    /**
     * Return the RBNB DataTurbine channel name for this text source
     * @return
     */
    public String getChannelName() {
        return this.rbnbChannelName;
    
    }
    
    /**
     * Set the connection for this text source
     * 
     * @param connectionType  the connection type ('file', 'socket', or 'serial')
     */
    public void setConnectionType(String connectionType) {
    	this.connectionType = connectionType;
    }
    
    /**
     * Return the connection type for this text source
     * @return
     */
    public String getConnectionType() {
        return this.connectionType;
    
    }
    
    /**
     * A method that returns the versioning info for this file.  In this case, 
     * it returns a String that includes the Subversion LastChangedDate, 
     * LastChangedBy, LastChangedRevision, and HeadURL fields.
     */
    public String getCVSVersionString(){
      return (
      "$LastChangedDate: 2013-01-10 20:39:51 -0700 (Thu, 10 Jan 2013) $" +
      "$LastChangedBy: cjones $" +
      "$LastChangedRevision: 935 $" +
      "$HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/utilities/FileSource.java $"
      );
    }

    /**
     * A method that returns true if the RBNB connection is established
     * and if the data streaming Thread has been started
     */
    public boolean isRunning() {
      // return the connection status and the thread status
      return ( isConnected() && readyToStream );
    }
    
    /*
     * A method that runs the data streaming work performed by the execute()
     * by handling execution problems and continuously trying to re-execute after 
     * a specified retry interval for the thread.
     */
    private void runWork() {
      
      // handle execution problems by retrying if execute() fails
      boolean retry = true;
      
      while ( retry ) {
        
        // connect to the RBNB server
        if ( connect() ) {
          // run the data streaming code
          retry = !execute();
        }
        
        disconnect();
        
        if ( retry ) {
          try {
            Thread.sleep(retryInterval);
            
          } catch ( Exception e ){
            log.info("There was an execution problem. Retrying. Message is: " +
            e.getMessage());
            
          }
        }
      }
      // stop the streaming when we are done
      stop();
    }

    /**
     * A method that starts the streaming of data lines from the source file to
     * the RBNB server.  
     */
    public boolean start() {
      
      // return false if the streaming is running
      if ( isRunning() ) {
        return false;
      }
      
      // reset the connection to the RBNB server
      if ( isConnected() ) {
        disconnect();
      }
      connect();
      
      // return false if the connection fails
      if ( !isConnected() ) {
        return false;
      }
      
      // begin the streaming thread to the source
      startThread();
      
      return true;  
    }

    /**
     * A method that creates and starts a new Thread with a run() method that 
     * begins processing the data streaming from the source file.
     */
    private void startThread() {
      
      // build the runnable class and implement the run() method
      Runnable runner = new Runnable() {
        public void run() {
          runWork();
        }
      };
      
      // build the Thread and start it, indicating that it has been started
      readyToStream = true;
      streamingThread = new Thread(runner, "StreamingThread");
      streamingThread.start();     
      
    }

    /**
     * A method that stops the streaming of data between the source file and
     * the RBNB server.  
     */ 
    public boolean stop() {
      
      // return false if the thread is not running
      if ( !isRunning() ) {
        return false;
      }
      
      // stop the thread and disconnect from the RBNB server
      stopThread();
      disconnect();
      return true;
    }

    /**
     * A method that interrupts the thread created in startThread()
     */
    private void stopThread() {
      // set the streaming status to false and stop the Thread
      readyToStream = false;
      streamingThread.interrupt();
    }

    /**
     * A method that executes the reading of data from the instrument to the RBNB
     * server after all configuration of settings, connections to hosts, and
     * thread initializing occurs.  This method contains the detailed code for 
     * reading and interpreting the streaming data. this method must be implemented by
     * a connection-specific source class.
     */
    protected abstract boolean execute();

    /**
     * A method that starts the connection with the RBNB DataTurbine. Used as a public wrapper
     * method to RBNBSource.connect(), which has protected scope.
     */
    public boolean startConnection() {
      return connect();
    }
     
    /**
     * A method that stops the connection with the RBNB DataTurbine. Used as a public wrapper
     * method to RBNBSource.disconnect(), which has protected scope.
     */
    public void stopConnection() {
      disconnect();
    }

    /**
     * Set the identifier of the instrument, usually a string like "NS01".
     * 
     * @param identifier  the identifier of the instrument
     */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
     
    /**
     * Set the identifier of the instrument, usually a string like "NS01".
     * 
     * @param identifier  the identifier of the instrument
     */
	public String getIdentifier() {
		return this.identifier;
	}

}
