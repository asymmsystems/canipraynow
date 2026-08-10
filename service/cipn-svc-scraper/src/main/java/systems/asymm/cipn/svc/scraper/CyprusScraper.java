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
 * {@link AuthorityScraper} for Northern Cyprus's Din İşleri Başkanlığı, over its yearly HTML takvim
 * (ADR-0017). Composes a {@link FeedReader} (transport) with {@link CyprusFeedParser} (the mapping).
 * The authority publishes a single location (Lefkoşa/Nicosia), so there is one zone; the endpoint
 * takes no zone parameter and returns the current published year.
 */
public final class CyprusScraper implements AuthorityScraper {

    private static final String COUNTRY = "CY";
    private static final String AUTHORITY = "KKTC-DIB";
    private static final String AUTHORITY_NAME = "Din İşleri Başkanlığı (Northern Cyprus)";
    private static final String AUTHORITY_URL = "https://www.kktcdinisleri.com/";
    private static final String ENDPOINT = "https://www.kktcdinisleri.com/namazvakti.php";

    private final FeedReader feed;
    private final CyprusFeedParser parser = new CyprusFeedParser();

    public CyprusScraper() {
        this(new HttpFeedReader());
    }

    CyprusScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (zone == null || zone.id() == null || zone.id().isBlank()) {
            throw new ScrapeException("A zone is required");
        }
        List<TimetableRow> rows = parser.parse(feed.get(ENDPOINT), year);
        if (rows.isEmpty()) {
            throw new ScrapeException("KKTC returned no " + year + " days for zone " + zone.id());
        }
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
