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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Golden gate for the Sri Lanka (ACJU) scraper (offline): one captured monthly PDF is parsed to the
 * published minutes (explicit AM/PM), and the index builder maps the awkward hrefs - including the
 * {@code May_010.pdf} alternate - to (zone, month). The live fetch runs only under {@code make scrape}.
 */
class AcjuScraperTest {

    private static byte[] janPdf() {
        try (InputStream in = AcjuScraperTest.class.getResourceAsStream("/acju-zone01-jan-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing acju-zone01-jan-sample.pdf");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesAMonthlyPdfToPublishedMinutes() {
        List<TimetableRow> rows = new AcjuPdfParser().parse(janPdf(), 2026);

        assertEquals(31, rows.size(), "January has 31 days");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(5 * 60, jan1.fajr());          // 5:00 AM
        assertEquals(6 * 60 + 22, jan1.sunrise());  // 6:22 AM
        assertEquals(12 * 60 + 15, jan1.dhuhr());   // Luhr 12:15 PM
        assertEquals(15 * 60 + 37, jan1.asr());     // 3:37 PM
        assertEquals(18 * 60 + 7, jan1.maghrib());  // 6:07 PM
        assertEquals(19 * 60 + 21, jan1.isha());    // 7:21 PM
        assertEquals(0, jan1.solarNoon());
    }

    @Test
    void indexerMapsStandardAndAlternateHrefNames() {
        String html = ""
                + "<a href=\"https://www.acju.lk/wp-content/uploads/2025/07/"
                + "01-COLOMBO-DISTRICT-GAMPAHA-DISTRICT-KALUTARA-DISTRICT-1-Jan.pdf\">z1 jan</a>"
                + "<a href=\"https://www.acju.lk/wp-content/uploads/2025/07/"
                + "01-COLOMBO-DISTRICT-GAMPAHA-DISTRICT-KALUTARA-DISTRICT-6-June.pdf\">z1 jun</a>"
                + "<a href=\"https://www.acju.lk/wp-content/uploads/2026/05/May_010.pdf\">z10 may alt</a>";
        Map<String, Map<Integer, String>> idx = AcjuScraper.buildIndex(html);

        assertTrue(idx.get("01").get(1).endsWith("1-Jan.pdf"));
        assertTrue(idx.get("01").get(6).endsWith("6-June.pdf"));
        assertTrue(idx.get("10").get(5).endsWith("May_010.pdf"), "alternate name maps to zone 10 May");
    }
}
