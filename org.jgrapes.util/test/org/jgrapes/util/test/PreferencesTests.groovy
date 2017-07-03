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
import org.jgrapes.util.events.UpdatePreferences

import groovy.transform.CompileStatic
import spock.lang.*

class PreferencesTests extends Specification {
	
	class Update extends Event<Void> {
	}
	
	class App extends Component {
		
		public int value = 0;
		public boolean foundInvisible;
		
		@Handler
		public void onInitialPrefs(InitialPreferences event) {
			value = Integer.parseInt(event.preferences("").get("answer"))
			foundInvisible = event.preferences(".hidden").size() != 0
		}
		
		@Handler
		public void onUpdate(Update event) {
			fire(new UpdatePreferences().add("", "updated", "new"))
		}
	}

	void "Init Test"() {
		setup: "Create app"
		App app = new App();
		app.attach(new PreferencesStore(app, getClass()))
		Preferences prefs = Preferences.userNodeForPackage(getClass())
			.node(getClass().getSimpleName())
		prefs.put("answer", "42")
		prefs.node(".hidden").put("invisible", "True")

		when: "Start"
		Components.start(app)
		Components.awaitExhaustion();

		then: "Value must be set"
		app.value == 42
		!app.foundInvisible

		when: "UpdatePreferences fired"
		app.fire(new Update(), app)
		Components.awaitExhaustion();
		
		then: "Preference must be updated"
		prefs.get("updated", "") == "new"
		
		cleanup:
		prefs.removeNode()
	}
	
}