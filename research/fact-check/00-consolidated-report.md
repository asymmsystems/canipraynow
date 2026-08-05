# canipraynow.com — independent fact-check report

Verification pass, no code edits. Every hadith, Qur'an, IslamQA, calculation-angle, and
country-mapping claim was checked against a primary or reputable source that was actually
opened; a citation merely existing was never accepted as proof. Sources are cited as URLs
throughout. Verdicts: CONFIRMED / INCORRECT / OVERSIMPLIFIED / UNVERIFIABLE / NEEDS-SCHOLAR.

## Bottom line

- **The evidentiary core is sound.** All 7 hadith resolve to the correct collection, number,
  authentic grade, and supporting text. Qur'an 4:103 and all 4 IslamQA references check out.
  No shipped verdict cell is flatly indefensible for its own school.
- **Nothing fabricates certainty** in the fiqh: every unresolved boundary parameter carries
  `verified_by: null` + a "scholar to set" note, and `verified_at` is null site-wide.
- **The real exposures are narrow:** one over-reached set of Evidence-basis cells (causal nafl
  inside the strict solar windows), one wrong Ḥanafī rationale note, and a handful of
  country/authority mislabels (Azerbaijan, Malaysia, UAE, "Egyptian Survey Authority").

## Findings — INCORRECT and NEEDS-SCHOLAR first

| # | Claim | Location | Verdict | Source opened | Correction / note |
|---|---|---|---|---|---|
| F1 | Causal nafl PERMITTED inside the three strict solar windows, attributed to Ibn Bāz/Ibn ʿUthaymīn | evidence.json SUNRISE/ZENITH/SUNSET causalNafl | NEEDS-SCHOLAR | islamqa.info/en/answers/112114, /48998 | Cited sources cover only the extended after-Fajr/after-Asr windows; zenith + literal setting not covered. Keep-vs-downgrade is a scholar's call. Biggest over-reach. |
| F2 | Azerbaijan → Tehran (Jafari, Maghrib 4.5°) | method-defaults.json AZ | INCORRECT | islamicfinder.org/world/azerbaijan | CMB discloses no angle; third-party consensus uses MWL. Tehran also imports delayed 4.5° Maghrib. → MWL. |
| F3 | Ḥanafī "basis is after performing Fajr/Asr" | hanafi.json anchor_convention.basis | INCORRECT (note) | islamqa.org/hanafi/…/88210 | Ḥanafī after-Fajr bar is time-based; only after-Asr is performance-based. Note half-wrong. |
| F4 | anchor="prayer-time-entry" though bar attaches to having prayed | evidence/shafii anchor_convention | NEEDS-SCHOLAR | islamqa.info/en/answers/136695 | Over-prohibits causal nafl in adhān→prayer gap. Genuine ijtihād call. |
| F5 | Malaysia method = "MUIS (Singapore) convention" | methods.html / method-defaults MY | INCORRECT (attribution) | e-solat.gov.my | Authority is JAKIM. Angles identical (20/18) so times unaffected; label wrong. |
| F6 | UAE "Dubai" 18.2/18.2 as UAE's method | methods.html / calc-methods | OVERSIMPLIFIED→INCORRECT | api.aladhan.com, arabeyes ITL | "Dubai" 18.2 is an unofficial Batoul-Apps fit; official GAIAE/Awqaf = Gulf 19.5° + 90-min. |
| F7 | "Egyptian General Authority of Survey" | methods.html / calc-methods | INCORRECT (naming) | mwri.gov.eg | Correct: Egyptian Survey Authority. Angle right. |
| F8 | Bahrain/Oman/Yemen → Umm al-Qura | method-defaults | OVERSIMPLIFIED | api.aladhan.com, islamicfinder Oman | Gulf model is 19.5°, not 18.5°. Oman runs own MARA; Yemen unverifiable. |
| F9 | Levant → MWL | method-defaults | OVERSIMPLIFIED | arabeyes ITL | Off-UmmAlQura correct; region often 20/18; Jordan has own 18/18 Awqaf method. |
| F10 | Central Asia → MWL | method-defaults | OVERSIMPLIFIED | islamicfinder Uzbekistan | Regional practice leans Karachi 18/18. |
| F11 | Narrator "Umm Salamah" for after-Asr hadith | evidence.json prayer-after-asr-umm-salamah | OVERSIMPLIFIED (caveat) | sunnah.com/bukhari:1233, /muslim:834 | sunnah.com bylines both "Narrated Kuraib." Matn is Umm Salamah's (defensible), but field mismatches. |
| F12 | "Ṣaḥīḥ (al-Albānī)" across all three ṭawāf refs | evidence.json makkah-tawaf-any-hour | OVERSIMPLIFIED (grade) | sunnah.com abudawud:1894, tirmidhi:868, ibnmajah:1254 | Only Abū Dāwūd is al-Albānī; Tirmidhī + Ibn Mājah are "Ṣaḥīḥ (Darussalam)." |
| F13 | "sunrise, zenith and sunset — conventions do not change" | methodology.html | OVERSIMPLIFIED | code astronomy.js, windows.js | True 15/16 methods; Tehran/Jafari delays Maghrib to 4.5°, so sunset window ≠ true sunset there. |
| F14 | P_sunrise 12-20, P_zenith 5-15, P_isfirar 20-40 min | all rule_sets params | UNVERIFIABLE / NEEDS-SCHOLAR | (no primary text) | Conventional intervals; each basis already says "scholar to set." |
| F15 | Shāfiʿī isfirār "attaches to the setting itself" | shafii.json P_isfirar.basis | OVERSIMPLIFIED | islamqa.info/48998, seekersguidance | Shāfiʿī recognizes a distinct yellowing window (Reliance f.13). |

## CONFIRMED (the core)

- **Hadith:** all 7 verified verbatim on sunnah.com — Bukhārī 586+Muslim 827, Muslim 831 (names burial + 3 moments), Bukhārī 597+Muslim 684a, Bukhārī 579+Muslim 608a, Bukhārī 1233+Muslim 834, Abū Dāwūd 1894 (Ṣaḥīḥ al-Albānī), Bukhārī 931+Muslim 875a.
- **Qur'an 4:103** (Saheeh Int'l verbatim); **IslamQA 112114/81978/20013/136695** all support their claims; **al-Albānī, Aḥkām al-Janāʾiz** confirmed.
- **Fiqh cells:** Ḥanafī (qaḍāʾ invalid in strict times; Fajr-sunnah exception; janāzah ready-beforehand distinction; nafl makrūh taḥrīmī) and Shāfiʿī (cause-based validity; Friday-zenith + Makkah-Ḥaram exceptions) match their schools.
- **Calculation:** all 16 angles match both the vendored Adhan.js code and praytimes.org / aladhan API / adhan spec. Three-sun-times invariant holds (zenith from independent NOAA solar-noon). Sub-Saharan Africa→MWL, France→UOIF, Russia→SAMR corrections hold. Authorities ACJU/JAKIM/MUIS/Kemenag/Diyanet/Qatar Calendar House real and correctly named.
- **Disclosed simplifications** (site flags "extract later"): MoonsightingCommittee "18/18" is a nominal label for a seasonal algorithm; ACJU 20/18 is a reverse-engineered fit; Bangladesh/Turkey/Indonesia/Balkans/Nordics/Morocco publish deviating tables. High-latitude "no verdict" is astronomically correct and honestly framed as a deliberate conservative choice.

## Repo integrity

- **R1 — UNVERIFIABLE (in code repo):** `research/prayer-time-authorities.md`, `salafi-awqat-al-nahy.md`, `prayer-time-coverage.md` cited but not in the code repo (the authorities prose lives in the project tree `canipraynow/research/`). Reconcile locations.
- **R2 — UNVERIFIABLE (in-repo):** the "ACJU Colombo agreement within 1-2 min" fit claim rests on the R1 tables.

## What a scholar still must decide

1. Causal nafl inside the strict solar windows (F1) — the single most over-reached decision.
2. Anchor convention time-entry vs having-performed, plus the Ḥanafī W1/W4 asymmetry (F3, F4).
3. Exact boundary minutes P_sunrise/P_zenith/P_isfirar (F14).
4. Umm Salamah hadith framing — Ḥanafīs read it as khuṣūṣiyya (F11).
5. Whether to ship Mālikī/Ḥanbalī matrices vs keep the Evidence-based default.
