/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2023 Michael N. Lipp
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.FileSystemWatcher;
import org.jgrapes.util.events.FileChanged;
import org.jgrapes.util.events.WatchFile;
import static org.junit.Assert.*;
import org.junit.Test;

public class FileWatchTests {

    public static class Tracker extends Component {

        public Path path;
        public FileChanged.Kind change;

        public Tracker(Channel app) {
            super(app);
        }

        @Handler
        public void onFileChanged(FileChanged event) {
            path = event.path();
            change = event.change();
        }
    }

    @Test
    public void testNormal() throws InterruptedException, IOException {
        var app = new FileSystemWatcher();
        var tracker = new Tracker(app);
        app.attach(tracker);
        Components.start(app);
        Path watched = Files.createTempFile("jgrapes", null);
        Files.delete(watched);
        app.fire(new WatchFile(watched)).get();

        // Create
        Files.createFile(watched);
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.CREATED, tracker.change);

        // Make sure that timestamp differs
        Thread.sleep(100);
        try (var out = Files.newOutputStream(watched)) {
            out.write("Hello World!\n".getBytes());
            out.flush();
        }
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.MODIFIED, tracker.change);

        // Delete
        Files.delete(watched);
        // There's no sync and the watch service has a small delay
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.DELETED, tracker.change);

        // Finish
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

    @Test
    public void testExistingLink() throws InterruptedException, IOException {
        var app = new FileSystemWatcher();
        var tracker = new Tracker(app);
        app.attach(tracker);
        Components.start(app);

        // Setup
        Path realDir = Files.createTempDirectory("jgrapes");
        Path realFile = Files.createFile(realDir.resolve("test"));
        Path watched = Files.createTempFile("jgrapes-lnk", null);
        Files.delete(watched);
        Files.createSymbolicLink(watched, realFile);

        // Watch
        app.fire(new WatchFile(watched)).get();

        // Make sure that timestamp differs
        Thread.sleep(100);
        try (var out = Files.newOutputStream(watched)) {
            out.write("Hello World!\n".getBytes());
            out.flush();
        }
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.MODIFIED, tracker.change);

        // Delete
        Files.delete(watched);
        // There's no sync and the watch service has a small delay
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.DELETED, tracker.change);

        // Finish
        Files.walk(realDir).sorted(Comparator.reverseOrder()).map(Path::toFile)
            .forEach(File::delete);
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

    @Test
    public void testNewLink() throws InterruptedException, IOException {
        var app = new FileSystemWatcher();
        var tracker = new Tracker(app);
        app.attach(tracker);
        Components.start(app);
        Path realFile = Files.createTempFile("jgrapes", null);
        Files.delete(realFile);
        Path watched = Files.createTempFile("jgrapes-lnk", null);
        Files.delete(watched);
        app.fire(new WatchFile(watched)).get();

        // Create
        Files.createFile(realFile);
        Files.createSymbolicLink(watched, realFile);
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.CREATED, tracker.change);

        // Make sure that timestamp differs
        Thread.sleep(100);
        try (var out = Files.newOutputStream(watched)) {
            out.write("Hello World!\n".getBytes());
            out.flush();
        }
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.MODIFIED, tracker.change);

        // Delete
        Files.delete(watched);
        // There's no sync and the watch service has a small delay
        Thread.sleep(100);
        assertEquals(watched, tracker.path);
        assertEquals(FileChanged.Kind.DELETED, tracker.change);

        // Finish
        Files.delete(realFile);
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

}
