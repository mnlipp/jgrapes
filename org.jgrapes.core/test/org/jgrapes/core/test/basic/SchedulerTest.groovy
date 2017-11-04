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
		
		when: "Schedule and wait for first"
		Instant startTime = Instant.now();
		System.out.println("Test started at: " + startTime);
		Components.schedule({ expiredTimer -> hit1 = true },
			startTime.plusMillis(500));
		Components.schedule({ expiredTimer -> hit2 = true },
			startTime.plusMillis(1000));
		Timer timer3 = Components.schedule({ expiredTimer -> hit1 = false },
			startTime.plusMillis(1500));
		Instant now = Instant.now();
		failMessages.add("First check at: " + now);
		Thread.sleep(Duration.between(now, 
			startTime.plusMillis(750)).toMillis());
		
		then: "First set, second not"
		hit1;
		!hit2;
		
		when: "Waited longer"
		now = Instant.now();
		failMessages.add("Second check at: " + now);
		Thread.sleep(Duration.between(now, 
			startTime.plusMillis(1250)).toMillis());
		
		then:
		hit1;
		hit2;
		
		when: "Cancel and wait"
		timer3.cancel();
		now = Instant.now();
		failMessages.add("Final check at: " + now);
		Thread.sleep(Duration.between(now, 
			startTime.plusMillis(2000)).toMillis());
		
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
		boolean hit1 = false;
		
		when: "Schedule and wait for before hit"
		Instant startTime = Instant.now();
		Timer timer = Components.schedule({ expiredTimer -> hit1 = true },
			startTime.plusMillis(500));
		Thread.sleep(Duration.between(Instant.now(), 
			startTime.plusMillis(250)).toMillis());
		
		then: "Not hit"
		!hit1;
		
		when: "Reschedule and wait after initial timeout"
		timer.reschedule(startTime.plusMillis(1000))
		Thread.sleep(Duration.between(Instant.now(), 
			startTime.plusMillis(750)).toMillis());
		
		then:
		!hit1;
		
		when: "Wait for timeout"
		Thread.sleep(Duration.between(Instant.now(), 
			startTime.plusMillis(1500)).toMillis());
		
		then: "Hit"
		hit1;
	}

}
