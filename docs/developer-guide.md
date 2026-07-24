<!--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
  Developer and usage guide for the can-i-pray-now generator reactor.
  @author Fahim Farook
-->

# can-i-pray-now: Developer and Usage Guide

This guide takes you from a fresh clone to generated pages, and explains the whole
data lifecycle in between. It is meant to be read top to bottom the first time.

Throughout, `$REPO` means the reactor root:

```
/home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
```

Run every command from `$REPO` with `JAVA_HOME=/opt/eliya` set (section 2 explains why).

---

## 1. Glossary (read this first)

The rest of the guide uses these terms precisely; none of them are left implicit.

| Term | Meaning |
| --- | --- |
| **Authority** | The body that publishes official prayer times for a country or region, e.g. ACJU (Sri Lanka), JAKIM (Malaysia). We reproduce their tables rather than invent times. |
| **cc** | An **ISO 3166-1 alpha-2 country code** - two letters, e.g. `LK` (Sri Lanka), `GB` (United Kingdom), `MY` (Malaysia). Upper-case in data; the on-disk timetable folder uses the lower-case form (`timetables/lk/`). |
| **Zone** | A region an authority publishes one shared timetable for. An authority divides its country into zones (ACJU has districts grouped into numbered zones). |
| **Zone id** | The authority's own identifier for a zone, taken verbatim, e.g. ACJU zone `01` = "Colombo, Gampaha, Kalutara". It is not invented by us; it comes from the authority and is used to name the file `zone-01.xml`. |
| **Slug** | A URL-safe lower-case name, e.g. `colombo`, `sri-lanka`. A city's page URL is `<country-slug>/<slug>.html`. |
| **Adhan** | A single prayer time (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha). |
| **Madhab** | The juristic school that sets the Asr shadow factor; supplied per calculation, not stored in data. |
| **S1** | **Scrape** stage: read an authority's published source and emit standard XML data. |
| **S2** | **Generate** stage: read that XML data and produce the per-city static HTML pages. This is the stage you can run today. |
| **E1 / E2 / E3** | Calculation engines: E1 reads a baked table, E3 is the general astronomy engine, E2 = E3 plus an authority's published angles/offsets. |
| **C1 / C2** | Client side: C1 reads a baked table in the browser, C2 is the astronomy engine compiled to WASM. |
| **DATA_DIR** | Input directory for S2. Holds `cities.xml` and `timetables/<cc>/...`. It is **checked into the repo**; the default is `$REPO/data` (section 8). |
| **OUTPUT_DIR** | Output directory for S2. Where generated HTML is written. It lives under **`target/`** and is never committed; the default is `application/cipn-app-batch/target/site`. |

---

## 2. Prerequisite: the Eliya JDK (mandatory, enforced by the build)

The build requires the **Eliya JDK** (Asymm Systems' OpenJDK), installed at `/opt/eliya`.
This is enforced: the reactor root POM runs a Maven Enforcer rule (`RequireJavaVendor`) in the
`validate` phase that checks `java.vendor == "Asymm Systems"`. Any other JDK (Temurin, Zulu,
Oracle) fails the build before compilation with:

```
Build requires the Eliya JDK (java.vendor = "Asymm Systems").
Install it at /opt/eliya and set JAVA_HOME=/opt/eliya.
```

Set it for every command below, and verify:

```sh
export JAVA_HOME=/opt/eliya
/opt/eliya/bin/java -version      # -> Eliya-25.0.3+24, vendor "Asymm Systems"
```

Do **not** use `~/.sdkman/candidates/java/current` - that is Temurin, and the build rejects it.

---

## 3. The end-to-end lifecycle (the big picture)

canipraynow turns an authority's published prayer times into a static website. Data flows
through four stages. XML is read once, at the front (S1's output); after that everything is
Java objects until HTML comes out.

```
  Authority sources                data/  (standard XML,          target/site/  (HTML)
  (websites, PDFs)                  checked in)
        │                              │                              │
        ▼            S1               ▼            S2                 ▼        JBake        Deploy
  ┌───────────┐   scrape &     ┌────────────┐   generate      ┌────────────┐  bake   ┌──────────┐
  │  ACJU,    │──normalise ───▶│ cities.xml │──per-city ─────▶│ per-city   │────────▶│  static  │
  │  JAKIM... │  to standard   │ timetables/│   pages          │ .html      │         │  site    │
  └───────────┘   schema       └────────────┘                 └────────────┘         └──────────┘
```

| Stage | Reads | Writes | Where the output lives | Status |
| --- | --- | --- | --- | --- |
| **S1 scrape** | authority websites / PDFs | `cities.xml` + `timetables/<cc>/*.xml` (standard schema, section 7) | `$REPO/data` (checked in) | **Not yet in Java (T-26).** Legacy node scrapers still exist but are being removed. See section 10. |
| **S2 generate** | `$REPO/data` | per-city `<country-slug>/<slug>.html` | `application/cipn-app-batch/target/site` | **Done** - `SiteGeneratorApp`. Run it with the Maven target in section 9. |
| **Site bake** | generated HTML + templates | full static site | JBake output | Current site still bakes stubs via `GenerateCityPages.java`; wiring S2's output into JBake is part of the cutover (section 10). |
| **Deploy** | baked site | hosted site | hosting | out of scope here |

The important invariant: **whatever authority S1 scrapes, and whatever format the authority
publishes in, S1 normalises it into one standard XML schema** (section 7). Every country's data
therefore looks identical to S2 - S2 has no per-authority code.

---

## 4. Module map

The reactor is decomposed by **volatility** (IDesign / Löwy), built top-down:
`wiring → framework → service → application`.

```
$REPO
├── pom.xml                          reactor root (Eliya enforcer lives here)
├── data/                            checked-in standard XML data = the default DATA_DIR
├── schema/                          the standard XSDs S1 output / S2 input must satisfy
├── framework/
│   ├── cipn-fw-common               pure utilities: XML parser, geometry, formatters (WASM-safe)
│   └── cipn-fw-contract/
│       ├── cipn-fw-contract-data    pure records/enums (City, TimetableRow, ...). No behaviour.
│       ├── cipn-fw-contract-access  Accessor marker + accessor interfaces
│       ├── cipn-fw-contract-engine  Engine marker + engine interfaces
│       └── cipn-fw-contract-manager Manager marker + manager interfaces
├── service/
│   ├── cipn-svc-engine              engine impls (Default*): pure logic, zero I/O, WASM-bound
│   ├── cipn-svc-accessor/
│   │   └── cipn-svc-accessor-file   file/classpath accessors: I/O only
│   └── cipn-svc-manager             manager impls: orchestration + build-time templating
├── wiring/
│   ├── variant/                     per-service-type wiring (composes the Default* impls)
│   └── assembly/cipn-wiring-assembly-standard   the single assembly the app depends on
└── application/
    ├── cipn-app-site                the JBake static site (still holds the live JSON data)
    ├── cipn-app-server/*            runtime server variants
    └── cipn-app-batch               the S2 generator entry point (SiteGeneratorApp)
```

---

## 5. The data and config files (inventory)

Every input the code reads is XML (project rule: XML for all config/data/oracles - no JSON,
YAML, properties, or node). Here is every such file, where it lives, and who touches it.

| File | Location | What it is | Produced by | Consumed by | Schema |
| --- | --- | --- | --- | --- | --- |
| `cities.xml` | `$REPO/data/cities.xml` | Master list of cities to generate | S1 (today: seeded by hand) | S2 city accessor | `schema/cities.xsd` |
| `zones.xml` | `$REPO/data/timetables/<cc>/zones.xml` | One country's authority + its zone index | S1 | S2 timetable accessor | `schema/zones.xsd` |
| `zone-<id>.xml` | `$REPO/data/timetables/<cc>/zone-<id>.xml` | One zone's published timetable | S1 | S2 timetable accessor | `schema/timetable.xsd` |
| `calculation-methods.xml` | `service/cipn-svc-accessor/cipn-svc-accessor-file/src/main/resources/calculation-methods.xml` | E2 registry: each authority's Fajr/Isha angles + offsets | maintained by hand | calculation-method accessor | (internal) |
| `golden-vectors.xml` | `service/cipn-svc-engine/src/test/resources/golden-vectors.xml` | 2688 astronomy test oracles for E3 parity | one-off | engine parity test | (internal) |
| `cities.json`, `timetables/**` | `application/cipn-app-site/src/main/jbake/assets/data/` | **Legacy** live data, still JSON | old node scrapers | old `GenerateCityPages.java` | none (to be retired) |

The three files in `$REPO/data/**` are the standard S1-output / S2-input contract; they are
detailed next.

### 5.1 `cities.xml`

**Location:** `$REPO/data/cities.xml`. **Schema:** `schema/cities.xsd`.

The master list. One `<city>` per city; `page="true"` means "generate a page for it".

```xml
<?xml version="1.0" encoding="UTF-8"?>
<cities>
    <city name="Colombo" country="Sri Lanka" cc="LK" slug="colombo" country-slug="sri-lanka"
          lat="6.9271" lng="79.8612" tz="Asia/Colombo" page="true"/>
    <city name="London" country="United Kingdom" cc="GB" slug="london" country-slug="united-kingdom"
          lat="51.5074" lng="-0.1278" tz="Europe/London" page="true"/>
</cities>
```

| Attribute | Meaning |
| --- | --- |
| `name`, `country` | display names |
| `cc` | ISO 3166-1 alpha-2 country code (glossary). Links the city to `timetables/<lower-cc>/`. |
| `slug`, `country-slug` | URL segments; page path is `<country-slug>/<slug>.html` |
| `lat`, `lng` | coordinates (used to snap the city to a zone and to find nearby cities) |
| `tz` | IANA timezone id |
| `page` | `true` to generate a page, `false` to list without one |

### 5.2 `zones.xml` (one country's zone index)

**Location:** `$REPO/data/timetables/<cc>/zones.xml` - one per country, e.g.
`data/timetables/lk/zones.xml`. **Schema:** `schema/zones.xsd`.

Names the authority and lists its zones with a representative point each. The **zone id** is
the authority's own identifier (glossary) and must match a `zone-<id>.xml` file in the same
folder.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<zones authority="ACJU"
       authority-name="All Ceylon Jamiyyathul Ulama"
       authority-url="https://www.acju.lk/prayer-times/"
       country="Sri Lanka">
    <zone id="01" name="Colombo, Gampaha, Kalutara" lat="6.9271" lng="79.8612"/>
    <zone id="07" name="Kandy, Matale, Nuwara Eliya" lat="7.2906" lng="80.6337"/>
</zones>
```

### 5.3 `zone-<id>.xml` (one zone's timetable)

**Location:** `$REPO/data/timetables/<cc>/zone-<id>.xml`, where `<id>` is the **zone id** from
`zones.xml`, e.g. `data/timetables/lk/zone-01.xml`. **Schema:** `schema/timetable.xsd`.

The authority's published table for that zone. `unit="minutes-from-midnight"`, so `fajr="300"`
means 05:00. One `<day>` per calendar date; rows are sorted by month/day on read.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<timetable authority="ACJU" zone="01" name="Colombo, Gampaha, Kalutara"
           source="https://www.acju.lk/prayer-times/" unit="minutes-from-midnight">
    <day month="1" day="1" fajr="300" sunrise="382" dhuhr="735" asr="937" maghrib="1087" isha="1161"/>
    <day month="1" day="2" fajr="300" sunrise="383" dhuhr="736" asr="937" maghrib="1088" isha="1162"/>
</timetable>
```

A country with a `timetables/<cc>/` folder gets an authority table baked into its pages; a
country without one (e.g. `GB` above) gets a page with no baked table.

### 5.4 `calculation-methods.xml` (E2 registry)

**Location:** `service/cipn-svc-accessor/cipn-svc-accessor-file/src/main/resources/calculation-methods.xml`.
This is a **classpath resource** inside the accessor module, **not** part of `DATA_DIR`. It
holds each authority's Fajr/Isha angles and offsets (only non-default fields written).

```xml
<calculation-methods default-method="muslim-world-league">
    <method key="acju" fajr-angle="20" isha-angle="18"/>
    <method key="france" fajr-angle="12" isha-angle="12"/>
    <!-- ... -->
</calculation-methods>
```

---

## 6. Where XML is parsed (and where it is not)

There is exactly one XML parser: `StructuredXmlParser` in `cipn-fw-common` (a thin wrapper over
Jackson's `XmlMapper`). Only the **accessors** call it, and only to **read**. There is no
Java to XML serialisation anywhere. After the accessor, data is plain Java records; the manager
transforms them (pure Java) and FreeMarker renders HTML. The `cipn-svc-engine` never touches the
parser (WASM hygiene), and ArchUnit fails the build if it ever does.

---

## 7. The standard schema (`schema/*.xsd`)

**Location:** `$REPO/schema/`. These XSDs are the country-independent contract that all scraped
data must satisfy - the shape S1 must emit and S2 relies on.

| Schema | Validates |
| --- | --- |
| `schema/cities.xsd` | `data/cities.xml` |
| `schema/zones.xsd` | every `data/timetables/<cc>/zones.xml` |
| `schema/timetable.xsd` | every `data/timetables/<cc>/zone-<id>.xml` |

Validate a data file against its schema at any time:

```sh
xmllint --noout --schema "$REPO/schema/timetable.xsd" "$REPO/data/timetables/lk/zone-01.xml"
```

When S1 is built, validating its output against these XSDs is the acceptance test for every new
country: if it validates, S2 can consume it unchanged.

---

## 8. Building

Full reactor build (compile + test + package), skipping PMD (its remote ruleset 401s):

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
./mvnw verify -Dpmd.skip=true
```

| Goal | Command (from `$REPO`, `JAVA_HOME=/opt/eliya`) |
| --- | --- |
| Compile + test + package everything | `./mvnw verify -Dpmd.skip=true` |
| Compile + install to local `~/.m2` | `./mvnw install -Dpmd.skip=true` |
| Fast build, skip tests | `./mvnw verify -Dpmd.skip=true -Dmaven.test.skip=true` |
| Just check the Eliya gate passes | `./mvnw validate` |
| One module + its deps | `./mvnw verify -Dpmd.skip=true -pl application/cipn-app-batch -am` |

---

## 9. Running the stages

### 9.1 S2 - generate the per-city pages (works today)

`SiteGeneratorApp` reads `DATA_DIR`, writes pages to `OUTPUT_DIR`. Both default to locations
**inside the codebase**, so you pass nothing:

- `DATA_DIR` default: `$REPO/data` (checked in).
- `OUTPUT_DIR` default: `application/cipn-app-batch/target/site` (under `target/`, not committed).

Run the target (requires `install` first so the reactor classpath is resolvable):

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw

./mvnw install -Dpmd.skip=true -Dmaven.test.skip=true          # once, to populate ~/.m2
./mvnw -pl application/cipn-app-batch exec:java@generate-site   # generate

# inspect
find application/cipn-app-batch/target/site -type f
```

Expected: `Generated 2 city pages (1 with a baked authority timetable); swept 0 stale.`, and
`sri-lanka/colombo.html` (with the ACJU table + neighbour links) plus `united-kingdom/london.html`
(no baked table) under `target/site`.

Override the defaults only if you want to (e.g. point at a scratch export):

```sh
./mvnw -pl application/cipn-app-batch exec:java@generate-site \
  -Dcipn.data.dir="$REPO/data" -Dcipn.output.dir="$REPO/application/cipn-app-batch/target/export"
```

### 9.2 S1 - scrape into the standard XML (not yet available)

**S1 is not implemented in Java yet (task T-26).** Only S2 has an entry point today. The legacy
scrapers are node scripts (`application/cipn-app-site/scripts/fetch-*.mjs`), which the project is
removing (no node allowed). So there is no runnable S1 Maven target yet.

The intended shape, once T-26 lands, is a batch app that writes into the same checked-in `data/`
directory S2 reads, validated against `schema/*.xsd`:

```sh
# intended (pending T-26) - writes $REPO/data/**, then validate:
./mvnw -pl application/cipn-app-batch exec:java@scrape -Dcipn.authority=acju
xmllint --noout --schema schema/timetable.xsd data/timetables/lk/zone-*.xml
```

Until then, `data/**` is maintained by hand (currently a small sample), and the cutover from the
legacy JSON is what will populate it in full. See section 10.

---

## 10. Current state and known gaps

Honest status, so nothing here reads as finished when it is not:

- **S1 (Java scraper) does not exist** - only the deprecated node `.mjs` scripts. Building the
  Java S1 is **T-26**; per the Asymm process it needs a short spec/ADR before implementation.
- **`data/` holds a small sample** (Colombo, London, ACJU zone 01), enough to run and test S2.
  The full production data still lives as **JSON** in
  `application/cipn-app-site/src/main/jbake/assets/data/`. Converting it to the XML in `data/`
  is the **cutover**.
- **`GenerateCityPages.java`** (in `application/cipn-app-site/scripts/`) still generates page
  stubs during the site build; retiring it and feeding S2's `target/site` output into JBake is
  the rest of the cutover.

---

## 11. Testing

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
./mvnw test -Dpmd.skip=true
```

Key suites:

- **`ArchitectureTest`** (`application/cipn-app-batch`, ArchUnit) - enforces the layering: app
  depends only on wiring/contracts, engines are framework/parser-free (WASM), contracts do not
  depend on impls, no package cycles. Needs ArchUnit 1.4.1+ (Java 25 bytecode) and Surefire's
  `useManifestOnlyJar=false` (already configured).
- **`SiteGenerationIntegrationTest`** (`application/cipn-app-batch`) - **the S2 test**: wires the
  whole stack (assembly -> variants -> impls -> FreeMarker) and generates pages from XML fixtures
  in `src/test/resources/gen` (the same shape as `data/`).
- **Astronomy parity** (`cipn-svc-engine`) - checks E3 against the 2688 `golden-vectors.xml`
  oracles for exact adhan agreement.

Run one suite:

```sh
./mvnw test -Dpmd.skip=true -pl application/cipn-app-batch -Dtest=SiteGenerationIntegrationTest
```

**S1 has no test yet** because S1 does not exist yet (section 10); its acceptance test will be
XSD-validation of its output plus a fixture-based scrape test (offline HTML, no live network).

---

## 12. Conventions (so a build passes review)

- **Eliya only.** `JAVA_HOME=/opt/eliya`; the reactor rejects any other JDK (section 2).
- **XML for all config/data/oracles.** No JSON, YAML, properties. No node/npm/`.mjs`.
- **Service-type pattern.** A service = a marker interface (`Engine`/`Manager`/`Accessor`) + a
  specific contract interface in `cipn-fw-contract-*`, with a `Default*` impl in `cipn-svc-*`,
  wired externally. No frameworkless concrete class dumped in a service module.
- **Wiring only in the app.** `application/*` depends solely on `cipn-wiring-assembly-standard`;
  it never depends on `svc-*` or `new`s an impl.
- **WASM hygiene.** Keep FreeMarker/Jackson/Spring off `cipn-svc-engine` and off
  `cipn-fw-common`'s engine-reachable path. Templating lives in `cipn-svc-manager` (build-time).
- **Headers.** Every source file starts with
  `Copyright (c) 2026 Asymm Systems. All rights reserved.` + `@author Fahim Farook`. User-facing
  and legal text (site footer, README) uses the full legal entity `Asymm Systems (Pvt) Ltd`.
- **Every enum constant and record component is documented.**

`ArchitectureTest` mechanically enforces the layering, so a violation fails `verify` rather than
waiting for review.
