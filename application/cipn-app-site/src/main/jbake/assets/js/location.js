/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Location resolution (spec §6 boot, §7): navigator.geolocation (waits for the prompt)
 * -> /api/geo (Cloudflare request.cf, no IP stored) -> city picker.
 *
 * The city dataset is the baked city directory (/cities.html), not JSON: its <li> carry each
 * city's coordinates, timezone, country code, method, madhhab and slug (ADR-0015). We fetch that
 * ONE page once (SW-cached) and read search + nearest-city from its DOM - no cities.json, no
 * client-side XML. The caller decides ordering and renders the picker; this module only provides
 * the primitives. Never defaults to a city silently (spec §12).
 *
 * @author Fahim Farook
 */

const GEO_TIMEOUT_MS = 12000;   // room for the permission prompt + a GPS fix

/**
 * @param {number} [timeoutMs]  how long to wait for a fix.
 * @returns {Promise<{lat:number, lng:number, name:string|null, tz:string, cc:string|null}|null>}
 */
export function detectByGps(timeoutMs = GEO_TIMEOUT_MS) {
    return new Promise((resolve) => {
        if (!('geolocation' in navigator)) return resolve(null);
        let done = false;
        const finish = (v) => { if (!done) { done = true; resolve(v); } };
        const timer = setTimeout(() => finish(null), timeoutMs + 2000);
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                clearTimeout(timer);
                finish({
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude,
                    name: null, // enriched to nearest baked city by the caller
                    tz: Intl.DateTimeFormat().resolvedOptions().timeZone,
                    cc: null
                });
            },
            () => { clearTimeout(timer); finish(null); },
            { timeout: timeoutMs, maximumAge: 15 * 60 * 1000, enableHighAccuracy: true }
        );
    });
}

/** @returns {Promise<{lat:number, lng:number, name:string|null, tz:string, cc:string|null}|null>} */
export async function detectByEdge() {
    try {
        const res = await fetch('/api/geo', { signal: AbortSignal.timeout(4000) });
        if (!res.ok) return null;
        const geo = await res.json();
        if (typeof geo.latitude !== 'number' || typeof geo.longitude !== 'number') return null;
        return {
            lat: geo.latitude,
            lng: geo.longitude,
            name: geo.city || null,
            tz: geo.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone,
            cc: geo.country || null
        };
    } catch (e) {
        return null;
    }
}

let citiesCache = null;

/**
 * The baked city dataset, read once from the directory page's DOM (SW-cached). Each entry is a
 * full city: name, country, coordinates, timezone, cc, method, madhhab and its page slug (uri).
 * @returns {Promise<Array>}
 */
export async function loadCities() {
    if (!citiesCache) {
        const res = await fetch('/cities.html');
        const doc = new DOMParser().parseFromString(await res.text(), 'text/html');
        citiesCache = [...doc.querySelectorAll('li[data-uri]')].map((li) => ({
            name: li.dataset.name,
            country: li.dataset.country,
            lat: Number(li.dataset.lat),
            lng: Number(li.dataset.lng),
            tz: li.dataset.tz,
            cc: li.dataset.cc,
            method: li.dataset.method,
            madhhab: li.dataset.madhhab,
            uri: li.dataset.uri,
            search: li.dataset.city   // lower-cased "name country"
        }));
    }
    return citiesCache;
}

/** Diacritic-insensitive substring search over the baked list (spec §7: no library). */
export function searchCities(cities, query) {
    const q = normalize(query);
    if (q.length < 2) return [];
    return cities
        .filter((c) => normalize(c.search || `${c.name} ${c.country}`).includes(q))
        .slice(0, 12);
}

/** Nearest baked city, for labelling GPS coordinates and routing (ADR-0016). */
export function nearestCity(cities, lat, lng) {
    let best = null;
    let bestD = Infinity;
    for (const c of cities) {
        const d = (c.lat - lat) ** 2 + ((c.lng - lng) * Math.cos(lat * Math.PI / 180)) ** 2;
        if (d < bestD) { bestD = d; best = c; }
    }
    return best;
}

function normalize(s) {
    return (s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}
