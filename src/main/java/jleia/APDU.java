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
 * ISO 7816 command APDU payload sent to the LEIA board.
 * Serialises the APDU fields into the little-endian binary format expected by the board firmware.
 */
class APDU extends DataStructure {

    private final byte cla;
    private final byte ins;
    private final byte p1;
    private final byte p2;
    private final short lc;
    private final int le;
    private final byte sendLe;
    private final byte[] data;

    private static final int MAX_APDU_PAYLOAD_SIZE = 16384;

    /**
     * Constructs a command APDU with the given header bytes, optional data field, and expected
     * response length. Data longer than {@code MAX_APDU_PAYLOAD_SIZE} is silently truncated.
     *
     * @param cla  class byte
     * @param ins  instruction byte
     * @param p1   parameter 1
     * @param p2   parameter 2
     * @param data command data field, or {@code null} for no data
     * @param ne   expected response length from {@code CommandAPDU.getNe()}; 0 means no Le field
     */
    public APDU(byte cla, byte ins, byte p1, byte p2, byte[] data, int ne) {
        this.cla = cla;
        this.ins = ins;
        this.p1 = p1;
        this.p2 = p2;
        this.le = ne;
        this.sendLe = (byte) (ne > 0 ? 1 : 0);

        if (data == null) {
            this.data = new byte[0];
            this.lc = 0;
        } else {
            this.data = Arrays.copyOf(data, Math.min(data.length, MAX_APDU_PAYLOAD_SIZE));
            this.lc = (short) data.length;
        }
    }

    /**
     * Constructs a command APDU with no Le field.
     *
     * @param cla  class byte
     * @param ins  instruction byte
     * @param p1   parameter 1
     * @param p2   parameter 2
     * @param data command data field, or {@code null} for no data
     */
    public APDU(byte cla, byte ins, byte p1, byte p2, byte[] data) {
        this(cla, ins, p1, p2, data, 0);
    }

    /**
     * Serialises this APDU into the binary format expected by the board firmware.
     *
     * @return packed byte array
     */
    @Override
    public byte[] pack() {
        ByteBuffer buffer = ByteBuffer.allocate(11 + this.lc).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(this.cla);
        buffer.put(this.ins);
        buffer.put(this.p1);
        buffer.put(this.p2);
        buffer.putShort(this.lc);
        buffer.putInt(this.le);
        buffer.put(this.sendLe);
        buffer.put(this.data, 0, this.lc);
        return buffer.array();
    }

    @Override
    void unpack(byte[] buffer) {
        // FIXME: Implement missing method
    }
}
