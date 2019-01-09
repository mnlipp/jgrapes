package org.jgrapes.core.test.core;

import java.time.Duration
import java.time.Instant

import org.jgrapes.core.Channel
import org.jgrapes.core.CompletionLock
import org.jgrapes.core.Component
import org.jgrapes.core.Components
import org.jgrapes.core.Components.Timer
import org.jgrapes.core.annotation.Handler
import org.jgrapes.core.events.Start
import org.jgrapes.core.events.Started

import spock.lang.Specification

class CompletionLockTest extends Specification {

    class App extends Component {

        public Start startEvent;
        public CompletionLock lock;
        private long timeout = 0;
        boolean started = false;
        public Instant startedAt = null;

        App(long timeout) {
            this.timeout = timeout;
        }

        @Handler
        public onStart(Start event) {
            startEvent = event;
            startedAt = Instant.now();
            if (timeout >= 0) {
                this.lock = new CompletionLock(event, timeout);
            }
        }

        @Handler
        public onStarted(Started event) {
            started = true;
        }
    }

    void "Completion Lock Test"() {
        when: "Start App without lock and wait"
        App app = new App(-1);
        app.fire(new Start() , Channel.BROADCAST);
        Thread.sleep(500);

        then: "Got Started"
        app.started == true;

        when: "Start App with lock"
        Components.awaitExhaustion();
        app = new App(0);
        app.fire(new Start() , Channel.BROADCAST);
        Components.awaitExhaustion();

        then:
        app.started == false;

        when: "Remove lock"
        app.lock.remove();
        Thread.sleep(500);

        then: "Got Started"
        app.started == true;

        when: "Start App with timeout lock"
        app = new App(750);
        app.fire(new Start(), Channel.BROADCAST);
        while (app.startedAt == null) {
            Thread.sleep(10);
        }
        Thread.sleep(Duration.between(app.startedAt,
                Instant.now().plusMillis(500)).toMillis());

        then: "Not yet"
        app.started == false;

        when: "Wait for timeout over"
        Components.awaitExhaustion(1500);
        Instant completedAt = Instant.now();

        then: "Now started"
        Duration.between(app.startedAt, completedAt).toMillis() < 1500;
    }
}
