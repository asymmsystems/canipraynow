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
 * {@link AuthorityScraper} for Ukraine, over islam.ua's official annual "Kalendar namaziv" print PDF
 * for Kyiv (ADR-0017). One national reference table (Kyiv), one PDF for the whole year. The PDF URL
 * carries the year, so it is pinned here and updated by hand for a new year; the parser's per-month
 * calendar-length checks reject a mismatched PDF loudly.
 */
public final class UkraineScraper implements AuthorityScraper {

    private static final String COUNTRY = "UA";
    private static final String AUTHORITY = "islam.ua";
    private static final String AUTHORITY_NAME = "Religious Administration of Muslims of Ukraine (islam.ua)";
    private static final String AUTHORITY_URL = "https://islam.ua/";
    private static final String PDF_2026 =
            "https://islam.ua/images/kalendar-namazov/Calendar_2026/print/Kiev_2026_Print_Cal.pdf";

    private final FeedReader feed;
    private final UkrainePdfParser parser = new UkrainePdfParser();

    public UkraineScraper() {
        this(new HttpFeedReader());
    }

    UkraineScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (year != 2026) {
            throw new ScrapeException("islam.ua PDF URL is pinned to 2026; update UkraineScraper for " + year);
        }
        List<TimetableRow> rows = parser.parse(feed.getBytes(PDF_2026), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
