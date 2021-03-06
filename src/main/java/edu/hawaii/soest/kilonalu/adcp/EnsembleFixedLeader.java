/*
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that represents the ensemble Fixed Leader
 *             component of data produced by an RDI 1200kHz Workhorse Acoustic
 *             Doppler Current Profiler in the default PD0 format as described
 *             in RDI's "Workhorse Commands and Output Data Format" manual,
 *             P/N 957-6156-00 (March 2005)
 *    Authors: Christopher Jones
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

import java.nio.ByteBuffer;

/**
 * A class that represents the Fixed Leader of data produced by
 * an RDI 1200kHz Workhorse Acoustic Doppler Current Profiler in the
 * default PD0 format.
 */
public final class EnsembleFixedLeader {

    /**
     * A field that contains the base frequency index for Navigator only,
     * and is considered a spare for the Workhorse.
     */
    private ByteBuffer baseFrequencyIndex = ByteBuffer.allocate(1);

    /**
     * A field that contains the beam angle (H-ADCP only). This byte is
     * considered a spare for all other ADCPs.
     */
    private ByteBuffer beamAngle = ByteBuffer.allocate(1);

    /**
     * A field that contains the distance to the middle of the first depth
     * cell (bin).  This distance is a function of depth cell length,
     * the profiling mode, the blank after transmit distance.
     */
    private ByteBuffer binOneDistance = ByteBuffer.allocate(2);

    /**
     * A field that contains the blanking distance used by the Workhorse
     * to allow the transmit circuits time to recover before the receive
     * cycle begins.
     */
    private ByteBuffer blankAfterTransmit = ByteBuffer.allocate(2);

    /**
     * A field that contains the coordinate transformation processing
     * parameters.  These firmware switches indicate how the Workhorse
     * collected data.
     * See the manual for interpreting each bit.
     */
    private ByteBuffer coordinateTransformParams = ByteBuffer.allocate(1);

    /**
     * A field that contains the revision number of the CPU firmware.
     */
    private ByteBuffer cpuFirmwareRevision = ByteBuffer.allocate(1);

    /**
     * A field that contains the version number of the CPU firmware.
     */
    private ByteBuffer cpuFirmwareVersion = ByteBuffer.allocate(1);

    /**
     * A field that contains the serial number of the CPU board.
     */
    private ByteBuffer cpuBoardSerialNumber = ByteBuffer.allocate(8);

    /**
     * A field that contains the length of one depth cell.
     */
    private ByteBuffer depthCellLength = ByteBuffer.allocate(2);

    /**
     * A field that contains the actual threshold value used to flag
     * water-current data as good or bad.  If the error velocity value
     * exceeds this threshold, the Workhorse flags all four beams of the
     * affected bin as bad.
     */
    private ByteBuffer errorVelocityThreshold = ByteBuffer.allocate(2);

    /**
     * A field that contains the threshold value used to reject data
     * received from a false target, usually fish.
     */
    private ByteBuffer falseTargetThreshold = ByteBuffer.allocate(1);

    /**
     * A field that stores the default Fixed Leader ID (0x0000)
     */
    private static final ByteBuffer DEFAULT_FIXED_LEADER_ID =
        ByteBuffer.wrap(new byte[]{0x00, 0x00});

    /**
     * A field that stores the Fixed Leader ID (2-bytes) in a ByteBuffer
     */
    private ByteBuffer fixedLeaderID = ByteBuffer.allocate(2);

    /**
     * A field that stores the Fixed Leader Spare (1-byte) in a ByteBuffer
     * This field is reserved for RDI internal use.
     */
    private ByteBuffer fixedLeaderSpare = ByteBuffer.allocate(1);

    /**
     * A field that contains a correction factor for physical
     * heading misalignment.
     */
    private ByteBuffer headingAlignment = ByteBuffer.allocate(2);

    /**
     * A field that contains a correction factor for electrical/magnetic
     * heading bias.
     */
    private ByteBuffer headingBias = ByteBuffer.allocate(2);

    /**
     * A field stores the lag length, which is the time period between
     * sound pulses.
     */
    private ByteBuffer lagLength = ByteBuffer.allocate(1);

    /**
     * A field stores Contains the minimum threshold of correlation that
     * water-profile data can have to be considered good data.
     */
    private ByteBuffer lowCorrelationThreshold = ByteBuffer.allocate(1);

    /**
     * A field contains the number of beams used to calculate velocity data,
     * not the number of physical beams. The Workhorse needs only three
     * beams to calculate water-current velocities.
     */
    private ByteBuffer numberOfBeams = ByteBuffer.allocate(1);

    /**
     * A field that contains the number of depth cells over which the
     * Workhorse collects data.
     */
    private ByteBuffer numberOfCells = ByteBuffer.allocate(1);

    /**
     * A field that contains the number of code repetitions in the transmit pulse.
     */
    private ByteBuffer numberOfCodeRepetitions = ByteBuffer.allocate(1);

    /**
     * A field that indicates whether the PD0 data are real or simulated.
     * The default is real data(0).
     */
    private ByteBuffer pdRealOrSimulatedFlag = ByteBuffer.allocate(1);

    /**
     * A field that contains the minimum percentage of water-profiling pings
     * in an ensemble that must be considered good to output velocity data.
     */
    private ByteBuffer percentGoodMinimum = ByteBuffer.allocate(1);

    /**
     * pingMinutes, pingSeconds, and pingHundredths are fields that contains
     * the amount of time between ping groups in the ensemble.
     */
    private ByteBuffer pingHundredths = ByteBuffer.allocate(1);

    /**
     * pingMinutes, pingSeconds, and pingHundredths are fields that contains
     * the amount of time between ping groups in the ensemble.
     */
    private ByteBuffer pingMinutes = ByteBuffer.allocate(1);

    /**
     * pingMinutes, pingSeconds, and pingHundredths are fields that contains
     * the amount of time between ping groups in the ensemble.
     */
    private ByteBuffer pingSeconds = ByteBuffer.allocate(1);

    /**
     * A field that contains the number of pings averaged together
     * during a data ensemble.
     */
    private ByteBuffer pingsPerEnsemble = ByteBuffer.allocate(2);

    /**
     * A field that contains the profiling mode of the ADCP
     */
    private ByteBuffer profilingMode = ByteBuffer.allocate(1);

    /**
     * A field that contains the ending depth cell used for water
     * reference layer averaging.  See the manual for details.
     */
    private ByteBuffer referenceLayerEnd = ByteBuffer.allocate(1);

    /**
     * A field that contains the starting depth cell used for water
     * reference layer averaging.  See the manual for details.
     */
    private ByteBuffer referenceLayerStart = ByteBuffer.allocate(1);

    /**
     * A field that reflects which sensors are available.  The bit pattern
     * is the same as listed for the sensorSource field.
     */
    private ByteBuffer sensorAvailability = ByteBuffer.allocate(1);

    /**
     * A field that contains the selected source of environmental sensor data.
     * See the manual for interpreting each bit.
     */
    private ByteBuffer sensorSource = ByteBuffer.allocate(1);

    /**
     * A field that contains the instrument serial number (REMUS only),
     * and is considered a spare for all other Workhorse ADCPs.
     */
    private ByteBuffer serialNumber = ByteBuffer.allocate(4);

    /**
     * A field that contains the Signal Processing Mode.
     * This field will always be set to 1.
     */
    private ByteBuffer signalProcessingMode = ByteBuffer.allocate(1);

    /**
     * A field that contains the WB-command settings.  See the manual for details.
     */
    private ByteBuffer systemBandwidth = ByteBuffer.allocate(2);

    /**
     * A field that stores the Workhorse hardware configuration.
     * See the manual for interpreting each bit.
     */
    private ByteBuffer systemConfiguration = ByteBuffer.allocate(2);

    /**
     * A field that contains the CQ-command settings.  See the manual for details.
     */
    private ByteBuffer systemPower = ByteBuffer.allocate(1);

    /**
     * A field that contains contains the distance between pulse repetitions.
     */
    private ByteBuffer transmitLagDistance = ByteBuffer.allocate(2);

    /**
     * A field that contains the length of the transmit pulse.
     */
    private ByteBuffer transmitPulseLength = ByteBuffer.allocate(2);


    /**
     * Constructor.  This method populates the Fixed Leader fields from
     * the given ByteBuffer of data passed in as an argument, based on metadata
     * found in the EnsembleHeader.
     *
     * @param ensembleBuffer the ByteBuffer that contains the binary ensemble data
     * @param ensemble       the parent ensemble for this fixed leader
     */
    public EnsembleFixedLeader(ByteBuffer ensembleBuffer,
                               Ensemble ensemble) {

        // prepare the ensemble buffer for reading
        ensembleBuffer.flip();
        ensembleBuffer.limit(ensembleBuffer.capacity());

        // position the cursor at the correct offset given the sequential location
        // of the fixed leader in the data stream.
        int typeNumber =
            ensemble.getDataTypeNumber(EnsembleDataType.FIXED_LEADER);
        int offset = ensemble.getDataTypeOffset(typeNumber);
        ensembleBuffer.position(offset);

        // define the temporary arrays for passing bytes
        byte[] oneByte = new byte[1];
        byte[] twoBytes = new byte[2];

        // set all of the FixedLeader fields in the order that they are read from
        // the byte stream
        ensembleBuffer.get(twoBytes);
        setFixedLeaderID(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setCpuFirmwareVersion(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setCpuFirmwareRevision(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(twoBytes);
        setSystemConfiguration(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setPdRealOrSimulatedFlag(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setLagLength(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setNumberOfBeams(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setNumberOfCells(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(twoBytes);
        setPingsPerEnsemble(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(twoBytes);
        setDepthCellLength(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(twoBytes);
        setBlankAfterTransmit(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setProfilingMode(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setLowCorrelationThreshold(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setNumberOfCodeRepetitions(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setPercentGoodMinimum(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(twoBytes);
        setErrorVelocityThreshold(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setPingMinutes(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setPingSeconds(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setPingHundredths(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setCoordinateTransformParams(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(twoBytes);
        setHeadingAlignment(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(twoBytes);
        setHeadingBias(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setSensorSource(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setSensorAvailability(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(twoBytes);
        setBinOneDistance(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(twoBytes);
        setTransmitPulseLength(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setReferenceLayerStart(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setReferenceLayerEnd(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setFalseTargetThreshold(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(oneByte);
        setFixedLeaderSpare(oneByte);
        ensemble.addToByteSum(oneByte);
        ensembleBuffer.get(twoBytes);
        setTransmitLagDistance(twoBytes);
        ensemble.addToByteSum(twoBytes);
        byte[] boardSerialNumber = new byte[8];
        ensembleBuffer.get(boardSerialNumber);  // read 8 bytes
        setCpuBoardSerialNumber(boardSerialNumber);
        ensemble.addToByteSum(boardSerialNumber);
        ensembleBuffer.get(twoBytes);
        setSystemBandwidth(twoBytes);
        ensemble.addToByteSum(twoBytes);
        ensembleBuffer.get(oneByte);
        setSystemPower(oneByte);
        ensemble.addToByteSum(oneByte);

        // the following don't get called for Workhorse ADCPs
        // TODO: test for model and add fields if necessary

        //ensembleBuffer.get(oneByte);
        //setBaseFrequencyIndex(oneByte);
        //ensemble.addToByteSum(oneByte);
        //byte[] instrumentSerialNumber = new byte[4];
        //ensembleBuffer.get(instrumentSerialNumber);  // read 4 bytes
        //setSerialNumber(instrumentSerialNumber);
        //ensemble.addToByteSum(instrumentSerialNumber);
        //ensembleBuffer.get(oneByte);
        //setBeamAngle(oneByte);
        //ensemble.addToByteSum(oneByte);

    }

    /**
     * A method that returns the Ensemble baseFrequencyIndex field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getBaseFrequencyIndex() {
        this.baseFrequencyIndex.limit(this.baseFrequencyIndex.capacity());
        this.baseFrequencyIndex.position(0);
        return this.baseFrequencyIndex;
    }

    /**
     * A method that returns the Ensemble beamAngle field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getBeamAngle() {
        this.beamAngle.limit(this.beamAngle.capacity());
        this.beamAngle.position(0);
        return this.beamAngle;
    }

    /**
     * A method that returns the Ensemble binOneDistance field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getBinOneDistance() {
        this.binOneDistance.limit(this.binOneDistance.capacity());
        this.binOneDistance.position(0);
        return this.binOneDistance;
    }

    /**
     * A method that returns the Ensemble blankAfterTransmit field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getBlankAfterTransmit() {
        this.blankAfterTransmit.limit(this.blankAfterTransmit.capacity());
        this.blankAfterTransmit.position(0);
        return this.blankAfterTransmit;
    }

    /**
     * A method that returns the Ensemble coordinateTransformParams field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getCoordinateTransformParams() {
        this.coordinateTransformParams.limit(this.coordinateTransformParams.capacity());
        this.coordinateTransformParams.position(0);
        return this.coordinateTransformParams;
    }

    /**
     * A method that returns the Ensemble cpuFirmwareRevision field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getCpuFirmwareRevision() {
        this.cpuFirmwareRevision.limit(this.cpuFirmwareRevision.capacity());
        this.cpuFirmwareRevision.position(0);
        return this.cpuFirmwareRevision;
    }

    /**
     * A method that returns the Ensemble headerID field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getCpuFirmwareVersion() {
        this.cpuFirmwareVersion.limit(this.cpuFirmwareVersion.capacity());
        this.cpuFirmwareVersion.position(0);
        return this.cpuFirmwareVersion;
    }

    /**
     * A method that returns the Ensemble cpuFirmwareVersion field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getCpuBoardSerialNumber() {
        this.cpuBoardSerialNumber.limit(this.cpuBoardSerialNumber.capacity());
        this.cpuBoardSerialNumber.position(0);
        return this.cpuBoardSerialNumber;
    }

    /**
     * A method that returns the Ensemble depthCellLength field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getDepthCellLength() {
        this.depthCellLength.limit(this.depthCellLength.capacity());
        this.depthCellLength.position(0);
        return this.depthCellLength;
    }

    /**
     * A method that returns the Ensemble errorVelocityThreshold field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getErrorVelocityThreshold() {
        this.errorVelocityThreshold.limit(this.errorVelocityThreshold.capacity());
        this.errorVelocityThreshold.position(0);
        return this.errorVelocityThreshold;
    }

    /**
     * A method that returns the Ensemble falseTargetThreshold field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getFalseTargetThreshold() {
        this.falseTargetThreshold.limit(this.falseTargetThreshold.capacity());
        this.falseTargetThreshold.position(0);
        return this.falseTargetThreshold;
    }

    /**
     * A method that returns the Ensemble fixedLeaderID field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getFixedLeaderID() {
        this.fixedLeaderID.limit(this.fixedLeaderID.capacity());
        this.fixedLeaderID.position(0);
        return this.fixedLeaderID;
    }

    /**
     * A method that returns the Ensemble fixedLeaderSpare field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getFixedLeaderSpare() {
        this.fixedLeaderSpare.limit(this.fixedLeaderSpare.capacity());
        this.fixedLeaderSpare.position(0);
        return this.fixedLeaderSpare;
    }

    /**
     * A method that returns the Ensemble headingAlignment field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getHeadingAlignment() {
        this.headingAlignment.limit(this.headingAlignment.capacity());
        this.headingAlignment.position(0);
        return this.headingAlignment;
    }

    /**
     * A method that returns the Ensemble headingBias field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getHeadingBias() {
        this.headingBias.limit(this.headingBias.capacity());
        this.headingBias.position(0);
        return this.headingBias;
    }

    /**
     * A method that returns the Ensemble lagLength field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getLagLength() {
        this.lagLength.limit(this.lagLength.capacity());
        this.lagLength.position(0);
        return this.lagLength;
    }

    /**
     * A method that returns the Ensemble lowCorrelationThreshold field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getLowCorrelationThreshold() {
        this.lowCorrelationThreshold.limit(this.lowCorrelationThreshold.capacity());
        this.lowCorrelationThreshold.position(0);
        return this.lowCorrelationThreshold;
    }

    /**
     * A method that returns the Ensemble numberOfBeams field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getNumberOfBeams() {
        this.numberOfBeams.limit(this.numberOfBeams.capacity());
        this.numberOfBeams.position(0);
        return this.numberOfBeams;
    }

    /**
     * A method that returns the Ensemble numberOfCells field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getNumberOfCells() {
        this.numberOfCells.limit(this.numberOfCells.capacity());
        this.numberOfCells.position(0);
        return this.numberOfCells;
    }


    /**
     * A method that returns the Ensemble numberOfCodeRepetitions field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getNumberOfCodeRepetitions() {
        this.numberOfCodeRepetitions.limit(this.numberOfCodeRepetitions.capacity());
        this.numberOfCodeRepetitions.position(0);
        return this.numberOfCodeRepetitions;
    }

    /**
     * A method that returns the Ensemble pdRealOrSimulatedFlag field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getPdRealOrSimulatedFlag() {
        this.pdRealOrSimulatedFlag.limit(this.pdRealOrSimulatedFlag.capacity());
        this.pdRealOrSimulatedFlag.position(0);
        return this.pdRealOrSimulatedFlag;
    }

    /**
     * A method that returns the Ensemble percentGoodMinimum field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getPercentGoodMinimum() {
        this.percentGoodMinimum.limit(this.percentGoodMinimum.capacity());
        this.percentGoodMinimum.position(0);
        return this.percentGoodMinimum;
    }

    /**
     * A method that returns the Ensemble pingHundredths field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getPingHundredths() {
        this.pingHundredths.limit(this.pingHundredths.capacity());
        this.pingHundredths.position(0);
        return this.pingHundredths;
    }

    /**
     * A method that returns the Ensemble pingMinutes field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getPingMinutes() {
        this.pingMinutes.limit(this.pingMinutes.capacity());
        this.pingMinutes.position(0);
        return this.pingMinutes;
    }

    /**
     * A method that returns the Ensemble pingSeconds field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getPingSeconds() {
        this.pingSeconds.limit(this.pingSeconds.capacity());
        this.pingSeconds.position(0);
        return this.pingSeconds;
    }

    /**
     * A method that returns the Ensemble pingsPerEnsemble field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getPingsPerEnsemble() {
        this.pingsPerEnsemble.limit(this.pingsPerEnsemble.capacity());
        this.pingsPerEnsemble.position(0);
        return this.pingsPerEnsemble;
    }

    /**
     * A method that returns the Ensemble profilingMode field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getProfilingMode() {
        this.profilingMode.limit(this.profilingMode.capacity());
        this.profilingMode.position(0);
        return this.profilingMode;
    }

    /**
     * A method that returns the Ensemble referenceLayerEnd field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getReferenceLayerEnd() {
        this.referenceLayerEnd.limit(this.referenceLayerEnd.capacity());
        this.referenceLayerEnd.position(0);
        return this.referenceLayerEnd;
    }

    /**
     * A method that returns the Ensemble referenceLayerStart field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getReferenceLayerStart() {
        this.referenceLayerStart.limit(this.referenceLayerStart.capacity());
        this.referenceLayerStart.position(0);
        return this.referenceLayerStart;
    }

    /**
     * A method that returns the Ensemble sensorAvailability field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSensorAvailability() {
        this.sensorAvailability.limit(this.sensorAvailability.capacity());
        this.sensorAvailability.position(0);
        return this.sensorAvailability;
    }

    /**
     * A method that returns the Ensemble sensorSource field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSensorSource() {
        this.sensorSource.limit(this.sensorSource.capacity());
        this.sensorSource.position(0);
        return this.sensorSource;
    }

    /**
     * A method that returns the Ensemble serialNumber field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSerialNumber() {
        this.serialNumber.limit(this.serialNumber.capacity());
        this.serialNumber.position(0);
        return this.serialNumber;
    }

    /**
     * A method that returns the Ensemble signalProcessingMode field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSignalProcessingMode() {
        this.signalProcessingMode.limit(this.signalProcessingMode.capacity());
        this.signalProcessingMode.position(0);
        return this.signalProcessingMode;
    }

    /**
     * A method that returns the Ensemble systemBandwidth field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSystemBandwidth() {
        this.systemBandwidth.limit(this.systemBandwidth.capacity());
        this.systemBandwidth.position(0);
        return this.systemBandwidth;
    }

    /**
     * A method that returns the Ensemble systemConfiguration field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSystemConfiguration() {
        this.systemConfiguration.limit(this.systemConfiguration.capacity());
        this.systemConfiguration.position(0);
        return this.systemConfiguration;
    }

    /**
     * A method that returns the Ensemble systemPower field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getSystemPower() {
        this.systemPower.limit(this.systemPower.capacity());
        this.systemPower.position(0);
        return this.systemPower;
    }

    /**
     * A method that returns the Ensemble transmitLagDistance field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getTransmitLagDistance() {
        this.transmitLagDistance.limit(this.transmitLagDistance.capacity());
        this.transmitLagDistance.position(0);
        return this.transmitLagDistance;
    }

    /**
     * A method that returns the Ensemble transmitPulseLength field contents
     * as a ByteBuffer.
     */
    protected ByteBuffer getTransmitPulseLength() {
        this.transmitPulseLength.limit(this.transmitPulseLength.capacity());
        this.transmitPulseLength.position(0);
        return this.transmitPulseLength;
    }

    /**
     * A method that sets the baseFrequencyIndex field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setBaseFrequencyIndex(byte[] byteArray) {
        this.baseFrequencyIndex.put(byteArray);
    }

    /**
     * A method that sets the beamAngle field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setBeamAngle(byte[] byteArray) {
        this.beamAngle.put(byteArray);
    }

    /**
     * A method that sets the binOneDistance field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setBinOneDistance(byte[] byteArray) {
        this.binOneDistance.put(byteArray);
    }

    /**
     * A method that sets the blankAfterTransmit field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setBlankAfterTransmit(byte[] byteArray) {
        this.blankAfterTransmit.put(byteArray);
    }

    /**
     * A method that sets the coordinateTransformParams field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setCoordinateTransformParams(byte[] byteArray) {
        this.coordinateTransformParams.put(byteArray);
    }

    /**
     * A method that sets the cpuBoardSerialNumber field from the given
     * byte array. The byteArray argument must be 8-bytes in size.
     *
     * @param byteArray the 8-byte array that contains the fixed leader bytes
     */
    private void setCpuBoardSerialNumber(byte[] byteArray) {
        this.cpuBoardSerialNumber.put(byteArray);
    }

    /**
     * A method that sets the cpuFirmwareRevision field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setCpuFirmwareRevision(byte[] byteArray) {
        this.cpuFirmwareRevision.put(byteArray);
    }

    /**
     * A method that sets the cpuFirmwareVersion field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setCpuFirmwareVersion(byte[] byteArray) {
        this.cpuFirmwareVersion.put(byteArray);
    }

    /**
     * A method that sets the depthCellLength field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setDepthCellLength(byte[] byteArray) {
        this.depthCellLength.put(byteArray);
    }

    /**
     * A method that sets the errorVelocityThreshold field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setErrorVelocityThreshold(byte[] byteArray) {
        this.errorVelocityThreshold.put(byteArray);
    }

    /**
     * A method that sets the falseTargetThreshold field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setFalseTargetThreshold(byte[] byteArray) {
        this.falseTargetThreshold.put(byteArray);
    }

    /**
     * A method that sets the fixedLeaderID field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setFixedLeaderID(byte[] byteArray) {
        this.fixedLeaderID.put(byteArray);
    }

    /**
     * A method that sets the fixedLeaderSpare field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setFixedLeaderSpare(byte[] byteArray) {
        this.fixedLeaderSpare.put(byteArray);
    }

    /**
     * A method that sets the headingAlignment field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setHeadingAlignment(byte[] byteArray) {
        this.headingAlignment.put(byteArray);
    }

    /**
     * A method that sets the headingBias field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setHeadingBias(byte[] byteArray) {
        this.headingBias.put(byteArray);
    }

    /**
     * A method that sets the lagLength field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setLagLength(byte[] byteArray) {
        this.lagLength.put(byteArray);
    }

    /**
     * A method that sets the lowCorrelationThreshold field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setLowCorrelationThreshold(byte[] byteArray) {
        this.lowCorrelationThreshold.put(byteArray);
    }

    /**
     * A method that sets the numberOfBeams field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setNumberOfBeams(byte[] byteArray) {
        this.numberOfBeams.put(byteArray);
    }

    /**
     * A method that sets the numberOfCells field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setNumberOfCells(byte[] byteArray) {
        this.numberOfCells.put(byteArray);
    }

    /**
     * A method that sets the numberOfCodeRepetitions field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setNumberOfCodeRepetitions(byte[] byteArray) {
        this.numberOfCodeRepetitions.put(byteArray);
    }

    /**
     * A method that sets the pdRealOrSimulatedFlag field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setPdRealOrSimulatedFlag(byte[] byteArray) {
        this.pdRealOrSimulatedFlag.put(byteArray);
    }

    /**
     * A method that sets the percentGoodMinimum field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setPercentGoodMinimum(byte[] byteArray) {
        this.percentGoodMinimum.put(byteArray);
    }

    /**
     * A method that sets the pingHundredths field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setPingHundredths(byte[] byteArray) {
        this.pingHundredths.put(byteArray);
    }

    /**
     * A method that sets the pingMinutes field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setPingMinutes(byte[] byteArray) {
        this.pingMinutes.put(byteArray);
    }

    /**
     * A method that sets the pingSeconds field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setPingSeconds(byte[] byteArray) {
        this.pingSeconds.put(byteArray);
    }

    /**
     * A method that sets the pingsPerEnsemble field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setPingsPerEnsemble(byte[] byteArray) {
        this.pingsPerEnsemble.put(byteArray);
    }

    /**
     * A method that sets the profilingMode field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setProfilingMode(byte[] byteArray) {
        this.profilingMode.put(byteArray);
    }

    /**
     * A method that sets the referenceLayerEnd field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setReferenceLayerEnd(byte[] byteArray) {
        this.referenceLayerEnd.put(byteArray);
    }

    /**
     * A method that sets the referenceLayerStart field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setReferenceLayerStart(byte[] byteArray) {
        this.referenceLayerStart.put(byteArray);
    }

    /**
     * A method that sets the sensorAvailability field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setSensorAvailability(byte[] byteArray) {
        this.sensorAvailability.put(byteArray);
    }

    /**
     * A method that sets the sensorSource field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setSensorSource(byte[] byteArray) {
        this.sensorSource.put(byteArray);
    }

    /**
     * A method that sets the setSerialNumber field from the given
     * byte array. The byteArray argument must be 4-bytes in size.
     *
     * @param byteArray the 4-byte array that contains the fixed leader bytes
     */
    private void setSerialNumber(byte[] byteArray) {
        this.serialNumber.put(byteArray);
    }

    /**
     * A method that sets the signalProcessingMode field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setSignalProcessingMode(byte[] byteArray) {
        this.signalProcessingMode.put(byteArray);
    }

    /**
     * A method that sets the systemBandwidth field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setSystemBandwidth(byte[] byteArray) {
        this.systemBandwidth.put(byteArray);
    }

    /**
     * A method that sets the systemConfiguration field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setSystemConfiguration(byte[] byteArray) {
        this.systemConfiguration.put(byteArray);
    }

    /**
     * A method that sets the systemPower field from the given
     * byte array. The byteArray argument must be 1-byte in size.
     *
     * @param byteArray the 1-byte array that contains the fixed leader bytes
     */
    private void setSystemPower(byte[] byteArray) {
        this.systemPower.put(byteArray);
    }

    /**
     * A method that sets the transmitLagDistance field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setTransmitLagDistance(byte[] byteArray) {
        this.transmitLagDistance.put(byteArray);
    }

    /**
     * A method that sets the transmitPulseLength field from the given
     * byte array. The byteArray argument must be 2-bytes in size.
     *
     * @param byteArray the 2-byte array that contains the fixed leader bytes
     */
    private void setTransmitPulseLength(byte[] byteArray) {
        this.transmitPulseLength.put(byteArray);
    }
}