#!/usr/bin/env python3
"""Generate the prayer-time source coverage report.

Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

Answers one question for every country on earth: where does canipraynow.com
get its prayer times, and where are the gaps?

DERIVED, not hand-written: it reads the same cities.json and method-defaults.json
the app ships, so the report cannot quietly drift from what the product does.
A hand-maintained table would be wrong within a week.

NOT part of the Maven build - it documents the build, it does not feed it.
Run it when the dataset or the authority research changes:

    python3 scripts/coverage-report.py > ../../../research/prayer-time-coverage.md

@author Fahim Farook
"""

import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
DATA = HERE.parent / "src/main/jbake/assets/data"
ISO = HERE / "iso-3166.json"          # committed snapshot; see README in research/

# Official authority per ISO2, from the country research (sourced). Rich schema:
# {name, url, has_authority, feed, granularity, convention, redistributable,
#  confidence, note}. A cc absent from this map = NOT YET RESEARCHED.
_AUTH_DOC = json.loads((HERE / "authorities.json").read_text()) \
    if (HERE / "authorities.json").exists() else {}
AUTHORITIES = _AUTH_DOC.get("authorities", {})

# Can we serve this country's times EXACTLY as its authority publishes them?
# The answer is governed by the feed, not by whether an authority exists:
#   api   -> obtainable programmatically (still subject to terms)
#   html  -> scrapeable, brittle
#   pdf   -> parseable per-cycle (like ACJU), heavy
#   image -> needs OCR, error-prone
#   app   -> not obtainable without reverse-engineering
#   none  -> no authority feed at all
# "exactly as authority" is realistic only for api/pdf/html; image/app/none must
# fall back to computed. This is the classification the product decision needs.
FEED_SOURCEABLE = {"api": "yes", "pdf": "hard", "html": "hard",
                   "image": "no", "app": "no", "none": "no"}

# Which madhhab the shipped rule matrices cover. Anything else is a real gap:
# the app currently substitutes the nearest shipped matrix.
SHIPPED_MATRICES = {"hanafi", "shafii"}


def main() -> None:
    cities = json.loads((DATA / "cities.json").read_text())["cities"]
    md = json.loads((DATA / "method-defaults.json").read_text())
    iso = json.loads(ISO.read_text())

    by_country = md["byCountry"]
    predominant_by = md.get("predominantMadhhab", {})
    default_method = md["default"]
    # The app now defaults EVERYONE to the evidence-based ruleset; there is no
    # per-country madhhab selection, so there is no "madhhab fallback" any more.
    DEFAULT_RULESET = "evidence"

    # Roll our own dataset up per country.
    shipped: dict[str, dict] = {}
    for c in cities:
        e = shipped.setdefault(c["cc"], {"name": c["country"], "cities": 0, "pages": 0})
        e["cities"] += 1
        if c.get("page"):
            e["pages"] += 1

    # Codes used by cities.json that ISO-3166 does not list (e.g. XK Kosovo,
    # a user-assigned code). Appended as extra rows so no city is silently
    # dropped from the counts - a miscount here reads as coverage we do not have.
    iso_codes = {e["alpha-2"] for e in iso}
    extra = [{"alpha-2": cc, "name": shipped[cc]["name"], "region": ""}
             for cc in sorted(shipped) if cc not in iso_codes]

    rows = []
    for entry in iso + extra:
        cc = entry["alpha-2"]
        name = entry["name"]
        s = shipped.get(cc)
        method = by_country.get(cc)
        predominant = predominant_by.get(cc)
        auth = AUTHORITIES.get(cc)

        # What the app ACTUALLY computes with, vs what was explicitly chosen.
        method_used = method or default_method
        method_src = "explicit" if method else "fallback"

        rows.append({
            "cc": cc, "name": name, "region": entry.get("region") or "",
            "cities": s["cities"] if s else 0,
            "pages": s["pages"] if s else 0,
            "method": method_used, "method_src": method_src,
            "ruleset": DEFAULT_RULESET,
            "predominant": predominant,
            "authority": auth,
        })

    emit(rows, len(cities))


def source_of(r: dict) -> str:
    """How the times for this country are actually obtained today."""
    if r["method"] == "ACJU":
        return "fitted-to-authority"
    return "computed"


def sourceable(r: dict) -> str:
    """Can we serve this country EXACTLY as its authority publishes? yes/hard/no/?"""
    a = r["authority"]
    if a is None:
        return "?"                      # not researched
    if not a.get("has_authority"):
        return "no"                     # no authority to be exact to
    return FEED_SOURCEABLE.get(a.get("feed"), "?")


def gaps_for(r: dict) -> list[str]:
    g = []
    if r["cities"] == 0:
        g.append("no city in dataset")
    if r["method_src"] == "fallback":
        g.append("no method default")
    # Informational only now: the app serves the evidence-based ruling to
    # everyone, so a non-shipped predominant school is a note, not a fallback bug.
    if r["predominant"] and r["predominant"] not in SHIPPED_MATRICES:
        g.append(f"predominant school {r['predominant']} (not shipped; default ruling applies)")
    if r["authority"] is None:
        g.append("authority not researched")
    return g


def emit(rows: list[dict], total_cities: int) -> None:
    covered = [r for r in rows if r["cities"] > 0]
    out = print

    out("# Prayer-time sources and coverage")
    out("")
    out("**Generated** by `scripts/coverage-report.py` from the shipping "
        "`cities.json` and `method-defaults.json`. Do not hand-edit: regenerate it.")
    out("")
    out("## What this answers")
    out("")
    out("For every country on earth: where canipraynow.com gets its prayer times "
        "today, and what is missing.")
    out("")
    out("### Source types")
    out("")
    out("| Type | Meaning | Countries today |")
    out("|---|---|---|")
    fitted = [r for r in rows if source_of(r) == "fitted-to-authority"]
    computed = [r for r in covered if source_of(r) == "computed"]
    out(f"| `extracted-official` | The authority's own published timetable, parsed and baked. "
        f"**Nothing uses this yet.** | 0 |")
    out(f"| `fitted-to-authority` | Adhan.js with angles fitted to the authority's published "
        f"tables, with the residual error measured. | {len(fitted)} "
        f"({', '.join(r['cc'] for r in fitted) or 'none'}) |")
    out(f"| `computed` | Adhan.js with a standard method preset. Agrees with the local "
        f"authority only by coincidence. | {len(computed)} |")
    out("")

    # The load-bearing question: for how many countries is "serve exactly as the
    # authority" even physically possible? Governed by the feed, not by whether
    # an authority exists.
    researched = [r for r in rows if r["authority"] is not None]
    by_src = {"yes": [], "hard": [], "no": []}
    for r in researched:
        s = sourceable(r)
        if s in by_src:
            by_src[s].append(r)
    out("### Can we serve times *exactly as the authority*?")
    out("")
    out(f"Of **{len(researched)}** countries researched so far:")
    out("")
    out(f"- **{len(by_src['yes'])} obtainable via an official API** "
        f"({', '.join(r['cc'] for r in by_src['yes']) or 'none'}) - the only clean case.")
    out(f"- **{len(by_src['hard'])} obtainable but hard** (PDF/HTML to parse each cycle): "
        f"{', '.join(r['cc'] for r in by_src['hard']) or 'none'}.")
    out(f"- **{len(by_src['no'])} not obtainable** (image/app-only, or no authority at all) - "
        "these must fall back to a computed method.")
    out("")
    out("**And redistribution is not granted anywhere verified** - every authority's terms are "
        "silent or restrictive. Caching and re-serving official times needs written permission "
        "per authority. So \"exactly as authority, everywhere\" is not achievable: it is realistic "
        "for a handful (API/PDF, permission granted), and the rest fall back - which is the "
        "design the product already implements.")
    out("")
    out("### Headline numbers")
    out("")
    out(f"- **{total_cities}** cities in the dataset, **{sum(r['pages'] for r in rows)}** with pages")
    out(f"- **{len(covered)}** of {len(rows)} ISO-3166 countries have at least one city")
    out(f"- **{len(researched)}** countries researched for an authority; "
        f"**{len([r for r in researched if r['authority'].get('has_authority')])}** have one, "
        f"**{len([r for r in researched if not r['authority'].get('has_authority')])}** have none")
    out(f"- Every country now defaults to the **evidence-based** ruling; "
        f"the per-country madhhab fallback that used to affect "
        f"{len([r for r in rows if r['predominant'] and r['cities'] > 0])} countries is gone")
    out("")

    out("## Coverage by country")
    out("")
    out("`method`/`madhhab` marked *(fallback)* were never chosen for that country - "
        "the app is using a global default. That is a guess, not a decision.")
    out("")
    out("| Country | ISO2 | Cities | Pages | Method | Ruling | Authority | Feed | Exact? | Gaps |")
    out("|---|---|---:|---:|---|---|---|---|:--:|---|")
    exact_glyph = {"yes": "✅", "hard": "🟠", "no": "❌", "?": "—"}
    for r in sorted(rows, key=lambda x: (-x["cities"], x["name"])):
        auth = r["authority"]
        if auth is None:
            auth_cell, feed_cell = "_not researched_", "—"
        elif not auth.get("has_authority"):
            auth_cell, feed_cell = "_no national authority_", "none"
        else:
            auth_cell, feed_cell = auth["name"], auth.get("feed", "?")
        m = r["method"] + ("" if r["method_src"] == "explicit" else " _(fb)_")
        gaps = gaps_for(r)
        out(f"| {r['name']} | {r['cc']} | {r['cities']} | {r['pages']} | "
            f"{m} | {r['ruleset']} | {auth_cell} | {feed_cell} | {exact_glyph[sourceable(r)]} | "
            f"{'; '.join(gaps) if gaps else '-'} |")
    out("")
    out("*Exact? = can we serve this country's times exactly as its authority publishes them: "
        "✅ official API · 🟠 obtainable but must parse PDF/HTML each cycle · ❌ image/app-only "
        "or no authority (fall back to computed) · — not yet researched.*")
    out("")

    out("## Non-coverage")
    out("")
    nocity = [r for r in rows if r["cities"] == 0]
    out(f"### {len(nocity)} countries with no city in the dataset")
    out("")
    out("A visitor here still gets a correct answer via GPS - the engine works at any "
        "coordinates - but there is no city page, and the picker cannot find them.")
    out("")
    out(", ".join(f"{r['name']} ({r['cc']})" for r in sorted(nocity, key=lambda x: x["name"])))
    out("")
    nomethod = [r for r in rows if r["method_src"] == "fallback" and r["cities"] > 0]
    out(f"### {len(nomethod)} countries silently falling back on calculation method")
    out("")
    out("These compute with Muslim World League because nobody chose otherwise.")
    out("")
    out(", ".join(f"{r['name']} ({r['cc']})" for r in sorted(nomethod, key=lambda x: x["name"])))
    out("")
    unshipped = [r for r in rows if r["cities"] > 0 and r["predominant"]
                 and r["predominant"] not in SHIPPED_MATRICES]
    out(f"### {len(unshipped)} countries whose predominant school does not ship")
    out("")
    out("These are **disclosed in the settings drawer**, not silently substituted: the app "
        "tells the user which school it is actually showing them. Shipping these matrices "
        "needs scholar review, the same gate as the existing two.")
    out("")
    for school in sorted({r["predominant"] for r in unshipped}):
        names = ", ".join(f"{r['name']} ({r['cc']})" for r in sorted(unshipped, key=lambda x: x["name"])
                          if r["predominant"] == school)
        out(f"- **{school}** - {names}")
    out("")
    unshipped = sorted({r["predominant"] for r in rows
                        if r["predominant"] and r["predominant"] not in SHIPPED_MATRICES})
    out("### Predominant schools we do not ship (informational)")
    out("")
    out("The app serves the **evidence-based** ruling to everyone, so these are not fallbacks or "
        "errors - just a note that a user in these countries may follow a school whose dedicated "
        "matrix is not yet built. Choosing Ḥanafī or Shāfiʿī is always available.")
    out("")
    for school in unshipped:
        names = ", ".join(f"{r['name']} ({r['cc']})" for r in sorted(rows, key=lambda x: x["name"])
                          if r["predominant"] == school)
        out(f"- **{school}** - {names}")
    out("")
    out("### Madhhab matrices not shipped")
    out("")
    out(f"Shipped: {', '.join(sorted(SHIPPED_MATRICES))}. **Mālikī and Ḥanbalī are not.** "
        "Users in those countries are served the nearest shipped matrix, which is a "
        "substitution the UI does not currently disclose.")
    out("")


if __name__ == "__main__":
    main()
