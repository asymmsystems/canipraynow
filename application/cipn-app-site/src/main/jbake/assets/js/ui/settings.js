/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Settings drawer + first-run city picker (spec §5.6, §12). The Makkah
 * toggle is user-set only, never GPS-inferred.
 *
 * The drawer is a VIEW over app state, never a second copy of it: syncSettings()
 * re-renders it from the live state after every recompute. Without that the
 * drawer drifts - showing "Where are you praying?" while the app is happily
 * computing Colombo, and offering a method the app is not using.
 *
 * @author Fahim Farook
 */

import { METHODS } from '../astronomy.js';
import { authorityInfo, AUTHORITY_METHOD } from '../timetables.js';
import { t } from '../strings.js';
import { esc } from '../utils.js';
import { loadCities, searchCities, detectByGps, detectByEdge, nearestCity } from '../location.js';

let current = null;   // {settings, onChange} - kept so syncSettings can re-render

/**
 * @param {Object} opts
 * @param {Object} opts.settings   {location, method, madhhab, clock24, makkah}
 * @param {(patch:Object) => void} opts.onChange  invoked with the changed field(s)
 */
export function initSettings(opts) {
    current = opts;
    const drawer = document.getElementById('settings-drawer');
    const btn = document.getElementById('settings-btn');
    render(drawer, opts);
    btn.addEventListener('click', () => toggle(drawer, btn, true));
    drawer.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') toggle(drawer, btn, false);
    });
}

/**
 * Re-render the drawer from live app state. Called after every recompute, so
 * the controls always show what the app is actually computing with.
 * @param {Object} settings
 * @param {Object} [methodDefaults]  parsed method-defaults.json, for the recommendation
 */
export function syncSettings(settings, methodDefaults) {
    if (!current) return;
    current.settings = settings;
    if (methodDefaults) current.methodDefaults = methodDefaults;
    const drawer = document.getElementById('settings-drawer');
    const wasOpen = drawer.classList.contains('drawer-open');
    render(drawer, current);
    drawer.classList.toggle('drawer-open', wasOpen);
    drawer.setAttribute('aria-hidden', String(!wasOpen));
}

function toggle(drawer, btn, open) {
    drawer.classList.toggle('drawer-open', open);
    drawer.setAttribute('aria-hidden', String(!open));
    btn.setAttribute('aria-expanded', String(open));
    // Focus the first real control, not the close button that precedes them.
    if (open) {
        const first = drawer.querySelector('.drawer-body select, .drawer-body input, .drawer-body button');
        if (first) first.focus();
    }
}

/**
 * Method <select>, with the country's authority surfaced as the recommendation
 * rather than the list being cut down to it. Filtering to a single "applicable"
 * method would silently strip a legitimate choice - plenty of people follow a
 * convention other than their country's default, and this app must not decide
 * that for them. So: recommended first, everything else still reachable.
 */
function methodOptions(settings, defaults) {
    const cc = settings.location && settings.location.cc;
    const country = settings.location && settings.location.country;
    const authority = authorityInfo(cc);
    const opt = (key, label) => `<option value="${key}" ${key === settings.method ? 'selected' : ''}>${esc(label)}</option>`;

    // Where we have the authority's OWN extracted timetable, that is the top
    // choice and the default: "{ACJU} published timetable". Every computed
    // method stays selectable below, and picking one recomputes - so the user
    // (in Sri Lanka too) can switch to a method and see the times change.
    const authorityGroup = authority
        ? `<optgroup label="${esc(t('settings.METHOD_AUTHORITY_GROUP'))}">`
            + opt(AUTHORITY_METHOD, t('settings.METHOD_AUTHORITY_OPTION', { authority: authority.name }))
            + `</optgroup>`
        : '';

    const recKey = (defaults && cc && defaults.byCountry[cc]) || (defaults && defaults.default);
    const rec = METHODS.find((m) => m.key === recKey);
    const methodOpt = (m) => opt(m.key, m.label);
    if (!rec) return authorityGroup + METHODS.map(methodOpt).join('');
    const others = METHODS.filter((m) => m.key !== rec.key);
    // With an authority timetable present, the computed list is framed as the
    // "compute instead" alternative; without one, it is the recommendation.
    const recLabel = authority
        ? t('settings.METHOD_COMPUTE_INSTEAD')
        : (country ? t('settings.METHOD_RECOMMENDED_FOR', { country }) : t('settings.METHOD_RECOMMENDED'));
    return authorityGroup
        + `<optgroup label="${esc(recLabel)}">${methodOpt(rec)}</optgroup>`
        + `<optgroup label="${esc(t('settings.METHOD_OTHER'))}">${others.map(methodOpt).join('')}</optgroup>`;
}

/**
 * The line under the method select. Only meaningful where the country has an
 * authority timetable: it tells the user whether they are seeing the
 * authority's own published times or a computed alternative, and how to switch.
 */
function methodNote(settings) {
    const authority = authorityInfo(settings.location && settings.location.cc);
    if (!authority) return '';
    const key = settings.method === AUTHORITY_METHOD
        ? 'settings.METHOD_NOTE_AUTHORITY'
        : 'settings.METHOD_NOTE_COMPUTED';
    return `<p class="setting-note">${esc(t(key, { authority: authority.name }))}</p>`;
}

function render(drawer, opts) {
    const { settings, onChange, methodDefaults } = opts;
    const hasLoc = Boolean(settings.location);
    drawer.innerHTML = `
    <div class="drawer-head">
        <h2>${esc(t('settings.TITLE'))}</h2>
        <button type="button" class="drawer-close" aria-label="Close">&#10005;</button>
    </div>
    <div class="drawer-body">
        <section class="setting">
            <h3>${esc(t('settings.LOCATION'))}</h3>
            <div class="location-slot location-slot-drawer" id="setting-location-slot">
                <button type="button" class="location-chip">
                    <svg class="pin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                        <path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11z"/>
                        <circle cx="12" cy="10" r="2.5"/>
                    </svg>
                    <span class="location-name">${esc(hasLoc ? settings.location.name : t('locate.PROMPT'))}</span>
                    <span class="location-change">${esc(t('settings.CHANGE'))}</span>
                </button>
                <div class="location-search" hidden>
                    <input type="search" placeholder="${esc(t('locate.SEARCH'))}" autocomplete="off" aria-label="${esc(t('locate.SEARCH'))}">
                    <div class="location-dropdown">
                        <ul class="city-results"></ul>
                        <button type="button" class="btn-secondary location-detect">${esc(t('locate.DETECT'))}</button>
                        <p class="setting-note location-note" hidden></p>
                    </div>
                </div>
            </div>
        </section>
        <section class="setting">
            <h3><label for="setting-method">${esc(t('settings.METHOD'))}</label></h3>
            <select id="setting-method">${methodOptions(settings, methodDefaults)}</select>
            ${methodNote(settings)}
        </section>
        <section class="setting">
            <h3>${esc(t('settings.BASIS'))}</h3>
            <div class="seg seg-stack" role="radiogroup">
                <button type="button" data-madhhab="evidence" class="seg-btn ${settings.madhhab === 'evidence' ? 'seg-active' : ''}">${esc(t('basis.evidence'))}</button>
                <button type="button" data-madhhab="hanafi" class="seg-btn ${settings.madhhab === 'hanafi' ? 'seg-active' : ''}">${esc(t('basis.hanafi'))}</button>
                <button type="button" data-madhhab="shafii" class="seg-btn ${settings.madhhab === 'shafii' ? 'seg-active' : ''}">${esc(t('basis.shafii'))}</button>
            </div>
            <p class="setting-note">${esc(t('settings.BASIS_NOTE'))}</p>
        </section>
        <section class="setting setting-row">
            <label for="setting-clock24">${esc(t('settings.CLOCK'))}</label>
            <input type="checkbox" id="setting-clock24" ${settings.clock24 ? 'checked' : ''}>
        </section>
        <section class="setting setting-row">
            <label for="setting-makkah">${esc(t('settings.MAKKAH'))}</label>
            <input type="checkbox" id="setting-makkah" ${settings.makkah ? 'checked' : ''}>
        </section>
        <p class="setting-note">${esc(t('settings.MAKKAH_NOTE'))}</p>
        <p class="setting-note">${esc(t('device.CLOCK_NOTE'))}</p>
        <p class="drawer-footer"><a href="/methodology.html">${esc(t('settings.METHODOLOGY'))}</a></p>
    </div>`;

    const btn = document.getElementById('settings-btn');
    drawer.querySelector('.drawer-close').addEventListener('click', () => toggle(drawer, btn, false));
    drawer.querySelector('#setting-method').addEventListener('change', (e) => onChange({ method: e.target.value }));
    drawer.querySelector('#setting-clock24').addEventListener('change', (e) => onChange({ clock24: e.target.checked }));
    drawer.querySelector('#setting-makkah').addEventListener('change', (e) => onChange({ makkah: e.target.checked }));
    for (const b of drawer.querySelectorAll('.seg-btn')) {
        b.addEventListener('click', () => {
            for (const x of drawer.querySelectorAll('.seg-btn')) x.classList.toggle('seg-active', x === b);
            onChange({ madhhab: b.dataset.madhhab });
        });
    }

    // The drawer's location control is the SAME inline slot as the canvas and
    // sidebar - one behaviour everywhere. The drawer re-renders on every sync,
    // so this is a fresh element each time; initLocationSlot wires it once.
    initLocationSlot(
        drawer.querySelector('#setting-location-slot'),
        (loc) => onChange({ location: loc }),
        { openNow: !hasLoc });
}

/** Label raw coordinates with the nearest bundled city (name, country, cc). */
async function enrich(loc) {
    if (!loc) return null;
    const cities = await loadCities();
    const near = nearestCity(cities, loc.lat, loc.lng);
    return {
        name: loc.name || (near ? near.name : `${loc.lat.toFixed(2)}, ${loc.lng.toFixed(2)}`),
        country: near ? near.country : null,
        lat: loc.lat,
        lng: loc.lng,
        tz: loc.tz,
        cc: loc.cc || (near ? near.cc : null)
    };
}

/**
 * Explicit "Use my location": GPS is the intent, so try it first (it now waits
 * long enough for the permission prompt), then fall back to the edge IP hint.
 */
export async function detectLocation() {
    return enrich((await detectByGps()) || (await detectByEdge()));
}

/**
 * First-load auto-locate: try the edge IP hint FIRST - it is instant, needs no
 * permission prompt, and gives an approximate city so the app answers straight
 * away. Only if that is unavailable (e.g. the local dev server has no /api/geo)
 * do we ask GPS. This is why the first screen should not be a "pick a city"
 * prompt: the location is fetched automatically.
 */
export async function autoLocate() {
    return enrich((await detectByEdge()) || (await detectByGps()));
}

/**
 * Wire a location "slot" so clicking the city swaps it to an inline search,
 * IN PLACE - no full-screen overlay. Used on the canvas, the sidebar, and in
 * the settings drawer, so the behaviour is identical everywhere.
 *
 * Slot markup (see app.ftl):
 *   .location-slot > button.location-chip (the city, tap to search)
 *                  > .location-search[hidden] > input + ul.city-results + detect
 *
 * @param {HTMLElement} slot
 * @param {(loc:Object) => void} onPick
 * @param {{openNow?:boolean}} [opts]
 */
export function initLocationSlot(slot, onPick, opts = {}) {
    if (!slot || slot.dataset.wired) {
        if (slot && opts.openNow) openSlot(slot, true);
        return;
    }
    slot.dataset.wired = '1';
    const chip = slot.querySelector('.location-chip');
    const input = slot.querySelector('.location-search input');
    const list = slot.querySelector('.city-results');
    const detect = slot.querySelector('.location-detect');
    const note = slot.querySelector('.location-note');

    chip.addEventListener('click', () => openSlot(slot, true));
    input.addEventListener('keydown', (e) => { if (e.key === 'Escape') openSlot(slot, false); });

    wireCitySearch(input, list, (loc) => { openSlot(slot, false); onPick(loc); });

    if (detect) {
        detect.addEventListener('click', async (e) => {
            const original = e.target.textContent;
            e.target.textContent = t('locate.DETECTING');
            if (note) note.hidden = true;
            const loc = await detectLocation();
            e.target.textContent = original;
            if (loc) { openSlot(slot, false); onPick(loc); }
            else if (note) {
                const cur = slot.querySelector('.location-name')?.textContent;
                note.textContent = cur ? t('locate.DENIED_KEEP', { city: cur }) : t('locate.DENIED');
                note.hidden = false;
            }
        });
    }

    if (opts.openNow) openSlot(slot, true);
}

/**
 * Open/close a location slot's inline search. Collapse-on-outside-click is a
 * document pointerdown listener (registered only while open, and only after the
 * opening click has finished), NOT focusout - focusout fired the instant the
 * chip hid and snapped the search shut before you could type.
 */
function openSlot(slot, open) {
    const chip = slot.querySelector('.location-chip');
    const search = slot.querySelector('.location-search');
    search.hidden = !open;
    chip.hidden = open;
    slot.classList.toggle('slot-searching', open);

    if (slot._closeOnOutside) {
        document.removeEventListener('pointerdown', slot._closeOnOutside, true);
        slot._closeOnOutside = null;
    }
    if (open) {
        slot.querySelector('.location-search input').focus();
        slot._closeOnOutside = (ev) => {
            if (!slot.contains(ev.target)) openSlot(slot, false);
        };
        // Defer so the click that opened it does not immediately close it.
        setTimeout(() => {
            if (slot._closeOnOutside) document.addEventListener('pointerdown', slot._closeOnOutside, true);
        }, 0);
    }
}

function wireCitySearch(input, list, onPick) {
    input.addEventListener('input', async () => {
        const cities = await loadCities();
        const hits = searchCities(cities, input.value);
        list.innerHTML = hits.map((c, i) =>
            `<li><button type="button" data-i="${i}">${esc(c.name)} <span>${esc(c.country)}</span></button></li>`).join('');
        for (const b of list.querySelectorAll('button')) {
            b.addEventListener('click', () => {
                const c = hits[Number(b.dataset.i)];
                list.innerHTML = '';
                input.value = '';
                onPick({ name: c.name, country: c.country, lat: c.lat, lng: c.lng, tz: c.tz, cc: c.cc });
            });
        }
    });
}
