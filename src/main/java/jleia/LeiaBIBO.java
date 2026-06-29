// SPDX-FileCopyrightText: 2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * Adapter that bridges a TargetController to the apdu4j BIBO interface,
 * allowing tools like GlobalPlatformPro to perform card I/O through the LEIA board.
 */

package jleia;

import apdu4j.BIBO;
import apdu4j.BIBOException;

import javax.smartcardio.CommandAPDU;

public class LeiaBIBO implements BIBO {
    private final TargetController targetController;

    /**
     * Constructs a {@link LeiaBIBO} wrapping the given controller.
     *
     * @param targetController an already-connected {@link TargetController} instance
     */
    public LeiaBIBO(TargetController targetController) {
        this.targetController = targetController;
    }

    /**
     * Sends a raw APDU to the card via the LEIA board and returns the raw response bytes.
     *
     * @param apduBytes raw command APDU bytes
     * @return raw response bytes including status word
     * @throws BIBOException if the APDU exchange fails
     */
    @Override
    public byte[] transceive(byte[] apduBytes) throws BIBOException {
        try {
            return targetController.sendAPDU(new CommandAPDU(apduBytes)).getBytes();
        } catch (Exception e) {
            throw new BIBOException("LEIA APDU transceive failed", e);
        }
    }

    /**
     * Closes the underlying serial connection to the LEIA board.
     */
    @Override
    public void close() {
        targetController.close();
    }
}