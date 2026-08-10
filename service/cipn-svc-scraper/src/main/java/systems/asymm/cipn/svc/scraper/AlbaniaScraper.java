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
 * {@link AuthorityScraper} for Albania's KMSH (Komuniteti Mysliman i Shqiperise), over its official
 * annual "Kalendari" PDF (ADR-0017). KMSH publishes one Tirana reference table for the country, so
 * there is one zone and one PDF for the whole year. The PDF URL is content-hashed per year, so it is
 * pinned here and updated by hand for a new year; the parser's row count rejects a mismatched PDF.
 */
public final class AlbaniaScraper implements AuthorityScraper {

    private static final String COUNTRY = "AL";
    private static final String AUTHORITY = "KMSH";
    private static final String AUTHORITY_NAME = "Komuniteti Mysliman i Shqiperise (KMSH)";
    private static final String AUTHORITY_URL = "https://www.kmsh.al/";
    private static final String PDF_2026 =
            "https://admin.kmsh.al/uploads/2026/05/file-1778275088384-86c31e4382780837.pdf";

    private final FeedReader feed;
    private final AlbaniaPdfParser parser = new AlbaniaPdfParser();

    public AlbaniaScraper() {
        this(new HttpFeedReader());
    }

    AlbaniaScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (year != 2026) {
            throw new ScrapeException("KMSH PDF URL is pinned to 2026; update AlbaniaScraper for " + year);
        }
        List<TimetableRow> rows = parser.parse(feed.getBytes(PDF_2026), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
