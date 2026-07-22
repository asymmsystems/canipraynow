<#--
  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.

  TEMPORARY - controlled experiment, to be reverted.

  The HalalSpark footer placement, deliberately a plain markup script tag rather
  than the lazy JS injection used for the sidebar slot (assets/js/ui/ads.js).
  Purpose: isolate whether injection method or slot viewability explains the
  sidebar placement's fill and earnings. Because it is markup, it bypasses the
  data/ads.json kill switch and the idle-time delay - which is the point of the
  test, and the reason this must not outlive it.

  Revert = delete this file and its six <#include> lines in templates/page/.

  @author Fahim Farook
-->
<!-- HalalSpark Enhanced Ad Script v3.0 - footer -->
<script src="https://ads.halalspark.co.uk/get-ad-script-footer?placement_id=d0df402f-3dd4-43e6-98fd-ddfd7be718f2&domain=https://canipraynow.com&_v=3.0" async></script>
