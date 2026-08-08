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
 * Golden gate for the Botswana (BMA) HTML scraper (offline): the captured page is parsed per city,
 * mapping Zawwal to dhuhr and the Shafi asr column to asr. The live fetch runs under
 * {@code make scrape}.
 */
class BotswanaScraperTest {

    private static String fixture() {
        try (InputStream in = BotswanaScraperTest.class.getResourceAsStream("/botswana-2026-sample.html")) {
            if (in == null) {
                throw new IllegalStateException("Missing botswana-2026-sample.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesGaboroneToPublishedMinutes() {
        List<TimetableRow> rows = new BotswanaHtmlParser().parse(fixture(), "gabarone", 2026);

        // BMA publishes month by month; the capture holds Jan-Sep (Feb 29 dropped as a non-day).
        assertEquals(31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30, rows.size(), "the published months");
        assertEquals(9, rows.get(rows.size() - 1).month(), "up to September so far");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(4 * 60 + 2, jan1.fajr());        // Fajr 4:02 AM
        assertEquals(5 * 60 + 30, jan1.sunrise());    // Sunrise 5:30 AM
        assertEquals(12 * 60 + 20, jan1.dhuhr());     // Zawwal 12:20 PM -> dhuhr
        assertEquals(15 * 60 + 41, jan1.asr());       // Shafi Asr 3:41 PM (not Hanafi 5:03)
        assertEquals(19 * 60 + 12, jan1.maghrib());   // Maghrib 7:12 PM (not Sunset 7:09)
        assertEquals(20 * 60 + 32, jan1.isha());      // Isha 8:32 PM
        assertEquals(0, jan1.solarNoon());
    }

    @Test
    void parsesEachCityIndependentlyWithItsOwnTimes() {
        BotswanaHtmlParser p = new BotswanaHtmlParser();
        TimetableRow gab = p.parse(fixture(), "gabarone", 2026).get(0);
        List<TimetableRow> fra = p.parse(fixture(), "Francistown", 2026);
        // Each city is read from its own div block, with its own values (BMA publishes per city, so
        // totals differ by city and are enforced against a full year only at scrape time).
        assertEquals(false, fra.isEmpty());
        assertEquals(true, fra.get(0).fajr() != gab.fajr() || fra.get(0).maghrib() != gab.maghrib());
    }
}
