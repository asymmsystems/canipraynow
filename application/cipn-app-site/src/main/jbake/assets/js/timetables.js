/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Authority timetable layer (R1, ADR-0015). Where a national authority publishes its own prayer
 * times and we have baked them, the covered city's page carries that authority's ANNUAL TABLE in
 * the DOM (see timetable.ftl). This module reads TODAY's row straight from that baked table -
 * no fetch, no JSON - and serves those exact published times instead of a computed approximation.
 * A location off the covered city, or a day the table does not cover (a gap, a leap day), returns
 * null so the caller computes with the WASM engine; the answer is always defined.
 *
 * Zenith is not in the tables (authorities publish the five prayers, not solar noon), so the
 * caller still computes it.
 *
 * @author Fahim Farook
 */

/**
 * Countries with a baked authority timetable, by ISO2. Small synchronous registry used for the
 * method dropdown (it offers "{name} published timetable" as that country's default) and to know
 * a country is covered before the table itself is read from the DOM. The times themselves are
 * NOT here - they are baked into each covered city's page. Add a country once its tables are baked.
 */
const AUTHORITY_TIMETABLES = {
    LK: { folder: 'lk', name: 'ACJU', url: 'https://www.acju.lk/prayer-times/' },
    MY: { folder: 'my', name: 'JAKIM', url: 'https://www.e-solat.gov.my/' },
    CY: { folder: 'cy', name: 'KKTC Diyanet', url: 'https://www.kktcdinisleri.com/' },
    BG: { folder: 'bg', name: 'Chief Muftiate', url: 'https://www.grandmufti.bg/' },
    BA: { folder: 'ba', name: 'Rijaset', url: 'https://vaktija.ba/' },
    SG: { folder: 'sg', name: 'MUIS', url: 'https://www.muis.gov.sg/' },
    XK: { folder: 'xk', name: 'BIK', url: 'https://bislame.net/' },
    HU: { folder: 'hu', name: 'MME', url: 'https://iszlam.com/iszlam-imaidok/' },
    UA: { folder: 'ua', name: 'islam.ua', url: 'https://islam.ua/' },
    KZ: { folder: 'kz', name: 'DUMK', url: 'https://www.muftyat.kz/' },
    PT: { folder: 'pt', name: 'CIL', url: 'https://www.comunidadeislamica.pt/' },
    AL: { folder: 'al', name: 'KMSH', url: 'https://www.kmsh.al/' },
    DZ: { folder: 'dz', name: 'MARW', url: 'https://www.marw.gov.dz/' }
};

/** Sentinel method value: "serve the country's baked authority timetable". */
export const AUTHORITY_METHOD = 'AUTHORITY';

/** Beyond this the location is not this page's covered city, so R1 does not apply (degrees). */
const SAME_CITY_DEG = 0.5;

/** Is there an authority timetable for this location's country? */
export function hasAuthorityTimetable(cc) {
    return Boolean(cc && AUTHORITY_TIMETABLES[cc]);
}

/** The authority's display name + url for a country, or null. Synchronous, so settings can label
 *  the dropdown option without touching the table. */
export function authorityInfo(cc) {
    return (cc && AUTHORITY_TIMETABLES[cc]) || null;
}

/** Minutes from local midnight for a 24-hour "HH:MM" cell. */
function minutesOf(hhmm) {
    const [h, m] = hhmm.trim().split(':').map(Number);
    return Number.isFinite(h) && Number.isFinite(m) ? h * 60 + m : NaN;
}

/**
 * The baked authority table belongs to THIS page's city. It only applies when the active
 * location is that city (the covered-city navigation invariant keeps them aligned); if the
 * location is elsewhere, R1 does not apply and the caller computes.
 */
function locMatchesPage(loc) {
    const ds = document.body.dataset;
    if (!ds.presetLat || !ds.presetLng) return true;   // no preset to disagree with
    return Math.abs(Number(ds.presetLat) - loc.lat) <= SAME_CITY_DEG
        && Math.abs(Number(ds.presetLng) - loc.lng) <= SAME_CITY_DEG;
}

/**
 * The authority's published events for one civil day, read from the baked table on the page, or
 * null if this page carries no table / the location is not this page's city / the day has no row
 * (caller then computes).
 *
 * @param {Date} date  any instant of the civil day (used for month/day and the y/m/d base)
 * @param {{lat:number, lng:number, cc:string}} loc
 * @returns {{source:string, sourceUrl:string, zone:string,
 *   events:{fajr:Date,sunrise:Date,dhuhr:Date,asr:Date,maghrib:Date,isha:Date}}|null}
 */
export function authorityDayEvents(date, loc) {
    const section = document.querySelector('.authority-timetable');
    if (!section || section.dataset.computed === 'true' || !locMatchesPage(loc)) return null;

    const key = `${date.getMonth() + 1}-${date.getDate()}`;
    const row = section.querySelector(`tr[data-md="${key}"]`);
    if (!row) return null;   // gap / leap day -> caller computes

    // Cells: [day, fajr, sunrise, dhuhr, asr, maghrib, isha]; indices 1..6 are the times.
    const cells = row.querySelectorAll('td');
    if (cells.length < 7) return null;
    const mins = [1, 2, 3, 4, 5, 6].map((i) => minutesOf(cells[i].textContent));
    if (mins.some((m) => !Number.isFinite(m))) return null;

    // Build Dates in the civil day, from minutes-of-local-midnight (matches the computed path).
    const at = (m) => new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, m, 0);
    const [fajr, sunrise, dhuhr, asr, maghrib, isha] = mins;
    return {
        source: section.dataset.authority,
        sourceUrl: section.dataset.authorityUrl,
        zone: section.dataset.zone,
        events: {
            fajr: at(fajr), sunrise: at(sunrise), dhuhr: at(dhuhr),
            asr: at(asr), maghrib: at(maghrib), isha: at(isha)
        }
    };
}
