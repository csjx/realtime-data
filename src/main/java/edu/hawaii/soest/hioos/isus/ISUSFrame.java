/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents a Satlantic ISUS V3 data sample
 *             from a binary StorX data file.
 *
 *   Authors: Christopher Jones
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
package edu.hawaii.soest.hioos.isus;

import java.io.UnsupportedEncodingException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class that represents a single (full) binary frame from a Satlantic ISUS V3
 * nitrate concentration instrument. The class represents both light and dark
 * frames, and provides access to the individual fields of the binary data
 * sample as described in the ISUS V3 Operation Manual.
 */
public class ISUSFrame {

	/* The Logger instance used to log system messages */
	private static Log log = LogFactory.getLog(ISUSFrame.class);

	/* A ISUS frame size in bytes as an integer */
	private final int ISUS_FRAME_SIZE = 8192;

	/* The date format for the timestamp applied to a ISUS frame (Julian day) */
	private static final SimpleDateFormat FRAME_DATE_FORMAT = new SimpleDateFormat("yyyyDDDHHmmss");

	/* The timezone used for the sample date */
	private static final TimeZone TZ = TimeZone.getTimeZone("Pacific/Honolulu");

	/* A ISUS frame as a ByteBuffer */
	private ByteBuffer isusFrame = ByteBuffer.allocate(ISUS_FRAME_SIZE);

	/*
	 * AS10 The frame header or synchronization string starts with 'SAT' for a
	 * Satlantic instrument, followed by three characters identifying the frame
	 * type. The last four characters are the instrument serial number.
	 */
	private ByteBuffer header = ByteBuffer.allocate(10);

	/*
	 * BS4 The date field denotes the date at the time of the sample, using the
	 * year and Julian day. The format is YYYYDDD.
	 */
	private ByteBuffer sampleDate = ByteBuffer.allocate(4);

	/*
	 * BD8 The time field gives the GMT/UTC time of the sample in decimal hours
	 * of the day.
	 */
	private ByteBuffer sampleTime = ByteBuffer.allocate(8);

	/* BF4 The Nitrate concentration as calculated by the ISUS in μMol/L */
	private ByteBuffer nitrogenConcentration = ByteBuffer.allocate(4);

	/* BF4 The auxiliary 1 fitting result of the ISUS is reported. */
	private ByteBuffer auxConcentration1 = ByteBuffer.allocate(4);

	/* BF4 The auxiliary 2 fitting result of the ISUS is reported. */
	private ByteBuffer auxConcentration2 = ByteBuffer.allocate(4);

	/* BF4 The auxiliary 3 fitting result of the ISUS is reported. */
	private ByteBuffer auxConcentration3 = ByteBuffer.allocate(4);

	/*
	 * BF4 The Root Mean Square Error of the ISUS’ concentration calculation is
	 * given, in ASCII frames to 6 decimal places.
	 */
	private ByteBuffer rmsError = ByteBuffer.allocate(4);

	/*
	 * BF4 The temperature inside the ISUS housing is given indegrees Celsius;
	 * in ASCII frames to 2 decimal places.
	 */
	private ByteBuffer insideTemperature = ByteBuffer.allocate(4);

	/* BF4 The temperature of the spectrometer is given in degreesCelsius */
	private ByteBuffer spectrometerTemperature = ByteBuffer.allocate(4);

	/* BF4 The temperature of the lamp is given in degrees Celsius */
	private ByteBuffer lampTemperature = ByteBuffer.allocate(4);

	/* BU4 The lamp on-time of the current data acquisition in seconds. */
	private ByteBuffer lampTime = ByteBuffer.allocate(4);

	/*
	 * BF4 The humidity inside the instrument, given in percent. Increasing
	 * values of humidity indicate a slow leak.
	 */
	private ByteBuffer humidity = ByteBuffer.allocate(4);

	/* BF4 The voltage of the lamp power supply. */
	private ByteBuffer lampVoltage12 = ByteBuffer.allocate(4);

	/* BF4 The voltage of the internal analog power supply. */
	private ByteBuffer internalPowerVoltage5 = ByteBuffer.allocate(4);

	/* BF4 The voltage of the main internal supply. */
	private ByteBuffer mainPowerVoltage = ByteBuffer.allocate(4);

	/* BF4 The average Reference Channel measurement during the sample time */
	private ByteBuffer referenceAverage = ByteBuffer.allocate(4);

	/* BF4 The variance of the Reference Channel measurements */
	private ByteBuffer referenceVariance = ByteBuffer.allocate(4);

	/* BF4 The Sea-Water Dark calculation in spectrometer counts. */
	private ByteBuffer seaWaterDarkCounts = ByteBuffer.allocate(4);

	/* BF4 The average value of all spectrometer channels */
	private ByteBuffer spectrometerAverage = ByteBuffer.allocate(4);

	/* BU2 The spectrometer counts of the channel wavelengths (256 total) */
	private ByteBuffer channelWavelengths = ByteBuffer.allocate(2 * 256);

	/* BU1 Binary frames only: A check sum validates binary frames. */
	private ByteBuffer checksum = ByteBuffer.allocate(1);

	/* A ISUS frame timestamp as a ByteBuffer */
	private ByteBuffer timestamp = ByteBuffer.allocate(7);

	public ISUSFrame(ByteBuffer isusFrame) {

		this.isusFrame = isusFrame;

		// parse each of the fields from the incoming byte buffer
		byte[] twoBytes = new byte[2];
		byte[] sixBytes = new byte[6];
		byte[] sevenBytes = new byte[7];
		byte[] fiveTwelveBytes = new byte[512];

		try {

			// set the header field
			this.isusFrame.get(sixBytes);
			this.header.put(sixBytes);
			this.isusFrame.get(twoBytes);
			this.header.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.header.put(twoBytes);

			// set the sample date field
			this.isusFrame.get(twoBytes);
			this.sampleDate.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.sampleDate.put(twoBytes);

			// set the sample time field
			this.isusFrame.get(sixBytes);
			this.sampleTime.put(sixBytes);
			this.isusFrame.get(twoBytes);
			this.sampleTime.put(twoBytes);

			// set the nitrogen concentration field
			this.isusFrame.get(twoBytes);
			this.nitrogenConcentration.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.nitrogenConcentration.put(twoBytes);

			// set the first auxillary concentration field
			this.isusFrame.get(twoBytes);
			this.auxConcentration1.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.auxConcentration1.put(twoBytes);

			// set the second auxillary concentration field
			this.isusFrame.get(twoBytes);
			this.auxConcentration2.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.auxConcentration2.put(twoBytes);

			// set the third auxillary concentration field
			this.isusFrame.get(twoBytes);
			this.auxConcentration3.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.auxConcentration3.put(twoBytes);

			// set the root mean square error field
			this.isusFrame.get(twoBytes);
			this.rmsError.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.rmsError.put(twoBytes);

			// set the inside temperature field
			this.isusFrame.get(twoBytes);
			this.insideTemperature.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.insideTemperature.put(twoBytes);

			// set the spectrometer temperature field
			this.isusFrame.get(twoBytes);
			this.spectrometerTemperature.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.spectrometerTemperature.put(twoBytes);

			// set the lamp temperature field
			this.isusFrame.get(twoBytes);
			this.lampTemperature.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.lampTemperature.put(twoBytes);

			// set the lamp time field
			this.isusFrame.get(twoBytes);
			this.lampTime.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.lampTime.put(twoBytes);

			// set the humdity field
			this.isusFrame.get(twoBytes);
			this.humidity.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.humidity.put(twoBytes);

			// set the lamp voltage12 field
			this.isusFrame.get(twoBytes);
			this.lampVoltage12.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.lampVoltage12.put(twoBytes);

			// set the internal power voltage5 field
			this.isusFrame.get(twoBytes);
			this.internalPowerVoltage5.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.internalPowerVoltage5.put(twoBytes);

			// set the main power voltage field
			this.isusFrame.get(twoBytes);
			this.mainPowerVoltage.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.mainPowerVoltage.put(twoBytes);

			// set the reference average field
			this.isusFrame.get(twoBytes);
			this.referenceAverage.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.referenceAverage.put(twoBytes);

			// set the reference variance field
			this.isusFrame.get(twoBytes);
			this.referenceVariance.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.referenceVariance.put(twoBytes);

			// set the sea water dark counts field
			this.isusFrame.get(twoBytes);
			this.seaWaterDarkCounts.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.seaWaterDarkCounts.put(twoBytes);

			// set the average wavelength field
			this.isusFrame.get(twoBytes);
			this.spectrometerAverage.put(twoBytes);
			this.isusFrame.get(twoBytes);
			this.spectrometerAverage.put(twoBytes);

			// set the channel wavelengths field
			this.isusFrame.get(fiveTwelveBytes);
			this.channelWavelengths.put(fiveTwelveBytes);

			// set the checksum field
			this.checksum.put(this.isusFrame.get());

			// set the timestamp field
			this.isusFrame.get(sixBytes);
			this.timestamp.put(sixBytes);
			this.timestamp.put(this.isusFrame.get());

		} catch (BufferUnderflowException bue) {

			bue.printStackTrace();

		}
	}

	/*
	 * AS10 The frame header or synchronization string starts with SAT for a
	 * Satlantic instrument, followed by threecharacters identifying the frame
	 * type. The last four characters are the instrument serial number.
	 */
	public String getHeader() {
		try {
			return new String(this.header.array(), "US-ASCII");

		} catch (UnsupportedEncodingException uee) {
			log.debug("The string encoding was not recognized: " + uee.getMessage());
			return null;
		}

	}

	/*
	 * Get the frame serial number as a String
	 * 
	 * @return frameSerialNumber - the serial number as a String
	 */
	public String getSerialNumber() {

		try {

			byte[] fourBytes = new byte[4];
			this.header.position(6);
			this.header.get(fourBytes);
			this.header.flip();
			return new String(fourBytes, "US-ASCII");

		} catch (UnsupportedEncodingException uee) {
			log.debug("The string encoding was not recognized: " + uee.getMessage());
			return null;
		}

	}

	/*
	 * BS4 The date field denotes the date at the time of the sample, using the
	 * year and Julian day. The format is YYYYDDD.
	 */
	public String getSampleDate() {
		this.sampleDate.flip();
		int dateStamp = this.sampleDate.getInt();
		return String.format("%7d", dateStamp);

	}

	/*
	 * BD8 The time field gives the GMT/UTC time of the sample in decimal hours
	 * of the day.
	 */
	public String getSampleTime() {
		this.sampleTime.flip();
		double timeStamp = this.sampleTime.getDouble();
		return String.format("%13.10f", timeStamp);

	}

	/*
	 * Return the sample date from the sampleDate and sampleTime fields
	 * combined.
	 *
	 * @return sampleDateTime - the sample date and time as a Java Date object
	 */
	public Date getSampleDateTime() {
		SimpleDateFormat sampleDateFormat = new SimpleDateFormat("yyyyDDDHHmmss");
		Date sampleDateTime = new Date(0L);

		// get hours/minutes/seconds from the decimal hours time field
		double decimalHour = new Double(getSampleTime()).doubleValue();
		int wholeHour = new Double(decimalHour).intValue();
		double fraction = decimalHour - wholeHour;
		int minutes = new Double(fraction * 60d).intValue();
		double secondsFraction = (fraction * 60d) - minutes;
		int seconds = new Double(Math.round((secondsFraction * 60d))).intValue();

		// create a string version of the date
		String dateString = getSampleDate();
		dateString += new Integer(wholeHour).toString();
		dateString += new Integer(minutes).toString();
		dateString += new Integer(seconds).toString();

		// convert to a Java Date (instrument time is UTC)
		try {
			sampleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			sampleDateTime = sampleDateFormat.parse(dateString);

		} catch (ParseException pe) {
			log.debug(
					"There was a problem parsing the sampleDateTime. The error " + "message was: " + pe.getMessage());
		}

		return sampleDateTime;
	}

	/* in ASCII frames to 2 decimal places. */
	public double getNitrogenConcentration() {
		this.nitrogenConcentration.flip();
		return (new Float(this.nitrogenConcentration.getFloat())).doubleValue();
	}

	/* BF4 The first auxiliary fitting result of the ISUS is reported. */
	public double getAuxConcentration1() {
		this.auxConcentration1.flip();
		return (new Float(this.auxConcentration1.getFloat())).doubleValue();
	}

	/* BF4 The second auxiliary fitting result of the ISUS is reported. */
	public double getAuxConcentration2() {
		this.auxConcentration2.flip();
		return (new Float(this.auxConcentration2.getFloat())).doubleValue();
	}

	/* BF4 The first auxiliary fitting result of the ISUS is reported. */
	public double getAuxConcentration3() {
		this.auxConcentration3.flip();
		return (new Float(this.auxConcentration3.getFloat())).doubleValue();
	}

	/*
	 * BF4 The Root Mean Square Error of the ISUS’ concentration calculation is
	 * given, in ASCII frames to 6 decimal places.
	 */
	public double getRmsError() {
		this.rmsError.flip();
		return (new Float(this.rmsError.getFloat())).doubleValue();
	}

	/* The temperature inside the housing in degrees Celcius. */
	public double getInsideTemperature() {
		this.insideTemperature.flip();
		return (new Float(this.insideTemperature.getFloat())).doubleValue();
	}

	/* The temperature of the spectrometer in degrees Celcius. */
	public double getSpectrometerTemperature() {
		this.spectrometerTemperature.flip();
		return (new Float(this.spectrometerTemperature.getFloat())).doubleValue();
	}

	/* The temperature of the lamp in degrees Celcius. */
	public double getLampTemperature() {
		this.lampTemperature.flip();
		return (new Float(this.lampTemperature.getFloat())).doubleValue();
	}

	/* BU4 The lamp on-time of the current data acquisition in seconds. */
	int getLampTime() {
		this.lampTime.flip();

		return this.lampTime.getInt();
	}

	/*
	 * BF4 The humidity inside the instrument, given in percent. Increasing
	 * values of humidity indicate a slow leak.
	 */
	public double getHumidity() {
		this.humidity.flip();
		return (new Float(this.humidity.getFloat())).doubleValue();
	}

	/* BF4 The voltage of the lamp power supply. */
	public double getLampVoltage12() {
		this.lampVoltage12.flip();
		return (new Float(this.lampVoltage12.getFloat())).doubleValue();
	}

	/* BF4 The voltage of the internal analog power supply. */
	public double getInternalPowerVoltage5() {
		this.internalPowerVoltage5.flip();
		return (new Float(this.internalPowerVoltage5.getFloat())).doubleValue();
	}

	/* BF4 The voltage of the main internal supply. */
	public double getMainPowerVoltage() {
		this.mainPowerVoltage.flip();
		return (new Float(this.mainPowerVoltage.getFloat())).doubleValue();
	}

	/*
	 * BF4 The average Reference Channel measurement during thesample time, in
	 * ASCII mode to 2 decimal places.
	 */
	public double getReferenceAverage() {
		this.referenceAverage.flip();
		return (new Float(this.referenceAverage.getFloat())).doubleValue();
	}

	/*
	 * BF4 The variance of the Reference Channel measurements, inASCII mode to 2
	 * decimal places.
	 */
	public double getReferenceVariance() {
		this.referenceVariance.flip();
		return (new Float(this.referenceVariance.getFloat())).doubleValue();
	}

	/*
	 * BF4 An AF formatted field representing the Sea-Water Darkcalculation (to
	 * 2 decimal places), in spectrometer counts.
	 */
	public double getSeaWaterDarkCounts() {
		this.seaWaterDarkCounts.flip();
		return (new Float(this.seaWaterDarkCounts.getFloat())).doubleValue();
	}

	/*
	 * BF4 An AF formatted field representing the average value of all
	 * spectrometer channels, to 2 decimal places.
	 */
	public double getSpectrometerAverage() {
		this.spectrometerAverage.flip();
		return (new Float(this.spectrometerAverage.getFloat())).doubleValue();
	}

	/* BU2 The counts of the given channel wavelength of thespectrometer. */
	public int getChannelWavelengthCounts(int wavelength) {

		int position = (wavelength * 2) - 2;
		short counts = this.channelWavelengths.getShort(position);

		return new Short(counts).intValue();
	}

	/*
	 * BU1 Binary frames only: A check sum validates binary frames. Satlantic’s
	 * software rejects invalid frames.
	 */
	public int getChecksum() {
		this.checksum.flip();

		return this.checksum.get() & 0xFF;
	}

	/**
	 * Get the frame timestamp field as a byte array. The timestamp format is
	 * YYYYDDD from the first 3 bytes, and HHMMSS.SSS from the last four:
	 * Example: 1E AC CC = 2010316 (year 2010, julian day 316) 09 9D 3E 20 =
	 * 16:13:00.000 (4:13 pm)
	 * 
	 * @return timestamp - the frame timestamp as a byte array
	 */
	public byte[] getTimestamp() {

		this.timestamp.flip();
		return this.timestamp.array();

	}
}
