// SPDX-FileCopyrightText: 2019 The LEIA Team <leia@ssi.gouv.fr>
// SPDX-FileCopyrightText: 2025-2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: BSD-3-Clause

/**
 * This file is a derivative work based on code from the SmartLEIA project,
 * originally developed by the LEIA Team (https://github.com/cw-leia/smartleia),
 * and licensed under the BSD 3-Clause License.
 *
 * Original code licensed under the BSD 3-Clause License:
 * Copyright (c) 2019, The LEIA Team <leia@ssi.gouv.fr>
 *
 * Modifications and translation by Veronika Hanulikova.
 */

package smartleia;

import com.fazecast.jSerialComm.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class TargetController {
    private static final int RESPONSE_LEN_SIZE = 4;
    private static final int COMMAND_LEN_SIZE = 4;
    private static final int USB_VID = 0x3483;
    private static final int USB_PID = 0x0BB9;

    private SerialPort serialPort = null;
    private long lastTransmitTimeNano = 0L;

    private final Object lock = new Object();

    /**
     * Try to detect connected LEIA board and open serial port for communication.
     *
     * @return true if successfully opened, false if no board found (caller may retry)
     * @implNote LEIA.open, originally implemented in
     *           https://github.com/cw-leia/smartleia/blob/master/smartleia/__init__.py
     */
    public boolean open() {
        // Collect only matching ports first, mirroring Python's possible_ports list
        SerialPort[] all = SerialPort.getCommPorts();
        java.util.List<SerialPort> candidates = new java.util.ArrayList<>();
        for (SerialPort port : all) {
            if (port.getVendorID() == USB_VID && port.getProductID() == USB_PID) {
                candidates.add(port);
            }
        }

        if (candidates.isEmpty()) {
            return false;
        }
        if (candidates.size() > 2) {
            throw new RuntimeException(String.format(
                    "Too many %04X/%04X devices found — cannot determine which one to use.", USB_VID, USB_PID));
        }

        for (SerialPort port : candidates) {
            try {
                port.setBaudRate(115200);
                // TIMEOUT_WRITE_BLOCKING may not work correctly on non-Windows platforms.
                port.setComPortTimeouts(
                        SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                        1000, 0);
                if (!port.openPort()) {
                    continue;
                }
                serialPort = port;
                System.out.printf("Serial port %s (%04X/%04X) is open and ready%n",
                        serialPort.getDescriptivePortName(), USB_VID, USB_PID);

                drainBuffer();
                testWaitingFlag();

                port.setComPortTimeouts(
                        SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                        10000, 0);
                return true;
            } catch (Exception e) {
                port.closePort();
            }
        }

        serialPort = null;
        return false;
    }

    private void isValidPort() {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new RuntimeException("No serial connection created!");
        }
    }

    /**
     * Read whatever bytes are currently available in the receive buffer (single call).
     * Returns an empty array when the buffer is empty.
     */
    private byte[] readAvailableBytes() {
        isValidPort();
        int available = serialPort.bytesAvailable();
        if (available <= 0) {
            return new byte[0];
        }
        byte[] buffer = new byte[available];
        serialPort.readBytes(buffer, available, 0);
        return buffer;
    }

    /**
     * Drain the receive buffer completely.
     */
    private void drainBuffer() {
        isValidPort();
        while (true) {
            byte[] chunk = readAvailableBytes();
            if (chunk.length == 0) {
                break;
            }
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verify the board is in the 'waiting' state before issuing a command.
     *
     * @implNote LEIA._testWaitingFlag
     */
    private void testWaitingFlag() {
        isValidPort();
        drainBuffer();

        byte[] command = new byte[] { ' ' }; // b" "
        serialPort.writeBytes(command, command.length, 0);
        sleep(100); // Wait for 0.1s

        // Read 1 + all available bytes
        byte[] singleByte = new byte[1];
        int bytesRead = serialPort.readBytes(singleByte, 1);
        byte[] allBytes = readAvailableBytes();
        if (bytesRead == 0 && allBytes.length == 0)
            throw new RuntimeException("Expected waiting flag");

        // Combine
        byte[] buffer = new byte[1 + allBytes.length];
        buffer[0] = singleByte[0];
        System.arraycopy(allBytes, 0, buffer, 1, allBytes.length);

        if (buffer[buffer.length - 1] != 87) { // "W"
            throw new RuntimeException("Cannot connect to LEIA.");
        }
    }

    /**
     * Read and validate the status byte(s) returned after every command.
     *
     * @implNote LEIA._checkStatus
     */
    private void checkStatus() {
        isValidPort();
        byte[] status = new byte[1];

        // FIXME: Raise IOError on missing status; here we only log and continue,
        //        which means subsequent reads may silently parse garbage data.
        int read = serialPort.readBytes(status, 1);
        if (read == 0) {
            System.err.println("checkStatus: no status flag received.");
        }

        while (status[0] == 'w') {
            read = serialPort.readBytes(status, 1);
            if (read == 0) {
                System.err.println("checkStatus: no status flag received (wait extension).");
            }
        }

        // FIXME: Python maps non-zero secondary status codes to named error strings
        //        via ERR_FLAGS dict; here we only print a generic message.
        if (status[0] == 'U') {
            System.err.println("checkStatus: firmware does not handle this command ('U').");
        } else if (status[0] == 'E') {
            System.err.println("checkStatus: unknown firmware error ('E').");
        } else if (status[0] != 'S') {
            System.err.printf("checkStatus: unexpected status flag '%c'.%n", (char) status[0]);
        }

        read = serialPort.readBytes(status, 1);
        if (read == 0) {
            System.err.println("checkStatus: secondary status byte not received.");
        } else if (status[0] != 0x00) {
            System.err.printf("checkStatus: error status 0x%02X.%n", status[0]);
        }
    }

    /**
     * Read and validate the acknowledge byte returned after every command.
     *
     * @implNote LEIA._checkAck
     */
    private void checkAck() {
        isValidPort();
        byte[] ack = new byte[1];
        int read = serialPort.readBytes(ack, 1);
        if (read == 0 || ack[0] != 'R') {
            throw new RuntimeException("checkAck: no acknowledge 'R' received.");
        }
    }

    /**
     * Send a command byte and optional payload to the board, then wait for
     * status and acknowledge.
     *
     * @param command 1-byte command identifier
     * @param struct  payload to pack, or {@code null} for a zero-length payload
     * @implNote LEIA._send_command
     */
    private void sendCommand(byte[] command, DataStructure struct) {
        isValidPort();
        testWaitingFlag();
        serialPort.writeBytes(command, command.length, 0);

        if (struct == null) {
            byte[] zero = new byte[COMMAND_LEN_SIZE];
            serialPort.writeBytes(zero, zero.length, 0);
        } else {
            byte[] payload = struct.pack();
            // Length prefix is big-endian (matches Python byteorder="big")
            byte[] size = ByteBuffer.allocate(COMMAND_LEN_SIZE).putInt(payload.length).array();
            serialPort.writeBytes(size, size.length, 0);
            serialPort.writeBytes(payload, payload.length, 0);
        }

        checkStatus();
        checkAck();
    }

    /**
     * Read the 4-byte little-endian response-length prefix.
     *
     * @implNote LEIA._read_response_size
     */
    private int readResponseSize() {
        isValidPort();
        byte[] buf = new byte[RESPONSE_LEN_SIZE];
        int read = serialPort.readBytes(buf, RESPONSE_LEN_SIZE);
        if (read != RESPONSE_LEN_SIZE) {
            throw new RuntimeException("readResponseSize: expected 4 bytes, got " + read);
        }
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Tests whether a smartcard is currently inserted into the LEIA board.
     *
     * @return {@code true} if a card is present
     * @implNote LEIA.is_card_inserted
     */
    public boolean isCardInserted() {
        isValidPort();
        byte[] response;
        synchronized (lock) {
            sendCommand("?".getBytes(), null);
            int size = readResponseSize();
            if (size != 1) {
                throw new RuntimeException("isCardInserted: unexpected response size " + size);
            }
            response = new byte[1];
            serialPort.readBytes(response, 1);
        }
        return response[0] == 1;
    }

    /**
     * Configures the smartcard communication parameters.
     *
     * @param protocolToUse T0 or T1; {@code null} defaults to T1
     * @param ETUToUse      ETU value; 0 to let the board negotiate
     * @param freqToUse     frequency in Hz; 0 to let the board negotiate
     * @param negotiatePts  whether to perform PTS negotiation
     * @param negotiateBaudrate whether to negotiate baud rate
     * @implNote LEIA.configure_smartcard
     */
    public void configureSmartcard(Protocol protocolToUse,
                                   int ETUToUse, int freqToUse,
                                   boolean negotiatePts, boolean negotiateBaudrate) {
        isValidPort();
        if (!isCardInserted()) {
            throw new RuntimeException("configureSmartcard: no card inserted.");
        }
        if (protocolToUse == null) {
            protocolToUse = Protocol.T1;
        }
        // FIXME: Python configure_smartcard() retries with AUTO protocol when
        //        explicit PTS negotiation fails; this implementation does not retry.
        if (protocolToUse == Protocol.AUTO) {
            throw new IllegalArgumentException("Protocol.AUTO is not supported; specify T0 or T1 explicitly.");
        }

        synchronized (lock) {
            testWaitingFlag();
            try {
                ConfigureSmartcardCommand cmd = new ConfigureSmartcardCommand(
                        protocolToUse.value(), ETUToUse, freqToUse, negotiatePts, negotiateBaudrate);
                sendCommand("c".getBytes(), cmd);
            } catch (Exception e) {
                throw new RuntimeException("configureSmartcard failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Reads the Answer-to-Reset from the card.
     *
     * @return parsed {@link ATR} structure
     * @implNote LEIA.get_ATR
     */
    public ATR getATR() {
        isValidPort();
        ATR atr = new ATR();
        synchronized (lock) {
            sendCommand("t".getBytes(), null);
            int size = readResponseSize();
            if (size != 55) {
                throw new RuntimeException("getATR: unexpected response size " + size + " (expected 55).");
            }
            byte[] response = new byte[55];
            serialPort.readBytes(response, 55);
            atr.unpack(response);
        }
        return atr;
    }

    /**
     * Resets all trigger strategies to none (no trigger).
     *
     * @implNote LEIA.set_trigger_strategy(1, point_list=[], delay=0)
     */
    public void resetTriggerStrategy() {
        // FIXME: Python set_trigger_strategy() accepts an arbitrary SID (strategy bank 0-3),
        //        point_list, delay, and single parameters. This always uses bank 1 with
        //        an empty point list; see SetTriggerStrategy for the full list of limitations.
        isValidPort();
        synchronized (lock) {
            sendCommand("O".getBytes(), new SetTriggerStrategy(true));
        }
    }

    /**
     * Activates the pre-send-APDU trigger strategy on bank 1.
     *
     * @implNote LEIA.set_trigger_strategy(1, point_list=[TriggerPoints.TRIG_PRE_SEND_APDU], delay=0)
     */
    public void setPreSendAPDUTriggerStrategy() {
        // FIXME: see resetTriggerStrategy() — same limitations apply.
        isValidPort();
        synchronized (lock) {
            sendCommand("O".getBytes(), new SetTriggerStrategy(false));
        }
    }

    /**
     * Sends a command APDU to the card and returns the response.
     * The board-measured round-trip time is available via {@link #getLastTransmitTimeNano()}.
     *
     * @param commandApdu the APDU to transmit
     * @return the card's response
     * @implNote LEIA.send_APDU
     */
    public ResponseAPDU sendAPDU(CommandAPDU commandApdu) {
        isValidPort();
        APDU apdu = new APDU(
                (byte) commandApdu.getCLA(), (byte) commandApdu.getINS(),
                (byte) commandApdu.getP1(), (byte) commandApdu.getP2(),
                commandApdu.getData());
        ResponseAPDU responseApdu;
        synchronized (lock) {
            sendCommand("a".getBytes(), apdu);
            int size = readResponseSize();
            if (size < 14) {
                throw new RuntimeException("sendAPDU: response too short (" + size + " bytes).");
            }
            byte[] raw = new byte[size];
            serialPort.readBytes(raw, size);
            RESP resp = new RESP();
            resp.unpack(raw);
            // FIXME: Python send_APDU() returns the full RESP object including deltaT and
            //        deltaTAnswer timing fields. This returns only a ResponseAPDU, discarding
            //        deltaTAnswer. deltaT is preserved via getLastTransmitTimeNano().
            lastTransmitTimeNano = (resp.getDeltaT() & 0xFFFFFFFFL) * 1000L;
            responseApdu = new ResponseAPDU(resp.toArray());
        }
        return responseApdu;
    }

    /**
     * Returns the total APDU round-trip time measured by the board during the
     * last {@link #sendAPDU(CommandAPDU)} call, in nanoseconds.
     *
     * <p>The board measures in microseconds; this method converts to nanoseconds
     * for consistency with {@code javax.smartcardio} timing conventions.</p>
     *
     * @return nanoseconds elapsed, or 0 if no APDU has been sent yet
     */
    public long getLastTransmitTimeNano() {
        return lastTransmitTimeNano;
    }

    /**
     * Closes the serial port connection to the LEIA board.
     *
     * @implNote LEIA.close (via serial.Serial.close)
     */
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            System.out.printf("Closing serial port %s (%04X/%04X)%n",
                    serialPort.getDescriptivePortName(), USB_VID, USB_PID);
            serialPort.closePort();
            serialPort = null;
        }
    }
}