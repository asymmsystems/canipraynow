/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * /go/{id} (spec §9): sponsor click-through. Increments a Cloudflare KV
 * counter (binding: SPONSOR_CLICKS) and 302-redirects to the sponsor URL
 * from data/sponsors.json. Privacy-preserving: no cookies, no user IDs,
 * only an aggregate count.
 *
 * @author Fahim Farook
 */

export async function onRequestGet({ params, env, request }) {
    const id = params.id;
    if (!/^[a-z0-9-]{1,64}$/.test(id)) {
        return new Response('Not found', { status: 404 });
    }

    // Resolve the target from the same sponsors.json the client renders,
    // so there is a single source of truth for sponsor inventory.
    const origin = new URL(request.url).origin;
    let target = null;
    try {
        const res = await fetch(`${origin}/data/sponsors.json`);
        if (res.ok) {
            const data = await res.json();
            const slot = (data.slots || []).find((s) => s.id === id);
            if (slot && data.enabled === true) target = slot.url;
        }
    } catch (e) { /* fall through to 404 */ }

    if (!target) return new Response('Not found', { status: 404 });

    if (env.SPONSOR_CLICKS) {
        try {
            const key = `clicks:${id}`;
            const current = parseInt(await env.SPONSOR_CLICKS.get(key), 10) || 0;
            await env.SPONSOR_CLICKS.put(key, String(current + 1));
        } catch (e) { /* counting must never block the redirect */ }
    }

    return Response.redirect(target, 302);
}
