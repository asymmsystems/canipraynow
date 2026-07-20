<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  Document head, included by every page template. SEO tags derive from
  per-page front matter with site-level fallbacks (same pattern as
  asymm-corporate). Canonical URLs are the clean (extensionless) form
  Cloudflare Pages serves.

  Front-matter knobs (all optional):
    seo_title    complete <title> override (else "${title} - Can I Pray Now?")
    description  per-page meta + og + twitter description

  A calling template may instead <#assign metaTitle / metaDescription> before
  including this file and they win - that is how page/city.ftl keeps its SEO
  copy in the template rather than in the generated front-matter stub.

  @author Fahim Farook
-->
<#if !(metaTitle??)>
  <#if (content.seo_title)??>
    <#assign metaTitle = content.seo_title>
  <#elseif (content.title)??>
    <#assign metaTitle = content.title + " - Can I Pray Now?">
  <#else>
    <#assign metaTitle = "Can I Pray Now?">
  </#if>
</#if>
<#if !(metaDescription??)>
  <#assign metaDescription = (content.description)!config.site_description>
</#if>
<#assign pageUri = (content.uri)!"">
<#if pageUri?ends_with("index.html")>
  <#assign pageUri = pageUri?substring(0, pageUri?length - 10)>
<#elseif pageUri?ends_with(".html")>
  <#assign pageUri = pageUri?substring(0, pageUri?length - 5)>
</#if>
<#assign canonicalUrl = config.site_host + "/" + pageUri>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${metaTitle}</title>
<meta name="description" content="${metaDescription}">
<link rel="canonical" href="${canonicalUrl}">
<meta property="og:title" content="${metaTitle}">
<meta property="og:description" content="${metaDescription}">
<meta property="og:type" content="website">
<meta property="og:url" content="${canonicalUrl}">
<meta property="og:image" content="${config.site_host}/images/og-sun-ruler.svg">
<meta name="twitter:card" content="summary_large_image">
<meta name="theme-color" content="#064e3b">
<link rel="icon" href="/favicon.ico" sizes="any">
<link rel="icon" href="/images/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/images/icon-192.svg">
<link rel="manifest" href="/manifest.webmanifest">
<link rel="preload" href="/fonts/inter-latin-wght-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="/css/style.css">
<#-- TechArticle for reference pages (front matter: schema_type=TechArticle). Reuses the
     already-computed metaTitle / metaDescription / canonicalUrl. -->
<#if (content.schema_type)?? && content.schema_type == "TechArticle">
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "TechArticle",
  "headline": "${metaTitle?json_string}",
  "description": "${metaDescription?json_string}",
  "url": "${canonicalUrl}",
  "mainEntityOfPage": "${canonicalUrl}",
  "publisher": { "@type": "Organization", "name": "Can I Pray Now?", "url": "https://canipraynow.com/" }
}
</script>
</#if>
