package org.jgrapes.util.test;

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpCookie
import java.net.URI
import java.util.HashMap
import java.util.Map
import java.util.prefs.Preferences

import org.hamcrest.core.IsInstanceOf
import org.jdrupes.json.JsonBeanDecoder
import org.jdrupes.json.JsonDecodeException
import org.jgrapes.core.Component
import org.jgrapes.core.Components
import org.jgrapes.core.Event
import org.jgrapes.core.annotation.Handler
import org.jgrapes.core.events.Start
import org.jgrapes.util.JsonConfigurationStore
import org.jgrapes.util.PreferencesStore
import org.jgrapes.util.events.InitialPreferences
import org.omg.CORBA.portable.OutputStream
import org.jgrapes.util.events.ConfigurationUpdate

import groovy.transform.CompileStatic
import spock.lang.*

class JsonConfigTests extends Specification {
	
	class UpdateTrigger extends Event<Void> {
	}
	
	class App extends Component {

		public int value = 0;
		public int subValue = 0;
		
		@Handler
		public void onConfigurationUpdate(ConfigurationUpdate event) {
			event.value("/", "answer").ifPresent({ value = Integer.parseInt(it) })
			event.values("/sub/tree")
				.ifPresent({ subValue = Integer.parseInt(it.get("value")) })
		}
		
		@Handler
		public void onTriggerUpdate(UpdateTrigger event) {
			fire(new ConfigurationUpdate().add("/", "updated", "new"))
		}
	}

	void "Init Test"() {
		setup: "Create app and initial file"
		App app = new App();
		File file = new File("testConfig.json");
		Writer out = new OutputStreamWriter(new FileOutputStream(file), "utf-8")
		out.write("{\"answer\":42, \"/sub\":{\"/tree\":{\"value\":24}}}")
		out.close()
		app.attach(new JsonConfigurationStore(app, file))

		when: "Start"
		Components.start(app)
		Components.awaitExhaustion();

		then: "Values must have been set in component"
		app.value == 42
		app.subValue == 24

		when: "Update fired"
		app.fire(new UpdateTrigger(), app)
		Components.awaitExhaustion();
		Reader input = new InputStreamReader(new FileInputStream(file), "utf-8")
		Map root = JsonBeanDecoder.create(input).readObject(HashMap.class);
		input.close();
		
		then: "File must have been updated"
		root.get("updated") == "new"

		when: "Remove sub tree event"
		app.fire(new ConfigurationUpdate().removePath("/sub"), app)
		Components.awaitExhaustion();
		input = new InputStreamReader(new FileInputStream(file), "utf-8")
		root = JsonBeanDecoder.create(input).readObject(HashMap.class);
		input.close();

		then: "Sub tree must have been removed in file"
		!root.containsKey("/sub")
		
		when: "Remove test preferences"
		app.fire(new ConfigurationUpdate().removePath("/"), app)
		Components.awaitExhaustion();
		input = new InputStreamReader(new FileInputStream(file), "utf-8")
		root = JsonBeanDecoder.create(input).readObject(HashMap.class);
		input.close();

		then: "Data must have been removed"
		root.isEmpty()
		
		cleanup:
		file.delete();
	}
	
}