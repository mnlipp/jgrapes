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

package org.jgrapes.portal.events;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.jgrapes.core.Event;

/**
 * Adds `<link .../>`, `<style>...</style>` or `<script ...></script>` nodes
 * to the portal's `<head>` node.
 * 
 * Adding resource references causes the browser to issue `GET` request that
 * (usually) refer to resources that must be provided by the component
 * that created the {@link AddPageResources} event.
 * 
 * The complete sequence of events is shown in the diagram.
 * 
 * ![Portal Ready Event Sequence](AddPortletTypeSeq.svg)
 * 
 * Of course, due to internal buffering, the "Response Header" data
 * and the "Response body" data may collapse in a single message
 * that is sent to the browser (in case of a small resource).
 * 
 * The `GET` request may also, of course, refer to a resource from 
 * another server.
 * 
 * Adding a `<script src=...></script>` node to a document's `<head>` 
 * causes the references JavaScript to be loaded asynchronously. This
 * can cause problems if an added library relies on another library
 * to be available. Script reosurces are therefore specified using
 * the {@link ScriptResource} class, which allows to specify dependencies
 * between resource. The code in the browser delays the addition of
 * a `<script>` node until all other script resources that is depends
 * on are loaded. 
 * 
 * @startuml AddToHead.svg
 * hide footbox
 * 
 * activate Browser
 * Browser -> Portal: "portalReady"
 * deactivate Browser
 * activate Portal
 * Portal -> PageResourceProvider: PortalReady 
 * activate PageResourceProvider
 * PageResourceProvider -> Portal: AddPageResource
 * deactivate PageResourceProvider
 * Portal -> Browser: "addPageResource"
 * deactivate Portal
 * activate Browser
 * deactivate Portal
 * Browser -> Portal: "GET <page resource1 URL>"
 * activate Portal
 * Portal -> PageResourceProvider: PageResourceRequest
 * deactivate Portal
 * activate PageResourceProvider
 * PageResourceProvider -> Browser: "Response Header"
 * PageResourceProvider -> Browser: "Resource Data"
 * deactivate PageResourceProvider
 * 
 * @enduml
 */
public class AddPageResources extends Event<Void> {

	private List<ScriptResource> scriptResources = new ArrayList<>();
	private List<URI> cssUris = new ArrayList<>();
	private String cssSource;
	
	/**
	 * Create a new event.
	 */
	public AddPageResources() {
	}
	
	/**
	 * Add the URI of a JavaScript resource that is to be added to the
	 * header section of the portal page.
	 * 
	 * @param scriptResource the resource to add
	 * @return the event for easy chaining
	 */
	public AddPageResources addScriptResource(ScriptResource scriptResource) {
		scriptResources.add(scriptResource);
		return this;
	}

	/**
	 * Return all script URIs
	 * 
	 * @return the result
	 */
	public ScriptResource[] scriptResources() {
		return scriptResources.toArray(new ScriptResource[scriptResources.size()]);
	}

	/**
	 * Add the URI of a CSS resource that is to be added to the
	 * header section of the portal page.
	 * 
	 * @param uri the URI
	 * @return the event for easy chaining
	 */
	public AddPageResources addCss(URI uri) {
		cssUris.add(uri);
		return this;
	}

	/**
	 * Return all CSS URIs.
	 * 
	 * @return the result
	 */
	public URI[] cssUris() {
		return cssUris.toArray(new URI[cssUris.size()]);
	}
	
	/**
	 * @return the cssSource
	 */
	public String cssSource() {
		return cssSource;
	}

	/**
	 * @param cssSource the cssSource to set
	 */
	public AddPageResources setCssSource(String cssSource) {
		this.cssSource = cssSource;
		return this;
	}

	/**
	 * Represents a script resource that is to be loaded or evaluated
	 * by the browser. Note that a single instance can either be used
	 * for a URI or inline JavaScript, not for both.
	 */
	public static class ScriptResource {
		private String[] EMPTY_ARRAY = new String[0];
		
		private URI scriptUri;
		private String scriptSource;
		private String[] provides = EMPTY_ARRAY;
		private String[] requires = EMPTY_ARRAY;
		
		/**
		 * @return the scriptUri to be loaded
		 */
		public URI scriptUri() {
			return scriptUri;
		}
		
		/**
		 * Sets the scriptUri to to be loaded, clears the `scriptSource`
		 * attribute.
		 * 
		 * @param scriptUri the scriptUri to to be loaded
		 * @return this object for easy chaining
		 */
		public ScriptResource setScriptUri(URI scriptUri) {
			this.scriptUri = scriptUri;
			return this;
		}
		
		/**
		 * @return the script source
		 */
		public String scriptSource() {
			return scriptSource;
		}
		
		/**
		 * Sets the script source to evaluate. Clears the
		 * `scriptUri` attribute.
		 * 
		 * @param scriptSource the scriptSource to set
		 * @return this object for easy chaining
		 */
		public ScriptResource setScriptSource(String scriptSource) {
			this.scriptSource = scriptSource;
			return this;
		}
		
		/**
		 * Returns the list of JavaScript features that this
		 * script resource provides.
		 * 
		 * @return the list of features
		 */
		public String[] provides() {
			return provides;
		}
		
		/**
		 * Sets the list of JavaScript features that this
		 * script resource provides. For commonly available
		 * JavaScript libraries, it is recommended to use
		 * their home page URL (without the protocol part) as
		 * feature name. 
		 * 
		 * @param provides the list of features
		 * @return this object for easy chaining
		 */
		public ScriptResource setProvides(String[] provides) {
			this.provides = provides;
			return this;
		}
		
		/**
		 * Returns the list of JavaScript features that this
		 * script resource requires.
		 * 
		 * @return the list of features
		 */
		public String[] requires() {
			return requires;
		}
		
		/**
		 * Sets the list of JavaScript features that this
		 * script resource requires.
		 * 
		 * @param requires the list of features
		 * @return this object for easy chaining
		 */
		public ScriptResource setRequires(String[] requires) {
			this.requires = requires;
			return this;
		}
		
		public JsonObject toJsonValue() {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			if (scriptUri != null) {
				objBuilder.add("uri", scriptUri.toString());
			}
			if (scriptSource != null) {
				objBuilder.add("source", scriptSource);
			}
			JsonArrayBuilder strArrayBuilder = Json.createArrayBuilder();
			for (String req: requires) {
				strArrayBuilder.add(req);
			}
			objBuilder.add("requires", strArrayBuilder);
			strArrayBuilder = Json.createArrayBuilder();
			for (String prov: provides) {
				strArrayBuilder.add(prov);
			}
			objBuilder.add("provides", strArrayBuilder);
			return objBuilder.build();
		}
	}
}
