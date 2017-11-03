package org.jgrapes.core.test.basic;

import java.time.Instant

import org.jgrapes.core.Components
import org.jgrapes.core.Components.Timer

import spock.lang.Specification

class SchedulerTest extends Specification {

	void "Basic Scheduler Test"() {
		setup: "Initialize controlled variables"
		boolean hit1 = false;
		boolean hit2 = false;
		
		when: "Schedule and wait for first"
		Instant startTime = Instant.now();
		Components.schedule({ expiredTimer -> hit1 = true },
			startTime.plusMillis(500));
		Components.schedule({ expiredTimer -> hit2 = true },
			startTime.plusMillis(1000));
		Timer timer3 = Components.schedule({ expiredTimer -> hit1 = false },
			startTime.plusMillis(1500));
		Thread.sleep(750);
		
		then: "First set, second not"
		hit1;
		!hit2;
		
		when: "Waited longer"
		Thread.sleep(500);
		
		then:
		hit1;
		hit2;
		
		when: "Cancel and wait"
		timer3.cancel();
		Thread.sleep(750);
		
		then: "Nothing happened"
		hit1;
		hit2;
	}

	void "Scheduler Reschedule Test"() {
		setup: "Initialize controlled variables"
		boolean hit1 = false;
		
		when: "Schedule and wait for before hit"
		Timer timer = Components.schedule({ expiredTimer -> hit1 = true },
			Instant.now().plusMillis(500));
		Thread.sleep(250);
		
		then: "Not hit"
		!hit1;
		
		when: "Reschedule and wait after initial timeout"
		timer.reschedule(Instant.now().plusMillis(750))
		Thread.sleep(500);
		
		then:
		!hit1;
		
		when: "Wait for timeout"
		Thread.sleep(1000);
		
		then: "Hit"
		hit1;
	}

}
