/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that is the main entry point for communicating
 *             with remote instruments streaming simple text-based
 *             samples.
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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple class used to start a SimpleTextSource driver.  Configure the driver by changing
 * the XML-based configuration file with the proper settings.
 * 
 * @author cjones
 *
 */
public class TextSourceApp {

    private static final Log log = LogFactory.getLog(TextSourceApp.class);

	public static SimpleTextSource textSource = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String xmlConfiguration = null;
		if (args.length != 1) {
			log.error("Please provide the path to the instrument's XML configuration file " +
		              "as a single parameter.");
			System.exit(1);
		} else {
			xmlConfiguration = args[0];
		}
		try {
		    textSource = TextSourceFactory.getSimpleTextSource(xmlConfiguration);
		    
		    if ( textSource != null ) {
		    	textSource.start();	
		    	
		    }
		    
		    // Handle ctrl-c's and other abrupt death signals to the process
		    Runtime.getRuntime().addShutdownHook(new Thread() {
		      // stop the streaming process
		      public void run() {
		    	  log.info("Stopping the SimpleTextSource driver due to user request");
		          textSource.stop();		          
		      }
		    });

		} catch (ConfigurationException e) {
			if (log.isDebugEnabled() ) {
				e.printStackTrace();				
			}
			
			log.error("There was a problem configuring the driver.  The error message was: " +
			          e.getMessage());
			System.exit(1);

		}
	}

}
