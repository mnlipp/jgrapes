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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.util.events.InitialPreferences;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * This component provides a store for an application's configuration
 * backed by the Java {@link Preferences}. Preferences
 * are maps of key value pairs that are associated with a path. A common
 * base path is passed to the component on creation. The application's
 * configuration information is stored using paths relative to that 
 * base path.
 * 
 * The component reads the initial values from the Java {@link Preferences}
 * tree denoted by the base path. During application bootstrap, it 
 * intercepts the {@link Start} event using a handler with  priority 
 * 999999. When receiving this event, it fires all known preferences 
 * values on the channels of the start event as a 
 * {@link InitialPreferences} event. Then, it re-fires the 
 * intercepted {@link Start} event. 
 * 
 * Components that depend on configuration values define handlers
 * for {@link ConfigurationUpdate} events and adapt themselves to the values 
 * received. Note that due to the intercepted {@link Start} event, the initial
 * preferences values are received before the {@link Start} event, so
 * components' configurations can be rearranged before they actually
 * start doing something.
 *
 * Besides initially publishing the stored preferences values,
 * the component also listens for {@link ConfigurationUpdate} events
 * on its channel and updates the preferences store (may be suppressed).
 */
public class PreferencesStore extends Component {

	private Preferences preferences;
	private Set<Start> replacements = Collections.newSetFromMap(
			new WeakHashMap<>());
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel and a base path derived from the given class.
	 * 
	 * @param componentChannel the channel 
	 * @param appClass the application class; the base path
	 * is formed by replacing each dot in the class's package's full 
	 * name with a slash, prepending a slash, and appending 
	 * "`/PreferencesStore`".
	 */
	public PreferencesStore(Channel componentChannel, Class<?> appClass) {
		this(componentChannel, appClass, true);
	}

	/**
	 * Allows the creation of a "read-only" store.
	 * 
	 * @param componentChannel the channel 
	 * @param appClass the application class; the base path
	 * is formed by replacing each dot in the class's package's full 
	 * name with a slash, prepending a slash, and appending 
	 * "`/PreferencesStore`".
	 * @param update whether to update the store when 
	 * {@link ConfigurationUpdate} events are received
	 * 
	 * @see #PreferencesStore(Channel, Class)
	 */
	public PreferencesStore(
			Channel componentChannel, Class<?> appClass, boolean update) {
		super(componentChannel);
		if (update) {
			Handler.Evaluator.add(this, "onConfigurationUpdate", 
					channel().defaultCriterion());
		}
		preferences = Preferences.userNodeForPackage(appClass)
				.node("PreferencesStore");
	}

	@Handler(priority=999999, channels=Channel.class)
	public void onStart(Start event) throws BackingStoreException {
		if (replacements.contains(event)) {
			return;
		}
		event.cancel(false);
		InitialPreferences updEvt 
			= new InitialPreferences(preferences.parent().absolutePath());
		addPrefs(updEvt, preferences.absolutePath(), preferences);
		fire(updEvt, event.channels());
		Start replacement = new Start();
		replacements.add(replacement);
		fire(replacement, event.channels());
	}

	private void addPrefs(
			InitialPreferences updEvt, String rootPath, Preferences node) 
					throws BackingStoreException {
		String nodePath = node.absolutePath();
		String relPath = "/" + nodePath.substring(Math.min(
				rootPath.length() + 1, nodePath.length()));
		for (String key: node.keys()) {
			updEvt.add(relPath, key, node.get(key, null));
		}
		for (String child: node.childrenNames()) {
			addPrefs(updEvt, rootPath, node.node(child));
		}
	}
	
	@Handler(dynamic=true)
	public void onConfigurationUpdate(ConfigurationUpdate event) 
			throws BackingStoreException {
		if (event instanceof InitialPreferences) {
			return;
		}
		for (String path: event.paths()) {
			Map<String,String> prefs = event.values(path);
			path = path.substring(1); // Remove leading slash
			if (prefs == null) {
				preferences.node(path).removeNode();
				continue;
			}
			for (Map.Entry<String, String> e: prefs.entrySet()) {
				preferences.node(path).put(e.getKey(), e.getValue());
			}
		}
		preferences.flush();
	}
}
