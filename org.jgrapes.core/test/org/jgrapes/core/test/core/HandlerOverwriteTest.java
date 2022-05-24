package org.jgrapes.core.test.core;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import static org.junit.Assert.*;
import org.junit.Test;

public class HandlerOverwriteTest {

    public static class BaseComponent extends Component {
        public boolean baseGotEvent;

        @Handler
        public void onConfigurationUpdate(Start event) {
            baseGotEvent = true;
        }
    }

    public static class DerivedComponent extends BaseComponent {
        public boolean derivedGotEvent;

        @Handler
        public void onConfigurationUpdate(Start event) {
            derivedGotEvent = true;
        }
    }

    public static class DerivedComponentWithoutHandler extends BaseComponent {
        public boolean derivedGotEvent;

        public void onConfigurationUpdate(Start event) {
            derivedGotEvent = true;
        }
    }

    @Test
    public void testOverwrite() throws InterruptedException {
        var base = new BaseComponent();
        var derived = new DerivedComponent();
        base.attach(derived);
        var derivedWithout = new DerivedComponentWithoutHandler();
        base.attach(derivedWithout);
        EventPipeline pipeline = Components.manager(base).newEventPipeline();
        pipeline.fire(new Start());
        pipeline.awaitExhaustion();
        assertTrue(base.baseGotEvent);
        assertFalse(derived.baseGotEvent);
        assertTrue(derived.derivedGotEvent);
        assertFalse(derivedWithout.baseGotEvent);
        assertFalse(derivedWithout.derivedGotEvent);
    }

}
