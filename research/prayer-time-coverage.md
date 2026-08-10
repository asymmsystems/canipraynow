# Prayer-time coverage (generated from country.xml)

Generated from `country.xml` (T-01/T-27), 87 countries. Do not edit by hand; regenerate from the register.

## Source-of-truth

- **computed**: 85
- **authority**: 2

## Feed type

- none: 50
- html: 21
- api: 7
- app: 5
- pdf: 3
- image: 1

## Redistributable

- unknown: 86
- no: 1

## Confidence

- low: 34
- medium: 34
- high: 19

## Has-authority

- true: 46
- false: 41

## Served from authority table (scrape done)

- LK — All Ceylon Jamiyyathul Ulama (ACJU) (pdf)
- MY — JAKIM (e-Solat) (api)

## Scrape candidates (obtainable feed, currently computed): 30

| cc | feed | authority | note |
|---|---|---|---|
| AE | api | GAIAE (Awqaf) federal; IACAD for Dubai | Official GAIAE/Awqaf method is Gulf-region (Fajr 19.5, Isha 90 min); the Dubai 18.2 preset |
| DE | api | DITIB (Diyanet-affiliated) communities | Diaspora default for the Turkish-origin majority; many German Muslims use MWL. Computed (D |
| KZ | api | Spiritual Administration of Muslims of K | Board method (Hanafi). Verified keyless JSON: settlement list api.muftyat.kz/cities/ (coun |
| NL | api | Diyanet-affiliated communities | Diaspora default; Moroccan communities use MWL. Computed (Diyanet). |
| QA | api | Qatar Calendar House (published via MOI) | Keyless JSON API exists (portal.moi.gov.qa), but today-only, single-point (Doha), behind a |
| TR | api | Diyanet İşleri Başkanlığı | Official tables add a temkin margin (deviate). API is credential-gated (JWT, rate-limited) |
| AF | html | Ministry of Hajj and Religious Affairs ( | Kabul-only HTML; no national machine-readable table. Computed (Karachi). |
| AL | html | Komuniteti Mysliman i Shqipërisë (KMSH) | Publishes an official takvim that deviates from a pure preset; HTML scrape candidate, curr |
| BA | html | Islamic Community in Bosnia and Herzegov | Official BiH takvim deviates from a pure preset; HTML scrape candidate, currently computed |
| DK | html | National Islamic councils (high-latitude | High-latitude tables deviate; HTML scrape candidate (prayertimes.dk), currently computed. |
| DZ | html | Ministry of Religious Affairs and Awqaf | Official annual table close to MWL angles (18/17). Computed. |
| GB | html | No single authority; Moonsighting Commit | Some mosque tables deviate in summer (high-latitude). Computed. |
| ID | html | Kementerian Agama (Kemenag) - Ditjen Bim | 20/18 + ~2 min ihtiyat. HTML page + undocumented session-gated JSON (ajax/getShalatbln); o |
| IR | html | Institute of Geophysics, University of T | Tehran 17.7/14, Maghrib 4.5 (Jafari). Computing reproduces the national standard. Computed |
| JO | html | General Iftaa Department / Ministry of A | Jordan has its own 18/18 Awqaf method (Maghrib +5); generic MWL 18/17 is a close but imper |
| KG | html | Spiritual Administration of Muslims of K | Hanafi board method; site is JS-rendered with no exposed JSON endpoint. Computed (regional |
| MA | html | Ministry of Habous and Islamic Affairs | Legally-binding national table deviates (offsets, Dhuhr/Maghrib +5). HTML/PDF scrape candi |
| MK | html | Islamic Religious Community of North Mac | Official takvim deviates; HTML scrape candidate, currently computed. |
| NO | html | National Islamic councils (high-latitude | High-latitude tables deviate; scrape candidate, currently computed. |
| OM | html | Ministry of Endowments and Religious Aff | MARA runs its own national calendar (Ibadi ~18/18); Umm al-Qura is a placeholder and the c |
| SE | html | National Islamic councils (high-latitude | High-latitude tables deviate; scrape candidate, currently computed. |
| SG | html | MUIS (Majlis Ugama Islam Singapura) | MUIS 20/18. Official table is on data.gov.sg (SODL is permissive - the one country where r |
| TH | html | Sheikhul Islam Office / CICOT | ~18/18; verify. Computed with the MUIS 20/18 regional preset. |
| TJ | html | Islamic Center of the Republic of Tajiki | Static HTML: one Dushanbe table + per-city minute offsets, no API/feed. Hanafi board metho |
| UZ | html | Muslim Board of Uzbekistan | Board method (Hanafi), per-region corrections (deviates). Authority page (islom.uz) bot-bl |
| XK | html | Islamic Community of Kosovo | Official takvim deviates; HTML scrape candidate, currently computed. |
| ZA | html | Regional bodies (MJC, Jamiatul Ulama) | No single national authority; regional tables deviate (local angles + safety minutes). Com |
| BD | pdf | Islamic Foundation Bangladesh | Official annual PDF (Dhaka base + district minute offsets, Karachi method); scanned/image, |
| PH | pdf | Bangsamoro Regional Darul Ifta (BARMM);  | BARMM-only PDF (5 provinces), not national; NCMF does moon-sighting. Computed (Karachi). |
| AZ | image | Caucasus Muslims Board (QMI) | Authority publishes a Jafari monthly per-city JPEG (needs OCR). Compute fallback is contes |

## Full register

| cc | served-by | feed | method-key | redistributable | confidence | authority |
|---|---|---|---|---|---|---|
| AE | computed | api | Dubai | unknown | high | GAIAE (Awqaf) federal; IACAD for Dubai |
| AF | computed | html | Karachi | unknown | low | Ministry of Hajj and Religious Affairs (MOHI |
| AL | computed | html | MuslimWorldLeague | unknown | medium | Komuniteti Mysliman i Shqipërisë (KMSH) |
| AT | computed | none | MuslimWorldLeague | unknown | low |  |
| AU | computed | none | MuslimWorldLeague | unknown | medium |  |
| AZ | computed | image | MuslimWorldLeague | unknown | medium | Caucasus Muslims Board (QMI) |
| BA | computed | html | MuslimWorldLeague | unknown | high | Islamic Community in Bosnia and Herzegovina  |
| BD | computed | pdf | Karachi | unknown | medium | Islamic Foundation Bangladesh |
| BE | computed | none | MuslimWorldLeague | unknown | low |  |
| BF | computed | none | MuslimWorldLeague | unknown | medium |  |
| BH | computed | app | UmmAlQura | unknown | medium | Ministry of Justice, Islamic Affairs and Awq |
| BN | computed | app | Singapore | unknown | medium | Ministry of Religious Affairs (Brunei) |
| BR | computed | none | MuslimWorldLeague | unknown | low |  |
| CA | computed | none | NorthAmerica | unknown | high |  |
| CH | computed | none | MuslimWorldLeague | unknown | low |  |
| CI | computed | none | MuslimWorldLeague | unknown | medium |  |
| CM | computed | none | MuslimWorldLeague | unknown | medium |  |
| DE | computed | api | Turkey | unknown | medium | DITIB (Diyanet-affiliated) communities |
| DJ | computed | none | MuslimWorldLeague | unknown | low |  |
| DK | computed | html | MoonsightingCommittee | unknown | medium | National Islamic councils (high-latitude rul |
| DZ | computed | html | MuslimWorldLeague | unknown | medium | Ministry of Religious Affairs and Awqaf |
| EG | computed | none | Egyptian | unknown | high | Egyptian Survey Authority |
| ER | computed | none | MuslimWorldLeague | unknown | low |  |
| ES | computed | none | MuslimWorldLeague | unknown | low |  |
| ET | computed | none | MuslimWorldLeague | unknown | low |  |
| FR | computed | none | France | unknown | high | Union of Islamic Organisations of France (UO |
| GB | computed | html | MoonsightingCommittee | unknown | high | No single authority; Moonsighting Committee  |
| GH | computed | none | MuslimWorldLeague | unknown | medium |  |
| GM | computed | none | MuslimWorldLeague | unknown | low |  |
| GN | computed | none | MuslimWorldLeague | unknown | low |  |
| GR | computed | none | MuslimWorldLeague | unknown | low |  |
| GY | computed | none | MuslimWorldLeague | unknown | low |  |
| ID | computed | html | Kemenag | unknown | high | Kementerian Agama (Kemenag) - Ditjen Bimas I |
| IN | computed | none | Karachi | unknown | medium |  |
| IQ | computed | none | MuslimWorldLeague | unknown | low | Sunni Endowment; Najaf (Shia) for Jafari com |
| IR | computed | html | Tehran | unknown | high | Institute of Geophysics, University of Tehra |
| IT | computed | none | MuslimWorldLeague | unknown | low |  |
| JO | computed | html | MuslimWorldLeague | unknown | medium | General Iftaa Department / Ministry of Awqaf |
| KE | computed | none | MuslimWorldLeague | unknown | medium |  |
| KG | computed | html | MuslimWorldLeague | unknown | medium | Spiritual Administration of Muslims of Kyrgy |
| KM | computed | none | MuslimWorldLeague | unknown | low |  |
| KW | computed | app | Kuwait | unknown | medium | Ministry of Awqaf and Islamic Affairs |
| KZ | computed | api | MuslimWorldLeague | unknown | high | Spiritual Administration of Muslims of Kazak |
| LB | computed | none | MuslimWorldLeague | unknown | low | Dar al-Fatwa; Higher Shia Council |
| LK | authority | pdf | ACJU | unknown | high | All Ceylon Jamiyyathul Ulama (ACJU) |
| LY | computed | none | MuslimWorldLeague | unknown | low | General Authority of Awqaf and Islamic Affai |
| MA | computed | html | MuslimWorldLeague | unknown | high | Ministry of Habous and Islamic Affairs |
| MK | computed | html | MuslimWorldLeague | unknown | medium | Islamic Religious Community of North Macedon |
| ML | computed | none | MuslimWorldLeague | unknown | low |  |
| MM | computed | none | Karachi | unknown | low |  |
| MR | computed | none | MuslimWorldLeague | unknown | low | Ministry of Islamic Affairs |
| MV | computed | app | Karachi | unknown | medium | Ministry of Islamic Affairs |
| MY | authority | api | Singapore | unknown | high | JAKIM (e-Solat) |
| NE | computed | none | MuslimWorldLeague | unknown | low |  |
| NG | computed | none | MuslimWorldLeague | unknown | medium | Nigerian Supreme Council for Islamic Affairs |
| NL | computed | api | Turkey | unknown | medium | Diyanet-affiliated communities |
| NO | computed | html | MoonsightingCommittee | unknown | medium | National Islamic councils (high-latitude rul |
| NZ | computed | none | MuslimWorldLeague | unknown | medium | FIANZ (NGO) |
| OM | computed | html | UmmAlQura | unknown | low | Ministry of Endowments and Religious Affairs |
| PH | computed | pdf | Karachi | unknown | medium | Bangsamoro Regional Darul Ifta (BARMM); NCMF |
| PK | computed | none | Karachi | unknown | high | Central Ruet-e-Hilal Committee - moon-sighti |
| PS | computed | none | MuslimWorldLeague | unknown | low | Ministry of Awqaf / Dar al-Ifta |
| QA | computed | api | Qatar | unknown | high | Qatar Calendar House (published via MOI) |
| RU | computed | none | Russia | unknown | high | Spiritual Administration of Muslims of Russi |
| SA | computed | app | UmmAlQura | no | high | Umm al-Qura Calendar (KACST) |
| SD | computed | none | MuslimWorldLeague | unknown | low | Islamic Fiqh Council / Guidance |
| SE | computed | html | MoonsightingCommittee | unknown | medium | National Islamic councils (high-latitude rul |
| SG | computed | html | Singapore | unknown | high | MUIS (Majlis Ugama Islam Singapura) |
| SL | computed | none | MuslimWorldLeague | unknown | low |  |
| SN | computed | none | MuslimWorldLeague | unknown | medium |  |
| SO | computed | none | MuslimWorldLeague | unknown | low |  |
| SR | computed | none | MuslimWorldLeague | unknown | low |  |
| SY | computed | none | MuslimWorldLeague | unknown | low | Ministry of Awqaf |
| TD | computed | none | MuslimWorldLeague | unknown | low |  |
| TH | computed | html | Singapore | unknown | medium | Sheikhul Islam Office / CICOT |
| TJ | computed | html | MuslimWorldLeague | unknown | medium | Islamic Center of the Republic of Tajikistan |
| TM | computed | none | MuslimWorldLeague | unknown | low |  |
| TN | computed | none | MuslimWorldLeague | unknown | medium | Ministry of Religious Affairs |
| TR | computed | api | Turkey | unknown | high | Diyanet İşleri Başkanlığı |
| TT | computed | none | MuslimWorldLeague | unknown | low |  |
| TZ | computed | none | MuslimWorldLeague | unknown | medium |  |
| UG | computed | none | MuslimWorldLeague | unknown | low |  |
| US | computed | none | NorthAmerica | unknown | high | No single authority; ISNA / Fiqh Council of  |
| UZ | computed | html | MuslimWorldLeague | unknown | medium | Muslim Board of Uzbekistan |
| XK | computed | html | MuslimWorldLeague | unknown | medium | Islamic Community of Kosovo |
| YE | computed | none | UmmAlQura | unknown | low | Ministry of Endowments (no functioning unifi |
| ZA | computed | html | MuslimWorldLeague | unknown | medium | Regional bodies (MJC, Jamiatul Ulama) |
