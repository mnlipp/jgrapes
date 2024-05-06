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

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Subchannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.mail.events.OpenMailSender;
import org.jgrapes.mail.events.SendMailMessage;
import org.jgrapes.util.Password;

/**
 * A component that sends mail using a system wide or user specific
 * connection.
 * 
 * The system wide connection is created upon the start of the component.
 * Additional connections can be created by firing events of type 
 * {@link OpenMailSender}.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class MailSender
        extends MailConnectionManager<MailSender.SenderChannel, Event<?>> {

    private Duration maxIdleTime = Duration.ofMinutes(1);
    private SenderChannel systemChannel;

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
    public MailSender(Channel componentChannel) {
        super(componentChannel);
    }

    @Override
    protected boolean connectionsGenerate() {
        return false;
    }

    /**
     * Sets the mail properties. See the Jakarta Mail documentation 
     * for available settings.
     *
     * @param props the props
     * @return the mail monitor
     */
    public MailSender setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
        return this;
    }

    /**
     * Sets the maximum idle time. An open connection to the mail server
     * is closed after this time.
     *
     * @param maxIdleTime the new max idle time
     */
    public MailSender setMaxIdleTime(Duration maxIdleTime) {
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

    @Override
    protected void configureComponent(Map<String, String> values) {
        Optional.ofNullable(values.get("maxIdleTime"))
            .map(Integer::parseInt).map(Duration::ofSeconds)
            .ifPresent(this::setMaxIdleTime);
    }

    /**
     * Start the component.
     *
     * @param event the event
     * @throws MessagingException 
     */
    @Handler
    public void onStart(Start event) throws MessagingException {
        systemChannel = new SenderChannel(event,
            channel(), mailProps, password());
    }

    /**
     * Open a connection for sending mail as specified by the event.
     * 
     * Properties configured for the component are used as fallbacks,
     * so simply sending an event without specific properties opens
     * another system connection.
     *
     * @param event the event
     * @param channel the channel
     * @throws MessagingException 
     */
    @Handler
    public void onOpenMailSender(OpenMailSender event, Channel channel)
            throws MessagingException {
        Properties sessionProps = new Properties(mailProps);
        sessionProps.putAll(event.mailProperties());
        new SenderChannel(event, channel, sessionProps,
            event.password().or(this::password));
    }

    /**
     * Sends the message as specified by the event.
     *
     * @param event the event
     * @throws MessagingException the messaging exception
     */
    @Handler
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void onMessage(SendMailMessage event, Channel channel)
            throws MessagingException {
        if (channel instanceof SenderChannel chan
            && chan.mailSender() == this) {
            chan.sendMessage(event);
        } else {
            systemChannel.sendMessage(event);
        }
    }

    /**
     * The specific implementation of the {@link MailChannel}.
     */
    protected class SenderChannel extends MailConnectionManager<
            MailSender.SenderChannel, Event<?>>.AbstractMailChannel {

        private final Session session;
        private final Transport transport;
        private Timer idleTimer;

        /**
         * Instantiates a new monitor channel.
         *
         * @param event the event that triggered the creation
         * @param mainChannel the main channel (of this {@link Subchannel})
         * @param sessionProps the session properties
         * @param password the password
         * @throws MessagingException the messaging exception
         */
        public SenderChannel(Event<?> event, Channel mainChannel,
                Properties sessionProps, Optional<Password> password)
                throws MessagingException {
            super(event, mainChannel);
            var passwd = password.map(Password::password).map(String::new)
                .orElse(null);
            session = Session.getInstance(sessionProps, new Authenticator() {
                // Workaround for class loading problem in OSGi with j.m. 2.1.
                // Authenticator's classpath allows accessing provider's
                // service. See https://github.com/eclipse-ee4j/mail/issues/631
                @Override
                protected PasswordAuthentication
                        getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        sessionProps.getProperty("mail.user"), passwd);
                }
            });
            transport = session.getTransport();
            transport.connect(sessionProps.getProperty("mail.user"), passwd);
            idleTimer
                = Components.schedule(timer -> closeConnection(), maxIdleTime);
        }

        private MailSender mailSender() {
            return MailSender.this;
        }

        /**
         * Send the message provided by the event.
         *
         * @param event the event
         * @throws MessagingException 
         */
        protected void sendMessage(SendMailMessage event)
                throws MessagingException {
            synchronized (transport) {
                if (idleTimer != null) {
                    idleTimer.cancel();
                    idleTimer = null;
                }
            }
            Message msg = new MimeMessage(session);
            if (event.from() != null) {
                msg.setFrom(event.from());
            } else {
                msg.setFrom();
            }
            msg.setRecipients(Message.RecipientType.TO, event.to());
            msg.setRecipients(Message.RecipientType.CC, event.cc());
            msg.setRecipients(Message.RecipientType.BCC, event.bcc());
            msg.setSentDate(new Date());
            for (var header : event.headers().entrySet()) {
                msg.setHeader(header.getKey(), header.getValue());
            }
            msg.setSubject(event.subject());
            msg.setContent(event.content());

            synchronized (transport) {
                if (!transport.isConnected()) {
                    transport.connect();
                }
                idleTimer
                    = Components.schedule(timer -> closeConnection(),
                        maxIdleTime);
            }
            msg.saveChanges();
            transport.sendMessage(msg, msg.getAllRecipients());
        }

        @Override
        public void close() {
            closeConnection();
            super.close();
        }

        /**
         * Close the connection (not the channel). May be reopened
         * later if closed due to idle time over.
         */
        @SuppressWarnings("PMD.GuardLogStatement")
        protected void closeConnection() {
            synchronized (transport) {
                if (idleTimer != null) {
                    idleTimer.cancel();
                    idleTimer = null;
                }
                if (transport.isConnected()) {
                    try {
                        transport.close();
                    } catch (MessagingException e) {
                        logger.log(Level.WARNING,
                            "Cannot close connection: " + e.getMessage(), e);
                    }
                }
            }
        }

    }

}
