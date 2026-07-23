/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * City index filter. A directory of 110 links needs a way in; this filters
 * client-side over what is already in the DOM, so the page stays a static
 * file and works with no network.
 *
 * @author Fahim Farook
 */

const input = document.getElementById('city-filter-input');
const empty = document.getElementById('city-filter-empty');
const groups = [...document.querySelectorAll('.country-group')];

if (input) {
    input.addEventListener('input', () => {
        const q = input.value.trim().toLowerCase()
            .normalize('NFD').replace(/[̀-ͯ]/g, '');
        let shown = 0;
        for (const group of groups) {
            let groupShown = 0;
            for (const li of group.querySelectorAll('li[data-city]')) {
                const hit = !q || li.dataset.city.includes(q);
                li.hidden = !hit;
                if (hit) groupShown++;
            }
            // A country heading with nothing under it is just noise.
            group.hidden = groupShown === 0;
            shown += groupShown;
        }
        if (empty) empty.hidden = shown > 0;
    });
}
