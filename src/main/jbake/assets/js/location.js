/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Location resolution (spec §6 boot, §7): navigator.geolocation (3 s timeout)
 * -> /api/geo (Cloudflare request.cf, no IP stored) -> city picker.
 * The caller decides the order and renders the picker; this module only
 * provides the primitives. Never defaults to a city silently (spec §12).
 *
 * @author Fahim Farook
 */

const GEO_TIMEOUT_MS = 12000;   // room for the permission prompt + a GPS fix

/**
 * @param {number} [timeoutMs]  how long to wait for a fix. The 3 s default was
 *   the bug behind "use my location never works": the browser's permission
 *   prompt routinely takes longer than 3 s for the user to answer, so the timer
 *   fired before they clicked Allow. An explicit request waits much longer.
 * @returns {Promise<{lat:number, lng:number, name:string|null, tz:string, cc:string|null}|null>}
 */
export function detectByGps(timeoutMs = GEO_TIMEOUT_MS) {
    return new Promise((resolve) => {
        if (!('geolocation' in navigator)) return resolve(null);
        let done = false;
        const finish = (v) => { if (!done) { done = true; resolve(v); } };
        // The permission prompt does not count against the option timeout in
        // every browser, so guard with our own generous ceiling too.
        const timer = setTimeout(() => finish(null), timeoutMs + 2000);
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                clearTimeout(timer);
                finish({
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude,
                    name: null, // enriched to nearest bundled city by the caller
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

/** @returns {Promise<Array>} bundled city list (spec §7) */
export async function loadCities() {
    if (!citiesCache) {
        const res = await fetch('/data/cities.json');
        citiesCache = (await res.json()).cities;
    }
    return citiesCache;
}

/** Diacritic-insensitive substring search over the bundled list (spec §7: no library). */
export function searchCities(cities, query) {
    const q = normalize(query);
    if (q.length < 2) return [];
    return cities
        .filter((c) => normalize(c.name).includes(q) || normalize(c.country).includes(q))
        .slice(0, 12);
}

/** Nearest bundled city, for labelling GPS coordinates. */
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
    return s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}
