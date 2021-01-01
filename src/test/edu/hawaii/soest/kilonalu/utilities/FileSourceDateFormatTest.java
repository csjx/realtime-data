/**
 *  Copyright: 2012 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *             
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
package edu.hawaii.soest.kilonalu.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * Tests that exercise the FileSource class' ability to apply date formats
 * to date fields in ASCII sample data from typical instrument output (CTDs)
 * 
 * @author cjones
 *
 */
public class FileSourceDateFormatTest {

	private FileSource fileSource = new FileSource();
	private static final Log log = LogFactory.getLog(FileSourceDateFormatTest.class);

	/**
	 * Test date parsing with the last column of the sample containing 
	 * the single date timestamp. Typical of SBE16plus CTD ASCII output.
	 */
	@Test
	public void testDatetimeLastColumn() {
		
		printHeader("testDatetimeLastColumn");
		
		Date sampleDate = null;
		Date actualDate = null;
		// define the data line
		String line = "# 26.1675,  4.93111,    0.695, 0.1918, 0.1163,  31.4138, 09 Dec 2012 15:46:55";
		log.debug("Sample: " + line);
		
		// set the one-based date field index list
		List<Integer> dateFields = new ArrayList<Integer>();
		dateFields.add(7);
		
		// set the one-based date format list
		List<String> dateFormats = new ArrayList<String>();
		dateFormats.add("dd MMM yyyy HH:mm:ss");
		
		fileSource.setDateFields(dateFields);
		fileSource.setDateFormats(dateFormats);
		fileSource.setDelimiter(",");
		fileSource.setTimezone("HST");
		
		try {
			sampleDate = fileSource.getSampleDate(line);
			
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Couldn't parse sampleDate: " + pe.getMessage());
			
		}
		
		try {
			SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
			format.setTimeZone(TimeZone.getTimeZone("Pacific/Honolulu"));
			actualDate = format.parse("09 Dec 2012 15:46:55");
			
		} catch (ParseException pe2) {
			pe2.printStackTrace();
			fail("Couldn't parse actualDate: " + pe2.getMessage());
			
		}
		//are they equal ?
		try {
			assertEquals(sampleDate, actualDate);
			log.info("Success");
			
		} catch (AssertionError ae) {
			fail(ae.getMessage());
			
		}
	}

	/**
	 * Test date parsing with the last column of the sample containing 
	 * the single date timestamp. Typical of SBE16plus CTD ASCII output.
	 */
	@Test
	public void testDatetimeFirstColumnWithWhitespaceDelim() {
		
		printHeader("testDatetimeFirstColumnWithWhitespaceDelim");
		
		Date sampleDate = null;
		Date actualDate = null;
		// define the data line
		String line = "10/01/2013 14:41:09 23.39 0.323   0.16  14.634  -0.045 -4.01     9.2   0.4    2.4    12.6";
		log.debug("Sample: " + line);
		
		// set the one-based date field index list
		List<Integer> dateFields = new ArrayList<Integer>();
		dateFields.add(1);
		dateFields.add(2);
		
		// set the one-based date format list
		List<String> dateFormats = new ArrayList<String>();
		dateFormats.add("MM/dd/yyyy");
		dateFormats.add("HH:mm:ss");
		
		fileSource.setDateFields(dateFields);
		fileSource.setDateFormats(dateFormats);
		fileSource.setDelimiter("\\s+");
		fileSource.setTimezone("HST");
		
		try {
			sampleDate = fileSource.getSampleDate(line);
			
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Couldn't parse sampleDate: " + pe.getMessage());
			
		}
		
		try {
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			format.setTimeZone(TimeZone.getTimeZone("Pacific/Honolulu"));
			actualDate = format.parse("10/01/2013 14:41:09");
			
		} catch (ParseException pe2) {
			pe2.printStackTrace();
			fail("Couldn't parse actualDate: " + pe2.getMessage());
			
		}
		//are they equal ?
		try {
			assertEquals(sampleDate, actualDate);
			log.info("Success");
			
		} catch (AssertionError ae) {
			fail(ae.getMessage());
			
		}
	}

	/**
	 * Test date parsing with the last two columns of the sample containing 
	 * the date then the time.
	 */
	@Test
	public void testDatetimeLastTwoColumns() {
		
		printHeader("testDatetimeLastTwoColumns");
		
		Date sampleDate = null;
		Date actualDate = null;
		// define the data line
		String line = "#  25.4746,  5.39169,    0.401,  35.2570, 09 Dec 2012, 15:44:36";
		log.debug("Sample: " + line);
		
		// set the one-based date field index list
		List<Integer> dateFields = new ArrayList<Integer>();
		dateFields.add(5); // date is in column 5
		dateFields.add(6); // time is in column 6
		
		// set the one-based date format list
		List<String> dateFormats = new ArrayList<String>();
		dateFormats.add("dd MMM yyyy");
		dateFormats.add("HH:mm:ss");
		
		fileSource.setDateFields(dateFields);
		fileSource.setDateFormats(dateFormats);
		fileSource.setDelimiter(",");
		fileSource.setTimezone("HST");
		
		try {
			sampleDate = fileSource.getSampleDate(line);
			
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Couldn't parse sampleDate: " + pe.getMessage());
			
		}
		
		try {
			SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
			format.setTimeZone(TimeZone.getTimeZone("Pacific/Honolulu"));
			actualDate = format.parse("09 Dec 2012, 15:44:36");
			
		} catch (ParseException pe2) {
			pe2.printStackTrace();
			fail("Couldn't parse actualDate: " + pe2.getMessage());
			
		}
		//are they equal ?
		try {
			assertEquals(sampleDate, actualDate);
			log.info("Success");
			
		} catch (AssertionError ae) {
			fail(ae.getMessage());
			
		}
	}

	/**
	 * Test date parsing with the first six columns of the sample containing 
	 * the date and time, like yyyy, MM, dd, HH, mm, ss, temp, sal, pressure.
	 */
	@Test
	public void testDatetimeFirstSixColumns() {
		
		printHeader("testDatetimeFirstSixColumns");
		
		Date sampleDate = null;
		Date actualDate = null;
		// define the data line
		String line = "2012, 12, 09, 15, 44, 36, 25.4746,  5.39169,    0.401,  35.2570";
		log.debug("Sample: " + line);
		
		// set the one-based date field index list
		List<Integer> dateFields = new ArrayList<Integer>();
		dateFields.add(1); // year
		dateFields.add(2); // month
		dateFields.add(3); // day
		dateFields.add(4); // hour
		dateFields.add(5); // min
		dateFields.add(6); // sec
		
		// set the one-based date format list
		List<String> dateFormats = new ArrayList<String>();
		dateFormats.add("yyyy");
		dateFormats.add("MM");
		dateFormats.add("dd");
		dateFormats.add("HH");
		dateFormats.add("mm");
		dateFormats.add("ss");
		
		fileSource.setDateFields(dateFields);
		fileSource.setDateFormats(dateFormats);
		fileSource.setDelimiter(",");
		fileSource.setTimezone("HST");
		
		try {
			sampleDate = fileSource.getSampleDate(line);
			
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Couldn't parse sampleDate: " + pe.getMessage());
			
		}
		
		try {
			SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
			format.setTimeZone(TimeZone.getTimeZone("Pacific/Honolulu"));
			actualDate = format.parse("09 Dec 2012, 15:44:36");
			
		} catch (ParseException pe2) {
			pe2.printStackTrace();
			fail("Couldn't parse actualDate: " + pe2.getMessage());
			
		}
		//are they equal ?
		try {
			assertEquals(sampleDate, actualDate);
			log.info("Success");
			
		} catch (AssertionError ae) {
			fail(ae.getMessage());
			
		}
	}

	public void printHeader(String testName) {
		log.info("--------------------------------------------------");
		log.info(testName);
		log.info("--------------------------------------------------");
	}
}
