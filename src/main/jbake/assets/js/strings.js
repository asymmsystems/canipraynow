/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Copy-string loader (spec §5.7). Keys are stable for future i18n
 * (including RTL Arabic); English ships in js/strings/en.json.
 *
 * @author Fahim Farook
 */

let table = {};

export async function loadStrings(lang = 'en') {
    const res = await fetch(`/js/strings/${lang}.json`);
    table = await res.json();
}

/**
 * Resolve a copy key with {placeholder} interpolation.
 * @param {string} key
 * @param {Object<string,string|number>} [vars]
 */
export function t(key, vars) {
    let s = table[key];
    if (s === undefined) return key; // visible fallback beats silent blank
    if (vars) {
        for (const [k, v] of Object.entries(vars)) {
            s = s.split(`{${k}}`).join(String(v));
        }
    }
    return s;
}
