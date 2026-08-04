/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

/**
 * Fetches a feed URL and returns its body as text - the transport concern, separated from a
 * scraper's field mapping so the mapping is testable offline (a stub {@code FeedReader}
 * supplies a captured response). Package-private: an implementation detail of this module.
 * {@link HttpFeedReader} is the production {@code HTTP GET}; a scraper composes one.
 */
interface FeedReader {

    /**
     * @param url the feed URL
     * @return the response body as text
     * @throws systems.asymm.cipn.fw.contract.scrape.ScrapeException on a non-success status or I/O failure
     */
    String get(String url);
}
