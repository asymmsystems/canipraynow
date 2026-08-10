/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.scrape;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

/**
 * Scraper contract for one national authority's published timetables (S1, ADR-0017).
 *
 * <p>Named by CONSUMED TYPE (authority timetables). One implementation owns exactly one
 * authority and one feed type: {@code JakimScraper} reads Malaysia's JAKIM e-Solat JSON
 * API today; an {@code AcjuScraper} (PDF) or {@code MuisScraper} (HTML) would each be a
 * separate implementation of this same contract. The feed's transport and format are
 * implementation details and never leak through this port.
 *
 * <p>The unit of work is one <em>zone</em> for one <em>year</em>: an authority divides a
 * country into coverage zones (JAKIM has ~59), and each zone has its own annual table. The
 * zone's identity and centroid are supplied by the caller (from the stable zone register);
 * the scraper fills that zone's daily rows plus the authority identity and source. Solar
 * noon is an E3 enrichment added downstream, so scraped rows carry it as absent (0).
 *
 * @see Scraper
 */
public interface AuthorityScraper extends Scraper {

    /**
     * The ISO-3166 alpha-2 country code this scraper serves (e.g. {@code "MY"}). Used by the
     * wiring/app to assert the resolved scraper matches the requested country.
     *
     * @return the country code, upper-case
     */
    String countryCode();

    /**
     * Fetch one zone's annual timetable from the authority.
     *
     * @param zone the zone to scrape: {@code zone.id()} is the authority's own zone code
     *             (e.g. {@code "WLY01"}), and its name/centroid carry through to the output
     * @param year the Gregorian year to fetch
     * @return the zone's annual timetable (the authority's own published times), with the
     *         authority identity and source populated
     * @throws systems.asymm.cipn.fw.contract.scrape.ScrapeException if the fetch fails or the
     *         feed cannot be parsed into a complete annual table
     */
    AuthorityTimetable scrape(TimetableZone zone, int year);
}
