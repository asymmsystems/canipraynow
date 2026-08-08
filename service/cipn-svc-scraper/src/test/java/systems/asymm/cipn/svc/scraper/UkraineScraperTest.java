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
 * Golden gate for the Ukraine (islam.ua) coordinate PDF scraper (offline): the captured Kyiv PDF is
 * parsed by the (x, y) clustering and reproduces the published 24-hour minutes, proving the day
 * alignment survives the merged fajr/holiday runs. The live fetch runs only under {@code make scrape}.
 */
class UkraineScraperTest {

    private static byte[] fixture() {
        try (InputStream in = UkraineScraperTest.class.getResourceAsStream("/ukraine-2026-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing ukraine-2026-sample.pdf");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static TimetableRow day(List<TimetableRow> rows, int month, int day) {
        return rows.stream().filter(r -> r.month() == month && r.day() == day).findFirst().orElseThrow();
    }

    @Test
    void parsesTheAnnualPdfToPublishedMinutes() {
        List<TimetableRow> rows = new UkrainePdfParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full non-leap year");
        TimetableRow jan1 = day(rows, 1, 1);
        assertEquals(6 * 60 + 9, jan1.fajr());       // Rassvet 6:09
        assertEquals(7 * 60 + 49, jan1.sunrise());   // Voskhod 7:49
        assertEquals(12 * 60 + 6, jan1.dhuhr());     // Zuhr 12:06
        assertEquals(13 * 60 + 53, jan1.asr());      // Asr 13:53
        assertEquals(16 * 60 + 14, jan1.maghrib());  // Magrib 16:14
        assertEquals(17 * 60 + 44, jan1.isha());     // Isha 17:44
        assertEquals(0, jan1.solarNoon());

        // Day 5 carries the "Isra & Miraj" holiday note merged into its fajr run - must still align.
        TimetableRow jan5 = day(rows, 1, 5);
        assertEquals(6 * 60 + 8, jan5.fajr());       // Rassvet 6:08

        TimetableRow dec31 = day(rows, 12, 31);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
