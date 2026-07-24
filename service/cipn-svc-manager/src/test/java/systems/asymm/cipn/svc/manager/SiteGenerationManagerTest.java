/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;
import systems.asymm.cipn.svc.accessor.file.FileCityAccessor;
import systems.asymm.cipn.svc.accessor.file.FileContentAccessor;
import systems.asymm.cipn.svc.accessor.file.FileTimetableAccessor;
import systems.asymm.cipn.svc.engine.NearestCentroidZoneResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end S2 orchestration wired from real file accessors, the centroid zone resolver,
 * and FreeMarker rendering: an authority-table city gets its baked table; a computed-only
 * country gets a page without one.
 */
class SiteGenerationManagerTest {

    private Path fixtures() throws Exception {
        return Path.of(getClass().getResource("/gen").toURI());
    }

    private SiteGenerationManager manager(Path fixtures, Path out) {
        TimetableAccessor timetables = new FileTimetableAccessor(fixtures.resolve("timetables"));
        CityAccessor cities = new FileCityAccessor(fixtures.resolve("cities.xml"));
        ContentAccessor content = new FileContentAccessor(out);
        ZoneResolverProvider provider = cc -> new NearestCentroidZoneResolver(timetables.zones(cc));
        return new DefaultSiteGenerationManager(cities, timetables, content, provider);
    }

    @Test
    void bakesAuthorityTableIntoTableCityAndSkipsComputedCountry(@TempDir Path out) throws Exception {
        SiteGenerationResult result = manager(fixtures(), out).generate();

        assertEquals(2, result.pagesWritten());
        assertEquals(1, result.pagesWithTimetable());

        String colombo = Files.readString(out.resolve("sri-lanka/colombo.html"));
        assertTrue(colombo.contains(DefaultSiteGenerationManager.MARKER));
        assertTrue(colombo.contains("All Ceylon Jamiyyathul Ulama annual timetable"));
        assertTrue(colombo.contains("Colombo, Gampaha, Kalutara"));
        assertTrue(colombo.contains("05:00"), "Fajr day 1 (300 min) should render as 05:00");
        assertTrue(colombo.contains("<summary>January</summary>"));
        assertTrue(colombo.contains("<summary>June</summary>"));

        String london = Files.readString(out.resolve("united-kingdom/london.html"));
        assertTrue(london.contains(DefaultSiteGenerationManager.MARKER));
        assertFalse(london.contains("authority-timetable"), "GB has no baked table");
    }

    @Test
    void sweepRemovesPriorGeneratedPages(@TempDir Path out) throws Exception {
        manager(fixtures(), out).generate();
        // A second run sweeps the first run's pages, then rewrites them: net count is stable.
        SiteGenerationResult second = manager(fixtures(), out).generate();
        assertEquals(2, second.stalePagesSwept());
        assertEquals(2, second.pagesWritten());
    }
}
