'use strict';

(function() {
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.portlets.sysinfo.SysInfoPortlet",
            "updateMemorySizes", function(portletId, params) {
                let portlet = JGPortal.findPortletPreview(portletId);
                if (portlet) {
                    let col = portlet.find(".maxMemory");
                    col.html(params[0]);
                    col = portlet.find(".totalMemory");
                    col.html(params[1]);
                    col = portlet.find(".freeMemory");
                    col.html(params[2]);
                }
            });

})();

