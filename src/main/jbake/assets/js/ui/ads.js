/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Ad slot (spec §9 Phase B): a single network placement, configured from
 * data/ads.json so it can be switched off without a deploy ({enabled:false}
 * empties the slot instantly, same kill-switch contract as the sponsor card).
 *
 * Unlike the Phase A sponsor card this DOES load a third-party script, so it is
 * deliberately fenced in: exactly one placement, never above the ruler, injected
 * only after the page is interactive and idle so it cannot compete with the
 * verdict for first paint, and into a slot whose height is reserved in CSS so a
 * late-arriving unit cannot shift layout (spec §6, CLS budget).
 *
 * @author Fahim Farook
 */

/** @param {HTMLElement} host */
export async function renderAd(host) {
    if (!host) return;

    let cfg;
    try {
        cfg = await (await fetch('/data/ads.json')).json();
    } catch (e) {
        host.hidden = true;
        return;
    }

    // Kill-switch and config errors both fail safe to an empty, hidden slot: an
    // ad is never the reason a page looks broken.
    if (!cfg || cfg.enabled !== true || !cfg.src) {
        host.hidden = true;
        host.innerHTML = '';
        return;
    }

    const delay = Number(cfg.delayMs) > 0 ? Number(cfg.delayMs) : 3000;

    const inject = () => {
        const tag = document.createElement('script');
        tag.src = cfg.src;
        tag.async = true;
        host.hidden = false;
        host.appendChild(tag);
    };

    // Hold the tag back past the point the answer is on screen, then wait for an
    // idle moment. The verdict is the product; the ad is not.
    setTimeout(() => {
        if ('requestIdleCallback' in window) {
            window.requestIdleCallback(inject, { timeout: 2000 });
        } else {
            inject();
        }
    }, delay);
}
