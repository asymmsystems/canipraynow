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
import systems.asymm.cipn.fw.contract.access.CountryAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.CountryDefaults;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.EventOffsets;
import systems.asymm.cipn.fw.contract.data.HighLatitudeRule;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Rounding;
import systems.asymm.cipn.fw.contract.data.Shafaq;
import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;
import java.time.Instant;
import java.time.LocalDate;
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
            "01", "Colombo, Gampaha, Kalutara", "https://www.acju.lk/prayer-times/", 2026,
            List.of(new TimetableRow(1, 1, 300, 382, 735, 937, 1087, 1161, 734)));

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

    /** Country-defaults double: a method phrase + key for every country, an authority for LK, with LK's
     *  redistribution answer parameterised so the redistribution flag can be exercised. */
    private static CountryAccessor countries(String lkRedistributable) {
        return cc -> new CountryDefaults(
                cc, "LK".equals(cc) ? "All Ceylon Jamiyyathul Ulama" : "",
                "the Muslim World League convention", "MuslimWorldLeague", "Shāfiʿī",
                "LK".equals(cc) ? lkRedistributable : "unknown", "");
    }

    private SiteGenerationManager manager() {
        return manager(false, countries("unknown"));
    }

    private SiteGenerationManager manager(boolean enforceRedistribution, CountryAccessor countryAccessor) {
        CityAccessor cities = () -> List.of(COLOMBO, LONDON);
        ZoneResolverProvider resolvers = cc -> (lat, lng) -> "LK".equals(cc) ? Optional.of("01") : Optional.empty();
        NeighbourEngine neighbours = (origin, candidates, count) -> List.of();
        CalculationParameters params = new CalculationParameters("stub", 18.0, 18.0, 0.0, 0.0,
                Madhab.SHAFI, HighLatitudeRule.TWILIGHT_ANGLE, Rounding.NEAREST, Shafaq.GENERAL,
                new EventOffsets(0, 0, 0, 0, 0, 0));
        AstronomyEngine engine = new AstronomyEngine() {
            @Override
            public DayEvents compute(Coordinates c, LocalDate d, CalculationParameters p) {
                return new DayEvents(
                        Instant.parse("2026-01-01T05:00:00Z"), Instant.parse("2026-01-01T08:00:00Z"),
                        Instant.parse("2026-01-01T12:00:00Z"), Instant.parse("2026-01-01T14:00:00Z"),
                        Instant.parse("2026-01-01T16:00:00Z"), Instant.parse("2026-01-01T16:00:00Z"),
                        Instant.parse("2026-01-01T18:00:00Z"));
            }
            @Override
            public Instant solarNoon(Coordinates c, LocalDate d) {
                return Instant.parse("2026-01-01T12:00:00Z");
            }
        };
        CalculationMethodAccessor methods = new CalculationMethodAccessor() {
            @Override
            public Optional<CalculationParameters> parameters(String key, Madhab madhab) {
                return Optional.of(params);
            }
            @Override
            public CalculationParameters parametersOrDefault(String key, Madhab madhab) {
                return params;
            }
            @Override
            public Set<String> methodKeys() {
                return Set.of();
            }
        };
        return new DefaultSiteGenerationManager(
                cities, countryAccessor, TIMETABLES, content, resolvers, neighbours,
                engine, methods, 2026, enforceRedistribution);
    }

    @Test
    void bakesTableForTableCityAndSkipsComputedCountry() {
        SiteGenerationResult result = manager().generate();

        assertEquals(2, result.pagesWritten());
        assertEquals(1, result.pagesWithTimetable());

        String colombo = written.get("sri-lanka/colombo.html");
        assertTrue(colombo.contains("generated_by=SiteGenerator"));
        assertTrue(colombo.contains("city_method=the Muslim World League convention"),
                "country method phrase should be in the front-matter");
        assertTrue(colombo.contains("city_madhhab=Shāfiʿī"));
        assertTrue(colombo.contains("All Ceylon Jamiyyathul Ulama annual timetable"));
        assertTrue(colombo.contains("05:00"), "Fajr 300 min should render as 05:00");

        String london = written.get("united-kingdom/london.html");
        assertTrue(london.contains("generated_by=SiteGenerator"));
        assertTrue(london.contains("data-computed=\"true\""),
                "GB (computed) now bakes a computed annual table for SEO (ADR-0009)");
        assertTrue(london.contains("<tr data-md=\"1-1\""), "the computed table has day rows");
    }

    @Test
    void reportsSweptCount() {
        sweptToReturn = 7;
        assertEquals(7, manager().generate().stalePagesSwept());
    }

    @Test
    void enforcedRedistributionSkipsTableNotClearedForReuse() {
        // Flag on + LK redistributable="unknown": the authority table is withheld, LK bakes computed.
        SiteGenerationResult result = manager(true, countries("unknown")).generate();

        assertEquals(2, result.pagesWritten());
        assertEquals(0, result.pagesWithTimetable());
        String colombo = written.get("sri-lanka/colombo.html");
        assertFalse(colombo.contains("All Ceylon Jamiyyathul Ulama annual timetable"),
                "the authority table must not be baked when redistribution is enforced");
        assertTrue(colombo.contains("data-computed=\"true\""),
                "but the city still bakes a computed table so no page is left without one");
    }

    @Test
    void enforcedRedistributionKeepsTableClearedForReuse() {
        // Flag on + LK redistributable="yes": the table is still baked.
        assertEquals(1, manager(true, countries("yes")).generate().pagesWithTimetable());
    }
}
