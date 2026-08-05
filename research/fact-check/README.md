# canipraynow.com — fact-check reports (2026-07-19)

Independent verification pass of every fiqh and technical claim the product makes, against
primary sources (sunnah.com, quran.com, islamqa.info, praytimes.org, aladhan API, arabeyes ITL,
each authority's own site). Verification only — **no code or content edits were made**.
Every source cited was actually opened; unverified items are marked UNVERIFIABLE, not CONFIRMED.

## Start here

- **[00-consolidated-report.md](00-consolidated-report.md)** — the single merged report: findings
  table (INCORRECT / NEEDS-SCHOLAR ranked first), CONFIRMED core, repo-integrity findings, and
  "what a scholar must still decide."

## Per-domain source reports

| # | Report | Scope |
|---|---|---|
| 01 | [hadith-citations.md](01-hadith-citations.md) | All 7 hadith on sunnah.com — collection, number, grade, narrator, text |
| 02 | [fiqh-verdicts.md](02-fiqh-verdicts.md) | Qur'an 4:103, IslamQA refs, al-Albānī, and defensibility of every Evidence/Ḥanafī/Shāfiʿī cell |
| 03 | [calculation-angles.md](03-calculation-angles.md) | All 16 method Fajr/Isha angles + sun-times invariant + high-latitude claim |
| 04 | [country-authority-method.md](04-country-authority-method.md) | Per-country authority + method mapping; the four flagged corrections |
| 05 | [authorities-real-current.md](05-authorities-real-current.md) | ACJU, JAKIM, MUIS, Kemenag, Diyanet, Qatar Calendar House, Egyptian Survey Authority — real/current/named |
| 06 | [gulf-umm-al-qura.md](06-gulf-umm-al-qura.md) | Saudi / Bahrain / Oman / Yemen → Umm al-Qura deep-dive |
| 07 | [uae-kuwait-qatar.md](07-uae-kuwait-qatar.md) | UAE (Dubai preset) / Kuwait / Qatar deep-dive |
| 08 | [egypt-iran-azerbaijan.md](08-egypt-iran-azerbaijan.md) | Egypt / Iran / Azerbaijan deep-dive |
| 09 | [south-asia-karachi.md](09-south-asia-karachi.md) | Pakistan / India / Bangladesh / Afghanistan (Karachi 18/18) |
| 10 | [se-asia-north-america.md](10-se-asia-north-america.md) | Indonesia / Malaysia / USA / Canada |

## The four hard errors to fix first

1. **Azerbaijan → Tehran** should be **MWL** (imports Jafari delayed-Maghrib) — `country.xml` AZ row
2. **Malaysia method prose** "the MUIS (Singapore) convention" should name **JAKIM** (times unaffected, attribution wrong)
3. **Ḥanafī `anchor_convention` basis note** — after-Fajr bar is time-based, not performance-based — `hanafi.json`
4. **"Egyptian General Authority of Survey"** → **Egyptian Survey Authority**

Plus: scope the Evidence-basis strict-window causal-nafl cells (F1) for the reviewing scholar, and
reconcile the `research/*.md` evidence files the data cites (R1).

> Note (2026-08-05 reconstruction): these 12 reports were originally written under a pre-reactor
> branch layout and were lost when the working tree moved to `develop`. They are reproduced here
> verbatim on `feature/authority-register-t01-t27` so the authority-register work has its cited
> starting point. Content is unchanged from the original verification pass.
