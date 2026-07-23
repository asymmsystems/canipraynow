<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
<#list published_content as content>
    <#-- 404 is not a content page. -->
    <#if content.uri != "404.html">
    <#assign loc = content.uri>
    <#if loc?ends_with("index.html")><#assign loc = loc?substring(0, loc?length - 10)><#elseif loc?ends_with(".html")><#assign loc = loc?substring(0, loc?length - 5)></#if>
    <url>
        <loc>${config.site_host}/${loc}</loc>
        <#if content.date??><lastmod>${content.date?string("yyyy-MM-dd")}</lastmod></#if>
    </url>
    </#if>
</#list>
</urlset>
