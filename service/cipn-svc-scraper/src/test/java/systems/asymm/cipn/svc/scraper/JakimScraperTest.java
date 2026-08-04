/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * The S1 pilot's golden gate (offline, deterministic): a captured e-Solat response for WLY01 must
 * map, minute-for-minute, to the values in the committed MY/2026 timetable, and the scraper must
 * assemble a well-formed {@link AuthorityTimetable}. The live network path is exercised by
 * {@code make scrape}, not the build (ADR-0002/0009).
 */
class JakimScraperTest {

    private static final TimetableZone WLY01 =
            new TimetableZone("WLY01", "Kuala Lumpur, Putrajaya", 3.1390, 101.6869);

    private static String fixture() {
        try (InputStream in = JakimScraperTest.class.getResourceAsStream("/esolat-wly01-sample.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture esolat-wly01-sample.json");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A stub transport returning a canned body, so the mapping is tested without network. */
    private static FeedReader canned(String body) {
        return url -> body;
    }

    @Test
    void mapsCapturedDaysToCommittedMinutes() {
        List<TimetableRow> rows = new JakimFeedParser().parse(fixture(), 2026);

        // Five 2026 days in the fixture (the 2027 row is dropped), in calendar order.
        assertEquals(5, rows.size());

        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(366, jan1.fajr());     // 06:06
        assertEquals(438, jan1.sunrise());  // 07:18 (e-Solat syuruk)
        assertEquals(799, jan1.dhuhr());    // 13:19
        assertEquals(1002, jan1.asr());     // 16:42
        assertEquals(1157, jan1.maghrib()); // 19:17
        assertEquals(1231, jan1.isha());    // 20:31
        assertEquals(0, jan1.solarNoon());  // E3 enrichment, not published

        TimetableRow dec31 = rows.get(rows.size() - 1);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
        assertEquals(366, dec31.fajr());
        assertEquals(437, dec31.sunrise());
        assertEquals(1231, dec31.isha());
    }

    @Test
    void dropsDaysOutsideRequestedYear() {
        // The fixture carries a 01-Jan-2027 row; requesting 2026 must exclude it, and there is no
        // stray 2027 row when requesting 2026.
        List<TimetableRow> rows = new JakimFeedParser().parse(fixture(), 2026);
        assertTrue(rows.stream().allMatch(r -> r.month() >= 1 && r.month() <= 12));
        assertEquals(5, rows.size());

        List<TimetableRow> onlyThe2027Row = new JakimFeedParser().parse(fixture(), 2027);
        assertEquals(1, onlyThe2027Row.size());
    }

    @Test
    void scrapeAssemblesAuthorityTimetable() {
        AuthorityTimetable table = new JakimScraper(canned(fixture())).scrape(WLY01, 2026);

        assertEquals("JAKIM", table.authority());
        assertEquals("Jabatan Kemajuan Islam Malaysia", table.authorityName());
        assertEquals("WLY01", table.zoneId());
        assertEquals("Kuala Lumpur, Putrajaya", table.zoneName());
        assertEquals(2026, table.year());
        assertEquals(5, table.rows().size());
    }

    @Test
    void scrapeReportsCountryCode() {
        assertEquals("MY", new JakimScraper(canned(fixture())).countryCode());
    }

    @Test
    void scrapeRejectsEmptyYear() {
        // No 2099 days in the fixture -> the scraper must fail loudly rather than write an empty table.
        ScrapeException e = assertThrows(ScrapeException.class,
                () -> new JakimScraper(canned(fixture())).scrape(WLY01, 2099));
        assertTrue(e.getMessage().contains("WLY01"));
    }

    @Test
    void parserRejectsNonESolatBody() {
        assertThrows(ScrapeException.class, () -> new JakimFeedParser().parse("{\"status\":\"error\"}", 2026));
    }

    @Test
    void parserRejectsMalformedTime() {
        String bad = "{\"prayerTime\":[{\"date\":\"01-Jan-2026\",\"fajr\":\"6am\",\"syuruk\":\"07:18:00\","
                + "\"dhuhr\":\"13:19:00\",\"asr\":\"16:42:00\",\"maghrib\":\"19:17:00\",\"isha\":\"20:31:00\"}]}";
        assertThrows(ScrapeException.class, () -> new JakimFeedParser().parse(bad, 2026));
    }
}
