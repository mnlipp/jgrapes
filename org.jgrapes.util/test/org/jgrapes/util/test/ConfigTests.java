/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.util.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.ConfigurationStore;
import static org.jgrapes.util.ConfigurationStore.asInstant;
import static org.jgrapes.util.ConfigurationStore.asNumber;
import org.jgrapes.util.JsonConfigurationStore;
import org.jgrapes.util.PreferencesStore;
import org.jgrapes.util.TomlConfigurationStore;
import org.jgrapes.util.YamlConfigurationStore;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.util.events.InitialConfiguration;
import static org.junit.Assert.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ConfigTests {

    public class UpdateTrigger extends Event<Void> {
    }

    public class App extends Component {

        @SuppressWarnings("unchecked")
        @Handler
        public void onConfigurationUpdate(ConfigurationUpdate event) {
            if (event instanceof InitialConfiguration) {
                assertEquals("42", event.value("/", "answer").get());
                assertEquals("24",
                    event.value("/sub/tree", "some.value").get());
                assertEquals("1", event.value("/sub/tree", "list.0").get());
                assertEquals("1", event.value("/sub/tree", "map.one").get());

                // Structured
                assertEquals(24, asNumber(event.structured("/sub/tree").get()
                    .get("some.value")).get().intValue());
                var list = (List<Number>) event.structured("/sub/tree").get()
                    .get("list");
                assertEquals(3, list.size());
                for (int i = 1; i <= 3; i++) {
                    assertEquals(i, asNumber(list.get(i - 1)).get().intValue());
                }
                var map = (Map<String, Object>) event.structured("/sub/tree")
                    .get().get("map");
                assertEquals(1, asNumber(map.get("one")).get().intValue());
                assertEquals(2, asNumber(((List<Number>) map.get("more"))
                    .get(0)).get().intValue());
                assertEquals(3, asNumber(((List<Number>) map.get("more"))
                    .get(1)).get().intValue());
            }
        }

        @Handler
        public void onTriggerUpdate(UpdateTrigger event) {
            fire(new ConfigurationUpdate().add("/", "updated", "new")
                .add("/sub/tree", "map.more.1", "4"));
        }
    }

    public class Child extends Component {

        public int value = 0;

        @Handler
        public void onConfigurationUpdate(ConfigurationUpdate event) {
            event.value("/", "answer", Number.class)
                .ifPresent(it -> value = it.intValue());
        }
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(strings = { "prefs", "json", "toml", "yaml" })
    public void testMisc(String format) throws InterruptedException,
            UnsupportedEncodingException, FileNotFoundException, IOException,
            JsonDecodeException, BackingStoreException {

        // Create app and initial file
        App app = new App();
        File file = null;
        Preferences prefsBase = null;
        ConfigurationStore conf = null;
        switch (format) {
        case "prefs":
            // Set values in Java Preferences
            prefsBase = Preferences.userNodeForPackage(getClass())
                .node("PreferencesStore");
            prefsBase.put("answer", "42");
            prefsBase.node("sub/tree").put("\"some.value\"", "24");
            prefsBase.node("sub/tree").put("list.0", "1");
            prefsBase.node("sub/tree").put("list.1", "2");
            prefsBase.node("sub/tree").put("list.2", "3");
            prefsBase.node("sub/tree").put("map.one", "1");
            prefsBase.node("sub/tree").put("map.more.0", "2");
            prefsBase.node("sub/tree").put("map.more.1", "3");
            prefsBase.node("sub/tree").put("at", "2022-09-29T12:34:00Z");
            prefsBase.node("sub/tree").put("objs.0.name", "obj0");
            prefsBase.node("sub/tree").put("objs.0.value", "0");
            prefsBase.node("sub/tree").put("objs.1.name", "obj1");
            prefsBase.node("sub/tree").put("objs.1.value", "1");
            prefsBase.node("sub/tree").put("objs.2.name", "obj2");
            prefsBase.node("sub/tree").put("objs.2.value", "2");
            prefsBase.flush();
            conf = new PreferencesStore(app, getClass());
            break;
        case "json":
            file = writeInitialFile(format);
            conf = new JsonConfigurationStore(app, file);
            break;
        case "toml":
            file = writeInitialFile(format);
            conf = new TomlConfigurationStore(app, file);
            break;
        case "yaml":
            file = writeInitialFile(format);
            conf = new YamlConfigurationStore(app, file);
            break;
        default:
            fail();
        }

        // Check direct access to store
        assertEquals("42", conf.values("/").get().get("answer"));
        assertEquals("24", conf.values("/sub/tree").get().get("some.value"));
        assertEquals("1", conf.values("/sub/tree").get().get("list.0"));
        assertEquals("1", conf.values("/sub/tree").get().get("map.one"));

        // Structured
        var list
            = (List<Number>) conf.structured("/sub/tree").get().get("list");
        assertEquals(3, list.size());
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, asNumber(list.get(i - 1)).get().intValue());
        }
        var map = (Map<String, Object>) conf.structured("/sub/tree").get()
            .get("map");
        assertEquals(1, asNumber(map.get("one")).get().intValue());
        assertEquals(2,
            asNumber(((List<Object>) map.get("more")).get(0)).get().intValue());
        assertEquals(3,
            asNumber(((List<Object>) map.get("more")).get(1)).get().intValue());
        assertEquals(Instant.parse("2022-09-29T12:34:00Z"),
            asInstant(conf.structured("/sub/tree").get().get("at")).get());
        var objs = (List<Map<String, Object>>) conf.structured("/sub/tree")
            .get().get("objs");
        assertEquals(3, objs.size());
        assertEquals("obj0", objs.get(0).get("name"));
        assertEquals("obj1", objs.get(1).get("name"));
        assertEquals("obj2", objs.get(2).get("name"));
        app.attach(conf);

        Components.start(app);
        Components.awaitExhaustion();
        Components.checkAssertions();

        // Does attached child get (initial) configuration?
        Child child = new Child();
        app.attach(child);
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(42, child.value);

        // Check storage update
        app.fire(new UpdateTrigger(), app);
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals("4", conf.values("/sub/tree").get().get("map.more.1"));
        switch (format) {
        case "prefs":
            assertEquals("new", prefsBase.get("updated", ""));
            break;
        case "json":
            jsonFindUpdated(file);
            break;
        case "toml":
        case "yaml":
            findUpdated(file);
            break;
        default:
            fail();
        }

        // Remove sub tree event
        app.fire(new ConfigurationUpdate().removePath("/sub"), app);
        Components.awaitExhaustion();
        Components.checkAssertions();
        switch (format) {
        case "prefs":
            assertFalse(prefsBase.nodeExists("sub"));
            assertTrue(prefsBase.nodeExists(""));
            break;
        case "json":
            jsonCheckRemove(file);
            break;
        case "toml":
        case "yaml":
            checkRemove(file);
            break;
        default:
            fail();
        }

        // Remove test preferences
        app.fire(new ConfigurationUpdate().removePath("/"), app);
        Components.awaitExhaustion();
        Components.checkAssertions();
        switch (format) {
        case "prefs":
            assertFalse(prefsBase.nodeExists(""));
            break;
        case "json":
        case "yaml":
            jsonCheckRemoveAll(file);
            break;
        default:
            checkEmpty(file);
            break;
        }

        // Cleanup
        if (file != null) {
            file.delete();
        }
        if (prefsBase != null) {
            Preferences.userNodeForPackage(getClass())
                .node(getClass().getSimpleName()).removeNode();
        }
    }

    private File writeInitialFile(String format) throws IOException,
            UnsupportedEncodingException, FileNotFoundException {
        File file = new File("testConfig." + format);
        Map<String, String> initial = Map.of("json",
            "{\"answer\":42, \"/sub\":{\"/tree\":{\"some.value\":24,"
                + "\"list\":[1,2,3],\"map\":{\"one\":1,\"more\":[2,3]},"
                + "\"at\":\"2022-09-29T12:34:00Z\","
                + "\"objs\":[{\"name\":\"obj0\",\"value\":0},"
                + "{\"name\":\"obj1\",\"value\":1},"
                + "{\"name\":\"obj2\",\"value\":2}]}}}",
            "toml", "answer = 42\n"
                + "[_sub.\"/tree\"]\n"
                + "\"some.value\" = 24\n"
                + "list = [1, 2, 3]\n"
                + "map.one = 1\n"
                + "map.more = [2, 3]\n"
                + "at = 2022-09-29T12:34:00Z\n"
                + "objs = [{name=\"obj0\",value=0},"
                + "{name=\"obj1\",value=1},{name=\"obj2\",value=2}]",
            "yaml", "answer: 42\n"
                + "_sub:\n"
                + "  _tree:\n"
                + "    some.value: 24\n"
                + "    list: \n"
                + "    - 1\n"
                + "    - 2\n"
                + "    - 3\n"
                + "    map:\n"
                + "      one: 1\n"
                + "      more: [2,3]\n"
                + "    at: \"2022-09-29T12:34:00Z\"\n"
                + "    objs:\n"
                + "    - name: obj0\n"
                + "      value: 0\n"
                + "    - name: obj1\n"
                + "      value: 1\n"
                + "    - name: obj2\n"
                + "      value: 2\n");
        try (Writer out
            = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
            out.write(initial.get(format));
        }
        return file;
    }

    @SuppressWarnings("unchecked")
    private void jsonFindUpdated(File file) throws JsonDecodeException,
            IOException, UnsupportedEncodingException, FileNotFoundException {
        Map<String, Object> root;
        try (Reader input
            = new InputStreamReader(new FileInputStream(file), "utf-8")) {
            root = JsonBeanDecoder.create(input).readObject(HashMap.class);
        }

        // File must have been updated
        assertEquals("new", root.get("updated"));
    }

    private void findUpdated(File file) throws UnsupportedEncodingException,
            FileNotFoundException, IOException {
        boolean found = false;
        try (BufferedReader input = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), "utf-8"))) {
            while (true) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains("update") && line.contains("new")) {
                    found = true;
                    break;
                }
            }
        }
        // File must have been updated
        assertTrue(found);
    }

    @SuppressWarnings("unchecked")
    private void jsonCheckRemove(File file) throws JsonDecodeException,
            IOException, UnsupportedEncodingException, FileNotFoundException {
        Map<String, Object> root;
        try (Reader input
            = new InputStreamReader(new FileInputStream(file), "utf-8")) {
            root = JsonBeanDecoder.create(input).readObject(HashMap.class);
        }

        // Sub tree must have been removed in file
        assertFalse(root.containsKey("/sub"));
    }

    private void checkRemove(File file) throws UnsupportedEncodingException,
            FileNotFoundException, IOException {
        boolean found = false;
        try (BufferedReader input = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), "utf-8"))) {
            while (true) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains("list")) {
                    found = true;
                    break;
                }
            }
        }

        // Sub tree must have been removed in file
        assertFalse(found);

    }

    @SuppressWarnings("unchecked")
    private void jsonCheckRemoveAll(File file) throws JsonDecodeException,
            IOException, UnsupportedEncodingException, FileNotFoundException {
        Map<String, Object> root;
        try (Reader input
            = new InputStreamReader(new FileInputStream(file), "utf-8")) {
            root = JsonBeanDecoder.create(input).readObject(HashMap.class);
        }

        // Data must have been removed
        assertTrue(root.isEmpty());
    }

    private void checkEmpty(File file) throws JsonDecodeException,
            IOException, UnsupportedEncodingException, FileNotFoundException {
        boolean found = false;
        try (BufferedReader input = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), "utf-8"))) {
            while (true) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                if (!line.trim().isEmpty()) {
                    found = true;
                    break;
                }
            }
        }

        // Data must have been removed
        assertFalse(found);
    }
}
