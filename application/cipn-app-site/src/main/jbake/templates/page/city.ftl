<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  City page (type=city, spec §8.1): SEO landing for "can i pray now in {city}".

  This template owns EVERY rendered byte. scripts/GenerateCityPages.java only
  materialises the front-matter stub, because JBake emits one page per content
  file and nothing in FreeMarker can fan a dataset out into 110 of them.

  Presets the app to the city's coordinates via body data-* attributes; a
  visitor with their own saved location keeps it (main.js gives storage
  precedence). Windows are computed live client-side - always today, never a
  stale baked table.

  Front matter (data only): city_name, country_name, city_lat, city_lng,
  city_tz, city_cc, nearby ("name|country|href|km" records joined by ';').

  @author Fahim Farook
-->
<#assign cityName = content.city_name>
<#assign countryName = content.country_name>
<#-- Assigned before the header include, which honours them (see header.ftl). -->
<#assign metaTitle = "Can I Pray Now in ${cityName}? Prohibited Prayer Times Today">
<#assign metaDescription = "Live answer for ${cityName}, ${countryName}: is it permissible to pray your salah right now? The prohibited and disliked namaz times (sunrise, zenith, sunset, and the after-Fajr and after-Asr windows), computed for ${cityName}.">
<!DOCTYPE html>
<html lang="en">
<head>
<#include "/partial/header.ftl">
<#-- FAQ structured data: gives the city page eligible rich results and a second
     machine-legible statement of what it answers, beyond the client-rendered app. -->
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "Can I pray now in ${cityName?json_string}?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "It depends on what you intend to pray and the current time. Prayer is withheld around sunrise, solar noon and sunset, and voluntary prayer waits after Fajr and after Asr. Can I Pray Now gives a live, on-device answer for ${cityName?json_string} conditioned on your intended prayer and school."
      }
    },
    {
      "@type": "Question",
      "name": "What are the prohibited prayer times in ${cityName?json_string}?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "The strict prohibited times in ${cityName?json_string} are the few minutes around sunrise, solar zenith (midday) and sunset. Voluntary prayer without a cause is also withheld from the entry of Fajr until the sun has risen, and from Asr until sunset. The exact minutes are computed astronomically for ${cityName?json_string} each day."
      }
    }
  ]
}
</script>
</head>
<body class="state-neutral"
      data-preset-name="${cityName}"
      data-preset-country="${countryName}"
      data-preset-lat="${content.city_lat}"
      data-preset-lng="${content.city_lng}"
      data-preset-tz="${content.city_tz}"
      data-preset-cc="${content.city_cc}"
      data-country-method="${content.city_method_key}"
      data-country-madhhab="${content.city_madhhab}">
<#-- Option A (ADR-0016 UX): the live app is the whole first screenful - no top nav bar, no
     visible headline (the location box already names the city). The h1 stays for search engines,
     visually hidden like the home page; the article moves BELOW the app; nav lives in the app
     footer (the sidebar). -->
<h1 class="visually-hidden">Can I pray now in ${cityName}?</h1>
<#include "/partial/app.ftl">
<div class="city-article">
<article class="page-wrap">
<span class="kicker">${countryName}</span>
<h2>Prayer times in ${cityName}</h2>

<p>The five restricted windows move with the sun over ${cityName} every day: the strict sunrise,
zenith, and sunset times in which prayer is withheld, and the extended after-Fajr and after-Asr
windows in which voluntary prayer waits. A fixed timetable cannot answer &ldquo;right now&rdquo; -
the live answer above is computed astronomically for ${cityName} (${content.city_tz}) this very
minute, and follows your madhhab and what you intend to pray.</p>

<h2>How ${cityName}&rsquo;s times are calculated</h2>
<#if (content.city_authority)?? && content.city_authority?has_content>
<p>In ${countryName}, published prayer timetables generally follow <strong>${content.city_authority}</strong>, using ${content.city_method}. Can I Pray Now computes ${cityName}&rsquo;s times with that same convention on your device - or serves the authority&rsquo;s own published table where we have extracted it - so the list matches what your local mosque prays by. You can change the method in settings.</p>
<#else>
<p>In ${countryName}, prayer times generally follow ${content.city_method}. Can I Pray Now computes ${cityName}&rsquo;s times with that convention on your device, so they match what your community prays by; you can switch to any other method in settings.</p>
</#if>
<#if (content.city_method_note)?? && content.city_method_note?has_content>
<p>${content.city_method_note}</p>
</#if>
<#if (content.city_madhhab)?? && content.city_madhhab?has_content>
<p>The school most widely followed in ${countryName} is <strong>${content.city_madhhab}</strong>, which sets the Asr time and shapes some of the restricted-time rulings. The app defaults to the evidence-based ruling for everyone; you can select a school in settings. For the twilight angles behind these times, see <a href="/methods.html">how prayer times work</a> and <a href="/calculation-methods.html">the methods compared</a>.</p>
<#else>
<p>For the twilight angles behind these times, see <a href="/methods.html">how prayer times work</a> and <a href="/calculation-methods.html">the methods compared</a>.</p>
</#if>

<p>The tool also lays out today&rsquo;s five daily prayer times for ${cityName}, Fajr, Dhuhr, Asr,
Maghrib and Isha, on the timeline and the prayer list. The open stretches between them are when
salah may be offered, and the restricted windows are when it may not; the live answer shows where
${cityName} stands this minute.</p>

<#if (content.nearby)?? && content.nearby?has_content>
<h2>Nearby cities</h2>
<ul class="nearby">
<#list content.nearby?split(";") as record>
    <#assign f = record?split("|")>
    <li><a href="${f[2]}">${f[0]}, ${f[1]}</a> <span>${f[3]}&nbsp;km</span></li>
</#list>
</ul>
</#if>

<#-- City -> question cross-links. Every city page links the full question set,
     so each /q/ answer is reachable from anywhere and gains real inbound links
     rather than the single link from the hub. Reuses .city-index (no new CSS).
     Auto-syncs with the question set the same way page/questions.ftl does. -->
<#assign cityQuestions = published_content?filter(c -> (c.type!"") == "question")?sort_by("title")>
<#if cityQuestions?has_content>
<h2>Common questions</h2>
<ul class="city-index">
<#list cityQuestions as q>    <li><a href="/${q.uri}">${q.title?html}</a></li>
</#list></ul>
</#if>

<p><em>The live answer above updates every minute for ${cityName} (${content.city_tz}) and follows
your selected madhhab and prayer type. See the
<a href="/methodology.html">methodology</a>.</em></p>
<#-- Baked authority timetable (T-11 / M1): the reactor writes it into the content body for
     authority-table cities (LK/MY). Empty for computed-only cities, which stay client-side. -->
<#if content.body?? && content.body?has_content>
${content.body}
</#if>
</article>
</div>
</body>
</html>
