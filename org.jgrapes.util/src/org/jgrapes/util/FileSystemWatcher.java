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
import java.lang.ref.WeakReference;
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
 * A component that watches paths in the file system for changes
 * and sends events if such changes occur. 
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class FileSystemWatcher extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final Logger logger
        = Logger.getLogger(FileSystemWatcher.class.getName());

    private final WatcherRegistry watcherRegistry = new WatcherRegistry();
    private final Map<Path, DirectorySubscription> subscriptions
        = new ConcurrentHashMap<>();

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
     * Register a path to wath. Subsequent {@link FileChanged} 
     * events will be fire on the channel(s) on which the
     * {@link WatchFile} event was fired.
     * 
     * The channel is stored using a weak reference, so no explicit
     * "clear watch" is required.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException if an I/O exception occurs
     */
    @Handler
    public void onWatchFile(WatchFile event, Channel channel)
            throws IOException {
        final Path path = event.path().toAbsolutePath();
        synchronized (subscriptions) {
            addSubscription(path, channel);
        }
    }

    private Subscription addSubscription(Path watched, Channel channel) {
        var subs = new Subscription(watched, channel);
        try {
            // Using computeIfAbsent causes recursive update
            var watcher = subscriptions.get(watched.getParent());
            if (watcher == null) {
                watcher = watcherRegistry.register(watched.getParent());
            }
            watcher.add(subs);
            if (Files.exists(watched)) {
                Path real = watched.toRealPath();
                if (!real.equals(watched)) {
                    addSubscription(real, channel).linkedFrom(subs);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e,
                () -> "Cannot watch: " + e.getMessage());
        }
        return subs;
    }

    private void handleWatchEvent(Path directory) {
        Optional.ofNullable(subscriptions.get(directory))
            .ifPresent(DirectorySubscription::directoryChanged);
    }

    /**
     * The Class WatcherRegistry.
     */
    private final class WatcherRegistry {
        private final Map<FileSystem, Watcher> watchers
            = new ConcurrentHashMap<>();

        private Watcher watcher(Path path) {
            @SuppressWarnings("PMD.CloseResource")
            Watcher watcher = watchers.get(path.getFileSystem());
            if (watcher == null) {
                try {
                    watcher = new Watcher(path.getFileSystem());
                    watchers.put(path.getFileSystem(), watcher);
                } catch (IOException e) {
                    logger.log(Level.WARNING, e,
                        () -> "Cannot get watch service: " + e.getMessage());
                    return null;
                }
            }
            return watcher;
        }

        /**
         * Register.
         *
         * @param toWatch the to watch
         * @return the directory subscription
         */
        public DirectorySubscription register(Path toWatch) {
            Watcher watcher = watcher(toWatch);
            if (watcher == null) {
                return null;
            }
            try {
                var watcherRef = new DirectorySubscription(
                    toWatch.register(watcher.watchService,
                        ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
                subscriptions.put(toWatch, watcherRef);
                return watcherRef;
            } catch (IOException e) {
                logger.log(Level.WARNING, e,
                    () -> "Cannot watch: " + e.getMessage());
            }
            return null;
        }

    }

    /**
     * The Class Watcher.
     */
    private final class Watcher {
        private final WatchService watchService;

        private Watcher(FileSystem fileSystem) throws IOException {
            watchService = fileSystem.newWatchService();
            Thread.ofVirtual().name(fileSystem.toString() + " watcher")
                .start(() -> {
                    while (true) {
                        try {
                            WatchKey key = watchService.take();
                            // Events have to be consumed
                            key.pollEvents();
                            if (!(key.watchable() instanceof Path)) {
                                key.reset();
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
        }
    }

    /**
     * The Class DirectorySubscription.
     */
    private class DirectorySubscription {
        private final WatchKey watchKey;
        private final List<Subscription> watched;

        /**
         * Instantiates a new directory watcher.
         *
         * @param watchKey the watch key
         */
        public DirectorySubscription(WatchKey watchKey) {
            this.watchKey = watchKey;
            watched = Collections.synchronizedList(new ArrayList<>());
        }

        /**
         * Adds the subscription.
         *
         * @param subs the subs
         */
        public void add(Subscription subs) {
            watched.add(subs);
        }

        /**
         * Removes the subscription.
         *
         * @param subs the subs
         */
        public void remove(Subscription subs) {
            watched.remove(subs);
            if (watched.isEmpty()) {
                subscriptions.remove(subs.directory());
                watchKey.cancel();
            }

        }

        /**
         * Directory changed.
         */
        public void directoryChanged() {
            // Prevent concurrent modification exception
            List.copyOf(watched).forEach(Subscription::handleChange);
        }
    }

    /**
     * The Class Registree.
     */
    private class Subscription {
        private WeakReference<Channel> notifyOn;
        private final Path path;
        private Subscription linkedFrom;
        private Subscription linksTo;
        private Instant lastModified;

        /**
         * Instantiates a new subscription.
         *
         * @param path the path
         * @param notifyOn the notify on
         */
        @SuppressWarnings("PMD.UseVarargs")
        public Subscription(Path path, Channel notifyOn) {
            this.notifyOn = new WeakReference<>(notifyOn);
            this.path = path;
            updateLastModified();
        }

        /**
         * Return the directoy of this subscription's path.
         *
         * @return the path
         */
        public Path directory() {
            return path.getParent();
        }

        /**
         * Linked from.
         *
         * @param symLinkSubs the sym link subs
         * @return the subscription
         */
        public Subscription linkedFrom(Subscription symLinkSubs) {
            linkedFrom = symLinkSubs;
            symLinkSubs.linksTo = this;
            notifyOn = null;
            return this;
        }

        /**
         * Removes the subscription.
         */
        public void remove() {
            synchronized (subscriptions) {
                if (linksTo != null) {
                    linksTo.remove();
                }
                var directory = path.getParent();
                var watchInfo = subscriptions.get(directory);
                if (watchInfo == null) {
                    // Shouldn't happen, but...
                    return;
                }
                watchInfo.remove(this);
            }
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

        /**
         * Handle change.
         */
        private void handleChange() {
            Subscription watched = Optional.ofNullable(linkedFrom).orElse(this);

            // Check if channel is still valid
            Channel channel = watched.notifyOn.get();
            if (channel == null) {
                watched.remove();
                return;
            }

            // Evaluate change from the perspective of "watched"
            Instant prevModified = watched.lastModified;
            watched.updateLastModified();
            if (prevModified == null) {
                // Check if created
                if (watched.lastModified != null) {
                    // Yes, created.
                    fire(new FileChanged(watched.path,
                        FileChanged.Kind.CREATED), channel);
                    checkLink(watched, channel);
                }
                return;
            }

            // File has existed (prevModified != null)
            if (watched.lastModified == null) {
                // ... but is now deleted
                if (watched.linksTo != null) {
                    watched.linksTo.remove();
                }
                fire(new FileChanged(watched.path, FileChanged.Kind.DELETED),
                    channel);
                return;
            }

            // Check if modified
            if (!prevModified.equals(watched.lastModified)) {
                fire(new FileChanged(watched.path, FileChanged.Kind.MODIFIED),
                    channel);
                checkLink(watched, channel);
            }
        }

        private void checkLink(Subscription watched, Channel channel) {
            try {
                Path curTarget = watched.path.toRealPath();
                if (!curTarget.equals(watched.path)) {
                    // watched is symbolic link
                    if (watched.linksTo == null) {
                        addSubscription(curTarget, channel).linkedFrom(watched);
                        return;
                    }
                    if (!watched.linksTo.path.equals(curTarget)) {
                        // Link target has changed
                        watched.linksTo.remove();
                        addSubscription(curTarget, channel).linkedFrom(watched);
                    }

                }
            } catch (IOException e) { // NOPMD
                // Race condition, target deleted?
            }
        }
    }
}
