// SPDX-FileCopyrightText: 2019 The LEIA Team <leia@ssi.gouv.fr>
// SPDX-FileCopyrightText: 2025-2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * This file is derived from the SmartLEIA project
 * (https://github.com/cw-leia/smartleia),
 * originally developed by the LEIA Team. Licensed under LGPL-2.1-or-later.
 *
 * Modifications and translation by Veronika Hanulikova.
 */

package jleia;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Response structure returned by the board after an APDU exchange.
 * Contains the card's response data, status words, and timing measurements.
 */
class RESP extends DataStructure {
    private int le;
    private byte sw1;
    private byte sw2;
    private int deltaT;
    private int deltaTAnswer;
    private byte[] data;

    /** Total APDU round-trip time measured by the board, in microseconds. */
    public int getDeltaT() {
        return deltaT;
    }

    /** Time from first byte sent to first byte of response, in microseconds. */
    public int getDeltaTAnswer() {
        return deltaTAnswer;
    }

    /**
     * Serialises this response into the board's binary format.
     *
     * @return packed byte array
     */
    @Override
    public byte[] pack() {
        ByteBuffer buffer = ByteBuffer.allocate(14 + this.data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(this.le);
        buffer.put(this.sw1);
        buffer.put(this.sw2);
        buffer.putInt(this.deltaT);
        buffer.putInt(this.deltaTAnswer);
        buffer.put(this.data, 0, this.data.length);
        return buffer.array();
    }

    /**
     * Deserialises a binary payload from the board into this response's fields.
     *
     * @param data raw bytes from the board (minimum 14 bytes)
     */
    @Override
    void unpack(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.le = buffer.getInt();
        this.sw1 = buffer.get();
        this.sw2 = buffer.get();
        this.deltaT = buffer.getInt();
        this.deltaTAnswer = buffer.getInt();
        this.data = Arrays.copyOfRange(data, 14, data.length);
    }

    /**
     * Returns the card's response as a flat byte array suitable for constructing a
     * {@link javax.smartcardio.ResponseAPDU}: response data followed by SW1 and SW2.
     *
     * @return response data bytes concatenated with the two status bytes
     */
    public byte[] toArray() {
        byte[] result = new byte[data.length + 2];
        System.arraycopy(data, 0, result, 0, data.length);
        result[data.length] = sw1;
        result[data.length + 1] = sw2;
        return result;
    }
}