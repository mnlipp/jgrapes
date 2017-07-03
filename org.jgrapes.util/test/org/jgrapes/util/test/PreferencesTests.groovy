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

import groovy.transform.CompileStatic
import spock.lang.*

class PreferencesTests extends Specification {
	
	class App extends Component {
		
		public int value = 0;
		public boolean foundInvisible;
		
		@Handler
		public onInitialPrefs(InitialPreferences event) {
			value = Integer.parseInt(event.preferences("").get("answer"))
			foundInvisible = event.preferences(".hidden").size() != 0
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

		cleanup:
		prefs.removeNode()
	}
	
}