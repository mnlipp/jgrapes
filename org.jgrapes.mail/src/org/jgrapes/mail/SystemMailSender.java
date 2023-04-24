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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.mail.events.SendMessage;
import org.jgrapes.util.Password;

/**
 * A component that sends mail using a system wide (user independent)
 * configuration to access the server.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SystemMailSender extends MailComponent {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(SystemMailSender.class.getName());

    private Session session;
    private Transport transport;
    private Duration maxIdleTime = Duration.ofMinutes(1);
    private Timer idleTimer;

    /**
     * Creates a new component with its channel set to itself.
     */
    public SystemMailSender() {
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
    public SystemMailSender(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Creates a new component base like {@link #SimpleMailSender(Channel)}
     * but with channel mappings for {@link Handler} annotations.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param channelReplacements the channel replacements to apply
     * to the `channels` elements of the {@link Handler} annotations
     */
    public SystemMailSender(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
    }

    /**
     * Sets the mail properties. See 
     * [the Jakarta Mail](https://jakarta.ee/specifications/mail/2.0/apidocs/jakarta.mail/jakarta/mail/package-summary.html)
     * documentation for available settings.
     *
     * @param props the props
     * @return the mail monitor
     */
    public SystemMailSender setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
        return this;
    }

    /**
     * Sets the maximum idle time. An open connection to the mail server
     * is closed after this time.
     *
     * @param maxIdleTime the new max idle time
     */
    public SystemMailSender setMaxIdleTime(Duration maxIdleTime) {
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
            .ifPresent(d -> setMaxIdleTime(d));
    }

    /**
     * Start the component.
     *
     * @param event the event
     * @throws MessagingException 
     */
    @Handler
    public void onStart(Start event) throws MessagingException {
        session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication
                    getPasswordAuthentication() {
                return new PasswordAuthentication(
                    mailProps.getProperty("mail.user"),
                    password().map(Password::password).map(String::new)
                        .orElse(null));
            }
        });
        transport = session.getTransport();
        transport.connect();
        idleTimer
            = Components.schedule(timer -> closeConnection(), maxIdleTime);
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private void closeConnection() {
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

    /**
     * Sends the message as specified by the event.
     *
     * @param event the event
     * @throws MessagingException the messaging exception
     */
    @Handler
    public void onMessage(SendMessage event) throws MessagingException {
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
                = Components.schedule(timer -> closeConnection(), maxIdleTime);
        }
        msg.saveChanges();
        transport.sendMessage(msg, msg.getAllRecipients());
    }

    /**
     * Stop the monitor.
     *
     * @param event the event
     * @throws MessagingException 
     */
    @Handler
    @SuppressWarnings("PMD.GuardLogStatement")
    public void onStop(Stop event) {
        closeConnection();
    }

}
