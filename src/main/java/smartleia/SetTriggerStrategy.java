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

package smartleia;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class SetTriggerStrategy extends DataStructure {
    // TRIG_PRE_SEND_APDU = TRIG_PRE_SEND_APDU_SHORT_T0 | TRIG_PRE_SEND_APDU_FRAGMENTED_T0 | TRIG_PRE_SEND_APDU_T1
    private static final int TRIG_PRE_SEND_APDU = 0x1C;
    private static final int TRIGGER_DEPTH = 10;
    // index(1) + size(1) + delay(4) + single(1) + 5 arrays × (TRIGGER_DEPTH × 4) = 207 bytes
    private static final int PACKED_SIZE = 1 + 1 + 4 + 1 + 5 * TRIGGER_DEPTH * 4;

    private final boolean reset;

    public SetTriggerStrategy(boolean toReset) {
        this.reset = toReset;
    }

    @Override
    public byte[] pack() {
        ByteBuffer buf = ByteBuffer.allocate(PACKED_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1);                           // strategy bank index
        buf.put(reset ? (byte) 0 : (byte) 1);        // size: number of trigger points
        buf.putInt(0);                               // delay = 0 ms
        buf.put((byte) 0);                           // single = 0
        buf.putInt(reset ? 0 : TRIG_PRE_SEND_APDU); // _list[0]
        // remaining _list entries + _list_trigged + _cnt_trigged + _event_time + _apply_delay are zero
        return buf.array();
    }

    @Override
    void unpack(byte[] buffer) {
        // FIXME: Implement missing method
    }
}