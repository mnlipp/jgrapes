<div data-portletTitle="${_("portletName")}" class="jgrapes-osgi-bundles-preview"
    data-appendContentHandler="tooltips"> 
  <table>
    <tr>
      <th>${_("bundleId")}</th>
      <th>${_("bundleName")}</th>
    </tr>
    <#list portlet.bundles() as bundle>
    <tr data-tooltipId="${"BundlesPreviewTooltip-" + bundle.bundleId}">
      <td>${bundle.bundleId}</td>
      <td>${bundle.getHeaders(locale).get("Bundle-Name")!(bundle.symbolicName)}</td>
    </tr>
    </#list>
  </table>
</div>

<#list portlet.bundles() as bundle>
<div id="${"BundlesPreviewTooltip-" + bundle.bundleId}" 
    class="jgrapes-tooltip-prototype">
  <table>
    <tr>
      <td>${_("bundleSymbolicName")}:</td>
      <td>${bundle.symbolicName}</td>
    </tr>
    <tr>
      <td>${_("bundleVersion")}:</td>
      <td>${bundle.version}</td>
    </tr>
    <tr>
      <td>${_("bundleCategory")}:</td>
      <td>${bundle.getHeaders(locale).get("Bundle-Category")!""}</td>
    </tr>
    <tr>
      <td>${_("bundleState")}:</td>
      <td>${_("bundleState_" + bundle.state)}</td>
    </tr>
  </table>
</div>
</#list>
  
<script>
    JGPortal.tooltip($( ".jgrapes-osgi-bundles-preview" ));
    $( ".jgrapes-osgi-bundles-preview" ).tooltip( "option", "classes.ui-tooltip", "ui-corner-all ui-widget-shadow jgrapes-osgi-bundles-preview-tooltip" );
</script>
