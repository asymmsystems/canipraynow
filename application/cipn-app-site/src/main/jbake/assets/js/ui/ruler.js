/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * The Sun Ruler - signature component (spec §5.5). Pure SVG-string renderer;
 * re-rendered only on state change, the now-marker moves per second via
 * updateCursor() without a re-render.
 *
 * Layout decisions worth keeping:
 *   - Labels collide near sunrise and sunset, where four events fall within
 *     minutes. Majors (prayer names) sit near the track with a full tick;
 *     minors (window edges) sit on a second row above with a short stub,
 *     because the coloured zone edge already marks their exact instant.
 *   - The now-marker is a tall hairline with an arrowhead, not a fat dot: a
 *     dot covers the very zone the user is asking about. It is
 *     pointer-events:none so it cannot steal hover from the track beneath.
 *   - Every point of the track answers "which prayer's time is this?" on
 *     hover, not only the restricted zones. Pointer users get the tooltip;
 *     screen-reader users get the same content as a visually-hidden list
 *     (the hit rects are aria-hidden, so there are no empty tab stops).
 *
 * @author Fahim Farook
 */

import { WINDOW } from '../engine/windows.js';
import { fmtTime, esc } from '../utils.js';
import { t } from '../strings.js';

const X0 = 40;   // track left (night stub boundary)
const X1 = 960;  // track right
const Y = 78;    // track top
const H = 14;    // track height

const ROW_MAJOR_Y = Y - 12;   // prayer names, hard against the track
const ROW_MINOR_Y = Y - 38;   // window edges, one row up
const MIN_GAP = 44;           // px (viewBox units) between two labels in a row

const STRICT = [WINDOW.SUNRISE, WINDOW.ZENITH, WINDOW.SUNSET];

/** The fard periods a moment can belong to, in order (hover: item 9). */
const PERIODS = [
    { key: 'fajr', from: 'fajr', to: 'sunrise' },
    { key: 'sunrise', from: 'sunrise', to: 'dhuhr' },   // no fard time runs here
    { key: 'dhuhr', from: 'dhuhr', to: 'asr' },
    { key: 'asr', from: 'asr', to: 'maghrib' },
    { key: 'maghrib', from: 'maghrib', to: 'isha' },
    { key: 'isha', from: 'isha', to: null }
];

/**
 * @param {import('../engine/windows.js').DayEvents} events
 * @param {import('../engine/windows.js').Win[]} windows
 * @param {Object} ruleSet   active madhhab rule set (drives amber fills + rulings)
 * @param {Date} now
 * @param {{prayerType:string, tz:string, clock24:boolean, sunriseMinutes:number, summary:string}} ctx
 * @returns {string} SVG markup
 */
export function renderRuler(events, windows, ruleSet, now, ctx) {
    const x = scaler(events);
    const rect = (a, b, fill, opacity, cls = '') =>
        `<rect x="${x(a).toFixed(1)}" y="${Y}" width="${Math.max(1, x(b) - x(a)).toFixed(1)}" height="${H}" rx="2" fill="${fill}" opacity="${opacity}"${cls ? ` class="${cls}"` : ''}/>`;

    // Three colours, by design (per the timeline spec):
    //   red  = a prohibited time - the three strict solar windows, ALWAYS red,
    //          plus an extended window when it is restricted for this prayer type
    //   X    = every other (permitted) slot - the muted base track
    //   Y    = the slot the cursor is in right now - drawn in the verdict colour
    //          (--state-ring) so it matches the now-prayer pill by construction
    const zones = windows
        .filter((w) => w.end > w.start)   // degenerate windows at extreme latitudes
        .map((w) => {
            const restricted = ruleSet.matrix[w.id][ctx.prayerType].status !== 'PERMITTED';
            const strict = STRICT.includes(w.id);
            if (!strict && !restricted) return '';           // permitted -> base track (X) shows
            const opacity = strict ? 0.9 : 0.5;              // extended-restricted reads softer
            return rect(w.start, w.end, 'var(--ring-red)', opacity);
        })
        .filter(Boolean)
        .join('\n  ');

    // Highlight the current slot in colour Y. The slot is the stretch between
    // the two boundaries the cursor sits between; drawn last so it reads on top.
    const cur = currentSlot(now, events, windows);
    const currentZone = cur
        ? rect(cur.start, cur.end, 'var(--state-ring)', 0.9, 'ruler-current')
        : '';

    const ticks = buildTicks(events, windows, ctx).map((tk) => {
        const tx = x(tk.at).toFixed(1);
        if (tk.minor) {
            // Stub only: the coloured zone edge below already marks the instant,
            // and a full-length tick here would strike through the row beneath.
            const line = `<line x1="${tx}" y1="${ROW_MINOR_Y + 4}" x2="${tx}" y2="${ROW_MINOR_Y + 12}" class="ruler-tick ruler-tick-minor"/>`;
            if (tk.hideText) return line;
            // The minor labels are the terms nobody knows - on hover they carry
            // their exact clock time (like the majors) plus a one-line explanation
            // (Ishrāq, Zenith, Isfirār).
            const tip = tk.tipKey
                ? ` data-tip="${esc(t(tk.tipKey, { time: fmtTime(tk.at, ctx.tz, ctx.clock24) }))}" tabindex="0"`
                : '';
            const cls = tk.tipKey ? 'ruler-label ruler-label-minor ruler-label-tip' : 'ruler-label ruler-label-minor';
            return `${line}<text x="${tx}" y="${ROW_MINOR_Y}" text-anchor="middle" class="${cls}"${tip}>${esc(tk.label)}</text>`;
        }
        // Majors carry their exact clock time on hover - the label alone tells
        // you a prayer is there, not when it is.
        const line = `<line x1="${tx}" y1="${ROW_MAJOR_Y + 3}" x2="${tx}" y2="${Y + H}" class="ruler-tick"/>`;
        if (tk.hideText) return line;
        const tip = esc(tk.tipKey === 'tip.SUNRISE_AT'
            ? t('tip.SUNRISE_AT', { time: fmtTime(tk.at, ctx.tz, ctx.clock24) })
            : t('tip.PRAYER_AT', { prayer: tk.label, time: fmtTime(tk.at, ctx.tz, ctx.clock24) }));
        return `${line}<text x="${tx}" y="${ROW_MAJOR_Y}" text-anchor="middle" class="ruler-label ruler-label-tip" data-tip="${tip}" tabindex="0">${esc(tk.label)}</text>`;
    }).join('\n  ');

    // Hover layer: one transparent rect per (prayer period x window) segment,
    // so every point of the track names its prayer time AND its ruling.
    // aria-hidden: the same content is exposed as a list by rulerA11yList().
    const hits = buildSegments(events, windows, ruleSet, ctx)
        .map((s) => `<rect x="${x(s.start).toFixed(1)}" y="${Y - 10}" width="${Math.max(1, x(s.end) - x(s.start)).toFixed(1)}" height="${H + 20}" class="ruler-hit" aria-hidden="true" data-tip="${esc(s.tip)}"/>`)
        .join('\n  ');

    const cx = cursorX(events, now);

    return `<svg viewBox="0 0 1000 150" xmlns="http://www.w3.org/2000/svg" class="sun-ruler" preserveAspectRatio="xMidYMid meet" role="img" aria-label="${esc(ctx.summary || '')}">
  <rect x="0" y="${Y}" width="${X0}" height="${H}" rx="4" class="ruler-night"/>
  <rect x="${X1}" y="${Y}" width="${1000 - X1}" height="${H}" rx="4" class="ruler-night"/>
  <text x="${X0 / 2}" y="${Y + H + 16}" text-anchor="middle" class="ruler-label ruler-label-night">${esc(t('label.NIGHT'))}</text>
  <text x="${(1000 + X1) / 2}" y="${Y + H + 16}" text-anchor="middle" class="ruler-label ruler-label-night">${esc(t('label.NIGHT'))}</text>
  <rect x="${X0}" y="${Y}" width="${X1 - X0}" height="${H}" rx="4" fill="var(--ruler-other)" opacity="0.7"/>
  ${zones}
  ${currentZone}
  ${ticks}
  ${hits}
  <g id="ruler-cursor" transform="translate(${cx.toFixed(1)},0)">
    <line x1="0" y1="${Y - 26}" x2="0" y2="${Y + H + 20}" class="cursor-line"/>
    <path d="M0 ${Y - 3} l-5 -9 h10 z" class="cursor-arrow"/>
    <text x="0" y="${Y + H + 42}" text-anchor="middle" class="ruler-now" id="ruler-now-label">${esc(fmtTime(now, ctx.tz, ctx.clock24))}</text>
  </g>
</svg>`;
}

/**
 * The hover content, as a list for assistive tech. Rendered visually hidden
 * next to the SVG so screen-reader users get the segment detail that pointer
 * users get from the tooltip - without phantom tab stops on the SVG rects.
 */
export function rulerA11yList(events, windows, ruleSet, ctx) {
    const items = buildSegments(events, windows, ruleSet, ctx)
        .map((s) => `<li>${esc(s.tip)}</li>`)
        .join('');
    return `<ul class="visually-hidden">${items}</ul>`;
}

/**
 * The slot the cursor is in: the interval between the two nearest boundaries
 * (window edges + prayer entries) that straddle `now`. Clamped to the Fajr-Isha
 * track so the highlight never spills into the night stubs.
 * @returns {{start:Date, end:Date}|null}
 */
function currentSlot(now, events, windows) {
    const t = now.getTime();
    if (t < events.fajr.getTime() || t >= events.isha.getTime()) return null;
    const cuts = new Set([events.fajr, events.sunrise, events.dhuhr, events.asr, events.maghrib, events.isha].map((d) => d.getTime()));
    for (const w of windows) {
        if (w.end > w.start) { cuts.add(w.start.getTime()); cuts.add(w.end.getTime()); }
    }
    const sorted = [...cuts].sort((a, b) => a - b);
    let start = events.fajr.getTime();
    let end = events.isha.getTime();
    for (const c of sorted) {
        if (c <= t) start = c;
        if (c > t) { end = c; break; }
    }
    return { start: new Date(start), end: new Date(end) };
}

/** Cursor x for `now`; clamps into the night stubs outside Fajr-Isha (spec §5.5). */
export function cursorX(events, now) {
    const raw = scaler(events)(now);
    return Math.max(X0 / 2, Math.min(1000 - X0 / 2, raw));
}

/** Per-second cheap update: move the cursor group, refresh its time label. */
export function updateCursor(svgRoot, events, now, ctx) {
    const g = svgRoot.querySelector('#ruler-cursor');
    if (!g) return;
    g.setAttribute('transform', `translate(${cursorX(events, now).toFixed(1)},0)`);
    const label = svgRoot.querySelector('#ruler-now-label');
    if (label) label.textContent = fmtTime(now, ctx.tz, ctx.clock24);
}

/**
 * Bind the hover tooltip. Called after every re-render; the host-level
 * listener is attached once (the host survives innerHTML replacement, so
 * re-adding it each time would leak a listener per render).
 * @param {HTMLElement} host  the <figure> wrapping the SVG
 */
export function bindRulerTooltip(host) {
    let tip = host.querySelector('.ruler-tip');
    if (!tip) {
        tip = document.createElement('div');
        tip.className = 'ruler-tip';
        tip.setAttribute('role', 'tooltip');
        tip.hidden = true;
        host.appendChild(tip);
    }

    // Track segments, plus the minor labels that explain the unfamiliar terms.
    for (const hit of host.querySelectorAll('.ruler-hit, .ruler-label-tip')) {
        hit.addEventListener('pointerenter', () => showTip(host, hit));
        hit.addEventListener('pointerleave', () => hideTip(host));
        hit.addEventListener('focus', () => showTip(host, hit));
        hit.addEventListener('blur', () => hideTip(host));
    }

    if (!host.dataset.tipBound) {
        host.addEventListener('pointerleave', () => hideTip(host));
        host.dataset.tipBound = '1';
    }

    // A re-render under a resting pointer leaves the new rects unhovered
    // (no pointer movement fires pointerenter), so re-assert from :hover.
    const hovered = host.querySelector('.ruler-hit:hover, .ruler-label-tip:hover');
    if (hovered) showTip(host, hovered);
    else hideTip(host);
}

function showTip(host, target) {
    const tip = host.querySelector('.ruler-tip');
    const text = target.getAttribute('data-tip');
    if (!tip || !text) return;
    tip.textContent = text;
    tip.hidden = false;

    // Clamp against the tip's own measured width, not just the anchor point,
    // or a long tip on an edge segment hangs off the page.
    const hostBox = host.getBoundingClientRect();
    const box = target.getBoundingClientRect();
    const half = tip.offsetWidth / 2;
    const cx = box.left + box.width / 2 - hostBox.left;
    const min = half + 8;
    const max = Math.max(min, hostBox.width - half - 8);
    tip.style.left = `${Math.max(min, Math.min(max, cx))}px`;
}

function hideTip(host) {
    const tip = host.querySelector('.ruler-tip');
    if (tip) tip.hidden = true;
}

/** Textual summary for the SVG's aria-label (spec §6 accessibility). */
export function rulerSummary(windows, verdict, now, ctx) {
    const spans = windows
        .filter((w) => STRICT.includes(w.id) && w.end > w.start)
        .map((w) => `${fmtTime(w.start, ctx.tz, ctx.clock24)}-${fmtTime(w.end, ctx.tz, ctx.clock24)}`)
        .join(', ');
    return `Prohibited ${spans}; now ${fmtTime(now, ctx.tz, ctx.clock24)}, ${statusLine(verdict.status)}`;
}

function scaler(events) {
    const span = events.isha.getTime() - events.fajr.getTime();
    return (d) => X0 + (X1 - X0) * (d.getTime() - events.fajr.getTime()) / span;
}

function statusLine(status) {
    return {
        PERMITTED: 'permitted',
        DISLIKED: 'disliked',
        PROHIBITED: 'prohibited',
        PERMITTED_WITH_WARNING: 'permitted with warning'
    }[status] || status;
}

/**
 * Split the track at every boundary (prayer entries + window edges) and label
 * each piece with the fard period it sits in and the ruling that applies there
 * for the selected prayer type.
 */
function buildSegments(events, windows, ruleSet, ctx) {
    const marks = new Set([events.fajr, events.sunrise, events.dhuhr, events.asr, events.maghrib, events.isha]
        .map((d) => d.getTime()));
    for (const w of windows) {
        marks.add(w.start.getTime());
        marks.add(w.end.getTime());
    }
    const cuts = [...marks]
        .filter((ms) => ms >= events.fajr.getTime() && ms <= events.isha.getTime())
        .sort((a, b) => a - b);

    const segs = [];
    for (let i = 0; i < cuts.length - 1; i++) {
        const start = new Date(cuts[i]);
        const end = new Date(cuts[i + 1]);
        if (end.getTime() - start.getTime() < 1000) continue;
        const mid = new Date((cuts[i] + cuts[i + 1]) / 2);

        const win = windows.find((w) => w.end > w.start && mid >= w.start && mid < w.end);
        const period = PERIODS.find((p) => {
            const to = p.to ? events[p.to] : null;
            return mid >= events[p.from] && (!to || mid < to);
        });

        const when = `${fmtTime(start, ctx.tz, ctx.clock24)}-${fmtTime(end, ctx.tz, ctx.clock24)}`;
        // Between sunrise and Dhuhr no obligatory prayer's time is running.
        const periodLabel = !period || period.key === 'sunrise'
            ? t('period.NONE')
            : t('period.RUNNING', { prayer: t('prayer.' + period.key) });
        const windowLabel = t('window.' + (win ? win.id : WINDOW.CLEAR));
        const status = win ? ruleSet.matrix[win.id][ctx.prayerType].status : 'PERMITTED';
        // Name WHAT is being ruled on. "Sunrise: permitted" reads as though the
        // sunrise window itself were permitted; the ruling is always about the
        // selected prayer type, so say so.
        const ruling = `${t('prayerType.' + ctx.prayerType)}: ${t('ruling.' + status)}`;
        segs.push({ start, end, tip: `${when} · ${windowLabel} · ${periodLabel} · ${ruling}` });
    }
    return segs;
}

/**
 * Event ticks across two rows. Majors (prayer names) hug the track; minors
 * (window edges) sit above. Collisions are only possible within a row - the
 * rows never share vertical space - so a crowded minor keeps its stub and
 * drops its text rather than overprinting its neighbour.
 */
function buildTicks(events, windows, ctx) {
    const w2 = windows.find((w) => w.id === WINDOW.SUNRISE);
    const w5 = windows.find((w) => w.id === WINDOW.SUNSET);
    // "+20m" named nothing. The instant the sunrise prohibition lifts is
    // Ishrāq - the sun risen a spear's length - so the label says that.
    const ticks = [
        { at: events.fajr, label: t('prayer.fajr'), minor: false },
        { at: events.sunrise, label: t('prayer.sunrise'), minor: false, tipKey: 'tip.SUNRISE_AT' },
        { at: w2.end, label: t('label.ISHRAQ'), minor: true, tipKey: 'tip.ISHRAQ' },
        { at: events.solarNoon, label: t('label.ZENITH'), minor: true, tipKey: 'tip.ZENITH' },
        { at: events.dhuhr, label: t('prayer.dhuhr'), minor: false },
        { at: events.asr, label: t('prayer.asr'), minor: false },
        { at: w5.start, label: t('label.ISFIRAR'), minor: true, tipKey: 'tip.ISFIRAR' },
        { at: events.maghrib, label: t('prayer.maghrib'), minor: false },
        { at: events.isha, label: t('prayer.isha'), minor: false }
    ].sort((a, b) => a.at - b.at);

    const px = scaler(events);
    for (const minor of [false, true]) {
        let lastShown = -Infinity;
        for (const tk of ticks.filter((k) => k.minor === minor)) {
            const p = px(tk.at);
            if (p - lastShown < MIN_GAP) {
                tk.hideText = true;   // keep the stub/tick, drop the text
            } else {
                tk.hideText = false;
                lastShown = p;
            }
        }
    }
    return ticks;
}
