/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Worker entry point: routes the two edge paths this Worker owns on canipraynow.com.
 * Everything else is served by the Pages project (this Worker only receives the routes
 * declared in wrangler.toml).
 *
 * @author Fahim Farook
 */

import { handleGo } from './go.js';
import { handleGeo } from './geo.js';

export default {
    async fetch(request, env) {
        const url = new URL(request.url);

        if (request.method === 'GET' && url.pathname.startsWith('/go/')) {
            return handleGo(request, env, url);
        }
        if (request.method === 'GET' && url.pathname === '/api/geo') {
            return handleGeo(request);
        }
        return new Response('Not found', { status: 404 });
    }
};
