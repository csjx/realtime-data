/*
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A factory class that creates different types of
 *             SimpleTextSource instrument drivers.
 *
 *   Authors: Christopher Jones
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

import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.configuration.ConfigurationException;
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

    /**
     * Configure and return a simple text-based source driver instance using the given XML-based
     * configuration file.
     * 
     * @param configLocation  the path to the file on disk or in a jar
     * @return  a new instance of a subclassed SimpleTextSource
     * @throws ConfigurationException  a configuration exception
     */
    public static SimpleTextSource getSimpleTextSource(String configLocation) 
        throws ConfigurationException {

        // Get a configuration based on the XML configuration
        Configuration config;
        config = new Configuration(configLocation);

        String connectionType = config.getConnectionType();
        
        /* The SimpleTextSource used to communicate with the instrument */
        SimpleTextSource simpleTextSource;

        // return the correct configuration type
        switch (connectionType) {
            case "file":
                FileTextSource fileTextSource = getFileTextSource(config);
                String filePath = config.getDataFilePath();
                fileTextSource.setDataFilePath(filePath);
                simpleTextSource = fileTextSource;
                log.debug("Created a file text source.");
                break;
            case "socket":
                SocketTextSource socketTextSource = getSocketTextSource(config);
                String hostName = config.getHostNameConnectionParam();
                socketTextSource.setHostName(hostName);
                int hostPort = config.getHostPortConnectionParam();
                socketTextSource.setHostPort(hostPort);
                simpleTextSource = socketTextSource;
                log.debug("Created a socket text source.");
                break;
            case "serial":
                SerialTextSource serialTextSource = getSerialTextSource(config);
                String serialPort = config.getSerialPortConnectionParam();
                serialTextSource.setSerialPort(serialPort);
                int baudRate = config.getSerialBaudRateConnectionParam();
                serialTextSource.setBaudRate(baudRate);
                int dataBits = config.getSerialDataBitsConnectionParam();
                serialTextSource.setDataBits(dataBits);
                int stopBits = config.getSerialStopBitsConnectionParam();
                serialTextSource.setStopBits(stopBits);
                String parity = config.getSerialParityConnectionParam();
                serialTextSource.setParity(parity);
                simpleTextSource = serialTextSource;
                log.debug("Created a serial text source.");
                break;
            default:
                throw new ConfigurationException("There was an issue parsing the configuration." +
                    " The connection type of " + connectionType + " wasn't recognized.");
        }
        return simpleTextSource;
    }

    /**
     * Provides an instance of a text-based source driver using data from a serial connection
     * @param config  the configuration instance
     * 
     * @return a new instance of SimpleTextSource
     * @throws ConfigurationException  a configuration exception
     */
    private static SerialTextSource getSerialTextSource(Configuration config)
        throws ConfigurationException {
        return new SerialTextSource(config);
    }

    /**
     * Provides an instance of a text-based source driver using data from a socket connection
     * @param config  the configuration instance
     * 
     * @return a new instance of SimpleTextSource
     * @throws ConfigurationException  a configuration exception
     */
    private static SocketTextSource getSocketTextSource(Configuration config)
        throws ConfigurationException {
        return new SocketTextSource(config);
    }

    /**
     * Provides an instance of a text-based source driver using data from a file
     * @param config  the configuration instance
     * 
     * @return a new instance of SimpleTextSource
     * @throws ConfigurationException  a configuration exception
     */
    private static FileTextSource getFileTextSource(Configuration config)
        throws ConfigurationException {
        return new FileTextSource(config);
    }
}
