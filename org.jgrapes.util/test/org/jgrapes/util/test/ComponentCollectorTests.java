package org.jgrapes.util.test;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.util.ComponentCollector;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Component Collector Tests")
class ComponentCollectorTests {

    @Test
    @DisplayName("Creation of singleton")
    void testDefaultCreation() {
        var root
            = new ComponentCollector<>(ComponentFactory.class, Channel.SELF);
        var comps = root.children();
        assertEquals(1, comps.size());
        assertEquals(LinkTimeComponent.class, comps.get(0).getClass());
    }

}
