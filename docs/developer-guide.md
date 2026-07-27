<!--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
  Developer and usage guide for the can-i-pray-now generator reactor.
  @author Fahim Farook
-->

# can-i-pray-now: Developer and Usage Guide

This guide takes you from a fresh clone to generated pages, and explains the whole data
lifecycle in between. It is meant to be read top to bottom the first time. The data model and
placement decisions here are recorded in ADR-0012 (and ADR-0009, ADR-0011).

Throughout, `$REPO` means the reactor root:

```
/home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
```

Run every command from `$REPO` with `JAVA_HOME=/opt/eliya` set (section 2 explains why).

---

## 1. Glossary (read this first)

| Term | Meaning |
| --- | --- |
| **Authority** | The body that publishes official prayer times for a country/region, e.g. ACJU (Sri Lanka), JAKIM (Malaysia). We reproduce their tables rather than invent times. |
| **cc** | An **ISO 3166-1 alpha-2 country code** - two letters, e.g. `LK`, `GB`, `MY`. Upper-case in data; the on-disk timetable folder uses the lower-case form (`.../lk/`). |
| **Zone** | A region an authority publishes one shared timetable for (e.g. ACJU groups districts into numbered zones). |
| **Zone id** | The authority's own identifier for a zone, taken verbatim, e.g. ACJU zone `01`. It names the file `zone-01.xml`. |
| **Slug** | A URL-safe lower-case name, e.g. `colombo`, `sri-lanka`. A city's page URL is `<country-slug>/<slug>.html`. |
| **S1** | **Scrape** stage: read an authority's published source and emit standard XML timetables. Native Java (JAKIM first). Not yet built - see section 10. |
| **S2** | **Generate** stage: read the bundled XML and produce the per-city static HTML. This is what you run today (`SiteGeneratorApp`). |
| **Output root** | Where S2 writes HTML. Under **`target/`**, never committed. Default `application/cipn-app-batch/target/site`. This is the only path you pass S2. |

> **There is no `DATA_DIR`.** Input data is **bundled on the classpath** inside the accessor
> module (ADR-0012), so the generator needs no data path - only an output root. Singular names
> throughout: `city.xml`, `zone.xml`, `zone-<id>.xml`, `calculation-method.xml`.

---

## 2. Prerequisite: the Eliya JDK (mandatory, enforced)

The build requires the **Eliya JDK** (Asymm Systems' OpenJDK) at `/opt/eliya`. Enforced: the
reactor root POM runs `maven-enforcer` `RequireJavaVendor` in `validate`, failing any JDK whose
`java.vendor` is not `"Asymm Systems"`.

```sh
export JAVA_HOME=/opt/eliya
/opt/eliya/bin/java -version      # -> Eliya-25.0.3+24, vendor "Asymm Systems"
```

Do **not** use `~/.sdkman/candidates/java/current` (Temurin) - the build rejects it.

---

## 3. The end-to-end lifecycle

An authority's published times become a static site through four stages. XML is read once, at
the front; after that everything is Java objects until HTML comes out.

```
  Authority sources         accessor classpath data       target/site/  (HTML)
  (websites, PDFs)          (committed XML resources)
        │                         │                             │
        ▼          S1            ▼          S2                  ▼      JBake       Deploy
  ┌───────────┐  scrape &  ┌────────────┐  generate     ┌────────────┐ bake  ┌──────────┐
  │ ACJU,     │─normalise ▶│ city.xml   │─per-city,─────▶│ per-city   │──────▶│  static  │
  │ JAKIM ... │ to schema  │ timetable/ │  per-year       │ .html      │       │  site    │
  └───────────┘            └────────────┘                └────────────┘       └──────────┘
```

| Stage | Reads | Writes | Where | Status |
| --- | --- | --- | --- | --- |
| **S1 scrape** | authority sites/PDFs | `timetable/<year>/<cc>/*.xml` + `index.xml` | accessor classpath resources | **Not yet in Java (T-26/F2).** JAKIM first. Legacy node scrapers being removed. |
| **S2 generate** | bundled classpath data | `<year>/<country-slug>/<slug>.html` (+ canonical current year at root) | `.../cipn-app-batch/target/site` | **Done** - `SiteGeneratorApp`. Section 9. |
| **Site bake** | generated HTML + templates | full static site | JBake output | cutover pending (retire `GenerateCityPages.java`). |
| **Deploy** | baked site | hosted site | hosting | out of scope here |

Whatever authority S1 scrapes, and whatever format it publishes, S1 **normalises it into one
standard schema** (section 7) - so every country looks identical to S2, which has no
per-authority code.

**Annual rollover (no cron):** an authority table is one year. S2 is year-parameterized and
bakes the **current + next** year; the next year's pages exist before Dec 31, so Jan 1 rolls
over with no build. The T-05 runbook makes "next-year data present" a release check - S2 prints
a WARNING for any baked year with no tables.

---

## 4. Module map

Decomposed by **volatility** (ADR-0011), built top-down: `wiring → framework → service → application`.

```
$REPO
├── pom.xml                          reactor root (Eliya enforcer)
├── framework/
│   ├── cipn-fw-common               pure utilities: XML parser, geometry (WASM-safe)
│   └── cipn-fw-contract/
│       ├── cipn-fw-contract-data    records/enums + resources/schema/*.xsd (the data contract)
│       ├── cipn-fw-contract-access  Accessor marker + accessor interfaces
│       ├── cipn-fw-contract-engine  Engine marker + engine interfaces
│       └── cipn-fw-contract-manager Manager marker + manager interfaces
├── service/
│   ├── cipn-svc-engine              engine impls (Default*): pure logic, WASM-bound
│   ├── cipn-svc-accessor/
│   │   ├── cipn-svc-accessor-classpath  READ accessors + the bundled data (city, timetable, methods)
│   │   └── cipn-svc-accessor-file        the content WRITER only (FileContentAccessor)
│   └── cipn-svc-manager             manager impls: orchestration + build-time templating
├── wiring/
│   ├── variant/{engine, accessor-classpath, accessor-file, manager}
│   └── assembly/cipn-wiring-assembly-standard   the single assembly the app depends on
└── application/
    ├── cipn-app-site                the JBake static site (legacy live JSON data)
    ├── cipn-app-server/*            runtime server variants
    └── cipn-app-batch               the S2 generator entry point (SiteGeneratorApp)
```

Accessors are split by **location** (ADR-032): the classpath module holds the read-side
(bundled data), the file module holds the write-side (output). Each accessor is named by its
**consumed type** (City / Timetable / CalculationMethod), one per resource.

---

## 5. The data and config files (inventory)

Every input is XML (project rule: XML for all config/data/oracles - no JSON/YAML/node). All of
it is committed inside the accessor module (data) or the contract-data module (schema).

| File | Location | What it is | Produced by | Consumed by | Schema |
| --- | --- | --- | --- | --- | --- |
| `city.xml` | `cipn-svc-accessor-classpath/src/main/resources/city.xml` | Curated city list (near-static) | maintained by hand | `ClasspathCityAccessor` | `schema/city.xsd` |
| `index.xml` | `.../resources/timetable/<year>/index.xml` | Coverage manifest: which countries have a table that year | S1 | `ClasspathTimetableAccessor` | `schema/index.xsd` |
| `zone.xml` | `.../resources/timetable/<year>/<cc>/zone.xml` | One country's authority + zone index | S1 | `ClasspathTimetableAccessor` | `schema/zone.xsd` |
| `zone-<id>.xml` | `.../resources/timetable/<year>/<cc>/zone-<id>.xml` | One zone's annual table | S1 | `ClasspathTimetableAccessor` | `schema/timetable.xsd` |
| `calculation-method.xml` | `.../resources/calculation-method.xml` | E2 registry: authority Fajr/Isha angles + offsets | maintained by hand | `ClasspathCalculationMethodAccessor` | (internal) |
| `golden-vectors.xml` | `cipn-svc-engine/src/test/resources/golden-vectors.xml` | 2688 astronomy oracles for E3 parity | one-off | engine parity test | (internal) |
| `cities.json`, `timetables/**` | `cipn-app-site/src/main/jbake/assets/data/` | **Legacy** live data, still JSON | old node scrapers | old `GenerateCityPages.java` | none (to be retired) |

The schema files live in `framework/cipn-fw-contract/cipn-fw-contract-data/src/main/resources/schema/`
because `timetable`/`zone` are the cross-module S1-produces / S2-consumes contract (section 7).

### 5.1 `city.xml`

**Location:** `service/cipn-svc-accessor/cipn-svc-accessor-classpath/src/main/resources/city.xml`.
Curated list; `page="true"` means "generate a page". One `<city>` per city.

```xml
<city name="Colombo" country="Sri Lanka" cc="LK" slug="colombo" country-slug="sri-lanka"
      lat="6.9271" lng="79.8612" tz="Asia/Colombo" page="true"/>
```

| Attr | Meaning | | Attr | Meaning |
| --- | --- | --- | --- | --- |
| `name`/`country` | display names | | `lat`/`lng` | coordinates (zone snap + neighbours) |
| `cc` | ISO alpha-2 country code | | `tz` | IANA timezone |
| `slug`/`country-slug` | URL segments | | `page` | `true` to generate a page |

### 5.2 `timetable/<year>/index.xml`

**Location:** `.../resources/timetable/<year>/index.xml`. Lists the countries that have a table
for that year, so the accessor enumerates coverage without listing a directory (jar-safe).

```xml
<timetable-index year="2026">
    <country cc="LK"/>
</timetable-index>
```

### 5.3 `timetable/<year>/<cc>/zone.xml`

**Location:** `.../resources/timetable/<year>/<cc>/zone.xml` (e.g. `timetable/2026/lk/zone.xml`).
Names the authority and lists its zones; the **zone id** must match a `zone-<id>.xml`.

```xml
<zones authority="ACJU" authority-name="All Ceylon Jamiyyathul Ulama"
       authority-url="https://www.acju.lk/prayer-times/" country="Sri Lanka">
    <zone id="01" name="Colombo, Gampaha, Kalutara" lat="6.9271" lng="79.8612"/>
</zones>
```

### 5.4 `timetable/<year>/<cc>/zone-<id>.xml`

**Location:** `.../resources/timetable/<year>/<cc>/zone-<id>.xml`. The authority's published
table; `unit="minutes-from-midnight"`, one `<day>` per date, including `solar-noon`.

```xml
<timetable authority="ACJU" zone="01" name="Colombo, Gampaha, Kalutara"
           source="https://www.acju.lk/prayer-times/" unit="minutes-from-midnight">
    <day month="1" day="1" fajr="300" sunrise="382" dhuhr="735" asr="937"
         maghrib="1087" isha="1161" solar-noon="734"/>
</timetable>
```

### 5.5 `calculation-method.xml`

**Location:** `.../resources/calculation-method.xml`. The E2 registry (Fajr/Isha angles +
offsets per authority); only non-default fields are written.

---

## 6. Where XML is parsed (and where it is not)

One parser: `StructuredXmlParser` in `cipn-fw-common` (a thin wrapper over Jackson `XmlMapper`).
Only the **accessors** call it, and only to **read** - there is no Java-to-XML anywhere. After
the accessor it is plain Java records; the manager transforms them and FreeMarker renders HTML.
The `cipn-svc-engine` never touches the parser (WASM hygiene); ArchUnit fails the build if it does.

---

## 7. The standard schema (`schema/*.xsd`)

**Location:** `framework/cipn-fw-contract/cipn-fw-contract-data/src/main/resources/schema/`.
These XSDs are the country-independent contract all scraped data must satisfy - the S1-output /
S2-input boundary. They live in the framework because `timetable`/`zone` are shared across S1
(producer) and S2 (consumer), so they belong with the record contract, not inside one impl.

| Schema | Validates |
| --- | --- |
| `city.xsd` | `city.xml` |
| `index.xsd` | every `timetable/<year>/index.xml` |
| `zone.xsd` | every `timetable/<year>/<cc>/zone.xml` |
| `timetable.xsd` | every `timetable/<year>/<cc>/zone-<id>.xml` |

Validate any data file:

```sh
SC=framework/cipn-fw-contract/cipn-fw-contract-data/src/main/resources/schema
D=service/cipn-svc-accessor/cipn-svc-accessor-classpath/src/main/resources
xmllint --noout --schema $SC/timetable.xsd $D/timetable/2026/lk/zone-01.xml
```

When S1 is built, validating its output against these XSDs is the acceptance test per new country.

---

## 8. Building

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
./mvnw verify -Dpmd.skip=true
```

| Goal | Command (from `$REPO`, `JAVA_HOME=/opt/eliya`) |
| --- | --- |
| Compile + test + package | `./mvnw verify -Dpmd.skip=true` |
| Install to local `~/.m2` | `./mvnw install -Dpmd.skip=true` |
| Fast, skip tests | `./mvnw verify -Dpmd.skip=true -Dmaven.test.skip=true` |
| Check the Eliya gate | `./mvnw validate` |
| One module + deps | `./mvnw verify -Dpmd.skip=true -pl application/cipn-app-batch -am` |

### 8.1 By lifecycle stage (Makefile) - Make orchestrates, Maven builds

The `Makefile` is the **orchestrator**: Maven builds one module, Make sequences the pipeline
(`scrape -> generate -> bake`). The two application modules never depend on each other in Maven;
the ordering lives in Make. Full rationale + every target and Maven goal: **ADR-0014**. Run
`make help` for the list. It defaults `JAVA_HOME=/opt/eliya` but honours an existing `JAVA_HOME`.

| Stage | Make target | What it does |
| --- | --- | --- |
| whole reactor | `make build` | compile + test + package everything (`mvn verify`) |
| whole reactor | `make test` | run every test suite (`mvn test`) |
| JDK check | `make doctor` | print the active JDK (must be Eliya) |
| **S1 scrape** | `make scrape` | scrape authority tables -> XML. **Not built yet (T-26)** - prints a notice |
| **S2 generate** | `make generate` | install the batch chain, then write the current year's city content into `src/main/jbake/content` (`SiteGeneratorApp --canonical-only`) |
| **site bake** | `make site` | run `generate`, then bake the static site (production, clean URLs) |
| site (dev) | `make dev` | live-reload JBake server on `:8020` |

`make site` is the whole build+bake: it runs `generate` (the reactor writes the city content,
including the baked authority table for LK/MY) and then JBake bakes it.

### 8.2 CI

Three GitHub workflows: `validate` (PRs), `preview` (push to develop), `production` (tags on
main). Each provisions the **Eliya JDK** via the composite action `.github/actions/setup-eliya`
(authenticated release download + GPG-signature + SHA-256 verification, then exports
`JAVA_HOME`), so CI satisfies the same `RequireJavaVendor` gate as local builds. The site
workflows' build step is **`make site`** - the same command humans run (ADR-0014).

---

## 9. Running the stages

### 9.1 Build the site (S2 generate -> JBake bake)

One command runs the whole pipeline (Make orchestrates it, ADR-0014):

```sh
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
make site
```

That runs, in order:
1. **generate (S2)** - the reactor (`cipn-app-batch`) reads the bundled classpath data and writes
   the current year's city content into `application/cipn-app-site/src/main/jbake/content/`
   (git-ignored, derived). For LK/MY cities the authority table is baked into the content body.
2. **bake** - JBake renders the content with `page/city.ftl`, emitting the full site (including
   the baked table) under `application/cipn-app-site/target/site`.

Inspect the result:

```sh
find application/cipn-app-site/target/site -name '*.html' | wc -l     # ~320 pages
grep -l authority-timetable application/cipn-app-site/target/site/malaysia/*.html
```

To generate the city content **without** baking (e.g. to inspect the reactor's output alone),
run the reactor directly - it defaults to the current + next year with per-year archives:

```sh
./mvnw install -Dpmd.skip=true -Dmaven.test.skip=true
./mvnw -pl application/cipn-app-batch exec:java@generate-site   # -> application/cipn-app-batch/target/site
```

Output layout of that standalone run:

```
target/site/sri-lanka/colombo.html            <- canonical (current year), clean URL
target/site/2026/sri-lanka/colombo.html       <- per-year archive
target/site/2027/sri-lanka/colombo.html       <- next year (WARNING if its data is absent)
```

### 9.2 S1 - scrape into the standard XML (not yet available)

**S1 is not implemented in Java yet (T-26/F2).** The legacy scrapers are node scripts being
removed. When built, S1 writes into the same accessor classpath resources S2 reads
(`timetable/<year>/<cc>/*.xml` + `index.xml`), validated against `schema/*.xsd`. **JAKIM /
Malaysia** is the first target (JSON over HTTP), then ACJU (PDF). See section 10.

---

## 10. Current state and known gaps

- **S1 (Java scraper) does not exist yet** - only deprecated node scripts. Building it is
  **T-26/F2**; JAKIM first. It gets its own service-type engine (`SourceExtractor`) per authority.
- **The bundled data is a small sample** (Colombo, London; ACJU 2026 zone 01), enough to run and
  test S2. The full world data still lives as **JSON** in `cipn-app-site/.../data/` - converting
  it into the classpath resources here is the **cutover**.
- **`GenerateCityPages.java`** still generates page stubs during the site build; retiring it and
  feeding S2's output into JBake is the rest of the cutover.
- **Next-year data** is typically absent until the annual refresh; S2 prints a WARNING for any
  baked year with no tables (the T-05 check).

---

## 11. Testing

```sh
export JAVA_HOME=/opt/eliya
./mvnw test -Dpmd.skip=true
```

- **`ArchitectureTest`** (`cipn-app-batch`, ArchUnit) - enforces the layering (app→wiring only,
  engines framework/parser-free, contracts don't depend on impls, no cycles). Needs ArchUnit
  1.4.1+ and Surefire `useManifestOnlyJar=false` (configured).
- **`SiteGenerationIntegrationTest`** (`cipn-app-batch`) - the S2 test: wires the whole stack and
  generates a year's pages from the bundled classpath data.
- **`Classpath*AccessorTest`** (`cipn-svc-accessor-classpath`) - the read accessors over the
  bundled resources.
- **Astronomy parity** (`cipn-svc-engine`) - E3 against the 2688 `golden-vectors.xml` oracles.

S1 has no test yet (it does not exist); its acceptance test will be XSD-validation of its output
plus a fixture-based scrape test (offline HTML/JSON, no live network in CI).

---

## 12. Conventions (so a build passes review)

- **Eliya only.** `JAVA_HOME=/opt/eliya`; the reactor rejects any other JDK.
- **XML for all config/data/oracles.** No JSON, YAML, properties. No node/npm/`.mjs`.
- **Singular names.** `city.xml`, `zone.xml`, `calculation-method.xml`, `timetable/`.
- **Service-type pattern.** Marker (`Engine`/`Manager`/`Accessor`) + specific contract in
  `cipn-fw-contract-*`, `Default*`/location-prefixed impl in `cipn-svc-*`, wired externally.
  Accessors are named by consumed type, one per resource; split into modules by location.
- **Wiring only in the app.** `application/*` depends solely on `cipn-wiring-assembly-standard`.
- **WASM hygiene.** Keep FreeMarker/Jackson/Spring off `cipn-svc-engine` and off `cipn-fw-common`'s
  engine-reachable path. Templating lives in `cipn-svc-manager` (build-time).
- **Headers.** Every source file: `Copyright (c) 2026 Asymm Systems. All rights reserved.` +
  `@author Fahim Farook`. Legal/user-facing text uses `Asymm Systems (Pvt) Ltd`.
- **Every enum constant and record component is documented.**

`ArchitectureTest` mechanically enforces the layering, so a violation fails `verify`.
