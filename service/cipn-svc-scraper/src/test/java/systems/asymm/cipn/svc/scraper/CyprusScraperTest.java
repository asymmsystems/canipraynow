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
 * Golden gate for the Cyprus scraper (offline): a captured KKTC takvim page maps to the exact
 * published minutes, and the Turkish date/month + HH:MM parsing is pinned. The live fetch runs
 * only under {@code make scrape} (ADR-0017).
 */
class CyprusScraperTest {

    private static final TimetableZone LEFKOSA =
            new TimetableZone("LEF", "Lefkoşa (Nicosia)", 35.1856, 33.3823);

    private static String fixture() {
        try (InputStream in = CyprusScraperTest.class.getResourceAsStream("/cyprus-lefkosa-sample.html")) {
            if (in == null) {
                throw new IllegalStateException("Missing cyprus-lefkosa-sample.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static FeedReader canned(String body) {
        return url -> body;
    }

    @Test
    void mapsCapturedTakvimToPublishedMinutes() {
        List<TimetableRow> rows = new CyprusFeedParser().parse(fixture(), 2026);

        assertTrue(rows.size() >= 1);
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());          // "01 Ocak 2026" - Turkish month Ocak parsed
        assertEquals(1, jan1.day());
        assertEquals(326, jan1.fajr());         // İmsak 05:26
        assertEquals(410, jan1.sunrise());      // Güneş 06:50
        assertEquals(717, jan1.dhuhr());        // Öğle 11:57
        assertEquals(873, jan1.asr());          // İkindi 14:33
        assertEquals(1014, jan1.maghrib());     // Akşam 16:54
        assertEquals(1092, jan1.isha());        // Yatsı 18:12
        assertEquals(0, jan1.solarNoon());
    }

    @Test
    void dropsOtherYears() {
        assertThrows(ScrapeException.class, () -> new CyprusFeedParser().parse(fixture(), 2099));
    }

    @Test
    void scrapeAssemblesAuthorityTimetable() {
        AuthorityTimetable table = new CyprusScraper(canned(fixture())).scrape(LEFKOSA, 2026);

        assertEquals("KKTC-DIB", table.authority());
        assertEquals("LEF", table.zoneId());
        assertEquals("CY", new CyprusScraper(canned(fixture())).countryCode());
        assertTrue(table.rows().size() >= 1);
    }
}
