'use strict';

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
var JGPortal = {
};

(function () {

    var log = {
        debug: function(message) {
            if (console && console.debug) {
                console.debug(message)
            }
        },
        info: function(message) {
            if (console && console.info) {
                console.info(message)
            }
        },
        warn: function(message) {
            if (console && console.warn) {
                console.warn(message)
            }
        },
        error: function(message) {
            if (console && console.error) {
                console.error(message)
            }
        }
    }
    JGPortal.log = log;

    // https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
    function generateUUID() {
        var d = new Date().getTime();
        if(window.performance && typeof window.performance.now === "function"){
            d += performance.now();; //use high-precision timer if available
        }
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = (d + Math.random()*16)%16 | 0;
            d = Math.floor(d/16);
            return (c=='x' ? r : (r&0x3|0x8)).toString(16);
        });
        return uuid;
    };

    /**
     * Utility method to format a memory size to a maximum
     * of 4 digits.
     */
    JGPortal.formatMemorySize = function(size, digits, lang) {
        if (lang === undefined) {
            lang = digits;
            digits = -1;
        }
        let scale = 0;
        while (size > 10000 && scale < 5) {
                size = size / 1024;
                scale += 1;
        }
        let unit = "PiB";
        switch (scale) {
        case 0:
            unit = "B";
            break;
        case 1:
            unit = "kiB";
            break;
        case 2:
            unit = "MiB";
            break;
        case 3:
            unit = "GiB";
            break;
        case 4:
            unit = "TiB";
            break;
        default:
            break;
        }
        if (digits >= 0) {
            return new Intl.NumberFormat(lang, {
                minimumFractionDigits: digits,
                maximumFractionDigits: digits
            }).format(size) + " " + unit;
        }
        return new Intl.NumberFormat(lang).format(size) + " " + unit;
    }
    
    
    // ///////////////////
    // WebSocket "wrapper"
    // ///////////////////
    
    var PortalWebSocket = function() {
        this._ws = null;
        this._sendQueue = [];
        this._recvQueue = [];
        this._recvQueueLocks = 0;
        this._messageHandlers = {};
        this._refreshTimer = null;
        this._reconnectTimer = null;
        this._connectRequested = false;
        this._portalSessionId = sessionStorage.getItem("org.jgrapes.portal.sessionId");
        if (!this._portalSessionId) {
            this._portalSessionId = generateUUID();
            sessionStorage.setItem("org.jgrapes.portal.sessionId", this._portalSessionId);
        }
        this._location = (window.location.protocol === "https:" ? "wss:" : "ws") +
            "//" + window.location.host + window.location.pathname;
        if (!this._location.endsWith("/")) {
            this._location += "/";
        }
        this._location += "portal-session/" + this._portalSessionId;
    };

    PortalWebSocket.prototype.portalSessionId = function() {
        return this._portalSessionId;
    };
    
    PortalWebSocket.prototype._connect = function() {
        this._ws = new WebSocket(this._location);
        let self = this;
        this._ws.onopen = function() {
            self._drainSendQueue();
            self._refreshTimer = setInterval(function() {
                if (self._sendQueue.length == 0) {
                    self.send({"jsonrpc": "2.0", "method": "keepAlive",
                        "params": []});
                }
            }, JGPortal.portalSessionRefreshInterval);
        }
        this._ws.onclose = function(event) {
            if (self._refreshTimer !== null) {
                clearInterval(self._refreshTimer);
                self._refreshTimer = null;
            }
            this._ws = null;
            if (this._connectRequested) {
                // Not an intended connect
                self._initiateReconnect();
            }
        }
        this._ws.onerror = function(event) {
            if (self._refreshTimer !== null) {
                clearInterval(self._refreshTimer);
                self._refreshTimer = null;
            }
            this._ws = null;
            if (this._connectRequested) {
                self._initiateReconnect();
            }
        }
        this._ws.onmessage = function(event) {
            var msg = JSON.parse(event.data);
            self._recvQueue.push(msg);
            if (self._recvQueue.length === 1) {
                self._handleMessages();
            }
        }
    }

    PortalWebSocket.prototype.connect = function() {
        this._connectRequested = true;
        this._connect();
    } 
    
    PortalWebSocket.prototype.close = function() {
        this.send({"jsonrpc": "2.0", "method": "disconnect",
            "params": [ this._portalSessionId ]});
        this._connectRequested = false;
        this._ws.close();
    }

    PortalWebSocket.prototype._initiateReconnect = function() {
        if (!this._reconnectTimer) {
            let self = this;
            this._reconnectTimer = setTimeout(function() {
                self._reconnectTimer = null;
                self._connect();
            }, 1000);
        }
    }
    
    PortalWebSocket.prototype._drainSendQueue = function() {
        while (this._ws.readyState == 1 && this._sendQueue.length > 0) {
            let msg = this._sendQueue[0];
            try {
                this._ws.send(msg);
                this._sendQueue.shift();
            } catch (e) {
                log.warn(e);
            }
        }
    }
    
    PortalWebSocket.prototype.send = function(data) {
        this._sendQueue.push(JSON.stringify(data));
        this._drainSendQueue();
    }

    PortalWebSocket.prototype.addMessageHandler = function(method, handler) {
        this._messageHandlers[method] = handler;
    }
    
    PortalWebSocket.prototype._handleMessages = function() {
        while (true) {
            if (this._recvQueue.length === 0 || this._recvQueueLocks > 0) {
                break;
            }
            var message = this._recvQueue.shift(); 
            var handler = this._messageHandlers[message.method];
            if (!handler) {
                log.error("No handler for invoked method " + message.method);
                continue;
            }
            if (message.hasOwnProperty("params")) {
                handler(...message.params);
            } else {
                handler();
            }
        }
    };
    
    PortalWebSocket.prototype.lockMessageReceiver = function() {
        this._recvQueueLocks += 1;
    }
    
    PortalWebSocket.prototype.unlockMessageReceiver = function() {
        this._recvQueueLocks -= 1;
        if (this._recvQueueLocks == 0) {
            this._handleMessages();
        }
    }
    
    var webSocketConnection = new PortalWebSocket();
    JGPortal.lockMessageQueue = function() {
        webSocketConnection.lockMessageReceiver();
    }
    JGPortal.unlockMessageQueue = function() {
        webSocketConnection.unlockMessageReceiver();
    }

    // Portal handlers/functions
    
    var lastPreviewLayout = [[], [], []];
    var lastTabsLayout = [];
    
    var portalIsConfigured = false;
    var portletFunctionRegistry = {};
    
    webSocketConnection.addMessageHandler('lastPortalLayout',
        function(previewLayout, tabsLayout) {
            lastPreviewLayout = previewLayout;
            lastTabsLayout = tabsLayout;
        });
    
    webSocketConnection.addMessageHandler('reload',
        function() { 
            window.location.reload(true);
        });
    
    JGPortal.registerPortletMethod = function(portletClass, methodName, method) {
        let classRegistry = portletFunctionRegistry[portletClass];
        if (!classRegistry) {
            classRegistry = {};
            portletFunctionRegistry[portletClass] = classRegistry;
        }
        classRegistry[methodName] = method;
    }

    webSocketConnection.addMessageHandler('invokePortletMethod',
        function invokePortletMethod(portletClass, portletId, method, params) {
            let classRegistry = portletFunctionRegistry[portletClass];
            if (classRegistry) {
                let f = classRegistry[method];
                if (f) {
                    f(portletId, params);
                }
            }
        });

    webSocketConnection.addMessageHandler('portalConfigured',
        function portalConfigured() {
            portalIsConfigured = true;
            layoutChanged();
            $("body").faLoading('remove');
        });
    
    webSocketConnection.addMessageHandler('addPortletType',
        function addPortletType(portletType, displayName, cssUris, scriptUris,
                isInstantiable) {
            for (let index in cssUris) {
                let uri = cssUris[index];
                if ($("head > link[href='" + uri + "']").length === 0) {
                    $("head link[href$='/portal.css']:last").after("<link rel='stylesheet' href='" + uri + "'>");
                }
            }
            // Don't use jquery, https://stackoverflow.com/questions/610995/cant-append-script-element
            for (let index in scriptUris) {
                let uri = scriptUris[index];
                if ($("head > script[src='" + uri + "']").length === 0) {
                    let script = document.createElement("script");
                    script.src = uri;
                    JGPortal.lockMessageQueue();
                    script.addEventListener('load', function() {
                        JGPortal.unlockMessageQueue();
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
        });

    function findPortletPreview(portletId) {
        let matches = $( ".portlet[data-portlet-id='" + portletId + "']" );
        if (matches.length === 1) {
            return $( matches[0] );
        }
        return undefined;
    };
    JGPortal.findPortletPreview = findPortletPreview;
    
    function findPortletView(portletId) {
        let tabs = $( "#tabs" ).tabs();
        let matches = tabs.find("> div[data-portlet-id='" + portletId + "']" );
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
            if ($(this).attr("data-portlet-id") === portletId) {
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
    
    function isBefore(items, x, limit) {
        for (let i = 0; i < items.length; i++) {
            if (items[i] === x) {
                return true;
            }
            if (items[i] === limit) {
                return false;
            }
        }
        return false;
    }
    
	function updatePreview(portletId, modes, content, foreground) {
		let portlet = findPortletPreview(portletId);
		if (!portlet) {
			portlet = $( '<div class="portlet">'
			        + '<div class="portlet-header"><span class="portlet-header-text"></span></div>'
			        + '<div class="portlet-content"></div>'
			        + '</div>');
			portlet.attr("data-portlet-id", portletId);
			portlet.addClass( "ui-widget ui-widget-content ui-helper-clearfix ui-corner-all" );
			let portletHeader = portlet.find( ".portlet-header" );
			portletHeader.addClass( "ui-widget-header ui-corner-all" );
            if (modes.includes("DeleteablePreview")) {
                portletHeader.prepend( "<span class='ui-icon ui-icon-delete portlet-delete'></span>");
                portletHeader.find(".portlet-delete").on( "click", function() {
                    let icon = $( this );
                    let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
                    JGPortal.sendDeletePortlet(portletId);
                });
            }
			if (modes.includes("View")) {
				portletHeader.prepend( "<span class='ui-icon ui-icon-fullscreen portlet-expand'></span>");
				portletHeader.find(".portlet-expand").on( "click", function() {
					let icon = $( this );
					let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
					if(findPortletView(portletId)) { 
					    activatePortletView(portletId);
					} else {
					    JGPortal.sendRenderPortlet(portletId, "View", true);
					}
				});
			}
			let inserted = false;
			$( ".column" ).each(function(colIndex) {
			    if (colIndex >= lastPreviewLayout.length) {
			        return false;
			    }
			    let colData = lastPreviewLayout[colIndex];
                // Hack to check if portletId is in colData
                if (isBefore(colData, portletId, portletId)) {
                    $( this ).find(".portlet").each(function(rowIndex) {
                        let item = $( this );
                        let itemId = item.attr("data-portlet-id");
                        if (!isBefore(colData, itemId, portletId)) {
                            item.before(portlet);
                            inserted = true;
                            return false;
                        }
                    });
                    if (!inserted) {
                        $( this ).append(portlet);
                    }
                    inserted = true;
                    return false;
                }
			});
			if (!inserted) {
			    $( ".column" ).first().prepend(portlet);
			}
			layoutChanged();
		}
		let newContent = $(content);
		let portletHeaderText = portlet.find(".portlet-header-text");
		portletHeaderText.text(newContent.attr("data-portlet-title"));
		let portletContent = portlet.find(".portlet-content");
		portletContent.children().detach();
		portletContent.append(newContent);
		if (foreground) {
		    $( "#tabs" ).tabs( "option", "active", 0 );
		}
	};

	function layoutChanged() {
	    if (!portalIsConfigured) {
	        return;
	    }
	    let previewLayout = [];
	    $(".overview-panel").find(".column").each(function(column) {
	        let colData = [];
	        previewLayout.push(colData);
	        $(this).find("div.portlet[data-portlet-id]").each(function(row) {
	            colData.push($(this).attr("data-portlet-id"));
	        });
	    });
	    let tabsLayout = [];
        let tabs = $( "#tabs" ).tabs();
        tabs.find(".ui-tabs-nav .ui-tabs-tab").each(function(index) {
            if (index > 0) {
                let tabId = $(this).attr("aria-controls");
                let portletId = tabs.find("#" + tabId).attr("data-portlet-id");
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
	                  .replace( /@\{label\}/g, newContent.attr("data-portlet-title")) );
	        let tabItems = tabs.find( ".ui-tabs-nav" );
	        tabItems.append( li );
	        portletView = $( "<div id='" + id + "'></div>" );
            portletView.append(newContent);
			portletView.attr("data-portlet-id", portletId);
	        tabs.append( portletView );
	        tabs.tabs( "refresh" );
	        layoutChanged();
		}
		if (foreground) {
		    activatePortletView(portletId);
		}
    }
	
    webSocketConnection.addMessageHandler('updatePortlet',
	    function updatePortlet(portletId, mode, modes, content, foreground) {
		    if (mode === "Preview" || mode === "DeleteablePreview") {
		        updatePreview(portletId, modes, content, foreground);
		    } else if (mode === "View") {
		        updateView(portletId, modes, content, foreground);
		    }
	    });

    webSocketConnection.addMessageHandler('deletePortlet',
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
	    });

    webSocketConnection.addMessageHandler('retrieveLocalData',
	    function retrieveLocalData(path) {
	        let result = [];
	        try {
	            for(let i = 0; i < localStorage.length; i++) {
	                let key = localStorage.key(i);
	                if (!path.endsWith("/")) {
	                    if (key !== path) {
	                        continue;
	                    }
	                } else {
	                    if (!key.startsWith(path)) {
	                        continue;
	                    }
	                }
	                let value = localStorage.getItem(key);
	                result.push([ key, value ])
	            }
	        } catch (e) {
	            log.error(e);
	        }
	        JGPortal.sendLocalData(result);
	    });
	
    webSocketConnection.addMessageHandler('storeLocalData',
        function storeLocalData(actions) {
            try {
                for (let i in actions) {
                    let action = actions[i];
                    if (action[0] === "u") {
                        localStorage.setItem(action[1], action[2]);
                    } else if (action[0] === "d") {
                        localStorage.removeItem(action[1]);
                    }
                }
            } catch (e) {
                log.error(e);
            }
        });
    
    // Everything set up, connect web socket
	webSocketConnection.connect();


	JGPortal.sendPortalReady = function() {
		webSocketConnection.send({"jsonrpc": "2.0", "method": "portalReady"});
	};
	
    JGPortal.sendSetTheme = function(themeId) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "setTheme",
            "params": [ themeId ]});
    };
    
    JGPortal.sendSetLocale = function(locale) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "setLocale",
            "params": [ locale ]});
    };
    
	JGPortal.sendRenderPortlet = function(portletId, mode, foreground) {
		webSocketConnection.send({"jsonrpc": "2.0", "method": "renderPortlet",
			"params": [ portletId, mode, foreground ]});
	};
    
    JGPortal.sendAddPortlet = function(portletType) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "addPortlet",
            "params": [ portletType, "Preview" ]});
    };
    
    JGPortal.sendDeletePortlet = function(portletId) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "deletePortlet",
            "params": [ portletId ]});
    };
    
    JGPortal.sendToPortlet = function(portletId, method, params) {
        if (params === undefined) {
            webSocketConnection.send({"jsonrpc": "2.0", "method": "sendToPortlet",
                "params": [ portletId, method ]});
        } else {
            webSocketConnection.send({"jsonrpc": "2.0", "method": "sendToPortlet",
                "params": [ portletId, method, params ]});
        }
    };
    
    JGPortal.sendLayout = function(previewLayout, tabLayout) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "portalLayout",
            "params": [ previewLayout, tabLayout ]});
    };
    
    JGPortal.sendLocalData = function(data) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "retrievedLocalData",
            "params": [ data ]});
    };
    
})();

(function() {

    JGPortal.tooltip = function(nodeSet) {
        nodeSet.tooltip({
            items: "[data-tooltip-id], [title]",
            content: function() {
                let element = $( this );
                if ( element.is( "[data-tooltip-id]" ) ) {
                    let id = element.attr("data-tooltip-id");
                    let tooltip = $( "#" + id );
                    if (tooltip) {
                        tooltip = tooltip.clone(true);
                        tooltip.removeClass("jgrapes-tooltip-prototype")
                        return tooltip;
                    }
                    return "<#" + id + ">";
                }
                if ( element.is( "[title]" ) ) {
                    return element.attr( "title" );
                }
            }
        });
    }
    
})();
