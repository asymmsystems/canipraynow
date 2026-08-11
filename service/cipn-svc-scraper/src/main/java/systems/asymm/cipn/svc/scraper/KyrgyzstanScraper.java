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
 * {@link AuthorityScraper} for Kyrgyzstan's KMDB (muftiyat.kg), over its calendar page (ADR-0017).
 * The page renders the whole requested date range as an inline JS array, so a single date-range
 * request returns the full year for Bishkek. The range endpoint first needs a session cookie, so a
 * plain GET of the calendar page is made to establish it (the reader carries the cookie forward).
 */
public final class KyrgyzstanScraper implements AuthorityScraper {

    private static final String COUNTRY = "KG";
    private static final String AUTHORITY = "KMDB";
    private static final String AUTHORITY_NAME = "Spiritual Administration of Muslims of Kyrgyzstan (KMDB)";
    private static final String AUTHORITY_URL = "https://muftiyat.kg/";
    private static final String CALENDAR = "https://muftiyat.kg/ru/calendar/";

    private final FeedReader feed;
    private final KyrgyzstanFeedParser parser = new KyrgyzstanFeedParser();

    public KyrgyzstanScraper() {
        this(new HttpFeedReader());
    }

    KyrgyzstanScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        feed.get(CALENDAR); // establish the session cookie the range query needs
        String url = CALENDAR + "?start=01-01-" + year + "&end=31-12-" + year;
        List<TimetableRow> rows = parser.parse(feed.get(url), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
