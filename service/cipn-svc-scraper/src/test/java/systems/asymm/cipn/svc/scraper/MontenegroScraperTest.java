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
import java.util.List;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.TimetableRow;

/** Golden gate for the Montenegro (vaktija.me) PDF scraper: the captured Podgorica annual PDF parses
 *  to the published 24-hour minutes. The live fetch runs under make scrape. */
class MontenegroScraperTest {

    private static byte[] fixture() {
        try (InputStream in = MontenegroScraperTest.class.getResourceAsStream("/montenegro-2026-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing montenegro-2026-sample.pdf");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesTheAnnualPdfToPublishedMinutes() {
        List<TimetableRow> rows = new MontenegroPdfParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full non-leap year");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(5 * 60 + 27, jan1.fajr());      // 05:27
        assertEquals(7 * 60 + 11, jan1.sunrise());   // 07:11
        assertEquals(11 * 60 + 47, jan1.dhuhr());    // 11:47
        assertEquals(13 * 60 + 57, jan1.asr());      // 13:57
        assertEquals(16 * 60 + 21, jan1.maghrib());  // 16:21
        assertEquals(17 * 60 + 53, jan1.isha());     // 17:53

        TimetableRow dec31 = rows.get(rows.size() - 1);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
