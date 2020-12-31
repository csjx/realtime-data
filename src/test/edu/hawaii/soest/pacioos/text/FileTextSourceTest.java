/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * @author cjones
 *
 */
public class FileTextSourceTest extends SimpleTextSourceTest {

    private static final Log log = LogFactory.getLog(FileTextSourceTest.class);

	/**
	 * Exercises the FileTextSource by starting a mock DataTurbine,
	 * and an instance of the FileTextSource.
	 * Test method for {@link edu.hawaii.soest.pacioos.text.FileTextSource#FileTextSource(edu.hawaii.soest.pacioos.text.configure.Configuration)}.
	 */
	// @Test
	public void testFileTextSource() {
		
		// Get a list of instruments to test
		testMockInstruments = new ArrayList<String>();
		// testMockInstruments.add("CHUUK_CO2");
		testMockInstruments.add("KANEOHE_CO2");
		
		// test each mock instrument file, using file ending naming conventions
		for ( String instrument : testMockInstruments ) {
			
			// Get the XML configuration file for this data source
			String configLocation = testResourcesDirectory + "edu/hawaii/soest/pacioos/text/" +
					instrument + "-instrument-config.xml";

			SimpleTextSource fileTextSource = null;
			try {
				fileTextSource = TextSourceFactory.getSimpleTextSource(configLocation);
				fileTextSource.start();
				
				int numberOfIntendedSamples = 20;
				// Slightly pause before the retrieval 
				Thread.sleep(1000);
				
				// Retrieve the data from the DataTurbine
			    ChannelMap requestMap = new ChannelMap();
			    int entryIndex = requestMap.Add(fileTextSource.getRBNBClientName() +
                    "/" +
                    fileTextSource.getChannelName());
			    log.debug("Request Map: " + requestMap.toString());
			    Sink sink = new Sink();
			    sink.OpenRBNBConnection(fileTextSource.getServer(), fileTextSource.getRBNBClientName());
			    sink.Request(requestMap, 0., 300000., "newest");
			    ChannelMap responseMap = null;
			    String[] dtLines;
			    int numberOfSuccessfulSamples = 0;
			    responseMap = sink.Fetch(60000); // get data within 60 seconds
			    	
			    dtLines = responseMap.GetDataAsString(entryIndex);
			    numberOfSuccessfulSamples = dtLines.length;

			    log.info("Intended samples  : " + numberOfIntendedSamples);
			    log.info("Successful samples: " + numberOfSuccessfulSamples);
			    
			    assertEquals(numberOfIntendedSamples, numberOfSuccessfulSamples);
				fileTextSource.stop();
				
			} catch (ConfigurationException e) {
				log.error(e.getMessage());
				e.printStackTrace();
				fail("Couldn't configure a driver using " + configLocation);
				
			} catch (SAPIException e) {
				e.printStackTrace();
				fail("Couldn't connect to the mock Data Turbine");
				
			} catch (InterruptedException e) {
				e.printStackTrace();
				
			}
		}
	}

}
