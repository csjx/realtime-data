/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: An enumerated type that represents the low level 
 *             data types allowed by the RBNB DataTurbine as
 *             input types.  
 *    Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy: $
 * $LastChangedRevision: $
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
package edu.hawaii.soest.kilonalu.utilities;

/**
 * An enumerated type that represents the list of low level data types
 * allowed by the RBNB DataTurbine.  The types include 'int8', 'int16',
 * 'int32', 'int64', 'float32', 'float64', 'string', and 'bytearray'.
 */
public enum EnsembleDataType {
    
    int8      , //represents an 8 bit integer data type
    int16     , //represents an 16 bit integer data type
    int32     , //represents an 32 bit integer data type
    int64     , //represents an 64 bit integer data type
    float32   , //represents an 32 bit float data type
    float64   , //represents an 64 bit float data type
    string    , //represents a string data type
    bytearray   //represents a byte array data type
}
