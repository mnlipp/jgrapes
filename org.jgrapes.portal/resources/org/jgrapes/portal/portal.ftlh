<!doctype html>

<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  
  <!-- Portal -->
  <link rel="shortcut icon" href="${resourceUrl('theme-resource/icons/favicon.ico')}">
  <title>${_("portalTitle")}</title>

  <!-- CSS -->
  <!-- jQuery / jQuery UI and extensions -->
  <link rel="stylesheet" href="${resourceUrl('theme-resource/jquery-ui.min.css')}"></script>
  <link rel="stylesheet" href="${resourceUrl('portal-resource/lib/jquery-ui-iconfont-2.3.2/jquery-ui-1.12.icon-font.min.css')}"></script>
  <link rel="stylesheet" href="${resourceUrl('portal-resource/lib/jquery-dropdown-2.0.3/jquery.dropdown.min.css')}"></script>

  <!-- Portal -->
  <link rel="stylesheet" href="${resourceUrl('portal-resource/normalize.css')}">
  <link rel="stylesheet" href="${resourceUrl('theme-resource/portal.css')}">
  <style>
  body {
    min-width: 520px;
  }
  
  #dialog label, #dialog input { display:block; }
  #dialog label { margin-top: 0.5em; }
  #dialog input, #dialog textarea { width: 95%; }
  #tabs { margin-top: 1em; }
  #tabs li .ui-icon-close { float: left; margin: 0.4em 0.2em 0 0; cursor: pointer; }
  #add_tab { cursor: pointer; }
  
  </style> 
  
  <!-- Scripts(s) --> 
  <!-- JQuery and extensions -->
  <script src="${resourceUrl('portal-resource/lib/jquery-3.2.1.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/jquery.simple.websocket.min.js')}"></script>
  <!-- JQuery UI and extensions -->
  <script src="${resourceUrl('portal-resource/lib/jquery-ui-1.12.1/jquery-ui.min.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/jquery.ui.touch-punch.min.js')}"></script>
  <script src="${resourceUrl('portal-resource/lib/jquery-dropdown-2.0.3/jquery.dropdown.js')}"></script>

  <!-- Portal -->
  <script src="${resourceUrl('portal-resource/functions.js')}"></script>

  <script>
  $( function() {
    // Top bar
    $( "#theme-menu" ).on("click", "[data-theme-id]", function() {
      JGPortal.sendSetTheme($(this).data("theme-id"));
      $( "#theme-menu" ).jqDropdown("hide");
    });
    
    $( "#language-menu" ).on("click", "[data-locale]", function() {
      JGPortal.sendSetLocale($(this).data("locale"));
      $( "#theme-menu" ).jqDropdown("hide");
    });
    
    $( "#addon-menu" ).on("click", "[data-portlet-type]", function() {
      JGPortal.sendAddPortlet($(this).data("portlet-type"));
      $( "#theme-menu" ).jqDropdown("hide");
    });
    
    // Tabs
    $( "#tabs" ).tabs();
    
    // AddTab button
    var tabs = $( "#tabs" ).tabs();

    // Close icon: removing the tab on click
    tabs.on( "click", "span.ui-icon-close", function() {
      var panelId = $( this ).closest( "li" ).remove().attr( "aria-controls" );
      $( "#" + panelId ).remove();
      tabs.tabs( "refresh" );
      JGPortal.layoutChanged();
    });

    //
    // Portlet Grid
    //
    
    // Prepare columns
    $( ".column" ).sortable({
      connectWith: ".column",
      handle: ".portlet-header",
      cancel: ".portlet-toggle",
      placeholder: "portlet-placeholder ui-corner-all",
      stop: function( event, ui ) { JGPortal.layoutChanged(); }
    });
 
    // With everything prepared, send portal ready
    JGPortal.sendPortalReady();

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
<div id="tabs">
  <ul>
    <li><a href="#tabs-1">${_("Overview")}</a></li>
  </ul>

  <div id="tabs-1" class="overview-panel">
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
  <div class="jq-dropdown-panel ui-widget-content">
    <div data-jq-dropdown="#language-menu">${_("Language")}</div>
    <div data-jq-dropdown="#theme-menu">${_("Theme")}</div>
  </div>
</div>

<div id="language-menu" class="jq-dropdown jq-dropdown-tip jq-dropdown-scroll">
  <ul class="jq-dropdown-menu ui-menu ui-widget ui-menu-item">
  <#list supportedLanguages as l>
    <li class="ui-menu-item">
      <div class="ui-menu-item-wrapper" data-locale="${l.locale}">${l.label}</div>
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

</body>
</html>