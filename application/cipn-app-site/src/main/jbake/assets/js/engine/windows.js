/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Fiqh engine - window computation (spec §3.1). Pure functions, zero DOM.
 * Windows are computed from the day's astronomical events plus the rule
 * set's scholar-set boundary parameters. Never anchored to timetables.
 *
 * @author Fahim Farook
 */

export const WINDOW = Object.freeze({
    CLEAR: 'CLEAR',
    AFTER_FAJR: 'AFTER_FAJR',
    SUNRISE: 'SUNRISE',
    ZENITH: 'ZENITH',
    AFTER_ASR: 'AFTER_ASR',
    SUNSET: 'SUNSET'
});

/** Strict (absolute) windows checked before the extended (nafl-restricted) ones. */
const STRICT = [WINDOW.SUNRISE, WINDOW.ZENITH, WINDOW.SUNSET];

const MINUTE = 60 * 1000;
export const GRACE_MS = 60 * 1000; // ±60 s boundary grace (spec §3.1)

/**
 * @typedef {Object} DayEvents
 * @property {Date} fajr
 * @property {Date} sunrise
 * @property {Date} solarNoon  astronomical transit - W3 derives from this ONLY (spec §3.6 inv 2)
 * @property {Date} dhuhr      zawāl passed (Adhan.js dhuhr)
 * @property {Date} asr
 * @property {Date} maghrib
 * @property {Date} isha
 */

/**
 * @typedef {Object} Win
 * @property {string} id     WINDOW value (never CLEAR)
 * @property {Date} start    inclusive
 * @property {Date} end      exclusive
 */

/**
 * Compute the five restricted windows W1-W5 for one day.
 * @param {DayEvents} events
 * @param {{P_sunrise:number, P_zenith:number, P_isfirar:number}} params  minutes
 * @returns {Win[]} in chronological order
 */
export function computeWindows(events, params) {
    const sunriseEnd = new Date(events.sunrise.getTime() + params.P_sunrise * MINUTE);
    const zenithStart = new Date(events.solarNoon.getTime() - params.P_zenith * MINUTE);
    // High-latitude winter can push Asr inside the final P_isfirar minutes.
    // Clamp so AFTER_ASR degenerates to zero width rather than inverting
    // (an inverted window silently drops its zone from the ruler).
    const isfirarStart = new Date(
        Math.max(events.maghrib.getTime() - params.P_isfirar * MINUTE, events.asr.getTime()));
    return [
        { id: WINDOW.AFTER_FAJR, start: events.fajr, end: events.sunrise },
        { id: WINDOW.SUNRISE, start: events.sunrise, end: sunriseEnd },
        { id: WINDOW.ZENITH, start: zenithStart, end: events.dhuhr },
        { id: WINDOW.AFTER_ASR, start: events.asr, end: isfirarStart },
        { id: WINDOW.SUNSET, start: isfirarStart, end: events.maghrib }
    ];
}

/**
 * The active window at `now`. Strict windows win over extended ones if the
 * geometry ever overlaps (defensive; they are disjoint by construction).
 * @param {Date} now
 * @param {Win[]} windows
 * @returns {Win|null} null means CLEAR
 */
export function windowAt(now, windows) {
    const t = now.getTime();
    const hit = (w) => t >= w.start.getTime() && t < w.end.getTime();
    const strict = windows.find((w) => STRICT.includes(w.id) && hit(w));
    if (strict) return strict;
    return windows.find(hit) || null;
}

/**
 * All boundary instants of the day, sorted, deduplicated.
 * @param {Win[]} windows
 * @returns {Date[]}
 */
export function boundaries(windows) {
    const ts = new Set();
    for (const w of windows) {
        ts.add(w.start.getTime());
        ts.add(w.end.getTime());
    }
    return [...ts].sort((a, b) => a - b).map((t) => new Date(t));
}

/**
 * Next boundary strictly after `now`, or null if the day's windows are exhausted.
 * @param {Date} now
 * @param {Win[]} windows
 * @returns {Date|null}
 */
export function nextBoundary(now, windows) {
    const t = now.getTime();
    for (const b of boundaries(windows)) {
        if (b.getTime() > t) return b;
    }
    return null;
}

/**
 * Grace state within ±60 s of a boundary (spec §5.3): the window about to
 * start ("starting") or the one about to end ("ending"). The verdict itself
 * does NOT flip until the boundary is crossed; grace is presentation only.
 * @param {Date} now
 * @param {Win[]} windows
 * @returns {{phase:'starting'|'ending', window:string}|null}
 */
export function graceAt(now, windows) {
    const t = now.getTime();
    for (const w of windows) {
        if (t < w.start.getTime() && w.start.getTime() - t <= GRACE_MS) {
            return { phase: 'starting', window: w.id };
        }
        if (t < w.end.getTime() && w.end.getTime() - t <= GRACE_MS) {
            return { phase: 'ending', window: w.id };
        }
    }
    return null;
}
