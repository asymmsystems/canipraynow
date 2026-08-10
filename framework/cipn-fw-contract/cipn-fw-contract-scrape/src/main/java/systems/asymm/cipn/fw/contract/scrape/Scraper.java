/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.scrape;

/**
 * Marker interface for Scraper services (S1 authority acquisition, ADR-0017).
 *
 * <p>A Scraper is the off-build data-acquisition service type: it fetches a national
 * authority's published prayer timetable and normalises it into the framework's
 * {@code AuthorityTimetable} shape, which the build then bakes (S2) and the client reads
 * at runtime (E1). Like an Accessor it owns I/O (it hits third-party sites); unlike a
 * runtime Accessor it is deliberately <em>off the Maven build</em> - a network fetch must
 * never be able to fail a build - and is run about once a year by hand ({@code make scrape}).
 *
 * <p>Contracts are named by the CONSUMED TYPE (e.g. {@code AuthorityScraper});
 * implementations are named by AUTHORITY (e.g. {@code JakimScraper}). The feed type
 * (JSON API, HTML, PDF, image) is an internal detail of the implementation.
 *
 * @see AuthorityScraper
 */
public interface Scraper {
    // Marker only - defines the Scraper service type.
}
