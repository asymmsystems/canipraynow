/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * A city in the bundled dataset: its identity, location, timezone, and whether it gets
 * its own generated page.
 *
 * @param name         display name (e.g. {@code "Colombo"})
 * @param country      display country (e.g. {@code "Sri Lanka"})
 * @param cc           ISO-3166 alpha-2 country code (e.g. {@code "LK"})
 * @param slug         URL slug for the city (e.g. {@code "colombo"})
 * @param countrySlug  URL slug for the country (e.g. {@code "sri-lanka"})
 * @param lat          latitude in decimal degrees
 * @param lng          longitude in decimal degrees
 * @param tz           IANA timezone id (e.g. {@code "Asia/Colombo"})
 * @param page         whether this city gets its own generated page
 */
public record City(
        String name,
        String country,
        String cc,
        String slug,
        String countrySlug,
        double lat,
        double lng,
        String tz,
        boolean page) {
}
