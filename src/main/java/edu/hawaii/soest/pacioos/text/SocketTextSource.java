/*
 * Copyright: 2013 Regents of the University of Hawaii and the
 * School of Ocean and Earth Science and Technology
 * Purpose: A class that provides properties and methods
 * for a simple instrument driver streaming data from a
 * text-based, TCP socket connection.
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

import com.rbnb.sapi.SAPIException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that provides properties and methods 
 * for a simple instrument driver streaming data from a
 * text-based, TCP socket connection.
 * @author cjones
 *
 */
public class SocketTextSource extends SimpleTextSource {

    private static final Log log = LogFactory.getLog(SocketTextSource.class);

    /* The FQDN or IP of the source instrument host */
    private String sourceHostName;

    /* The connection port of the source instrument host */
    private int sourceHostPort;

    /* The size of the ByteBuffer used to buffer the TCP stream from the instrument. */
    private int bufferSize = 8192;

    /* The state of the sample processing */
    protected int state = 0;

    /* The number of bytes in the sample as each byte is read from the stream */
    private int sampleByteCount = 0;

    /**
     * constructor: create an instance of the SerialTextSource 
     * @param xmlConfig the XML configuration
     * @throws ConfigurationException a configuration exception
     */
    public SocketTextSource(XMLConfiguration xmlConfig) throws ConfigurationException {
        super(xmlConfig);

    }

    /* (non-Javadoc)
     * @see edu.hawaii.soest.pacioos.text.SimpleTextSource#execute()
     */
    @Override
    protected boolean execute() {

        log.debug("SocketTextSource.execute() called.");
        // do not execute the stream if there is no connection
        if (!isConnected()) return false;

        /* Get a connection to the instrument */
        SocketChannel socket = getSocketConnection();
        if (socket == null) {
            log.info("Couldn't get socket connection to the remote instrument host: " +
                getHostName());
            return true;
        }

        // while data are being sent, read them into the buffer
        try {
            // create four byte placeholders used to evaluate up to a four-byte
            // window.  The FIFO layout looks like:
            //           -------------------------
            //   in ---> | One | Two |Three|Four |  ---> out
            //           -------------------------
            byte byteOne = 0x00,   // set initial placeholder values
                byteTwo = 0x00,
                byteThree = 0x00,
                byteFour = 0x00;

            // Create a buffer that will store the sample bytes as they are read
            ByteBuffer sampleBuffer = ByteBuffer.allocate(getBufferSize());

            // create a byte buffer to store bytes from the TCP stream
            ByteBuffer buffer = ByteBuffer.allocateDirect(getBufferSize());

            // while there are bytes to read from the socket ...
            while (socket.read(buffer) != -1 || buffer.position() > 0) {

                // prepare the buffer for reading
                buffer.flip();

                // while there are unread bytes in the ByteBuffer
                while (buffer.hasRemaining()) {
                    byteOne = buffer.get();

                    // log the byte stream
                    String character = new String(new byte[]{byteOne});
                    if (log.isTraceEnabled()) {
                        List<Byte> whitespaceBytes = new ArrayList<Byte>();
                        whitespaceBytes.add(new Byte((byte) 0x0A));
                        whitespaceBytes.add(new Byte((byte) 0x0D));
                        if (whitespaceBytes.contains(new Byte(byteOne))) {
                            character = new String(Hex.encodeHex((new byte[]{byteOne})));

                        }
                        log.trace("char: " + character + "\t" +
                            "b1: " + new String(Hex.encodeHex((new byte[]{byteOne}))) + "\t" +
                            "b2: " + new String(Hex.encodeHex((new byte[]{byteTwo}))) + "\t" +
                            "b3: " + new String(Hex.encodeHex((new byte[]{byteThree}))) + "\t" +
                            "b4: " + new String(Hex.encodeHex((new byte[]{byteFour}))) + "\t" +
                            "sample pos: " + sampleBuffer.position() + "\t" +
                            "sample rem: " + sampleBuffer.remaining() + "\t" +
                            "sample cnt: " + sampleByteCount + "\t" +
                            "buffer pos: " + buffer.position() + "\t" +
                            "buffer rem: " + buffer.remaining() + "\t" +
                            "state: " + state);
                    }

                    // evaluate each byte to find the record delimiter(s), and when found, validate and
                    // send the sample to the DataTurbine.
                    int numberOfChannelsFlushed = 0;

                    if (getRecordDelimiters().length == 2) {
                        // have we hit the delimiters in the stream yet?
                        if (byteTwo == getFirstDelimiterByte() &&
                            byteOne == getSecondDelimiterByte()) {
                            sampleBuffer.put(byteOne);
                            sampleByteCount++;
                            // extract just the length of the sample bytes out of the
                            // sample buffer, and place it in the channel map as a
                            // byte array.  Then, send it to the DataTurbine.
                            log.debug("Sample byte count: " + sampleByteCount);
                            byte[] sampleArray = new byte[sampleByteCount];
                            sampleBuffer.flip();
                            sampleBuffer.get(sampleArray);
                            String sampleString = new String(sampleArray, "US-ASCII");

                            if (validateSample(sampleString)) {
                                numberOfChannelsFlushed = sendSample(sampleString);

                            }

                            sampleBuffer.clear();
                            sampleByteCount = 0;
                            byteOne = 0x00;
                            byteTwo = 0x00;
                            byteThree = 0x00;
                            byteFour = 0x00;
                            log.debug("Cleared b1,b2,b3,b4. Cleared sampleBuffer. Cleared rbnbChannelMap.");

                        } else {
                            // still in the middle of the sample, keep adding bytes
                            sampleByteCount++; // add each byte found

                            if (sampleBuffer.remaining() > 0) {
                                sampleBuffer.put(byteOne);

                            } else {
                                sampleBuffer.compact();
                                log.debug("Compacting sampleBuffer ...");
                                sampleBuffer.put(byteOne);

                            }

                        }

                    } else if (getRecordDelimiters().length == 1) {
                        // have we hit the delimiter in the stream yet?
                        if (byteOne == getFirstDelimiterByte()) {
                            sampleBuffer.put(byteOne);
                            sampleByteCount++;
                            // extract just the length of the sample bytes out of the
                            // sample buffer, and place it in the channel map as a
                            // byte array.  Then, send it to the DataTurbine.
                            byte[] sampleArray = new byte[sampleByteCount];
                            sampleBuffer.flip();
                            sampleBuffer.get(sampleArray);
                            String sampleString = new String(sampleArray, StandardCharsets.US_ASCII);

                            if (validateSample(sampleString)) {
                                numberOfChannelsFlushed = sendSample(sampleString);

                            }

                            sampleBuffer.clear();
                            sampleByteCount = 0;
                            byteOne = 0x00;
                            byteTwo = 0x00;
                            byteThree = 0x00;
                            byteFour = 0x00;
                            log.debug("Cleared b1,b2,b3,b4. Cleared sampleBuffer. Cleared rbnbChannelMap.");

                        } else {
                            // still in the middle of the sample, keep adding bytes
                            sampleByteCount++; // add each byte found

                            if (sampleBuffer.remaining() > 0) {
                                sampleBuffer.put(byteOne);

                            } else {
                                sampleBuffer.compact();
                                log.debug("Compacting sampleBuffer ...");
                                sampleBuffer.put(byteOne);

                            }

                        }

                    } // end getRecordDelimiters().length

                    // shift the bytes in the FIFO window
                    byteFour = byteThree;
                    byteThree = byteTwo;
                    byteTwo = byteOne;

                } //end while (more unread bytes)

                // prepare the buffer to read in more bytes from the stream
                buffer.compact();


            } // end while (more socket bytes to read)
            // socket.close();

        } catch (IOException e) {
            // handle exceptions
            // In the event of an i/o exception, log the exception, and allow execute()
            // to return false, which will prompt a retry.
            log.error("There was a communication error in sending the data sample. The message was: " +
                e.getMessage());
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
            return true;

        } catch (SAPIException sapie) {
            // In the event of an RBNB communication  exception, log the exception, 
            // and allow execute() to return false, which will prompt a retry.
            log.error("There was an RBNB error while sending the data sample. The message was: " +
                sapie.getMessage());
            if (log.isDebugEnabled()) {
                sapie.printStackTrace();
            }
            return true;
        }
        return false;

    } // end if (  !isConnected() )

    /* (non-Javadoc)
     * @see org.nees.rbnb.RBNBBase#setArgs(org.apache.commons.cli.CommandLine)
     */
    @Override
    protected boolean setArgs(CommandLine cmd) {
        // TODO: Set fields as needed

        return false;
    }

    /**
     * Return the name (FQDN) or IP of the instrument host
     *
     * @return sourceHostName  the name or IP of the instrument host 
     */
    public String getHostName() {
        return sourceHostName;

    }

    /**
     * Set the name (FQDN) or IP of the instrument host
     *
     * @param hostName  the name or IP of the instrument host
     */
    public void setHostName(String hostName) {
        this.sourceHostName = hostName;

    }

    /**
     * Return the connection port of the instrument host
     *
     * @return sourceHostPort  the connection port of the instrument host 
     */
    public int getHostPort() {
        return sourceHostPort;

    }

    /**
     * Set the connection port of the instrument host
     *
     * @param hostPort  the connection port of the instrument host
     */
    public void setHostPort(int hostPort) {
        this.sourceHostPort = hostPort;

    }

    /**
     * A method used to the TCP socket of the remote source host for communication
     *
     * @return dataSocket the socket channel for the connection
     */
    protected SocketChannel getSocketConnection() {

        String host = getHostName();
        int portNumber = getHostPort();
        SocketChannel dataSocket = null;

        try {

            // create the socket channel connection to the data source via the
            // converter serial2IP converter
            dataSocket = SocketChannel.open();
            dataSocket.connect(new InetSocketAddress(host, portNumber));

            // if the connection to the source fails, close the socket
            if (!dataSocket.isConnected()) {
                dataSocket.close();
                dataSocket = null;
            }
        } catch (UnknownHostException ukhe) {

            log.info("Unable to look up host: " + host + "\n");
            disconnect();
            dataSocket = null;

        } catch (IOException nioe) {
            log.info("Couldn't get I/O connection to: " + host + ":" + portNumber);
            disconnect();
            dataSocket = null;
        }
        return dataSocket;
    }

    /**
     * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
     * data from a source instrument via a TCP connection
     */
    public int getBufferSize() {
        return this.bufferSize;
    }

    /**
     * A method that sets the size, in bytes, of the ByteBuffer used in streaming
     * data from a source instrument via a TCP connection
     *
     * @param bufferSize  the size, in bytes, of the ByteBuffer
     */
    public void setBuffersize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
