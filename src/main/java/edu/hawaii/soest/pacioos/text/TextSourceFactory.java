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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
		XMLConfiguration xmlConfig = new XMLConfiguration();
		xmlConfig.setDelimiterParsingDisabled(true);
		xmlConfig.setAttributeSplittingDisabled(true);
		xmlConfig.load(configLocation);
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
			FileTextSource fileTextSource = getFileTextSource();
			String filePath = xmlConfig.getString("connectionParams.filePath");
			fileTextSource.setDataFilePath(filePath);

			simpleTextSource = (SimpleTextSource) fileTextSource;

		} else if ( connectionType.equals("socket") ) {
			SocketTextSource socketTextSource = getSocketTextSource();
			String hostName = xmlConfig.getString("connectionParams.hostName");
			socketTextSource.setHostName(hostName);
			int hostPort = xmlConfig.getInt("connectionParams.hostPort");
			socketTextSource.setHostPort(hostPort);

			simpleTextSource = (SimpleTextSource) socketTextSource;

		} else if ( connectionType.equals("serial") ) {
			SerialTextSource serialTextSource = getSerialTextSource();
			String serialPort = xmlConfig.getString("connectionParams.serialPort");
			serialTextSource.setSerialPort(serialPort);
			int baudRate = xmlConfig.getInt("connectionParams.serialPortParams.baudRate");
			serialTextSource.setBaudRate(baudRate);
			int dataBits = xmlConfig.getInt("connectionParams.serialPortParams.dataBits");
			serialTextSource.setDataBits(dataBits);
			int stopBits = xmlConfig.getInt("connectionParams.serialPortParams.stopBits");
			serialTextSource.setStopBits(stopBits);
			String parity = xmlConfig.getString("connectionParams.serialPortParams.parity");
			serialTextSource.setParity(parity);

			simpleTextSource = (SimpleTextSource) serialTextSource;
			
		} else {
			throw new ConfigurationException("There was an issue parsing the configuration." +
		         " The connection type of " + connectionType + " wasn't recognized.");
		}
		
		// set the XML configuration in the simple text source for later use
		simpleTextSource.setConfiguration(xmlConfig);
		
		// set the common configuration fields
		simpleTextSource.setConnectionType(connectionType);
		String channelName = xmlConfig.getString("channelName");
		simpleTextSource.setChannelName(channelName);
		String identifier = xmlConfig.getString("identifier");
		simpleTextSource.setIdentifier(identifier);
		String rbnbName = xmlConfig.getString("rbnbName");
		simpleTextSource.setRBNBClientName(rbnbName);
		String rbnbServer = xmlConfig.getString("rbnbServer");
		simpleTextSource.setServerName(rbnbServer);
		int rbnbPort = xmlConfig.getInt("rbnbPort");
		simpleTextSource.setServerPort(rbnbPort);
		int archiveMemory = xmlConfig.getInt("archiveMemory");
		simpleTextSource.setCacheSize(archiveMemory);
		int archiveSize = xmlConfig.getInt("archiveSize");
		simpleTextSource.setArchiveSize(archiveSize);
		
		// set the default channel information 
		Object channels = xmlConfig.getList("channels.channel.name");
		int totalChannels = 1;
		if ( channels instanceof Collection) {
			totalChannels = ((Collection<?>) channels).size();
			
		}		
		// find the default channel with the ASCII data string
		for (int i = 0; i < totalChannels; i++) {
			boolean isDefaultChannel = xmlConfig.getBoolean("channels.channel(" + i + ")[@default]");
			if ( isDefaultChannel ) {
				String name = xmlConfig.getString("channels.channel(" + i + ").name");
				simpleTextSource.setChannelName(name);
				String dataPattern = xmlConfig.getString("channels.channel(" + i + ").dataPattern");
				simpleTextSource.setPattern(dataPattern);
				String fieldDelimiter = xmlConfig.getString("channels.channel(" + i + ").fieldDelimiter");
				simpleTextSource.setDelimiter(fieldDelimiter);
				String recordDelimiter = xmlConfig.getString("channels.channel(" + i + ").recordDelimiter");
				simpleTextSource.setRecordDelimiter(recordDelimiter);
				break;
			}
			
		}

		return simpleTextSource;
		
	}

    /**
     * Provides an instance of a text-based source driver using data from a serial connection
     * 
     * @return a new instance of SimpleTextSource
     */
	private static SerialTextSource getSerialTextSource() {
		return new SerialTextSource();
	}

    /**
     * Provides an instance of a text-based source driver using data from a socket connection
     * 
     * @return a new instance of SimpleTextSource
     */
	private static SocketTextSource getSocketTextSource() {
		return new SocketTextSource();
	}


    /**
     * Provides an instance of a text-based source driver using data from a file
     * 
     * @return a new instance of SimpleTextSource
     */
	private static FileTextSource getFileTextSource() {
		return new FileTextSource();
	}
}
