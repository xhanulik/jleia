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

/**
 * Base class for all binary structures exchanged with the LEIA board firmware.
 * Subclasses implement {@link #pack()} for host-to-board serialisation and
 * {@link #unpack(byte[])} for board-to-host deserialisation.
 */
abstract class DataStructure {

    /**
     * Serialises this structure into the binary format expected by the board.
     *
     * @return packed byte array
     */
    abstract byte[] pack();

    /**
     * Deserialises a binary payload received from the board into this structure's fields.
     *
     * @param buffer raw bytes from the board
     */
    abstract void unpack(byte[] buffer);
}