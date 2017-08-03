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
import org.jgrapes.util.events.PreferencesInitialized;
import org.jgrapes.util.events.PreferencesRemoval;
import org.jgrapes.util.events.PreferencesUpdate;

/**
 * This component provides a store for application preferences. Preferences
 * are maps of key value pairs that are associated with a path. A common
 * base path is passed to the component on creation. Components store 
 * their preferences using paths relative to that base path. Usually
 * the relative path simply corresponds to the comoponent's class name, 
 * though no restrictions for choosing the relative path exist.
 * 
 * The component reads the initial values from the Java {@link Preferences}
 * tree denoted by the base path. During application bootstrap, it 
 * intercepts the {@link Start} event using a handler with  priority 
 * 999999. When receiving this event, it fires all known preferences 
 * values on its channel as an {@link PreferencesInitialized} event. 
 * Then, it re-fires the intercepted {@link Start} event. 
 * 
 * Components that depend on preference values define handlers
 * for {@link PreferencesUpdate} events and adapt themselves to the values 
 * received. Note that due to the intercepted {@link Start} event, the initial
 * preferences values are received before the {@link Start} event, so
 * components' configurations can be rearranged before they actually
 * start doing something.
 *
 * Besides initially publishing the stored preferences values,
 * the component also listens for {@link PreferencesUpdate} events
 * fired by other components and updates the preferences store.
 */
public class PreferencesStore extends Component {

	private Preferences preferences;
	private boolean started = false;
	
	/**
	 * Creates a new component base with its channel set to the given 
	 * channel and a base path derived from the given class.
	 *  
	 * @param componentChannel the channel 
	 * @param appClass the application class; the base path
	 * is formed by replacing each dot in the class's full name with 
	 * a slash, prepending a slash, and appending "`/PreferencesStore`".
	 */
	public PreferencesStore(Channel componentChannel, Class<?> appClass) {
		super(componentChannel);
		preferences = Preferences.userNodeForPackage(appClass)
				.node(appClass.getSimpleName()).node("PreferencesStore");
	}

	@Handler(priority=999999)
	public void onStart(Start event) throws BackingStoreException {
		if (started) {
			return;
		}
		started = true;
		event.cancel(false);
		PreferencesInitialized updEvt 
			= new PreferencesInitialized(preferences.parent().absolutePath());
		addPrefs(updEvt, preferences.absolutePath(), preferences);
		fire(updEvt);
		fire(new Start(), event.channels());
	}

	private void addPrefs(
			PreferencesInitialized updEvt, String rootPath, Preferences node) 
					throws BackingStoreException {
		String nodePath = node.absolutePath();
		String relPath = nodePath.substring(Math.min(
				rootPath.length() + 1, nodePath.length()));
		for (String key: node.keys()) {
			updEvt.add(relPath, key, node.get(key, null));
		}
		for (String child: node.childrenNames()) {
			addPrefs(updEvt, rootPath, node.node(child));
		}
	}
	
	@Handler
	public void onStop(Stop event) {
		started = false;
	}
	
	@Handler
	public void onUpdatePreferences(PreferencesUpdate event) 
			throws BackingStoreException {
		if (event instanceof PreferencesInitialized) {
			return;
		}
		for (String path: event.paths()) {
			for (Map.Entry<String, String> e: 
				event.preferences(path).entrySet()) {
				preferences.node(path).put(e.getKey(), e.getValue());
			}
		}
		preferences.flush();
	}
	
	@Handler
	public void onRemovePreferences(PreferencesRemoval event) 
			throws BackingStoreException {
		for (String path: event.paths()) {
			preferences.node(path).removeNode();
		}
		preferences.flush();
	}
}
