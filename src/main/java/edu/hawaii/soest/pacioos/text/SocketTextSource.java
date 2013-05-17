/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that provides properties and methods 
 *             for a simple instrument driver streaming data from a
 *             text-based, TCP socket connection.
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * @author cjones
 *
 */
public class SocketTextSource extends SimpleTextSource {

	/* The FQDN or IP of the source instrument host */ 
	private String sourceHostName;
	
	/* The connection port of the source instrument host */
	private int sourceHostPort;
	
	/**
	 * constructor: create an instance of the SerialTextSource 
	 */
	public SocketTextSource() {
		super();
		
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

	/* (non-Javadoc)
	 * @see org.nees.rbnb.RBNBBase#setOptions()
	 */
	@Override
	protected Options setOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Return the name (FQDN) or IP of the instrument host
	 * 
	 * @return sourceHostName  the name or IP of the instrument host 
	 */
	public String getHostName() {
		return sourceHostName;
		
	}

	/**
	 * Set the name (FQDN) or IP of the instrument host
	 * 
	 * @param sourceHostName  the name or IP of the instrument host
	 */
	public void setHostName(String hostName) {
		this.sourceHostName = hostName;
		
	}

	/**
	 * Return the connection port of the instrument host
	 * 
	 * @return sourceHostPort  the connection port of the instrument host 
	 */
	public int getHostPort() {
		return sourceHostPort;
		
	}

	/**
	 * Set the connection port of the instrument host
	 * 
	 * @param sourceHostPort  the connection port of the instrument host
	 */
	public void setHostPort(int hostPort) {
		this.sourceHostPort = hostPort;
		
	}

}
