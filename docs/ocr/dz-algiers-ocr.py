#!/usr/bin/env python3
# Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
# @author Fahim Farook
#
# Off-build OCR pass for the DZ (MARW, Algiers) served table (see docs/annual-refresh-runbook.md).
# The Ministry publishes image-only PDFs (no text layer), so there is no live Java scraper: this
# reproduces the committed zone-Algiers.xml deterministically. A real OCR engine is used, never a
# summarising model.
#
# Pipeline:
#   1. Render each Hijri-year PDF (1447 + 1448 AH) to a raster with PDFBox (Java; see the runbook).
#   2. OCR each page with rapidocr-onnxruntime, keeping only date + HH:MM tokens with their (x, y).
#   3. Reconstruct rows by column position: each row's Gregorian date (printed on the sheet) plus the
#      six prayer columns. The Qibla column is empty (//://) in winter, so columns are assigned by
#      x-cluster, not a fixed token count.
#   4. Validate (do this before committing): 365 days, fajr<sunrise<dhuhr<asr<maghrib<isha, small
#      day-over-day deltas (Algeria has no DST), solar-noon symmetry, and a cross-check against an
#      independent read. Isha on Feb 14-18 and Apr 13-18 is a faithful source anomaly - keep as-is.
#
# Prereqs: pip install --user rapidocr-onnxruntime ; PNGs rendered to /tmp/dz1447-p*.png, dz1448-p*.png
# Usage:   python3 dz-algiers-ocr.py  # -> dz_rows.json { "MM-DD": [fajr,sunrise,dhuhr,asr,maghrib,isha] }

import glob
import json
import re
from datetime import date, timedelta

from rapidocr_onnxruntime import RapidOCR

DATE = re.compile(r'2026[/\-.،](\d{1,2})[/\-.،](\d{1,2})')
TIME = re.compile(r'^(\d{1,2})[:hH.](\d{2})$')


def cluster_x(xs, gap=40):
    """Group x-coordinates into columns (a gap wider than `gap` starts a new column)."""
    xs = sorted(xs)
    cols = [[xs[0]]]
    for v in xs[1:]:
        (cols.append([v]) if v - cols[-1][-1] > gap else cols[-1].append(v))
    return [sum(c) / len(c) for c in cols]


def reconstruct():
    engine = RapidOCR()
    rows = {}
    for page in sorted(glob.glob('/tmp/dz1447-p*.png')) + sorted(glob.glob('/tmp/dz1448-p*.png')):
        result, _ = engine(page)
        if not result:
            continue
        tokens = [(sum(p[0] for p in box) / 4, sum(p[1] for p in box) / 4, text.strip())
                  for box, text, _ in result]
        dates = [(x, y, int(m.group(1)), int(m.group(2)))
                 for x, y, t in tokens for m in [DATE.search(t.replace(' ', ''))] if m]
        times = [(x, y, int(m.group(1)) * 60 + int(m.group(2)))
                 for x, y, t in tokens for m in [TIME.match(t.replace(' ', ''))] if m]
        if len(times) < 15 or not dates:
            continue
        centers = sorted(cluster_x([x for x, _, _ in times]), reverse=True)  # x-desc, RTL
        if len(centers) >= 7:
            centers, prayer = centers[:7], [0, 1, 3, 4, 5, 6]  # drop Qibla (index 2)
        elif len(centers) == 6:
            prayer = [0, 1, 2, 3, 4, 5]                        # no Qibla column on this page
        else:
            continue
        for dx, dy, month, day in dates:
            col = [None] * len(centers)
            for x, y, v in times:
                if abs(y - dy) > 14 or x > dx - 20:
                    continue
                i = min(range(len(centers)), key=lambda k: abs(x - centers[k]))
                if abs(x - centers[i]) < 35 and col[i] is None:
                    col[i] = v
            vals = [col[i] for i in prayer]
            if any(v is None for v in vals) or not vals == sorted(vals):
                continue
            rows.setdefault(f'{month:02d}-{day:02d}', vals)
    return rows


if __name__ == '__main__':
    rows = reconstruct()
    json.dump(rows, open('dz_rows.json', 'w'))
    missing, d = [], date(2026, 1, 1)
    while d.year == 2026:
        if f'{d.month:02d}-{d.day:02d}' not in rows:
            missing.append(f'{d.month:02d}-{d.day:02d}')
        d += timedelta(days=1)
    print(f'reconstructed {len(rows)}/365; missing {missing}')
