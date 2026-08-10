/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Golden gate for the Kazakhstan (DUMK/muftyat.kz) JSON scraper (offline): the captured Almaty
 * response is mapped to the published 24-hour minutes. The live fetch runs only under
 * {@code make scrape}.
 */
class KazakhstanScraperTest {

    private static String fixture() {
        try (InputStream in = KazakhstanScraperTest.class.getResourceAsStream("/kazakhstan-2026-sample.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing kazakhstan-2026-sample.json");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesTheAnnualJsonToPublishedMinutes() {
        List<TimetableRow> rows = new KazakhstanFeedParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full non-leap year");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(5 * 60 + 59, jan1.fajr());       // Fajr 05:59
        assertEquals(7 * 60 + 19, jan1.sunrise());    // Sunrise 07:19
        assertEquals(12 * 60 + 1, jan1.dhuhr());      // Dhuhr 12:01
        assertEquals(14 * 60 + 50, jan1.asr());       // Asr 14:50
        assertEquals(16 * 60 + 32, jan1.maghrib());   // Maghrib 16:32
        assertEquals(17 * 60 + 53, jan1.isha());      // Isha 17:53
        assertEquals(0, jan1.solarNoon());

        TimetableRow dec31 = rows.get(rows.size() - 1);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
