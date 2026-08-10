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

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

/**
 * Golden gate for the Bulgaria scraper (offline): a captured monthly page maps to the published
 * minutes, the day+six-time row detection ignores surrounding layout cells, and the scraper POSTs
 * per month. Live fetching runs only under {@code make scrape}.
 */
class BulgariaScraperTest {

    private static final TimetableZone SOFIA = new TimetableZone("sofia", "Sofia", 42.6977, 23.3219);

    private static String fixture() {
        try (InputStream in = BulgariaScraperTest.class.getResourceAsStream("/bulgaria-sofia-august-sample.html")) {
            if (in == null) {
                throw new IllegalStateException("Missing bulgaria-sofia-august-sample.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void mapsCapturedMonthToPublishedMinutes() {
        List<TimetableRow> rows = new BulgariaFeedParser().parse(fixture(), 8);

        assertTrue(rows.size() >= 1);
        TimetableRow aug1 = rows.get(0);
        assertEquals(8, aug1.month());          // month supplied by the scraper
        assertEquals(1, aug1.day());
        assertEquals(259, aug1.fajr());         // Зора 4:19
        assertEquals(370, aug1.sunrise());      // Изгрев 6:10
        assertEquals(818, aug1.dhuhr());        // Обяд 13:38
        assertEquals(1055, aug1.asr());         // Следобяд 17:35
        assertEquals(1256, aug1.maghrib());     // Залез 20:56
        assertEquals(1358, aug1.isha());        // Нощ 22:38
        assertEquals(0, aug1.solarNoon());
    }

    @Test
    void scrapePostsPerMonthAndAssembles() {
        // Stub returns the August page only for the month=8 POST, empty otherwise -> assembles August.
        FeedReader stub = new FeedReader() {
            @Override
            public String get(String url) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String post(String url, String formBody) {
                return formBody.contains("month=8") ? fixture() : "<table></table>";
            }
        };

        AuthorityTimetable table = new BulgariaScraper(stub).scrape(SOFIA, 2026);
        assertEquals("BG-Muftiate", table.authority());
        assertEquals("sofia", table.zoneId());
        assertEquals("BG", new BulgariaScraper(stub).countryCode());
        assertTrue(table.rows().stream().allMatch(r -> r.month() == 8), "only the August POST returned data");
    }
}
