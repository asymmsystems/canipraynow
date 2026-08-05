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

## Known authority gotchas

- **JAKIM (MY), e-Solat:** `period=year` serves the current published year (there is no year
  parameter); `period=duration` for a full year returns **HTTP 500**. Dates are **Malay**
  (`01-Mac-2026`, `01-Ogos-2026`), parsed by first-three-letters. ~60 zones.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Unknown e-Solat month in date '...'` | a month token the parser's map misses (locale) - extend `JakimFeedParser.MONTHS` |
| `returned HTTP 500` | endpoint/params; for e-Solat use `period=year`, not `duration` |
| `JAKIM returned no <year> days` | the authority is not serving that year yet - use the current published year |
| large golden-diff on every zone | wrong offset/zone mapping or a source change - do not commit |
