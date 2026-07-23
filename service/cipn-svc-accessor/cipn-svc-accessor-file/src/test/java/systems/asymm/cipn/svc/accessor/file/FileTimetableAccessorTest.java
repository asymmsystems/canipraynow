/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the fixture timetable tree under {@code src/test/resources/timetables} and
 * checks the E1 read path: authority metadata, zones and calendar-ordered rows.
 */
class FileTimetableAccessorTest {

    private TimetableAccessor accessor() throws Exception {
        Path root = Path.of(getClass().getResource("/timetables").toURI());
        return new FileTimetableAccessor(root);
    }

    @Test
    void listsCountriesWithATable() throws Exception {
        assertTrue(accessor().countries().contains("LK"));
    }

    @Test
    void readsZonesWithCentroids() throws Exception {
        List<TimetableZone> zones = accessor().zones("LK");
        assertEquals(2, zones.size());
        TimetableZone colombo = zones.get(0);
        assertEquals("01", colombo.id());
        assertEquals("Colombo, Gampaha, Kalutara", colombo.name());
        assertEquals(6.9271, colombo.lat());
    }

    @Test
    void readsAuthorityTimetableWithOrderedRows() throws Exception {
        AuthorityTimetable table = accessor().timetable("LK", "01").orElseThrow();
        assertEquals("ACJU", table.authority());
        assertEquals("All Ceylon Jamiyyathul Ulama", table.authorityName());
        assertEquals("Colombo, Gampaha, Kalutara", table.zoneName());

        List<TimetableRow> rows = table.rows();
        assertEquals(3, rows.size());
        // Keys were declared out of order in the fixture; rows must come back calendar-sorted.
        assertEquals(1, rows.get(0).month());
        assertEquals(1, rows.get(0).day());
        assertEquals(300, rows.get(0).fajr());   // 05:00 local
        assertEquals(1161, rows.get(0).isha());  // 19:21 local
        assertEquals(2, rows.get(1).day());
        assertEquals(12, rows.get(2).month());
        assertEquals(31, rows.get(2).day());
    }

    @Test
    void unknownZoneIsEmpty() throws Exception {
        assertTrue(accessor().timetable("LK", "99").isEmpty());
    }
}
