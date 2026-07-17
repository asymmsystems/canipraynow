/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Status block renderer (spec §5.3): body state class (the colored canvas IS
 * the theme), the Big Answer, reason line, notes, and per-second countdown.
 *
 * @author Fahim Farook
 */

import { t } from '../strings.js';
import { fmtCountdown, fmtTime } from '../utils.js';

const ANSWER = {
    PERMITTED: 'answer.YES',
    PROHIBITED: 'answer.NO',
    DISLIKED: 'answer.CAUTION',
    PERMITTED_WITH_WARNING: 'answer.YES_STAR'
};

const STATE_CLASS = {
    PERMITTED: 'state-permitted',
    PROHIBITED: 'state-prohibited',
    DISLIKED: 'state-disliked',
    PERMITTED_WITH_WARNING: 'state-warning'
};

const ALL_STATES = 'state-permitted state-prohibited state-disliked state-warning state-neutral state-degraded';

/**
 * Full re-render on verdict/window change.
 * @param {import('../engine/verdict.js').Verdict} verdict
 * @param {{prayerType:string, sunriseMinutes:number}} ctx
 */
export function renderStatus(verdict, ctx) {
    setState(STATE_CLASS[verdict.status]);
    el('answer').textContent = t(ANSWER[verdict.status]);

    // One readable sentence, not "Can I pray now? · entering the mosque".
    el('q').textContent = t('label.QUESTION_FOR', { what: t('prayerType.' + ctx.prayerType) });

    // Anchor the answer to the clock: which obligatory prayer's time is this?
    const nowPrayer = el('now-prayer');
    if (nowPrayer) {
        if (!ctx.period) {
            nowPrayer.textContent = t('now.NONE');
        } else if (!ctx.period.endsAt) {
            nowPrayer.textContent = t('prayer.' + ctx.period.prayer);
        } else {
            // Isha reports its PREFERRED end. Saying "until" there would imply
            // the prayer is lost afterwards; it is not - only its virtue is.
            const key = ctx.period.endKind === 'preferred' ? 'now.PRAYER_PREFERRED' : 'now.PRAYER';
            nowPrayer.textContent = t(key, {
                prayer: t('prayer.' + ctx.period.prayer),
                end: fmtTime(ctx.period.endsAt, ctx.tz, ctx.clock24)
            });
        }
    }

    const reasonEl = el('reason');
    if (verdict.grace) {
        reasonEl.textContent = t(verdict.grace.phase === 'starting' ? 'grace.STARTING' : 'grace.ENDING',
            { window: t('window.' + verdict.grace.window) });
        reasonEl.classList.add('grace-pulse');
    } else {
        reasonEl.textContent = t(verdict.reason, { minutes: ctx.sunriseMinutes });
        reasonEl.classList.remove('grace-pulse');
    }

    const notes = (verdict.notes || [])
        .filter((n) => !['TODAYS_ASR', 'FAJR_EXPIRED'].includes(n)) // already the reason line itself
        .map((n) => t('note.' + n));
    el('status-notes').textContent = notes.join(' ');
    el('status-notes').hidden = notes.length === 0;
}

/** Degraded high-latitude state (spec §3.7): honest amber, no invented window. */
export function renderDegraded() {
    setState('state-degraded');
    el('answer').textContent = '···';
    el('reason').textContent = t('highlat.DEGRADED');
    el('status-notes').hidden = true;
    el('countdown').hidden = true;
    el('now-prayer').textContent = '';   // no verified clock context to show
}

/** Neutral pre-location state (spec §12): never a default-city wrong answer. */
export function renderNeutral() {
    setState('state-neutral');
    el('answer').textContent = '···';
    el('reason').textContent = t('locate.PROMPT');
    el('status-notes').hidden = true;
    el('countdown').hidden = true;
    el('now-prayer').textContent = '';   // no verified clock context to show
}

/**
 * Per-second countdown tick (spec §5.3 line 4).
 * Red/amber states count to the window's end; green counts to the next boundary.
 */
export function renderCountdown(verdict, now, ctx) {
    const box = el('countdown');
    const restricted = verdict.status === 'PROHIBITED' || verdict.status === 'DISLIKED';

    if (restricted) {
        const target = verdict.permittedAt || verdict.windowEndsAt;
        if (!target) { box.hidden = true; return; }
        box.hidden = false;
        el('countdown-label').textContent = t('countdown.CAN_PRAY_IN');
        el('countdown-value').textContent = fmtCountdown(target.getTime() - now.getTime());
        return;
    }

    // Permitted: name what is coming, not "window changes". A bare countdown to
    // an unnamed event tells the reader nothing they can act on.
    const ev = ctx.nextEvent;
    if (!ev) { box.hidden = true; return; }
    box.hidden = false;
    const label = ev.kind === 'prayer'
        ? t('event.PRAYER_STARTS', { prayer: t('prayer.' + ev.name) })
        : t('event.WINDOW_STARTS', { window: t('window.' + ev.name) });
    el('countdown-label').textContent = t('countdown.NEXT_EVENT_IN', { event: label });
    el('countdown-value').textContent = fmtCountdown(ev.at.getTime() - now.getTime());
}

function setState(cls) {
    const b = document.body;
    b.classList.remove(...ALL_STATES.split(' '));
    b.classList.add(cls);
}

function el(id) {
    return document.getElementById(id);
}
