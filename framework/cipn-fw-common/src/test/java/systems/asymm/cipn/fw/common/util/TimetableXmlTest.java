/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Round-trips {@link TimetableXml}: an {@link AuthorityTimetable} serialises to a schema-valid
 * document that parses back to the same rows, solar noon is emitted only when present, and an
 * out-of-range value is rejected by the XSD.
 */
class TimetableXmlTest {

    private static AuthorityTimetable table(List<TimetableRow> rows) {
        return new AuthorityTimetable("JAKIM", "Jabatan Kemajuan Islam Malaysia",
                "https://www.e-solat.gov.my/", "WLY01", "Kuala Lumpur, Putrajaya",
                "https://www.e-solat.gov.my/", 2026, rows);
    }

    @Test
    void serializesAndValidates() {
        String xml = TimetableXml.serialize(table(List.of(
                new TimetableRow(1, 1, 366, 438, 799, 1002, 1157, 1231, 0),
                new TimetableRow(1, 2, 367, 438, 800, 1002, 1157, 1232, 0))));

        assertTrue(xml.contains("authority=\"JAKIM\""));
        assertTrue(xml.contains("zone=\"WLY01\""));
        assertTrue(xml.contains("unit=\"minutes-from-midnight\""));
        // Solar noon absent (E3 enrichment) - not emitted on scraped rows.
        assertFalse(xml.contains("solar-noon"));
    }

    @Test
    void roundTripsThroughStructuredParser() {
        List<TimetableRow> rows = List.of(
                new TimetableRow(1, 1, 366, 438, 799, 1002, 1157, 1231, 797),
                new TimetableRow(12, 31, 366, 437, 799, 1001, 1156, 1231, 796));
        String xml = TimetableXml.serialize(table(rows));

        Doc parsed = new StructuredXmlParser<>(Doc.class)
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("JAKIM", parsed.authority());
        assertEquals("WLY01", parsed.zone());
        assertEquals(2, parsed.day().size());
        Day first = parsed.day().get(0);
        assertEquals(1, first.month());
        assertEquals(1, first.day());
        assertEquals(366, first.fajr());
        assertEquals(438, first.sunrise());
        assertEquals(799, first.dhuhr());
        assertEquals(1002, first.asr());
        assertEquals(1157, first.maghrib());
        assertEquals(1231, first.isha());
        assertEquals(797, first.solarNoon());
    }

    @Test
    void rejectsOutOfRangeMinute() {
        // 1440 is one past the last minute of the day (0..1439) - the XSD must reject it.
        assertThrows(ResourceParseException.class, () -> TimetableXml.serialize(table(List.of(
                new TimetableRow(1, 1, 366, 438, 799, 1002, 1157, 1440, 0)))));
    }

    @Test
    void rejectsMalformedDocument() {
        assertThrows(ResourceParseException.class,
                () -> TimetableXml.validate("<timetable><day/></timetable>"));
    }

    /** Parse-back binding for the serialized document (kebab-case: solar-noon -> solarNoon). */
    record Doc(String authority, String zone, String name, String source, String unit, List<Day> day) {
    }

    record Day(int month, int day, int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha, int solarNoon) {
    }
}
