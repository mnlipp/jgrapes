/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail;

import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.Authenticator;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.mail.events.ReceivedMailMessage;
import org.jgrapes.util.JsonConfigurationStore;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * A component that monitors the INBOX of a mail account.
 * The component uses [Jakarta Mail](https://eclipse-ee4j.github.io/mail/)
 * to connect to a mail server.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class MailMonitor extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(MailMonitor.class.getName());

    private final Properties mailProps = new Properties();
    private String password;
    private Duration maxIdleTime;
    private Duration pollInterval;
    private Store store;
    private Thread monitorThread;
    private boolean running;

    /**
     * Creates a new component with its channel set to itself.
     */
    public MailMonitor() {
        // Nothing to do.
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
    public MailMonitor(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Creates a new component base like {@link #MailMonitor(Channel)}
     * but with channel mappings for {@link Handler} annotations.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param channelReplacements the channel replacements to apply
     * to the `channels` elements of the {@link Handler} annotations
     */
    public MailMonitor(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public MailMonitor setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Sets the maximum idle time. A running {@link IMAPFolder#idle()}
     * is terminated and renewed after this time.
     *
     * @param maxIdleTime the new max idle time
     */
    public MailMonitor setMaxIdleTime(Duration maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
        return this;
    }

    /**
     * Sets the mail properties. See 
     * [the Jakarta Mail](https://jakarta.ee/specifications/mail/2.0/apidocs/jakarta.mail/jakarta/mail/package-summary.html)
     * documentation for available settings.
     *
     * @param props the props
     * @return the mail monitor
     */
    public MailMonitor setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
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
     * Sets the poll interval. Polling is used when the idle command
     * is not available.
     *
     * @param pollInterval the pollInterval to set
     */
    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    /**
     * Returns the poll interval.
     *
     * @return the pollInterval
     */
    public Duration pollInterval() {
        return pollInterval;
    }

    /**
     * Configure the component, using any values found under the 
     * {@link #componentPath()}. Properties for configuring
     * Jakarta Mail are taken from a sub-section "`mail`". The
     * valid keys are the properties defined for
     * [Jakarta Mail](https://jakarta.ee/specifications/mail/2.0/apidocs/jakarta.mail/jakarta/mail/package-summary.html)
     * with the prefix "`mail.`" removed to avoid unnecessary redundancy.
     * 
     * Here's an example configuration file for the 
     * {@link JsonConfigurationStore}.
     * 
     * ```json
     * {
     *     "/CleanMailsUntilStop": {
     *         "/MailMonitor": {
     *             "/mail": {
     *                 "host": "...",
     *                 "store.protocol": "imap",
     *                 "imap.ssl.enable": "true",
     *                 "imap.port": 993,
     *                 "user": "..."
     *             },
     *             "password": "..."
     *         }
     *     }
     * }
     * ```
     *
     * @param event the event
     */
    @Handler
    public void onConfigUpdate(ConfigurationUpdate event) {
        event.values(componentPath()).ifPresent(c -> {
            setPassword(c.get("password"));
            setMaxIdleTime(Optional.ofNullable(c.get("maxIdleTime"))
                .map(Integer::parseInt).map(Duration::ofSeconds)
                .orElse(Duration.ofMinutes(25)));
            setPollInterval(Optional.ofNullable(c.get("pollInterval"))
                .map(Integer::parseInt).map(Duration::ofSeconds)
                .orElse(Duration.ofMinutes(5)));
        });
        event.values(componentPath() + "/mail").ifPresent(c -> {
            for (var e : c.entrySet()) {
                mailProps.put("mail." + e.getKey(), e.getValue());
            }
        });
    }

    /**
     * Run the monitor.
     *
     * @param event the event
     * @throws NoSuchProviderException the no such provider exception
     */
    @Handler
    public void onStart(Start event) throws NoSuchProviderException {
        Session session
            = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication
                        getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        mailProps.getProperty("mail.user"), password);
                }
            });
        store = session.getStore();

        // Start monitoring
        running = true;
        monitorThread = new Thread(() -> monitor());
        monitorThread.setDaemon(true);
        monitorThread.start();
        registerAsGenerator();
    }

    @SuppressWarnings({ "PMD.GuardLogStatement", "PMD.AvoidDuplicateLiterals" })
    private void monitor() {
        Thread.currentThread().setName(Components.objectName(this));
        while (running) {
            try {
                store.connect();
                Folder folder = store.getFolder("INBOX");
                if (folder == null || !folder.exists()) {
                    logger.log(Level.SEVERE, "Store has no INBOX.");
                    throw new IllegalStateException("No INBOX");
                }
                folder.open(Folder.READ_WRITE);
                processMessages(folder);
            } catch (MessagingException e) {
                logger.log(Level.WARNING,
                    "Cannot open INBOX, will retry: " + e.getMessage(), e);
                closeStore();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        closeStore();
    }

    @SuppressWarnings({ "PMD.GuardLogStatement", "PMD.AvoidDuplicateLiterals" })
    private void closeStore() {
        if (store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                logger.log(Level.WARNING,
                    "Cannot close store: " + e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings({ "PMD.GuardLogStatement", "PMD.CognitiveComplexity",
        "PMD.NPathComplexity" })
    private void processMessages(Folder folder) {
        try {
            // Process existing (and newly arrived while processing)
            int start = 1;
            int end = folder.getMessageCount();
            while (running && start <= end) {
                Message[] msgs = folder.getMessages(start, end);
                for (Message msg : msgs) {
                    processMessage(msg);
                }
                // Check if more messages have arrived
                start = end + 1;
                end = folder.getMessageCount();
            }
            folder.expunge();

            // Add MessageCountListener to listen for new messages.
            // The listener will only be invoked when we do another
            // operation such as idle().
            folder.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent countEvent) {
                    Message[] msgs = countEvent.getMessages();
                    for (Message msg : msgs) {
                        processMessage(msg);
                    }
                    try {
                        folder.expunge();
                    } catch (MessagingException e) {
                        logger.log(Level.WARNING,
                            "Problem expunging folder: " + e.getMessage(), e);
                    }
                }
            });
            boolean canIdle = true;
            while (running) {
                if (canIdle) {
                    @SuppressWarnings("PMD.EmptyCatchBlock")
                    Timer idleTimout = Components.schedule(timer -> {
                        try {
                            folder.getMessageCount();
                        } catch (MessagingException e) {
                            // Just trying to be nice here.
                        }
                    }, maxIdleTime);
                    try {
                        ((IMAPFolder) folder).idle();
                    } catch (MessagingException e) {
                        canIdle = false;
                    } finally {
                        idleTimout.cancel();
                    }
                }
                if (!canIdle) {
                    try {
                        Thread.sleep(pollInterval.toMillis());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        continue;
                    }
                    folder.getMessageCount();
                }
            }
        } catch (MessagingException e) {
            logger.log(Level.WARNING,
                "Problem processing messages: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private void processMessage(Message msg) {
        try {
            if (msg.getFlags().contains(Flag.DELETED)) {
                return;
            }
            fire(new ReceivedMailMessage(msg));
        } catch (MessagingException e) {
            logger.log(Level.WARNING,
                "Problem processing message: " + e.getMessage(), e);
        }
    }

    /**
     * Stop the monitor.
     *
     * @param event the event
     */
    @Handler
    @SuppressWarnings("PMD.GuardLogStatement")
    public void onStop(Stop event) {
        running = false;
        // interrupt() does not terminate idle(), closing the store does.
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                logger.log(Level.WARNING,
                    "Cannot close store: " + e.getMessage(), e);
            }
        }
        // In case we don't use idle() but are sleeping.
        monitorThread.interrupt();
        try {
            monitorThread.join(1000);
        } catch (InterruptedException e) {
            // Ignored
        }
        unregisterAsGenerator();
    }

}
