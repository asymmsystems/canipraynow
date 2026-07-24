/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.batch;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;
import systems.asymm.cipn.wiring.accessor.classpath.ClasspathAccessorWiring;
import systems.asymm.cipn.wiring.engine.EngineWiring;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration parity check for the astronomy pipeline: the calculation-method accessor (E2)
 * resolves each method's parameters, the engine (E3) computes the day, and every one of the
 * seven events must reproduce the vendored adhan.js reference to the minute across all 2688
 * golden vectors. The oracle is the frozen {@code golden-vectors.xml} (captured once from
 * adhan.js, the historical JS reference).
 */
class AstronomyEngineParityTest {

    @Test
    void engineWithResolvedMethodReproducesAdhanToTheMinute() throws Exception {
        GoldenVectors golden;
        try (InputStream in = getClass().getResourceAsStream("/golden-vectors.xml")) {
            assertNotNull(in, "golden-vectors.xml missing from test resources");
            golden = new StructuredXmlParser<>(GoldenVectors.class).parse(in);
        }

        CalculationMethodAccessor methods = ClasspathAccessorWiring.calculationMethodAccessor();
        AstronomyEngine engine = EngineWiring.astronomyEngine();

        List<String> mismatches = new ArrayList<>();
        long worstMs = 0;
        int checked = 0;

        for (Vector v : golden.vector()) {
            Coordinates coords = new Coordinates(v.lat(), v.lng());
            LocalDate date = LocalDate.of(v.y(), v.mo() + 1, v.da());
            CalculationParameters params = methods.parametersOrDefault(
                    v.method(), "hanafi".equals(v.madhab()) ? Madhab.HANAFI : Madhab.SHAFI);
            DayEvents e = engine.compute(coords, date, params);

            long[] expected = {v.fajr(), v.sunrise(), v.dhuhr(), v.asr(), v.sunset(), v.maghrib(), v.isha()};
            Instant[] actual = {e.fajr(), e.sunrise(), e.dhuhr(), e.asr(), e.sunset(), e.maghrib(), e.isha()};
            String[] names = {"fajr", "sunrise", "dhuhr", "asr", "sunset", "maghrib", "isha"};
            for (int i = 0; i < names.length; i++) {
                checked++;
                long got = actual[i].toEpochMilli();
                long diff = Math.abs(expected[i] - got);
                if (diff != 0) {
                    worstMs = Math.max(worstMs, diff);
                    if (mismatches.size() < 40) {
                        mismatches.add("%s/%s %s %s %s: exp=%s got=%s (%+d s)".formatted(
                                v.method(), v.madhab(), v.loc(), date, names[i],
                                Instant.ofEpochMilli(expected[i]), actual[i], (got - expected[i]) / 1000));
                    }
                }
            }
        }

        if (!mismatches.isEmpty()) {
            fail("Parity failures: %d of %d checks off (worst %d s). Samples:%n%s".formatted(
                    mismatches.size(), checked, worstMs / 1000, String.join("\n", mismatches)));
        }
    }

    /** XML binding for {@code golden-vectors.xml}. */
    record GoldenVectors(List<Vector> vector) {
    }

    /** XML binding for one {@code <vector .../>} (times are UTC epoch millis). */
    record Vector(
            String method, String madhab, String loc, double lat, double lng, int y, int mo, int da,
            long fajr, long sunrise, long dhuhr, long asr, long sunset, long maghrib, long isha) {
    }
}
