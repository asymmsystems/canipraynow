/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Authority timetable layer. Where a national authority publishes its own
 * prayer times and we have scraped them (scripts/fetch-timetables.mjs), this
 * serves those EXACT published times instead of a computed approximation. The
 * only country wired so far is Sri Lanka (ACJU): a location is matched to the
 * nearest of ACJU's 13 district zones, and that zone's baked year table is
 * looked up by day of the month.
 *
 * Zenith is not in the tables (authorities publish the five prayers, not solar
 * noon), so the caller still computes it. Any day missing from a table - an
 * unscraped month, a leap day - falls back to computation, so the answer is
 * always defined.
 *
 * @author Fahim Farook
 */

/**
 * Countries with a baked authority timetable, by ISO2. `folder` is the data
 * directory; `name` / `url` label the authority in the UI (the method dropdown
 * offers "{name} published timetable" as the default for that country). Add a
 * country here once scripts/fetch-timetables.mjs has baked its tables.
 */
const AUTHORITY_TIMETABLES = {
    LK: { folder: 'lk', name: 'ACJU', url: 'https://www.acju.lk/prayer-times/' }
};

/** Sentinel method value: "serve the country's extracted authority timetable". */
export const AUTHORITY_METHOD = 'AUTHORITY';

const zonesCache = {};   // folder -> zones.json
const zoneTableCache = {};   // "folder/zoneId" -> table json

/** Is there an authority timetable for this location's country? */
export function hasAuthorityTimetable(cc) {
    return Boolean(cc && AUTHORITY_TIMETABLES[cc]);
}

/** The authority's display name + url for a country, or null. Synchronous, so
 *  settings can label the dropdown option without awaiting the zone files. */
export function authorityInfo(cc) {
    return (cc && AUTHORITY_TIMETABLES[cc]) || null;
}

async function loadZones(folder) {
    if (!zonesCache[folder]) {
        const res = await fetch(`/data/timetables/${folder}/zones.json`);
        zonesCache[folder] = await res.json();
    }
    return zonesCache[folder];
}

async function loadZoneTable(folder, zoneId) {
    const key = `${folder}/${zoneId}`;
    if (!(key in zoneTableCache)) {
        try {
            const res = await fetch(`/data/timetables/${folder}/zone-${zoneId}.json`);
            zoneTableCache[key] = res.ok ? await res.json() : null;
        } catch (e) {
            zoneTableCache[key] = null;
        }
    }
    return zoneTableCache[key];
}

/** Nearest zone centroid to the coordinates (flat approximation is fine at this scale). */
function nearestZone(zones, lat, lng) {
    let best = null;
    let bestD = Infinity;
    for (const z of zones) {
        const d = (z.lat - lat) ** 2 + ((z.lng - lng) * Math.cos((lat * Math.PI) / 180)) ** 2;
        if (d < bestD) { bestD = d; best = z; }
    }
    return best;
}

/**
 * The authority's published events for one civil day, or null if there is no
 * table for this location / no row for this day (caller then computes).
 *
 * @param {Date} date       any instant of the civil day (used for month/day and the y/m/d base)
 * @param {{lat:number, lng:number, cc:string}} loc
 * @returns {Promise<{source:string, sourceUrl:string, zone:string,
 *   events:{fajr:Date,sunrise:Date,dhuhr:Date,asr:Date,maghrib:Date,isha:Date}}|null>}
 */
export async function authorityDayEvents(date, loc) {
    const entry = AUTHORITY_TIMETABLES[loc.cc];
    if (!entry) return null;
    const folder = entry.folder;

    const zonesDoc = await loadZones(folder);
    const zone = nearestZone(zonesDoc.zones, loc.lat, loc.lng);
    if (!zone) return null;

    const table = await loadZoneTable(folder, zone.id);
    if (!table) return null;

    const key = `${date.getMonth() + 1}-${date.getDate()}`;
    const row = table.times[key];
    if (!row || row.length < 6) return null;   // gap -> caller computes

    // Build Dates in the civil day, from minutes-of-local-midnight. Adhan gives
    // device-local Dates too, so downstream time math stays consistent.
    const at = (mins) => new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, mins, 0);
    const [fajr, sunrise, dhuhr, asr, maghrib, isha] = row;
    return {
        source: zonesDoc.authorityName || table.authority,
        sourceUrl: zonesDoc.authorityUrl || table.source,
        zone: zone.name,
        events: {
            fajr: at(fajr), sunrise: at(sunrise), dhuhr: at(dhuhr),
            asr: at(asr), maghrib: at(maghrib), isha: at(isha)
        }
    };
}
