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
import java.util.Arrays;

public class LeiaBIBO implements BIBO {
    private final TargetController targetController;
    private final byte[] atrBytes;
    private final byte protocolByte;

    // apdu4j pseudo-APDUs: answered locally, never forwarded to the physical card
    private static final byte[] GET_ATR      = {(byte)0xFF, (byte)0xCA, 0x10, 0x00, 0x00};
    private static final byte[] GET_PROTOCOL = {(byte)0xFF, (byte)0xCA, 0x11, 0x00, 0x00};

    /**
     * Constructs a {@link LeiaBIBO} wrapping the given controller.
     * Reads the ATR once at construction time to satisfy apdu4j pseudo-APDU queries.
     *
     * @param targetController an already-connected {@link TargetController} instance
     */
    public LeiaBIBO(TargetController targetController) {
        this.targetController = targetController;
        ATR atr = targetController.getATR();
        this.atrBytes = parseHex(atr.normalized());
        // ATR.getProtocol(): 1 = T=0, 2 = T=1; CardBIBO returns 0x00 for T=0, 0x01 for T=1
        this.protocolByte = (byte) (atr.getProtocol() - 1);
    }

    /**
     * Sends a raw APDU to the card via the LEIA board and returns the raw response bytes.
     * apdu4j pseudo-APDUs (CLA=0xFF) are answered locally without forwarding to the card.
     *
     * @param apduBytes raw command APDU bytes
     * @return raw response bytes including status word
     * @throws BIBOException if the APDU exchange fails
     */
    @Override
    public byte[] transceive(byte[] apduBytes) throws BIBOException {
        if (Arrays.equals(apduBytes, GET_ATR)) {
            byte[] resp = Arrays.copyOf(atrBytes, atrBytes.length + 2);
            resp[atrBytes.length]     = (byte) 0x90;
            resp[atrBytes.length + 1] = 0x00;
            return resp;
        }
        if (Arrays.equals(apduBytes, GET_PROTOCOL)) {
            return new byte[]{protocolByte, (byte) 0x90, 0x00};
        }
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

    private static byte[] parseHex(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }
}
