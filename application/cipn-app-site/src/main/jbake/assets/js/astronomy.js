/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Astronomy helpers (spec §4): astronomical solar noon plus the pure period/next-event
 * helpers shared by the covered (authority-table) and computed (WASM engine) paths. The
 * prayer-time kernel itself moved to engine.js (the WASM engine; ADR-0015) - this module
 * is framework-free and has no adhan.js dependency. W3 (zenith) derives from the solar
 * transit computed here, never from a timetable or the method-adjusted Dhuhr (spec §3.6 inv 2).
 *
 * @author Fahim Farook
 */

/**
 * Calculation methods exposed in settings (spec §4). Labels name the AUTHORITY each
 * convention belongs to, since that is what a user recognises and trusts. The angles/offsets
 * behind each key live in calculation-method.xml, read by the WASM engine (engine.js).
 */
export const METHODS = Object.freeze([
    { key: 'ACJU', label: 'ACJU (Sri Lanka)' },
    { key: 'Kemenag', label: 'Kemenag (Indonesia)' },
    { key: 'MuslimWorldLeague', label: 'Muslim World League' },
    { key: 'NorthAmerica', label: 'ISNA (North America)' },
    { key: 'Egyptian', label: 'Egyptian Survey Authority' },
    { key: 'UmmAlQura', label: 'Umm al-Qura (Saudi Arabia)' },
    { key: 'Karachi', label: 'University of Islamic Sciences, Karachi' },
    { key: 'Tehran', label: 'Institute of Geophysics, Tehran (Iran)' },
    { key: 'Gulf', label: 'Gulf Region (UAE / Awqaf)' },
    { key: 'Dubai', label: 'Dubai (UAE, unofficial)' },
    { key: 'Kuwait', label: 'Kuwait (Awqaf)' },
    { key: 'Qatar', label: 'Qatar (Calendar House)' },
    { key: 'Singapore', label: 'MUIS (Singapore)' },
    { key: 'Turkey', label: 'Diyanet (Turkey)' },
    { key: 'France', label: 'UOIF (France)' },
    { key: 'Russia', label: 'Spiritual Administration of Muslims (Russia)' },
    { key: 'MoonsightingCommittee', label: 'Moonsighting Committee Worldwide' }
]);

/**
 * Astronomical solar noon (transit) for a civil date at a longitude, via the
 * NOAA equation-of-time approximation (accurate to well under a minute,
 * ample for a window that starts P_zenith >= 5 minutes before it).
 * @param {Date} date  any instant of the civil day in question
 * @param {number} lng degrees east
 * @returns {Date}
 */
export function solarNoon(date, lng) {
    const start = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
    const doy = Math.floor((start - Date.UTC(date.getFullYear(), 0, 0)) / 86400000);
    const g = (2 * Math.PI / 365) * (doy - 1 + 0.5); // fractional year at ~noon
    const eotMin = 229.18 * (0.000075
        + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g)
        - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));
    const noonUtcMin = 720 - 4 * lng - eotMin;
    let noon = new Date(start + noonUtcMin * 60000);
    // Pin the transit nearest to the location's civil-day noon; longitudes
    // near the antimeridian can land a day off in UTC terms.
    const localNoonGuess = new Date(start + (720 - 4 * lng) * 60000);
    if (Math.abs(noon - localNoonGuess) > 12 * 3600000) {
        noon = new Date(noon.getTime() - Math.sign(noon - localNoonGuess) * 86400000);
    }
    return noon;
}

/**
 * @typedef {Object} DayResult
 * @property {boolean} valid  false -> degrade honestly (spec §3.7), never invent a window
 * @property {import('./engine/windows.js').DayEvents|null} events
 */

/**
 * Assemble a DayEvents from an AUTHORITY's five published prayer times plus a
 * computed solar noon (authorities do not publish the zenith). Same shape and
 * same strict-ordering guard as computeDayEvents, so downstream code cannot
 * tell whether the times were published or computed - only that they are valid.
 *
 * @param {{fajr:Date,sunrise:Date,dhuhr:Date,asr:Date,maghrib:Date,isha:Date}} pub
 * @param {Date} date
 * @param {number} lng
 * @returns {DayResult}
 */
export function assembleAuthorityEvents(pub, date, lng) {
    const events = {
        fajr: pub.fajr, sunrise: pub.sunrise, solarNoon: solarNoon(date, lng),
        dhuhr: pub.dhuhr, asr: pub.asr, maghrib: pub.maghrib, isha: pub.isha
    };
    const ordered = [events.fajr, events.sunrise, events.dhuhr, events.asr, events.maghrib, events.isha];
    for (let i = 0; i < ordered.length; i++) {
        if (!(ordered[i] instanceof Date) || !isFinite(ordered[i].getTime())) return { valid: false, events: null };
        if (i > 0 && ordered[i].getTime() <= ordered[i - 1].getTime()) return { valid: false, events: null };
    }
    return { valid: true, events };
}

/**
 * When Isha's time ends - which is two different answers, and conflating them
 * is why "Isha until Fajr" is misleading guidance.
 *
 *   preferred (waqt al-ikhtiyari): the virtue window. Ḥanafī/Ḥanbalī close it
 *       at Islamic midnight; Shāfiʿī/Mālikī at the end of the first third.
 *   valid (waqt al-darura): true dawn, in all four madhhabs. Praying after the
 *       preferred window is disliked/sinful without excuse, but the prayer is
 *       still valid - so the app must never present the preferred end as a
 *       deadline after which Isha is lost.
 *
 * Islamic midnight is the midpoint of sunset -> TRUE DAWN, not sunset ->
 * sunrise. The common library default (praytimes' "Standard", Aladhan's
 * midnightMode=0) uses sunset->sunrise and so overstates the window by
 * roughly 30-45 minutes. Refs: IslamQA 9940, Islamweb 355576 / 357307.
 *
 * @param {import('./engine/windows.js').DayEvents} events   today
 * @param {import('./engine/windows.js').DayEvents|null} tomorrow  for true dawn
 * @param {string} madhhab
 * @returns {{preferred:Date, valid:Date}|null}
 */
export function ishaEnds(events, tomorrow, madhhab) {
    if (!tomorrow) return null;
    const dawn = tomorrow.fajr;
    const night = dawn.getTime() - events.maghrib.getTime();
    if (!(night > 0)) return null;
    const fraction = madhhab === 'shafii' ? 1 / 3 : 1 / 2;
    return {
        preferred: new Date(events.maghrib.getTime() + night * fraction),
        valid: dawn
    };
}

/**
 * The next thing that will actually happen, named. "Window changes in 9:45:34"
 * tells nobody what is about to change; "Isha starts in 9:45:34" does.
 *
 * Considers both prayer entries and restricted-window starts, and returns
 * whichever comes first, so the countdown never promises the wrong event.
 *
 * @param {Date} now
 * @param {import('./engine/windows.js').DayEvents} events   today
 * @param {import('./engine/windows.js').Win[]} windows
 * @param {import('./engine/windows.js').DayEvents|null} tomorrow
 * @returns {{at:Date, kind:'prayer'|'window', name:string}|null}
 */
export function nextEvent(now, events, windows, tomorrow) {
    const t = now.getTime();
    const cands = [];
    for (const key of ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha']) {
        cands.push({ at: events[key], kind: 'prayer', name: key });
    }
    // Sunrise is not a prayer, but it ends Fajr's time - worth naming.
    cands.push({ at: events.sunrise, kind: 'prayer', name: 'sunrise' });
    for (const w of windows) {
        if (w.end > w.start) cands.push({ at: w.start, kind: 'window', name: w.id });
    }
    if (tomorrow) cands.push({ at: tomorrow.fajr, kind: 'prayer', name: 'fajr' });

    const future = cands.filter((c) => c.at.getTime() > t).sort((a, b) => a.at - b.at);
    return future.length ? future[0] : null;
}

/**
 * Which obligatory prayer's time is running at `now`, and when it ends.
 * Between sunrise and Dhuhr none runs - Fajr's time expired at sunrise.
 * Before Fajr, the previous day's Isha is still running.
 *
 * @param {Date} now
 * @param {import('./engine/windows.js').DayEvents} events   today
 * @param {import('./engine/windows.js').DayEvents|null} tomorrow  for Isha's end
 * @returns {{prayer:string, endsAt:Date|null}|null} null = no obligatory time running
 */
export function currentPeriod(now, events, tomorrow, madhhab) {
    const t = now.getTime();
    if (t >= events.isha.getTime()) {
        // Isha's PREFERRED end, not its validity bound - "until Fajr" is true
        // but useless, and hides that the virtue window closed hours earlier.
        const ends = ishaEnds(events, tomorrow, madhhab);
        return ends
            ? { prayer: 'isha', endsAt: ends.preferred, endKind: 'preferred', validUntil: ends.valid }
            : { prayer: 'isha', endsAt: null, endKind: 'valid' };
    }
    if (t >= events.maghrib.getTime()) return { prayer: 'maghrib', endsAt: events.isha, endKind: 'valid' };
    if (t >= events.asr.getTime()) return { prayer: 'asr', endsAt: events.maghrib, endKind: 'valid' };
    if (t >= events.dhuhr.getTime()) return { prayer: 'dhuhr', endsAt: events.asr, endKind: 'valid' };
    if (t >= events.sunrise.getTime()) return null;   // Fajr expired, Dhuhr not yet in
    if (t >= events.fajr.getTime()) return { prayer: 'fajr', endsAt: events.sunrise, endKind: 'valid' };
    return { prayer: 'isha', endsAt: events.fajr, endKind: 'valid' };   // pre-dawn: yesterday's Isha, valid to true dawn
}
