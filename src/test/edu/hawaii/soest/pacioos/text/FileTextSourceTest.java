/**
 * 
 */
package edu.hawaii.soest.pacioos.text;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	 * Test method for {@link edu.hawaii.soest.pacioos.text.FileTextSource#FileTextSource(org.apache.commons.configuration.XMLConfiguration)}.
	 */
	@Test
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
				
				int numberOfIntendedSamples = 10;
				
				// retreive the data from the DataTurbine
			    ChannelMap requestMap = new ChannelMap();
			    int entryIndex = requestMap.Add(fileTextSource.getRBNBClientName() + "/" + fileTextSource.getChannelName());
			    log.debug("Request Map: " + requestMap.toString());
			    Sink sink = new Sink();
			    sink.OpenRBNBConnection(fileTextSource.getServer(), "lastEntrySink");
			    sink.Request(requestMap, 0., 240000., "newest");
			    ChannelMap responseMap = sink.Fetch(60000); // get data within 60 seconds
			    String[] dtLines = responseMap.GetDataAsString(entryIndex);
			    int numberOfSuccessfulSamples = dtLines.length;
			    
			    log.info("Intended samples  : " + numberOfIntendedSamples);
			    log.info("Successful samples: " + numberOfSuccessfulSamples);
			    
			    assertEquals(numberOfIntendedSamples, numberOfSuccessfulSamples);
				fileTextSource.stop();
				Thread.sleep(2000);
				
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
