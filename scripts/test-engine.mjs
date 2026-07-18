/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Fiqh engine test suite. Runs the REAL modules from assets/js against the
 * golden matrix, hard invariant 1, boundary math, and the Isha two-phase
 * timing. Node's built-in runner - no npm, no dependencies.
 *
 *   node scripts/test-engine.mjs      (or: make test)
 *
 * @author Fahim Farook
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const JS = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../src/main/jbake/assets/js');
const DATA = path.resolve(JS, '../data');

const { computeWindows } = await import(path.join(JS, 'engine/windows.js'));
const { computeVerdict, ruleParams, PRAYER_TYPES } = await import(path.join(JS, 'engine/verdict.js'));
const { computeDayEvents, ishaEnds } = await import(path.join(JS, 'astronomy.js'));

const rs = {
    evidence: JSON.parse(readFileSync(path.join(DATA, 'rule_sets/evidence.json'), 'utf8')),
    hanafi: JSON.parse(readFileSync(path.join(DATA, 'rule_sets/hanafi.json'), 'utf8')),
    shafii: JSON.parse(readFileSync(path.join(DATA, 'rule_sets/shafii.json'), 'utf8'))
};
const md = JSON.parse(readFileSync(path.join(DATA, 'method-defaults.json'), 'utf8'));

const LOCS = [
    ['Colombo', 6.9271, 79.8612], ['London', 51.5074, -0.1278], ['Makkah', 21.4225, 39.8262],
    ['Jakarta', -6.2088, 106.8456], ['New York', 40.7128, -74.006], ['Sydney', -33.8688, 151.2093],
    ['Tehran', 35.6892, 51.389], ['Casablanca', 33.5731, -7.5898]
];

let pass = 0;
let fail = 0;
const bad = [];
const ck = (c, m) => { if (c) pass++; else { fail++; bad.push(m); } };

const day = new Date(2026, 6, 17);
const { events } = computeDayEvents(day, { lat: 6.9271, lng: 79.8612 }, 'ACJU', 'hanafi');

// 1. Golden matrix: every madhhab x window x prayerType, with invariant-1 override.
for (const [m, rset] of Object.entries(rs)) {
    for (const w of computeWindows(events, ruleParams(rset))) {
        if (w.end <= w.start) continue;
        const mid = new Date((+w.start + +w.end) / 2);
        for (const pt of PRAYER_TYPES) {
            const v = computeVerdict(mid, events, rset, { prayerType: pt, isFriday: false, inMakkahHaram: false });
            const cell = rset.matrix[w.id][pt];
            const fajrExp = (cell.notes || []).includes('FAJR_EXPIRED');
            const exp = pt === 'fardCurrent' && !fajrExp && ['PROHIBITED', 'DISLIKED'].includes(cell.status)
                ? 'PERMITTED' : cell.status;
            ck(v.status === exp, `${m}/${w.id}/${pt}: ${v.status} != ${exp}`);
        }
    }
}

// 2. Hard invariant 1: fardCurrent never a bare NO. Minute sweep x 8 locations x 2 madhhabs.
let sweeps = 0;
let viol = 0;
for (const [, lat, lng] of LOCS) {
    for (const m of ['evidence', 'hanafi', 'shafii']) {
        const r = computeDayEvents(day, { lat, lng }, 'MuslimWorldLeague', m);
        if (!r.valid) continue;
        for (let i = 0; i < 1440; i++) {
            const now = new Date(r.events.fajr.getTime() + i * 60000);
            sweeps++;
            const v = computeVerdict(now, r.events, rs[m], { prayerType: 'fardCurrent', isFriday: false, inMakkahHaram: false });
            if (['PROHIBITED', 'DISLIKED'].includes(v.status) && !v.exceptionsApplied.includes('FAJR_EXPIRED')) viol++;
        }
    }
}
ck(viol === 0, `INVARIANT 1 violated ${viol}x`);

// 3. Every configured method key must actually compute (not silently fall back).
for (const [cc, key] of Object.entries(md.byCountry)) {
    ck(computeDayEvents(day, { lat: 21.4, lng: 39.8 }, key, 'hanafi').valid, `method ${key} (${cc}) failed`);
}

// 4. The default ruleset must ship, and every predominant-school note must name
// a real school (informational only - it never selects a matrix).
ck(Boolean(rs.evidence), 'the evidence default ruleset must ship');
const SCHOOLS = ['evidence', 'hanafi', 'shafii', 'maliki', 'hanbali', 'jafari', 'ibadi'];
for (const [cc, m] of Object.entries(md.predominantMadhhab || {})) {
    ck(SCHOOLS.includes(m), `predominantMadhhab ${cc}=${m} is not a known school`);
}

// 4b. The evidence-based default: dhawāt al-asbāb permitted, causeless nafl
// blocked, janāzah discouraged (not permitted) in the three solar windows.
{
    const w = computeWindows(events, ruleParams(rs.evidence));
    const at = (id) => new Date((+w.find((x) => x.id === id).start + +w.find((x) => x.id === id).end) / 2);
    const V = (pt, id) => computeVerdict(at(id), events, rs.evidence,
        { prayerType: pt, isFriday: false, inMakkahHaram: false }).status;
    ck(V('causalNafl', 'SUNRISE') === 'PERMITTED', 'evidence: causal nafl permitted at sunrise');
    ck(V('qada', 'SUNSET') === 'PERMITTED', 'evidence: qada permitted at sunset (vs hanafi block)');
    ck(V('pureNafl', 'AFTER_ASR') === 'PROHIBITED', 'evidence: causeless nafl blocked after Asr');
    ck(V('janazah', 'ZENITH') === 'DISLIKED', 'evidence: janazah discouraged at zenith (al-Albani)');
    ck(V('janazah', 'AFTER_ASR') === 'PERMITTED', 'evidence: janazah permitted after Asr');
    // The one behavioural difference from shafii: janazah in the solar windows.
    const sh = computeVerdict(at('ZENITH'), events, rs.shafii,
        { prayerType: 'janazah', isFriday: false, inMakkahHaram: false }).status;
    ck(sh === 'PERMITTED', 'shafii: janazah permitted at zenith (the divergence)');
}

// 4c. Evidence integrity: every primary-source id a cell cites must exist in
// the ruleset's evidences map, and every evidence must carry a URL. A dangling
// citation in a religious app is exactly the kind of error to fail the build on.
{
    const ev = rs.evidence.evidences || {};
    ck(Object.keys(ev).length > 0, 'evidence ruleset must carry an evidences map');
    for (const [id, e] of Object.entries(ev)) {
        ck(typeof e.url === 'string' && e.url.length > 0, `evidence ${id} has no url`);
        ck(e.type === 'quran' || e.type === 'hadith', `evidence ${id} bad type`);
    }
    for (const [w, types] of Object.entries(rs.evidence.matrix)) {
        for (const [pt, cell] of Object.entries(types)) {
            for (const id of cell.evidence || []) {
                ck(Boolean(ev[id]), `evidence ruleset ${w}/${pt} cites unknown source "${id}"`);
            }
        }
    }
}

// 5. Isha preferred end precedes true dawn everywhere it resolves.
for (const [n, lat, lng] of LOCS) {
    const a = computeDayEvents(day, { lat, lng }, 'MuslimWorldLeague', 'hanafi');
    const b = computeDayEvents(new Date(2026, 6, 18), { lat, lng }, 'MuslimWorldLeague', 'hanafi');
    if (!a.valid || !b.valid) continue;
    const e = ishaEnds(a.events, b.events, 'hanafi');
    ck(e && e.preferred < e.valid, `${n}: isha preferred must precede dawn`);
}

console.log(`invariant sweep: ${sweeps} samples, ${viol} violations`);
console.log(`PASS ${pass}  FAIL ${fail}`);
if (bad.length) {
    console.log('\nfailures:');
    bad.slice(0, 12).forEach((b) => console.log('  - ' + b));
}
process.exit(fail ? 1 : 0);
