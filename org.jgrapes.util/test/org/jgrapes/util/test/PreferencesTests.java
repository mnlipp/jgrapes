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

import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.PreferencesStore;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.util.events.InitialPreferences;
import static org.junit.Assert.*;
import org.junit.Test;

public class PreferencesTests {

    public class UpdateTrigger extends Event<Void> {
    }

    public class App extends Component {

        public String appPath;
        public int value = 0;
        public int subValue = 0;

        @Handler
        public void onInitialPrefs(InitialPreferences event) {
            appPath = event.applicationPath();
        }

        @Handler
        public void onConfigurationUpdate(ConfigurationUpdate event) {
            event.value("/", "answer")
                .ifPresent(it -> value = Integer.parseInt(it));
            event.values("/sub/tree")
                .ifPresent(it -> subValue = Integer.parseInt(it.get("value")));
        }

        @Handler
        public void onTriggerUpdate(UpdateTrigger event) {
            fire(new ConfigurationUpdate().add("/", "updated", "new"));
        }
    }

    public class Child extends Component {

        public int value = 0;

        @Handler
        public void onConfigurationUpdate(ConfigurationUpdate event) {
            event.value("/", "answer")
                .ifPresent(it -> value = Integer.parseInt(it));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInit() throws InterruptedException, BackingStoreException {

        // Set values in Java Preferences
        Preferences base = Preferences.userNodeForPackage(getClass())
            .node("PreferencesStore");
        base.put("answer", "42");
        base.node("sub/tree").put("value", "24");
        base.node("sub/tree").put("list.0", "1");
        base.node("sub/tree").put("list.1", "2");
        base.node("sub/tree").put("list.2", "3");
        base.flush();

        // Create app
        App app = new App();
        var conf = new PreferencesStore(app, getClass());
        assertEquals("42", conf.values("/").get().get("answer"));
        assertEquals("24", conf.values("/sub/tree").get().get("value"));
        var list
            = (List<String>) conf.structured("/sub/tree").get().get("list");
        assertEquals(3, list.size());
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, Integer.parseInt(list.get(i - 1)));
        }
        app.attach(conf);

        Components.start(app);
        Components.awaitExhaustion();
        assertEquals(app.appPath, Preferences.userNodeForPackage(getClass())
            .absolutePath());
        assertEquals(42, app.value);
        assertEquals(24, app.subValue);

        // Does attached child get (initial) configuration?
        Child child = new Child();
        app.attach(child);
        Components.awaitExhaustion();
        assertEquals(42, child.value);

        // Check storage update
        app.fire(new UpdateTrigger(), app);
        Components.awaitExhaustion();
        assertEquals("new", base.get("updated", ""));

        // Remove sub tree event
        app.fire(new ConfigurationUpdate().removePath("/sub"), app);
        Components.awaitExhaustion();

        // Sub tree must have been removed in Java Preferences
        assertFalse(base.nodeExists("sub"));
        assertTrue(base.nodeExists(""));

        // Remove test preferences
        app.fire(new ConfigurationUpdate().removePath("/"), app);
        Components.awaitExhaustion();

        // Data must have been removed
        assertFalse(base.nodeExists(""));

        // Cleanup
        Preferences.userNodeForPackage(getClass())
            .node(getClass().getSimpleName()).removeNode();

    }

}
