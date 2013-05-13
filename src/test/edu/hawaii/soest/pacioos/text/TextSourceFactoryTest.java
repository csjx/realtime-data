/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: Unit tests for the TextSourceFactory class
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Exercise methods in the TextSourceFactory for producing the three types of SimpleTextSource
 * instances.
 * 
 * @author cjones
 *
 */
public class TextSourceFactoryTest {

	String testResourcesDirectory;
	@Before
	public void setUp() throws Exception {
		InputStream propsStream = ClassLoader.getSystemResourceAsStream("test.properties");
		Properties properties = new Properties();
		try {
			properties.load(propsStream);
			testResourcesDirectory = properties.getProperty("test.resources.directory");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test getting a file text source driver
	 */
	@Test
	public void testGetFileTextSource() {
		String configLocation = testResourcesDirectory +
				"edu/hawaii/soest/pacioos/text/mock-file-instrument-config.xml";
				SimpleTextSource fileTextSource = null;
				try {
					fileTextSource = TextSourceFactory.getSimpleTextSource(configLocation);
					assertNotNull(fileTextSource);
					
					String connectionType = fileTextSource.getConnectionType();
					assertEquals("file", connectionType);
					
				} catch (ConfigurationException e) {
					fail("Couldn't configure a driver using " + configLocation);
					e.printStackTrace();
				}
	}

	/**
	 * Test getting a socket text source driver
	 */
	@Test
	public void testGetSocketTextSource() {
		String configLocation = testResourcesDirectory +
				"edu/hawaii/soest/pacioos/text/mock-socket-instrument-config.xml";
		SimpleTextSource socketTextSource = null;
		try {
			socketTextSource = TextSourceFactory.getSimpleTextSource(configLocation);
			assertNotNull(socketTextSource);
			
			String connectionType = socketTextSource.getConnectionType();
			assertEquals("socket", connectionType);
			
		} catch (ConfigurationException e) {
			fail("Couldn't configure a driver using " + configLocation);
			e.printStackTrace();
		}
	}
	
	/**
	 * Test getting a serial text source driver
	 */
	@Test
	public void testGetSerialTextSource() {
		String configLocation = testResourcesDirectory +
		    "edu/hawaii/soest/pacioos/text/mock-serial-instrument-config.xml";
		SimpleTextSource serialTextSource = null;
		try {
			serialTextSource = TextSourceFactory.getSimpleTextSource(configLocation);
			assertNotNull(serialTextSource);
			
			String connectionType = serialTextSource.getConnectionType();
			assertEquals("serial", connectionType);
			
		} catch (ConfigurationException e) {
			fail("Couldn't configure a driver using " + configLocation);
			e.printStackTrace();
		}
	}
	
}
