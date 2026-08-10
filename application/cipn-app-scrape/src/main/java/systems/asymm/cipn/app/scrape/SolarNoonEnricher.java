/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.scrape;

import java.util.ArrayList;
import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Fills the {@code solar-noon} (zenith) column on scraped rows, with authority-first priority
 * (ADR-0017): an authority's own announced value wins, and otherwise it is computed as the midpoint
 * of the authority's published sunrise and sunset (its maghrib).
 *
 * <p>By the symmetry of the sun's daily arc, the midpoint of sunrise and sunset <em>is</em> local
 * solar noon, to within about a minute (a sub-minute intraday-declination wobble plus whole-minute
 * rounding of the two published times). Using it directly needs no timezone, no engine, and no
 * offset recovery, and it is correct by construction under any convention that would otherwise trip
 * a computed approach: it is latitude-independent (a zone's stored centroid does not bias it), free
 * of any dhuhr "temkin" margin (sunrise and sunset carry none), and daylight-saving-correct (both
 * times are in the authority's own clock, so their midpoint follows the DST change day by day).
 *
 * <p>Assumes the authority's maghrib is sunset (true for every Sunni authority we scrape). A Shia
 * authority whose maghrib is an angle after sunset would need its maghrib un-adjusted first.
 *
 * <p>An authority-announced solar-noon <em>offset</em> rule (should one ever exist) would slot in as
 * a third case from the authority's E2 config; no such authority exists today, so it is not built.
 */
final class SolarNoonEnricher {

    /** Return a copy of the table with solar noon filled wherever the authority did not announce it. */
    AuthorityTimetable enrich(AuthorityTimetable table) {
        List<TimetableRow> out = new ArrayList<>(table.rows().size());
        for (TimetableRow r : table.rows()) {
            if (r.solarNoon() > 0) {
                out.add(r);
                continue;
            }
            int solarNoon = (int) Math.round((r.sunrise() + r.maghrib()) / 2.0);
            out.add(new TimetableRow(r.month(), r.day(),
                    r.fajr(), r.sunrise(), r.dhuhr(), r.asr(), r.maghrib(), r.isha(), solarNoon));
        }
        return new AuthorityTimetable(table.authority(), table.authorityName(), table.authorityUrl(),
                table.zoneId(), table.zoneName(), table.source(), table.year(), List.copyOf(out));
    }
}
