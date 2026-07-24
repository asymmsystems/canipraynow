/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.engine.NeighbourEngine;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-tests the manager's orchestration with in-memory test doubles for every port - no real
 * accessors or engines, so no cross-service dependency. The internal (FreeMarker) rendering is
 * exercised for real. The wire-everything integration test lives at the app level.
 */
class DefaultSiteGenerationManagerTest {

    private static final City COLOMBO =
            new City("Colombo", "Sri Lanka", "LK", "colombo", "sri-lanka", 6.9271, 79.8612, "Asia/Colombo", true);
    private static final City LONDON =
            new City("London", "United Kingdom", "GB", "london", "united-kingdom", 51.5074, -0.1278, "Europe/London", true);

    private static final AuthorityTimetable ACJU = new AuthorityTimetable(
            "ACJU", "All Ceylon Jamiyyathul Ulama", "https://www.acju.lk/prayer-times/",
            "01", "Colombo, Gampaha, Kalutara", "https://www.acju.lk/prayer-times/",
            List.of(new TimetableRow(1, 1, 300, 382, 735, 937, 1087, 1161)));

    /** Timetable accessor double: LK has a table (zone 01 -> ACJU), everything else has none. */
    private static final TimetableAccessor TIMETABLES = new TimetableAccessor() {
        @Override
        public Set<String> countries() {
            return Set.of("LK");
        }

        @Override
        public List<TimetableZone> zones(String cc) {
            return "LK".equals(cc) ? List.of(new TimetableZone("01", "Colombo", 6.9271, 79.8612)) : List.of();
        }

        @Override
        public Optional<AuthorityTimetable> timetable(String cc, String zoneId) {
            return "LK".equals(cc) && "01".equals(zoneId) ? Optional.of(ACJU) : Optional.empty();
        }
    };

    private final Map<String, String> written = new HashMap<>();
    private int sweptToReturn = 0;

    private final ContentAccessor content = new ContentAccessor() {
        @Override
        public void write(String relativePath, String contentText) {
            written.put(relativePath, contentText);
        }

        @Override
        public int sweep(String marker) {
            return sweptToReturn;
        }
    };

    private SiteGenerationManager manager() {
        CityAccessor cities = () -> List.of(COLOMBO, LONDON);
        ZoneResolverProvider resolvers = cc -> (lat, lng) -> "LK".equals(cc) ? Optional.of("01") : Optional.empty();
        NeighbourEngine neighbours = (origin, candidates, count) -> List.of();
        return new DefaultSiteGenerationManager(cities, TIMETABLES, content, resolvers, neighbours);
    }

    @Test
    void bakesTableForTableCityAndSkipsComputedCountry() {
        SiteGenerationResult result = manager().generate();

        assertEquals(2, result.pagesWritten());
        assertEquals(1, result.pagesWithTimetable());

        String colombo = written.get("sri-lanka/colombo.html");
        assertTrue(colombo.contains("generated_by=SiteGenerator"));
        assertTrue(colombo.contains("All Ceylon Jamiyyathul Ulama annual timetable"));
        assertTrue(colombo.contains("05:00"), "Fajr 300 min should render as 05:00");

        String london = written.get("united-kingdom/london.html");
        assertTrue(london.contains("generated_by=SiteGenerator"));
        assertFalse(london.contains("authority-timetable"), "GB has no baked table");
    }

    @Test
    void reportsSweptCount() {
        sweptToReturn = 7;
        assertEquals(7, manager().generate().stalePagesSwept());
    }
}
