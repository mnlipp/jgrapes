'use strict';

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
var JGPortal = {

};

(function () {

    var portletFunctionRegistry = {};
    
    JGPortal.registerPortletMethod = function(portletClass, methodName, method) {
        let classRegistry = portletFunctionRegistry[portletClass];
        if (!classRegistry) {
            classRegistry = {};
            portletFunctionRegistry[portletClass] = classRegistry;
        }
        classRegistry[methodName] = method;
    }
    
    function invokePortletMethod(portletClass, portletId, method, params) {
        let f = portletFunctionRegistry[portletClass][method];
        if (f) {
            f(portletId, params);
        }
    }
    
    function addPortletResources(cssUris, scriptUris) {
        for (let index in cssUris) {
            let uri = cssUris[index];
            if ($("head > link[href='" + uri + "']").length === 0) {
                $("head link[href$='/portal.css']:last").after("<link rel='stylesheet' href='" + uri + "'>");
            }
        }
        // Don't use jquery, https://stackoverflow.com/questions/610995/cant-append-script-element
        pendingResourcesCallbacks = scriptUris.length;
        if (pendingResourcesCallbacks === 0) {
            messageHandled();
        }
        for (let index in scriptUris) {
            let uri = scriptUris[index];
            if ($("head > script[src='" + uri + "']").length === 0) {
                let script = document.createElement("script");
                script.src = uri;
                script.addEventListener('load', function() {
                    if (--pendingResourcesCallbacks === 0) {
                        messageHandled();
                    }
                });
                let head = $("head").get()[0];
                head.appendChild(script);
            }
        }
    };
    addPortletResources.callsBackMessageHandled = true;

    function findPortletPreview(portletId) {
        let matches = $( ".portlet[data-portletId='" + portletId + "']" );
        if (matches.length === 1) {
            return $( matches[0] );
        }
        return undefined;
    };
    JGPortal.findPortletPreview = findPortletPreview;
    
    function findPortletView(portletId) {
        let tabs = $( "#tabs" ).tabs();
        let matches = tabs.find("> div[data-portletId='" + portletId + "']" );
        if (matches.length === 1) {
            return $( matches[0] );
        }
        return undefined;
    };
    JGPortal.findPortletView = findPortletView;
    
	function updatePreview(portletId, title, modes, content) {
		let portlet = findPortletPreview(portletId);
		if (!portlet) {
			portlet = $( '<div class="portlet">\
<div class="portlet-header"><span class="portlet-header-text"></span></div>\
<div class="portlet-content"></div>\
</div>');
			portlet.attr("data-portletId", portletId);
			portlet.addClass( "ui-widget ui-widget-content ui-helper-clearfix ui-corner-all" );
			let portletHeader = portlet.find( ".portlet-header" );
			portletHeader.addClass( "ui-widget-header ui-corner-all" );
			if (modes.includes("View")) {
				portletHeader.prepend( "<span class='ui-icon ui-icon-fullscreen portlet-expand'></span>");
				portletHeader.find(".portlet-expand").on( "click", function() {
					let icon = $( this );
					let portlet = icon.closest( ".portlet" );
					JGPortal.sendRenderPortlet(portlet.attr("data-portletId"), "View");
				});
			}
			$( ".column" ).first().prepend(portlet);
		}
		let portletHeaderText = portlet.find(".portlet-header-text");
		portletHeaderText.text(title);
		let portletContent = portlet.find(".portlet-content");
		portletContent.children().detach();
		portletContent.append($(content));
	};

    var tabTemplate = "<li><a href='@{href}'>@{label}</a> " +
        "<span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>";
    var tabCounter = 1;

	function updateView(portletId, title, modes, content) {
		let tabs = $( "#tabs" ).tabs();
        let portletIndex;
		let matches = tabs.find("> div").filter(function(index) {
			if ($(this).attr("data-portletId") === portletId) {
			    portletIndex = index;
			    return true;
			} else {
			    return false;
			}
		});
		let portletView;
		if (matches.length === 1) {
			portletView = $( matches[0] );
	        portletView.children().detach();
	        portletView.append($(content));
		} else {
			tabCounter += 1;
	        let id = "tabs-" + tabCounter,
	          li = $( tabTemplate.replace( /@\{href\}/g, "#" + id ).replace( /@\{label\}/g, title ) );
	        let tabItems = tabs.find( ".ui-tabs-nav" );
	        tabItems.append( li );
	        portletView = $( "<div id='" + id + "'>" + content + "</div>" )
			portletView.attr("data-portletId", portletId);
	        tabs.append( portletView );
	        tabs.tabs( "refresh" );
	        portletIndex = tabItems.find("li").length - 1;
		}
        tabs.tabs( "option", "active", portletIndex );
	}
	
	function updatePortlet(portletId, title, mode, modes, content) {
		if (mode === "Preview") {
			updatePreview(portletId, title, modes, content);
		} else if (mode === "View") {
			updateView(portletId, title, modes, content);
		}
	};

	var pendingResourcesCallbacks = 0;
	
	var messageHandlers = {
	    'addPortletResources': addPortletResources,
	    'invokePortletMethod': invokePortletMethod,
		'reload': function() { window.location.reload(true); },
        'updatePortlet': updatePortlet,
	};
	
	var messageQueue = [];
	
	function handleNextMessage () {
	    var message = messageQueue[0]; 
	    var handler = messageHandlers[message.method];
        if (!handler) {
            return;
        }
        if (message.hasOwnProperty("params")) {
            handler(...message.params);
        } else {
            handler();
        }
        if (!handler.callsBackMessageHandled) {
            messageHandled();
        }
	};

	function messageHandled() {
	    messageQueue.shift();
	    if (messageQueue.length !== 0) {
	        handleNextMessage();
	    }
	};
	
	JGPortal.handleMessage = function(message) {
	    messageQueue.push(message);
	    if (messageQueue.length === 1) {
	        handleNextMessage();
	    }
    };
	
})();

(function() {
	var wsConn;
	
	var loc = (window.location.protocol === "https:" ? "wss:" : "ws") +
		"//" + window.location.host + window.location.pathname;
	wsConn = $.simpleWebSocket({ "url": loc })
			.listen(JGPortal.handleMessage);
	wsConn.connect();

	JGPortal.sendPortalReady = function() {
		wsConn.send({"jsonrpc": "2.0", "method": "portalReady"});
	};
	
    JGPortal.sendSetTheme = function(themeId) {
        wsConn.send({"jsonrpc": "2.0", "method": "setTheme",
            "params": [ themeId ]});
    };
    
	JGPortal.sendRenderPortlet = function(portletId, mode) {
		wsConn.send({"jsonrpc": "2.0", "method": "renderPortlet",
			"params": [ portletId, mode ]});
	};
    
    JGPortal.sendToPortlet = function(portletId, method, params) {
        if (params === undefined) {
            wsConn.send({"jsonrpc": "2.0", "method": "sendToPortlet",
                "params": [ portletId, method ]});
        } else {
            wsConn.send({"jsonrpc": "2.0", "method": "sendToPortlet",
                "params": [ portletId, method, params ]});
        }
    };
})();
