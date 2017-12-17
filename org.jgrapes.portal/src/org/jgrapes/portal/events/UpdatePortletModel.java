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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.json.JsonArray;

import org.jgrapes.core.Event;

/**
 * Sent to a portlet to update some of its properties. The interpretation
 * of the properties is completely dependent on the handling portlet.
 * 
 * This event has a close relationship to the {@link NotifyPortletModel}
 * event. The latter is used by portlet's functions to send information
 * from the portal page to the portlet model. It passes the information 
 * as a {@link JsonArray}. The interpretation of this information is only 
 * known by the portlet. The {@link UpdatePortletModel} event should be 
 * used to to pass information within the application, i.e. on the server
 * side.
 * 
 * Depending on the information passed, it may be good practice to 
 * write an event handler for the portlet that converts a 
 * {@link NotifyPortletModel} to a {@link UpdatePortletModel} that is
 * fired on its channel instead of handling it immediately. This allows 
 * event sent from the portal page and from other components in the 
 * application to be handled in a uniform way.
 */
public class UpdatePortletModel extends Event<Void> {

	private String portletId;
	private Map<? extends Object, ? extends Object> properties = null;
	
	/**
	 * Creates a new event.
	 * 
	 * @param portletId the id of the portlet
	 * @param properties the properties to update
	 */
	public UpdatePortletModel(String portletId, Map<?,?> properties) {
		this.portletId = portletId;
		@SuppressWarnings("unchecked")
		Map<Object, Object> props = (Map<Object,Object>)properties;
		this.properties = props;
	}

	/**
	 * Returns the portlet id.
	 * 
	 * @return the portlet id
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * Returns the properties. Every event returns a mutable map,
	 * thus allowing event handlers to modify the map even if
	 * none was passed to the constructor.
	 */
	public Map<Object,Object> properties() {
		if (properties == null) {
			properties = new HashMap<>();
		}
		@SuppressWarnings("unchecked")
		Map<Object, Object> props = (Map<Object, Object>) properties;
		return props;
	}
	
	/**
	 * Convenience method for adding properties one-by-one.
	 * 
	 * @param key the property key
	 * @param value the property value
	 * @return the event for easy chaining
	 */
	public UpdatePortletModel addOption(Object key, Object value) {
		properties().put(key, value);
		return this;
	}
	
	/**
	 * Convenience method that performs the given action if a property
	 * with the given key exists.
	 * 
	 * @param key the property key
	 * @param action the action to perform
	 */
	public UpdatePortletModel ifPresent(
			Object key, BiConsumer<Object,Object> action) {
		if (properties().containsKey(key)) {
			action.accept(key, properties().get(key));
		}
		return this;
	}
}
