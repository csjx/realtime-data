/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To create a simulated CTD ASCII data source
 *    Authors: Christopher Jones
 *
 * $HeadURL$
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
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
package edu.hawaii.soest.kilonalu.ctd;

import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Calendar;

import org.apache.log4j.Logger;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
// import gnu.io.PortInUseException;

/**
 * A simple class used to generate a fake ADCP data stream from a data
 * file formatted in the RDI common PD0 binary format.
 */
public class FakeSerialCTD {
  
  private static String SERIAL_PORT = "/dev/ttyS0";
  
  private static String FAKE_DATA_FILE = 
    "/home/cjones/development/bbl/test/AW01XX_002CTDXXXXR00.10.1.txt";
  
  private static String FILE_ACCESS_MODE = "r";
  
  Thread streamingThread;
  
  private static OutputStream outStream;
  
  private static DataOutputStream out;
  
  private static String sample;
  
  /**
   * Constructor - create an instance of the fake data source object
   */
  public FakeSerialCTD() {
  }
  
  /**
   * A method to list the serial ports available on the system
   */
  private static void listPorts() {
    Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
    while ( portEnum.hasMoreElements() ) {
      CommPortIdentifier portIdentifier = 
        (CommPortIdentifier) portEnum.nextElement();
      System.out.println(portIdentifier.getName());
    }
  }  
  
  /**
   * A method used to connect to the serial port for communication
   * @param portName the name of the serial port to connect 
   *                 to (i.e. COM1, COM4, etc.)
   */
  private void connect( String portName ) throws Exception {
    CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
    
    // check if the port is busy
    if ( portIdentifier.isCurrentlyOwned() ) {
      System.out.println("Error: Port " + portName + "is currently in use.");
    } else {

      //if not busy, open the serial port for communication
      CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);

      if ( commPort instanceof SerialPort ) {
        SerialPort serialPort = (SerialPort)commPort;
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,
                                            SerialPort.STOPBITS_1,
                                            SerialPort.PARITY_NONE);

        this.outStream = serialPort.getOutputStream();
        this.out = new DataOutputStream(outStream);

      } else {
       
        System.out.println("Error: Only serial ports are handled by this example.");
      }
    }
  }

  
  private void runWork() {
   
    try {

      RandomAccessFile testCTDFile = 
        new RandomAccessFile(FAKE_DATA_FILE, FILE_ACCESS_MODE);

      while ( (this.sample = testCTDFile.readLine()) != null ) {
        this.out.writeChars(this.sample);
        this.out.writeChars("\r\n");
        this.streamingThread.sleep(15000);
          
      }
      testCTDFile.close();
      
    } catch ( FileNotFoundException fnfe ) {
      fnfe.printStackTrace();

    } catch ( InterruptedException intde ) {
      intde.printStackTrace();
    
    } catch (IOException e ) {
      e.printStackTrace();

    }
      
  }
  
  /**
   * A method that creates and starts a new Thread with a run() method that 
   * begins processing the data streaming from the source instrument.
   */
  private void startThread() {
  
    // build the runnable class and implement the run() method
    Runnable runner = new Runnable() {
      
      public void run() {
        runWork();  
      }
    };
  
    // build the Thread and start it, indicating that it has been started
    this.streamingThread = new Thread(runner, "StreamingThread");
    this.streamingThread.start();     
  }
  
  /**
   * The main method for running the code
   * @ param args  the command line list of string arguments, none are needed
   */
  public static void main (String args[]) {
    try {
      
      FakeSerialCTD fakeSerialCTD = new FakeSerialCTD();
      
      fakeSerialCTD.listPorts();
      fakeSerialCTD.connect(SERIAL_PORT);
      fakeSerialCTD.startThread();

    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }

}