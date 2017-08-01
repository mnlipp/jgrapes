/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

/**
 * The portal package provides a portal implementation for the
 * JGrapes framework. The {@link org.jgrapes.portal.Portal} component 
 * is conceptually the main component. It exchanges events 
 * with the portlets, usually using a channel that is independent
 * of the  channel used for HTTP Input/Output.
 *
 * When created, a {@link org.jgrapes.portal.Portal} component automatically 
 * instantiates a child component of type {@link org.jgrapes.portal.PortalView}
 * which handles the HTTP side of the portal. You can think of the 
 * {@link org.jgrapes.portal.PortalView}/{@link org.jgrapes.portal.Portal}
 * pair as a gateway that translates the Input/Output related events on the 
 * HTTP side to portal/portlet related events on the portlet side and 
 * vice versa.
 * 
 * The portal is implemented as a single page application. There is only
 * one initial HTML document that provides the basic structure of the portal.
 * Aside from requests for static resources like JavaScript libraries, CSS,
 * images etc. all information is then exchanged using a web socket connection
 * that is established immediately after the initial HTML has been loaded.
 * 
 * The following diagram shows the details of the portal bootstrap to 
 * the first JSON message.
 * 
 * ![Event Sequence](PortalBootSeq.svg)
 * 
 * After the portal page has loaded and the web socket connection has been
 * established, all information is exchanged using 
 * [JSON RPC notifications](http://www.jsonrpc.org/specification#notification). 
 * The {@link org.jgrapes.portal.PortalView} processes 
 * {@link org.jgrapes.io.events.Input} events with a serialized JSON RPC data
 * from the web socket until the complete JSON RPCnotification has been 
 * received. The notification (a {@link org.jgrapes.portal.events.JsonRequest}
 * from the servers point of view) is then fired on the portal channel, 
 * which allows it to be intercepted by additional components. Usually, 
 * however, it is handled by the {@link org.jgrapes.portal.Portal} that 
 * converts it to a higher level event that is again fired on the portal 
 * channel.
 * 
 * The following diagram shows the sequence of events following the
 * portal ready message. Note that the documentation of the events uses a 
 * slightly simplified version of the sequence diagram that combines the 
 * {@link org.jgrapes.portal.PortalView} and the 
 * {@link org.jgrapes.portal.Portal} into a single object.
 * 
 * ![Event Sequence](PortalReadySeq.svg)
 * 
 * Portlets trigger actions on the browser by firing events on the portal 
 * channel. The events are forward to the {@link org.jgrapes.portal.PortalView}
 * that converts them to JSON RPCs that are serialized and sent on the web 
 * socket (as {@link org.jgrapes.io.events.Output} events). 
 * 
 * Details about the handling of the different events can be found in their 
 * respective JavaDoc. The diagrams used there also combine the 
 * {@link org.jgrapes.portal.PortalView} and the 
 * {@link org.jgrapes.portal.Portal} into a single object.
 * 
 * @startuml PortalBootSeq.svg
 * hide footbox
 * 
 * Browser -> PortalView: "GET <portal URL>"
 * activate PortalView
 * PortalView -> Browser: "HTML document"
 * deactivate PortalView
 * activate Browser
 * Browser -> PortalView: "GET <resource1 URL>"
 * activate PortalView
 * PortalView -> Browser: Resource
 * deactivate PortalView
 * Browser -> PortalView: "GET <resource2 URL>"
 * activate PortalView
 * PortalView -> Browser: Resource
 * deactivate PortalView
 * Browser -> PortalView: "GET <Upgrade to WebSocket>"
 * activate PortalView
 * Browser -> PortalView: JSON RPC (Input)
 * Browser -> PortalView: JSON RPC (Input)
 * deactivate Browser
 * PortalView -> Portal: JsonRequest("portalReady")
 * deactivate PortalView
 * activate Portal
 * @enduml
 * 
 * @startuml PortalReadySeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "portalReady"
 * activate Portal
 * 
 * loop for all portlets
 *     Portal -> PortletX: PortalReady
 *     activate PortletX
 *     PortletX -> Portal: AddPortletType 
 *     deactivate PortletX
 *     activate Portal
 *     Portal -> Browser: "addPortletType"
 *     deactivate Portal
 * end
 * 
 * actor System
 * System -> PortalPolicy: PortalPrepared
 * deactivate Portal
 * activate PortalPolicy
 * loop for all portlets to be displayed
 *     PortalPolicy -> PortletY: RenderPortletRequest
 *     activate PortletY
 *     PortletY -> Portal: RenderPortlet
 *     deactivate PortletY
 *     activate Portal
 *     Portal -> Browser: "renderPortlet"
 *     deactivate Portal
 * end
 * deactivate PortalPolicy
 * System -> Portal: PortalConfigured
 * activate Portal
 * Portal -> Browser: "portalConfigured"
 * deactivate Portal
 * 
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.portal;