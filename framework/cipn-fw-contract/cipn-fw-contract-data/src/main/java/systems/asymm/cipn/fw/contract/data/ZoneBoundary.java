/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

import java.util.List;

/**
 * The geographic coverage of one authority zone: the zone id and the polygon(s) that
 * make up its area (an administrative district group can be several disjoint polygons).
 * A location belongs to the zone if it lies inside any of these polygons.
 *
 * <p>This is the accurate coverage model the polygon zone resolver matches against;
 * the per-country polygon data is sourced at build time (S1).
 *
 * @param zoneId   the authority's zone identifier
 * @param polygons the polygons whose union is the zone's area
 */
public record ZoneBoundary(String zoneId, List<GeoPolygon> polygons) {
}
