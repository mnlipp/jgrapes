package org.jgrapes.core.test.core;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.events.Start;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Action on completion")
class CompletionActionTest {

    @Test
    @DisplayName("Must be executed")
    void testExecution() throws InterruptedException {
        var root = new Component() {
        };
        boolean[] flags = { false };
        Start evt = new Start();
        Event.onCompletion(evt, e -> {
            flags[0] = true;
        });
        root.fire(evt);
        Components.awaitExhaustion();
        assertTrue(flags[0]);
    }

}
