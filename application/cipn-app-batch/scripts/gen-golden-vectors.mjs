/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Golden-vector generator for the E3 astronomy engine (T-14, ADR-0009).
 * Runs the vendored adhan.js - the reference implementation the Java port must
 * match - over a grid of (method x madhab x location x date) and emits the seven
 * event times as UTC epoch millis. The Java parity test asserts the port
 * reproduces these to the minute. Mirrors astronomy.js buildParams() exactly:
 * the four custom-angle methods use a bare CalculationParameters (no dhuhr
 * adjustment); the rest use the adhan CalculationMethod presets.
 *
 *   node service/cipn-svc-engine/scripts/gen-golden-vectors.mjs
 *
 * Writes src/test/resources/golden-vectors.json. Regenerate only when the
 * method registry or adhan.js changes; the JSON is the committed oracle.
 *
 * @author Fahim Farook
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ADHAN = path.resolve(HERE, '../../cipn-app-site/src/main/jbake/assets/js/vendor/adhan.js');
const OUT = path.resolve(HERE, '../src/test/resources/golden-vectors.json');

const Adhan = await import(ADHAN);

// astronomy.js: the four methods computed from a bare CalculationParameters.
const CUSTOM = {
    ACJU: { fajrAngle: 20, ishaAngle: 18 },
    Kemenag: { fajrAngle: 20, ishaAngle: 18 },
    France: { fajrAngle: 12, ishaAngle: 12 },
    Russia: { fajrAngle: 16, ishaAngle: 15 },
};

const METHODS = ['ACJU', 'Kemenag', 'MuslimWorldLeague', 'NorthAmerica', 'Egyptian',
    'UmmAlQura', 'Karachi', 'Tehran', 'Dubai', 'Kuwait', 'Qatar', 'Singapore',
    'Turkey', 'France', 'Russia', 'MoonsightingCommittee'];

const MADHABS = ['shafi', 'hanafi'];

// Latitudes kept < 55 so v1 avoids the moonsighting seasonal-twilight and
// polar-circle branches (deferred, tracked in the module README).
const LOCS = [
    ['Colombo', 6.9271, 79.8612], ['KualaLumpur', 3.139, 101.6869],
    ['Makkah', 21.4225, 39.8262], ['Dubai', 25.2048, 55.2708],
    ['Cairo', 30.0444, 31.2357], ['Casablanca', 33.5731, -7.5898],
    ['Tehran', 35.6892, 51.389], ['Tokyo', 35.6762, 139.6503],
    ['NewYork', 40.7128, -74.006], ['Istanbul', 41.0082, 28.9784],
    ['London', 51.5074, -0.1278], ['Jakarta', -6.2088, 106.8456],
    ['Sydney', -33.8688, 151.2093], ['CapeTown', -33.9249, 18.4241],
];

// [year, monthIndex(0-11), day] - solstices, equinoxes, and two off-season dates.
const DATES = [[2026, 0, 15], [2026, 2, 20], [2026, 5, 21], [2026, 6, 17], [2026, 8, 23], [2026, 11, 21]];

function buildParams(methodKey, madhhab) {
    const custom = CUSTOM[methodKey];
    const params = custom
        ? new Adhan.CalculationParameters(methodKey, custom.fajrAngle, custom.ishaAngle)
        : (Adhan.CalculationMethod[methodKey] || Adhan.CalculationMethod.MuslimWorldLeague)();
    params.madhab = madhhab === 'hanafi' ? Adhan.Madhab.Hanafi : Adhan.Madhab.Shafi;
    return params;
}

const ms = (d) => (d && !isNaN(d.getTime()) ? d.getTime() : null);

const vectors = [];
for (const method of METHODS) {
    for (const madhab of MADHABS) {
        for (const [loc, lat, lng] of LOCS) {
            for (const [y, mo, da] of DATES) {
                const date = new Date(y, mo, da);
                const t = new Adhan.PrayerTimes(new Adhan.Coordinates(lat, lng), date, buildParams(method, madhab));
                vectors.push({
                    method, madhab, loc, lat, lng, y, mo, da,
                    fajr: ms(t.fajr), sunrise: ms(t.sunrise), dhuhr: ms(t.dhuhr),
                    asr: ms(t.asr), sunset: ms(t.sunset), maghrib: ms(t.maghrib), isha: ms(t.isha),
                });
            }
        }
    }
}

mkdirSync(path.dirname(OUT), { recursive: true });
writeFileSync(OUT, JSON.stringify({
    _comment: 'Golden vectors from vendored adhan.js - the E3 parity oracle (T-14). Regenerate with gen-golden-vectors.mjs.',
    adhanSource: 'assets/js/vendor/adhan.js',
    generatedFor: '2026',
    count: vectors.length,
    vectors,
}, null, 0) + '\n');
console.log(`wrote ${vectors.length} vectors -> ${path.relative(process.cwd(), OUT)}`);
