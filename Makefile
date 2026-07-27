# Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
#
# Task runner for canipraynow.com. Targets are named for the pipeline STAGE they
# drive (see docs/developer-guide.md section 3 for the lifecycle):
#
#   scrape    (S1) authority tables -> standard XML    (native Java; JAKIM first; T-26)
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
SITE        := application/cipn-app-site
CONTENT     := $(SITE)/src/main/jbake/content
PORT        ?= 8020

# The mandated JDK. Override only if Eliya lives elsewhere; never point at another vendor.
JAVA_HOME   ?= /opt/eliya
export JAVA_HOME

.DEFAULT_GOAL := help
.PHONY: help doctor build test install validate clean \
        scrape generate site dev serve

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

scrape:  ## (S1) Scrape an authority's tables -> classpath XML. NOT YET BUILT (T-26; JAKIM first)
	@echo "S1 (native-Java scraper) is not implemented yet - T-26/F2, JAKIM first."
	@echo "It will write timetable/<year>/<cc>/*.xml + index.xml and XSD-validate. See ADR-0012 section 7."
	@exit 1

## ----------------------------------------------------------------- generate (S2)

generate:  ## (S2) Generate current-year city content into the JBake content dir
	$(MVNW) -pl $(BATCH) -am install $(MVN_FLAGS) -Dmaven.test.skip=true
	$(MVNW) -pl $(BATCH) $(MVN_FLAGS) exec:java \
		-Dexec.mainClass=systems.asymm.cipn.app.batch.SiteGeneratorApp \
		-Dexec.arguments=$(CONTENT),--canonical-only
	@echo "Generated current-year city content into $(CONTENT)"

## ----------------------------------------------------------------- site: JBake bake
##
## Make (not Maven) orchestrates the pipeline: `site` runs `generate` first (the reactor
## writes city content), then JBake bakes it. The two modules stay independent - no Maven
## app->app dependency; Make sequences the stages (ADR-0009 / lifecycle).

site: generate  ## (site) Generate city content, then bake the static site (clean URLs)
	$(MVNW) -Pproduction -pl $(SITE) clean process-resources $(MVN_FLAGS)

dev serve:  ## Live-reload JBake dev server on http://localhost:$(PORT)
	$(MVNW) -pl $(SITE) jbake:inline -Djbake.port=$(PORT) $(MVN_FLAGS)
