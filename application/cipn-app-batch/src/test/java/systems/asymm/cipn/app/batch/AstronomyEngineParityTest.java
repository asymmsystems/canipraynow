/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;
import systems.asymm.cipn.svc.accessor.file.ClasspathCalculationMethodAccessor;
import systems.asymm.cipn.svc.engine.DefaultAstronomyEngine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration parity check for the astronomy pipeline: the calculation-method
 * accessor (E2) resolves each method's parameters, the engine (E3) computes the day,
 * and every one of the seven events must reproduce the vendored adhan.js reference to
 * the minute across all 2688 golden vectors. This is where accessor and engine are
 * wired together (as S2 will wire them), so the golden oracle lives here rather than in
 * either pure service module. Regenerate with {@code scripts/gen-golden-vectors.mjs}.
 */
class AstronomyEngineParityTest {

    private static final String[] EVENTS = {"fajr", "sunrise", "dhuhr", "asr", "sunset", "maghrib", "isha"};

    @Test
    void engineWithResolvedMethodReproducesAdhanToTheMinute() throws Exception {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/golden-vectors.json")) {
            assertNotNull(in, "golden-vectors.json missing from test resources");
            root = new ObjectMapper().readTree(in);
        }

        CalculationMethodAccessor methods = new ClasspathCalculationMethodAccessor();
        AstronomyEngine engine = new DefaultAstronomyEngine();

        List<String> mismatches = new ArrayList<>();
        long worstMs = 0;
        int checked = 0;

        for (JsonNode v : root.get("vectors")) {
            String method = v.get("method").asText();
            String madhab = v.get("madhab").asText();
            Coordinates coords = new Coordinates(v.get("lat").asDouble(), v.get("lng").asDouble());
            LocalDate date = LocalDate.of(v.get("y").asInt(), v.get("mo").asInt() + 1, v.get("da").asInt());

            CalculationParameters params = methods.parametersOrDefault(
                    method, "hanafi".equals(madhab) ? Madhab.HANAFI : Madhab.SHAFI);
            DayEvents e = engine.compute(coords, date, params);

            Instant[] actual = {e.fajr(), e.sunrise(), e.dhuhr(), e.asr(), e.sunset(), e.maghrib(), e.isha()};
            for (int i = 0; i < EVENTS.length; i++) {
                JsonNode expNode = v.get(EVENTS[i]);
                Long expected = expNode.isNull() ? null : expNode.asLong();
                Long got = actual[i] == null ? null : actual[i].toEpochMilli();
                checked++;
                if (expected == null || got == null) {
                    if (expected != got) {
                        mismatches.add("%s/%s %s %s: null-mismatch exp=%s got=%s"
                                .formatted(method, madhab, v.get("loc").asText(), EVENTS[i], expected, got));
                    }
                    continue;
                }
                long diff = Math.abs(expected - got);
                if (diff != 0) {
                    worstMs = Math.max(worstMs, diff);
                    if (mismatches.size() < 40) {
                        mismatches.add("%s/%s %s %s %s: exp=%s got=%s (%+d s)".formatted(
                                method, madhab, v.get("loc").asText(), date, EVENTS[i],
                                Instant.ofEpochMilli(expected), Instant.ofEpochMilli(got), (got - expected) / 1000));
                    }
                }
            }
        }

        if (!mismatches.isEmpty()) {
            fail("Parity failures: %d of %d checks off (worst %d s). Samples:%n%s".formatted(
                    mismatches.size(), checked, worstMs / 1000, String.join("\n", mismatches)));
        }
    }
}
