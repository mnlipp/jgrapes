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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.FileSystemWatcher;
import org.jgrapes.util.JsonConfigurationStore;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.util.events.WatchFile;
import static org.junit.Assert.*;
import org.junit.Test;

public class ConfigWatchTests {

    public class Tracker extends Component {

        public int answer;

        @Handler
        public void onConfigurationUpdate(ConfigurationUpdate event) {
            answer = event.value("/", "answer", Integer.class).get();
        }
    }

    @Test
    public void testChange() throws InterruptedException,
            UnsupportedEncodingException, FileNotFoundException, IOException,
            BackingStoreException {

        // Create app and initial file
        Tracker app = new Tracker();
        Path config = Files.createTempFile("jgrapes", ".json");
        try (var out = Files.newOutputStream(config)) {
            out.write("{ \"answer\": 42 }".getBytes());
        }
        app.attach(new JsonConfigurationStore(app, config.toFile()));
        app.attach(new FileSystemWatcher(app));
        Components.start(app);
        app.fire(new WatchFile(config)).get();
        Components.awaitExhaustion();
        assertEquals(42, app.answer);

        // Change file
        try (var out = Files.newOutputStream(config)) {
            out.write("{ \"answer\": 24 }".getBytes());
        }
        Thread.sleep(100);
        assertEquals(24, app.answer);

        Components.awaitExhaustion();
        Components.checkAssertions();
        Files.delete(config);
    }

}
