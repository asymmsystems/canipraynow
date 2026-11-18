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
 * verdict for first paint, and into a slot placed last in the sidebar that is
 * laid out at its final size before the tag is requested, so a late-arriving
 * unit shifts nothing (spec §6, CLS budget).
 *
 * The network serves a fixed-size unit (ins.width x ins.height in ads.json: a
 * 728x90 leaderboard) that knows nothing of the slot's width. The unit is laid
 * out at that natural size and scaled down to the slot, never up, so the whole
 * creative shows at any width and stays clickable (see fitUnit).
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

    // Lay the slot out now, at the size it keeps: the frame (slot-wide, as tall as the unit
    // looks), the unit inside it (the network's natural size, reserved from the configured
    // width x height) and the attribution line. Only the tag itself is held back, so its late
    // arrival fills a box that is already there.
    const frame = document.createElement('div');
    frame.className = 'ad-frame';
    const unit = document.createElement('div');
    unit.className = 'ad-unit';
    const width = Number(cfg.ins && cfg.ins.width);
    const height = Number(cfg.ins && cfg.ins.height);
    if (width > 0 && height > 0) {
        unit.style.minWidth = `${width}px`;
        unit.style.minHeight = `${height}px`;
    }
    frame.appendChild(unit);
    host.appendChild(frame);
    // Optional attribution line the network requires beneath the unit. Rendered by us (not the
    // tag) so it is a plain labelled link, marked rel="sponsored" and opened in a new tab.
    if (cfg.attribution && cfg.attribution.url && cfg.attribution.text) {
        const p = document.createElement('p');
        p.className = 'ad-attribution';
        const a = document.createElement('a');
        a.href = cfg.attribution.url;
        a.textContent = cfg.attribution.text;
        a.rel = 'noopener nofollow sponsored';
        a.target = '_blank';
        p.appendChild(a);
        host.appendChild(p);
    }
    host.hidden = false;
    fitUnit(frame, unit);

    const inject = () => {
        // Some networks fill a placeholder element the loader finds by key (e.g. Muslim Ad
        // Network's <ins data-key>), rather than writing into the script's own parent. Create it
        // BEFORE the script so the loader sees it on first scan. Both go inside the unit, so
        // either kind of tag renders into the box that is scaled to the slot.
        if (cfg.ins && cfg.ins.dataKey) {
            const ins = document.createElement('ins');
            if (cfg.ins.class) ins.className = cfg.ins.class;
            ins.setAttribute('data-key', cfg.ins.dataKey);
            unit.appendChild(ins);
        }
        const tag = document.createElement('script');
        tag.src = cfg.src;
        tag.async = true;
        // A tag that cannot load (offline, blocked) leaves nothing to show: release the slot
        // rather than keep an empty reserved box on screen.
        tag.onerror = () => { host.hidden = true; };
        unit.appendChild(tag);
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

/**
 * Keep the unit scaled to the slot. The unit keeps its natural layout size (the configured
 * size, or more when the network renders a larger creative) and is scaled down, never up, to
 * the frame's width. A transform does not change the layout box, so the frame is given the
 * scaled height: the slot is exactly as tall as the unit looks.
 *
 * @param {HTMLElement} frame  slot-wide box that carries the scaled height
 * @param {HTMLElement} unit   natural-size box the network fills
 */
function fitUnit(frame, unit) {
    const fit = () => {
        const width = unit.offsetWidth;     // layout size: a transform does not change it
        const height = unit.offsetHeight;
        const available = frame.getBoundingClientRect().width;
        if (!width || !height || !available) return;   // unsized and unfilled, or not displayed
        const scale = Math.min(1, available / width);
        unit.style.transform = scale < 1 ? `scale(${scale})` : '';
        frame.style.height = `${height * scale}px`;
    };

    // Re-fit when the slot's width changes (rotation, window resize) or the unit's own size does
    // (a creative of another size). The write waits for the next frame: resizing an observed
    // element inside its own callback is a ResizeObserver loop.
    let queued = false;
    const observer = new ResizeObserver(() => {
        if (queued) return;
        queued = true;
        requestAnimationFrame(() => {
            queued = false;
            fit();
        });
    });
    observer.observe(frame);
    observer.observe(unit);
    fit();
}
