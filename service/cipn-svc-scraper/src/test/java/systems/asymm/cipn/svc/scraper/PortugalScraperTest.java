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
 * Golden gate for the Portugal (CIL) PDF scraper (offline): the captured Lisbon PDF is parsed by
 * PDFBox and the first eight time columns map to the published minutes, dropping Zawal and the
 * second isha column and ignoring the moon-phase note times. The live fetch runs under
 * {@code make scrape}.
 */
class PortugalScraperTest {

    private static byte[] fixture() {
        try (InputStream in = PortugalScraperTest.class.getResourceAsStream("/portugal-2026-sample.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Missing portugal-2026-sample.pdf");
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
        List<TimetableRow> rows = new PortugalPdfParser().parse(fixture(), 2026);

        assertEquals(365, rows.size(), "a full non-leap year");
        TimetableRow jan1 = day(rows, 1, 1);
        assertEquals(6 * 60 + 20, jan1.fajr());       // Fajr 06:20
        assertEquals(7 * 60 + 54, jan1.sunrise());    // Nascer do Sol 07:54
        assertEquals(12 * 60 + 46, jan1.dhuhr());     // Zuhr 12:46 (Zawal 12:41 skipped)
        assertEquals(15 * 60 + 8, jan1.asr());        // Assr 15:08
        assertEquals(17 * 60 + 29, jan1.maghrib());   // Magrib 17:29
        assertEquals(18 * 60 + 32, jan1.isha());      // Isha 12deg+3 18:32 (not the 18:46 column)
        assertEquals(0, jan1.solarNoon());

        // Jan 18 carries a trailing moon-phase note "als 19:52" - must not shift the columns.
        TimetableRow jan18 = day(rows, 1, 18);
        assertEquals(6 * 60 + 19, jan18.fajr());      // Fajr 06:19
        assertEquals(18 * 60 + 47, jan18.isha());     // Isha 12deg+3 18:47, not 19:03 (01:20) or 19:52 (note)

        TimetableRow dec31 = day(rows, 12, 31);
        assertEquals(12, dec31.month());
        assertEquals(31, dec31.day());
    }
}
