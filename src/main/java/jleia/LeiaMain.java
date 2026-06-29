// SPDX-FileCopyrightText: 2026 Veronika Hanulikova <xhanulik@gmail.com>
// SPDX-License-Identifier: LGPL-2.1-or-later

package jleia;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Minimal smoke-test: connect to a LEIA board, read the ATR, optionally SELECT
 * an applet and send a custom APDU, then disconnect.
 *
 * Usage:
 *   java -cp jleia.jar smartleia.LeiaMain [AID-hex [APDU-hex]]
 *
 * Examples:
 *   # Connect + ATR only
 *   java -jar jleia.jar
 *
 *   # Connect, SELECT applet, read ATR
 *   java -jar jleia.jar A0000000031010
 *
 *   # Connect, SELECT applet, send a custom APDU
 *   java -jar jleia.jar A0000000031010 00A4040007A0000000031010
 */
public class LeiaMain {

    public static void main(String[] args) {
        String aidHex  = args.length > 0 ? args[0] : null;
        String apduHex = args.length > 1 ? args[1] : null;

        System.out.println("=== LEIA board smoke test ===");

        TargetController tc = new TargetController();

        // Step 1 — find and open LEIA board
        System.out.println("[1] Connecting to LEIA board...");
        while (!tc.open()) {
            System.out.println("    Board not found, retrying in 1 s...");
            sleep(1000);
        }

        // Step 2 — wait for card
        System.out.println("[2] Waiting for card insertion...");
        while (!tc.isCardInserted()) {
            System.out.println("    No card detected, retrying in 1 s...");
            sleep(1000);
        }

        // Step 3 — configure (T=1, negotiate ETU/frequency automatically)
        System.out.println("[3] Configuring smartcard (T=1, auto ETU/freq, negotiate PTS)...");
        tc.configureSmartcard(Protocol.T1, 0, 0, true, true);

        // Step 4 — read ATR
        System.out.println("[4] Reading ATR...");
        ATR atr = tc.getATR();
        System.out.printf("    Protocol  : T=%d%n", atr.getProtocol());
        System.out.printf("    Clock freq: %d kHz%n", atr.getMaxFrequencyHz() / 1000);
        System.out.printf("    ATR (hex) : %s%n", atr.normalized());

        // Step 5 — optionally SELECT an applet
        if (aidHex != null) {
            System.out.printf("[5] SELECT AID %s...%n", aidHex);
            byte[] aid = hexToBytes(aidHex);
            CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid);
            ResponseAPDU resp = tc.sendAPDU(select);
            System.out.printf("    SW        : %04X%n", resp.getSW());
            if (resp.getData().length > 0) {
                System.out.printf("    Data      : %s%n", bytesToHex(resp.getData()));
            }
            System.out.printf("    Round-trip: %d µs%n", tc.getLastTransmitTimeNano() / 1000);
        }

        // Step 6 — optionally send a custom APDU
        if (apduHex != null) {
            System.out.printf("[6] Sending APDU %s...%n", apduHex);
            CommandAPDU cmd = new CommandAPDU(hexToBytes(apduHex));
            ResponseAPDU resp = tc.sendAPDU(cmd);
            System.out.printf("    SW        : %04X%n", resp.getSW());
            if (resp.getData().length > 0) {
                System.out.printf("    Data      : %s%n", bytesToHex(resp.getData()));
            }
            System.out.printf("    Round-trip: %d µs%n", tc.getLastTransmitTimeNano() / 1000);
        }

        // Step 7 — disconnect
        System.out.println("[7] Disconnecting...");
        tc.close();
        System.out.println("=== Done ===");
    }

    // -------------------------------------------------------------------------

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Odd-length hex string: " + hex);
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}