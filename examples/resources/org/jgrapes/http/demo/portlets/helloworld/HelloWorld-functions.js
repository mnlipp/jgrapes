'use strict';

(function() {

    $("body").on("click", ".HelloWorld-view .HelloWorld-toggle",
            function(event) {
        let portletId = $(this).closest("[data-portlet-id]").attr("data-portlet-id");
        JGPortal.sendToPortlet(portletId, "toggleWorld");
    })

    JGPortal.registerPortletMethod(
            "org.jgrapes.http.demo.portlets.helloworld.HelloWorldPortlet",
            "setWorldVisible", function(portletId, params) {
                let portlet = JGPortal.findPortletView(portletId);
                let image = portlet.find(".helloWorldIcon");
                let state = params[0]; 
                if (params[0]) {
                    image.show();
                } else {
                    image.hide();
                }
            });
    
})();

