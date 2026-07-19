/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * localStorage wrapper (spec §6). Keys: cipn.location | cipn.method |
 * cipn.madhhab | cipn.clock24 | cipn.makkah. Prayer type is per-session
 * only (spec §5.4) and lives in sessionStorage.
 *
 * @author Fahim Farook
 */

const NS = 'cipn.';

function safeGet(store, key) {
    try {
        const raw = store.getItem(NS + key);
        return raw === null ? null : JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

function safeSet(store, key, value) {
    try {
        store.setItem(NS + key, JSON.stringify(value));
    } catch (e) { /* private mode / quota - settings just won't persist */ }
}

export const storage = {
    /** @returns {{name:string, lat:number, lng:number, tz:string, cc:string}|null} */
    getLocation() { return safeGet(localStorage, 'location'); },
    setLocation(loc) { safeSet(localStorage, 'location', loc); },

    // The user's EXPLICIT method choice, or null. Deliberately a NEW key
    // ('methodChoice'): an older build persisted auto-derived methods under
    // 'method', and that stale value made the app read a country default as if
    // the user had chosen it - which is why New York kept showing Karachi. The
    // new key ignores that history and starts null, so method follows country
    // until the user actually picks one.
    getMethodChoice() { return safeGet(localStorage, 'methodChoice'); },
    setMethodChoice(m) { safeSet(localStorage, 'methodChoice', m); },

    /** null = the user has never chosen; the country default applies until they do. */
    getMadhhab() { return safeGet(localStorage, 'madhhab'); },
    setMadhhab(m) { safeSet(localStorage, 'madhhab', m); },

    getClock24() { return Boolean(safeGet(localStorage, 'clock24')); },
    setClock24(v) { safeSet(localStorage, 'clock24', Boolean(v)); },

    getMakkah() { return Boolean(safeGet(localStorage, 'makkah')); },
    setMakkah(v) { safeSet(localStorage, 'makkah', Boolean(v)); },

    getPrayerType() { return safeGet(sessionStorage, 'prayerType') || 'pureNafl'; },
    setPrayerType(t) { safeSet(sessionStorage, 'prayerType', t); }
};
