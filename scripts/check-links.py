#!/usr/bin/env python3
"""Fail the build report if any internal href 404s.

Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

Cloudflare Pages serves /page -> page.html, so a link is valid if either the
literal path or path+".html" exists. Catches the orphan/typo class of bug that
a static site otherwise ships silently.

@author Fahim Farook
"""
import pathlib
import re
import sys

site = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "target/site")
bad = set()
for f in site.rglob("*.html"):
    for href in re.findall(r'href="(/[^"#?]*)"', f.read_text(errors="ignore")):
        p = site / href.lstrip("/")
        if p.is_dir() or p.exists() or site.joinpath(href.lstrip("/") + ".html").exists():
            continue
        bad.add(href)

if bad:
    print(f"broken internal links: {len(bad)}")
    for b in sorted(bad):
        print("  MISSING:", b)
    sys.exit(1)
print("links:  0 broken")
