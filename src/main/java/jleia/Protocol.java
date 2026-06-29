// SPDX-FileCopyrightText: 2019 The LEIA Team <leia@ssi.gouv.fr>
// SPDX-FileCopyrightText: 2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * This file is derived from the SmartLEIA project
 * (https://github.com/cw-leia/smartleia),
 * originally developed by the LEIA Team. Licensed under LGPL-2.1-or-later.
 *
 * Modifications and translation by Veronika Hanulikova.
 */

package jleia;

/** ISO 7816 communication protocol to use when configuring the smartcard. */
public enum Protocol {
    /** Let the board auto-detect the protocol (not currently supported by the driver). */
    AUTO((byte) 0),
    /** ISO 7816 T=0 (character-oriented). */
    T0((byte) 1),
    /** ISO 7816 T=1 (block-oriented). */
    T1((byte) 2);

    private final byte value;

    Protocol(byte value) {
        this.value = value;
    }

    /**
     * Returns the byte value sent to the board firmware for this protocol.
     *
     * @return firmware protocol byte
     */
    byte value() {
        return value;
    }
}