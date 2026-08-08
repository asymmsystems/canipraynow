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

    /**
     * POST a {@code application/x-www-form-urlencoded} body (for authorities whose timetable is
     * behind a form, e.g. a month/town selector). Default: unsupported, so GET-only readers stay
     * one-method lambdas.
     *
     * @param url      the form action URL
     * @param formBody the url-encoded form body (e.g. {@code "month=8&town=sofia"})
     * @return the response body as text
     */
    default String post(String url, String formBody) {
        throw new UnsupportedOperationException("POST not supported by this FeedReader");
    }

    /**
     * GET raw bytes (for authorities whose timetable is a PDF or image). Default: unsupported.
     *
     * @param url the feed URL
     * @return the response body as bytes
     */
    default byte[] getBytes(String url) {
        throw new UnsupportedOperationException("byte fetch not supported by this FeedReader");
    }
}
