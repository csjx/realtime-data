/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that provides properties and methods 
 *             for a simple instrument driver streaming data from a
 *             text-based file.
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
public class FileTextSource extends SimpleTextSource {

	/* The full path to the data file with the incoming data being appended */
	private String dataFilePath;
	
	/**
	 * constructor: create an instance of the SerialTextSource 
	 */
	public FileTextSource() {
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
