/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

import java.util.List;

/**
 * An authority's published annual timetable for one zone: the authority's identity and
 * source, the zone, and the day-by-day rows (its own numbers, read not computed - the
 * E1 path). This is what S2 bakes into a city page for authority-table countries.
 *
 * @param authority     short authority code (e.g. {@code "ACJU"}, {@code "JAKIM"})
 * @param authorityName the authority's full name
 * @param authorityUrl  the authority's published-times source URL
 * @param zoneId        the zone this table is for
 * @param zoneName      the districts the zone covers
 * @param source        the specific source the rows were scraped from
 * @param rows          the annual rows, one per day, in calendar order
 */
public record AuthorityTimetable(
        String authority,
        String authorityName,
        String authorityUrl,
        String zoneId,
        String zoneName,
        String source,
        List<TimetableRow> rows) {
}
