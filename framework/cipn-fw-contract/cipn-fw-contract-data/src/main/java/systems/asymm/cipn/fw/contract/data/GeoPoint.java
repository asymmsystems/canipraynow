/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * A point in decimal degrees, used as a polygon vertex or a query location.
 *
 * @param lat latitude, degrees north (negative south)
 * @param lng longitude, degrees east (negative west)
 */
public record GeoPoint(double lat, double lng) {
}
