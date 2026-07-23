/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
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
 * <p>Pure and I/O-free. Always resolves to some zone unless the zone list is empty.
 */
public class NearestCentroidZoneResolver implements ZoneResolver {

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
        return Optional.ofNullable(nearest);
    }
}
