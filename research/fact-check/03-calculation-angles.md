# Prayer-time calculation method fact-check (all 16 methods)

External check of every documented Fajr/Isha angle and the two structural claims. Values were
already confirmed to match the vendored Adhan.js library internally; this pass confirms they
also match what the real-world authorities publish/use.

Sources opened: api.aladhan.com/v1/methods; praytimes.org/calculation;
github.com/batoulapps/adhan-js/blob/master/METHODS.md; moonsighting.com/how-we.html;
acju.lk/prayer-times; support.muslimpro.com (UOIF).

Headline: nothing is flat-out INCORRECT. Every twilight-angle value matches ≥2 reputable
references. Soft spots: ACJU (no published angle exists), Moonsighting (18/18 is a label, not
the real algorithm), and the "show no verdict" high-latitude claim (correct but conservative).

| method / claim | claimed | verdict | reference | note |
|---|---|---|---|---|
| ACJU (Sri Lanka) | 20/18, fitted, err 0.74/0.97 min | UNVERIFIABLE (angles) / OVERSIMPLIFIED | acju.lk/prayer-times | ACJU publishes district minute-tables, not angles (Makdoomi calc, per-building-height offsets). "20/18" is a reverse-engineered fit. Present as "approximates ACJU tables," never "ACJU uses 20/18." |
| Moonsighting Committee | 18/18 + seasonal | OVERSIMPLIFIED | moonsighting.com/how-we.html | Not a fixed-angle method; functions of latitude+season (shafaq general), 1/7-night above 55°. "18/18" is a nominal label. |
| UOIF (France) | 12/12 | CONFIRMED (caveat) | muslimpro; aladhan France=12/12 | Genuine UOIF ("Muslims of France") convention. France fragmented (GMP uses 15/18); don't call it "the French standard." |
| Diyanet (Turkey) | 18/17 + temkin | CONFIRMED (caveat) | praytimes.org; aladhan | Both list Turkey 18/17 ("experimental"; table-based with temkin margin). |
| Dubai (UAE) | 18.2/18.2 | CONFIRMED value (caveat) | aladhan; adhan METHODS.md | Matches library, but the "Dubai" preset is NOT the official UAE/Awqaf method (Gulf 19.5/90). See country report. |
| Kemenag (Indonesia) | 20/18 + ~2 min ihtiyat | CONFIRMED | aladhan | Indonesia = 20/18; ihtiyat standard Kemenag. |
| SAMR (Russia) | 16/15 | CONFIRMED | aladhan; praytimes | Both list Russia 16/15. |
| Tehran | 17.7/14, Maghrib 4.5°, Jafari midnight | CONFIRMED | praytimes; aladhan; adhan spec | All three agree. Isha 14 correct. |
| Kuwait | 18/17.5 | CONFIRMED | praytimes; aladhan | Both 18/17.5. |
| Qatar | 18 + 90-min Isha | CONFIRMED | praytimes; adhan spec | Fajr 18, Isha 90-min interval. |
| ISNA (North America) | 15/15 | CONFIRMED | praytimes; aladhan | Published ISNA method (debated 15° vs 18° internally). |
| MWL | 18/17 | CONFIRMED | praytimes; aladhan | Confirmed. |
| Egyptian | 19.5/17.5 | CONFIRMED | praytimes; adhan spec | Current standard (older 20/18 is a separate variant). Name should read "Egyptian Survey Authority." |
| Umm al-Qura | 18.5 + 90-min (120 Ramadan) | CONFIRMED | praytimes; adhan spec | Confirmed. |
| Karachi | 18/18 | CONFIRMED | praytimes; aladhan | Confirmed. |
| MUIS (Singapore) | 20/18 | CONFIRMED | praytimes; aladhan | Confirmed. |

## Structural claims

- **Three sun-times invariant** — CONFIRMED. Sunrise/sunset are geometric disc-horizon crossing (−0.833°); solar noon is meridian crossing. Identical across methods to the minute. **Tehran/Jafari exception real**: Maghrib at 4.5° below horizon, so its "Maghrib" is ~4–5 min after true sunset and is not the invariant sunset.
- **High latitudes (~>55°)** — astronomically CONFIRMED (above ~48.5–55° the sun never reaches the Fajr/Isha depression on summer nights; polar circles lose sunrise/sunset). "Show no verdict" is the most conservative choice — NOT what named authorities do (they substitute middle-of-night / 1/7-night / nearest-latitude). Frame abstaining as a deliberate design decision, not the industry default.

## Bottom line

All 16 angle rows externally corroborated. Tighten wording on three: ACJU ("fitted to ACJU tables"), Moonsighting ("18/18" nominal label), high-latitude "no verdict" (conservative choice).
