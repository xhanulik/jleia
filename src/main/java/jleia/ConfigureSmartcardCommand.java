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

class ConfigureSmartcardCommand extends DataStructure {
    private final byte protocol;
    private final int etu;
    private final int freq;
    private final byte negotiatePts;
    private final byte negotiateBaudrate;

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