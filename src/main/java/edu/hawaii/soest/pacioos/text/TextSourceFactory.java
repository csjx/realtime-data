/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A factory class that creates different types of
 *             SimpleTextSource instrument drivers.
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

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides a factory method for returning a simple text-based source driver by providing an XML-based
 * configuration file that chooses one connection type: file, socket, or serial.
 * 
 * @author cjones
 */
public class TextSourceFactory {

    private static final Log log = LogFactory.getLog(TextSourceFactory.class);

	/* The SimpleTextSource used to communicate with the instrument */
	private static SimpleTextSource simpleTextSource;

	/**
	 * Configure and return a simple text-based source driver instance using the given XML-based
	 * configuration file.
	 * 
	 * @param configLocation  the path to the file on disk or in a jar
	 * @return  a new instance of a subclassed SimpleTextSource
	 * @throws ConfigurationException
	 */
	public static SimpleTextSource getSimpleTextSource(String configLocation) 
		throws ConfigurationException {
		
		// Get the per-driver configuration
		char delimiter = "|".charAt(0);
		AbstractConfiguration.setDefaultListDelimiter(delimiter);
		Configuration xmlConfig = new XMLConfiguration(configLocation);
		String connectionType = xmlConfig.getString("connectionType");
		
		if ( log.isDebugEnabled() ) {
			Iterator i = xmlConfig.getKeys();
			while ( i.hasNext()) {
				String key = (String) i.next();
				String value = xmlConfig.getString(key);
				log.debug(key + ":\t\t" + value);
				
			}
			
		}
		
		// return the correct configuration type
		if ( connectionType.equals("file") ) {
			simpleTextSource = getFileTextSource();
			String filePath = xmlConfig.getString ("connectionParams.filePath");

		} else if ( connectionType.equals("socket") ) {
			simpleTextSource = getSocketTextSource();
			String hostName = xmlConfig.getString ("connectionParams.hostName");
			String hostPort = xmlConfig.getString ("connectionParams.hostPort");

		} else if ( connectionType.equals("serial") ) {
			simpleTextSource = getSerialTextSource();
			String serialPort = xmlConfig.getString ("connectionParams.serialPort");
			String baudRate = xmlConfig.getString ("connectionParams.serialPortParams.baudRate");
			String dataBits = xmlConfig.getString ("connectionParams.serialPortParams.dataBits");
			String stopBits = xmlConfig.getString ("connectionParams.serialPortParams.stopBits");
			String parity = xmlConfig.getString ("connectionParams.serialPortParams.parity");

		} else {
			throw new ConfigurationException("There was an issue parsing the configuration." +
		         " The connection type of " + connectionType + " wasn't recognized.");
		}
		
		// set the common configuration fields
		simpleTextSource.setConnectionType(connectionType);
		String channelName = xmlConfig.getString("channelName");
		simpleTextSource.setChannelName(channelName);
		String identifier = xmlConfig.getString ("identifier");
		simpleTextSource.setIdentifier(identifier);
		String rbnbName = xmlConfig.getString ("rbnbName");
		String rbnbServer = xmlConfig.getString ("rbnbServer");
		String rbnbPort = xmlConfig.getString ("rbnbPort");
		String archiveMemory = xmlConfig.getString ("archiveMemory");
		String archiveSize = xmlConfig.getString ("archiveSize");
		String name = xmlConfig.getString ("channels.channel.name");
		String dataType = xmlConfig.getString ("channels.channel.dataType");
		String dataPattern = xmlConfig.getString ("channels.channel.dataPattern");
		String fieldDelimiter = xmlConfig.getString ("channels.channel.fieldDelimiter");
		String recordDelimiter = xmlConfig.getString ("channels.channel.recordDelimiter");
		String dateFormat = xmlConfig.getString ("channels.channel.dateFormats.dateFormat");
		String dateField = xmlConfig.getString ("channels.channel.dateFields.dateField");
		String defaultChannel = xmlConfig.getString ("channels.channel[@default]");

		return simpleTextSource;
		
	}

    /**
     * Provides an instance of a text-based source driver using data from a serial connection
     * 
     * @return a new instance of SimpleTextSource
     */
	private static SimpleTextSource getSerialTextSource() {
		return new SerialTextSource();
	}

    /**
     * Provides an instance of a text-based source driver using data from a socket connection
     * 
     * @return a new instance of SimpleTextSource
     */
	private static SimpleTextSource getSocketTextSource() {
		return new SocketTextSource();
	}


    /**
     * Provides an instance of a text-based source driver using data from a file
     * 
     * @return a new instance of SimpleTextSource
     */
	private static SimpleTextSource getFileTextSource() {
		return new FileTextSource();
	}
}
