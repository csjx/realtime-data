/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To create a simulated ADCP PD0 formatted binary data source
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
package edu.hawaii.soest.kilonalu.adcp;

import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
// import gnu.io.PortInUseException;

/**
 * A simple class used to generate a fake ADCP data stream from a data
 * file formatted in the RDI common PD0 binary format.
 */
public class FakeSerialADCP {
  
  private static String SERIAL_PORT = "/dev/ttyS0";
  private static String FAKE_DATA_FILE = 
    "/home/cjones/development/bbl/test/ALE31X_015ADCP015R00_20060921.10.1.000";
  //  "/home/cjones/development/bbl/test/KNRT_20061120162608_000r.007";
  private static String FILE_ACCESS_MODE = "r";
  /**
   * Constructor - create an instance of the fake data source object
   */
  public FakeSerialADCP() {
    super();
  }
  
  /**
   * A method to list the serial ports available on the system
   */
  static void listPorts() {
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
  void connect( String portName ) throws Exception {
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
        serialPort.setSerialPortParams(9600,SerialPort.DATABITS_8,
                                            SerialPort.STOPBITS_1,
                                            SerialPort.PARITY_NONE);

        OutputStream outStream = serialPort.getOutputStream();
        DataOutputStream out = new DataOutputStream(outStream);

        (new Thread(new SerialDataWriter(out))).start();

      } else {
       System.out.println("Error: Only serial ports are handled by this example.");
      }
    }
  }

  /**
   * A serial data writer that implements the Runnable interface for threading
   */
  public static class SerialDataWriter implements Runnable {
    OutputStream out;

    /**
     * Constructor - create a serial writer object with the given output stream
     */
    public SerialDataWriter( OutputStream out ) {
      this.out = out;
    }


    public void run(){
      try {
        RandomAccessFile testADCPFile = 
          new RandomAccessFile(FAKE_DATA_FILE, FILE_ACCESS_MODE);
        // the length of the data file in bytes
        long testFileLength = testADCPFile.length();
        System.err.println("testBuffer length: " + testFileLength);
        byte[] dataBuffer = new byte[(int)testFileLength];
        
        //while( true ) {
          int bytesRead = testADCPFile.read(dataBuffer);
          testADCPFile.seek(0);
          for ( int i=0; i < dataBuffer.length; ++i ) {
            this.out.write(dataBuffer[i]);
          }
        //}
      } catch ( FileNotFoundException fnfe ) {
        fnfe.printStackTrace();
      } catch (IOException e ) {
        e.printStackTrace();
      }
    }
  }

  /**
   * The main method for running the code
   * @ param args  the command line list of string arguments, none are needed
   */

  public static void main (String args[]) {
    try {
      // ( new FakeSerialADCP() ).listPorts();
      ( new FakeSerialADCP() ).connect(SERIAL_PORT);

    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }

}