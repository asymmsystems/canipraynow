/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.geo;

import java.util.List;

import systems.asymm.cipn.fw.contract.data.GeoPoint;

/**
 * Pure planar/spherical geometry: even-odd point-in-polygon and great-circle distance.
 * A stateless utility (no I/O, no business logic, no domain decision) - engines such as
 * the zone resolvers call it; they decide "which zone", it only does the maths.
 */
public final class Geometry {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private Geometry() {
    }

    /**
     * Whether a point lies inside a polygon ring by the even-odd (ray-casting) rule.
     * Treats the ring as closed (the last vertex joins the first). Points exactly on an
     * edge are resolved consistently but their inclusion is not guaranteed.
     *
     * @param ring the ring vertices, in order
     * @param lat  query latitude
     * @param lng  query longitude
     * @return true if the point is inside the ring
     */
    public static boolean ringContains(List<GeoPoint> ring, double lat, double lng) {
        int n = ring.size();
        if (n < 3) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double yi = ring.get(i).lat();
            double xi = ring.get(i).lng();
            double yj = ring.get(j).lat();
            double xj = ring.get(j).lng();
            boolean crosses = (yi > lat) != (yj > lat)
                    && lng < (xj - xi) * (lat - yi) / (yj - yi) + xi;
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Great-circle distance in kilometres.
     *
     * @return the distance between the two points, in km
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
    }
}
