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

/**
 * Golden gate for the Singapore PDF scraper (offline): the captured MUIS PDF is parsed by PDFBox and
 * the 12-hour AM/PM inference reproduces the published minutes. The live fetch runs only under
 * {@code make scrape}.
 */
class SingaporeScraperTest {

    private static byte[] fixture() {
        try (InputStream in = SingaporeScraperTest.class.getResourceAsStream("/singapore-2026-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing singapore-2026-sample.pdf");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesTheAnnualPdfToPublishedMinutes() {
        List<TimetableRow> rows = new SingaporePdfParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full year from the 12 PDF pages");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(344, jan1.fajr());      // Subuh 5 44 (AM)
        assertEquals(428, jan1.sunrise());   // Syuruk 7 08 (AM)
        assertEquals(790, jan1.dhuhr());     // Zohor 1 10 -> 13:10 (PM)
        assertEquals(994, jan1.asr());       // Asar 4 34 -> 16:34 (PM)
        assertEquals(1151, jan1.maghrib());  // Maghrib 7 11 -> 19:11 (PM)
        assertEquals(1225, jan1.isha());     // Isyak 8 25 -> 20:25 (PM)
        assertEquals(0, jan1.solarNoon());

        TimetableRow dec31 = rows.get(rows.size() - 1);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
