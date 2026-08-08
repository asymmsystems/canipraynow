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
 * {@link AuthorityScraper} for Portugal's CIL (Comunidade Islamica de Lisboa), over its official
 * annual Lisbon prayer PDF (ADR-0017). CIL publishes one Lisbon reference table for the country, so
 * there is one zone and one PDF for the whole year. The PDF URL carries the year, so it is pinned
 * here and updated by hand for a new year; the parser's row count rejects a mismatched PDF loudly.
 */
public final class PortugalScraper implements AuthorityScraper {

    private static final String COUNTRY = "PT";
    private static final String AUTHORITY = "CIL";
    private static final String AUTHORITY_NAME = "Comunidade Islamica de Lisboa (CIL)";
    private static final String AUTHORITY_URL = "https://www.comunidadeislamica.pt/";
    private static final String PDF_2026 =
            "https://irp.cdn-website.com/cce0f1db/files/uploaded/lisboa_2026.pdf";

    private final FeedReader feed;
    private final PortugalPdfParser parser = new PortugalPdfParser();

    public PortugalScraper() {
        this(new HttpFeedReader());
    }

    PortugalScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (year != 2026) {
            throw new ScrapeException("CIL PDF URL is pinned to 2026; update PortugalScraper for " + year);
        }
        List<TimetableRow> rows = parser.parse(feed.getBytes(PDF_2026), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
