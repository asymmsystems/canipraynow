# Annual authority-table refresh (S1) runbook

How to re-scrape a national authority's prayer timetable and commit the new year (T-05). S1 is a
native-Java, **off-build** data tool (ADR-0017): it hits third-party sites, so it is never part of
the Maven build. Run it by hand, review the golden-diff, commit the XML.

## When

Once a year per scraped authority, after the authority publishes the new year's table. Note some
feeds only serve the **current** published year (JAKIM's does), so a future year cannot be fetched
until the authority releases it.

## Prerequisites

- Eliya JDK: `export JAVA_HOME=/opt/eliya`.
- Network access to the authority's site.
- Work on a branch off `develop`.

## Run

```sh
make scrape CC=<cc> YEAR=<year>
```

- `CC` = ISO-3166 alpha-2, lower-case (e.g. `my`).
- `YEAR` = the published year to onboard. For JAKIM, e-Solat's `period=year` returns only the
  **current** published year, so `YEAR` must be that year; a not-yet-published year returns an
  empty table and a clear "no days" failure.

## What it does

1. Resolves the authority scraper (`ScraperWiring.authorityScraper(cc)`).
2. Reads the stable zone register from the latest committed year's `zone.xml`.
3. Per zone: fetch live -> parse -> **enrich solar-noon** (authority-first, else computed) -> serialize
   -> **XSD-validate** -> write `timetable/<year>/<cc>/zone-<id>.xml`.
4. Carries `zone.xml` (geography) into the new year; merges the country into `index.xml`.
5. **Golden-diffs** each zone against the last committed table and prints a summary.

## Reading the golden-diff

- **0 min** across a zone = the live feed matches what we hold (the ideal for re-scraping a year we
  already have; a strong trust signal).
- **1-2 min on prayer times** = the authority refreshed its source since we captured it. Review;
  usually fine to accept.
- **solar-noon is always 0** (the enricher reproduces the committed values). Non-zero solar-noon
  drift = an enrichment regression - investigate, do not commit.
- **Large/systematic drift, or missing days** = a moved endpoint, wrong zone, or parser regression.
  Do **not** commit; investigate first.

## Verify before committing

- Eyeball a written `zone-<id>.xml`: sane times, 365 (or 366) `<day>` rows.
- `make build` - the new XML is XSD-validated on write and the accessor tests read it back.

## Commit

Commit the written tree (`timetable/<year>/<cc>/zone-*.xml`, `zone.xml`, `../index.xml`). Describe
the final state and the diff summary; do not enumerate the debugging.

## Wired authorities (11 served)

| CC | Authority | Feed | Zones | Parser note |
|---|---|---|---|---|
| MY | JAKIM (e-Solat) | api (JSON) | ~60 | Malay dates, `period=year` |
| CY | KKTC Diyanet | html (GET) | 1 | Turkish dates, single Lefkosa takvim |
| BG | Chief Muftiate | html (POST) | towns | Cyrillic columns, month+town POST |
| BA | Rijaset (vaktija.ba) | api (JSON) | 3 | `vaktija/v1/{id}/{year}/{MM}`, retry on 503 |
| LK | ACJU | (pre-existing) | zones | already committed |
| SG | MUIS | pdf | 1 | row-major 12h, space-separated `H MM` |
| XK | BIK Takvim | pdf | 1 | 24h row-major, fajr=Imsaku, `.`/`:` minute sep |
| HU | MME (iszlam.com) | html | 1 | 12 monthly tables, month from heading (not order) |
| UA | islam.ua | pdf | 1 | column-major, coordinate (x,y) clustering |
| KZ | DUMK (muftyat.kz) | api (JSON) | 3 | per-settlement coords, `Double.toString` round-trip |
| PT | CIL (Lisbon) | pdf | 1 | first-8 times, drop Zawal + 2nd isha column |

## Known authority gotchas

- **JAKIM (MY), e-Solat:** `period=year` serves the current published year (there is no year
  parameter); `period=duration` for a full year returns **HTTP 500**. Dates are **Malay**
  (`01-Mac-2026`, `01-Ogos-2026`), parsed by first-three-letters. ~60 zones.
- **KKTC (CY), `namazvakti.php`:** one year-round HTML takvim for a single location (Lefkoşa) - one
  zone. Dates are **Turkish** (`01 Ocak 2026`); columns İmsak/Güneş/Öğle/İkindi/Akşam/Yatsı map to
  fajr/sunrise/dhuhr/asr/maghrib/isha. Cyprus observes **DST** and Diyanet adds a dhuhr temkin, both
  handled automatically by the sunrise/sunset-midpoint solar-noon enrichment. For a brand-new country
  first hand-write its `zone.xml` (geography), then scrape.
- **PDF parsers:** row-major tables (SG, XK, PT) parse from `PDFTextStripper` text with a regex;
  column-major tables (UA) need the coordinate (x,y) clustering in `UkrainePdfParser` because the
  text linearises out of order. Reuse the column-cluster approach for any new column-major PDF.
- **Multi-zone from one API (KZ):** each zone's centroid must be the API's exact registered
  coordinate; the scraper builds the URL from `zone.lat()/lng()` via `Double.toString`.

## Deferred authorities (feed exists, needs a decision or a manual step)

Do NOT ship these from a guessed mapping - each needs the noted input first.

- **AL (KMSH):** annual PDF with >6 columns (Agimi/Imsaku dawn pair, Yleja/Pasditja/Iqindia afternoon
  set) split half-month-per-page over 32 pages. Needs a scholar decision on which dawn = fajr and
  which afternoon = asr before parsing. See `task/scholar-verification.md`.
- **BW (BMA):** full-year HTML but ~34 locations and dual-asr columns (Shafi/Hanafi). Needs an
  asr-madhhab choice and a decision on which locations to onboard as zones/cities.
- **KR (KMF):** annual PDF, but January is image-only and July never produced a clean read - the
  source itself is incomplete. Needs OCR for January.
- **FI (Rabita):** clean 6-column monthly HTML table (single fajr, single asr, dates in each row),
  but the month selector is a Laravel CSRF-gated multipart POST - each month needs a GET to harvest
  the `_token` + session cookie, then a multipart POST (`year`, `month`, `_token`). Needs cookie/CSRF
  support in the reader; the authority also discloses no fajr/isha angle (high-latitude caveat).
- **ME (vaktija.me):** the annual PDF is behind client-side JS; needs a browser to obtain the file.
- **SI (Islamska skupnost):** only a Ramadan vaktija PDF is published - no full-year source exists.
- **Image feeds (AZ, RS, FJ) + scanned PDFs (BD, DZ, AF):** timetable is a JPG/scanned page with no
  text layer; needs OCR (Tesseract) before parsing.
- **LK zone 01 (Colombo), June only:** ACJU publishes that one month as a scanned image (no text
  layer), so `AcjuScraper` fails zone 01 and the committed table keeps its June gap. Every other LK
  month/zone is text-extracted. Needs OCR for that single PDF.

## Known source data-quality issues

- **JAKIM SBH09 (Sabah), zohor:** e-Solat currently returns **null `zohor` for all 365 days** of
  SBH09. `JakimFeedParser` fails loudly on a null time, so a re-scrape safely fails that one zone and
  keeps the committed table (captured when e-Solat still served zohor). Those committed values carry
  a small (~8 min) JAKIM dhuhr discontinuity at 1 Apr - it is JAKIM's own data, not a parser error.
  Do not blind-overwrite MY from a fresh scrape without checking SBH09.
- **JS-only / today-only sites (ID, PL, SE, NO, MU, KG, TJ, UZ, IR, HK, HR, LB, MK, JO, TH, IE, GB, ZA):**
  no reachable full-year machine-readable feed; either the data is injected by client JS (endpoint
  must be found in the browser Network tab) or the page shows only today.
- **api placeholders (AE, DE, NL, QA, TR):** no keyless full-year feed - AE/DE/NL are computed
  presets, QA is today-only behind a bot-WAF, TR is JWT-gated. Left computed.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Unknown e-Solat month in date '...'` | a month token the parser's map misses (locale) - extend `JakimFeedParser.MONTHS` |
| `returned HTTP 500` | endpoint/params; for e-Solat use `period=year`, not `duration` |
| `JAKIM returned no <year> days` | the authority is not serving that year yet - use the current published year |
| large golden-diff on every zone | wrong offset/zone mapping or a source change - do not commit |
