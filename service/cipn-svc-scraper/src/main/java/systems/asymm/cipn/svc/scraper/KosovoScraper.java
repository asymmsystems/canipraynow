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
 * {@link AuthorityScraper} for Kosovo's BIK (Bashkesia Islame e Kosoves), over its official annual
 * "Takvim" PDF (ADR-0017). The published table is BIK's Prishtina reference point for all of Kosovo,
 * so there is one zone and one PDF for the whole year. The PDF URL is dated per year rather than
 * templated, so it is pinned here and updated by hand when BIK publishes a new year (the parser's
 * row count rejects a mismatched PDF loudly).
 */
public final class KosovoScraper implements AuthorityScraper {

    private static final String COUNTRY = "XK";
    private static final String AUTHORITY = "BIK";
    private static final String AUTHORITY_NAME = "Bashkesia Islame e Kosoves (BIK)";
    private static final String AUTHORITY_URL = "https://bislame.net/";
    private static final String PDF_2026 =
            "https://dituriaislame.com/wp-content/uploads/2026/01/takvimi2026vaktet.pdf";

    private final FeedReader feed;
    private final KosovoPdfParser parser = new KosovoPdfParser();

    public KosovoScraper() {
        this(new HttpFeedReader());
    }

    KosovoScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (year != 2026) {
            throw new ScrapeException("BIK Takvim PDF URL is pinned to 2026; update KosovoScraper for " + year);
        }
        List<TimetableRow> rows = parser.parse(feed.getBytes(PDF_2026), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
