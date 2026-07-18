/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Fiqh engine - verdict computation (spec §3.3-3.6). Pure functions, zero DOM.
 * The rule matrix is DATA (assets/data/rule_sets/*.json), never hardcoded
 * here; this module only applies it plus the behavioural exceptions
 * (FAJR_EXPIRED, TODAYS_ASR, FRIDAY_ZENITH, MAKKAH_HARAM).
 *
 * Hard invariant 1 (spec §3.6): fardCurrent for a running prayer time never
 * yields a bare PROHIBITED. Enforced defensively at the end of computeVerdict.
 *
 * @author Fahim Farook
 */

import { WINDOW, computeWindows, windowAt, nextBoundary, boundaries, graceAt } from './windows.js';

export const PRAYER_TYPES = Object.freeze(['pureNafl', 'qada', 'janazah', 'causalNafl', 'fardCurrent']);

/**
 * @typedef {Object} Verdict
 * @property {'PERMITTED'|'DISLIKED'|'PROHIBITED'|'PERMITTED_WITH_WARNING'} status
 * @property {string} window        WINDOW value
 * @property {Date|null} windowEndsAt   end of the restricting window
 * @property {Date|null} permittedAt    instant this prayer type becomes permitted again (drives the red/amber countdown)
 * @property {Date|null} nextBoundary   next window boundary after now (drives the green countdown)
 * @property {string} reason        strings-table key (§5.7)
 * @property {string[]} notes       strings-table note suffixes, e.g. ["MAKRUH_TAHRIMI"]
 * @property {string[]} exceptionsApplied  e.g. ["FRIDAY_ZENITH", "TODAYS_ASR"]
 * @property {{phase:string, window:string}|null} grace
 * @property {string} ruleSetVersion
 */

/** Reason key for a restricted (non-PERMITTED) verdict in a given window. */
const RESTRICTED_REASON = {
    [WINDOW.SUNRISE]: 'reason.SUNRISE',
    [WINDOW.ZENITH]: 'reason.ZENITH',
    [WINDOW.SUNSET]: 'reason.SUNSET',
    [WINDOW.AFTER_FAJR]: 'reason.AFTER_FAJR',
    [WINDOW.AFTER_ASR]: 'reason.AFTER_ASR'
};

/** Extract numeric params {P_sunrise, P_zenith, P_isfirar} from a rule set. */
export function ruleParams(ruleSet) {
    return {
        P_sunrise: ruleSet.params.P_sunrise.value,
        P_zenith: ruleSet.params.P_zenith.value,
        P_isfirar: ruleSet.params.P_isfirar.value
    };
}

/**
 * Compute the verdict for one instant.
 *
 * @param {Date} now
 * @param {import('./windows.js').DayEvents} events  valid day events (caller handles degradation, spec §3.7)
 * @param {Object} ruleSet   parsed rule_sets/<madhhab>.json
 * @param {Object} settings  {prayerType, isFriday, inMakkahHaram}
 * @param {boolean} [probe]  internal: skip permittedAt computation while probing
 * @returns {Verdict}
 */
export function computeVerdict(now, events, ruleSet, settings, probe = false) {
    const windows = computeWindows(events, ruleParams(ruleSet));
    const win = windowAt(now, windows);
    const grace = graceAt(now, windows);
    const next = nextBoundary(now, windows);
    const exceptions = [];
    const base = {
        window: win ? win.id : WINDOW.CLEAR,
        windowEndsAt: win ? win.end : null,
        nextBoundary: next,
        permittedAt: null,
        grace,
        ruleSetVersion: ruleSet.version
    };

    if (!win) {
        return { ...base, status: 'PERMITTED', reason: 'reason.CLEAR', notes: [], exceptionsApplied: [] };
    }

    // Madhhab-level global exceptions (Shāfiʿī, spec §3.4).
    const rsExceptions = ruleSet.exceptions || [];
    if (settings.inMakkahHaram && rsExceptions.includes('MAKKAH_HARAM')) {
        exceptions.push('MAKKAH_HARAM');
        return { ...base, status: 'PERMITTED', reason: 'reason.PERMITTED_GENERIC', notes: ['MAKKAH_HARAM'], exceptionsApplied: exceptions };
    }
    if (settings.isFriday && win.id === WINDOW.ZENITH && rsExceptions.includes('FRIDAY_ZENITH')) {
        exceptions.push('FRIDAY_ZENITH');
        return { ...base, status: 'PERMITTED', reason: 'reason.PERMITTED_GENERIC', notes: ['FRIDAY_ZENITH'], exceptionsApplied: exceptions };
    }

    const cell = ruleSet.matrix[win.id][settings.prayerType];
    let status = cell.status;
    let notes = (cell.notes || []).slice();
    let reason;

    if (status === 'PERMITTED') {
        reason = 'reason.PERMITTED_GENERIC';
    } else if (status === 'PERMITTED_WITH_WARNING' && notes.includes('TODAYS_ASR')) {
        // The single most important cell (spec §3.3 fn 8).
        exceptions.push('TODAYS_ASR');
        reason = 'warn.TODAYS_ASR';
    } else {
        reason = RESTRICTED_REASON[win.id];
    }

    // Fajr expired at sunrise (spec §3.3 fn 5): guidance, never a bare NO.
    if (settings.prayerType === 'fardCurrent' && notes.includes('FAJR_EXPIRED')) {
        exceptions.push('FAJR_EXPIRED');
        reason = 'warn.FAJR_EXPIRED';
    }

    // Hard invariant 1 (spec §3.6): a fardCurrent verdict may only be
    // PERMITTED, PERMITTED_WITH_WARNING, or the Fajr-expired guidance.
    // Any other PROHIBITED/DISLIKED cell for fardCurrent is a data error:
    // fail PERMITTED, never wrongly stop an obligatory prayer.
    if (settings.prayerType === 'fardCurrent'
        && (status === 'PROHIBITED' || status === 'DISLIKED')
        && !exceptions.includes('FAJR_EXPIRED')) {
        status = 'PERMITTED';
        reason = 'reason.PERMITTED_GENERIC';
        notes = [];
    }

    const verdict = { ...base, status, reason, notes, exceptionsApplied: exceptions, permittedAt: null };

    // "You can pray in {mm:ss}" must point at the instant this prayer type
    // actually becomes permitted, not merely the current window's edge:
    // consecutive windows can chain (AFTER_FAJR flows into SUNRISE for nafl).
    // A wrong promise here violates product principle 1.
    if (!probe && (status === 'PROHIBITED' || status === 'DISLIKED')) {
        for (const b of boundaries(windows)) {
            if (b.getTime() <= now.getTime()) continue;
            const later = computeVerdict(new Date(b.getTime() + 1000), events, ruleSet, settings, true);
            if (later.status === 'PERMITTED' || later.status === 'PERMITTED_WITH_WARNING') {
                verdict.permittedAt = b;
                break;
            }
        }
        // Past the last boundary every window is behind us: CLEAR. No match
        // can only mean data trouble; leave null and the UI shows no promise.
    }
    return verdict;
}

/**
 * True when this rule set has been signed off by a named scholar. Until then
 * the footer shows the plain provenance line (sources + version), not a scholar
 * name - it never fabricates verification.
 */
export function isVerified(ruleSet) {
    return Boolean(ruleSet.verified_at);
}
