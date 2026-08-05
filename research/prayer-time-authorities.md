# Prayer-time authority register (T-01 + T-27) — authority-first

**Date:** 2026-08-05. **Status:** research, re-verified against live sources. **Author:** Fahim Farook.

This is the sourced register behind `country.xml`. It records, per country: the authority, the
calculation method (always, since compute is the fallback), and the **authority-first source
decision** — do we serve the authority's own published table (scrape) or compute with their method?
Companion audit of the method/angle claims: `research/fact-check/` (12 reports). This file
supersedes the pre-reactor copy that lived in the project tree, and retires `script/authorities.json`.

## 1. Guiding principle: authority-first, gated by obtainability

Where a national authority publishes a timetable **we can obtain and parse**, we serve *their*
numbers with attribution (`served-by="authority"`) — even if a standard method reproduces them,
because the authority's published value is what a follower of that authority expects. Computation
is the fallback where there is no authority, or no obtainable machine-readable feed
(`served-by="computed"`, using the authority's stated `method-key`).

The gate is **obtainability, not deviation**:
- Authority publishes an obtainable machine-readable table (api / html / pdf / image we can parse) → **scrape**.
- Authority exists but no obtainable feed (app-only, credential-gated, unreachable) → **compute** with its stated method.
- No authority at all → **compute** with a sensible default (MWL, or the regional convention).

The verdict the product actually rules on (sunrise, solar zenith, sunset) is astronomical and
authority-independent to within ~1-2 min (measured against ACJU Colombo; see section 5), so the
scrape-vs-compute choice affects the *sidebar prayer list*, not the "can I pray now" answer. The one
exception is the Jafari/Tehran convention, which delays Maghrib to 4.5 deg below the horizon (so its
"sunset" is not true astronomical sunset) — relevant to IR and to AZ's own (Jafari) authority table.

## 2. Where we are today

- **Served from the authority's extracted table:** Sri Lanka (ACJU, 13 district zones, monthly PDF)
  and Malaysia (JAKIM e-Solat, 60 official zones, JSON API). Both baked; the native-Java scraper
  (ADR-0017) pilots on JAKIM.
- **Everything else is computed** with the authority's method. The register below marks, per country,
  whether an obtainable feed exists (a **scrape candidate**) so the ADR-0017 rollout (T-26) can be
  ordered API-first.

## 3. Redistribution: the headline legal finding

**No authority surveyed grants explicit redistribution of its prayer times — including the two we
already publish (ACJU, JAKIM).** Every authority page carries an all-rights-reserved copyright or is
silent; several are actively restrictive (Saudi Umm al-Qura "all rights reserved"; Diyanet requires
a signed "User Request and Commitment Form"). Therefore `redistributable="unknown"` across the
register (Saudi marked `"no"`), with the basis recorded per row. **This drives the redistribution
feature flag: until a written grant exists, serving an authority's numbers rests on fair-use /
attribution, not permission.** The one plausible *yes* is Singapore — MUIS's table is on data.gov.sg
under the Singapore Open Data Licence (permissive) — but applicability to the prayer dataset must be
confirmed before flipping the flag.

## 4. Scrape source specs (verbatim, for the obtainable feeds)

Only feeds I (or the sub-agents) actually reached are listed with an endpoint. Blocked/gated feeds
are named so ADR-0017 does not waste a pilot on them. **Never treat an unverified endpoint as live.**

### API (easiest — ADR-0017 order: JAKIM → QA → KZ)

- **MY — JAKIM e-Solat** (DONE). `GET https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone=<ZONE>` → JSON. Locale trap: dates are Malay (`01-Mac-2026`), times `HH:MM:SS`. ~59 official zones. Keyless. Redistribution: unconfirmed (data.gov.my terms not established for republication).
- **QA — Qatar Calendar House via MOI.** `GET https://portal.moi.gov.qa/MoiPortalRestServices/rest/prayertimings/today/en` → JSON (`Fajr/Dhuhr/Asr/Maghrib/Isha` + City/Latitude/Longitude/QiblaDirection/TimeDifference). **Keyless but bot-hostile:** an F5/BIG-IP WAF rejects bare fetches (needs browser-like headers), and it is **today-only, single-point (Doha)**. The "Qatar" preset (18 / Isha 90 min) already reproduces it, so the scrape buys attribution, not accuracy. Confidence: high (endpoint identity), med (body unread behind WAF).
- **KZ — QMDB (muftyat.kz).** Two keyless GET steps (verified live 2026-08-05):
  1. Settlements: `GET https://api.muftyat.kz/cities/?page=<1..57>` → DRF-paginated JSON `{count:5695, results:[{id,title,lng,lat,timezone,region,district,...}]}`; `?search=<name>` filters.
  2. Times: `GET https://namaz.muftyat.kz/api/times/{YEAR}/{LAT}/{LNG}` → `{success:true, result:[365 daily {Fajr,Sunrise,Dhuhr,Asr,Maghrib,Isha}]}`. **Gotcha:** use the exact `lat`/`lng` strings from `/cities/` (order lat-then-lng); rounded/swapped coords return `{"success":false,"result":[]}` (200) or 500. The documented `api.muftyat.kz/prayer-times/...` path is dead (404); the working host is `namaz.muftyat.kz`. This is the cleanest world scrape candidate — 5695 settlements feed a future `timetable/<year>/kz/zone.xml`.
- **TR — Diyanet.** `https://awqatsalah.diyanet.gov.tr/api/` (login `.../Auth/Login` → JWT, 45-min token, refresh via `/Auth/RefreshToken/{token}`). **Credential-gated:** registration + a signed "Kullanıcı İstek ve Taahhüt Formu" (User Request and Commitment Form) required; rate-limited (5 req/endpoint; yearly range 10 req/month). Not a scrape — a credentialed pull. Only keyless view is the human HTML at `https://prayertimes.diyanet.gov.tr/en/`.
- **AE — GAIAE (federal) / IACAD (Dubai).** Federal `https://mobileappapi.awqaf.gov.ae` needs `clientGuid`+`clientSecret` with **no public key issuance** (keys are lifted from app traffic); IACAD `https://www.iacad.gov.ae/en/open-data/prayer-time-open-data` returns **HTTP 403** to bots. Both blocked → compute.
- **MV — third-party only.** The Ministry (islamicaffairs.gov.mv) is app-only (site 403). A live keyless mirror exists but is **unofficial**: `GET https://api.namaadhuvaguthu.com/prayertimes?format=json&island=<ID>` (per-island, 21 atolls / 194 islands; `range=month&month=&year=` supported). No stated licence; underlying data is legally the Ministry's. Compute (Karachi) unless we accept an unofficial mirror.

### HTML (parse; ADR-0017 phase 2)

- **ID — Kemenag/Bimas Islam.** Page `GET https://bimasislam.kemenag.go.id/jadwalshalat` (HTML, province/city form). Undocumented JSON behind it: `POST https://bimasislam.kemenag.go.id/ajax/getShalatbln` (params `x`=provinsi, `y`=kabkota, `bln`=month, `thn`=year → JSON month), with a session cookie first obtained from the page; `POST .../ajax/getKabkoshalat` (param `x` → city HTML). Official API is **permission-gated** (email `humasbimasislam@kemenag.go.id`, partner allow-list). 20/18 + ~2 min ihtiyat. Confidence: endpoints high, redistribution low.
- **SG — MUIS.** Official timetable on **data.gov.sg** (Singapore Open Data Licence — the one plausible redistribution *yes*; confirm). 20/18. Scrape candidate.
- **KG — muftiyat.kg** `/{ru,kg}/calendar/`: times are `00:00` placeholders filled client-side by JS; **no JSON endpoint exposed in static HTML** (needs live JS/network inspection). Not currently machine-readable.
- **TJ — shuroiulamo.tj** `/en/namaz`: a single static Dushanbe table + per-city minute offsets (-20..+5). No API. Light parse only.
- **UZ — islom.uz/prayertimes**: authority page returns **403** (bot-block); the cited `islomapi.uz` wrapper is third-party and was **502-down**. Not obtainable now.
- **Balkans (BA/XK/AL/MK), Nordics (SE/NO/DK), MA, ZA, DZ, JO:** official takvim/tables that deviate; HTML/PDF per-authority parsers, deferred.

### PDF / IMAGE (hard; ADR-0017 phase 3-4, PDFBox/Tesseract)

- **LK — ACJU** (DONE): monthly district PDFs, baked.
- **BD — Islamic Foundation Bangladesh:** annual **scanned/image** PDF (Dhaka base + district offsets, Karachi method). Portal (`islamicfoundation.gov.bd`) has a **broken TLS chain**; the file-store is keyless via Oracle object-storage (`objectstorage.ap-dcc-gazipur-1.oraclecloud15.com/n/axvjbnqprylg/b/V2Ministry/o/office-islamicfoundation/...`) but the artifact is graphical, not structured — needs OCR. No confirmed canonical PDF URL.
- **PH — BARMM Darul Ifta:** PDF, 5 provinces only (not national).
- **AZ — Caucasus Muslims Board:** monthly per-city JPEG, e.g. `https://www.qafqazislam.com/teqvim/2026/Baki/8.jpg` (1200x1696). **Single Jafari table** (İmsak, Sübh/Fajr, sunrise, Zöhr, Əsr, sunset, Məğrib, İşa, midnight) — *not* separate Shia/Sunni columns (corrects the prior lead). Needs OCR.

## 5. The astronomy still holds (unchanged from the prior pass)

Measured against ACJU's official Colombo-district July 2025 PDF, our engine: **sunrise 0.0 min,
zenith 0.2 min, sunset 1.6 min** (the three events the verdict depends on) — authority-grade,
offline, no API. Fajr/Isha are convention-dependent; ACJU fits **Fajr 20 / Isha 18** (mean error
0.74 / 0.97 min). So even where we compute rather than scrape, the verdict matches the authority.
All 16 method angles were re-verified externally (`research/fact-check/03-calculation-angles.md`).

## 6. Fact-check corrections applied to the register

From `research/fact-check/` (README "four hard errors"):
1. **AZ → MWL** (was Tehran): already corrected; Tehran imported the Jafari 4.5 deg Maghrib. Kept MWL; noted the authority's own table is Jafari (image).
2. **MY method phrase → "the JAKIM (Malaysia) convention"** (was "the MUIS (Singapore) convention"). Compute-fallback preset stays `Singapore` (identical 20/18 angles); times unchanged.
3. **EG → "Egyptian Survey Authority"** (was "Egyptian General Authority of Survey"): already corrected.
4. **UAE "Dubai" preset is unofficial** (Batoul-Apps 18.2 fit); official GAIAE/Awqaf is Gulf-region (Fajr 19.5, Isha 90 min). Recorded in the AE note; a `gulf-region` preset + client wiring is a follow-up (would shift UAE Fajr ~1 deg, so it is a reviewed change, not silent).

## 7. Deferred (explicitly not done in this pass)

- **zone.xml enumeration** for scrape candidates — especially **KZ's 5695 settlements** (mechanism captured in section 4; not enumerated), and SG/ID/Diyanet zone lists.
- **Java `*Scraper` impls** (ADR-0017) beyond JAKIM: QA and KZ are the API-first next targets.
- **Redistribution feature-flag wiring** into `CountryDefaults` + templates (the register now carries the data; the contract record does not yet expose it).
- **A `gulf-region` calculation method** (19.5 / 90 min) for AE/BH, and **a Jordan 18/18 preset**.
- **madhhab enrichment** (Shāfiʿī for LK/MV/SE-Asia; Ḥanafī for TR/S+C Asia) — held back to keep this
  a behavior-preserving register PR; it may shift the Asr default if `country.xml` drives the client.
- **Pass 2** (every country with a resident Muslim population) — this pass covered ~40 priorities + the full existing register.

## 8. Method note

The register was re-verified with three regional research agents (Gulf; SE/South Asia; Central
Asia/Caucasus/Turkey), each opening authority sites, aggregator method registries (aladhan,
arabeyes ITL), and open-data portals, and recording "unknown"/"blocked" where a source could not be
reached rather than guessing. Fetch blocks that are themselves findings for an automated scraper:
QA (F5 WAF), SA (conn refused), OM (TLS), KW (DNS), ID (conn refused), UZ (403), MV/AE (403),
BD (TLS). Every endpoint above that carries no "blocked" caveat was reached live.
