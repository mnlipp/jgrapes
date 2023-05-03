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

package org.jgrapes.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.events.FileChanged;
import org.jgrapes.util.events.WatchFile;

/**
 * A component that watches paths in the file system for changes. 
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class FileSystemWatcher extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final Logger logger
        = Logger.getLogger(FileSystemWatcher.class.getName());

    private final Map<FileSystem, WatchServiceInfo> watchServices
        = new ConcurrentHashMap<>();
    private final Map<Path, WatchInfo> watched = new ConcurrentHashMap<>();

    /**
     * Creates a new component base with its channel set to
     * itself.
     */
    public FileSystemWatcher() {
        super();
    }

    /**
     * Creates a new component base with its channel set to the given 
     * channel. As a special case {@link Channel#SELF} can be
     * passed to the constructor to make the component use itself
     * as channel. The special value is necessary as you 
     * obviously cannot pass an object to be constructed to its 
     * constructor.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     */
    public FileSystemWatcher(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Register a path for being watched.
     *
     * @param event the event
     */
    @Handler
    public void onWatchFile(WatchFile event) {
        final Path path = event.path().toAbsolutePath();
        @SuppressWarnings("PMD.CloseResource")
        WatchServiceInfo serviceInfo = watchServices.get(path.getFileSystem());
        if (serviceInfo == null) {
            try {
                serviceInfo = new WatchServiceInfo(path.getFileSystem());
                watchServices.put(path.getFileSystem(), serviceInfo);
            } catch (IOException e) {
                logger.log(Level.WARNING, e,
                    () -> "Cannot get watch service: " + e.getMessage());
                return;
            }
        }
        Path toWatch = path.getParent();
        synchronized (watched) {
            if (!watched.containsKey(toWatch)) {
                try {
                    watched.put(toWatch, new WatchInfo(
                        toWatch.register(serviceInfo.watchService,
                            ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)));
                } catch (IOException e) {
                    logger.log(Level.WARNING, e,
                        () -> "Cannot watch: " + e.getMessage());
                }
            }
        }
        watched.get(toWatch).watched.add(new WatchedInfo(path));
    }

    private void handleWatchEvent(Path directory) {
        Optional.ofNullable(watched.get(directory)).map(wi -> wi.watched)
            .orElse(Collections.emptyList()).forEach(wi -> {
                FileChanged.Kind change = checkForChange(wi);
                if (change != null) {
                    fire(new FileChanged(wi.path, change));
                }
            });
    }

    /**
     * Watching file in directory.
     * @param changes the changes
     * @param path the path
     * @param watched the wi
     */
    private FileChanged.Kind checkForChange(WatchedInfo watched) {
        Instant prevModified = watched.lastModified;
        watched.updateLastModified();
        if (prevModified == null) {
            // Check if created
            if (watched.lastModified != null) {
                return FileChanged.Kind.CREATED;
            }
            return null;
        }
        // File has existed
        if (watched.lastModified == null) {
            // Now deleted
            return FileChanged.Kind.DELETED;
        }
        // Check if modified
        if (!prevModified.equals(watched.lastModified)) {
            return FileChanged.Kind.MODIFIED;
        }
        return null;
    }

    /**
     * The Class WatchServiceInfo.
     */
    private class WatchServiceInfo {
        private final WatchService watchService;
        private final Thread watcher;

        private WatchServiceInfo(FileSystem fileSystem) throws IOException {
            watchService = fileSystem.newWatchService();
            watcher = new Thread(() -> {
                while (true) {
                    try {
                        WatchKey key = watchService.take();
                        if (!(key.watchable() instanceof Path)) {
                            continue;
                        }
                        handleWatchEvent((Path) key.watchable());
                        key.reset();
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, e,
                            () -> "No WatchKey: " + e.getMessage());
                    }
                }
            });
            watcher.setDaemon(true);
            watcher.setName(fileSystem.toString() + " watcher");
            watcher.start();
        }

    }

    /**
     * The Class WatchInfo.
     */
    private static class WatchInfo {
        @SuppressWarnings("unused")
        private final WatchKey watchKey;
        private final List<WatchedInfo> watched;

        private WatchInfo(WatchKey watchKey) {
            this.watchKey = watchKey;
            watched = Collections.synchronizedList(new ArrayList<>());
        }

    }

    /**
     * The Class WatchedInfo.
     */
    private static class WatchedInfo {
        private final Path path;
        private Instant lastModified;

        @SuppressWarnings("PMD.UseVarargs")
        private WatchedInfo(Path path) {
            this.path = path;
            updateLastModified();
        }

        private void updateLastModified() {
            try {
                if (!Files.exists(path)) {
                    lastModified = null;
                    return;
                }
                lastModified = Files.getLastModifiedTime(path).toInstant();
            } catch (NoSuchFileException e) {
                // There's a race condition here.
                lastModified = null;
            } catch (IOException e) {
                logger.log(Level.WARNING, e,
                    () -> "Cannot get modified time: " + e.getMessage());
            }
        }
    }
}
