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
<#-- QAPage so the question and its answer are machine-legible as Q&A, not
     inferred from prose. acceptedAnswer.text uses the page's own summary
     (plain text); the full answer stays in the visible body for readers. -->
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "QAPage",
  "mainEntity": {
    "@type": "Question",
    "name": "${content.title?json_string}",
    "text": "${content.title?json_string}",
    "answerCount": 1,
    "acceptedAnswer": {
      "@type": "Answer",
      "text": "${(content.description!"")?json_string}",
      "url": "${canonicalUrl}"
    }
  }
}
</script>
</head>
<body class="state-neutral q-page">
<nav class="site-nav">
    <a class="brand" href="/index.html">Can I Pray Now?</a>
    <a href="/q/index.html">Questions</a>
    <a href="/methodology.html">Rulings &amp; evidence</a>
</nav>
<#--
  The question is its OWN screen and the live answer is the next one, so the
  reader isn't handed the ruling and its long-tail explanation crammed into one
  viewport. We split the body at its "Sources" heading (every question page has
  exactly one): the question + explanation form the full-height hero, the
  citations move BELOW the interactive answer as supporting material. If the
  heading ever changes, the split simply no-ops and the whole body stays in the
  hero - degraded, never broken.
-->
<#assign parts = content.body?split("<h2>Sources</h2>")>
<article class="page-wrap q-hero">
<span class="kicker"><a href="/q/index.html">Question</a></span>
${parts[0]}
<p class="q-lead"><em>Rulings differ by madhhab and by what you intend to pray - the live answer below is conditioned on both. New to the terms? See <a href="/methods.html">how prayer times work</a> and the <a href="/methodology.html">rulings &amp; evidence</a>.</em></p>
<a class="scroll-cue" href="#answer-canvas" aria-label="Scroll to the live answer">
    <span>See the live answer</span>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 5v14M5 12l7 7 7-7"/></svg>
</a>
</article>
<#include "/partial/app.ftl">
<#if parts?size gt 1>
<section class="page-wrap q-sources">
<h2>Sources</h2>${parts[1]}
</section>
</#if>
<#include "/partial/ad-footer.ftl">
</body>
</html>
