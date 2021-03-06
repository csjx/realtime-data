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

package edu.hawaii.soest.hioos.satlantic;

import java.text.ParseException;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for the Calibration class
 */

public class CalibrationTest extends TestCase {
  
  Calibration calibration;
  
  /**
   * Constructor: builds the test case
   */
  public CalibrationTest(String testName) {
    super(testName);
    
  }
  
  public void setUp() {
    
    // read the sample data from a binary StorX file
    try {
      
      // create a parser instance and test that it succeeds
      this.calibration = new Calibration();
      
    } catch (Exception e) {
      fail("There was a problem. The error was: " + e.getMessage());
      
    }
    
  }
  
  /**
   *  Create a test suite
   */
  public static Test suite() {
    
    TestSuite suite = new TestSuite();
    //suite.addTest(new CalibrationTest("testApplyPolyU"));
    
    return suite;
  }
  
  /**
   *  Test the POLYU calibration
   */
  public void testApplyPolyU() {
    double observedValue = 32787d;
    boolean isImmersed = true;
    ArrayList<Double> coefficients = new ArrayList<Double>();
    coefficients.add(0.25d);
    coefficients.add(0.000382698d);
    String fitType = "POLYU";
    
    Double result = this.calibration.apply(observedValue, isImmersed, fitType);
    assertTrue(result.equals(12.797519326d));
  }
  
  /**
   *  Test the calibration file parsing
   */
  public void testParse() {
    
    String calibrationURL = "http://realtime.pacioos.hawaii.edu/hioos/wqb-kn/calibration/SATSTX0063a.cal";
    // test the parsing of the data buffer
    try {
      
      boolean success = this.calibration.parse(calibrationURL);
      //assertTrue(1 == 1);
      
    } catch (ParseException pe) {
      
      fail("Parsing failed. The error was: " + pe.getMessage());
      pe.printStackTrace();
    }
    
  }
  
}
