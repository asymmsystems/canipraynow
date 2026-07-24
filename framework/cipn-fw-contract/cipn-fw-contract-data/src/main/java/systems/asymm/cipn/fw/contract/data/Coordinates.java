/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * A geographic location in decimal degrees.
 *
 * @param latitude  degrees north of the equator, negative for south (range -90..90)
 * @param longitude degrees east of the prime meridian, negative for west (range -180..180)
 */
public record Coordinates(double latitude, double longitude) {
}
