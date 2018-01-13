package org.jgrapes.util.test;

import java.net.HttpCookie
import java.net.URI
import java.util.prefs.Preferences

import org.hamcrest.core.IsInstanceOf
import org.jgrapes.core.Component
import org.jgrapes.core.Components
import org.jgrapes.core.Event
import org.jgrapes.core.annotation.Handler
import org.jgrapes.core.events.Start
import org.jgrapes.util.PreferencesStore
import org.jgrapes.util.events.InitialPreferences
import org.jgrapes.util.events.ConfigurationUpdate

import groovy.transform.CompileStatic
import spock.lang.*

class PreferencesTests extends Specification {
	
	class Update extends Event<Void> {
	}
	
	class App extends Component {

		public String appPath;		
		public int value = 0;
		public int subValue = 0;
		
		@Handler
		public void onInitialPrefs(InitialPreferences event) {
			appPath = event.applicationPath();
			value = Integer.parseInt(event.preferences("/").get("answer"))
			subValue = Integer.parseInt(event.preferences("/sub/tree").get("value"))
		}
		
		@Handler
		public void onUpdate(Update event) {
			fire(new ConfigurationUpdate().add("/", "updated", "new"))
		}
	}

	void "Init Test"() {
		setup: "Create app and set values in Java Preferences"
		App app = new App();
		app.attach(new PreferencesStore(app, getClass()))
		Preferences base = Preferences.userNodeForPackage(getClass())
			.node(getClass().getSimpleName()).node("PreferencesStore")
		base.put("answer", "42")
		base.node("sub/tree").put("value", "24")
		base.flush();

		when: "Start"
		Components.start(app)
		Components.awaitExhaustion();

		then: "Values must have been set in component"
		app.appPath == Preferences.userNodeForPackage(getClass())
			.node(getClass().getSimpleName()).absolutePath()
		app.value == 42
		app.subValue == 24

		when: "PreferencesUpdate fired"
		app.fire(new Update(), app)
		Components.awaitExhaustion();
		
		then: "Java Preference must have been updated"
		base.get("updated", "") == "new"

		when: "Remove sub tree event"
		app.fire(new ConfigurationUpdate().removePath("/sub"), app)
		Components.awaitExhaustion();
		
		then: "Sub tree must have been removed in Java Preferences"
		!base.nodeExists("sub")
		base.nodeExists("")
		
		when: "Remove test preferences"
		app.fire(new ConfigurationUpdate().removePath("/"), app)
		Components.awaitExhaustion();
		
		then: "Preferences must have been removed in Java Preferences"
		!base.nodeExists("")
		
		cleanup:
		Preferences.userNodeForPackage(getClass())
			.node(getClass().getSimpleName()).removeNode()
	}
	
}