/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Service worker (spec §6). Caching strategy by asset kind:
 *   - code & style (/css, /js, strings): STALE-WHILE-REVALIDATE - served from
 *     cache instantly (sub-second), but refetched in the background so a new
 *     build surfaces on the next load. Pure cache-first (the old behaviour)
 *     never updated CSS/JS across builds unless VERSION changed by hand, which
 *     is why theme changes appeared to do nothing.
 *   - big immutable assets (fonts, the WASM engine, rule sets, data): cache-first
 *     - they rarely change and are large. The ~130 KB WASM engine caches on first
 *     fallback use rather than being precached, keeping the install light.
 *   - navigations (HTML): network-first, cache fallback.
 *   - sponsors.json: network-first.
 * The core answer still works fully offline once cached - the covered path reads the
 * baked table with no engine, and the WASM engine caches after its first use.
 *
 * @author Fahim Farook
 */

// Bump on any release that must invalidate old caches. The activate handler
// deletes every cache that is not this version.
const VERSION = 'cipn-v17';

const SHELL = [
    '/',
    '/css/style.css',
    '/js/main.js',
    '/js/engine.js',
    '/js/astronomy.js',
    '/js/location.js',
    '/js/storage.js',
    '/js/strings.js',
    '/js/utils.js',
    '/js/engine/windows.js',
    '/js/engine/verdict.js',
    '/js/ui/status.js',
    '/js/ui/chips.js',
    '/js/ui/ruler.js',
    '/js/ui/prayers.js',
    '/js/ui/settings.js',
    '/js/ui/sponsor.js',
    '/js/strings/en.json',
    '/data/rule_sets/hanafi.json',
    '/data/rule_sets/shafii.json',
    '/fonts/inter-latin-wght-normal.woff2',
    '/manifest.webmanifest'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(VERSION).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);
    if (event.request.method !== 'GET' || url.origin !== location.origin) return;
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/go/')) return;

    const p = url.pathname;
    if (p === '/data/sponsors.json') {
        event.respondWith(networkFirst(event.request));
    } else if (event.request.mode === 'navigate') {
        event.respondWith(networkFirst(event.request));   // HTML: fresh when online
    } else if (p.startsWith('/css/') || p.startsWith('/js/')) {
        event.respondWith(staleWhileRevalidate(event.request));  // code & style
    } else {
        event.respondWith(cacheFirst(event.request));    // fonts, /wasm/ engine, data
    }
});

async function cacheFirst(request) {
    const cached = await caches.match(request, { ignoreSearch: true });
    if (cached) return cached;
    const response = await fetch(request);
    if (response.ok) {
        const cache = await caches.open(VERSION);
        cache.put(request, response.clone());
    }
    return response;
}

/** Serve cache immediately (fast), refetch in the background for next time. */
async function staleWhileRevalidate(request) {
    const cache = await caches.open(VERSION);
    const cached = await cache.match(request);
    const network = fetch(request).then((response) => {
        if (response.ok) cache.put(request, response.clone());
        return response;
    }).catch(() => cached);
    return cached || network;
}

async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(VERSION);
            cache.put(request, response.clone());
        }
        return response;
    } catch (e) {
        const cached = await caches.match(request);
        return cached || Response.error();
    }
}
