# jleia

Java driver for the [LEIA smartcard board](https://github.com/cw-leia/leia-hardware),
a port of the Python [smartleia](https://github.com/cw-leia/smartleia) library.

## Features

- Connect to the LEIA board over USB serial
- Configure smartcard protocol (T=0 / T=1), ETU, and frequency
- Read the Answer-to-Reset (ATR)
- Send ISO 7816 APDUs and receive responses
- APDU round-trip timing from the board's on-chip timer (microseconds)
- Trigger strategy control for power-analysis measurements
- `LeiaBIBO` adapter for `apdu4j`-based tools (e.g. GlobalPlatformPro applet installation)

## Requirements

- Java 8+
- LEIA board with a smartcard inserted
- USB driver for the LEIA board (CP2102 on Windows; native on Linux/macOS)

## Quick start

```bash
./gradlew run                                    # connect + ATR only
./gradlew run --args="A0000000031010"            # SELECT applet
./gradlew run --args="A0000000031010 8010000000" # SELECT + custom APDU
```

Or build a fat JAR and run directly:

```bash
./gradlew jar
java -jar build/libs/jleia-*.jar [AID-hex [APDU-hex]]
```

## Usage as a library

```java
TargetController tc = new TargetController();
tc.open();
tc.configureSmartcard(Protocol.T1, 0, 0, true, true);

ATR atr = tc.getATR();
System.out.println(atr.normalized()); // ISO 7816 ATR hex string
System.out.printf("Protocol T=%d, clock %d kHz%n",
        atr.getProtocol(), atr.getMaxFrequencyHz() / 1000);

ResponseAPDU response = tc.sendAPDU(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
System.out.printf("SW: %04X, round-trip: %d µs%n",
        response.getSW(), tc.getLastTransmitTimeNano() / 1000);

tc.close();
```

### GlobalPlatformPro integration (BIBO)

`LeiaBIBO` implements the [`apdu4j` BIBO](https://github.com/martinpaljak/apdu4j) interface
(`byte[] transceive(byte[]) / void close()`), which lets tools like
[GlobalPlatformPro](https://github.com/martinpaljak/GlobalPlatformPro) use the LEIA board
as a card I/O channel — for example to install or manage applets.

`apdu4j-core` is a **provided** dependency: jleia compiles against it but does not bundle it.
Add it to your project alongside jleia:

```gradle
// Gradle
implementation 'com.github.martinpaljak:gptool:20.08.12'   // pulls in apdu4j-core transitively
implementation files('path/to/jleia-0.1.0.jar')
```

Usage:

```java
TargetController tc = new TargetController();
tc.open();
tc.configureSmartcard(Protocol.T1, 0, 0, true, true);

BIBO bibo = new LeiaBIBO(tc);
int exitCode = new GPTool().run(bibo, new String[]{"--install", "MyApplet.cap"});
// bibo.close() delegates to tc.close()
bibo.close();
```

The `LeiaBIBO.close()` call closes the underlying serial port, so no separate `tc.close()` is needed after handing off to `LeiaBIBO`.

### Trigger strategies (for power-analysis profiling)

```java
tc.resetTriggerStrategy();           // disarm all triggers
tc.setPreSendAPDUTriggerStrategy();  // arm GPIO trigger before APDU is sent
tc.sendAPDU(apdu);                   // oscilloscope captures the trace
```

## Known limitations

Several features from the Python `smartleia` library are not yet implemented:

- `open()` — does not handle more than 2 matching USB ports gracefully
- `checkStatus()` / `checkAck()` — logs errors instead of throwing exceptions
- `configureSmartcard()` — does not retry with AUTO protocol when PTS negotiation fails
- `setPreSendAPDUTriggerStrategy()` / `resetTriggerStrategy()` — only two hardcoded
  strategies; configurable SID, delay, point list, and single-shot mode are not exposed
- `sendAPDU()` — returns only `ResponseAPDU`; `deltaTAnswer` (first-byte latency) is discarded
- No `reset()`, `get_trigger_strategy()`, `get_timers()`, `set_mode()`, or `pcsc_relay()` equivalents

## License

GNU Lesser General Public License v2.1 or later — see [LICENSE](LICENSE).

Derived from the [SmartLEIA](https://github.com/cw-leia/smartleia) project
by The LEIA Team (LGPL-2.1-or-later / BSD-3-Clause).