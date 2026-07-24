/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.access;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

/**
 * Accessor contract for the baked authority timetables (the E1 read path): an
 * authority's own published prayer times, per zone, for countries where we hold an
 * extracted table (currently LK/ACJU and MY/JAKIM).
 *
 * <p>Named by CONSUMED TYPE (authority timetables). Reads only, zero business logic:
 * it returns the zones and each zone's annual rows; picking the zone for a location
 * (snap-to-nearest-centroid) is the caller's concern. The location and format are
 * implementation details ({@code ClasspathTimetableAccessor} reads bundled JSON today).
 *
 * @see Accessor
 */
public interface TimetableAccessor extends Accessor {

    /**
     * The country codes for which an extracted timetable is available.
     *
     * @return ISO-3166 alpha-2 codes (e.g. {@code "LK"}, {@code "MY"})
     */
    Set<String> countries();

    /**
     * The coverage zones for a country, each with a centroid to snap a location to.
     *
     * @param countryCode ISO-3166 alpha-2 code
     * @return the zones, or an empty list if the country has no extracted table
     */
    List<TimetableZone> zones(String countryCode);

    /**
     * The annual timetable for one zone.
     *
     * @param countryCode ISO-3166 alpha-2 code
     * @param zoneId      the zone identifier
     * @return the timetable, or empty if the country or zone is unknown
     */
    Optional<AuthorityTimetable> timetable(String countryCode, String zoneId);
}
