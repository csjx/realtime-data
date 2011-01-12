/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: An enumerated type that represents the calibration types
 *            found in the Satlantic Instrument File Standard, (SAT-DN-00134),
 *            Version 6.1 (E) February 4, 2010.
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
package edu.hawaii.soest.hioos.satlantic;

/**
 * An enumerated type that represents the list of calibration types found in
 * the Satlantic Instrument File Standard, (SAT-DN-00134),
 * Version 6.1 (E) February 4, 2010.
 */
public enum CalibrationType {
    
    OPTIC1    ,
    OPTIC2    ,
    OPTIC3    ,
    THERM1    ,
    POW10     ,
    POLYU     ,
    POLYF     ,
    GPSTIME   ,
    GPSPOS    ,
    GPSHEMI   ,
    GPSMODE   ,
    GPSSTATUS ,
    DDMM      ,
    HHMMSS    ,
    DDMMYY    ,
    TIME2     ,
    COUNT     ,
    NONE      ,
    DELIMITER             
}
