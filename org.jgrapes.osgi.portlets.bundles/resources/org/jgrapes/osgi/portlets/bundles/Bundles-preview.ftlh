<div data-portletTitle="${_("portletName")}">${_("portletName")} 
<#list portlet.bundles as bundle>
  <ul>
    <li>${bundle.symbolicName}</li>
  </ul>
</#list>
</div>

