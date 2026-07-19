/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Multi-authority timetable driver - the "scrape the world" framework.
 *
 * Reads the researched registry (scripts/authorities.json) and, for one
 * country, runs the matching parser to bake its authority's OWN published
 * times into assets/data/timetables/<cc>/ - the same shape ACJU uses, so the
 * app serves them verbatim (see timetables.js, AUTHORITY_TIMETABLES).
 *
 * Adding an authority is two things, no more:
 *   1. a parser here (keyed by ISO2), and
 *   2. assets/data/timetables/<cc>/zones.json listing that authority's zones
 *      with a representative centroid each (for nearest-zone matching).
 * Then register <cc> in timetables.js so the app prefers it.
 *
 * This is a DEV/DATA tool run about once a year - NOT part of the Maven build
 * (it hits third-party sites; a build must never depend on them). It needs Node
 * (fetch is built in on Node 18+):
 *
 *     node scripts/fetch-authority.mjs <CC>          # e.g. MY, LK
 *     node scripts/fetch-authority.mjs --list        # what is implemented / blocked
 *
 * STATUS: LK (ACJU, PDF) is baked by scripts/fetch-timetables.mjs. MY (JAKIM,
 * JSON API) is implemented below but UNRUN in this environment (needs network),
 * and needs its timetables/my/zones.json first. Every other registry entry
 * reports its feed type and what blocks it, so the backlog is self-documenting.
 *
 * @author Fahim Farook
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const DATA = path.resolve(HERE, '../src/main/jbake/assets/data/timetables');
const REGISTRY = JSON.parse(readFileSync(path.join(HERE, 'authorities.json'), 'utf8')).authorities;

const FIELDS = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

/** "HH:MM[:SS]" or "H:MM AM/PM" -> minutes from midnight; null if unparseable. */
function toMinutes(s) {
    if (!s) return null;
    let m = s.match(/(\d{1,2}):(\d{2})(?::\d{2})?\s*(AM|PM)?/i);
    if (!m) return null;
    let h = Number(m[1]);
    if (m[3]) { h %= 12; if (/pm/i.test(m[3])) h += 12; }
    return h * 60 + Number(m[2]);
}

/** Load timetables/<cc>/zones.json, or fail with instructions. */
function loadZones(cc) {
    const p = path.join(DATA, cc.toLowerCase(), 'zones.json');
    if (!existsSync(p)) {
        throw new Error(
            `Missing ${p}. Create it first: a JSON { authorityName, authorityUrl, zones: [` +
            `{ id, name, lat, lng }, ... ] } listing this authority's zones and a representative ` +
            `centroid each (see the lk/ example). The parser fetches one table per zone.`);
    }
    return JSON.parse(readFileSync(p, 'utf8'));
}

function writeZone(cc, zoneId, authority, source, table, filled) {
    const dir = path.join(DATA, cc.toLowerCase());
    mkdirSync(dir, { recursive: true });
    const out = {
        authority, zone: zoneId, source,
        unit: 'minutes-from-midnight', fields: FIELDS, days: filled, times: table
    };
    writeFileSync(path.join(dir, `zone-${zoneId}.json`), JSON.stringify(out));
    console.log(`  -> ${cc.toLowerCase()}/zone-${zoneId}.json (${filled} days)`);
}

/* ----------------------------------------------------------- parsers (by CC) */

/**
 * Malaysia - JAKIM e-Solat. Public JSON API, one year per zone:
 *   GET .../index.php?r=esolatApi/takwimsolat&period=year&zone=<ZONE>
 *   -> { prayerTime: [ { date:"01-Jan-2025", fajr, syuruk, dhuhr, asr, maghrib, isha }, ... ] }
 * Zone ids + centroids come from timetables/my/zones.json.
 */
async function jakim(cc) {
    const doc = loadZones(cc);
    const base = 'https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone=';
    for (const z of doc.zones) {
        const res = await fetch(base + encodeURIComponent(z.id));
        const json = await res.json();
        const rows = json.prayerTime || json.data || [];
        const table = {};
        let filled = 0;
        for (const r of rows) {
            const d = new Date(r.date);                       // "01-Jan-2025"
            if (isNaN(d)) continue;
            const key = `${d.getMonth() + 1}-${d.getDate()}`;
            const times = [r.fajr, r.syuruk, r.dhuhr, r.asr, r.maghrib, r.isha].map(toMinutes);
            if (times.some((v) => v == null)) continue;
            table[key] = times;
            filled++;
        }
        writeZone(cc, z.id, doc.authorityName || 'JAKIM', doc.authorityUrl || base, table, filled);
    }
}

/** ISO2 -> parser. Add an entry here to wire a new authority. */
const PARSERS = { MY: jakim };

/* -------------------------------------------------------------------- driver */

function list() {
    console.log('Authority scrape status (from scripts/authorities.json):\n');
    for (const [cc, r] of Object.entries(REGISTRY)) {
        const baked = existsSync(path.join(DATA, cc.toLowerCase(), 'zone-01.json'))
            || existsSync(path.join(DATA, cc.toLowerCase()));
        let state;
        if (cc === 'LK') state = 'BAKED (via fetch-timetables.mjs)';
        else if (PARSERS[cc]) state = 'parser ready, run to bake';
        else if (r.has_authority === false) state = 'no authority timetable - served by computed method';
        else state = `no parser yet (feed: ${r.feed || '?'})`;
        console.log(`  ${cc}  ${state}${r.note ? '  // ' + r.note : ''}`);
    }
    console.log('\nEverything without a baked table is served by computing with that country\'s');
    console.log('authority method (method-defaults.json) - correct for the majority. Add a parser');
    console.log('above + a timetables/<cc>/zones.json to extract a deviating authority exactly.');
}

async function main() {
    const arg = process.argv[2];
    if (!arg || arg === '--list') return list();
    const cc = arg.toUpperCase();
    if (cc === 'LK') {
        console.error('LK (ACJU) is a PDF source - run: node scripts/fetch-timetables.mjs');
        process.exit(2);
    }
    const parser = PARSERS[cc];
    if (!parser) {
        const r = REGISTRY[cc];
        console.error(`No parser for ${cc} yet.` + (r ? `\n  authority: ${r.name || '(none)'}\n  feed: ${r.feed || '?'}` +
            `${r.note ? '\n  note: ' + r.note : ''}\n  url: ${r.url || '-'}` : ' Unknown country.') +
            `\nImplement PARSERS.${cc} + timetables/${cc.toLowerCase()}/zones.json, then re-run.`);
        process.exit(2);
    }
    console.log(`Fetching ${cc} (${REGISTRY[cc] && REGISTRY[cc].name})...`);
    await parser(cc);
    console.log('Done. Review the diff and commit the JSON.');
}

main().catch((e) => { console.error(e.message || e); process.exit(1); });
