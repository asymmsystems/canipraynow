/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import java.util.List;
import java.util.Optional;

import systems.asymm.cipn.fw.common.geo.Geometry;
import systems.asymm.cipn.fw.contract.data.GeoPolygon;
import systems.asymm.cipn.fw.contract.data.ZoneBoundary;
import systems.asymm.cipn.fw.contract.engine.ZoneResolver;

/**
 * Accurate {@link ZoneResolver}: matches a location to the zone whose district polygon(s)
 * contain it (point-in-polygon). The per-country coverage polygons are supplied at
 * construction (read by an accessor, sourced at build time by S1).
 *
 * <p>Pure and I/O-free. Returns empty when no zone covers the location (e.g. offshore or
 * a gap in the coverage); callers may then fall back to a centroid resolver.
 *
 * @see NearestCentroidZoneResolver
 */
public class PolygonZoneResolver implements ZoneResolver {

    private final List<ZoneBoundary> boundaries;

    /**
     * @param boundaries the authority's zone coverage polygons
     */
    public PolygonZoneResolver(List<ZoneBoundary> boundaries) {
        this.boundaries = List.copyOf(boundaries);
    }

    @Override
    public Optional<String> resolve(double latitude, double longitude) {
        for (ZoneBoundary boundary : boundaries) {
            for (GeoPolygon polygon : boundary.polygons()) {
                if (Geometry.ringContains(polygon.ring(), latitude, longitude)) {
                    return Optional.of(boundary.zoneId());
                }
            }
        }
        return Optional.empty();
    }
}
