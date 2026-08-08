/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

/**
 * Golden gate for the Bosnia scraper (offline): a captured vaktija.ba month maps to the published
 * minutes, and the scraper GETs one request per month and assembles the year. Live fetching runs
 * only under {@code make scrape}.
 */
class BosniaScraperTest {

    private static final TimetableZone SARAJEVO = new TimetableZone("77", "Sarajevo", 43.8563, 18.4131);

    private static String fixture() {
        try (InputStream in = BosniaScraperTest.class.getResourceAsStream("/bosnia-sarajevo-august-sample.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing bosnia-sarajevo-august-sample.json");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void mapsCapturedMonthToPublishedMinutes() {
        List<TimetableRow> rows = new BosniaFeedParser().parse(fixture(), 8);

        assertEquals(31, rows.size());
        TimetableRow aug1 = rows.get(0);
        assertEquals(8, aug1.month());
        assertEquals(1, aug1.day());
        assertEquals(214, aug1.fajr());       // zora 3:34
        assertEquals(327, aug1.sunrise());    // izlazak 5:27
        assertEquals(773, aug1.dhuhr());      // podne 12:53
        assertEquals(1011, aug1.asr());       // ikindija 16:51
        assertEquals(1216, aug1.maghrib());   // akšam 20:16
        assertEquals(1315, aug1.isha());      // jacija 21:55
        assertEquals(0, aug1.solarNoon());
    }

    @Test
    void scrapeGetsPerMonthAndAssembles() {
        // The stub returns the same month payload for every GET, so all 12 months are present.
        FeedReader stub = url -> fixture();

        AuthorityTimetable table = new BosniaScraper(stub).scrape(SARAJEVO, 2026);
        assertEquals("BA-Rijaset", table.authority());
        assertEquals("77", table.zoneId());
        assertEquals("BA", new BosniaScraper(stub).countryCode());
        boolean allTwelve = IntStream.rangeClosed(1, 12)
                .allMatch(m -> table.rows().stream().anyMatch(r -> r.month() == m));
        assertTrue(allTwelve, "one GET per month, all 12 assembled");
    }
}
