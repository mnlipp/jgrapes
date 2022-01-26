package org.jgrapes.core.test.core;

import java.util.Map;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.Components;
import org.jgrapes.core.Manager;
import static org.junit.Assert.*;
import org.junit.Test;

public class ComponentFactoryTest {

    public static class MyComponent extends Component {
    }

    @Test
    public void testProps() {
        var props = Map.of("name", "TestComponent");
        Manager comp = Components.manager(ComponentFactory
            .setStandardProperties(new MyComponent(), props));
        assertEquals("TestComponent", comp.name());
    }

}
