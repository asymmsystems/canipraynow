/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import java.util.List;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads the bundled per-year {@code timetable/<year>/<cc>/} tree for a fixed year. */
class ClasspathTimetableAccessorTest {

    private final ClasspathTimetableAccessor accessor = new ClasspathTimetableAccessor(2026);

    @Test
    void enumeratesOnlyCountriesWithAZoneIndex() {
        assertEquals(java.util.Set.of("LK"), accessor.countries());
    }

    @Test
    void readsZoneIndexWithCentroids() {
        List<TimetableZone> zones = accessor.zones("LK");
        assertEquals(2, zones.size());
        assertTrue(zones.stream().anyMatch(z -> z.id().equals("01")));
        assertTrue(zones.stream().anyMatch(z -> z.id().equals("07")));
    }

    @Test
    void readsAnnualTableStampedWithItsYearAndSolarNoon() {
        AuthorityTimetable table = accessor.timetable("LK", "01").orElseThrow();
        assertEquals("ACJU", table.authority());
        assertEquals("All Ceylon Jamiyyathul Ulama", table.authorityName());
        assertEquals(2026, table.year());
        TimetableRow first = table.rows().get(0);
        assertEquals(1, first.month());
        assertEquals(1, first.day());
        assertEquals(300, first.fajr());
        assertEquals(734, first.solarNoon());
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
