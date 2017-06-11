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

package org.jgrapes.util;

import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.util.events.UpdatePreferences;

/**
 * This component provides a store for application preferences.
 * 
 * The component reads the initial values from a Java {@link Preferences}
 * node. During application bootstrap, it intercepts the {@link Start} 
 * event using a handler with  priority 999999. When receiving this event, 
 * it fires all known preferences values on its channel as an 
 * {@link UpdatePreferences} event. Then, it re-fires the intercepted 
 * {@link Start} event.
 * 
 * Components that depend on preference values define handlers
 * for {@link UpdatePreferences} events and adapt themselves to the values 
 * received. Note that due to the intercepted {@link Start} event, the initial
 * preferences values are received before the {@link Start} event, so
 * components' configurations can be rearranged before they actually
 * start doing something.
 *
 * Besides initially publishing the stored preferences values,
 * the component also listens for {@link UpdatePreferences} events
 * fired by other components and updates the preferences store.
 */
public class PreferencesStore extends Component {

	private Preferences preferences;
	private boolean started = false;
	
	/**
	 * Creates a new component base with its channel set to the given 
	 * channel.
	 *  
	 * @param componentChannel the channel 
	 */
	public PreferencesStore(Channel componentChannel, Class<?> node) {
		super(componentChannel);
		preferences = Preferences.userNodeForPackage(node);
	}

	@Handler(priority=999999)
	public void onStart(Start event) throws BackingStoreException {
		if (started) {
			return;
		}
		started = true;
		event.intercept();
		UpdatePreferences updEvt = new UpdatePreferences();
		for (String scope: preferences.childrenNames()) {
			Preferences scoped = preferences.node(scope);
			for (String key: scoped.keys()) {
				updEvt.add(scope, key, scoped.get(key, null));
			}
		}
		fire(new Start(), event.channels());
	}
	
	@Handler
	public void onStop(Stop event) {
		started = false;
	}
	
	@Handler
	public void onUpdatePreferences(UpdatePreferences event) 
			throws BackingStoreException {
		for (String scope: event.scopes()) {
			for (Map.Entry<String, String> e: event.preferences(scope).entrySet()) {
				preferences.node(scope).put(e.getKey(), e.getValue());
			}
		}
		preferences.flush();
	}
}
