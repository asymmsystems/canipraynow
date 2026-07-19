<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  The live app shell (spec §5.2-5.6): Status Canvas + chips + Sun Ruler +
  sidebar. Shared by the home page and the city/question SEO pages; main.js
  hydrates it.

  @author Fahim Farook
-->
<#-- Location slot: the city is a control. Tapping it swaps to an inline
     search IN PLACE (no full-screen overlay). Same markup everywhere it
     appears, wired by initLocationSlot() so behaviour is identical. -->
<#macro locationSlot id extra="">
    <div class="location-slot ${extra}" id="${id}">
        <button type="button" class="location-chip">
            <svg class="pin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11z"/>
                <circle cx="12" cy="10" r="2.5"/>
            </svg>
            <span class="location-name"></span>
            <span class="location-change">Change</span>
        </button>
        <div class="location-search" hidden>
            <input type="search" placeholder="Search for a city" autocomplete="off" aria-label="Search for a city">
            <div class="location-dropdown">
                <ul class="city-results"></ul>
                <button type="button" class="btn-secondary location-detect">Use my location</button>
                <p class="setting-note location-note" hidden></p>
            </div>
        </div>
    </div>
</#macro>

<div class="app-shell" id="answer-canvas">
    <main class="canvas">
        <@locationSlot id="location-slot-canvas" extra="location-slot-canvas"/>

        <p class="status-kicker" id="q">Can I pray now?</p>
        <div class="status" role="status" aria-live="polite">
            <p class="answer" id="answer">&#183;&#183;&#183;</p>
            <#-- Which obligatory prayer's time we are actually in. The big
                 answer is about the SELECTED prayer type; this anchors it to
                 the clock so YES/NO never floats free of context. -->
            <p class="now-prayer" id="now-prayer"></p>
            <p class="reason" id="reason">&#8230;</p>
            <p class="status-notes" id="status-notes" hidden></p>
            <p class="countdown" id="countdown" hidden><span id="countdown-label"></span> <strong id="countdown-value"></strong></p>
        </div>
        <div class="chips" id="chips"></div>
        <#-- Safety caption (spec §5.4): the big answer is about the SELECTED
             prayer type, which defaults to voluntary. Hidden by main.js when
             "Today's fard" is selected, where it would be noise. -->
        <p class="fard-note" id="fard-note"></p>
        <#-- No role here: the inner <svg role="img"> carries the summary, and
             the hover rects are aria-hidden with an equivalent list beside
             them (a role="img" figure would prune both from the a11y tree). -->
        <figure class="ruler" id="ruler"></figure>
    </main>
    <aside class="sidebar">
        <#-- The ayah that grounds the whole product: prayer is enjoined at
             fixed times. It lives in the side pane, where it frames the tool
             without competing with the answer for the eye. -->
        <figure class="ayah">
            <p class="ayah-ar" lang="ar" dir="rtl">إِنَّ ٱلصَّلَوٰةَ كَانَتْ عَلَى ٱلْمُؤْمِنِينَ كِتَٰبًا مَّوْقُوتًا</p>
            <figcaption class="ayah-en">
                <span>&#8220;Indeed, prayer has been decreed upon the believers a timed prescription.&#8221;</span>
                <cite>an-Nis&#257;ʾ 4:103</cite>
            </figcaption>
        </figure>

        <@locationSlot id="location-slot-sidebar" extra="location-slot-sidebar"/>
        <section id="prayers" aria-label="Today's prayers"></section>
        <section id="sponsor" hidden aria-label="Sponsored"></section>
        <#-- Ad slot (spec §9 Phase B): below the fold on mobile, below the
             prayer list on desktop - never above or inside the Status Canvas.
             Height reserved so a late-loading unit cannot shift layout. -->
        <div class="ad-slot" id="ad-slot-1" data-ad-slot hidden aria-label="Advertisement"></div>
        <footer class="app-footer">
            <span id="ruleset-version"></span>
            <br><a href="/methods.html">Prayer times</a> &#183; <a href="/methodology.html">Rulings &amp; evidence</a> &#183; <a href="/cities.html">Cities</a> &#183; <a href="/q/index.html">Questions</a> &#183; <a href="/privacy.html">Privacy</a>
        </footer>
    </aside>
</div>

<button type="button" id="settings-btn" class="settings-btn" aria-label="Settings" aria-expanded="false" aria-controls="settings-drawer">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
        <circle cx="12" cy="12" r="3.2"/>
        <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1.03 1.56V21a2 2 0 1 1-4 0v-.09A1.7 1.7 0 0 0 8.9 19.4a1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.7 1.7 0 0 0 .34-1.87 1.7 1.7 0 0 0-1.56-1.03H3a2 2 0 1 1 0-4h.09A1.7 1.7 0 0 0 4.6 8.9a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.87.34H9A1.7 1.7 0 0 0 10 3.09V3a2 2 0 1 1 4 0v.09a1.7 1.7 0 0 0 1.03 1.56 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.87V9c.66.29 1.11.93 1.11 1.66V12a2 2 0 0 1-2 2h-.09a1.7 1.7 0 0 0-1.01 1z"/>
    </svg>
</button>

<div id="settings-drawer" class="drawer" aria-hidden="true" role="dialog" aria-label="Settings"></div>
<div id="toast" class="toast" role="status"></div>

<noscript>
    <div class="notice-banner">This tool needs JavaScript to compute prayer windows for your location.</div>
</noscript>

<script type="module" src="/js/main.js"></script>
