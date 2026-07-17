# canipraynow.com

Single-purpose, mobile-first web utility answering one question for Muslims: **"Is it permissible for me to pray right now?"** - the inverse of a prayer-times app. The answer is conditioned on what you want to pray and which madhhab you follow, computed astronomically for your location, live, offline-capable.

Built and operated by Asymm Systems (Pvt) Ltd. Product specification: `canipraynow/product/product-specification-initial.md` (the source of truth for the fiqh rules and UX).

## Status

**PREVIEW.** The rule matrix is a draft pending named-scholar verification. The app renders a permanent PREVIEW banner until the rule sets carry `verified_at` (enforced in code; see `task/scholar-verification.md`).

## Stack

Same pipeline as asymm-corporate-code-pfw - deliberately npm-free:

- **JBake 2.7 + FreeMarker** bake all pages (Maven plugin)
- **Dart Sass** compiles `src/main/scss/style.scss`
- **CSS minify + HTML compress** in `process-resources`
- **Client app**: hand-written vanilla ES modules with JSDoc types (`src/main/jbake/assets/js/`), no bundler, no framework. The only runtime dependency is [Adhan.js](https://github.com/batoulapps/adhan-js) 4.4.4, vendored as a single ESM file (`assets/js/vendor/adhan.js`)
- **Cloudflare Pages** hosting; `functions/` holds the only server code (`/api/geo` location fallback, `/go/{id}` sponsor click counter)

## Build

```sh
./mvnw clean process-resources                 # dev build -> target/site (browsable, .html links)
./mvnw -Pproduction clean process-resources    # CI build: also strips .html for clean URLs
```

## Serve locally

Live-reload dev server (same as asymm-corporate):

```sh
./mvnw jbake:inline -Djbake.port=8020
```

Then open <http://localhost:8020>. Source files keep `.html` in hrefs so the dev server resolves
them as-is; the production profile strips them for Cloudflare Pages, which serves `/page` ->
`page.html` natively.

Note the app's ES modules must be served over HTTP, not opened as `file://` - `import` and the
`fetch` calls for rule sets and strings are blocked by the browser on the file protocol.

## Layout

```
src/main/jbake/
├─ content/            # index (app), methodology, privacy, 404, q/*, {country}/{city}
├─ templates/          # page/app|page|question|city.ftl, partial/header|app.ftl, sitemap
└─ assets/
   ├─ js/engine/       # pure fiqh engine: windows.js, verdict.js (zero DOM)
   ├─ js/ui/           # status, chips, ruler (the Sun Ruler), prayers, settings, sponsor
   ├─ js/              # main.js boot+loop, astronomy.js (adhan wrapper), location, storage, strings, utils
   ├─ data/            # rule_sets/{hanafi,shafii}.json (versioned rules DATA), cities.json, method-defaults, sponsors
   └─ sw.js, manifest.webmanifest, fonts/, images/
functions/             # Cloudflare Pages Functions (api/geo.js, go/[id].js)
scripts/               # clean-urls.sh (production), generate-city-pages.py
```

## Fiqh safety invariants (spec §3.6)

1. "Today's fard" while its time runs never renders a bare NO - enforced defensively in `engine/verdict.js` even against rule-data errors.
2. The zenith window derives from astronomical solar noon only (`astronomy.js`), never a timetable.
3. Every verdict carries its rule-set version; the footer renders it.
4. `verified_at == null` forces the PREVIEW banner.
5. Restricted countdowns point at the instant the selected prayer type actually becomes permitted (`permittedAt`), not merely the current window's edge.

## City pages

`scripts/generate-city-pages.py` regenerates `content/{country}/{city}.html` from `assets/data/cities.json` entries with `page: true` (currently 110 of 301 cities). Output is committed; the build needs no Python.

## Deploy

GitHub Actions (same flow as asymm-corporate): PRs to develop/main run `validate.yml` (Maven build + node syntax gate); pushes to develop deploy a Cloudflare Pages preview; tags on main deploy production. Secrets: `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`. The `/go/` counter expects a KV namespace bound as `SPONSOR_CLICKS` on the Pages project (optional - redirects work without it).

## Deferred work

See `canipraynow/task/`: full-test-suite.md, city-dataset-expansion.md, asset-polish.md, scholar-verification.md.
