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

/**
 * {@link AuthorityScraper} for Hungary's MME (Magyarorszagi Muszlimok Egyhaza), over its static
 * iszlam.com prayer-times page (ADR-0017). The single page carries the whole year as twelve monthly
 * HTML tables for Budapest, so there is one zone and one fetch. The page is a living "current year"
 * document with no year token, so the year is not re-checked against the page; the parser still
 * rejects a page that does not yield a full year.
 */
public final class HungaryScraper implements AuthorityScraper {

    private static final String COUNTRY = "HU";
    private static final String AUTHORITY = "MME";
    private static final String AUTHORITY_NAME = "Magyarorszagi Muszlimok Egyhaza (MME)";
    private static final String AUTHORITY_URL = "https://iszlam.com/iszlam-imaidok/";

    private final FeedReader feed;
    private final HungaryHtmlParser parser = new HungaryHtmlParser();

    public HungaryScraper() {
        this(new HttpFeedReader());
    }

    HungaryScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        List<TimetableRow> rows = parser.parse(feed.get(AUTHORITY_URL), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
