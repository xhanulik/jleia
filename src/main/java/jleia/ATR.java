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
import java.util.ArrayList;

public class ATR extends DataStructure {
    private byte ts = 0;
    private byte t0 = 0;
    private final byte[] ta = new byte[4];
    private final byte[] tb = new byte[4];
    private final byte[] tc = new byte[4];
    private final byte[] td = new byte[4];
    private final byte[] h = new byte[16];
    private final byte[] tMask = new byte[4];
    private byte hNum = 0;
    private byte tck = 0;
    private byte tckPresent = 0;
    private int dICurr = 0;
    private int fICurr = 0;
    private int fMaxCurr = 0;
    private byte tProtocolCurr = 0;
    private byte ifsc = 0;

    /** Returns the negotiated protocol number (1 = T=0, 2 = T=1). */
    public int getProtocol() {
        return tProtocolCurr & 0xFF;
    }

    /** Returns the maximum ISO 7816 clock frequency supported by the card, in Hz. */
    public int getMaxFrequencyHz() {
        return fMaxCurr;
    }

    /**
     * Reconstructs the standard ISO 7816 ATR byte sequence from the parsed fields,
     * mirroring the Python smartleia ATR.normalized() method.
     *
     * @return hex string of the ATR bytes
     */
    public String normalized() {
        ArrayList<Byte> atr = new ArrayList<>();
        atr.add(ts);
        atr.add(t0);
        // interface bytes: tMask[j] bit i set means TAi+1/TBi+1/TCi+1/TDi+1 present
        byte[][] groups = { ta, tb, tc, td };
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if ((tMask[j] & (1 << i)) != 0) {
                    atr.add(groups[i][j]);
                }
            }
        }
        for (int i = 0; i < (hNum & 0xFF); i++) {
            atr.add(h[i]);
        }
        if (tckPresent != 0) {
            atr.add(tck);
        }
        byte[] bytes = new byte[atr.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = atr.get(i);
        }
        return bytesToHex(bytes);
    }

    @Override
    byte[] pack() {
        // FIXME: Implement missing method
        return new byte[0];
    }

    @Override
    void unpack(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.ts = buffer.get();
        this.t0 = buffer.get();
        buffer.get(this.ta);
        buffer.get(this.tb);
        buffer.get(this.tc);
        buffer.get(this.td);
        buffer.get(this.h);
        buffer.get(this.tMask);
        this.hNum = buffer.get();
        this.tck = buffer.get();
        this.tckPresent = buffer.get();
        this.dICurr = buffer.getInt();
        this.fICurr = buffer.getInt();
        this.fMaxCurr = buffer.getInt();
        this.tProtocolCurr = buffer.get();
        this.ifsc = buffer.get();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02X", b));
        return sb.toString();
    }
}