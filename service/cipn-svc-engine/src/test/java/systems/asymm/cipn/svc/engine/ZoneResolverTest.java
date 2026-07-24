/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import systems.asymm.cipn.fw.contract.data.GeoPoint;
import systems.asymm.cipn.fw.contract.data.GeoPolygon;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.data.ZoneBoundary;
import systems.asymm.cipn.fw.contract.engine.ZoneResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Point-in-polygon zone matching, and the nearest-centroid fallback.
 */
class ZoneResolverTest {

    // Two unit squares: A = [0,2]x[0,2], B = [2,4]x[0,2] (lat x lng).
    private static GeoPolygon square(double latLo, double lngLo, double side) {
        return new GeoPolygon(List.of(
                new GeoPoint(latLo, lngLo),
                new GeoPoint(latLo + side, lngLo),
                new GeoPoint(latLo + side, lngLo + side),
                new GeoPoint(latLo, lngLo + side)));
    }

    private final ZoneResolver polygon = new PolygonZoneResolver(List.of(
            new ZoneBoundary("A", List.of(square(0, 0, 2))),
            new ZoneBoundary("B", List.of(square(0, 2, 2)))));

    @Test
    void polygonResolvesPointToContainingZone() {
        assertEquals("A", polygon.resolve(1.0, 1.0).orElseThrow());
        assertEquals("B", polygon.resolve(1.0, 3.0).orElseThrow());
    }

    @Test
    void polygonReturnsEmptyOutsideAllZones() {
        assertTrue(polygon.resolve(5.0, 5.0).isEmpty());
    }

    @Test
    void polygonHandlesAMultiPolygonZone() {
        ZoneResolver split = new PolygonZoneResolver(List.of(
                new ZoneBoundary("split", List.of(square(0, 0, 1), square(10, 10, 1)))));
        assertEquals("split", split.resolve(0.5, 0.5).orElseThrow());
        assertEquals("split", split.resolve(10.5, 10.5).orElseThrow());
        assertTrue(split.resolve(5.0, 5.0).isEmpty());
    }

    @Test
    void centroidResolvesToNearestZone() {
        ZoneResolver centroid = new NearestCentroidZoneResolver(List.of(
                new TimetableZone("01", "Colombo", 6.9271, 79.8612),
                new TimetableZone("07", "Kandy", 7.2906, 80.6337)));
        // A point next to Kandy resolves to Kandy, not Colombo.
        assertEquals("07", centroid.resolve(7.29, 80.63).orElseThrow());
        assertEquals("01", centroid.resolve(6.93, 79.86).orElseThrow());
    }

    @Test
    void centroidIsEmptyWithNoZones() {
        assertTrue(new NearestCentroidZoneResolver(List.of()).resolve(0, 0).isEmpty());
    }
}
