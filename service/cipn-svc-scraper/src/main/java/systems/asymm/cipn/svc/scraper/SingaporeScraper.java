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
 * {@link AuthorityScraper} for Singapore's MUIS, over its official annual PDF (ADR-0017). Singapore
 * is a single national location, so there is one zone and one PDF for the whole year. MUIS's PDF URL
 * is content-hashed per year rather than templated, so the URL is pinned here and updated by hand
 * when MUIS publishes a new year (the parser's year filter rejects a mismatched PDF loudly).
 */
public final class SingaporeScraper implements AuthorityScraper {

    private static final String COUNTRY = "SG";
    private static final String AUTHORITY = "MUIS";
    private static final String AUTHORITY_NAME = "Majlis Ugama Islam Singapura (MUIS)";
    private static final String AUTHORITY_URL = "https://www.muis.gov.sg/";
    private static final String PDF_2026 =
            "https://isomer-user-content.by.gov.sg/48/f989baef-c5eb-440e-b3bb-874626a0664e/Prayer%20timetable%202026.pdf";

    private final FeedReader feed;
    private final SingaporePdfParser parser = new SingaporePdfParser();

    public SingaporeScraper() {
        this(new HttpFeedReader());
    }

    SingaporeScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (year != 2026) {
            throw new ScrapeException("MUIS PDF URL is pinned to 2026; update SingaporeScraper for " + year);
        }
        List<TimetableRow> rows = parser.parse(feed.getBytes(PDF_2026), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
