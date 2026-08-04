/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.scrape;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;

/**
 * Fills the {@code solar-noon} (zenith) column on scraped rows, with authority-first priority
 * (ADR-0017): an authority's own announced value wins, and astronomical computation is the
 * fallback. Solar noon is method- and madhab-independent (the meridian crossing), so it is the
 * same for every method.
 *
 * <p>Priority per row:
 * <ol>
 *   <li>Solar noon already present ({@code > 0}) - the authority announced it (the scraper read an
 *       exact value from the feed): kept as-is.</li>
 *   <li>Otherwise computed: the engine's astronomical transit (UTC), expressed in the zone's local
 *       clock. The zone's UTC offset is recovered from the authority's own data, so no timezone is
 *       stored anywhere.</li>
 * </ol>
 *
 * <p>The offset is recovered from the published <em>dhuhr</em> versus the computed transit, not from
 * sunrise: dhuhr and transit are both longitude-only (a zone's stored centroid, however far from the
 * authority's reference town, has the same longitude sensitivity), whereas sunrise adds a latitude
 * term that biases large or mountainous zones enough to snap to the wrong grid cell. The only gap
 * between dhuhr and transit is the authority's small dhuhr safety margin ({@code dhuhr = transit +
 * margin}, e.g. JAKIM's +2 min), which is absorbed by snapping the offset to the 15-minute timezone
 * grid. The offset is derived once per zone (median across the year, immune to daily jitter) and
 * applied to every day. (A zone that observed DST would need a two-cluster split; none of today's
 * authorities do.)
 *
 * <p>An authority-announced solar-noon <em>offset</em> rule (should one ever exist) would slot in as
 * a third case from the authority's E2 config; no such authority exists today, so it is not built.
 */
final class SolarNoonEnricher {

    private static final int TZ_GRID_MINUTES = 15;

    private final AstronomyEngine engine;

    SolarNoonEnricher(AstronomyEngine engine) {
        this.engine = engine;
    }

    /** Return a copy of the table with solar noon filled wherever the authority did not announce it. */
    AuthorityTimetable enrich(AuthorityTimetable table, TimetableZone zone) {
        Coordinates coords = new Coordinates(zone.lat(), zone.lng());
        List<TimetableRow> rows = table.rows();

        // One transit per row (UTC seconds from the day's UTC midnight), reused for both the offset
        // derivation and the final value. Announced rows are left untouched (transit unused).
        long[] transitSec = new long[rows.size()];
        List<Double> offsets = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            TimetableRow r = rows.get(i);
            if (r.solarNoon() > 0) {
                continue;
            }
            LocalDate date = LocalDate.of(table.year(), r.month(), r.day());
            transitSec[i] = secondsFromUtcMidnight(engine.solarNoon(coords, date), date);
            offsets.add(((long) r.dhuhr() * 60L - transitSec[i]) / 60.0);
        }
        long offsetMinutes = snapToGrid(median(offsets));

        List<TimetableRow> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            TimetableRow r = rows.get(i);
            if (r.solarNoon() > 0) {
                out.add(r);
                continue;
            }
            long localSec = transitSec[i] + offsetMinutes * 60L;
            int solarNoon = (int) Math.floorMod(Math.round(localSec / 60.0), 1440L);
            out.add(new TimetableRow(r.month(), r.day(),
                    r.fajr(), r.sunrise(), r.dhuhr(), r.asr(), r.maghrib(), r.isha(), solarNoon));
        }
        return new AuthorityTimetable(table.authority(), table.authorityName(), table.authorityUrl(),
                table.zoneId(), table.zoneName(), table.source(), table.year(), List.copyOf(out));
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static long snapToGrid(double offsetMinutes) {
        return Math.round(offsetMinutes / TZ_GRID_MINUTES) * (long) TZ_GRID_MINUTES;
    }

    /** Signed seconds from UTC midnight of {@code date} to {@code instant} (negative if the prior UTC day). */
    private static long secondsFromUtcMidnight(Instant instant, LocalDate date) {
        return Duration.between(date.atStartOfDay(ZoneOffset.UTC).toInstant(), instant).getSeconds();
    }
}
