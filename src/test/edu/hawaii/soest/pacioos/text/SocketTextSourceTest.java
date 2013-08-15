/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A test class that exercises the SocketTextSource class.
 *
 *   Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy:  $
 * $LastChangedRevision:  $
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rbnb.api.Server;

/**
 * Exercises the SocketTextSource by starting a mock data source server, a mock DataTurbine,
 * and an instance of the SocketTextSource.
 * @author cjones
 *
 */
public class SocketTextSourceTest {

    private static final Log log = LogFactory.getLog(SocketTextSourceTest.class);

    private Server mockDT;
	
    private String testResourcesDirectory;
	
    private String mockCTDData;
	
	private MockDataSource mockDataSource;
		
	private Thread mockDataSourceThread;
	

	/**
	 * Start a mock DataTurbine and a MockDataSource, both of which will be connected to 
	 * by the SocketTextSource class being tested.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		// get the resources directory
		InputStream propsStream = ClassLoader.getSystemResourceAsStream("test.properties");
		Properties properties = new Properties();
		try {
			properties.load(propsStream);
			testResourcesDirectory = properties.getProperty("test.resources.directory");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// start up a mock DataTurbine instance
		//FileUtils.deleteDirectory(new File("/tmp/dt"));
		FileUtils.forceMkdir(new File("/tmp/dt"));
		String[] options = new String[]{"-a", "127.0.0.1:33333", "-H", "/tmp/dt"};
		mockDT = Server.launchNewServer(options);
		log.info("Dataturbine is running on " + mockDT.getAddress());
		Thread.sleep(1000);
		
		// start up the mock data source
		mockCTDData = testResourcesDirectory + 
			"edu/hawaii/soest/pacioos/text/AW02XX001CTDXXXXR00-mock-data.txt";
		mockDataSource = new MockDataSource(mockCTDData);
		mockDataSourceThread = new Thread(mockDataSource);
		mockDataSourceThread.start();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		// shut down the DT
		log.info("Stopping the mock DataTurbine");
		if ( mockDT != null ) {
			mockDT.stop();
			
		}
		// shut down the data source
		if ( mockDataSource != null ) {			
			mockDataSource.stop();
			
		}
	}

	/**
	 * Test method for {@link edu.hawaii.soest.pacioos.text.SocketTextSource#SocketTextSource(org.apache.commons.configuration.XMLConfiguration)}.
	 */
	@Test
	public void testSocketTextSource() {
		
		String configLocation = testResourcesDirectory +
				"edu/hawaii/soest/pacioos/text/mock-socket-instrument-config.xml";

		//fail("Not yet implemented");
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
			
		} catch (ConfigurationException e) {
			log.error(e.getMessage());
			e.printStackTrace();
			fail("Couldn't configure a driver using " + configLocation);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
		
}
