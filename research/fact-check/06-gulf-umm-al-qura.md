# Prayer-time method fact-check — Gulf / Umm al-Qura countries

Deep-dive on "Umm al-Qura for Saudi + Bahrain + Oman + Yemen." Sources: api.aladhan.com/v1/methods,
praytimes.org/docs/calculation, arabeyes ITL method-info, IslamicFinder Oman.

**Anchor:** Umm al-Qura = Fajr 18.5°, Isha 90 min after Maghrib (120 Ramadan). Isha is a fixed interval, not an angle.

| Country | Assigned | Matches? | Authority's actual method | Reference |
|---|---|---|---|---|
| Saudi Arabia | Umm al-Qura | yes | Umm al-Qura IS the Saudi method (Fajr 18.5°, was 19° pre-1430H) | praytimes.org/docs/calculation |
| Bahrain | Umm al-Qura | oversimplified | arabeyes ITL groups Bahrain under "Gulf Region" = Fajr 19.5°, Isha 90 min (different Fajr from UAQ 18.5°) | arabeyes ITL method-info |
| Oman | Umm al-Qura | oversimplified / no | Own Ministry (MARA) publishes its own national calendar; Gulf model uses 19.5° Fajr | islamicfinder.org/world/oman |
| Yemen | Umm al-Qura | oversimplified | Appears in no primary technical table; state fragmentation; fallback grouping only | api.aladhan.com/v1/methods |

## Takeaways

- Only Saudi Arabia is a true match. Bahrain/Oman/Yemen are lumped into a "Gulf Region" 19.5°/90-min model that differs from UAQ 18.5° by ~1°.
- Oman is the clearest miss (runs its own MARA). Yemen is unverifiable.
- Reference sources disagree (IslamicFinder=18.5°, arabeyes/aladhan Gulf=19.5°), itself evidence a single-preset assignment isn't defensible for these three.
- Note: mara.gov.om has a broken TLS cert chain; the Oman finding rests on IslamicFinder + arabeyes/aladhan Gulf grouping.
