package org.jgrapes.core.test.basic;

import java.time.Duration
import java.time.Instant

import org.jgrapes.core.Components
import org.jgrapes.core.Components.Timer

import spock.lang.Specification

class SchedulerTest extends Specification {

	void "Basic Scheduler Test"() {
		setup: "Initialize controlled variables"
		def failMessages = [];
		boolean hit1 = false;
		boolean hit2 = false;
		long t1 = 500;
		long t2 = 1000;
		long t3 = 1500;
		long t4 = 2000;
		if (System.getenv().get("TRAVIS") == 'true') {
			t1 = 2000;
			t2 = 4000;
			t3 = 6000;
			t4 = 8000;
		}
		
		when: "Schedule and wait for first"
		Instant startTime = Instant.now();
		failMessages.add("Test started at: " + startTime);
		Components.schedule({ expiredTimer -> hit1 = true },
			startTime.plusMillis(t1));
		Components.schedule({ expiredTimer -> hit2 = true },
			startTime.plusMillis(t2));
		Timer timer3 = Components.schedule({ expiredTimer -> hit1 = false },
			startTime.plusMillis(t3));
		// Wait until "then" (between t1 and t2)
		Instant then = startTime.plusMillis((long)((t1 + t2) / 2));
		failMessages.add("First check scheduled for: " + then);
		Thread.sleep(Duration.between(Instant.now(), then).toMillis());
		failMessages.add("First check at: " + Instant.now());
		
		then: "First set, second not"
		hit1;
		!hit2;
		
		when: "Waited longer"
		// between t2 and t3
		then = startTime.plusMillis((long)((t2 + t3) / 2));
		failMessages.add("Second check scheduled for: " + then);
		Thread.sleep(Duration.between(Instant.now(), then).toMillis());
		failMessages.add("Second check at: " + Instant.now());
		
		then:
		hit1;
		hit2;
		
		when: "Cancel and wait"
		timer3.cancel();
		// well after t3
		then = startTime.plusMillis(t4);
		failMessages.add("Final check scheduled for: " + then);
		Thread.sleep(Duration.between(Instant.now(), then).toMillis());
		failMessages.add("Final check at: " + Instant.now());
		
		then: "Nothing happened"
		hit1;
		hit2;
		(failMessages = []).empty;
		
		cleanup:
		failMessages.each {
			println it;
		}
	}

	void "Scheduler Reschedule Test"() {
		setup: "Initialize controlled variables"
		def failMessages = [];
		boolean hit1 = false;
		
		when: "Schedule and wait for before hit"
		Instant startTime = Instant.now();
		failMessages.add("Test started at: " + startTime);
		Timer timer = Components.schedule({ expiredTimer -> hit1 = true },
			startTime.plusMillis(1000));
		Instant then = startTime.plusMillis(500);		
		failMessages.add("First check scheduled for: " + then);
		Thread.sleep(Duration.between(Instant.now(), then).toMillis());
		failMessages.add("First check at: " + Instant.now());
		
		then: "Not hit"
		!hit1;
		
		when: "Reschedule and wait after initial timeout"
		timer.reschedule(startTime.plusMillis(2000));
		then = startTime.plusMillis(1500);
		failMessages.add("Second check scheduled for: " + then);
		Thread.sleep(Duration.between(Instant.now(), then).toMillis());
		failMessages.add("Second check at: " + Instant.now());
		
		then:
		!hit1;
		
		when: "Wait for timeout"
		then = startTime.plusMillis(3000);
		failMessages.add("Final check scheduled for: " + then);
		Thread.sleep(Duration.between(Instant.now(), then).toMillis());
		failMessages.add("final check at: " + Instant.now());
		
		then: "Hit"
		hit1;
		(failMessages = []).empty;
		
		cleanup:
		failMessages.each {
			println it;
		}
	}

}
