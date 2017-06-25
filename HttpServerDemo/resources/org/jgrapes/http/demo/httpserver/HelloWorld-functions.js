'use strict';

var org_jgrapes_http_demo_HelloWorldPortlet = {

};

(function() {

    $("body").on("click", ".HelloWorld-view .HelloWorld-toggle",
            function(event) {
        let portletId = $(this).closest("[data-portletId]").attr("data-portletId");
        JGPortal.sendToPortlet(portletId, "toggleWorld");
    })

})();

