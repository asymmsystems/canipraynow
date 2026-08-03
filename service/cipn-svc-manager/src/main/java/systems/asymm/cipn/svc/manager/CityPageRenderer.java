/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.CountryDefaults;
import systems.asymm.cipn.fw.contract.data.Neighbour;

/**
 * Renders a complete city content page - the JBake front-matter plus, for authority-table
 * cities, the baked timetable - from domain objects. Owns all presentation (view-model
 * shaping + the FreeMarker templates), so the manager stays free of any rendering.
 */
final class CityPageRenderer {

    private final TemplateRenderer templateRenderer = new TemplateRenderer();

    /**
     * @param marker    the generated-file marker to stamp into the front-matter (for sweeping)
     * @param city      the city
     * @param country   its country's presentation defaults (authority, method, madhhab)
     * @param nearby    its nearest neighbours (closest first)
     * @param timetable the baked authority timetable, or {@code null} for computed-only cities
     * @return the full content-file text (front-matter + body)
     */
    String render(String marker, City city, CountryDefaults country, List<Neighbour> nearby,
                  AuthorityTimetable timetable) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("marker", marker);
        model.put("city", cityModel(city));
        // Distinct keys: the timetable view-model also has an "authority" (the table's authority).
        model.put("countryAuthority", country.authority());
        model.put("countryMethod", country.method());
        model.put("countryMethodKey", country.methodKey());
        model.put("countryMadhhab", country.madhhab());
        model.put("nearby", nearbyModel(nearby));
        if (timetable != null) {
            model.putAll(TimetableViewModel.of(timetable));
        }
        return templateRenderer.render("city-page.ftl", model);
    }

    private static Map<String, Object> cityModel(City c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.name());
        m.put("country", c.country());
        m.put("lat", c.lat());
        m.put("lng", c.lng());
        m.put("tz", c.tz());
        m.put("cc", c.cc());
        return m;
    }

    private static List<Map<String, Object>> nearbyModel(List<Neighbour> nearby) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Neighbour n : nearby) {
            City c = n.city();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.name());
            m.put("country", c.country());
            m.put("href", "/" + c.countrySlug() + "/" + c.slug() + ".html");
            m.put("km", n.distanceKm());
            out.add(m);
        }
        return out;
    }
}
