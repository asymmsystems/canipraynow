/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.scrape;

/**
 * Raised when an {@link AuthorityScraper} cannot produce a complete, valid annual table:
 * the feed could not be fetched, the response could not be parsed, or the parsed table was
 * incomplete (missing days). Because S1 is off-build, this surfaces to {@code make scrape}
 * as a non-zero exit for the operator to review - it never fails the site build.
 */
public class ScrapeException extends RuntimeException {

    public ScrapeException(String message) {
        super(message);
    }

    public ScrapeException(String message, Throwable cause) {
        super(message, cause);
    }
}
