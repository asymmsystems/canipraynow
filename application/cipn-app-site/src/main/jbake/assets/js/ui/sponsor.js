/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Sponsor slot (spec §9 Phase A): native cards from data/sponsors.json,
 * network-first (the SW handles caching), always labelled, never above the
 * ruler, and a kill-switch ({enabled:false}) empties the slot instantly.
 * Zero third-party scripts.
 *
 * @author Fahim Farook
 */

import { esc } from '../utils.js';

/** @param {HTMLElement} host */
export async function renderSponsor(host) {
    let data;
    try {
        data = await (await fetch('/data/sponsors.json')).json();
    } catch (e) {
        host.hidden = true;
        return;
    }
    if (!data || data.enabled !== true || !Array.isArray(data.slots) || data.slots.length === 0) {
        host.hidden = true;
        host.innerHTML = '';
        return;
    }
    const s = data.slots[0];
    // label is a required field, either "Sponsored" (paid placement) or
    // "Affiliate" (commission link). A slot missing it is a config error, so
    // fail safe and render nothing rather than an unlabelled commercial card.
    if (!s.label) {
        host.hidden = true;
        host.innerHTML = '';
        return;
    }
    host.hidden = false;
    host.innerHTML = `<a class="sponsor-card" href="/go/${esc(s.id)}" rel="sponsored noopener">
        <span class="sponsor-label">${esc(s.label)}</span>
        ${s.img ? `<img src="${esc(s.img)}" alt="" loading="lazy" width="48" height="48">` : ''}
        <span class="sponsor-body"><strong>${esc(s.title)}</strong><span>${esc(s.body)}</span></span>
    </a>`;
}
