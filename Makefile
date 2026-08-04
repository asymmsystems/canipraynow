# Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
#
# Task runner for canipraynow.com. Targets are named for the pipeline STAGE they
# drive (see docs/developer-guide.md section 3 for the lifecycle):
#
#   scrape    (S1) authority tables -> standard XML    (native Java; JAKIM pilot, ADR-0017)
#   generate  (S2) per-city pages   -> HTML            (cipn-app-batch)
#   site           bake the static site                (cipn-app-site, JBake)
#
# Plus whole-reactor build/test targets. Everything runs on the Eliya JDK, which the
# reactor enforces (RequireJavaVendor); no Node anywhere (ADR-0002).
#
# @author Fahim Farook

MVNW        := ./mvnw
MVN_FLAGS   := --batch-mode --no-transfer-progress -Dpmd.skip=true
BATCH       := application/cipn-app-batch
SCRAPE      := application/cipn-app-scrape
SITE        := application/cipn-app-site
WASM        := application/cipn-app-wasm
CONTENT     := $(SITE)/src/main/jbake/content
WASM_ASSETS := $(SITE)/src/main/jbake/assets/wasm
CLASSPATH_RES := service/cipn-svc-accessor/cipn-svc-accessor-classpath/src/main/resources
METHOD_XML  := $(CLASSPATH_RES)/calculation-method.xml
PORT        ?= 8020

# S1 scrape parameters (override on the command line): the country to scrape and the year to
# fetch, e.g. `make scrape CC=my YEAR=2027`. Defaults to Malaysia (JAKIM), current year.
CC          ?= my
YEAR        ?= $(shell date +%Y)

# TeaVM version is read from the reactor POM so the runtime-loader we extract always matches
# the compiler that produced the .wasm. The generic WASM-GC loader ships inside teavm-core.
TEAVM_VERSION := $(shell grep -oP '(?<=<teavm.version>)[^<]+' pom.xml)
TEAVM_CORE  := $(HOME)/.m2/repository/org/teavm/teavm-core/$(TEAVM_VERSION)/teavm-core-$(TEAVM_VERSION).jar

# The mandated JDK. Override only if Eliya lives elsewhere; never point at another vendor.
JAVA_HOME   ?= /opt/eliya
export JAVA_HOME

.DEFAULT_GOAL := help
.PHONY: help doctor build test install validate clean \
        scrape generate wasm site dev serve

help:  ## Show this help
	@grep -E '^[a-zA-Z0-9_ -]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

## ----------------------------------------------------------------- whole reactor

doctor:  ## Verify the active JDK is Eliya (what the build enforces)
	@$(JAVA_HOME)/bin/java -version

build:  ## Compile + test + package the whole reactor (Eliya enforced)
	$(MVNW) verify $(MVN_FLAGS)

test:  ## Run every test suite (ArchUnit, integration, accessors, engine parity)
	$(MVNW) test $(MVN_FLAGS)

install:  ## Install artifacts to ~/.m2 (prerequisite for the generate target)
	$(MVNW) install $(MVN_FLAGS) -Dmaven.test.skip=true

validate:  ## Check only the Eliya JDK enforcer gate
	$(MVNW) validate $(MVN_FLAGS)

clean:  ## Remove all target/ output
	$(MVNW) clean $(MVN_FLAGS)

## ----------------------------------------------------------------- scrape (S1)

scrape:  ## (S1) Scrape an authority (CC=my YEAR=2027) -> classpath timetable XML + golden-diff
	$(MVNW) -pl $(SCRAPE) -am install $(MVN_FLAGS) -Dmaven.test.skip=true
	$(MVNW) -pl $(SCRAPE) $(MVN_FLAGS) exec:java \
		-Dexec.arguments=$(CC),$(YEAR),$(CLASSPATH_RES)/timetable/$(YEAR)
	@echo "Scraped $(CC) $(YEAR) into $(CLASSPATH_RES)/timetable/$(YEAR)/$(CC). Review the golden-diff, then commit the XML."

## ----------------------------------------------------------------- generate (S2)

generate:  ## (S2) Generate current-year city content into the JBake content dir
	$(MVNW) -pl $(BATCH) -am install $(MVN_FLAGS) -Dmaven.test.skip=true
	$(MVNW) -pl $(BATCH) $(MVN_FLAGS) exec:java \
		-Dexec.mainClass=systems.asymm.cipn.app.batch.SiteGeneratorApp \
		-Dexec.arguments=$(CONTENT),--canonical-only
	@echo "Generated current-year city content into $(CONTENT)"

## ----------------------------------------------------------------- wasm: browser engine
##
## Compile the engine + XML parser to WebAssembly-GC (TeaVM, behind the `wasm` profile) and
## stage the .wasm + its generic runtime loader into the site's assets, so JBake bakes them.
## The client loads them only for the fallback (off-grid / non-default method); the common
## covered-city path reads the baked table and never touches WASM (ADR-0009, ADR-0015).

wasm:  ## (wasm) Compile the browser engine to WASM-GC and stage it into the site assets
	$(MVNW) -pl $(WASM) -am -Pwasm process-classes $(MVN_FLAGS) -Dmaven.test.skip=true
	@mkdir -p $(WASM_ASSETS)
	cp $(WASM)/target/wasm/classes.wasm $(WASM_ASSETS)/prayer-times.wasm
	unzip -o -j "$(TEAVM_CORE)" 'org/teavm/backend/wasm/wasm-gc-runtime.min.js' -d $(WASM_ASSETS)
	cp $(METHOD_XML) $(WASM_ASSETS)/calculation-method.xml
	@echo "Staged prayer-times.wasm + wasm-gc-runtime.min.js + calculation-method.xml into $(WASM_ASSETS)"

## ----------------------------------------------------------------- site: JBake bake
##
## Make (not Maven) orchestrates the pipeline: `site` runs `generate` + `wasm` first (the
## reactor writes city content and the browser engine), then JBake bakes it all. The modules
## stay independent - no Maven app->app dependency; Make sequences the stages (ADR-0009/0014).

site: generate wasm  ## (site) Generate city content + WASM engine, then bake the static site
	$(MVNW) -Pproduction -pl $(SITE) clean process-resources $(MVN_FLAGS)

dev serve: wasm  ## Live-reload JBake dev server on http://localhost:$(PORT)
	$(MVNW) -pl $(SITE) jbake:inline -Djbake.port=$(PORT) $(MVN_FLAGS)
