package org.jgrapes.util.test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Start;
import org.jgrapes.util.ComponentProvider;
import org.jgrapes.util.events.ConfigurationUpdate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@DisplayName("Component Provider Tests")
class ComponentProviderTests {

    public static class TestComponent1 extends Component {
    }

    public static class Tc1Factory implements ComponentFactory {
        @Override
        public Class<? extends ComponentType> componentType() {
            return TestComponent1.class;
        }

        @Override
        public Optional<ComponentType> create(Channel componentChannel,
                Map<?, ?> properties) {
            return Optional.of(new TestComponent1());
        }
    }

    public static class TestComponent2 extends Component {
    }

    public static class Tc2Factory implements ComponentFactory {
        @Override
        public Class<? extends ComponentType> componentType() {
            return TestComponent2.class;
        }

        @Override
        public Optional<ComponentType> create(Channel componentChannel,
                Map<?, ?> properties) {
            return Optional.of(new TestComponent2());
        }
    }

    @Test
    @DisplayName("Nameless pinned configuration")
    void testNamelessPinned() {
        var root = new ComponentProvider();
        root.setFactories(new Tc1Factory(), new Tc2Factory());
        root.setPinned(List.of(Map.of(
            "componentType", TestComponent1.class.getName())));
        var comps = root.children();
        assertEquals(1, comps.size());
        assertEquals(TestComponent1.class, comps.get(0).getClass());
    }

    @Test
    @DisplayName("Named pinned configuration")
    void testNamedPinned() {
        var root = new ComponentProvider();
        root.setFactories(new Tc1Factory(), new Tc2Factory());
        root.setPinned(List.of(
            Map.of("componentType", TestComponent1.class.getName(),
                "name", "Comp1"),
            Map.of("componentType", TestComponent1.class.getName(),
                "name", "Comp2")));
        var comps = root.children();
        assertEquals(2, comps.size());
        assertEquals(TestComponent1.class, comps.get(0).getClass());
        assertEquals(TestComponent1.class, comps.get(1).getClass());
        assertEquals(1, comps.stream()
            .filter(c -> Components.manager(c).name().equals("Comp1")).count());
        assertEquals(1, comps.stream()
            .filter(c -> Components.manager(c).name().equals("Comp2")).count());
    }

    @Nested
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("Process events")
    class UpdateProcessing {

        ComponentProvider root;

        @BeforeEach
        void createStartConfiguration() throws InterruptedException {
            root = new ComponentProvider();
            root.setFactories(new Tc1Factory(), new Tc2Factory());
            var config = Map.of("components", List.of(
                Map.of("componentType", TestComponent1.class.getName(),
                    "name", "Comp1"),
                Map.of("componentType", TestComponent1.class.getName(),
                    "name", "Comp2")));
            var evt = new ConfigurationUpdate();
            evt.set("/ComponentProvider", config);
            root.fire(evt, root);
            root.fire(new Start());
            Components.awaitExhaustion();
        }

        @Test
        @Order(1)
        @DisplayName("Start configuration")
        void testStartConfiguration() throws InterruptedException {
            var comps = root.children();
            assertEquals(2, comps.size());
            assertEquals(TestComponent1.class, comps.get(0).getClass());
            assertEquals(TestComponent1.class, comps.get(1).getClass());
            assertEquals(1, comps.stream()
                .filter(c -> Components.manager(c).name().equals("Comp1"))
                .count());
            assertEquals(1, comps.stream()
                .filter(c -> Components.manager(c).name().equals("Comp2"))
                .count());
        }

        @Test
        @Order(2)
        @DisplayName("Remove second")
        void testRemoveSecond() throws InterruptedException {
            var config = Map.of("components", List.of(
                Map.of("componentType", TestComponent1.class.getName(),
                    "name", "Comp1")));
            var evt = new ConfigurationUpdate();
            evt.set("/ComponentProvider", config);
            root.fire(evt, Channel.BROADCAST);
            Components.awaitExhaustion();

            var comps = root.children();
            assertEquals(1, comps.size());
            assertEquals(1,
                comps.stream()
                    .filter(c -> c.getClass().equals(TestComponent1.class)
                        && Components.manager(c).name().equals("Comp1"))
                    .count());
        }

        @Test
        @Order(3)
        @DisplayName("Replace second")
        void testReplaceSecond() throws InterruptedException {
            var config = Map.of("components", List.of(
                Map.of("componentType", TestComponent1.class.getName(),
                    "name", "Comp1"),
                Map.of("componentType", TestComponent2.class.getName(),
                    "name", "Comp2")));
            var evt = new ConfigurationUpdate();
            evt.set("/ComponentProvider", config);
            root.fire(evt, Channel.BROADCAST);
            Components.awaitExhaustion();

            var comps = root.children();
            assertEquals(2, comps.size());
            assertEquals(1,
                comps.stream()
                    .filter(c -> c.getClass().equals(TestComponent1.class)
                        && Components.manager(c).name().equals("Comp1"))
                    .count());
            assertEquals(1,
                comps.stream()
                    .filter(c -> c.getClass().equals(TestComponent2.class)
                        && Components.manager(c).name().equals("Comp2"))
                    .count());
        }

        @Test
        @Order(4)
        @DisplayName("Remove and re-add factory")
        void testReAddFactory() throws InterruptedException {
            root.setFactories();
            var comps = root.children();
            assertEquals(0, comps.size());

            root.setFactories(new Tc1Factory(), new Tc2Factory());
            Components.awaitExhaustion();
            comps = root.children();
            assertEquals(2, comps.size());
            assertEquals(TestComponent1.class, comps.get(0).getClass());
            assertEquals(TestComponent1.class, comps.get(1).getClass());
            assertEquals(1, comps.stream()
                .filter(c -> Components.manager(c).name().equals("Comp1"))
                .count());
            assertEquals(1, comps.stream()
                .filter(c -> Components.manager(c).name().equals("Comp2"))
                .count());
        }

    }

}
