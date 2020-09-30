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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filter that filters out sample characters in the underlying InputStream
 * when they match the dataPrefix from an instrument channel configuration
 */
public class SampleFilterInputStream extends FilterInputStream {

    private final Log log = LogFactory.getLog(SampleFilterInputStream.class);

    /* The character that is a prefix to data samples */
    private final Character dataPrefix;

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public SampleFilterInputStream(InputStream in, Character dataPrefix) {
        super(in);
        this.dataPrefix = dataPrefix;
    }

    /**
     * Override InputStream.read() and filter out the dataPrefix character
     * @return the next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException a read exception
     */
    @Override
    public int read() throws IOException {
        int nextByte;
        do {
            nextByte = super.read();
        } while ( nextByte == dataPrefix );
        return nextByte;
    }

    /**
     * Override InputStream.read(byte[] nextBytes, int offset, int len)
     * and filter out the dataPrefix character
     * @param nextBytes the incoming byte array
     * @param offset the read offset
     * @param len the read length
     * @return the next byte
     * @throws IOException a read exception
     */
    @Override
    public int read(byte[] nextBytes, int offset, int len) throws IOException {

        int nextByte = super.read(nextBytes, offset, len);

        if ( nextByte == -1 ) {
            return -1;
        }
        int position = offset -1;
        for (int nextBytePosition = offset; nextBytePosition < offset + nextByte; nextBytePosition++) {
            if ( nextByte == this.dataPrefix) {
                log.debug("Filtered out: " + nextByte);
            } else {
                position++;
            }

            if ( position < nextBytePosition ) {
                nextBytes[position] = nextBytes[nextBytePosition];
            }
        }
        return position - offset + 1;
    }
}
