# smartleia-java

Java driver for the [LEIA smartcard board](https://github.com/cw-leia/leia-hardware),
a port of the Python [smartleia](https://github.com/cw-leia/smartleia) library.

## Features

- Connect to the LEIA board over USB serial
- Configure smartcard protocol (T=0 / T=1), ETU, and frequency
- Read the Answer-to-Reset (ATR)
- Send ISO 7816 APDUs and receive responses
- APDU round-trip timing from the board's on-chip timer (microseconds)
- Trigger strategy control for power-analysis measurements

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
java -jar build/libs/smartleia-java-*.jar [AID-hex [APDU-hex]]
```

## Usage as a library

```java
TargetController tc = new TargetController();
tc.open();
tc.configureSmartcard(ConfigureSmartcardCommand.T.T1, 0, 0, true, true);

ATR atr = tc.getATR();
System.out.println(atr.normalized()); // ISO 7816 ATR hex string

ResponseAPDU response = tc.sendAPDU(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
System.out.printf("SW: %04X, round-trip: %d µs%n",
        response.getSW(), tc.getLastTransmitTimeNano() / 1000);

tc.close();
```

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

BSD 3-Clause License — see [LICENSE](LICENSE).

Derived from the [SmartLEIA](https://github.com/cw-leia/smartleia) project
by The LEIA Team, also under BSD 3-Clause.