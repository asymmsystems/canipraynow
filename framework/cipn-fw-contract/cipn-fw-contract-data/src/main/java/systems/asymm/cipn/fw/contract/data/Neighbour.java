/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * A nearby city and its great-circle distance from an origin, used to render the
 * "nearby cities" links on a city page.
 *
 * @param city       the neighbouring city
 * @param distanceKm distance from the origin, in whole kilometres
 */
public record Neighbour(City city, int distanceKm) {
}
