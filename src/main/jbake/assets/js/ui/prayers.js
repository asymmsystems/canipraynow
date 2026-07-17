/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Today's prayer list (spec §5.6): six rows, active row marked with the
 * current state's ring colour, next prayer shows relative time.
 *
 * @author Fahim Farook
 */

import { t } from '../strings.js';
import { fmtTime, fmtRelative, esc } from '../utils.js';

const ROWS = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

/**
 * @param {HTMLElement} host
 * @param {import('../engine/windows.js').DayEvents} events
 * @param {Date} now
 * @param {{tz:string, clock24:boolean}} ctx
 */
export function renderPrayers(host, events, now, ctx) {
    const nowT = now.getTime();
    let activeIdx = -1;
    for (let i = 0; i < ROWS.length; i++) {
        if (events[ROWS[i]].getTime() <= nowT) activeIdx = i;
    }
    const nextIdx = ROWS.findIndex((k) => events[k].getTime() > nowT);
    const rows = ROWS.map((key, i) => {
        const cls = ['prayer-row'];
        if (i === activeIdx) cls.push('prayer-active');
        const rel = i === nextIdx
            ? `<span class="prayer-rel">${t('sidebar.NEXT_IN')} ${esc(fmtRelative(events[key].getTime() - nowT))}</span>`
            : '';
        return `<li class="${cls.join(' ')}">
            <span class="prayer-name">${esc(t('prayer.' + key))}</span>
            ${rel}
            <span class="prayer-time">${esc(fmtTime(events[key], ctx.tz, ctx.clock24))}</span>
        </li>`;
    }).join('');
    host.innerHTML = `<h2 class="widget-title">${esc(t('sidebar.TODAY'))}</h2><ul class="prayer-list">${rows}</ul>`;
}
