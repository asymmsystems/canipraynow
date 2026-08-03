/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Boot + render loop (spec §6): render shell instantly -> settings ->
 * location -> day events -> 1000 ms tick against precomputed boundaries.
 * Full re-render only on window/state change; countdown and cursor are the
 * cheap per-second path.
 *
 * @author Fahim Farook
 */

import { computeDayEvents } from './engine.js';
import { assembleAuthorityEvents, currentPeriod, nextEvent } from './astronomy.js';
import { computeWindows } from './engine/windows.js';
import { computeVerdict, isVerified, ruleParams } from './engine/verdict.js';
import { storage } from './storage.js';
import { loadStrings, t } from './strings.js';
import { renderStatus, renderDegraded, renderNeutral, renderCountdown } from './ui/status.js';
import { renderChips } from './ui/chips.js';
import { renderRuler, updateCursor, rulerSummary, rulerA11yList, bindRulerTooltip } from './ui/ruler.js';
import { renderPrayers } from './ui/prayers.js';
import { renderSponsor } from './ui/sponsor.js';
import { renderAd } from './ui/ads.js';
import { initSettings, syncSettings, initLocationSlot, autoLocate } from './ui/settings.js';
import { isFridayIn, distanceKm } from './utils.js';
import { hasAuthorityTimetable, authorityDayEvents, AUTHORITY_METHOD } from './timetables.js';

const state = {
    location: null,      // {name, lat, lng, tz, cc}
    method: null,        // derived from the location's baked country method (country.xml)
    madhhab: 'hanafi',
    clock24: false,
    makkah: false,
    prayerType: 'pureNafl',
    ruleSets: {},        // hanafi/shafii parsed JSON
    today: null,         // DayEvents
    tomorrow: null,      // DayEvents (for the post-Isha green countdown)
    degraded: false,
    lastSig: null,
    lastMinute: null,
    deviceTz: Intl.DateTimeFormat().resolvedOptions().timeZone
};

/**
 * Adopt a new location: set it and persist it. Nothing else - the method is a
 * pure derivation of (methodChoice, country) recomputed on every recompute()
 * via resolveDefaults(), so changing the city always re-derives the method and
 * an unchosen one can never strand on the previous country (New York keeping
 * Karachi). An explicit choice in state.methodChoice always wins.
 */
function adoptLocation(loc) {
    state.location = loc;
    storage.setLocation(loc);
}

/** Within this, the nearest baked city is "here", so we navigate to its page (ADR-0016). */
const LOCALITY_KM = 50;

/**
 * Route a resolved location (ADR-0016). A baked city within the locality navigates to its own
 * page, so a covered city serves its exact published table (R1) and every location lands on its
 * city page (the single R1-vs-compute decision point). An off-grid location - no city within the
 * locality - is adopted here at its exact coordinates and computed inline. Navigation is
 * client-side (a link, not a 301/302); see ADR-0016 for why.
 */
function route(loc) {
    if (!loc) return;
    if (loc.uri && loc.nearKm <= LOCALITY_KM) {
        window.location.assign('/' + loc.uri);
        return;
    }
    adoptLocation(loc);
    recompute();
}

boot();

async function boot() {
    const [, evidence, hanafi, shafii] = await Promise.all([
        loadStrings('en'),
        fetch('/data/rule_sets/evidence.json').then((r) => r.json()),
        fetch('/data/rule_sets/hanafi.json').then((r) => r.json()),
        fetch('/data/rule_sets/shafii.json').then((r) => r.json())
    ]);
    state.ruleSets = { evidence, hanafi, shafii };

    state.madhhab = storage.getMadhhab();
    state.clock24 = storage.getClock24();
    state.makkah = storage.getMakkah();
    state.prayerType = storage.getPrayerType();
    // The user's explicit method choice (null until they pick one). state.method
    // is DERIVED from this plus the country on every recompute - never stored,
    // never stale.
    state.methodChoice = storage.getMethodChoice();

    // On a city/question page the page's OWN city wins, so the baked authority table always
    // matches the location it is served for (R1, ADR-0016). The saved location is used only off a
    // preset page (the home page). The preset carries the country's method, baked from country.xml.
    const ds = document.body.dataset;
    if (ds.presetLat) {
        state.location = {
            name: ds.presetName,
            country: ds.presetCountry || null,
            lat: Number(ds.presetLat),
            lng: Number(ds.presetLng),
            tz: ds.presetTz,
            cc: ds.presetCc || null,
            method: ds.countryMethod || null,
            madhhab: ds.countryMadhhab || null,
            preset: true
        };
    } else {
        state.location = storage.getLocation();
    }

    renderFooter();
    renderChips(document.getElementById('chips'), state.prayerType, (type) => {
        state.prayerType = type;
        storage.setPrayerType(type);
        state.lastSig = null; // force full re-render
        tick();
    });
    initSettings({
        settings: state,
        onChange(patch) {
            if ('location' in patch) { route(patch.location); return; }   // may navigate away
            Object.assign(state, patch);
            if ('method' in patch) { state.methodChoice = patch.method; storage.setMethodChoice(patch.method); }
            if ('madhhab' in patch) { storage.setMadhhab(patch.madhhab); renderFooter(); }
            if ('clock24' in patch) storage.setClock24(patch.clock24);
            if ('makkah' in patch) storage.setMakkah(patch.makkah);
            recompute();
        }
    });
    // The city is shown on the canvas and the sidebar; both are inline search
    // slots (no full-screen overlay). Picking a city there flows through the
    // same adoptLocation path as the settings drawer.
    const pick = (loc) => route(loc);
    initLocationSlot(document.getElementById('location-slot-canvas'), pick);
    initLocationSlot(document.getElementById('location-slot-sidebar'), pick);
    renderSponsor(document.getElementById('sponsor'));
    renderAd(document.getElementById('ad-slot-1'));
    registerSw();

    if (!state.location) {
        // Auto-fetch on first load (spec §6). Neutral canvas while we look, then
        // adopt whatever we find - IP hint first (instant, no prompt), GPS if
        // needed. Only if BOTH fail do we ask the user to pick a city. The first
        // screen is never a manual "pick a city" prompt.
        renderNeutral();
        const detected = await autoLocate();
        if (detected) {
            route(detected);   // navigates to the nearest city page, or adopts + computes off-grid
            return;
        }
        initLocationSlot(document.getElementById('location-slot-canvas'), pick, { openNow: true });
        return;
    }
    await recompute();
}

/**
 * Ruleset ids with a shipped matrix. 'evidence' is the DEFAULT: the Salafi /
 * ahl al-hadith position issued as one ruling for everyone, so the app does not
 * put a madhhab choice in the user's way (they can still pick one). Ḥanafī and
 * Shāfiʿī remain as explicit alternatives.
 */
const RULESETS = ['evidence', 'hanafi', 'shafii'];
const DEFAULT_RULESET = 'evidence';

/**
 * Country default for the calculation METHOD only. The ruleset is NOT
 * country-derived: everyone gets the evidence-based default unless they choose
 * otherwise, which is why the old "44 countries fall back on madhhab" problem
 * no longer exists - there is nothing to fall back to. An explicit user choice
 * is persisted and always wins.
 */
function resolveDefaults() {
    // Method = pure function of (explicit choice, country), recomputed EVERY time. No stored
    // state.method to go stale, so changing New York never keeps Karachi. The country's own
    // method is baked onto the location from country.xml - the preset carries it on a city page,
    // the routed / off-grid location carries it from the directory - so there is no
    // method-defaults.json fetch. Where we have baked an authority's real timetable the default is
    // the AUTHORITY sentinel (serve those exact times); elsewhere it is the country's computed
    // convention. The dropdown stays live: an explicit choice always wins.
    const cc = state.location && state.location.cc;
    const countryMethod = (state.location && state.location.method) || 'MuslimWorldLeague';
    let m = state.methodChoice;
    if (m === AUTHORITY_METHOD && !hasAuthorityTimetable(cc)) m = null;   // stale after travel
    if (!m) m = hasAuthorityTimetable(cc) ? AUTHORITY_METHOD : countryMethod;
    state.method = m;
    state.countryMethod = countryMethod;   // real computed fallback for AUTHORITY days/gaps
    // Ruleset: evidence-based for everyone by default; the user may override.
    // A stored value that names no shipped matrix would take the verdict down,
    // so it is always coerced back to a real one.
    if (!state.madhhab || !RULESETS.includes(state.madhhab)) {
        state.madhhab = DEFAULT_RULESET;
        renderFooter();
    }
}

/** Civil-date parts of `now` in the location's timezone. */
function civilDate(tz, now) {
    const parts = new Intl.DateTimeFormat('en-CA', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' })
        .formatToParts(now)
        .reduce((acc, p) => (acc[p.type] = p.value, acc), {});
    return { y: Number(parts.year), m: Number(parts.month), d: Number(parts.day), key: `${parts.year}-${parts.month}-${parts.day}` };
}

/** Adhan reads y/m/d from a device-local Date; noon avoids DST-edge day shifts. */
function dateFor(cd) {
    return new Date(cd.y, cd.m - 1, cd.d, 12, 0, 0);
}

/** Full recompute (spec §4 triggers): location/method/madhhab change, midnight, tz change. */
async function recompute() {
    if (!state.location) return;
    resolveDefaults();
    const now = new Date();
    const cd = civilDate(state.location.tz, now);
    state.dateKey = cd.key;
    const coords = { lat: state.location.lat, lng: state.location.lng };
    const today = await dayEvents(dateFor(cd), coords);
    state.timesSource = today.source;   // the authority whose times we used, or null
    // The drawer renders before the country defaults and time-source are known,
    // so re-render it from live state now: it must show the city, method, madhhab
    // AND the authority the app is ACTUALLY computing with, not what it guessed
    // at boot. In particular the method note depends on timesSource being set.
    syncSettings(state);
    if (!today.result.valid) {
        // A degraded day is a property of THIS date and place: at high latitude
        // the very next day may compute fine. Keep watching for the rollover
        // (and for device travel) instead of freezing until a page reload.
        state.degraded = true;
        state.today = null;
        renderDegraded();
        startLoop();
        return;
    }
    const tomorrowRef = new Date(dateFor(cd).getTime() + 26 * 3600000);
    const tomorrow = await dayEvents(tomorrowRef, coords);
    state.degraded = false;
    state.today = today.result.events;
    state.tomorrow = tomorrow.result.valid ? tomorrow.result.events : null;
    // Force a full re-render of BOTH the ruler (lastSig) and the prayer list +
    // its authority attribution (lastMinute). The prayer list is otherwise gated
    // per-minute, so a location/method/madhhab change within the same minute would
    // leave the sidebar showing the previous city's times until the minute ticked.
    state.lastSig = null;
    state.lastMinute = null;
    startLoop();
    tick();
}

/**
 * One civil day's events. When the effective method is the AUTHORITY sentinel
 * (the default wherever we have baked an authority's real table), serve those
 * exact published times; on any day the table does not cover, fall back to the
 * country's own computed convention. When the user has explicitly picked a
 * calculation method, compute with it - even in a country that has a table, so
 * the dropdown always changes the times the user sees.
 * @returns {Promise<{result: import('./astronomy.js').DayResult, source: Object|null}>}
 */
async function dayEvents(date, coords) {
    if (state.method === AUTHORITY_METHOD && hasAuthorityTimetable(state.location.cc)) {
        const auth = await authorityDayEvents(date, { ...coords, cc: state.location.cc });
        if (auth) {
            const result = assembleAuthorityEvents(auth.events, date, coords.lng);
            if (result.valid) return { result, source: auth };
        }
        // A day the table does not cover: compute with the country's real method.
        return { result: await computeDayEvents(date, coords, state.countryMethod, state.madhhab), source: null };
    }
    return { result: await computeDayEvents(date, coords, state.method, state.madhhab), source: null };
}

/**
 * The device's timezone changed - it travelled. The saved coordinates are now
 * suspect: recomputing them under a new timezone would produce confidently
 * wrong times (Colombo's sun, London's clock). So re-detect the position and
 * adopt it if it really moved; if detection is refused or fails, say plainly
 * that the location may be stale rather than silently pretending otherwise.
 */
async function relocate() {
    const detected = await detectLocation();
    if (detected && distanceKm(detected, state.location) > MOVED_KM) {
        adoptLocation(detected);
        toast(t('toast.LOCATION_CHANGED', { city: detected.name }));
    } else if (!detected) {
        toast(t('toast.TZ_CHANGED_CHECK'));
    } else {
        toast(t('toast.TZ_CHANGED'));   // same place, clock shifted (DST)
    }
    recompute();
}

/** Below this, treat it as the same locality: prayer times shift by well under a minute. */
const MOVED_KM = 50;

let timer = null;
function startLoop() {
    if (!timer) timer = setInterval(tick, 1000);
}

function tick() {
    if (!state.location) return;
    const now = new Date();

    // Midnight rollover + device-travel timezone change (spec §12). Checked
    // before the degraded guard, so a degraded day can recover on rollover.
    const cd = civilDate(state.location.tz, now);
    if (cd.key !== state.dateKey) { recompute(); return; }
    const devTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (devTz !== state.deviceTz) {
        state.deviceTz = devTz;
        relocate();   // the device travelled: recomputing the OLD coordinates would be worse than useless
        return;
    }
    if (state.degraded || !state.today) return;

    const ruleSet = state.ruleSets[state.madhhab];
    const verdict = computeVerdict(now, state.today, ruleSet, {
        prayerType: state.prayerType,
        isFriday: isFridayIn(state.location.tz, now),
        inMakkahHaram: state.makkah
    });
    // Post-Isha: the day's boundaries are exhausted; the next change is
    // tomorrow's Fajr (W1 opens).
    if (!verdict.nextBoundary && state.tomorrow) verdict.nextBoundary = state.tomorrow.fajr;

    const minuteKey = Math.floor(now.getTime() / 60000);
    const period = currentPeriod(now, state.today, state.tomorrow, state.madhhab);
    const windows = computeWindows(state.today, ruleParams(ruleSet));
    const ev = nextEvent(now, state.today, windows, state.tomorrow);
    // The period is part of the signature: crossing into Dhuhr's time changes
    // the "which prayer is due" line even when the verdict itself is unchanged.
    const sig = [verdict.window, verdict.status, verdict.reason, verdict.grace && verdict.grace.phase,
        state.prayerType, state.madhhab, period && period.prayer, period && period.endKind, ev && ev.name].join('|');
    const ctx = {
        prayerType: state.prayerType,
        tz: state.location.tz,
        clock24: state.clock24,
        sunriseMinutes: ruleParams(ruleSet).P_sunrise,
        period,
        nextEvent: ev,
        timesSource: state.timesSource
    };

    // The ruler is rebuilt ONLY when the verdict state changes. Rebuilding it
    // per minute would destroy the tooltip and any keyboard focus mid-use; the
    // cursor and its clock label are updated below without a re-render.
    if (sig !== state.lastSig) {
        renderStatus(verdict, ctx);
        const rulerHost = document.getElementById('ruler');
        ctx.summary = rulerSummary(windows, verdict, now, ctx);
        rulerHost.innerHTML = renderRuler(state.today, windows, ruleSet, now, ctx)
            + rulerA11yList(state.today, windows, ruleSet, ctx);
        bindRulerTooltip(rulerHost);
        renderLocationChip();
        renderFardNote(period);
        state.lastSig = sig;
    }
    // The prayer list carries relative times ("in 1 h 12 m"), so it ticks per
    // minute - it has no hover state to lose.
    if (minuteKey !== state.lastMinute) {
        renderPrayers(document.getElementById('prayers'), state.today, now, ctx);
        state.lastMinute = minuteKey;
    }
    renderCountdown(verdict, now, ctx);
    const svg = document.querySelector('#ruler svg');
    if (svg) updateCursor(svg, state.today, now, ctx);
}


function renderFooter() {
    const ruleSet = state.ruleSets[state.madhhab];
    const el = document.getElementById('ruleset-version');
    if (!el || !ruleSet) return;   // re-rendered once the madhhab default resolves
    // Footer provenance: a named scholar's line once verified, otherwise the
    // plain sources + version line. verified_at is never faked, so the scholar
    // form only appears when a real sign-off is present in the rule set.
    el.textContent = isVerified(ruleSet)
        ? t('footer.VERIFIED', { scholar: ruleSet.verified_by, date: ruleSet.verified_at, version: ruleSet.version })
        : t('footer.PROVENANCE', { version: ruleSet.version });
}

/** The city drives every number on screen, so it is named in every slot. */
function renderLocationChip() {
    if (!state.location) return;
    const name = state.location.name || '';
    for (const el of document.querySelectorAll('.location-name')) el.textContent = name;
}

/**
 * The safety caption only earns its place while the big answer is about some
 * OTHER prayer type - selecting "Today's fard" answers it directly.
 */
function renderFardNote(period) {
    const el = document.getElementById('fard-note');
    if (!el) return;
    el.textContent = period
        ? t('note.FARD_ALWAYS', { prayer: t('prayer.' + period.prayer) })
        : t('note.FARD_NONE');
    el.hidden = state.prayerType === 'fardCurrent';
}

let toastTimer = null;
function toast(msg) {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('toast-show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('toast-show'), 4000);
}

function registerSw() {
    if ('serviceWorker' in navigator && location.protocol === 'https:') {
        navigator.serviceWorker.register('/sw.js').catch(() => { /* offline still fine without SW */ });
    }
}
