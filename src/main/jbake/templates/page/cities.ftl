<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  City index (type=citiesindex). Reads assets/data/cities.json straight from
  JBake's data model (data.folder=assets/data) - no generator involved, because
  this is ONE page from one dataset, which is exactly what a template does.
  Only the 110-way fan-out needs a program.

  Without this page the city pages are orphans, reachable only via sitemap.xml
  and with no internal links: the classic doorway-page signature that search
  engines discount. It is linked from the footer of every page.

  @author Fahim Farook
-->
<#assign metaTitle = "Can I Pray Now? Prohibited Prayer Times by City">
<#assign metaDescription = "Is it permissible to pray right now? Live prohibited and disliked prayer times (awqat al-nahy) for cities worldwide, by madhhab.">
<#assign paged = published_content?filter(c -> (c.type!"") == "city")?sort_by("city_name")?sort_by("country_name")>
<!DOCTYPE html>
<html lang="en">
<head>
<#include "/partial/header.ftl">
</head>
<body class="page">
<nav class="site-nav">
    <a class="brand" href="/index.html">Can I Pray Now?</a>
    <a href="/methodology.html">Rulings &amp; evidence</a>
</nav>
<article class="page-wrap">
<span class="kicker">Cities</span>
<h1>Prayer times by city</h1>
<p>The prohibited and disliked times for prayer move with the sun, so they differ in every city and
on every day. Choose a city for its live answer, or use the tool itself, which follows wherever you
are.</p>

<a class="widget-cta" href="/index.html">Can I pray now?<span>The live answer for your own location.</span></a>

<div class="city-filter">
    <label class="visually-hidden" for="city-filter-input">Filter cities</label>
    <input type="search" id="city-filter-input" placeholder="Filter ${paged?size} cities&hellip;" autocomplete="off">
</div>
<p class="city-empty" id="city-filter-empty" hidden>No city matches that.</p>

<#-- Grouped by country, alphabetical within each. ?sort_by keeps the output
     byte-identical across rebuilds regardless of the dataset's own order. -->
<#assign seen = []>
<#list paged as c>
    <#if !seen?seq_contains(c.country_name)>
        <#assign seen = seen + [c.country_name]>
        <#if seen?size gt 1></ul></div></#if>
        <div class="country-group" data-country="${c.country_name?html}">
        <h2 class="country-head">${c.country_name?html}</h2>
        <ul class="city-index">
    </#if>
    <li data-city="${c.city_name?lower_case?html} ${c.country_name?lower_case?html}"><a href="/${c.uri}">Can I pray now in ${c.city_name?html}?</a></li>
    <#if !c_has_next></ul></div></#if>
</#list>
</article>
<footer class="app-footer">
    <span>&#169; 2026 Asymm Systems (Pvt) Ltd &#183; <a href="/privacy.html">Privacy</a> &#183; <a href="/methods.html">Prayer times</a> &#183; <a href="/methodology.html">Rulings &amp; evidence</a> &#183; <a href="/q/index.html">Questions</a></span>
</footer>
<script src="/js/cities.js" defer></script>
</body>
</html>
