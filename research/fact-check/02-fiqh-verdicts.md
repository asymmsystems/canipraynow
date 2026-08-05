# canipraynow.com — Adversarial Fiqh Fact-Check Findings

Verification of Qur'an citations, IslamQA references, and the defensibility of each fiqh
verdict against mainstream scholarship for its own basis (Evidence / Ḥanafī / Shāfiʿī).
Files audited: `rule_sets/evidence.json`, `hanafi.json`, `shafii.json`.

## Findings table (INCORRECT / NEEDS-SCHOLAR ranked first)

| Claim | Location | Verdict | Primary source opened | Correction / note |
|---|---|---|---|---|
| Ibn Bāz / Ibn ʿUthaymīn extend the causal-prayer exception into the strict solar windows — causal nafl PERMITTED at sunrise, zenith AND sunset | evidence.json SUNRISE/ZENITH/SUNSET causalNafl | NEEDS-SCHOLAR / OVERSIMPLIFIED | abukhadeejah.com/tahiyyat-al-masjid…; islamqa.info/en/answers/48998; /112114 | Over-claimed attribution. The cited fatwas (112114, 81978) and Ibn Bāz/Ibn ʿUthaymīn texts frame dhawāt al-asbāb around the two EXTENDED windows (after Fajr / after ʿAsr). Ibn Bāz's wording covers "before Maghrib" (isfirār) and "before the sun has fully risen"; zenith and the literal setting are not explicitly addressed. Defensible on Ibn Taymiyyah's general principle, but IslamQA 48998 flags the three solar times as the more emphatic prohibition. Scholar must decide keep-PERMITTED vs downgrade at zenith/sunset. |
| "Scholarly basis is after performing Fajr/Asr" for the anchor convention | hanafi.json params.anchor_convention.basis | INCORRECT (basis note) / NEEDS-SCHOLAR | islamqa.org/hanafi/daruliftaa-birmingham/88210/ | Ḥanafī after-Fajr (W1) bar is TIME-based (from true dawn), not performance-based; only after-ʿAsr (W4) is performance-based. Note half-wrong. Verdict (anchor to time-entry) fine; rationale inaccurate. |
| anchor_convention = "prayer-time-entry" despite IslamQA saying bar attaches to having prayed | evidence.json & shafii.json anchor_convention | NEEDS-SCHOLAR (product self-flags) | islamqa.info/en/answers/136695 | Ibn ʿUthaymīn: bar attaches to having PERFORMED Fajr/ʿAsr. App can't know performance so anchors to time-entry; over-prohibits causal nafl in the adhān→prayer gap. Real ijtihād call. |
| P_sunrise 12–20, P_zenith 5–15, P_isfirar 20–40 min | all three params.*.value | UNVERIFIABLE / NEEDS-SCHOLAR | (no primary source fixes minutes) | Conventional intervals; every basis says "scholar to set final value." Correctly flagged. |
| Shāfiʿī isfirār "attaches to the setting itself; window kept for display symmetry" | shafii.json params.P_isfirar.basis | OVERSIMPLIFIED | islamqa.info/en/answers/48998; seekersguidance.org (Shāfiʿī tahiyyat al-masjid) | Reliance f.13 lists a distinct yellowing (isfirār) window as a real time-based prohibited period. Shāfiʿī recognizes it, not only the instant of setting. Affects rationale not the cell. |
| Qurʾān 4:103 = an-Nisāʾ, "prayer decreed at specified times" | evidences.salah-timed; quran.com/4/103 | CONFIRMED | quran.com/4/103 | Correct surah:ayah; Saheeh International verbatim. Supports "prayer at fixed times." |
| IslamQA 112114 = dhawāt al-asbāb permitted; Ibn Taymiyyah's view | commentary[0] | CONFIRMED | islamqa.info/en/answers/112114 | "Naafil prayers that it is permissible to do at times when prayer is otherwise forbidden." Lists taḥiyyat al-masjid, ṭawāf, kusūf, wuḍūʾ, istikhāra. |
| IslamQA 81978 = causal-prayer exception is the more correct view | commentary[1] | CONFIRMED | islamqa.info/en/answers/81978 | States the causal-prayer view "is the view of ash-Shāfiʿī… and it is the more correct view." |
| IslamQA 20013 = qaḍāʾ of fard obligatory even in prohibited times | commentary[2]; qada cells | CONFIRMED | islamqa.info/en/answers/20013 | "must make up the prayer… even if that is at one of the times when prayer is disallowed. This is the view of the majority." |
| IslamQA 136695 = W1 tied to the Fajr prayer, not the raw time | commentary[3] | CONFIRMED | islamqa.info/en/answers/136695 | Ibn ʿUthaymīn: "the prohibition has to do with Fajr prayer itself." |
| al-Albānī: janāzah in the 3 solar times disliked-unless-necessity | evidence.json solar-times janazah = DISLIKED | CONFIRMED | emaanlibrary.com (Aḥkām al-Janāʾiz); en.tohed.com | Aḥkām al-Janāʾiz ~p.165: not permissible except in necessity. Product's DISLIKED-unless-need accurate. |
| Evidence: janāzah PERMITTED after Fajr & after ʿAsr | evidence.json AFTER_FAJR/AFTER_ASR janazah | CONFIRMED | sunnah.com/muslim:831 | Uqbah hadith names only the three solar times for burial. |
| Evidence: qaḍāʾ PERMITTED in all five windows incl. strict | evidence.json qada everywhere | CONFIRMED | islamqa.info/en/answers/20013 | Majority + Salafi view: due/missed fard prayed whenever remembered. |
| Ḥanafī: qaḍāʾ INVALID in the three strict times | hanafi.json solar-times qada = invalid | CONFIRMED | islamqa.org/hanafi/…/88210/ | Standard Ḥanafī, except a wājib arising within the time and that day's ʿAsr. |
| Ḥanafī: two rakʿahs of Fajr sunnah excepted after Fajr entry | hanafi.json AFTER_FAJR note SUNNAH_FAJR | CONFIRMED | islamweb.net/en/fatwa/361474/; islamqa.org/…/88210/ | Valid, not merely makrūh. |
| Ḥanafī: janāzah "disliked if ready beforehand, permitted if it became ready within" | hanafi.json strict janazah JANAZAH_STRICT | CONFIRMED | islamqa.org/…/88210/; fiqh.islamonline.net | Standard Ḥanafī position. |
| Ḥanafī: nafl (causal or pure) after Fajr & after ʿAsr = makrūh taḥrīmī | hanafi.json AFTER_FAJR/AFTER_ASR | CONFIRMED | islamqa.org/…/88210/ | Ḥanafī rejects the dhāt-sabab exception. Correctly divergent from Shāfiʿī/Salafi. |
| Shāfiʿī: prayers with a cause VALID in all prohibited times; only causeless nafl barred | shafii.json all cells | CONFIRMED | seekersguidance.org; islamqa.info/en/answers/81978 | Madhhab position (Nawawī, al-Majmūʿ / Reliance f.13). |
| Shāfiʿī: Friday-zenith carries no prohibition | shafii.json exceptions FRIDAY_ZENITH | CONFIRMED | islamqa.info/en/answers/48998 | Genuine Shāfiʿī exception. |
| Shāfiʿī: within the Makkan Ḥaram no time prohibited | shafii.json exceptions MAKKAH_HARAM | CONFIRMED | islamqa.info/en/answers/48998 | Jubayr b. Muṭʿim hadith. |
| Evidence SUNSET "Ibn Bāz permitted causal prayers even as the sun sets" | evidence.json SUNSET causalNafl.basis | OVERSIMPLIFIED | abukhadeejah.com | Ibn Bāz's wording is "before Maghrib" (extended window), not the strict setting moment. Same root issue as top row. |

## What a scholar must still decide

1. Causal nafl inside the three STRICT solar moments (zenith and the literal setting) for the Evidence basis — the single most over-reached set of cells.
2. Anchor convention (time-entry vs having-performed); for Ḥanafī split W1 time-based (correct) vs W4 performance-based; fix the hanafi.json rationale note.
3. Exact P_sunrise / P_zenith / P_isfirar minute values.
4. Shāfiʿī isfirār rationale — Shāfiʿī treats yellowing as a distinct prohibited window (Reliance f.13).
5. Umm Salamah after-ʿAsr hadith framing — Ḥanafīs read it as khuṣūṣiyya; confirm not presented as universally binding.
