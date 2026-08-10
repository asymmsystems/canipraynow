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
 * {@link AuthorityScraper} for Kazakhstan's DUMK (Spiritual Administration of Muslims of Kazakhstan),
 * over its keyless namaz.muftyat.kz JSON API (ADR-0017). The API keys times to a settlement's exact
 * registered coordinates (an arbitrary lat/lng returns {@code success=false}), so each zone in the
 * register carries a muftyat.kz settlement's coordinates and is scraped in turn - one request per
 * zone, one national reference city per zone.
 */
public final class KazakhstanScraper implements AuthorityScraper {

    private static final String COUNTRY = "KZ";
    private static final String AUTHORITY = "DUMK";
    private static final String AUTHORITY_NAME = "Spiritual Administration of Muslims of Kazakhstan (DUMK)";
    private static final String AUTHORITY_URL = "https://www.muftyat.kz/";

    private final FeedReader feed;
    private final KazakhstanFeedParser parser = new KazakhstanFeedParser();

    public KazakhstanScraper() {
        this(new HttpFeedReader());
    }

    KazakhstanScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        // The zone centroid must be a muftyat.kz settlement's exact coordinates; Double.toString
        // round-trips the register value to the string the API registered it under.
        String url = "https://namaz.muftyat.kz/kk/api/times/" + year + "/"
                + Double.toString(zone.lat()) + "/" + Double.toString(zone.lng());
        List<TimetableRow> rows = parser.parse(feed.get(url), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
