/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * {@link AuthorityScraper} for the Islamic Community in Bosnia and Herzegovina (Rijaset), over the
 * official vaktija.ba JSON API (ADR-0017). The API serves one month per request
 * ({@code /vaktija/v1/<locationId>/<year>/<MM>}, month zero-padded), so a full year is 12 GETs for
 * one location; the location id is the zone id (77 = Sarajevo). Composes a {@link FeedReader} with
 * {@link BosniaFeedParser}.
 */
public final class BosniaScraper implements AuthorityScraper {

    private static final String COUNTRY = "BA";
    private static final String AUTHORITY = "BA-Rijaset";
    private static final String AUTHORITY_NAME = "Islamic Community in Bosnia and Herzegovina (Rijaset)";
    private static final String AUTHORITY_URL = "https://vaktija.ba/";
    private static final String ENDPOINT = "https://api.vaktija.ba/vaktija/v1/%s/%d/%02d";

    private final FeedReader feed;
    private final BosniaFeedParser parser = new BosniaFeedParser();

    public BosniaScraper() {
        this(new HttpFeedReader());
    }

    BosniaScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (zone == null || zone.id() == null || zone.id().isBlank()) {
            throw new ScrapeException("A zone with a vaktija.ba location id is required");
        }
        List<TimetableRow> rows = new ArrayList<>(366);
        for (int month = 1; month <= 12; month++) {
            String url = String.format(ENDPOINT, zone.id(), year, month);
            rows.addAll(parser.parse(feed.get(url), month));
        }
        if (rows.isEmpty()) {
            throw new ScrapeException("vaktija.ba returned no " + year + " days for " + zone.id());
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
