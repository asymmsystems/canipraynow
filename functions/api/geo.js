/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * /api/geo (spec §7): privacy-preserving location fallback. Returns only what
 * Cloudflare already derives from the connection (request.cf). No IP is
 * stored, no third-party geo API is called, nothing is logged.
 *
 * @author Fahim Farook
 */

export async function onRequestGet({ request }) {
    const cf = request.cf || {};
    const lat = parseFloat(cf.latitude);
    const lng = parseFloat(cf.longitude);
    if (!isFinite(lat) || !isFinite(lng)) {
        return json({ error: 'unavailable' }, 404);
    }
    return json({
        city: cf.city || null,
        latitude: lat,
        longitude: lng,
        timezone: cf.timezone || null,
        country: cf.country || null
    });
}

function json(body, status = 200) {
    return new Response(JSON.stringify(body), {
        status,
        headers: {
            'content-type': 'application/json; charset=utf-8',
            'cache-control': 'no-store'
        }
    });
}
