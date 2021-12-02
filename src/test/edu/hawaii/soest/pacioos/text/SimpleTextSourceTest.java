/*
 *  Copyright: 2016 Regents of the University of Hawaii and the
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;

import com.rbnb.api.AddressException;
import com.rbnb.api.SerializeException;
import com.rbnb.api.Server;

/**
 * The SimpleTextSourceTest class provides methods shared across the 
 * individual test subclasses.  Mainly, it starts up a mock 
 * Data Turbine for the tests to push data into, and shuts it down
 * after the tests are complete.
 * 
 * @author cjones
 */
public class SimpleTextSourceTest {

    private static final Log log = LogFactory.getLog(SimpleTextSourceTest.class);

    protected Server mockDT;
	
    protected String testResourcesDirectory;
	
    protected String mockCTDData;
	
    protected MockDataSource mockDataSource;
		
    protected Thread mockDataSourceThread;
	
    protected List<String> testMockInstruments;
    

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
		FileUtils.deleteDirectory(new File("/tmp/dt"));
		FileUtils.forceMkdir(new File("/tmp/dt"));
		String[] options = new String[]{"-a", "127.0.0.1:33333", "-H", "/tmp/dt"};
		mockDT = Server.launchNewServer(options);
		log.info("Dataturbine is running on " + mockDT.getAddress());
		Thread.sleep(2000);
		
	}

	/**
	 * Clean up, shutting down the temporary Data Turbine instance
	 * 
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		
		// shut down the DT
		log.info("Stopping the mock DataTurbine");
		if ( mockDT != null ) {
			try {
				mockDT.stop();
			} catch (AddressException | SerializeException | IOException | InterruptedException e) {
				e.printStackTrace();
			}
        }
		
		// clear the list of test instruments
		testMockInstruments.clear();
	}


}
