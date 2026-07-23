/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Prayer-type selector chips (spec §5.4). Session-persisted only; resets to
 * Voluntary daily by virtue of sessionStorage scope.
 *
 * @author Fahim Farook
 */

import { PRAYER_TYPES } from '../engine/verdict.js';
import { t } from '../strings.js';

// Obligatory first: it is the weightiest thing a user could be asking about,
// even though the DEFAULT selection stays Voluntary (spec §5.4) because that
// is the question people actually arrive with.
const ORDER = ['fardCurrent', 'pureNafl', 'qada', 'janazah', 'causalNafl'];

/**
 * @param {HTMLElement} host
 * @param {string} active
 * @param {(type:string) => void} onChange
 */
export function renderChips(host, active, onChange) {
    host.innerHTML = '';
    host.setAttribute('role', 'radiogroup');
    host.setAttribute('aria-label', t('label.QUESTION'));
    for (const type of ORDER) {
        if (!PRAYER_TYPES.includes(type)) continue;
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'chip' + (type === active ? ' chip-active' : '');
        btn.setAttribute('role', 'radio');
        btn.setAttribute('aria-checked', String(type === active));
        btn.dataset.type = type;
        btn.textContent = t('chip.' + type);
        btn.addEventListener('click', () => {
            if (btn.classList.contains('chip-active')) return;
            for (const b of host.children) {
                b.classList.toggle('chip-active', b === btn);
                b.setAttribute('aria-checked', String(b === btn));
            }
            onChange(type);
        });
        host.appendChild(btn);
    }
}
