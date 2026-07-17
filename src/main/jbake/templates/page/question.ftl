<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  Question page (type=question, spec §8.2): a direct answer for the long-tail
  query, then the full live app hydrating below - the reader gets the ruling
  for their own location without leaving the page.

  @author Fahim Farook
-->
<!DOCTYPE html>
<html lang="en">
<head>
<#include "/partial/header.ftl">
</head>
<body class="state-neutral">
<nav class="site-nav">
    <a class="brand" href="/index.html">Can I Pray Now?</a>
    <a href="/q/index.html">Questions</a>
    <a href="/methodology.html">Methodology</a>
</nav>
<article class="page-wrap">
<span class="kicker"><a href="/q/index.html">Question</a></span>
${content.body}
<p><em>Rulings differ by madhhab and by what you intend to pray - the live answer below is conditioned on both. Draft pending scholar verification; see the <a href="/methodology.html">methodology</a>.</em></p>
</article>
<#include "/partial/app.ftl">
</body>
</html>
