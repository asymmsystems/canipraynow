# Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
#
# Task runner for canipraynow.com.
#
# Two kinds of target live here, and the difference matters:
#
#   BUILD targets wrap Maven. They are hermetic - no network beyond Maven's own
#   dependency resolution, reproducible, safe in CI.
#
#   DATA targets (timetables, coverage) reach out to third-party websites and
#   REWRITE COMMITTED DATA. They are deliberately NOT wired into the build: a
#   build that depends on acju.lk being up is a build that fails when someone
#   else redesigns their website. Run them by hand, review the diff, commit.
#
# @author Fahim Farook

MVNW        := ./mvnw
MVN_FLAGS   := --batch-mode --no-transfer-progress
SITE        := target/site
PORT        ?= 8020
RESEARCH    := ../../../research

# Maven needs a JDK on PATH; SDKMAN's "current" symlink is the project convention.
JAVA_HOME   ?= $(HOME)/.sdkman/candidates/java/current
export JAVA_HOME

.DEFAULT_GOAL := help
.PHONY: help build dev serve clean verify test lint check \
        timetables coverage data release

## ----------------------------------------------------------------- build

help:  ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

build:  ## Dev build -> target/site (keeps .html links, browsable locally)
	$(MVNW) clean process-resources $(MVN_FLAGS)

release:  ## Production build (strips .html for Cloudflare clean URLs)
	$(MVNW) -Pproduction clean process-resources $(MVN_FLAGS)

dev serve:  ## Live-reload dev server on http://localhost:$(PORT)
	$(MVNW) jbake:inline -Djbake.port=$(PORT) $(MVN_FLAGS)

clean:  ## Remove target/ and the generated city stubs
	$(MVNW) clean $(MVN_FLAGS)

## ----------------------------------------------------------------- checks

test:  ## Run the fiqh engine suite (needs node on PATH)
	@command -v node >/dev/null || { echo "node not found - see scripts/README"; exit 1; }
	@node scripts/test-engine.mjs

lint:  ## Syntax-check every JS file
	@command -v node >/dev/null || { echo "node not found"; exit 1; }
	@find src/main/jbake/assets/js -name '*.js' -exec node --check {} \; && echo "js: clean"

check: lint test release  ## Everything CI runs, in CI's order

verify: release  ## Build, then report budgets and broken internal links
	@echo "pages:  $$(find $(SITE) -name '*.html' | wc -l)"
	@echo "js gz:  $$(for f in $$(find $(SITE)/js -name '*.js'); do gzip -c $$f; done | wc -c) B"
	@echo "css gz: $$(gzip -c $(SITE)/css/style.css | wc -c) B"
	@python3 scripts/check-links.py $(SITE)

## ----------------------------------------------------------------- data
##
## These hit the network and rewrite committed data. Never run by the build.

timetables:  ## Fetch + parse an authority's timetable -> assets/data/timetables/ (yearly). AUTH=lk (default) | my | --list
	@echo "Fetching official timetables. This hits third-party sites and rewrites"
	@echo "committed data - review the diff before committing."
	@if [ "$(AUTH)" = "lk" ] || [ -z "$(AUTH)" ]; then \
		node scripts/fetch-timetables.mjs; \
	else \
		node scripts/fetch-authority.mjs $(AUTH); \
	fi

authorities:  ## List every authority and its scrape status (implemented / blocked)
	@node scripts/fetch-authority.mjs --list

coverage:  ## Regenerate the country coverage report
	@python3 scripts/coverage-report.py > $(RESEARCH)/prayer-time-coverage.md
	@echo "wrote $(RESEARCH)/prayer-time-coverage.md"

data: timetables coverage  ## All data refreshes (yearly-ish)
