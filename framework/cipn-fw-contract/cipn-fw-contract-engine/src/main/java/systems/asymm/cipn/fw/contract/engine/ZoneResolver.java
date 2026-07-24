/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.engine;

import java.util.Optional;

/**
 * Engine contract for resolving a location to the authority zone that covers it - the
 * business rule "which published timetable applies here". How coverage is defined is
 * authority/country-specific (administrative-district polygons, grids, ...), so this is
 * an abstraction with per-strategy implementations:
 *
 * <ul>
 *   <li>{@code PolygonZoneResolver} - point-in-polygon over a zone's district polygons
 *       (the accurate, primary strategy).</li>
 *   <li>{@code NearestCentroidZoneResolver} - nearest zone centroid (the fallback used
 *       until per-country polygons are sourced).</li>
 * </ul>
 *
 * <p>Pure and I/O-free: the coverage data is supplied to the implementation; reading it
 * is an accessor's job.
 *
 * @see Engine
 */
public interface ZoneResolver extends Engine {

    /**
     * The zone that covers the given location.
     *
     * @param latitude  location latitude in decimal degrees
     * @param longitude location longitude in decimal degrees
     * @return the covering zone's id, or empty if no zone covers the location
     */
    Optional<String> resolve(double latitude, double longitude);
}
