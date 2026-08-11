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

/** Golden gate for the Kyrgyzstan (KMDB/muftiyat.kg) scraper: the captured year page parses to the
 *  published 24-hour minutes from its inline JS day array. The live fetch runs under make scrape. */
class KyrgyzstanScraperTest {

    private static String fixture() {
        try (InputStream in = KyrgyzstanScraperTest.class.getResourceAsStream("/kyrgyzstan-2026-sample.html")) {
            if (in == null) {
                throw new IllegalStateException("Missing kyrgyzstan-2026-sample.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesTheYearToPublishedMinutes() {
        List<TimetableRow> rows = new KyrgyzstanFeedParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full non-leap year");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(6 * 60 + 50, jan1.fajr());      // 06:50
        assertEquals(8 * 60 + 32, jan1.sunrise());   // 08:32
        assertEquals(13 * 60 + 5, jan1.dhuhr());     // 13:05
        assertEquals(15 * 60 + 57, jan1.asr());      // 15:57
        assertEquals(17 * 60 + 44, jan1.maghrib());  // 17:44
        assertEquals(19 * 60 + 8, jan1.isha());      // 19:08

        TimetableRow dec31 = rows.get(rows.size() - 1);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
