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
        return execute("GET", url, HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Override
    public byte[] getBytes(String url) {
        return execute("GET", url, HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/pdf,image/*,*/*;q=0.8")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    @Override
    public String post(String url, String formBody) {
        return execute("POST", url, HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,*/*;q=0.8")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Multi-request scrapers can trip an authority's rate limit; retry transient 5xx/429 with backoff. */
    private static final int MAX_ATTEMPTS = 4;
    private static final long RETRY_BASE_MS = 800;

    private <T> T execute(String method, String url, HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        for (int attempt = 1; ; attempt++) {
            try {
                HttpResponse<T> response = client.send(request, handler);
                int status = response.statusCode();
                if (status / 100 == 2) {
                    return response.body();
                }
                if ((status == 503 || status == 429 || status == 502) && attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                throw new ScrapeException(method + " " + url + " returned HTTP " + status);
            } catch (IOException e) {
                if (attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                throw new ScrapeException(method + " " + url + " failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScrapeException(method + " " + url + " was interrupted", e);
            }
        }
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(RETRY_BASE_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
