package org.jgrapes.core.test.core;

import java.time.Instant

import org.jgrapes.core.Channel
import org.jgrapes.core.CompletionLock
import org.jgrapes.core.Component
import org.jgrapes.core.Components
import org.jgrapes.core.Event
import org.jgrapes.core.Components.Timer
import org.jgrapes.core.annotation.Handler
import org.jgrapes.core.events.Start
import org.jgrapes.core.events.Started

import spock.lang.Specification

class CacheTest extends Specification {

	class App extends Component {
	}

	class TestEvent extends Event<Void> {
	}
	
	class Comp extends Component {

		public int testEvents = 0;

		public Comp(Channel channel) {
			super(channel);
		}
		
		@Handler
		public onTest(TestEvent event) {
			testEvents += 1;
		}
		
	}
		
	void "Cache Update Test"() {
		setup: "App with component"
		App app = new App();
		Comp comp1 = app.attach(new Comp(app));
		app.fire(new Start());
		
		when: "Fire event"
		app.fire(new TestEvent(), app);
		Components.awaitExhaustion();
		
		then: "Event handled"
		comp1.testEvents == 1;
	
		when: "Attach another component and fire again"
		Comp comp2 = app.attach(new Comp(app));
		app.fire(new TestEvent(), app);
		Components.awaitExhaustion();

		then: "Event handled by both"
		comp1.testEvents == 2;
		comp2.testEvents == 1;
		
		when: "Detach component"
		comp1.detach();
		app.fire(new TestEvent(), app);
		Components.awaitExhaustion();

		then: "Event handled by remaining"
		comp1.testEvents == 2;
		comp2.testEvents == 2;
	}

}
