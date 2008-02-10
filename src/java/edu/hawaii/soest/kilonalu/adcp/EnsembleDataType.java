/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: An enumerated type that represents the data types produced by
 *            an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 *            default PD0 format as described in RDI's "Workhorse Commands and 
 *            Output Data Format" manual, P/N 957-6156-00 (March 2005)
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
package edu.hawaii.soest.kilonalu.adcp;

/**
 * An enumerated type that represents the list of data types produced by 
 * an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 * default PD0 format 
 */
public enum EnsembleDataType {
    
    HEADER               , //represents data type ID 0x7F7F
    FIXED_LEADER         , //represents data type ID 0x0000
    VARIABLE_LEADER      , //represents data type ID 0x8000
    VELOCITY_PROFILE     , //represents data type ID 0x0001
    CORRELATION_PROFILE  , //represents data type ID 0x0002
    ECHOINTENSITY_PROFILE, //represents data type ID 0x0003
    PERCENTGOOD_PROFILE  , //represents data type ID 0x0004
    STATUS_PROFILE       , //represents data type ID 0x0005
    BOTTOMTRACK_DATA     , //represents data type ID 0x0006
    MICROCAT_DATA          //represents data type ID 0x0008
}
