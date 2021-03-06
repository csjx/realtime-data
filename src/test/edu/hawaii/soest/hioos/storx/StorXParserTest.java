/*
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that tests functionality of the StorXParser class
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

package edu.hawaii.soest.hioos.storx;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.fail;

/**
 * Unit tests for the StorXParser class
 */

public class StorXParserTest {

    private byte[] dataAsByteArray;
    private InputStream storXData;
    ByteBuffer buffer;
    StorXParser parser;

    @Before
    public void setUp() {

        // read the sample data from a binary StorX file
        try {

            // create a byte buffer from the binary file data
            storXData = this.getClass().getResourceAsStream(
                "/edu/hawaii/soest/kilonalu/ctd/2010171.raw");
            dataAsByteArray = IOUtils.toByteArray(storXData);
            buffer = ByteBuffer.wrap(dataAsByteArray);

            // create a parser instance and test that it succeeds
            this.parser = new StorXParser();

        } catch (IOException | NullPointerException ioe) {
            fail("There was a problem reading the" +
                " data file.  The error was: " + ioe.getMessage());

        }

    }

    //@Test
    //public void testParser() {
    //
    //  // test that our instance object was created
    //  assertThat(parser, IsInstanceOf.instanceOf(StorXParser.class));
    //
    //}

    @Test
    public void testParse() {

        // test the parsing of the data buffer
        try {

            this.parser.parse(buffer);
            //assertTrue(1 == 1);

        } catch (Exception e) {

            fail("Parsing failed. The error was: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
