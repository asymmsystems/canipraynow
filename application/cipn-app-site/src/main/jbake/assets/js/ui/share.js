/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * Share control: one quiet pill under the prayer list. A click hands the message to the
 * device's own share sheet (navigator.share), which is how the times reach a WhatsApp group.
 * Where the browser has no share sheet, the same click opens two inline choices instead:
 * WhatsApp, and Copy link.
 *
 * The message is the prayer list as it is on screen: the same rows, names and formatted times
 * as ui/prayers.js, read from live state at the moment of the click. Nothing is recomputed.
 *
 * Privacy (ADR-0001): no third-party script, and nothing is loaded or sent before the click.
 * WhatsApp is reached by opening its public wa.me link, from that click only.
 *
 * @author Fahim Farook
 */

import { t } from '../strings.js';
import { fmtDate, fmtTime, esc } from '../utils.js';
import { ROWS } from './prayers.js';

/** WhatsApp's public click-to-chat link: opens the app, or WhatsApp Web, with the text filled in. */
const WHATSAPP_URL = 'https://wa.me/?text=';

const ICON = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
    <path d="M12 15V3"/>
    <path d="M8 7l4-4 4 4"/>
    <path d="M5 12v7a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-7"/>
</svg>`;

/**
 * @typedef {Object} ShareContent
 * @property {string} title  one-line heading; share targets with a subject line use it
 * @property {string} text   the complete message, ending with the page URL
 * @property {string} url    the page URL on its own, for "Copy link"
 */

/**
 * Today's prayers as a message: the city, the date, the source line when the times are an
 * authority's own published ones, the six rows exactly as the list shows them, then the page URL.
 *
 * @param {import('../engine/windows.js').DayEvents} events
 * @param {Date} now
 * @param {{city:string, tz:string, clock24:boolean, timesSource:Object|null}} ctx
 * @param {string} url
 * @returns {ShareContent}
 */
export function timesShare(events, now, ctx, url) {
    const title = t('share.HEADING', { city: ctx.city });
    const head = [title, fmtDate(now, ctx.tz)];
    if (ctx.timesSource) {
        head.push(`${t('sidebar.SOURCE', { authority: ctx.timesSource.source })} ${ctx.timesSource.zone}`);
    }
    const rows = ROWS.map((key) => `${t('prayer.' + key)} ${fmtTime(events[key], ctx.tz, ctx.clock24)}`);
    return { title, text: [head.join('\n'), rows.join('\n'), url].join('\n\n'), url };
}

/**
 * A page with no city times to share: its title and its URL.
 *
 * @param {string} title
 * @param {string} url
 * @returns {ShareContent}
 */
export function pageShare(title, url) {
    return { title, text: `${title}\n${url}`, url };
}

/**
 * Render the control into its host and wire it.
 *
 * @param {HTMLElement} host
 * @param {Object} opts
 * @param {() => ShareContent} opts.content  what to share, read at the moment of the click
 * @param {(msg:string) => void} opts.notify  shows the one-line result of "Copy link"
 */
export function initShare(host, opts) {
    if (!host) return;
    host.innerHTML = `
        <button type="button" class="chip share-btn">${ICON}<span>${esc(t('share.BUTTON'))}</span></button>
        <div class="share-options" id="share-options" role="group" aria-label="${esc(t('share.BUTTON'))}" hidden>
            <button type="button" class="chip" data-share="whatsapp">${esc(t('share.WHATSAPP'))}</button>
            <button type="button" class="chip" data-share="copy">${esc(t('share.COPY'))}</button>
        </div>`;

    const btn = host.querySelector('.share-btn');
    const options = host.querySelector('.share-options');
    const hasSheet = typeof navigator.share === 'function';

    const show = (open) => {
        options.hidden = !open;
        btn.setAttribute('aria-expanded', String(open));
    };
    const done = () => {
        show(false);
        btn.focus();
    };

    // Without a share sheet the pill is a disclosure for the two choices, and says so from the
    // start. With one it is a plain button, unless the sheet fails and the choices open instead.
    if (!hasSheet) {
        btn.setAttribute('aria-controls', options.id);
        show(false);
    }

    btn.addEventListener('click', async () => {
        if (!options.hidden) {
            show(false);
            return;
        }
        if (hasSheet) {
            const { title, text } = opts.content();
            try {
                // The URL travels inside the text, not as a separate url field, so every share
                // target receives the one whole message however it combines the fields.
                await navigator.share({ title, text });
                return;
            } catch (e) {
                if (e && e.name === 'AbortError') return;   // the sheet was dismissed: not a failure
            }
        }
        show(true);
    });

    host.querySelector('[data-share="whatsapp"]').addEventListener('click', () => {
        window.open(WHATSAPP_URL + encodeURIComponent(opts.content().text), '_blank', 'noopener,noreferrer');
        done();
    });

    host.querySelector('[data-share="copy"]').addEventListener('click', async () => {
        done();
        try {
            await navigator.clipboard.writeText(opts.content().url);
            opts.notify(t('share.COPIED'));
        } catch (e) {
            opts.notify(t('share.COPY_FAILED'));
        }
    });

    host.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && !options.hidden) done();
    });
    // A press anywhere else closes the choices, as it does for the city search.
    document.addEventListener('pointerdown', (e) => {
        if (!options.hidden && !host.contains(e.target)) show(false);
    });
}
