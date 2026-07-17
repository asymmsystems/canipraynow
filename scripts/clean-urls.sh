#!/bin/sh
# clean-urls.sh — Production-only clean URL transform for Cloudflare Pages
#
# Strips .html from all internal href links in generated HTML files.
# Cloudflare Pages natively serves /page -> page.html, so no directory
# restructuring is needed.
#
# Source files keep .html in hrefs so jbake:inline dev server works as-is.
# This script runs only when the "production" Maven profile is active:
#   ./mvnw -Pproduction clean process-resources
# Default builds (./mvnw clean process-resources, no profile) keep .html so
# target/site/ is browsable with a plain local file server.
#
# Usage: ./scripts/clean-urls.sh [output-dir]
#   Default output-dir: target/site

set -e

SITE_DIR="${1:-target/site}"

if [ ! -d "$SITE_DIR" ]; then
    echo "Error: Site directory '$SITE_DIR' not found. Run jbake:generate first."
    exit 1
fi

echo "=== Clean URLs: stripping .html from href links ==="

# Order matters. index.html resolves to its directory, not to a page named
# "index": page/sitemap.ftl and partial/header.ftl both strip the whole
# segment, so <loc> and rel=canonical say "/" and "/q/". Internal links have
# to name that same URL or every link is a hop to a non-canonical one.
find "$SITE_DIR" -name '*.html' -exec sed -i \
    -e 's|href="\(/[^"]*/\)index\.html"|href="\1"|g' \
    -e 's|href="/index\.html"|href="/"|g' \
    -e 's|href="\(/[^"]*\)\.html"|href="\1"|g' {} +
echo "=== Done ==="
