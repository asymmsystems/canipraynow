/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.time.Year;
import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * {@link AuthorityScraper} for Botswana's BMA (Botswana Muslim Association), over its bma.org.bw
 * prayer-times page (ADR-0017). The single static page carries every city's twelve monthly tables,
 * so it is fetched once and cached across the run's zones; each zone's id is the city's div-id
 * spelling on the page (e.g. {@code gabarone}). The three national reference cities are onboarded.
 */
public final class BotswanaScraper implements AuthorityScraper {

    private static final String COUNTRY = "BW";
    private static final String AUTHORITY = "BMA";
    private static final String AUTHORITY_NAME = "Botswana Muslim Association (BMA)";
    private static final String AUTHORITY_URL = "https://bma.org.bw/prayer-times/";

    private final FeedReader feed;
    private final BotswanaHtmlParser parser = new BotswanaHtmlParser();
    private String page; // cached across zones in one run

    public BotswanaScraper() {
        this(new HttpFeedReader());
    }

    BotswanaScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (page == null) {
            page = feed.get(AUTHORITY_URL);
        }
        List<TimetableRow> rows = parser.parse(page, zone.id(), year);
        int expected = Year.isLeap(year) ? 366 : 365;
        if (rows.size() != expected) {
            // BMA publishes month by month; future months are empty table shells until published.
            throw new ScrapeException("BMA has " + rows.size() + " of " + expected + " days for "
                    + zone.id() + " - the source year is not fully published yet");
        }
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
