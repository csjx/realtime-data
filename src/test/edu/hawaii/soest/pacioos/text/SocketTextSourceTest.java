/*
 *  Copyright: 2013 Regents of the University of Hawaii and the
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
package edu.hawaii.soest.pacioos.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Test;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * Exercises the SocketTextSource by starting a mock data source server, a mock DataTurbine,
 * and an instance of the SocketTextSource.
 * @author cjones
 *
 */
public class SocketTextSourceTest extends SimpleTextSourceTest {

    private static final Log log = LogFactory.getLog(SocketTextSourceTest.class);

    /**
     * Test multiple instrument file formats and instrument configurations:
     * Data from NS02
     * Data from NS03
     * Test method for {@link edu.hawaii.soest.pacioos.text.SocketTextSource#SocketTextSource(edu.hawaii.soest.pacioos.text.configure.Configuration)}.
     */
    @Test
    public void testSocketTextSource() {
        
        testMockInstruments = new ArrayList<String>();
        testMockInstruments.add("AW02XX_001CTDXXXXR00");
        testMockInstruments.add("WK01XX_001CTDXXXXR00");
        testMockInstruments.add("KN0101_010TCHNXXXR00");
        testMockInstruments.add("MU01XX_001YSIXXXXR00");
        
        // test each mock instrument file, using file ending naming conventions
        for (String instrument : testMockInstruments) {
            // start up the mock data source
            mockCTDData = testResourcesDirectory + "edu/hawaii/soest/pacioos/text/" + instrument + "-mock-data.txt";
            mockDataSource = new MockDataSource(mockCTDData);
            mockDataSourceThread = new Thread(mockDataSource);
            mockDataSourceThread.start();

            String configLocation = testResourcesDirectory + "edu/hawaii/soest/pacioos/text/" +
                instrument + "-instrument-config.xml";

            SimpleTextSource socketTextSource = null;
            try {

                // start the SocketTextSource
                socketTextSource = TextSourceFactory.getSimpleTextSource(configLocation);
                socketTextSource.start();
                
                // wait while data are streamed from the mock data source
                while ( mockDataSourceThread.isAlive() ) {
                    Thread.sleep(1000);
                }
                            
                // stop the SocketTextSource
                socketTextSource.stop();
                
                // count the number of samples intended to be sent to the DataTurbine
                File ctdData = new File(mockCTDData);
                int numberOfIntendedSamples = 0;
                List<String> lines = new ArrayList<String>();
                try {
                    lines = FileUtils.readLines(ctdData);
                    for (String line : lines ) {
                        if ( line.matches(socketTextSource.getPattern()) ) {
                            numberOfIntendedSamples++;
                        }
                    }
                    //numberOfIntendedSamples = lines.size();
                } catch (IOException e) {
                    fail("Couldn't read data file: " + e.getMessage());
                    
                }

                // retrieve the data from the DataTurbine
                ChannelMap requestMap = new ChannelMap();
                int entryIndex = requestMap.Add(socketTextSource.getRBNBClientName() + "/" + socketTextSource.getChannelName());
                log.debug("Request Map: " + requestMap.toString());
                Sink sink = new Sink();
                sink.OpenRBNBConnection(socketTextSource.getServer(), "lastEntrySink");
                sink.Request(requestMap, 0., 60000., "newest");
                ChannelMap responseMap = sink.Fetch(60000); // get data within 60 seconds
                String[] dtLines = responseMap.GetDataAsString(entryIndex);
                int numberOfSuccessfulSamples = dtLines.length;
                
                log.info("Intended samples  : " + numberOfIntendedSamples);
                log.info("Successful samples: " + numberOfSuccessfulSamples);
                
                assertEquals(numberOfIntendedSamples, numberOfSuccessfulSamples);

                sink.CloseRBNBConnection();
                // shut down the data source
                if ( mockDataSource != null ) {
                    mockDataSource.stop();
                    
                }
                
            } catch (ConfigurationException e) {
                log.error(e.getMessage());
                e.printStackTrace();
                fail("Couldn't configure a driver using " + configLocation);
                
            } catch (InterruptedException | SAPIException e) {
                e.printStackTrace();
            
            }
        }
    }
}
