/**
 * 
 */
package edu.hawaii.soest.pacioos.text;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
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
		mockDT.stop();
		// shut down the data source
		mockDataSource.stop();
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
			fail("Couldn't configure a driver using " + configLocation);
			e.printStackTrace();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
		
}
