# Findings Table — hadith citations (sunnah.com)

Adversarial verification of every hadith cited in `evidence.json` / `methodology.html` /
`q/*.html`. Each URL was opened; collection, number, grade, narrator, and text were checked
against the live page. Scope: hadith only (Qur'an 4:103 handled in the fiqh report).

| evidence id | URL checked | verdict | what sunnah.com actually says | note |
|---|---|---|---|---|
| no-nafl-after-fajr-asr | sunnah.com/bukhari:586 | CONFIRMED | Sahih al-Bukhari 586, Book 9 H.61; Abu Sa`id Al-Khudri | "There is no prayer after the morning prayer till the sun rises, and there is no prayer after the `Asr prayer till the sun sets." Exact. |
| no-nafl-after-fajr-asr (also) | sunnah.com/muslim:827 | CONFIRMED | Sahih Muslim 827, Book 6 H.351; Abu Sa'id al-Khudri | Same content; clause order reversed. Fine. |
| three-solar-times | sunnah.com/muslim:831 | CONFIRMED | Sahih Muslim 831, Book 6 H.357; Uqba b. 'Amir | Names prayer AND burial, all three solar moments. Exact. |
| qada-when-remembered | sunnah.com/bukhari:597 | CONFIRMED | Sahih al-Bukhari 597, Book 9 H.72; Anas | "If anyone forgets a prayer he should pray it when he remembers it. There is no expiation except to pray the same." Matches. |
| qada-when-remembered (also) | sunnah.com/muslim:684a | CONFIRMED | Sahih Muslim 684a, Book 5 H.399; Anas b. Malik | Matches. |
| asr-rakah-before-sunset | sunnah.com/bukhari:579 | CONFIRMED (partial quote) | Sahih al-Bukhari 579, Book 9 H.55; Abu Huraira | Text covers BOTH Fajr+Asr rak'ah; product quotes the Asr half only. Faithful partial. |
| asr-rakah-before-sunset (also) | sunnah.com/muslim:608a | CONFIRMED | Sahih Muslim 608a, Book 5 H.208; Abu Huraira | Same both-prayers wording. Supports claim. |
| prayer-after-asr-umm-salamah | sunnah.com/bukhari:1233 | CONFIRMED (narrator caveat) | Sahih al-Bukhari 1233, Book 22 H.11; byline "Narrated Kuraib" — NOT "Umm Salamah" | Content supports claim (two rak'ahs after Asr = missed Zuhr). Matn is Umm Salamah's via Kuraib; site's narrator field is Kuraib. |
| prayer-after-asr-umm-salamah (also) | sunnah.com/muslim:834 | CONFIRMED (narrator caveat) | Sahih Muslim 834, Book 6 H.361; byline "Kuraib, freed slave of Ibn 'Abbas" | Same account; Umm Salama's testimony. Matches. |
| makkah-tawaf-any-hour | sunnah.com/abudawud:1894 | CONFIRMED | Sunan Abi Dawud 1894, Book 11 H.174; Jubayr ibn Mut'im; grade "Sahih (Al-Albani)" | "Do not prevent anyone from going round this House and from praying any moment he desires by day or by night." Grade exactly as claimed. |
| makkah-tawaf-any-hour (also) | sunnah.com/tirmidhi:868 | CONFIRMED (grader differs) | Jami` at-Tirmidhi 868, Book 9 H.61; Jubair bin Mut'im; grade "Sahih (Darussalam)" | Sahih, but graded by Darussalam, not al-Albani. |
| makkah-tawaf-any-hour (also) | sunnah.com/ibnmajah:1254 | CONFIRMED (grader differs) | Sunan Ibn Majah 1254, Book 5 H.452; Jubair bin Mut'im; grade "Sahih (Darussalam)" | Sahih (Darussalam). |
| friday-khutbah-two-rakahs | sunnah.com/bukhari:931 | CONFIRMED | Sahih al-Bukhari 931, Book 11 H.55; Jabir | "Have you prayed? ... Pray two rak`at." Matches. |
| friday-khutbah-two-rakahs (also) | sunnah.com/muslim:875a | CONFIRMED | Sahih Muslim 875a, Book 7 H.68; Jabir b. 'Abdullah | Matches. |

## Summary

1. Nothing INCORRECT — all 7 hadith resolve to the right collection, number, text, and an authentic grade.
2. **Umm Salamah narrator**: sunnah.com bylines Bukhari 1233 / Muslim 834 as "Narrated Kuraib." Matn is Umm Salamah's (defensible), but the narrator field differs.
3. **Makkah-tawaf cross-refs**: only Abu Dawud 1894 is "Sahih (Al-Albani)"; Tirmidhi 868 and Ibn Majah 1254 are "Sahih (Darussalam)." Bukhari 579 quoted for the Asr half only (accurate partial).
