<!--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
  @author Fahim Farook
-->
# cipn-svc-engine

The pure business engine (no I/O, no accessor calls). Its first component is the
**E3 astronomy engine** (`systems.asymm.cipn.engine.astronomy`): the one general
prayer-times engine that the model runs natively on the JVM (build/S2, server)
and, later, compiled to WASM for the browser (T-15).

## E3 astronomy (T-14, ADR-0009)

The contract is `AstronomyEngine` (in `cipn-fw-contract-engine`, extending the
`Engine` marker); `DefaultAstronomyEngine` implements it. `compute(Coordinates,
LocalDate, CalculationParameters)` returns a `DayEvents` (fajr, sunrise, dhuhr,
asr, sunset, maghrib, isha). It is a faithful port of the vendored `adhan.js` the
browser has always used, so both runtimes agree to the minute. The value types
(`Coordinates`, `CalculationParameters`, `DayEvents`, the enums) live in
`cipn-fw-contract-data`; the solar math (`Astronomical`, `SolarCoordinates`,
`SolarTime`, `SolarDayCalculator`) is package-private impl detail here.

- **E3** = `compute(...)` with any angles. **E2** = `resolveMethod(key, madhab)`
  (the authority's published angles). The registry mirrors `astronomy.js`
  `buildParams()` exactly: ACJU / Kemenag / France / Russia are bare custom-angle
  parameter sets (no Dhuhr offset); the rest reproduce the adhan presets.

### Parity oracle

`src/test/resources/golden-vectors.json` holds 2688 reference rows captured from
`adhan.js` (16 methods x 2 madhabs x 14 locations x 6 dates). `PrayerTimesParityTest`
asserts every event matches to the minute. Regenerate the oracle only when the
method registry or `adhan.js` changes:

```
node service/cipn-svc-engine/scripts/gen-golden-vectors.mjs
```

(needs Node on PATH; the JSON is the committed source of truth).

### Deferred (tracked)

- **Polar-circle resolution** (adhan `PolarCircleResolution`): omitted, because the
  site never enables it. Latitudes beyond ~55&deg; where twilight does not resolve
  are a follow-up; the golden set stays below 55&deg; for now.
- **TeaVM/WASM compile** of this engine for the browser is the remaining half of
  T-14 / WS-B; the JVM engine and its parity oracle land first.
