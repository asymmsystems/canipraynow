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
 * Golden gate for the Hungary (MME/iszlam.com) HTML scraper (offline): the captured page is parsed
 * and the 12-hour AM/PM inference reproduces the published minutes. Critically it also proves the
 * month is read from each table's heading, not table order: the page prints October before
 * September, so table order would swap the two whole months. The live fetch runs under
 * {@code make scrape}.
 */
class HungaryScraperTest {

    private static String fixture() {
        try (InputStream in = HungaryScraperTest.class.getResourceAsStream("/hungary-2026-sample.html")) {
            if (in == null) {
                throw new IllegalStateException("Missing hungary-2026-sample.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static TimetableRow day(List<TimetableRow> rows, int month, int day) {
        return rows.stream().filter(r -> r.month() == month && r.day() == day).findFirst().orElseThrow();
    }

    @Test
    void parsesAllTwelveMonthsWithHeadingDrivenMonths() {
        List<TimetableRow> rows = new HungaryHtmlParser().parse(fixture(), 2026);
        assertEquals(365, rows.size(), "a full non-leap year");

        TimetableRow jan1 = day(rows, 1, 1);
        assertEquals(6 * 60 + 4, jan1.fajr());       // Hajnali 6:04
        assertEquals(7 * 60 + 37, jan1.sunrise());   // Napkelte 7:37
        assertEquals(11 * 60 + 53, jan1.dhuhr());    // Deli 11:53
        assertEquals(13 * 60 + 51, jan1.asr());      // Delutani 1:51 -> 13:51
        assertEquals(16 * 60 + 8, jan1.maghrib());   // Esti 4:08 -> 16:08
        assertEquals(17 * 60 + 42, jan1.isha());     // Ejszakai 5:42 -> 17:42
        assertEquals(0, jan1.solarNoon());

        // The swap trap: September has 30 days, October 31, and each carries its own values even
        // though October is printed first on the page.
        assertEquals(30, rows.stream().filter(r -> r.month() == 9).count(), "September = 30 days");
        assertEquals(31, rows.stream().filter(r -> r.month() == 10).count(), "October = 31 days");
        TimetableRow sep1 = day(rows, 9, 1);
        assertEquals(4 * 60 + 34, sep1.fajr());      // Sep 1 Hajnali 4:34
        assertEquals(6 * 60 + 2, sep1.sunrise());    // Sep 1 Napkelte 6:02
        TimetableRow oct1 = day(rows, 10, 1);
        assertEquals(5 * 60 + 20, oct1.fajr());      // Oct 1 Hajnali 5:20
        assertEquals(6 * 60 + 42, oct1.sunrise());   // Oct 1 Napkelte 6:42
    }
}
