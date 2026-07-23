/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * One coverage zone of an authority's timetable: an id, the districts it groups, and
 * a representative centroid a location is matched (snapped) to.
 *
 * @param id   the authority's zone identifier (e.g. {@code "01"}, {@code "SGR01"})
 * @param name the districts the zone covers
 * @param lat  centroid latitude in decimal degrees
 * @param lng  centroid longitude in decimal degrees
 */
public record TimetableZone(String id, String name, double lat, double lng) {
}
