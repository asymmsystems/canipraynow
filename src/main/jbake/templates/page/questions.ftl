<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  Question index (type=questionsindex). Reads the question pages straight from
  JBake's data model, the same way page/cities.ftl reads the city set - one
  page from one dataset is what a template is for.

  This page exists for the reason page/cities.ftl states for the cities: without
  it the /q/ pages carry no internal links at all and are reachable only through
  sitemap.xml. It is linked from the footer of every page, so every question is
  one hop from anywhere on the site.

  The one-line teaser under each link is the page's own meta description, so the
  hub answers the question set at a glance instead of being a bare link list.

  @author Fahim Farook
-->
<#assign metaTitle = "Can I Pray Now? Answers on the Prohibited Prayer Times">
<#assign metaDescription = "Direct answers to the common questions about the prohibited and disliked times for prayer (awqat al-nahy): zawal, isfirar, after Fajr, after Asr, and what may still be prayed in each.">
<#assign questions = published_content?filter(c -> (c.type!"") == "question")?sort_by("title")>
<!DOCTYPE html>
<html lang="en">
<head>
<#include "/partial/header.ftl">
</head>
<body class="page">
<nav class="site-nav">
    <a class="brand" href="/index.html">Can I Pray Now?</a>
    <a href="/cities.html">Cities</a>
    <a href="/methodology.html">Methodology</a>
</nav>
<article class="page-wrap">
<span class="kicker">Questions</span>
<h1>Questions about the prohibited times</h1>
<p>The times in which prayer is withheld are fixed in the sources but move with the sun, and what
they forbid depends on your madhhab and on what you intend to pray. These are the questions that
follow from that, answered directly.</p>

<a class="widget-cta" href="/index.html">Can I pray now?<span>The live answer for your own location.</span></a>

<ul class="q-index">
<#list questions as q>
    <#assign teaser = (q.description)!"">
    <li>
        <a href="/${q.uri}">
            <strong>${q.title?html}</strong>
            <#if teaser?has_content><span>${teaser?html}</span></#if>
        </a>
    </li>
</#list>
</ul>
</article>
<footer class="app-footer">
    <span>&#169; 2026 Asymm Systems (Pvt) Ltd &#183; <a href="/privacy.html">Privacy</a> &#183; <a href="/methodology.html">Methodology</a> &#183; <a href="/cities.html">Cities</a></span>
</footer>
</body>
</html>
