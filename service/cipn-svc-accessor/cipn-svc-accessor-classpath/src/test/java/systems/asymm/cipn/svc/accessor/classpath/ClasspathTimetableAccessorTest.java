/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import java.util.Set;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads the bundled per-year {@code timetable/<year>/<cc>/} tree for a fixed year. */
class ClasspathTimetableAccessorTest {

    private final ClasspathTimetableAccessor accessor = new ClasspathTimetableAccessor(2026);

    @Test
    void enumeratesCoveredCountriesFromTheManifest() {
        assertEquals(Set.of("LK", "MY", "CY", "BG", "BA", "SG", "XK", "HU", "UA", "KZ", "PT", "AL", "DZ"), accessor.countries());
    }

    @Test
    void readsEveryZoneIndex() {
        assertEquals(13, accessor.zones("LK").size());
        assertEquals(60, accessor.zones("MY").size());
        assertEquals(1, accessor.zones("CY").size());
        assertEquals(1, accessor.zones("BG").size());
        assertEquals(1, accessor.zones("BA").size());
        assertEquals(1, accessor.zones("SG").size());
        assertEquals(1, accessor.zones("XK").size());
        assertEquals(1, accessor.zones("HU").size());
        assertEquals(1, accessor.zones("UA").size());
        assertEquals(3, accessor.zones("KZ").size());
        assertEquals(1, accessor.zones("PT").size());
        assertEquals(1, accessor.zones("AL").size());
        assertEquals(1, accessor.zones("DZ").size());
    }

    @Test
    void readsAnnualTableStampedWithItsYear() {
        AuthorityTimetable table = accessor.timetable("LK", "01").orElseThrow();
        assertEquals("ACJU", table.authority());
        assertEquals("All Ceylon Jamiyyathul Ulama", table.authorityName());
        assertEquals(2026, table.year());
        TimetableRow first = table.rows().get(0);
        assertEquals(1, first.month());
        assertEquals(1, first.day());
        assertEquals(300, first.fajr());
        assertEquals(735, first.solarNoon(), "solar noon (sunrise/sunset midpoint) is enriched into the tables");
    }

    @Test
    void readsAMalaysianTable() {
        AuthorityTimetable table = accessor.timetable("MY", "WLY01").orElseThrow();
        assertEquals(2026, table.year());
        assertFalse(table.rows().isEmpty());
    }

    @Test
    void unknownZoneOrCountryIsEmpty() {
        assertTrue(accessor.timetable("LK", "99").isEmpty());
        assertTrue(accessor.timetable("GB", "01").isEmpty());
        assertFalse(accessor.countries().contains("GB"));
    }

    @Test
    void yearWithNoDataEnumeratesEmpty() {
        assertTrue(new ClasspathTimetableAccessor(1999).countries().isEmpty());
    }
}
