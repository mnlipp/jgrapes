<div class="HelloWorld-view">
    <div style="font-size: 400%">Hello World! 
        <img 
        <#if portlet.worldVisible>
            style='vertical-align: bottom;'
        <#else>
            style='vertical-align: bottom; display: none;'
        </#if>
            src="${portletResource('globe#1.ico')}">
        <!-- World icon from http://apathae.deviantart.com/ -->
    </div>
    <div>
        <button class="HelloWorld-toggle ui-button ui-widget ui-corner-all">Toggle World</button>
    </div>
</div>