/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * {@link AuthorityScraper} for Malaysia's JAKIM, over the e-Solat {@code takwimsolat} JSON API
 * (the S1 pilot, ADR-0017). Composes a {@link FeedReader} (the transport) with
 * {@link JakimFeedParser} (the mapping): it fetches one zone's annual times for a year and
 * returns them as an {@link AuthorityTimetable}. Framework-free; the wiring layer instantiates
 * it.
 *
 * <p>e-Solat serves times per JAKIM zone code (e.g. {@code WLY01} = Kuala Lumpur/Putrajaya);
 * the zone's identity and centroid come from the caller (the stable zone register), and this
 * scraper fills the daily rows plus the JAKIM identity and source.
 */
public final class JakimScraper implements AuthorityScraper {

    private static final String COUNTRY = "MY";
    private static final String AUTHORITY = "JAKIM";
    private static final String AUTHORITY_NAME = "Jabatan Kemajuan Islam Malaysia";
    private static final String AUTHORITY_URL = "https://www.e-solat.gov.my/";
    // e-Solat's period=year returns JAKIM's current published year (there is no year parameter;
    // period=duration with a full-year range returns HTTP 500). The requested year is enforced
    // client-side by the parser's year filter: it drops any row not in `year`, so a request for a
    // year JAKIM is not yet serving yields an empty table and a clear "no days" failure. Onboard
    // the current published year; a future year can only be fetched once JAKIM publishes it.
    private static final String ENDPOINT =
            "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone=%s";

    private final FeedReader feed;
    private final JakimFeedParser parser = new JakimFeedParser();

    /** Production instance: fetches over HTTP. */
    public JakimScraper() {
        this(new HttpFeedReader());
    }

    /** For testing: supply a stub {@link FeedReader} that returns a captured response. */
    JakimScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (zone == null || zone.id() == null || zone.id().isBlank()) {
            throw new ScrapeException("A zone with an e-Solat zone code is required");
        }
        String url = String.format(ENDPOINT, zone.id());
        String body = feed.get(url);
        List<TimetableRow> rows = parser.parse(body, year);
        if (rows.isEmpty()) {
            throw new ScrapeException("JAKIM returned no " + year + " days for zone " + zone.id());
        }
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
