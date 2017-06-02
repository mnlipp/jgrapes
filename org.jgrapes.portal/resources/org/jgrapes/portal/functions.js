/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
JGPortal = {

// Prepare the given selection of portlets
preparePortlets: function (portlets) {
	portlets.addClass( "ui-widget ui-widget-content ui-helper-clearfix ui-corner-all" )
		.find( ".portlet-header" )
		.addClass( "ui-widget-header ui-corner-all" )
		.prepend( "<span class='ui-icon ui-icon-closethick portlet-close'></span>");
	portlets.find(".portlet-close").on( "click", function() {
		var icon = $( this );
		icon.closest( ".portlet" ).remove();
	});
},

updatePortlet: function updatePortlet(params) {
	var portlets = $( "div[portletId=" + params[0] + "]" );
	var portlet;
	if (portlets.length === 1) {
		portlet = portlets[0];
	} else {
		portlet = $( '<div class="portlet">\
<div class="portlet-header"></div>\
<div class="portlet-content"></div>\
</div>');
		JGPortal.preparePortlets(portlet);
		$( ".column" ).first().prepend(portlet);
	}
	var portletHeader = portlet.find(".portlet-header");
	portletHeader.children().detach();
	portletHeader.text(params[1]);
	var portletContent = portlet.find(".portlet-content");
	portletContent.children().detach();
	portletContent.append($(params[2]));
},

};

(function() {

	var messageHandlers = {
		'updatePortlet': JGPortal.updatePortlet
	};
	    
	JGPortal.handleMessage = function(message) {
        try {
        	messageHandlers[message.method](message.params);
        } catch (e) {
        }    
    };
	
})();

JGPortal.openConnection = function() {
	var loc = (window.location.protocol === "https:" ? "wss:" : "ws") +
		"//" + window.location.host + window.location.pathname;
	var wsConn = $.simpleWebSocket({ "url": loc })
		.listen(JGPortal.handleMessage)    
		.send({"jsonrpc": "2.0", "method": "portalReady"});
};
