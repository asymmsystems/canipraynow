<!--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
  Developer and usage guide for the can-i-pray-now generator reactor.
  @author Fahim Farook
-->

# Developer and Usage Guide

This is the build-and-run guide for the **can-i-pray-now** Java reactor
(`can-i-pray-now-code-pfw`): the multi-module project that scrapes prayer-time data,
generates the per-city static pages, and (later) ships the in-browser WASM engine.

It is written to be followed top to bottom by someone who has just cloned the repo. Every
command uses full absolute paths, and every placeholder is spelled out with a concrete value.

- Repo root (referred to below as `$REPO`):
  `/home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw`

---

## 1. Prerequisites: the Eliya JDK (mandatory, enforced)

The build **requires the Eliya JDK** (Asymm Systems' OpenJDK distribution). This is not a
preference; the reactor's root POM runs a Maven Enforcer rule (`RequireJavaVendor`) in the
`validate` phase that checks `java.vendor == "Asymm Systems"`. Building with Temurin, Zulu,
Oracle, etc. fails immediately with:

```
Build requires the Eliya JDK (java.vendor = "Asymm Systems").
Install it at /opt/eliya and set JAVA_HOME=/opt/eliya.
```

Eliya is installed at `/opt/eliya`. Point `JAVA_HOME` at it for every command in this guide:

```sh
export JAVA_HOME=/opt/eliya
```

Verify:

```sh
/opt/eliya/bin/java -version
# openjdk version "25.0.3" ... Eliya-25.0.3+24
# and: java.vendor = Asymm Systems
```

> Do **not** use `~/.sdkman/candidates/java/current` — that is Temurin and the build will
> reject it. This gate is what makes the Eliya mandate real for canipraynow. (It lives in the
> reactor root POM today; when canipraynow adopts `asymm-build-parent`, task T-06, the same
> rule comes from the parent instead.)

---

## 2. What is in the repo (module map)

The reactor is decomposed by **volatility** (IDesign / Löwy), not by feature. Build order is
top-down: `wiring(-1) → framework(0) → service(1) → application(2)`.

```
$REPO
├── pom.xml                          reactor root (Eliya enforcer lives here)
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
    ├── cipn-app-server/*            (runtime server variants)
    └── cipn-app-batch               the S2 generator entry point (SiteGeneratorApp)
```

The generation pipeline is labelled **S / E / C**:

- **S1** scrape authority sources → XML data (in progress, replaces the old `.mjs` scrapers).
- **S2** read that XML → generate per-city static HTML. **This is what you run** (section 5).
- **E1/E2/E3** the calculation engines (table read, authority angles, general astronomy).
- **C1/C2** the client-side read + the WASM engine.

---

## 3. Building

Full reactor build (compile + test + package), skipping PMD (its remote ruleset 401s):

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
./mvnw verify -Dpmd.skip=true
```

Common variants:

| Goal | Command (run from `$REPO`, with `JAVA_HOME=/opt/eliya`) |
| --- | --- |
| Compile + test + package everything | `./mvnw verify -Dpmd.skip=true` |
| Compile + install to local `~/.m2` | `./mvnw install -Dpmd.skip=true` |
| Fast build, skip tests | `./mvnw verify -Dpmd.skip=true -Dmaven.test.skip=true` |
| Just check the JDK gate passes | `./mvnw validate` |
| Build one module + what it needs | `./mvnw verify -Dpmd.skip=true -pl application/cipn-app-batch -am` |

`verify` runs the `validate` phase first, so the Eliya check happens before any compilation.

---

## 4. Testing

Tests run as part of `verify`. To run them alone:

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw
./mvnw test -Dpmd.skip=true
```

Notable suites:

- **`ArchitectureTest`** (`application/cipn-app-batch`, ArchUnit) — enforces the layering:
  app depends only on wiring/contracts, engines are framework/parser-free (WASM hygiene),
  contracts don't depend on impls, no package cycles. Requires ArchUnit 1.4.1+ (older ASM
  can't read Java 25 bytecode) and Surefire's `useManifestOnlyJar=false` (already configured).
- **`SiteGenerationIntegrationTest`** (`application/cipn-app-batch`) — wires the whole stack
  (assembly → variants → impls → FreeMarker) and generates real pages from the XML fixtures
  under `src/test/resources/gen`. This is the best worked example of a valid `DATA_DIR`.
- **Astronomy parity** (`cipn-svc-engine`) — checks E3 against 2688 golden vectors
  (`golden-vectors.xml`) for exact adhan agreement.

Run a single suite:

```sh
./mvnw test -Dpmd.skip=true -pl application/cipn-app-batch -Dtest=SiteGenerationIntegrationTest
```

---

## 5. Running the S2 generator (per-city page generation)

The generator entry point is **`SiteGeneratorApp`** in `application/cipn-app-batch`. It takes
**two positional arguments**:

```
SiteGeneratorApp <DATA_DIR> <OUTPUT_DIR>
```

### 5.1 What `<DATA_DIR>` means

`DATA_DIR` is a directory that holds the input XML the generator reads. It must contain **two
things**:

1. `cities.xml` — the master list of cities (one `<city>` per row).
2. `timetables/<cc>/` — one sub-directory **per country code** (lower-case, e.g. `lk`, `my`),
   each holding that authority's `zones.xml` plus one `zone-<id>.xml` per zone.

So a valid `DATA_DIR` looks exactly like this:

```
<DATA_DIR>/
├── cities.xml
└── timetables/
    ├── lk/
    │   ├── zones.xml
    │   ├── zone-01.xml
    │   └── zone-07.xml
    └── my/
        ├── zones.xml
        ├── zone-01.xml
        └── ...
```

A country with a `timetables/<cc>/` folder gets an **authority table** baked into its cities'
pages; a country without one (e.g. `GB` in the fixture) gets a page with no baked table.

**Concrete value you can run today:** the integration-test fixture is a complete, valid
`DATA_DIR`:

```
/home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw/application/cipn-app-batch/src/test/resources/gen
```

It contains `cities.xml` (Colombo + London) and `timetables/lk/` (ACJU zones 01 and 07).

> **Live data note (current state):** the *production* city list and timetables still live in
> the JBake site module as **JSON**, not XML:
> `application/cipn-app-site/src/main/jbake/assets/data/cities.json` and
> `.../data/timetables/{lk,my}/`. Converting those to the XML layout above (and retiring the
> old `GenerateCityPages.java`) is the remaining **cutover** work. Until then, the fixture
> directory is the only ready-made `DATA_DIR`; point production runs at the converted XML tree
> once it exists.

### 5.2 What `<OUTPUT_DIR>` means

`OUTPUT_DIR` is where the generator writes the produced HTML. It is created/swept by the run;
pages are written as `<country-slug>/<city-slug>.html`, e.g. `sri-lanka/colombo.html`. Use any
scratch directory you own — nothing else reads from here directly. In the eventual cutover this
becomes the JBake content directory so the pages feed the site build.

**Concrete value:** `/tmp/cipn-out` (or a scratch dir of your choice).

### 5.3 Run it, end to end

```sh
export JAVA_HOME=/opt/eliya
cd /home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw

# 1. Build so the classpath (assembly -> impls) is available.
./mvnw install -Dpmd.skip=true

# 2. Pick concrete values for the two arguments.
DATA_DIR=/home/fahimfarookme/Documents/projects/canipraynow/repository/code/can-i-pray-now-code-pfw/application/cipn-app-batch/src/test/resources/gen
OUTPUT_DIR=/tmp/cipn-out

# 3. Run the generator via the exec plugin (resolves the reactor classpath for you).
./mvnw -q -pl application/cipn-app-batch exec:java \
  -Dexec.mainClass=systems.asymm.cipn.app.batch.SiteGeneratorApp \
  -Dexec.args="$DATA_DIR $OUTPUT_DIR"

# 4. Inspect the result.
find /tmp/cipn-out -type f
cat /tmp/cipn-out/sri-lanka/colombo.html
```

With the fixture `DATA_DIR` you get `sri-lanka/colombo.html` (with the ACJU annual table and
nearest-neighbour links) and `united-kingdom/london.html` (no baked table). Each page also
links its nearest page-cities, computed by the `NeighbourEngine`.

---

## 6. The XML data formats

Everything the generator reads is **XML** (project rule: XML for all config/data/oracles, no
JSON/YAML/properties). Attributes are lower-case kebab-case; a CamelCase id normalizes to its
slug at the boundary.

### 6.1 `cities.xml`

One `<city>` element per city. `page="true"` means "generate a page for this city".

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
| `name` / `country` | display names |
| `cc` | ISO country code (upper-case; maps to `timetables/<cc>/`) |
| `slug` / `country-slug` | URL segments; page path is `<country-slug>/<slug>.html` |
| `lat` / `lng` | coordinates (used for zone snapping and neighbour distance) |
| `tz` | IANA timezone |
| `page` | `true` to generate a page, `false` to skip |

### 6.2 `timetables/<cc>/zones.xml` (the zone index)

Describes the authority and lists its zones (id + representative point). One per country.

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

### 6.3 `timetables/<cc>/zone-<id>.xml` (one authority timetable)

The published table for one zone. `unit="minutes-from-midnight"`; one `<day>` per date. Rows
are sorted by month/day on read.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<timetable authority="ACJU" zone="01" name="Colombo, Gampaha, Kalutara"
           source="https://www.acju.lk/prayer-times/" unit="minutes-from-midnight">
    <day month="1" day="1" fajr="300" sunrise="382" dhuhr="735" asr="937" maghrib="1087" isha="1161"/>
    <day month="1" day="2" fajr="300" sunrise="383" dhuhr="736" asr="937" maghrib="1088" isha="1162"/>
</timetable>
```

(`fajr="300"` = 300 minutes after midnight = 05:00.)

### 6.4 `calculation-methods.xml` (the E2 method registry)

Not part of `DATA_DIR` — it is a **classpath resource** inside `cipn-svc-accessor-file`
(`src/main/resources/calculation-methods.xml`), read by the calculation-method accessor. It
holds each authority's Fajr/Isha angles and offsets; only non-default fields are written.

```xml
<calculation-methods default-method="muslim-world-league">
    <method key="acju" fajr-angle="20" isha-angle="18"/>
    <method key="france" fajr-angle="12" isha-angle="12"/>
    <!-- ... -->
</calculation-methods>
```

---

## 7. Conventions (so a build passes review)

- **Eliya only.** `JAVA_HOME=/opt/eliya`. The reactor rejects any other JDK (section 1).
- **XML for all config/data/oracles.** No JSON, YAML, properties. No node/npm/`.mjs`.
- **Service-type pattern.** A concrete service is a marker interface (`Engine`/`Manager`/
  `Accessor`) + a specific contract interface in `cipn-fw-contract-*`, with a `Default*` impl in
  `cipn-svc-*`, wired externally. No frameworkless concrete class dumped in a service module.
- **Wiring only in the app.** `application/*` depends solely on
  `cipn-wiring-assembly-standard`; it never depends on `svc-*` or `new`s an impl.
- **WASM hygiene.** Keep FreeMarker/Jackson/Spring off `cipn-svc-engine` and off
  `cipn-fw-common`'s engine-reachable path. Templating lives in `cipn-svc-manager` (build-time).
- **Headers.** Every source file starts with
  `Copyright (c) 2026 Asymm Systems. All rights reserved.` + `@author Fahim Farook`.
  (User-facing/legal text — site footer, README — uses the full legal entity
  `Asymm Systems (Pvt) Ltd`.)
- **Every enum constant and record component is documented.**

The `ArchitectureTest` ArchUnit suite mechanically enforces the layering rules above, so a
violation fails `verify` rather than waiting for review.
