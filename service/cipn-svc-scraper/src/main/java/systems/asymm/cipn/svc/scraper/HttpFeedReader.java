/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Production {@link FeedReader}: a plain JDK {@link HttpClient} {@code GET}. No third-party
 * HTTP dependency - the standard client is enough for the yearly, low-volume S1 fetch. Follows
 * redirects, sends a descriptive User-Agent, and treats any non-2xx status as a failure.
 */
final class HttpFeedReader implements FeedReader {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final String USER_AGENT = "canipraynow-scraper/1.0 (+https://canipraynow.com)";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new ScrapeException("GET " + url + " returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new ScrapeException("GET " + url + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScrapeException("GET " + url + " was interrupted", e);
        }
    }
}
