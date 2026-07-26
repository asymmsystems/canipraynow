<#--
  Copyright (c) 2026 Asymm Systems. All rights reserved.
  The full generated city content file: JBake front-matter, then (for authority-table
  cities) the baked timetable. Driven by CityPageRenderer. @author Fahim Farook
-->
title=Can I pray now in ${city.name}?
type=city
status=published
${marker}
city_name=${city.name}
country_name=${city.country}
city_lat=${city.lat}
city_lng=${city.lng}
city_tz=${city.tz}
city_cc=${city.cc}
nearby=<#list nearby as n>${n.name}|${n.country}|${n.href}|${n.km}<#if n?has_next>;</#if></#list>
city_authority=${countryAuthority}
city_method=${countryMethod}
city_madhhab=${countryMadhhab}
~~~~~~
<#if months??>
<#include "timetable.ftl">
</#if>
