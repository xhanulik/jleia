// SPDX-FileCopyrightText: 2019 The LEIA Team <leia@ssi.gouv.fr>
// SPDX-FileCopyrightText: 2025-2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: BSD-3-Clause

/**
 * This file is derived from the SmartLEIA project
 * (https://github.com/cw-leia/smartleia),
 * originally developed by the LEIA Team and licensed under the BSD 3-Clause
 * License.
 *
 * Modifications and translation by Veronika Hanulikova.
 */

package jleia;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Command payload sent to the board to configure smartcard communication parameters.
 * Serialises to the 11-byte little-endian format expected by the firmware's 'c' command.
 */
class ConfigureSmartcardCommand extends DataStructure {
    private final byte protocol;
    private final int etu;
    private final int freq;
    private final byte negotiatePts;
    private final byte negotiateBaudrate;

    /**
     * Constructs a configure-smartcard command with the given parameters.
     *
     * @param protocol          protocol byte from {@link Protocol#value()}
     * @param etu               ETU value; 0 to let the board negotiate
     * @param freq              clock frequency in Hz; 0 to let the board negotiate
     * @param negotiatePts      whether to perform PTS negotiation
     * @param negotiateBaudrate whether to negotiate baud rate
     */
    ConfigureSmartcardCommand(
            byte protocol,
            int etu,
            int freq,
            boolean negotiatePts,
            boolean negotiateBaudrate) {
        this.protocol = protocol;
        this.etu = etu;
        this.freq = freq;
        this.negotiatePts = negotiatePts ? (byte) 1 : (byte) 0;
        this.negotiateBaudrate = negotiateBaudrate ? (byte) 1 : (byte) 0;
    }

    @Override
    public byte[] pack() {
        ByteBuffer buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(protocol);
        buffer.putInt(etu);
        buffer.putInt(freq);
        buffer.put(negotiatePts);
        buffer.put(negotiateBaudrate);
        return buffer.array();
    }

    @Override
    void unpack(byte[] data) {
        // not used — board never sends a ConfigureSmartcardCommand back
    }
}