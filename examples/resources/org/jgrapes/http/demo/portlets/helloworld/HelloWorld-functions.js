/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
 
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

