/*
 *  Copyright: 2022 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A test class that exercises the SocketTextSource class.
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

import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class RawToPacIOOS2020ConverterTest {
    private static final Log log = LogFactory.getLog(RawToPacIOOS2020ConverterTest.class);
    protected List<String> testMockInstruments;
    protected String testResourcesDirectory;

    /**
     *  Set up a mock data source
     */
    @Before
    public void setUp() {

        // get the resources directory
        InputStream propsStream = ClassLoader.getSystemResourceAsStream("test.properties");
        Properties properties = new Properties();
        try {
            properties.load(propsStream);
            testResourcesDirectory = properties.getProperty("test.resources.directory");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clean up, clearing the mock instruments array
     */
    @After
    public void tearDown() {
        // clear the list of test instruments
        testMockInstruments.clear();
    }

    /**
     * Ensure the RawToPacIOOS2020Converter writes timestamps without milliseconds
     * @throws ConfigurationException a configuration exception
     * @throws IOException an I/O exception
     */
    @Test
    public void testConverterWithCorrectTimestamp() throws ConfigurationException, IOException {

        // Get a list of instruments to test
        testMockInstruments = new ArrayList<>();
        testMockInstruments.add("AW02XX_001CTDXXXXR00");

        for (String instrument : testMockInstruments) {
            // Get the XML configuration file for this data source
            String configLocation = testResourcesDirectory +
                    "edu/hawaii/soest/pacioos/text/" +
                    instrument +
                    "-instrument-config.xml";

            String dataLocation = testResourcesDirectory +
                    "edu/hawaii/soest/pacioos/text/" +
                    instrument +
                    "-mock-data.txt";

            File dataFile = new File(dataLocation);
            String data = FileUtils.readFileToString(dataFile, StandardCharsets.UTF_8);

            // Load the configuration
            Configuration config = new Configuration(configLocation);

            RawToPacIOOS2020Converter converter = new RawToPacIOOS2020Converter();
            int channelIndex = 0; // raw DecimalASCIISampleData
            int archiverIndex = 1; // pacioos-2020-format
            // Get the channel time zone
            ZoneId timeZoneId = ZoneId.of(config.getTimeZoneID(channelIndex));
            // Get the archive type
            String archiveType = config.getArchiveType(channelIndex, archiverIndex);
            // Get the channel data prefix if any
            String dataPrefix = config.getDataPrefix(channelIndex);

            converter.setFieldDelimiter(config.getFieldDelimiter(channelIndex));
            converter.setRecordDelimiter("\n");
            converter.setMissingValueCode(config.getMissingValueCode(channelIndex));
            converter.setNumberHeaderLines(0);
            converter.setTimeZoneId(timeZoneId);
            converter.setDataPrefix(dataPrefix);

            // Also set the dateFields list
            converter.setDateFields(config.getDateFields(channelIndex));
            converter.setDateFormats(config.getDateFormats(channelIndex));

            // Set the date and time format strings in the converter depending on the config count
            if ( config.getTotalDateFields(channelIndex) > 1 ) {
                converter.setDateFormat(config.getDateFormat(channelIndex));
                converter.setTimeFormat(config.getTimeFormat(channelIndex));
            } else {
                converter.setDateTimeFormat(config.getDateTimeFormat(channelIndex));
            }

            // Set the column types
            converter.setColumnTypes(config.getColumnTypes(channelIndex));

            InputStream samples =
                new BufferedInputStream(
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))
                );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            converter.parse(samples);
            converter.convert();
            int count = converter.write(out);
            log.info("\n" + out);
            assertEquals(20, count);

            int linesMatchingMillis = StringUtils.countMatches(out.toString(), ".000Z");
            assertEquals(0, linesMatchingMillis);
        }
    }
}
