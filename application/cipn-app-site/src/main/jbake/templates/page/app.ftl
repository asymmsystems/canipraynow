<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
  Home page (type=app): nothing but the live app. The answer IS the page.
  @author Fahim Farook
-->
<!DOCTYPE html>
<html lang="en">
<head>
<#include "/partial/header.ftl">
</head>
<body class="state-neutral" data-router>
<h1 class="visually-hidden">Can I pray now? Live prohibited prayer times, and today&rsquo;s salah (salat, namaz) times, for your location</h1>
<#include "/partial/app.ftl">
${content.body}
<#-- Muslim Ads Network site-verification event snippet for https://canipraynow.com/ (the root,
     type=app home page). Wrapped in noparse: the vendor noscript URL contains ${GDPR} tokens that
     are NOT FreeMarker variables and must be emitted literally. -->
<#noparse>
<!--
Event snippet for canipraynow.com - Site Verification on https://canipraynow.com/: Please do not remove.
Place this snippet on pages with events you are tracking.
Creation date: 11/11/2026
-->
<script>
  gtag('event', 'conversion', {
    'allow_custom_scripts': true,
    'send_to': 'DC-16361189/manau0/canip0+standard'
  });
</script>
<noscript>
<img src="https://ad.doubleclick.net/ddm/activity/src=16361189;type=manau0;cat=canip0;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;npa=;gdpr=${GDPR};gdpr_consent=${GDPR_CONSENT_755};ord=1?" width="1" height="1" alt=""/>
</noscript>
<!-- End of event snippet: Please do not remove -->
</#noparse>
</body>
</html>
