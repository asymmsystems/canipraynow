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
<#assign metaDescription = "Live answer for ${cityName}, ${countryName}: is it permissible to pray right now? The prohibited and disliked prayer times (sunrise, zenith, sunset and the after-Fajr and after-Asr windows), computed astronomically.">
<!DOCTYPE html>
<html lang="en">
<head>
<#include "/partial/header.ftl">
</head>
<body class="state-neutral"
      data-preset-name="${cityName}"
      data-preset-country="${countryName}"
      data-preset-lat="${content.city_lat}"
      data-preset-lng="${content.city_lng}"
      data-preset-tz="${content.city_tz}"
      data-preset-cc="${content.city_cc}">
<nav class="site-nav">
    <a class="brand" href="/index.html">Can I Pray Now?</a>
    <a href="/cities.html">Cities</a>
    <a href="/methodology.html">Methodology</a>
</nav>
<article class="page-wrap">
<span class="kicker">${countryName}</span>
<h1>Can I pray now in ${cityName}?</h1>

<#-- The article is the SEO payload; the app below it is the product. This
     pulls the live answer to the centre of the screen without auto-scrolling
     the reader somewhere they did not ask to go. -->
<a class="jump-to-answer" href="#answer-canvas">See the live answer for ${cityName}
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 5v14M5 12l7 7 7-7"/></svg>
</a>

<p>The five restricted windows move with the sun over ${cityName} every day: the strict sunrise,
zenith, and sunset times in which prayer is withheld, and the extended after-Fajr and after-Asr
windows in which voluntary prayer waits. A fixed timetable cannot answer &ldquo;right now&rdquo; -
the verdict below is computed astronomically for ${cityName} (${content.city_tz}) this very minute,
and follows your madhhab and what you intend to pray.</p>

<p>The tool also lays out today&rsquo;s five daily prayer times for ${cityName}, Fajr, Dhuhr, Asr,
Maghrib and Isha, on the timeline and the prayer list. The open stretches between them are when
salah, or namaz, may be prayed, and the restricted windows are when it may not; the live answer
shows where ${cityName} stands this minute.</p>

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

<p><em>The verdict below updates live for ${cityName} (${content.city_tz}) and follows your selected
madhhab and prayer type. See the
<a href="/methodology.html">methodology</a>.</em></p>
</article>
<#include "/partial/app.ftl">
</body>
</html>
