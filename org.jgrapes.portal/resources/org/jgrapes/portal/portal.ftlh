<!doctype html>

<html lang="${locale.toLanguageTag()}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  
  <!-- Portal -->
  <link rel="icon" href="${resourceUrl('theme-resource/favicon.ico')}">
  <title>${_("portalTitle")}</title>

  <!-- CSS -->
  <link rel="stylesheet" href="${resourceUrl('portal-resource/normalize-7.0.0.css')}">
  <!-- Portal -->
  <link rel="stylesheet" href="${resourceUrl('portal-resource/structure.css')}">
  <!-- jQuery / jQuery UI and extensions -->
  <link rel="stylesheet" href="${resourceUrl('theme-resource/jquery-ui' + minifiedExtension + '.css')}">
  <link rel="stylesheet" href="${resourceUrl('portal-resource/lib/jquery-ui-iconfont-2.3.2/jquery-ui-1.12.icon-font' + minifiedExtension + '.css')}">
  <link rel="stylesheet" href="${resourceUrl('portal-resource/lib/jquery-dropdown-2.0.3/jquery.dropdown' + minifiedExtension + '.css')}">
  <link rel="stylesheet" href="${resourceUrl('portal-resource/lib/font-awesome-4.7.0/css/font-awesome' + minifiedExtension + '.css')}"/>
  <link rel="stylesheet" href="${resourceUrl('portal-resource/lib/jquery-fa-loading-0.3/jquery.faloading' + minifiedExtension + '.css')}">

  <!-- Portal -->
  <link rel="stylesheet" href="${resourceUrl('theme-resource/portal.css')}">
  <style>
  body {
    min-width: 520px;
  }
  
  #dialog label, #dialog input { display:block; }
  #dialog label { margin-top: 0.5em; }
  #dialog input, #dialog textarea { width: 95%; }
  #portlet-tabs { margin-top: 1em; }
  #portlet-tabs li .ui-icon-close { float: left; margin: 0.4em 0.2em 0 0; cursor: pointer; }
  #add_tab { cursor: pointer; }
  
  </style> 
  
  <!-- Scripts(s) --> 
  <!-- JQuery and extensions -->
  <script src="${resourceUrl('portal-resource/lib/jquery-3.2.1' + minifiedExtension + '.js')}"></script>
  <!-- JQuery UI and extensions -->
  <script src="${resourceUrl('portal-resource/lib/jquery-ui-1.12.1/jquery-ui' + minifiedExtension + '.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/jquery.ui.touch-punch' + minifiedExtension + '.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/jquery-dropdown-2.0.3/jquery.dropdown.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/jquery-fa-loading-0.3/jquery.faloading' + minifiedExtension + '.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/moment-js-2.19.1/moment-with-locales' + minifiedExtension + '.js')}"></script>

  <!-- Portal -->
  <script src="${resourceUrl('portal-resource/functions.js')}"></script>

  <script>
  $( function() {
    JGPortal.init(${portalSessionRefreshInterval?c}, ${portalSessionInactivityTimeout?c});    
  } );
  </script>
  
</head>

<body>

<!-- The bar at the top of the page -->
<div class="ui-widget top-bar">
  <span class="ui-icon ui-icon-settings" data-jq-dropdown="#settings-panel">Settings</span>
  <span class="ui-icon ui-icon-addon" data-jq-dropdown="#addon-menu">Add-Ons</span>
</div>

<!-- Main content: The tabs -->
<div id="portlet-tabs">
  <ul>
    <li><a href="#portlet-tabs-1">${_("Overview")}</a></li>
  </ul>

  <div id="portlet-tabs-1" class="overview-panel">
    <div class="column">
    </div>
 
    <div class="column">
    </div>
 
    <div class="column">
    </div>

    <div style="clear:both;"></div>
    
  </div>
  
</div>

<!-- (Initially invisible) --> 

<!-- The settings panel and its sub-structures -->
<div id="settings-panel" class="jq-dropdown jq-dropdown-anchor-right">
  <ul class="jq-dropdown-panel ui-widget ui-widget-content">
    <li class="ui-menu-item" data-jq-dropdown="#language-menu">
      <div class="ui-menu-item-wrapper">${_("Language")}</div>
    </li>
    <li class="ui-menu-item" data-jq-dropdown="#theme-menu">
      <div class="ui-menu-item-wrapper">${_("Theme")}</div>
    </li>
  </ul>
</div>

<div id="language-menu" class="jq-dropdown jq-dropdown-tip jq-dropdown-scroll">
  <ul class="jq-dropdown-menu ui-menu ui-widget ui-menu-item">
  <#list supportedLanguages as l>
    <li class="ui-menu-item">
      <div class="ui-menu-item-wrapper" data-locale="${l.locale.toLanguageTag()}">${l.label}</div>
    </li>
  </#list>
  </ul>
</div>

<div id="theme-menu" class="jq-dropdown jq-dropdown-tip">
  <ul class="jq-dropdown-menu ui-menu ui-widget ui-menu-item">
  <#list themeInfos as t>
    <li class="ui-menu-item">
      <div class="ui-menu-item-wrapper" data-theme-id="${t.id()}">${t.name()}</div>
    </li>
  </#list>
  </ul>
</div>

<div id="addon-menu" class="jq-dropdown jq-dropdown-tip jq-dropdown-anchor-right">
  <ul id="addon-menu-list" class="jq-dropdown-menu ui-menu ui-widget ui-menu-item">
  </ul>
</div>

<div id="portal-session-suspended-dialog" style="display:none;"
  title="${_("Portal Session Suspended")}">
  <p>
    <span class="ui-icon ui-icon-pause" style="float:left; margin:0 7px 50px 0;"></span>
    ${_("portalSessionSuspendedMessage")}
  </p>
</div>

<div id="server-connection-lost-notification" style="display:none;">
    ${_("serverConnectionLostMessage")}
</div>

<div id="server-connection-restored-notification" style="display:none;">
    ${_("serverConnectionRestoredMessage")}
</div>

</body>
</html>