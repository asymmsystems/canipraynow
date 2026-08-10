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
 * Golden gate for the Albania (KMSH) coordinate PDF scraper (offline): the captured Tirana PDF is
 * parsed by the (x, y) clustering, proving the six columns and the half-month day numbers align. The
 * live fetch runs only under {@code make scrape}.
 */
class AlbaniaScraperTest {

    private static byte[] fixture() {
        try (InputStream in = AlbaniaScraperTest.class.getResourceAsStream("/albania-2026-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing albania-2026-sample.pdf");
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
        List<TimetableRow> rows = new AlbaniaPdfParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full non-leap year");
        TimetableRow jan1 = day(rows, 1, 1);
        assertEquals(5 * 60 + 27, jan1.fajr());       // 5:27
        assertEquals(7 * 60, jan1.sunrise());         // 7:00
        assertEquals(11 * 60 + 49, jan1.dhuhr());     // 11:49
        assertEquals(14 * 60 + 8, jan1.asr());        // 14:08
        assertEquals(16 * 60 + 28, jan1.maghrib());   // 16:28
        assertEquals(17 * 60 + 55, jan1.isha());      // 17:55
        assertEquals(0, jan1.solarNoon());

        // A second-half day proves the day column (16-31) is read, not row order.
        TimetableRow jan16 = day(rows, 1, 16);
        assertEquals(16, jan16.day());

        TimetableRow dec31 = day(rows, 12, 31);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
