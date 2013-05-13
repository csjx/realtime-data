/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that is the main entry point for communicating
 *             with remote instruments streaming simple text-based
 *             samples.
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
		
		try {
		    textSource = TextSourceFactory.getSimpleTextSource(null);
		    
		    if ( textSource != null ) {
		    	//textSource.start();
		    	
		    }
		} catch (ConfigurationException e) {
			if (log.isDebugEnabled() ) {
				e.printStackTrace();				
			}
			System.exit(1);

		}
	}

}
