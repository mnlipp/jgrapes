/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022,2023 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.event.ConnectionListener;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.StoreEvent;
import jakarta.mail.event.StoreListener;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IdleManager;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Subchannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.ConnectError;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Opening;
import org.jgrapes.mail.events.MailFoldersUpdated;
import org.jgrapes.mail.events.MailMonitorOpened;
import org.jgrapes.mail.events.OpenMailMonitor;
import org.jgrapes.mail.events.UpdateMailFolders;
import org.jgrapes.util.Password;

/**
 * A component that opens mail stores and monitors mail folders for 
 * mails. After establishing a connection to a store and selected 
 * folders (see {@link #onOpenMailMonitor(OpenMailMonitor, Channel)}), 
 * the existing and all subsequently arriving mails will be sent 
 * downstream using {@link MailFoldersUpdated} events.
 * 
 * This implementation uses the {@link IdleManager}. The 
 * {@link IdleManager} works only if its {@link IdleManager#watch}
 * method is invoked (for a folder) after any operation on that folder. 
 * Note that operations such as e.g. setting the deleted flag of 
 * a message is also an operation on a folder.
 * 
 * Folders are updated in response to an {@link UpdateMailFolders} event 
 * or when the store signals the arrival of new messages. Information 
 * about the folders is delivered by a {@link MailFoldersUpdated} event. 
 * Folders may be freely used while handling the event, because the
 * folders will be re-registered with the {@link IdleManager}
 * when the {@link MailFoldersUpdated} event completes.
 * Any usage of folders independent of handling the events mentioned
 * will result in a loss of the monitor function.
 * 
 * If required, the monitor function may be reestablished any time
 * by firing a {@link UpdateMailFolders} event for the folders used.
 */
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
    "PMD.DataflowAnomalyAnalysis", "PMD.ExcessiveImports",
    "PMD.CouplingBetweenObjects" })
public class MailMonitor extends MailConnectionManager<
        MailMonitor.MonitorChannel, OpenMailMonitor> {

    private Duration maxIdleTime = Duration.ofMinutes(25);
    private static IdleManager idleManager;
    private final EventPipeline retrievals = newEventPipeline();

    /**
     * Creates a new server using the given channel.
     * 
     * @param componentChannel the component's channel
     */
    public MailMonitor(Channel componentChannel) {
        super(componentChannel);
    }

    @Override
    protected boolean connectionsGenerate() {
        return true;
    }

    /**
     * Sets the maximum idle time. A running {@link IMAPFolder#idle()}
     * is terminated and renewed after this time. Defaults to 25 minutes.
     *
     * @param maxIdleTime the new max idle time
     */
    public MailMonitor setMaxIdleTime(Duration maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
        return this;
    }

    /**
     * Returns the max idle time.
     *
     * @return the duration
     */
    public Duration maxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Configure the component. Currently, only max idle time
     * is supported.
     *
     * @param values the values
     */
    @Override
    protected void configureComponent(Map<String, String> values) {
        Optional.ofNullable(values.get("maxIdleTime"))
            .map(Integer::parseInt).map(Duration::ofSeconds)
            .ifPresent(this::setMaxIdleTime);
    }

    /**
     * Open a store as specified by the event and monitor the folders
     * (also specified by the event). Information about all existing 
     * and all subsequently arriving mails will be signaled downstream 
     * using {@link MailFoldersUpdated} events.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onOpenMailMonitor(OpenMailMonitor event, Channel channel) {
        Properties sessionProps = new Properties(mailProps);
        sessionProps.putAll(event.mailProperties());
        sessionProps.put("mail.imap.usesocketchannels", true);
        Session session = Session.getInstance(sessionProps,
            // Workaround for class loading problem in OSGi with j.m. 2.1.
            // Authenticator's classpath allows accessing provider's service.
            // See https://github.com/eclipse-ee4j/mail/issues/631
            new Authenticator() {
                @Override
                @SuppressWarnings("PMD.StringInstantiation")
                protected PasswordAuthentication
                        getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        sessionProps.getProperty("mail.user"),
                        new String(event.password().or(() -> password())
                            .map(Password::password).orElse(new char[0])));
                }
            });

        try {
            synchronized (MailMonitor.class) {
                // Cannot be created earlier, need session.
                if (idleManager == null) {
                    idleManager = new IdleManager(session,
                        Components.defaultExecutorService());
                }
            }
            new MonitorChannel(event, channel, session.getStore(),
                sessionProps.getProperty("mail.user"),
                event.password().or(this::password).orElse(null));
        } catch (NoSuchProviderException e) {
            fire(new ConnectError(event, "Cannot create store.", e));
        } catch (IOException e) {
            fire(new IOError(event, "Cannot create resource.", e));
        }
    }

    /**
     * Retrieves the folders specified in the event.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onUpdateFolders(UpdateMailFolders event, MailChannel channel) {
        if (!connections.contains(channel)) {
            return;
        }
        // This can take very long.
        retrievals
            .submit(() -> ((MonitorChannel) channel).onUpdateFolders(event));
    }

    /**
     * The Enum ChannelState.
     */
    @SuppressWarnings("PMD.FieldNamingConventions")
    private enum ChannelState {
        Opening {
            @Override
            public boolean isOpening() {
                return true;
            }
        },
        Open {
            @Override
            public boolean isOpen() {
                return true;
            }
        },
        Reopening {
            @Override
            public boolean isOpening() {
                return true;
            }
        },
        Reopened {
            @Override
            public boolean isOpen() {
                return true;
            }
        },
        Closing,
        Closed;

        /**
         * Checks if is open.
         *
         * @return true, if is open
         */
        public boolean isOpen() {
            return false;
        }

        /**
         * Checks if is opening.
         *
         * @return true, if is opening
         */
        public boolean isOpening() {
            return false;
        }
    }

    /**
     * The specific implementation of the {@link MailChannel}.
     */
    protected class MonitorChannel extends
            MailConnectionManager<MailMonitor.MonitorChannel,
                    OpenMailMonitor>.AbstractMailChannel
            implements ConnectionListener, StoreListener {

        private final EventPipeline requestPipeline;
        private ChannelState state = ChannelState.Opening;
        private final Store store;
        private final String user;
        private final Password password;
        private final String[] subscribed;
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        private final Map<String, Folder> folderCache = new HashMap<>();
        private final Timer idleTimer;

        /**
         * Instantiates a new monitor channel.
         *
         * @param event the event that triggered the creation
         * @param mainChannel the main channel (of this {@link Subchannel})
         * @param store the store
         * @param user the user
         * @param password the password
         */
        public MonitorChannel(OpenMailMonitor event, Channel mainChannel,
                Store store, String user, Password password) {
            super(event, mainChannel);
            this.store = store;
            this.user = user;
            this.password = password;
            this.subscribed = event.folderNames();
            requestPipeline = event.processedBy().get();
            store.addConnectionListener(this);
            store.addStoreListener(this);
            idleTimer = Components.schedule(t -> {
                requestPipeline.fire(new UpdateMailFolders(), this);
            }, maxIdleTime);
            connect(
                t -> downPipeline().fire(new ConnectError(event, t),
                    mainChannel));
        }

        /**
         * Attempt connections until connected. Attempts are stopped
         * if it is the first time that the connection is to be
         * established and the error indicates that the connection
         * will never succeed (e.g. due to an authentication
         * problem).
         *
         * @param onOpenFailed the on open failed
         */
        private void connect(Consumer<Throwable> onOpenFailed) {
            synchronized (this) {
                if (state.isOpen()) {
                    return;
                }
                activeEventPipeline().executorService().submit(() -> {
                    while (state.isOpening()) {
                        try {
                            attemptConnect(onOpenFailed);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
            }
        }

        /**
         * Single connection attempt.
         *
         * @param onOpenFailed the on open failed
         * @throws InterruptedException the interrupted exception
         */
        @SuppressWarnings({ "PMD.AvoidInstanceofChecksInCatchClause",
            "PMD.StringInstantiation" })
        private void attemptConnect(Consumer<Throwable> onOpenFailed)
                throws InterruptedException {
            try {
                store.connect(user, new String(password.password()));
                synchronized (this) {
                    if (state == ChannelState.Opening) {
                        state = ChannelState.Open;
                    } else {
                        state = ChannelState.Reopened;
                    }
                }
            } catch (MessagingException e) {
                synchronized (this) {
                    if (state == ChannelState.Opening
                        && (e instanceof AuthenticationFailedException
                            || e instanceof NoSuchProviderException)) {
                        logger.log(Level.WARNING,
                            "Connecting to store failed, closing.", e);
                        state = ChannelState.Closed;
                        super.close();
                        if (onOpenFailed != null) {
                            onOpenFailed.accept(e);
                        }
                        return;
                    }
                }
                logger.log(Level.WARNING,
                    "(Re)connecting to store failed, retrying.", e);
                Thread.sleep(5000);
            }
        }

        /**
         * Close the connection to the store.
         */
        @Override
        public void close() {
            synchronized (this) {
                if (state == ChannelState.Closing
                    || state == ChannelState.Closed) {
                    return;
                }
                state = ChannelState.Closing;
            }

            idleTimer.cancel();
            try {
                // Initiate close, callback will inform downstream components.
                store.close();
            } catch (MessagingException e) {
                // According to the documentation, the listeners should
                // be invoked nevertheless.
                logger.log(Level.WARNING, "Cannot close connection properly.",
                    e);
            }
        }

        /**
         * Callback from store.connect is the connection is successful.
         *
         * @param event the event
         */
        @Override
        @SuppressWarnings({ "PMD.GuardLogStatement",
            "PMD.AvoidDuplicateLiterals" })
        public void opened(ConnectionEvent event) {
            folderCache.clear();
            if (state == ChannelState.Reopened) {
                // This is a re-open, only retrieve messages.
                requestPipeline.fire(new UpdateMailFolders(), this);
                return;
            }
            // (1) Opening, (2) Opened, (3) start retrieving mails
            downPipeline().fire(Event.onCompletion(new Opening<Void>(),
                o -> downPipeline().fire(
                    Event.onCompletion(
                        new MailMonitorOpened(openEvent(), store),
                        p -> requestPipeline
                            .fire(new UpdateMailFolders(), this)),
                    this)),
                this);
        }

        /**
         * According to the documentation,
         * {@link ConnectionEvent#DISCONNECTED} is currently not
         * used. It's implemented nevertheless and called explicitly.
         *
         * @param event the event or `null` if called explicitly
         */
        @Override
        public void disconnected(ConnectionEvent event) {
            synchronized (this) {
                folderCache.clear();
                if (state.isOpen()) {
                    state = ChannelState.Reopening;
                    connect(null);
                }
            }
        }

        /**
         * Callback that indicates the connection close,
         * can be called any time by jakarta mail.
         * 
         * Whether closing is intended (callback after a call to 
         * {@link #close}) can be checked by looking at the state. 
         *
         * @param event the event
         */
        @Override
        public void closed(ConnectionEvent event) {
            // Ignore if already closed.
            if (state == ChannelState.Closed) {
                return;
            }

            // Handle involuntary close by reopening.
            if (state != ChannelState.Closing) {
                disconnected(event);
                return;
            }

            // Cleanup and remove channel.
            synchronized (this) {
                state = ChannelState.Closed;
                folderCache.clear();
            }
            downPipeline().fire(new Closed<Void>(), this);
            super.close();
        }

        @Override
        public void notification(StoreEvent event) {
            if (event.getMessage().contains("SocketException")) {
                logger.fine(() -> "Problem with store: " + event.getMessage());
                if (store.isConnected()) {
                    logger.fine(() -> "Updating folders to resume");
                    requestPipeline.fire(new UpdateMailFolders(), this);
                    return;
                }
                logger.fine(() -> "Reconnecting to resume");
                disconnected(null);
            }
        }

        /**
         * Retrieve the new messages from the folders specified in the
         * event.
         * 
         * @param event
         */
        @SuppressWarnings({ "PMD.CognitiveComplexity",
            "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.AvoidDuplicateLiterals" })
        public void onUpdateFolders(UpdateMailFolders event) {
            List<Folder> folders = new ArrayList<>();
            List<Message> newMsgs = new ArrayList<>();
            if (store.isConnected()) {
                Set<String> folderNames
                    = new HashSet<>(Arrays.asList(subscribed));
                if (event.folderNames().length > 0) {
                    folderNames.retainAll(Arrays.asList(event.folderNames()));
                }
                try {
                    for (var folderName : folderNames) {
                        @SuppressWarnings("PMD.CloseResource")
                        Folder folder = getFolder(folderName);
                        if (folder == null) {
                            continue;
                        }
                        folders.add(folder);
                    }
                } catch (FolderClosedException e) {
                    disconnected(null);
                }
            } else {
                disconnected(null);
            }
            event.setResult(folders);
            Event.onCompletion(event, e -> downPipeline().fire(Event
                .onCompletion(new MailFoldersUpdated(folders, newMsgs),
                    this::refreshWatches),
                this));
        }

        @SuppressWarnings({ "PMD.GuardLogStatement",
            "PMD.AvoidRethrowingException", "PMD.CloseResource" })
        private Folder getFolder(String folderName)
                throws FolderClosedException {
            synchronized (folderCache) {
                Folder folder = folderCache.get(folderName);
                if (folder != null) {
                    return folder;
                }
                try {
                    folder = store.getFolder(folderName);
                    if (folder == null || !folder.exists()) {
                        logger.fine(() -> "No folder \"" + folderName
                            + "\" in store " + store);
                        return null;
                    }
                    folder.open(Folder.READ_WRITE);
                    folderCache.put(folderName, folder);
                    // Add MessageCountListener to listen for new messages.
                    folder.addMessageCountListener(new MessageCountAdapter() {
                        @Override
                        public void
                                messagesAdded(MessageCountEvent countEvent) {
                            retrievals.submit("UpdateFolder",
                                () -> updateFolders(countEvent));
                        }

                        @Override
                        public void
                                messagesRemoved(MessageCountEvent countEvent) {
                            retrievals.submit("UpdateFolder",
                                () -> updateFolders(countEvent));
                        }
                    });
                    return folder;
                } catch (FolderClosedException e) {
                    throw e;
                } catch (MessagingException e) {
                    logger.log(Level.FINE,
                        "Cannot open folder: " + e.getMessage(), e);
                }
                return null;
            }
        }

        @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.GuardLogStatement" })
        private void updateFolders(MessageCountEvent event) {
            List<Message> newMsgs = new ArrayList<>();
            if (event.getType() == MessageCountEvent.ADDED) {
                newMsgs.addAll(Arrays.asList(event.getMessages()));
            } else if (event.getType() != MessageCountEvent.REMOVED) {
                return;
            }
            downPipeline().fire(
                Event.onCompletion(
                    new MailFoldersUpdated(
                        new ArrayList<>(folderCache.values()), newMsgs),
                    this::refreshWatches),
                this);
        }

        /**
         * Registers the folders from which messages have been received
         * with the {@link IdleManager}.
         *
         * @param event the event
         */
        @SuppressWarnings({ "PMD.CloseResource", "PMD.UnusedPrivateMethod" })
        private void refreshWatches(MailFoldersUpdated event) {
            if (!state.isOpen()) {
                return;
            }
            for (Folder folder : event.folders()) {
                try {
                    idleManager.watch(getFolder(folder.getFullName()));
                } catch (MessagingException e) {
                    logger.log(Level.WARNING, "Cannot watch folder.",
                        e);
                }
            }
            idleTimer.reschedule(maxIdleTime);
        }
    }

    @Override
    public String toString() {
        return Components.objectName(this);
    }

}
