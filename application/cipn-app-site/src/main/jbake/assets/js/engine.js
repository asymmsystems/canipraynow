/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Client prayer-time engine (spec §4). The astronomy kernel is the SAME Java engine the build
 * and server use, compiled to WebAssembly-GC by TeaVM (ADR-0015); this module loads it and
 * hands it the method table. It replaces the vendored adhan.js.
 *
 * The WASM module is loaded LAZILY, on the first fallback compute only - a covered city on the
 * default method reads its baked table (see timetables.js) and never loads this. So the ~130 KB
 * module is off the critical path for the common case (ADR-0009: WASM is fallback-only).
 *
 * The method angles are read from calculation-method.xml INSIDE the WASM (the parser is compiled
 * in): JS fetches the XML as text and passes it through, never parsing it - the only client-side
 * XML read is the engine's own input (ADR-0015).
 *
 * @author Fahim Farook
 */

import { solarNoon } from './astronomy.js';

const WASM_URL = '/wasm/prayer-times.wasm';
const RUNTIME_URL = '/wasm/wasm-gc-runtime.min.js';
const METHOD_XML_URL = '/wasm/calculation-method.xml';

let enginePromise = null;
let methodXmlPromise = null;

/** Load the TeaVM WASM-GC runtime once. It is a classic script that registers window.TeaVM. */
function loadRuntime() {
    return new Promise((resolve, reject) => {
        if (window.TeaVM && window.TeaVM.wasmGC) return resolve(window.TeaVM);
        const script = document.createElement('script');
        script.src = RUNTIME_URL;
        script.async = true;
        script.onload = () => resolve(window.TeaVM);
        script.onerror = () => reject(new Error('WASM runtime failed to load'));
        document.head.appendChild(script);
    });
}

/** The instantiated engine module (cached; loaded on first use). */
function engine() {
    if (!enginePromise) {
        enginePromise = loadRuntime().then(async (TeaVM) => {
            // TeaVM's runtime compiles the module with WebAssembly.compileStreaming, which rejects
            // any response whose MIME is not application/wasm. Cloudflare Pages serves that, but
            // dev servers (and some hosts) send application/octet-stream, which would fail the
            // compile. Fetch the bytes and hand the runtime a blob URL stamped application/wasm,
            // so the compile succeeds regardless of the origin server's MIME.
            const bytes = await (await fetch(WASM_URL)).arrayBuffer();
            const typedUrl = URL.createObjectURL(new Blob([bytes], { type: 'application/wasm' }));
            try {
                return await TeaVM.wasmGC.load(typedUrl);
            } finally {
                URL.revokeObjectURL(typedUrl);
            }
        });
    }
    return enginePromise;
}

/** The method-table XML text, fetched once. The engine's own input; parsed inside the WASM. */
function methodXml() {
    if (!methodXmlPromise) {
        methodXmlPromise = fetch(METHOD_XML_URL).then((r) => r.text());
    }
    return methodXmlPromise;
}

/** METHODS CamelCase id (MuslimWorldLeague) -> calculation-method.xml kebab key. */
function slug(methodKey) {
    return methodKey
        .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
        .replace(/([A-Z]+)([A-Z][a-z])/g, '$1-$2')
        .toLowerCase();
}

/**
 * Compute one civil day's events for coordinates via the WASM engine. Same shape and the same
 * strict-ordering guard as the authority path (astronomy.js#assembleAuthorityEvents), so
 * downstream code cannot tell computed from published - only valid from degraded. Solar noon
 * is computed here (adhan-free, NOAA), never taken from the method-adjusted Dhuhr (spec §3.6).
 *
 * @param {Date} date
 * @param {{lat:number, lng:number}} coords
 * @param {string} methodKey  a METHODS key (CamelCase)
 * @param {string} madhhab    the active ruleset; only 'hanafi' selects the later (Hanafi) Asr
 * @returns {Promise<import('./astronomy.js').DayResult>}
 */
export async function computeDayEvents(date, coords, methodKey, madhhab) {
    let secs;
    try {
        const [wasm, xml] = await Promise.all([engine(), methodXml()]);
        const madhabOrdinal = madhhab === 'hanafi' ? 2 : 1;
        secs = wasm.exports.compute(
            coords.lat, coords.lng, date.getFullYear(), date.getMonth() + 1, date.getDate(),
            madhabOrdinal, slug(methodKey), xml);
    } catch (e) {
        return { valid: false, events: null };   // load/compute failure degrades honestly
    }
    if (!secs || secs.length < 7) return { valid: false, events: null };
    // WASM order: [fajr, sunrise, dhuhr, asr, sunset, maghrib, isha] as UTC epoch-seconds.
    const at = (i) => new Date(secs[i] * 1000);   // absolute instant; formatted later in the location tz
    const events = {
        fajr: at(0),
        sunrise: at(1),
        solarNoon: solarNoon(date, coords.lng),
        dhuhr: at(2),
        asr: at(3),
        maghrib: at(5),
        isha: at(6)
    };
    const ordered = [events.fajr, events.sunrise, events.dhuhr, events.asr, events.maghrib, events.isha];
    for (let i = 0; i < ordered.length; i++) {
        if (!(ordered[i] instanceof Date) || !isFinite(ordered[i].getTime())) return { valid: false, events: null };
        if (i > 0 && ordered[i].getTime() <= ordered[i - 1].getTime()) return { valid: false, events: null };
    }
    return { valid: true, events };
}
