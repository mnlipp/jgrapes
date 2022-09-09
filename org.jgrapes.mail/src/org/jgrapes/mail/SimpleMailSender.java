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

import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.mail.events.ReceivedMailMessage;
import org.jgrapes.mail.events.SendMailMessage;
import org.jgrapes.util.JsonConfigurationStore;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * A component that sends mail.
 * The component uses [Jakarta Mail](https://eclipse-ee4j.github.io/mail/)
 * to connect to a mail server.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SimpleMailSender extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(SimpleMailSender.class.getName());

    private final Properties mailProps = new Properties();
    private String password;
    private Session session;
    private Transport transport;

    /**
     * Creates a new component with its channel set to itself.
     */
    public SimpleMailSender() {
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
    public SimpleMailSender(Channel componentChannel) {
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
    public SimpleMailSender(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public SimpleMailSender setPassword(String password) {
        this.password = password;
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
    public SimpleMailSender setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
        return this;
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
     *     "/SendMail": {
     *         "/MailSender": {
     *             "/mail": {
     *                 "host": "...",
     *                 "transport.protocol": "smtp",
     *                 "smtp.ssl.enable": "true",
     *                 "smtp.port": 465,
     *                 "smtp.auth": true,
     *                 "user": "...",
     *                 "debug": true
     *              },
     *              "password": "..."
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
     * @throws NoSuchProviderException 
     */
    @Handler
    public void onStart(Start event) throws NoSuchProviderException {
        session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication
                    getPasswordAuthentication() {
                return new PasswordAuthentication(
                    mailProps.getProperty("mail.user"), password);
            }
        });
        transport = session.getTransport();
    }

    @Handler
    public void onMessage(SendMailMessage event) throws MessagingException {
        Message msg = new MimeMessage(session);
        if (event.from() != null) {
            msg.setFrom(event.from());
        } else {
            msg.setFrom();
        }
        msg.setRecipients(Message.RecipientType.TO,
            event.to().toArray(new Address[0]));
//        msg.setRecipients(Message.RecipientType.CC,
//            event.cc().toArray(new Address[0]));
//        msg.setRecipients(Message.RecipientType.BCC,
//            event.bcc().toArray(new Address[0]));
        msg.setSentDate(new Date());
        for (var header : event.headers().entrySet()) {
            msg.setHeader(header.getKey(), header.getValue());
        }
        msg.setSubject(event.subject());
        msg.setText("Test");
//        msg.setContent(event.body());

        if (!transport.isConnected()) {
            transport.connect();
        }
        Transport.send(msg);
    }

    /**
     * Stop the monitor.
     *
     * @param event the event
     */
    @Handler
    @SuppressWarnings("PMD.GuardLogStatement")
    public void onStop(Stop event) {
    }

}
