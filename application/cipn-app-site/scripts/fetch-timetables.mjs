/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Fetch and parse a national authority's OWN published prayer timetables into
 * baked per-zone year tables the app serves directly - "exactly as the
 * authority publishes", not a computed approximation.
 *
 * Currently implements Sri Lanka / ACJU: 13 district zones x 12 monthly PDFs.
 * Each zone PDF is a daily table (DATE | FAJR | SUNRISE | LUHR | ASR | MAGRIB |
 * ISHA). Output: assets/data/timetables/lk/zone-NN.json, one row per day of the
 * year as [fajr, sunrise, dhuhr, asr, maghrib, isha] in MINUTES from local
 * midnight. Indexed by (month, day) so a table cut for one year serves the next
 * (the times repeat to well under a minute).
 *
 * This is a DEV/DATA tool, run about once a year - NOT part of the Maven build
 * (it hits third-party sites). It needs Node + pdfjs-dist:
 *
 *     npm i pdfjs-dist@4.0.379        # once, anywhere on PATH / NODE_PATH
 *     node scripts/fetch-timetables.mjs [--zones=01,07] [--year=2025]
 *
 * Review the diff and commit the JSON. astronomy.js loads it and prefers the
 * published times when the user is in Sri Lanka, computing only the zenith
 * (which the tables do not carry) from solar noon.
 *
 * @author Fahim Farook
 */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, '../src/main/jbake/assets/data/timetables/lk');
const ACJU_PAGE = 'https://www.acju.lk/prayer-times/';

const argv = Object.fromEntries(process.argv.slice(2).map((a) => {
    const [k, v] = a.replace(/^--/, '').split('=');
    return [k, v ?? true];
}));
const onlyZones = argv.zones ? String(argv.zones).split(',') : null;

/** pdfjs is a dev-only dependency; fail with a clear message if it is absent. */
async function loadPdfjs() {
    try {
        return await import('pdfjs-dist/legacy/build/pdf.mjs');
    } catch (e) {
        console.error('pdfjs-dist not found. Run:  npm i pdfjs-dist@4.0.379');
        process.exit(2);
    }
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'June', 'July', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Parse "4:34 AM" -> minutes from midnight; null if unparseable. */
function toMinutes(t) {
    const m = t.match(/(\d{1,2}):(\d{2})\s*(AM|PM)/i);
    if (!m) return null;
    let h = Number(m[1]) % 12;
    if (/pm/i.test(m[3])) h += 12;
    return h * 60 + Number(m[2]);
}

/** Extract the ACJU PDF URL set from the live page, grouped by zone + month. */
async function discoverUrls() {
    const html = await (await fetch(ACJU_PAGE)).text();
    const re = /https:\/\/www\.acju\.lk\/wp-content\/uploads\/\d{4}\/\d+\/(\d{2})-[A-Z][^"']*?-(\d{1,2})-([A-Za-z]+)\.pdf/g;
    const byZone = {};
    for (const m of html.matchAll(re)) {
        const [url, zone, , monthName] = m;
        const mi = MONTHS.findIndex((x) => x.toLowerCase() === monthName.toLowerCase());
        if (mi < 0) continue;
        (byZone[zone] ||= {})[mi] = url;
    }
    return byZone;
}

/** Rows of a single monthly PDF: { day, times:[6 minutes] } keyed by the "N-Mon" date. */
async function parseMonthly(pdfjs, buf, monthIndex) {
    const doc = await pdfjs.getDocument({ data: new Uint8Array(buf), useSystemFonts: true }).promise;
    const rows = {};
    for (let pageNo = 1; pageNo <= doc.numPages; pageNo++) {
        const tc = await (await doc.getPage(pageNo)).getTextContent();
        // Group text items into rows by rounded y, then order each row by x.
        const lines = {};
        for (const it of tc.items) {
            const s = it.str.trim();
            if (!s) continue;
            const y = Math.round(it.transform[5] / 4) * 4;
            (lines[y] ||= []).push({ s, x: Math.round(it.transform[4]) });
        }
        for (const y of Object.keys(lines)) {
            const cells = lines[y].sort((a, b) => a.x - b.x).map((c) => c.s);
            const line = cells.join(' ');
            const dm = line.match(/\b(\d{1,2})-[A-Za-z]{3,4}\b/);
            if (!dm) continue;
            const times = cells.map(toMinutes).filter((v) => v != null);
            if (times.length < 6) continue;
            rows[Number(dm[1])] = times.slice(0, 6);
        }
    }
    return rows;
}

async function buildZone(pdfjs, zone, months) {
    // year[monthIndex] = { day: [6 minutes] }
    const year = [];
    for (let mi = 0; mi < 12; mi++) {
        const url = months[mi];
        if (!url) { console.warn(`  zone ${zone}: month ${MONTHS[mi]} missing`); year.push(null); continue; }
        try {
            const buf = Buffer.from(await (await fetch(url)).arrayBuffer());
            const rows = await parseMonthly(pdfjs, buf, mi);
            const days = Object.keys(rows).length;
            process.stdout.write(`  zone ${zone} ${MONTHS[mi]}: ${days} days\n`);
            year.push(rows);
        } catch (e) {
            console.warn(`  zone ${zone} ${MONTHS[mi]}: FAILED (${e.message})`);
            year.push(null);
        }
    }
    // Flatten to a { "M-D": [6] } map - compact and day-of-year addressable.
    const table = {};
    let filled = 0;
    year.forEach((rows, mi) => {
        if (!rows) return;
        for (const [day, times] of Object.entries(rows)) { table[`${mi + 1}-${day}`] = times; filled++; }
    });
    return { table, filled };
}

async function main() {
    const pdfjs = await loadPdfjs();
    mkdirSync(OUT, { recursive: true });
    const zones = JSON.parse(readFileSync(path.join(OUT, 'zones.json'), 'utf8')).zones;
    const urls = await discoverUrls();
    console.log(`Discovered ${Object.keys(urls).length} zones on ${ACJU_PAGE}`);

    let total = 0;
    for (const z of zones) {
        if (onlyZones && !onlyZones.includes(z.id)) continue;
        const months = urls[z.id];
        if (!months) { console.warn(`zone ${z.id}: no URLs found - skipped`); continue; }
        console.log(`Zone ${z.id} (${z.name})`);
        const { table, filled } = await buildZone(pdfjs, z.id, months);
        const out = {
            authority: 'ACJU', zone: z.id, name: z.name, source: ACJU_PAGE,
            unit: 'minutes-from-midnight', fields: ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'],
            days: filled, times: table
        };
        writeFileSync(path.join(OUT, `zone-${z.id}.json`), JSON.stringify(out));
        console.log(`  -> zone-${z.id}.json (${filled} days)`);
        total += filled;
    }
    console.log(`\nDone. ${total} day-rows written to ${OUT}`);
}

main().catch((e) => { console.error(e); process.exit(1); });
