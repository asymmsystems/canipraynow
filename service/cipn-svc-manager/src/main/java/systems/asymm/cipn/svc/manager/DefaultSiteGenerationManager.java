/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.CountryAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.CountryDefaults;
import systems.asymm.cipn.fw.contract.data.Neighbour;
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

    private final CityAccessor cityAccessor;
    private final CountryAccessor countryAccessor;
    private final TimetableAccessor timetableAccessor;
    private final ContentAccessor contentAccessor;
    private final ZoneResolverProvider resolverProvider;
    private final NeighbourEngine neighbourEngine;
    private final boolean enforceRedistribution;
    private final CityPageRenderer renderer = new CityPageRenderer();

    public DefaultSiteGenerationManager(CityAccessor cityAccessor, CountryAccessor countryAccessor,
                                        TimetableAccessor timetableAccessor, ContentAccessor contentAccessor,
                                        ZoneResolverProvider resolverProvider, NeighbourEngine neighbourEngine,
                                        boolean enforceRedistribution) {
        this.cityAccessor = cityAccessor;
        this.countryAccessor = countryAccessor;
        this.timetableAccessor = timetableAccessor;
        this.contentAccessor = contentAccessor;
        this.resolverProvider = resolverProvider;
        this.neighbourEngine = neighbourEngine;
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
            AuthorityTimetable timetable = timetableFor(city, tableCountries);
            if (timetable != null) {
                withTimetable++;
            }
            List<Neighbour> nearby = neighbourEngine.nearest(city, allCities, NEARBY_COUNT);
            CountryDefaults country = countryAccessor.defaults(city.cc());
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
}
