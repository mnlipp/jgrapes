'use strict';

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
var JGPortal = {
    lastPreviewLayout: [[], [], []],
    lastTabsLayout: [],
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
    
    function addPortletType(portletType, displayName, cssUris, scriptUris,
            isInstantiable) {
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
        // Add to menu
        let item = $('<li class="ui-menu-item">'
                + '<div class="ui-menu-item-wrapper" data-portlet-type="' 
                + portletType + '">' + displayName + '</div></li>');
        $("#addon-menu-list").append(item);
    };
    addPortletType.callsBackMessageHandled = true;

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
    
    function activatePortletView(portletId) {
        let tabs = $( "#tabs" ).tabs();
        let portletIndex = undefined;
        let matches = tabs.find("> div").filter(function(index) {
            if ($(this).attr("data-portletId") === portletId) {
                portletIndex = index;
                return true;
            } else {
                return false;
            }
        });
        if (portletIndex) {
            tabs.tabs( "option", "active", portletIndex );
        }
    }
    
	function updatePreview(portletId, modes, content, foreground) {
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
            if (modes.includes("DeleteablePreview")) {
                portletHeader.prepend( "<span class='ui-icon ui-icon-delete portlet-delete'></span>");
                portletHeader.find(".portlet-delete").on( "click", function() {
                    let icon = $( this );
                    let portletId = icon.closest( ".portlet" ).attr("data-portletId");
                    JGPortal.sendDeletePortlet(portletId);
                });
            }
			if (modes.includes("View")) {
				portletHeader.prepend( "<span class='ui-icon ui-icon-fullscreen portlet-expand'></span>");
				portletHeader.find(".portlet-expand").on( "click", function() {
					let icon = $( this );
					let portletId = icon.closest( ".portlet" ).attr("data-portletId");
					if(findPortletView(portletId)) { 
					    activatePortletView(portletId);
					} else {
					    JGPortal.sendRenderPortlet(portletId, "View", true);
					}
				});
			}
			let inserted = false;
			$( ".column" ).each(function(index) {
			    if (index >= JGPortal.lastPreviewLayout.length) {
			        return false;
			    }
			    let col = JGPortal.lastPreviewLayout[index];
			    for (let i = 0; i < col.length; i++) {
			        if (col[i] === portletId) {
			            $( this ).prepend(portlet);
			            inserted = true;
			            return false;
			        }
			    }
			});
			if (!inserted) {
			    $( ".column" ).first().prepend(portlet);
			}
			layoutChanged();
		}
		let newContent = $(content);
		let portletHeaderText = portlet.find(".portlet-header-text");
		portletHeaderText.text(newContent.attr("data-portletTitle"));
		let portletContent = portlet.find(".portlet-content");
		portletContent.children().detach();
		portletContent.append(newContent);
		if (foreground) {
		    $( "#tabs" ).tabs( "option", "active", 0 );
		}
	};

	function layoutChanged() {
	    let previewLayout = [];
	    $(".overview-panel").find(".column").each(function(column) {
	        let colData = [];
	        previewLayout.push(colData);
	        $(this).find("div.portlet[data-portletId]").each(function(row) {
	            colData.push($(this).attr("data-portletId"));
	        });
	    });
	    let tabsLayout = [];
        let tabs = $( "#tabs" ).tabs();
        tabs.find(".ui-tabs-nav .ui-tabs-tab").each(function(index) {
            if (index > 0) {
                let tabId = $(this).attr("aria-controls");
                let portletId = tabs.find("#" + tabId).attr("data-portletId");
                tabsLayout.push(portletId);
            }
        });

	    JGPortal.sendLayout(previewLayout, tabsLayout);
	};
	JGPortal.layoutChanged = layoutChanged;
	
    var tabTemplate = "<li><a href='@{href}'>@{label}</a> " +
        "<span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>";
    var tabCounter = 1;

	function updateView(portletId, modes, content, foreground) {
		let portletView = findPortletView(portletId);
		let newContent = $(content);
		if (portletView) {
	        portletView.children().detach();
	        portletView.append(newContent);
		} else {
	        let tabs = $( "#tabs" ).tabs();
			tabCounter += 1;
	        let id = "tabs-" + tabCounter;
	        let li = $( tabTemplate.replace( /@\{href\}/g, "#" + id )
	                  .replace( /@\{label\}/g, newContent.attr("data-portletTitle")) );
	        let tabItems = tabs.find( ".ui-tabs-nav" );
	        tabItems.append( li );
	        portletView = $( "<div id='" + id + "'></div>" );
            portletView.append(newContent);
			portletView.attr("data-portletId", portletId);
	        tabs.append( portletView );
	        tabs.tabs( "refresh" );
	        layoutChanged();
		}
		if (foreground) {
		    activatePortletView(portletId);
		}
    }
	
	function updatePortlet(portletId, mode, modes, content, foreground) {
		if (mode === "Preview" || mode === "DeleteablePreview") {
			updatePreview(portletId, modes, content, foreground);
		} else if (mode === "View") {
			updateView(portletId, modes, content, foreground);
		}
	};

	function deletePortlet(portletId) {
        let portletView = findPortletView(portletId);
        if (portletView) {
            let panelId = portletView.closest(".ui-tabs-panel").remove().attr("id");
            let tabs = $( "#tabs" ).tabs();
            tabs.find("li[aria-controls='" + panelId + "']").remove();
            $( "#tabs" ).tabs().tabs( "refresh" );
        }
        let portlet = findPortletPreview(portletId);
        if (portlet) {
            portlet.remove();
        }
        layoutChanged();
	}

	function retrieveData(path) {
	    let result = null;
	    try {
	        result = localStorage.getItem(path);
	    } catch (e) {
	    }
	    JGPortal.sendData(path, result);
	}
	
    function storeData(path, data) {
        try {
            localStorage.setItem(path, data);
        } catch (e) {
        }
    }
    
	var pendingResourcesCallbacks = 0;
	
	var messageHandlers = {
	    'addPortletType': addPortletType,
        'deletePortlet': deletePortlet,
	    'invokePortletMethod': invokePortletMethod,
	    'lastPortalLayout': function(previewLayout, tabsLayout) {
	        JGPortal.lastPreviewLayout = previewLayout;
	        JGPortal.lastTabsLayout = tabsLayout;
	    },
		'reload': function() { window.location.reload(true); },
        'retrieveData': retrieveData,
		'storeData': storeData,
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
    
    JGPortal.sendSetLocale = function(locale) {
        wsConn.send({"jsonrpc": "2.0", "method": "setLocale",
            "params": [ locale ]});
    };
    
	JGPortal.sendRenderPortlet = function(portletId, mode, foreground) {
		wsConn.send({"jsonrpc": "2.0", "method": "renderPortlet",
			"params": [ portletId, mode, foreground ]});
	};
    
    JGPortal.sendAddPortlet = function(portletType) {
        wsConn.send({"jsonrpc": "2.0", "method": "addPortlet",
            "params": [ portletType, "Preview" ]});
    };
    
    JGPortal.sendDeletePortlet = function(portletId) {
        wsConn.send({"jsonrpc": "2.0", "method": "deletePortlet",
            "params": [ portletId ]});
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
    
    JGPortal.sendLayout = function(previewLayout, tabLayout) {
        wsConn.send({"jsonrpc": "2.0", "method": "portalLayout",
            "params": [ previewLayout, tabLayout ]});
    };
    
    JGPortal.sendData = function(path, data) {
        wsConn.send({"jsonrpc": "2.0", "method": "retrievedData",
            "params": [ path, data ]});
    };
    
})();
