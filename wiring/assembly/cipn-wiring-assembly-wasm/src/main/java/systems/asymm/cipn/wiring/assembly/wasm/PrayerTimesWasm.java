/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.assembly.wasm;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import org.teavm.jso.JSExport;
import org.teavm.jso.typedarrays.Float64Array;

import systems.asymm.cipn.fw.common.util.MinimalXmlParser;
import systems.asymm.cipn.fw.common.util.XmlElement;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;
import systems.asymm.cipn.wiring.engine.EngineWiring;

/**
 * WASM entry point for the browser fallback (R2). Composes, per ADR-0015, the single browser
 * artefact from two pure, framework-free parts: the astronomy {@link AstronomyEngine} (E3) and
 * a {@link MinimalXmlParser} that reads the method table. The module is loaded only for the
 * fallback (off-grid GPS or a non-default method); the common covered-city path reads the baked
 * table from the page and never touches WASM.
 *
 * <p>The engine is obtained through {@link EngineWiring} rather than instantiated directly, so
 * impl construction stays in the wiring layer (M360 convention) and the engine remains pure.
 * The parser + {@link MethodTable} mapping is validated against the JVM Jackson accessor by a
 * parity test, so the WASM path computes identical parameters to the build/server path.
 */
public final class PrayerTimesWasm {

    private static final AstronomyEngine ENGINE = EngineWiring.astronomyEngine();

    private PrayerTimesWasm() {
    }

    /**
     * Compute the seven daily events, returned as a JS {@code Float64Array} of UTC epoch-seconds
     * in the fixed order {@code [fajr, sunrise, dhuhr, asr, sunset, maghrib, isha]}. A typed array
     * (rather than a Java {@code double[]}) is used so JavaScript receives a native, directly
     * indexable {@code Float64Array} off {@code instance.exports.compute(...)}; it formats each
     * value in the display zone.
     *
     * @param latitude      observer latitude in decimal degrees
     * @param longitude     observer longitude in decimal degrees
     * @param year          Gregorian year
     * @param month         month of year, 1-12
     * @param day           day of month
     * @param madhabOrdinal 2 selects Hanafi (later Asr); any other value selects Shafi
     * @param methodKey     the calculation-method key (matched case-insensitively; unknown keys
     *                      fall back to the table default)
     * @param methodXml     the contents of {@code calculation-method.xml} (the parser's input)
     * @return the seven events as UTC epoch-seconds
     */
    @JSExport
    public static Float64Array compute(double latitude, double longitude,
            int year, int month, int day, int madhabOrdinal, String methodKey, String methodXml) {
        XmlElement root = new MinimalXmlParser()
                .parse(new ByteArrayInputStream(methodXml.getBytes(StandardCharsets.UTF_8)));
        Madhab madhab = madhabOrdinal == 2 ? Madhab.HANAFI : Madhab.SHAFI;
        CalculationParameters params = MethodTable.resolve(root, methodKey, madhab);
        DayEvents events = ENGINE.compute(
                new Coordinates(latitude, longitude), LocalDate.of(year, month, day), params);
        Float64Array out = Float64Array.create(7);
        out.set(0, epochSeconds(events.fajr()));
        out.set(1, epochSeconds(events.sunrise()));
        out.set(2, epochSeconds(events.dhuhr()));
        out.set(3, epochSeconds(events.asr()));
        out.set(4, epochSeconds(events.sunset()));
        out.set(5, epochSeconds(events.maghrib()));
        out.set(6, epochSeconds(events.isha()));
        return out;
    }

    private static double epochSeconds(Instant instant) {
        return instant.getEpochSecond();
    }

    /**
     * TeaVM entry point. The WASM package is a library of {@link JSExport} methods rather than
     * a program, so this is a no-op; it exists only to satisfy the compiler's main-class root.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // intentionally empty
    }
}
