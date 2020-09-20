/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that is the main entry point for communicating
 *             with a DataTurbine and archiving channel data to disk.
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
package edu.hawaii.soest.pacioos.text.convert;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface used to parse data samples and convert them into
 * a data frame table
 */
public interface Converter {

    /**
     * Parse the given samples into an internal table
     * @param samples the samples to parse
     */
    void parse(InputStream samples);

    /**
     * Convert the parsed samples to a new table
     */
    void convert();

    /**
     * Write the samples to the given output stream
     * @param outputStream the location to write to
     * @return count the number of samples written
     */
    int write(OutputStream outputStream);


}
