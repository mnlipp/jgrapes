'use strict';

(function() {

    JGPortal.registerPortletMethod(
            "org.jgrapes.portlets.markdowndisplay.MarkdownDisplayPortlet",
            "updateAll", function(portletId, params) {
                let portlet = JGPortal.findPortletPreview(portletId);
                if (portlet) {
                    let content = portlet.find(".jgrapes-markdownportlet-content");
                    content.empty();
                    content.append(params[1]);
                }
                portlet = JGPortal.findPortletView(portletId);
                if (portlet) {
                    let content = portlet.find(".jgrapes-markdownportlet-content");
                    content.empty();
                    content.append(params[2]);
                }
            });

})();

