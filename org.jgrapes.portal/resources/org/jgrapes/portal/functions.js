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
     * of 4 digits for the integer part by appending the
     * appropriate unit.
     * 
     * @param {integer} size the size value to format
     * @param {integer} digits the number of digits of the factional part
     * @param {string} lang the language (BCP 47 code, 
     * used to determine the delimiter)
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
    
    /**
     * Defines a wrapper class for a web socket. An instance 
     * creates and maintains a connection to a session on the
     * server, i.e. it reconnects automatically to the session
     * when the connection is lost. The connection is used to
     * exchange JSON RPC notifications.
     */
    var PortalWebSocket = function() {
        this._ws = null;
        this._sendQueue = [];
        this._recvQueue = [];
        this._recvQueueLocks = 0;
        this._messageHandlers = {};
        this._refreshTimer = null;
        this._inactivity = 0;
        this._reconnectTimer = null;
        this._connectRequested = false;
        this._portalSessionId = sessionStorage.getItem("org.jgrapes.portal.sessionId");
        if (!this._portalSessionId) {
            this._portalSessionId = generateUUID();
            sessionStorage.setItem("org.jgrapes.portal.sessionId", this._portalSessionId);
        }
        this._location = (window.location.protocol === "https:" ? "wss" : "ws") +
            "://" + window.location.host + window.location.pathname;
        if (!this._location.endsWith("/")) {
            this._location += "/";
        }
        this._location += "portal-session/" + this._portalSessionId;
        this._connectionLostNotification = null;
    };

    /**
     * Returns the unique session id used to identify the connection.
     * 
     * @return {string} the id
     */
    PortalWebSocket.prototype.portalSessionId = function() {
        return this._portalSessionId;
    };
    
    PortalWebSocket.prototype._connect = function() {
        this._ws = new WebSocket(this._location);
        let self = this;
        this._ws.onopen = function() {
            if (self._connectionLostNotification != null) {
                self._connectionLostNotification.notification( "close" );
                $( "#server-connection-restored-notification" ).notification({
                    autoClose: 2000,
                });
            }
            findPreviewIds().forEach(function(id) {
                JGPortal.sendRenderPortlet(id, "Preview", false);
            });
            findViewIds().forEach(function(id) {
                JGPortal.sendRenderPortlet(id, "View", false);
            });
            self._drainSendQueue();
            self._refreshTimer = setInterval(function() {
                if (self._sendQueue.length == 0) {
                    self._inactivity += portalSessionRefreshInterval;
                    if (portalSessionInactivityTimeout > 0 &&
                            self._inactivity >= portalSessionInactivityTimeout) {
                        self.close();
                        $( "#portal-session-suspended-dialog" ).dialog(
                                "option", "buttons", {
                                    Ok: function() {
                                        $( this ).dialog( "close" );
                                        self.connect();
                                    }
                                });
                        $( "#portal-session-suspended-dialog" ).dialog( "open" );
                    }
                    self._send({"jsonrpc": "2.0", "method": "keepAlive",
                        "params": []});
                }
            }, portalSessionRefreshInterval);
        }
        this._ws.onclose = function(event) {
            if (self._refreshTimer !== null) {
                clearInterval(self._refreshTimer);
                self._refreshTimer = null;
            }
            self._ws = null;
            if (self._connectRequested) {
                // Not an intended disconnect
                if (self._connectionLostNotification == null) {
                    self._connectionLostNotification = 
                        $( "#server-connection-lost-notification" ).notification({
                            error: true,
                            icon: "alert",
                            destroyOnClose: false,
                        });
                } else {
                    self._connectionLostNotification.notification( "open" );
                }
                self._initiateReconnect();
            }
        }
        this._ws.onerror = function(event) {
            if (self._refreshTimer !== null) {
                clearInterval(self._refreshTimer);
                self._refreshTimer = null;
            }
            self._ws = null;
            if (self._connectRequested) {
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

    /**
     * Establishes the connection.
     */
    PortalWebSocket.prototype.connect = function() {
        this._connectRequested = true;
        this._connect();
    } 

    /**
     * Closes the connection.
     */
    PortalWebSocket.prototype.close = function() {
        this._send({"jsonrpc": "2.0", "method": "disconnect",
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
    
    PortalWebSocket.prototype._send = function(data) {
        this._sendQueue.push(JSON.stringify(data));
        this._drainSendQueue();
    }

    /**
     * Convert the passed object to its JSON representation
     * and sends it to the server. The object should represent
     * a JSON RPC notification.
     * 
     * @param  {object} data the data
     */
    PortalWebSocket.prototype.send = function(data) {
        this._inactivity = 0;
        this._send(data);
    }

    /**
     * When a JSON RPC notification is received, its method property
     * is matched against all added handlers. If a match is found,
     * the associated handler function is invoked with the
     * params values from the notification as parameters.
     * 
     * @param {string} method the method property to match
     * @param {function} handler the handler function
     */
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

    /*
     * The web socket connection of the current portal page.
     */
    var webSocketConnection = new PortalWebSocket();
    
    /**
     * Increses the lock count on the receiver. As long as
     * the lock count is greater than 0, the invocation of
     * handlers is suspended.
     */
    JGPortal.lockMessageQueue = function() {
        webSocketConnection.lockMessageReceiver();
    }
    
    /**
     * Decreases the lock count on the receiver. When the
     * count reaches 0, the invocation of handlers is resumed.
     */
    JGPortal.unlockMessageQueue = function() {
        webSocketConnection.unlockMessageReceiver();
    }

    // Portal handlers/functions (order roughly according to their
    // usage after portal start)
    
    var portalSessionRefreshInterval;
    var portalSessionInactivityTimeout;
    
    JGPortal.init = function(refreshInterval, inactivityTimeout) {
        portalSessionRefreshInterval = refreshInterval;
        portalSessionInactivityTimeout = inactivityTimeout;
        
        $("body").faLoading({
            icon: "fa-circle-o-notch",
            spin: true,
            text: false
        });
      
        // Top bar
        $( "#theme-menu" ).on("click", "[data-theme-id]", function() {
          JGPortal.sendSetTheme($(this).data("theme-id"));
          $( "#theme-menu" ).jqDropdown("hide");
        });
        
        $( "#language-menu" ).on("click", "[data-locale]", function() {
          JGPortal.sendSetLocale($(this).data("locale"));
          $( "#theme-menu" ).jqDropdown("hide");
        });
        
        $( "#addon-menu" ).on("click", "[data-portlet-type]", function() {
          JGPortal.sendAddPortlet($(this).data("portlet-type"));
          $( "#theme-menu" ).jqDropdown("hide");
        });
        
        // Tabs
        $( "#portlet-tabs" ).tabs();
        
        // AddTab button
        var tabs = $( "#portlet-tabs" ).tabs();

        // Close icon: removing the tab on click
        tabs.on( "click", "span.ui-icon-close", function() {
          var panelId = $( this ).closest( "li" ).remove().attr( "aria-controls" );
          $( "#" + panelId ).remove();
          tabs.tabs( "refresh" );
          layoutChanged();
        });

        // Dialogs
        $( "#portal-session-suspended-dialog" ).dialog({
            autoOpen: false,
            modal: true,
        });

        //
        // Portlet Grid
        //
        
        // Prepare columns
        $( ".column" ).sortable({
          connectWith: ".column",
          handle: ".portlet-header",
          cancel: ".portlet-toggle",
          placeholder: "portlet-placeholder ui-corner-all",
          stop: function( event, ui ) { layoutChanged(); }
        });
     
        // With everything prepared, send portal ready
        JGPortal.sendPortalReady();

    }
    
    let providedScriptResources = new Set(); // Names, i.e. strings
    let unresolvedScriptRequests = []; // ScriptResource objects
    let loadingScripts = new Set(); // uris (src attribute)
    let unlockMessageQueueAfterLoad = false;

    function mayBeStartScriptLoad (scriptResource) {
        let stillRequired = scriptResource.requires;
        scriptResource.requires = [];
        stillRequired.forEach(function(required) {
            if (!providedScriptResources.has(required)) {
                scriptResource.requires.push(required);
            }
        });
        if (scriptResource.requires.length > 0) {
            unresolvedScriptRequests.push(scriptResource);
            return;
        }
        let head = $("head").get()[0];
        let script = document.createElement("script");
        if (scriptResource.source) {
            script.text = scriptResource.source;
            // Whatever it provides is now provided
            scriptResource.provides.forEach(function(res) {
                providedScriptResources.add(res);
            });
        } else {
            script.src = scriptResource.uri;
            script.addEventListener('load', function(event) {
                // Remove this from loading
                loadingScripts.delete(script.src);
                // Whatever it provides is now provided
                scriptResource.provides.forEach(function(res) {
                    providedScriptResources.add(res);
                });
                // Re-evaluate
                let stillUnresolved = unresolvedScriptRequests;
                unresolvedScriptRequests = [];
                stillUnresolved.forEach(function(req) {
                    mayBeStartScriptLoad(req);
                });
                // All done?
                if (loadingScripts.size == 0 && unlockMessageQueueAfterLoad) {
                    JGPortal.unlockMessageQueue();
                }
            });
            // Put on script load queue to indicate load in progress
            loadingScripts.add(script.src);
        }
        head.appendChild(script);
    }
    
    function addPageResources(cssUris, cssSource, scriptResources) {
        for (let index in cssUris) {
            let uri = cssUris[index];
            if ($("head > link[href='" + uri + "']").length === 0) {
                $("head link[href$='/portal.css']:last").after("<link rel='stylesheet' href='" + uri + "'>");
            }
        }
        if (cssSource) {
            let style = $("style");
            style.text(cssSource);
            $("head link[href$='/portal.css']:last").after(style);
        }
        // Don't use jquery, https://stackoverflow.com/questions/610995/cant-append-script-element
        for (let index in scriptResources) {
            let scriptResource = scriptResources[index];
            if (scriptResource.uri) {
                if ($("head > script[src='" + scriptResource.uri + "']").length > 0) {
                    continue;
                }
            }
            mayBeStartScriptLoad(scriptResource);
        }
    }    
    webSocketConnection.addMessageHandler('addPageResources', addPageResources);

    webSocketConnection.addMessageHandler('addPortletType',
        function addPortletType(portletType, displayName, cssUris, scriptResources,
                isInstantiable) {
            addPageResources(cssUris, null, scriptResources);
            // Add to menu
            let item = $('<li class="ui-menu-item">'
                    + '<div class="ui-menu-item-wrapper" data-portlet-type="' 
                    + portletType + '">' + displayName + '</div></li>');
            $("#addon-menu-list").append(item);
        });

    var lastPreviewLayout = [[], [], []];
    var lastTabsLayout = [];
    
    var portalIsConfigured = false;
    var portletFunctionRegistry = {};
    
    webSocketConnection.addMessageHandler('lastPortalLayout',
        function(previewLayout, tabsLayout) {
            lastPreviewLayout = previewLayout;
            lastTabsLayout = tabsLayout;
            // Should we wait with further actions?
            if (loadingScripts.size > 0) {
                JGPortal.lockMessageQueue();
                unlockMessageQueueAfterLoad = true;
            }
        });
    
    webSocketConnection.addMessageHandler('reload',
        function() { 
            window.location.reload(true);
        });
    
    /**
     * Registers a portlet method that to be invoked if a
     * JSON RPC notification with method <code>invokePortletMethod</code>
     * is received.
     * 
     * @param {string} portletClass the portlet type for which
     * the method is registered
     * @param {string} methodName the method that is registered
     * @param {function} method the function to invoke
     */
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

    function execOnLoad(tree) {
        let onLoad = tree.data("onLoad");
        if (onLoad) {
            let segs = onLoad.split(".");
            let obj = window;
            while (obj && segs.length > 0) {
                obj = obj[segs.shift()];
            }
            if (obj && typeof obj === "function") {
                obj.apply(null, [ tree ]);
            }
        }
    }
    
    /**
     * Find the <code>div</code> that displays the preview of the
     * portlet with the given id.
     * 
     * @param {string} portletId the portlet id
     */
    JGPortal.findPortletPreview = function (portletId) {
        let matches = $( ".portlet[data-portlet-id='" + portletId + "']" );
        if (matches.length === 1) {
            return $( matches[0] );
        }
        return undefined;
    };
    var findPortletPreview = JGPortal.findPortletPreview;

    function findPreviewIds() {
        let ids = $( ".portlet[data-portlet-id]" ).map(function() {
            return $( this ).attr("data-portlet-id");
        }).get();
        return ids;
    }
    
    /**
     * Find the <code>div</code> that displays the view of the
     * portlet with the given id.
     * 
     * @param {string} portletId the portlet id
     */
    JGPortal.findPortletView = function (portletId) {
        let tabs = $( "#portlet-tabs" ).tabs();
        let matches = tabs.find("> div[data-portlet-id='" + portletId + "']" );
        if (matches.length === 1) {
            return $( matches[0] );
        }
        return undefined;
    };
    var findPortletView = JGPortal.findPortletView;

    /**
     * Update the title of the portlet with the given id.
     * 
     * @param {string} portletId the portlet id
     * @param {string} title the new title
     */
    JGPortal.updatePortletViewTitle = function (portletId, title) {
        let tabs = $( "#portlet-tabs" ).tabs();
        let portlet = tabs.find("> div[data-portlet-id='" + portletId + "']" );
        if (portlet.length === 0) {
            return;
        }
        portlet.find("[data-portlet-title]").attr("data-portlet-title", title);
        let tabId = portlet.attr("id");
        let portletTab = tabs.find("a[href='#" + tabId + "']");
        portletTab.empty();
        portletTab.append(title);
    }
    
    function findViewIds() {
        let tabs = $( "#portlet-tabs" ).tabs();
        let ids = tabs.find("> div[data-portlet-id]" ).map(function() {
            return $( this ).attr("data-portlet-id");
        }).get();
        return ids;
    }
    
    function activatePortletView(portletId) {
        let tabs = $( "#portlet-tabs" ).tabs();
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
    
    function setModeIcons(portlet, modes) {
        let portletHeader = portlet.find( ".portlet-header" );
        portletHeader.find( ".ui-icon" ).remove();
        if (modes.includes("Edit")) {
            portletHeader.prepend( "<span class='ui-icon ui-icon-wrench portlet-edit'></span>");
            portletHeader.find(".portlet-edit").on( "click", function() {
                let icon = $( this );
                let portletId = icon.closest( ".portlet" ).attr("data-portlet-id");
                JGPortal.sendRenderPortlet(portletId, "Edit", true);
            });
        }
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
    }
    
	function updatePreview(portletId, modes, content, foreground) {
		let portlet = findPortletPreview(portletId);
		if (!portlet) {
			portlet = $( '<div class="portlet ui-widget ui-widget-content ui-helper-clearfix ui-corner-all">'
			        + '<div class="portlet-header ui-widget-header"><span class="portlet-header-text"></span></div>'
			        + '<div class="portlet-content ui-widget-content"></div>'
			        + '</div>');
			portlet.attr("data-portlet-id", portletId);
			let portletHeader = portlet.find( ".portlet-header" );
			portletHeader.addClass( "ui-widget-header ui-corner-all" );
			setModeIcons(portlet, modes);
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
		execOnLoad(newContent);
		if (foreground) {
		    $( "#portlet-tabs" ).tabs( "option", "active", 0 );
		}
	};
	
    /**
     * Update the modes of the portlet with the given id.
     * 
     * @param {string} portletId the portlet id
     * @param {string[]} modes the modes
     */
    JGPortal.updatePortletModes = function (portletId, modes) {
        let portlet = findPortletPreview(portletId);
        if (!portlet) {
            return;
        }
        setModeIcons(portlet, modes);
    }
    var updatePortletModes = JGPortal.updatePortletModes; 
    
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
        let tabs = $( "#portlet-tabs" ).tabs();
        tabs.find(".ui-tabs-nav .ui-tabs-tab").each(function(index) {
            if (index > 0) {
                let tabId = $(this).attr("aria-controls");
                let portletId = tabs.find("#" + tabId).attr("data-portlet-id");
                tabsLayout.push(portletId);
            }
        });

	    JGPortal.sendLayout(previewLayout, tabsLayout);
	};
	
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
	        let tabs = $( "#portlet-tabs" ).tabs();
			tabCounter += 1;
	        let id = "portlet-tabs-" + tabCounter;
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
        execOnLoad(newContent);
		if (foreground) {
		    activatePortletView(portletId);
		}
    }

    function showEditDialog(portletId, modes, content) {
        let dialog = $( content );
        dialog.dialog({
            modal: true,
            width: "auto",
        });
        dialog.attr("data-portlet-id", portletId);
        execOnLoad(dialog);
    }
	
    webSocketConnection.addMessageHandler('updatePortlet',
	    function updatePortlet(portletId, mode, modes, content, foreground) {
		    if (mode === "Preview" || mode === "DeleteablePreview") {
		        updatePreview(portletId, modes, content, foreground);
		    } else if (mode === "View") {
		        updateView(portletId, modes, content, foreground);
		    } else if (mode === "Edit") {
		        showEditDialog(portletId, modes, content);
		    }
	    });

    webSocketConnection.addMessageHandler('deletePortlet',
        function deletePortlet(portletId) {
            let portletView = findPortletView(portletId);
            if (portletView) {
                let panelId = portletView.closest(".ui-tabs-panel").remove().attr("id");
                let tabs = $( "#portlet-tabs" ).tabs();
                tabs.find("li[aria-controls='" + panelId + "']").remove();
                $( "#portlet-tabs" ).tabs().tabs( "refresh" );
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
    
    webSocketConnection.addMessageHandler('createNotification',
        function (content, options) {
            $( content ).notification( options );
        });
    
    // Everything set up, connect web socket
	webSocketConnection.connect();

	/**
	 * Sends a portal ready notification to the server.
	 */
	JGPortal.sendPortalReady = function() {
		webSocketConnection.send({"jsonrpc": "2.0", "method": "portalReady"});
	};
	
	/**
	 * Sends a notification for changing the theme to the server.
	 * 
	 * @param {string} themeId the id of the selected theme
	 */
    JGPortal.sendSetTheme = function(themeId) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "setTheme",
            "params": [ themeId ]});
    };
    
    /**
     * Sends a notification for changing the language to the server.
     * 
     * @param {string} locale the id of the selected locale
     */
    JGPortal.sendSetLocale = function(locale) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "setLocale",
            "params": [ locale ]});
    };

    /**
     * Sends a notification that requests the rendering of a portlet.
     * 
     * @param {string} portletId the portlet id
     * @param {string} mode the requested render mode
     * @param {boolean} foreground if the portlet is to be put in
     * the foreground after rendering
     */
	JGPortal.sendRenderPortlet = function(portletId, mode, foreground) {
		webSocketConnection.send({"jsonrpc": "2.0", "method": "renderPortlet",
			"params": [ portletId, mode, foreground ]});
	};

	/**
	 * Sends a notification that requests the addition of a portlet.
	 * 
	 * @param {string} portletType the type of the portlet to add
	 */
    JGPortal.sendAddPortlet = function(portletType) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "addPortlet",
            "params": [ portletType, "Preview" ]});
    };

    /**
     * Send a notification that request the removal of a portlet.
     * 
     * @param {string} portletId the id of the portlet to be deleted
     */
    JGPortal.sendDeletePortlet = function(portletId) {
        webSocketConnection.send({"jsonrpc": "2.0", "method": "deletePortlet",
            "params": [ portletId ]});
    };

    /**
     * Send a notification with method <code>sendToPortlet</code>
     * and the given portlet id, method and parameters as the 
     * notification's parameters to the server.
     * 
     * @param {string} portletId the id of the portlet to send to
     * @param {string} method the method to invoke
     * @param params the parameters to send
     */
    JGPortal.sendToPortlet = function(portletId, method, ...params) {
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

(function() {
    var notificationContainer = null;
    var notificationCounter = 0;
    
    $( function() {
        let top = $( "<div></div>" );
        notificationContainer = $( "<div class='ui-notification-container ui-front'></div>" );
        top.append(notificationContainer);
        $( "body" ).prepend(top);
    });
    
    $.widget( "ui.notification", {
        
        // Default options.
        options: {
            classes: {
                "ui-notification": "ui-corner-all ui-widget-shadow",
                "ui-notification-content": "ui-corner-all",
            },
            error: false,
            icon: "circle-info",
            autoOpen: true,
            autoClose: null,
            destroyOnClose: null,
            show: {
                effect: "blind",
                direction: "up",
                duration: "fast",
            },
            hide: { 
                effect: "blind",
                direction: "up",
                duration: "fast",
            },
        },
     
        _create: function() {
            let self = this;
            this._isOpen = false;
            if (this.options.destroyOnClose === null) {
                this.options.destroyOnClose = this.options.autoOpen;
            }
            // Options are already merged and stored in this.options
            let widget = $( "<div></div>" );
            let notificationId = "ui-notification-" + ++notificationCounter;
            widget.attr("id", notificationId);
            this._addClass( widget, "ui-notification" );
            widget.addClass( "ui-widget-content" );
            widget.addClass( "ui-widget" );
            if (this.options.error) {
                widget.addClass( "ui-state-error" );
            } else {
                widget.addClass( "ui-state-highlight" );
            }
            
            // Close button (must be first for overflow: hidden to work).
            let button = $( "<button></button>");
            button.addClass( "ui-notification-close" );
            button.button({
                icon: "ui-icon-close",
                showLabel: false,
            });
            button.on ( "click", function() {
                self.close();
            } );
            widget.append(button);
            
            // Prepend icon
            if (this.options.icon) {
                widget.append( $( '<span class="ui-notification-content ui-icon' +
                        ' ui-icon-' + this.options.icon + '"></span>' ) );
            }
            let widgetContent = $( "<div></div>" );
            this._addClass( widgetContent, "ui-notification-content" );
            widget.append(widgetContent);
            widgetContent.append( this.element.clone() );
            widgetContent.find(":first-child").show();
            widget.hide();

            // Optional auto close
            if (this.options.autoClose) {
                setInterval(function() {
                    self.close();
                }, this.options.autoClose);
            }
            
            // Add to container
            this.notification = widget;
            
            // Open if desired
            if (this.options.autoOpen) {
                this.open();
            }
        },

        open: function() {
            notificationContainer.prepend( this.widget() );
            this._isOpen = true;
            this._show( this.widget(), this.options.show );
        },
        
        widget: function() {
            return this.notification;
        },
        
        close: function() {
            let self = this;
            this._isOpen = false;
            this._hide( this.widget(), this.options.hide, function() {
                if (self.options.destroyOnClose) {
                    self.destroy();
                }
            } );
        },
        
        _destroy: function() {
            if (this._isOpen) {
                this.close();
            }
        }
    });
})();