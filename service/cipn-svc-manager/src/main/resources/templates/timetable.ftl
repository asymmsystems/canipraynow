<#--
  Copyright (c) 2026 Asymm Systems. All rights reserved.
  Standalone (non-JBake) template: renders a baked authority timetable into a
  month-sectioned HTML fragment. Driven by TimetableViewModel. @author Fahim Farook
-->
<section class="authority-timetable">
<h2>${authority} annual timetable - ${zone}</h2>
<p class="timetable-note">Published times from ${authority}, served exactly (not computed). Source: <a href="${url}" rel="nofollow">${authorityCode}</a>.</p>
<#list months as month>
<details class="timetable-month">
<summary>${month.name}</summary>
<div class="table-scroll">
<table>
<tr><th>Day</th><th>Fajr</th><th>Sunrise</th><th>Dhuhr</th><th>Asr</th><th>Maghrib</th><th>Isha</th></tr>
<#list month.days as d>
<tr><td>${d.day}</td><td>${d.fajr}</td><td>${d.sunrise}</td><td>${d.dhuhr}</td><td>${d.asr}</td><td>${d.maghrib}</td><td>${d.isha}</td></tr>
</#list>
</table>
</div>
</details>
</#list>
</section>
