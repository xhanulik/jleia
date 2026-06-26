// SPDX-FileCopyrightText: 2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: BSD-3-Clause

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

    public LeiaBIBO(TargetController targetController) {
        this.targetController = targetController;
    }

    @Override
    public byte[] transceive(byte[] apduBytes) throws BIBOException {
        try {
            return targetController.sendAPDU(new CommandAPDU(apduBytes)).getBytes();
        } catch (Exception e) {
            throw new BIBOException("LEIA APDU transceive failed", e);
        }
    }

    @Override
    public void close() {
        targetController.close();
    }
}