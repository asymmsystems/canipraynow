/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.CountryAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.CountryDefaults;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Neighbour;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;
import systems.asymm.cipn.fw.contract.engine.NeighbourEngine;
import systems.asymm.cipn.fw.contract.engine.ZoneResolver;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;

/**
 * Default {@link SiteGenerationManager}: orchestrates S2. For each page-city it finds its
 * neighbours (engine), resolves the covering zone (engine) and reads that zone's baked table
 * (accessor, E1), then hands the domain objects to the renderer and writes the result
 * (accessor). Pure orchestration - no business logic, no presentation, no framework
 * annotations; wired externally.
 *
 * @see SiteGenerationManager
 */
public class DefaultSiteGenerationManager implements SiteGenerationManager {

    /** Marker every generated file carries, so a prior run can be swept. */
    static final String MARKER = "generated_by=SiteGenerator";

    private static final int NEARBY_COUNT = 5;

    /** Sentinel authority code for a baked computed table, so the renderer frames it as computed. */
    static final String COMPUTED = "COMPUTED";
    private static final String METHODOLOGY_URL = "/methodology.html";

    private final CityAccessor cityAccessor;
    private final CountryAccessor countryAccessor;
    private final TimetableAccessor timetableAccessor;
    private final ContentAccessor contentAccessor;
    private final ZoneResolverProvider resolverProvider;
    private final NeighbourEngine neighbourEngine;
    private final AstronomyEngine astronomyEngine;
    private final CalculationMethodAccessor methodAccessor;
    private final int year;
    private final boolean enforceRedistribution;
    private final CityPageRenderer renderer = new CityPageRenderer();

    public DefaultSiteGenerationManager(CityAccessor cityAccessor, CountryAccessor countryAccessor,
                                        TimetableAccessor timetableAccessor, ContentAccessor contentAccessor,
                                        ZoneResolverProvider resolverProvider, NeighbourEngine neighbourEngine,
                                        AstronomyEngine astronomyEngine, CalculationMethodAccessor methodAccessor,
                                        int year, boolean enforceRedistribution) {
        this.cityAccessor = cityAccessor;
        this.countryAccessor = countryAccessor;
        this.timetableAccessor = timetableAccessor;
        this.contentAccessor = contentAccessor;
        this.resolverProvider = resolverProvider;
        this.neighbourEngine = neighbourEngine;
        this.astronomyEngine = astronomyEngine;
        this.methodAccessor = methodAccessor;
        this.year = year;
        this.enforceRedistribution = enforceRedistribution;
    }

    @Override
    public SiteGenerationResult generate() {
        int swept = contentAccessor.sweep(MARKER);
        List<City> allCities = cityAccessor.all();
        Set<String> tableCountries = tableCountries();

        int written = 0;
        int withTimetable = 0;
        for (City city : allCities) {
            if (!city.page()) {
                continue;
            }
            CountryDefaults country = countryAccessor.defaults(city.cc());
            AuthorityTimetable timetable = timetableFor(city, tableCountries);
            if (timetable != null) {
                withTimetable++;
            } else {
                // Every page bakes the whole annual table (ADR-0009): where no authority table
                // covers the city, compute it with the shared astronomy engine (E2) so search
                // engines and no-JS readers still get the full year, not a JS-only render.
                timetable = computedTable(city, country);
            }
            List<Neighbour> nearby = neighbourEngine.nearest(city, allCities, NEARBY_COUNT);
            contentAccessor.write(
                    city.countrySlug() + "/" + city.slug() + ".html",
                    renderer.render(MARKER, city, country, nearby, timetable));
            written++;
        }
        return new SiteGenerationResult(written, swept, withTimetable);
    }

    /**
     * The countries whose baked authority table we will publish. Normally every country with a
     * table; when redistribution is enforced, only those the register marks {@code redistributable
     * ="yes"} (a country not cleared for reuse falls back to computed times). The flag defaults to
     * off, so we republish every held table regardless of the register's redistribution answer -
     * see ADR-0017 / T-31.
     */
    private Set<String> tableCountries() {
        Set<String> withTable = timetableAccessor.countries();
        if (!enforceRedistribution) {
            return withTable;
        }
        return withTable.stream()
                .filter(cc -> "yes".equalsIgnoreCase(countryAccessor.defaults(cc).redistributable()))
                .collect(Collectors.toSet());
    }

    /** The baked authority table for a city, or {@code null} when its country has no table or no zone matches. */
    private AuthorityTimetable timetableFor(City city, Set<String> tableCountries) {
        if (!tableCountries.contains(city.cc())) {
            return null;
        }
        ZoneResolver resolver = resolverProvider.resolverFor(city.cc());
        return resolver.resolve(city.lat(), city.lng())
                .flatMap(zoneId -> timetableAccessor.timetable(city.cc(), zoneId))
                .orElse(null);
    }

    /**
     * Compute a city's full annual table with the shared astronomy engine, using its country's
     * registered method and madhhab - the same calculation the client runs, baked once. Returned as
     * an {@link AuthorityTimetable} with the {@link #COMPUTED} sentinel so the renderer frames it as
     * a computed (not authority-published) table.
     */
    private AuthorityTimetable computedTable(City city, CountryDefaults country) {
        CalculationParameters params = methodAccessor.parametersOrDefault(country.methodKey(), madhabOf(country.madhhab()));
        Coordinates coordinates = new Coordinates(city.lat(), city.lng());
        ZoneId tz = ZoneId.of(city.tz());
        List<TimetableRow> rows = new ArrayList<>(366);
        for (LocalDate date = LocalDate.of(year, 1, 1); date.getYear() == year; date = date.plusDays(1)) {
            DayEvents e = astronomyEngine.compute(coordinates, date, params);
            rows.add(new TimetableRow(date.getMonthValue(), date.getDayOfMonth(),
                    minute(e.fajr(), tz), minute(e.sunrise(), tz), minute(e.dhuhr(), tz),
                    minute(e.asr(), tz), minute(e.maghrib(), tz), minute(e.isha(), tz),
                    minute(astronomyEngine.solarNoon(coordinates, date), tz)));
        }
        return new AuthorityTimetable(COMPUTED, country.method(), METHODOLOGY_URL,
                city.name(), city.name(), METHODOLOGY_URL, year, rows, null);
    }

    /** Local-clock minutes from midnight for an instant, rounded to the nearest minute. */
    private static int minute(Instant instant, ZoneId tz) {
        LocalTime local = instant.atZone(tz).toLocalTime();
        return (int) Math.round(local.toSecondOfDay() / 60.0) % (24 * 60);
    }

    /** Only the Ḥanafī school changes the ʿAsr shadow factor (2x); every other school uses 1x. */
    private static Madhab madhabOf(String madhhab) {
        return madhhab != null && madhhab.toLowerCase(Locale.ROOT).contains("anaf") ? Madhab.HANAFI : Madhab.SHAFI;
    }
}
