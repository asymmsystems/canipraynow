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
 * Golden gate for the Kosovo (BIK) PDF scraper (offline): the captured Takvim PDF is parsed by PDFBox
 * and the row-major 24-hour layout reproduces the published minutes, with fajr taken from the Imsaku
 * column. The live fetch runs only under {@code make scrape}.
 */
class KosovoScraperTest {

    private static byte[] fixture() {
        try (InputStream in = KosovoScraperTest.class.getResourceAsStream("/kosovo-2026-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing kosovo-2026-sample.pdf");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesTheAnnualPdfToPublishedMinutes() {
        List<TimetableRow> rows = new KosovoPdfParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full year");
        TimetableRow jan1 = rows.get(0);
        assertEquals(1, jan1.month());
        assertEquals(1, jan1.day());
        assertEquals(5 * 60 + 21, jan1.fajr());      // Imsaku 5:21
        assertEquals(7 * 60 + 3, jan1.sunrise());    // Lindja e diellit 7:03
        assertEquals(11 * 60 + 44, jan1.dhuhr());    // Dreka 11:44
        assertEquals(14 * 60 + 5, jan1.asr());       // Ikindia 14:05
        assertEquals(16 * 60 + 21, jan1.maghrib());  // Akshami 16:21
        assertEquals(18 * 60, jan1.isha());          // Jacia 18:00
        assertEquals(0, jan1.solarNoon());

        TimetableRow dec31 = rows.get(rows.size() - 1);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
