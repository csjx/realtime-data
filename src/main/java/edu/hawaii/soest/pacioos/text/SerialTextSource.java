/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.configuration.ConfigurationException;


/**
 * Represents a simple instrument driver streaming data from a
 *  text-based serial connection.
 */
public class SerialTextSource extends SimpleTextSource {

	/* The serial port device for the serial connection to the instrument */
	private String serialPort;
	
	/* The baud rate for the serial connection to the instrument */
	private int baudRate;
	
	/* The number of data bits for the serial connection to the instrument */
	private int dataBits;
	
	/* The number of stop bits for the serial connection to the instrument */
	private int stopBits;
	
	/* The parity setting for the serial connection to the instrument */
	private String parity;
	
	/**
	 * constructor: create an instance of the SerialTextSource 
	 * @param config a configurtion instance
	 * @throws ConfigurationException a configuration exception
	 */
	public SerialTextSource(Configuration config) throws ConfigurationException {
		super(config);
		
	}
	
	/* (non-Javadoc)
	 * @see edu.hawaii.soest.pacioos.text.SimpleTextSource#execute()
	 */
	@Override
	protected boolean execute() {
		// TODO Auto-generated method stub
		return false;
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
	 * Return the serial port device for the serial connection to the instrument (e.g. /dev/tty0)
	 * 
	 * @return the serialPort
	 */	
	public String getSerialPort() {
		return serialPort;
		
	}

	/**
	 * Set the serial port device for the serial connection to the instrument (e.g. /dev/tty0)
	 * 
	 * @return the baudRate
	 */
	public void setSerialPort(String serialPort) {
		this.serialPort = serialPort;
		
	}

	/**
	 * Return the configured baud rate for the serial connection to the instrument
	 * 
	 * @return the baudRate
	 */
	public int getBaudRate() {
		return baudRate;
		
	}

	/**
	 * Set the baud rate for the serial connection to the instrument
	 * 
	 * @param baudRate the baudRate to set
	 */
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
		
	}

	/**
	 * Return the number of data bits set for the serial connection to the instrument (e.g. 8)
	 * 
	 * @return the dataBits
	 */
	public int getDataBits() {
		return dataBits;
		
	}

	/**
	 * Set the number of data bits set for the serial connection to the instrument (e.g. 8)
	 * 
	 * @param dataBits the dataBits to set
	 */
	public void setDataBits(int dataBits) {
		this.dataBits = dataBits;
		
	}

	/**
	 * Return the number of stop bits set for the serial connection to the instrument (e.g. 1)
	 * 
	 * @return the stopBits
	 */
	public int getStopBits() {
		return stopBits;
		
	}

	/**
	 * Set the number of stop bits set for the serial connection to the instrument (e.g. 8)
	 * 
	 * @param stopBits the stopBits to set
	 */
	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
		
	}

	/**
	 * Return the parity for the serial connection to the instrument (e.g. NONE)
	 * 
	 * @return the parity
	 */
	public String getParity() {
		return parity;
		
	}

	/**
	 * Set the parity for the serial connection to the instrument (e.g. NONE)
	 * 
	 * @param parity the parity to set
	 */
	public void setParity(String parity) {
		this.parity = parity;
		
	}

}
