/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import java.util.List;
import java.util.Optional;

import systems.asymm.cipn.fw.common.geo.Geometry;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.engine.ZoneResolver;

/**
 * Fallback {@link ZoneResolver}: matches a location to the zone whose centroid is nearest
 * by great-circle distance. Less accurate than {@link PolygonZoneResolver} near zone
 * boundaries, but needs only the centroids we already hold - the working strategy until
 * per-country coverage polygons are sourced. Mirrors the site's current snap rule.
 *
 * <p>Pure and I/O-free. Resolves to the nearest zone only when that zone is within
 * {@link #MAX_SNAP_KM} of the location; a location farther than that from every zone falls back to
 * computed times (empty result). This keeps a single-city authority table (e.g. Algiers) from being
 * served to a far-away city of the same country while every real served city today sits well inside
 * the radius (the farthest is ~75 km).
 */
public class NearestCentroidZoneResolver implements ZoneResolver {

    /**
     * Maximum great-circle distance from a location to a zone centroid for that zone's table to
     * apply. Calibrated above the farthest current served city (~75 km) and well below the gap to
     * an out-of-area city (hundreds of km), so it changes no existing snap and only excludes cities
     * a national table was never meant to cover.
     */
    static final double MAX_SNAP_KM = 150.0;

    private final List<TimetableZone> zones;

    /**
     * @param zones the authority's zones, each with a representative centroid
     */
    public NearestCentroidZoneResolver(List<TimetableZone> zones) {
        this.zones = List.copyOf(zones);
    }

    @Override
    public Optional<String> resolve(double latitude, double longitude) {
        String nearest = null;
        double nearestKm = Double.MAX_VALUE;
        for (TimetableZone zone : zones) {
            double km = Geometry.haversineKm(latitude, longitude, zone.lat(), zone.lng());
            if (km < nearestKm) {
                nearestKm = km;
                nearest = zone.id();
            }
        }
        return nearestKm <= MAX_SNAP_KM ? Optional.ofNullable(nearest) : Optional.empty();
    }
}
