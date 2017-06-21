/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
JGPortal = {

};

(function () {

	function updatePreview(portletId, title, modes, content) {
		var matches = $( ".portlet" ).filter(function(index) {
			return $(this).data("portletId") === portletId;
		});
		var portlet;
		if (matches.length === 1) {
			portlet = $( matches[0] );
		} else {
			portlet = $( '<div class="portlet">\
<div class="portlet-header"><span class="portlet-header-text"></span></div>\
<div class="portlet-content"></div>\
</div>');
			portlet.data("portletId", portletId);
			portlet.addClass( "ui-widget ui-widget-content ui-helper-clearfix ui-corner-all" );
			var portletHeader = portlet.find( ".portlet-header" );
			portletHeader.addClass( "ui-widget-header ui-corner-all" );
			if (modes.includes("View")) {
				portletHeader.prepend( "<span class='ui-icon ui-icon-fullscreen portlet-expand'></span>");
				portletHeader.find(".portlet-expand").on( "click", function() {
					var icon = $( this );
					var portlet = icon.closest( ".portlet" );
					JGPortal.sendRenderPortlet(portlet.data("portletId"), "View");
				});
			}
			$( ".column" ).first().prepend(portlet);
		}
		var portletHeaderText = portlet.find(".portlet-header-text");
		portletHeaderText.text(title);
		var portletContent = portlet.find(".portlet-content");
		portletContent.children().detach();
		portletContent.append($(content));
	};

    var tabTemplate = "<li><a href='@{href}'>@{label}</a> " +
    		"<span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>";
	var tabCounter = 1;

	function updateView(portletId, title, modes, content) {
		var tabs = $( "#tabs" ).tabs();
        var portletIndex;
		var matches = tabs.find("> div").filter(function(index) {
			if ($(this).data("portletId") === portletId) {
			    portletIndex = index;
			    return true;
			} else {
			    return false;
			}
		});
		var portletView;
		if (matches.length === 1) {
			portletView = $( matches[0] );
		} else {
			tabCounter += 1;
	        var id = "tabs-" + tabCounter,
	          li = $( tabTemplate.replace( /@\{href\}/g, "#" + id ).replace( /@\{label\}/g, title ) );
	        var tabItems = tabs.find( ".ui-tabs-nav" );
	        tabItems.append( li );
	        portletView = $( "<div id='" + id + "'>" + content + "</div>" )
			portletView.data("portletId", portletId);
	        tabs.append( portletView );
	        tabs.tabs( "refresh" );
	        portletIndex = tabItems.find("li").length - 1;
		}
        tabs.tabs( "option", "active", portletIndex );
	}
	
	updatePortlet = function updatePortlet(portletId, title, mode, modes, content) {
		if (mode === "Preview") {
			updatePreview(portletId, title, modes, content);
		} else if (mode === "View") {
			updateView(portletId, title, modes, content);
		}
	};

	var pendingResourcesCallbacks = 0;
	
	addPortletResources = function(cssUris, scriptUris) {
        for (index in cssUris) {
            var uri = cssUris[index];
            if ($("head > link[href='" + uri + "']").length === 0) {
                $("head link[href$='/portal.css']:last").after("<link rel='stylesheet' href='" + uri + "'>");
            }
        }
        // Don't use jquery, https://stackoverflow.com/questions/610995/cant-append-script-element
        pendingResourcesCallbacks = scriptUris.length;
        if (pendingResourcesCallbacks === 0) {
            messageHandled();
        }
	    for (index in scriptUris) {
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

	var messageHandlers = {
	    'addPortletResources': addPortletResources,
		'updatePortlet': updatePortlet,
		'reload': function() { window.location.reload(true); }
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
})();
