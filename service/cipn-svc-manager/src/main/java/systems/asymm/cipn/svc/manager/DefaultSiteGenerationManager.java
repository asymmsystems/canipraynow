/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import java.util.Optional;
import java.util.Set;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.engine.ZoneResolver;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;

/**
 * Default {@link SiteGenerationManager}: orchestrates S2. For each page-city it resolves
 * the covering zone (engine), reads that zone's baked table (accessor, E1), renders it
 * (template), and writes the page (accessor). Zero business logic and no framework
 * annotations - wired externally.
 *
 * @see SiteGenerationManager
 */
public class DefaultSiteGenerationManager implements SiteGenerationManager {

    /** Marker every generated stub carries, so a prior run can be swept. */
    static final String MARKER = "generated_by=SiteGenerator";

    private final CityAccessor cityAccessor;
    private final TimetableAccessor timetableAccessor;
    private final ContentAccessor contentAccessor;
    private final ZoneResolverProvider resolverProvider;
    private final TemplateRenderer renderer;

    public DefaultSiteGenerationManager(CityAccessor cityAccessor, TimetableAccessor timetableAccessor,
                                        ContentAccessor contentAccessor, ZoneResolverProvider resolverProvider) {
        this.cityAccessor = cityAccessor;
        this.timetableAccessor = timetableAccessor;
        this.contentAccessor = contentAccessor;
        this.resolverProvider = resolverProvider;
        this.renderer = new TemplateRenderer();
    }

    @Override
    public SiteGenerationResult generate() {
        int swept = contentAccessor.sweep(MARKER);
        Set<String> tableCountries = timetableAccessor.countries();

        int written = 0;
        int withTimetable = 0;
        for (City city : cityAccessor.all()) {
            if (!city.page()) {
                continue;
            }
            String timetableHtml = timetableHtml(city, tableCountries);
            if (!timetableHtml.isEmpty()) {
                withTimetable++;
            }
            contentAccessor.write(city.countrySlug() + "/" + city.slug() + ".html", stub(city, timetableHtml));
            written++;
        }
        return new SiteGenerationResult(written, swept, withTimetable);
    }

    /** The baked authority table for a city, or empty when its country has no table or no zone matches. */
    private String timetableHtml(City city, Set<String> tableCountries) {
        if (!tableCountries.contains(city.cc())) {
            return "";
        }
        ZoneResolver resolver = resolverProvider.resolverFor(city.cc());
        Optional<String> zoneId = resolver.resolve(city.lat(), city.lng());
        if (zoneId.isEmpty()) {
            return "";
        }
        Optional<AuthorityTimetable> table = timetableAccessor.timetable(city.cc(), zoneId.get());
        return table.map(t -> renderer.render("timetable.ftl", TimetableViewModel.of(t))).orElse("");
    }

    private static String stub(City city, String timetableHtml) {
        return """
                title=Can I pray now in %1$s?
                type=city
                status=published
                %2$s
                city_name=%1$s
                country_name=%3$s
                city_lat=%4$s
                city_lng=%5$s
                city_tz=%6$s
                city_cc=%7$s
                ~~~~~~
                %8$s""".formatted(
                city.name(), MARKER, city.country(),
                city.lat(), city.lng(), city.tz(), city.cc(), timetableHtml);
    }
}
