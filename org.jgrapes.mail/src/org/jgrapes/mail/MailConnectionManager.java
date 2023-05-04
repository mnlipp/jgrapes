/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022, 2023 Michael N. Lipp
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.util.ConnectionManager;
import org.jgrapes.util.Password;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * Provides a base class for mail components using connections.
 *
 * @param <O> the type of the open event
 * @param <C> the type of the channel
 */
public abstract class MailConnectionManager<
        C extends MailConnectionManager<C, O>.AbstractMailChannel,
        O extends Event<?>> extends ConnectionManager<C> {

    protected final Properties mailProps = new Properties();
    private Password password;

    /**
     * Creates a new server using the given channel.
     * 
     * @param componentChannel the component's channel
     */
    public MailConnectionManager(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets the mail properties. See 
     * [the Jakarta Mail](https://jakarta.ee/specifications/mail/2.0/apidocs/jakarta.mail/jakarta/mail/package-summary.html)
     * documentation for available settings. The given properties are
     * merged with the already existing properties.
     *
     * @param props the props
     * @return the mail monitor
     */
    public MailConnectionManager<C, O>
            setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
        return this;
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public MailConnectionManager<C, O> setPassword(Password password) {
        this.password = password;
        return this;
    }

    /**
     * Return the password.
     *
     * @return the optional password
     */
    protected Optional<Password> password() {
        return Optional.ofNullable(password);
    }

    /**
     * Configure the component. Attempts to access all paths specified
     * in the package description in sequence as described in 
     * {@link org.jgrapes.mail}. For each path, merges the
     * `mail` properties and invokes {@link #configureComponent}
     * with the available key/value pairs.  
     *  
     * @param event the event
     */
    @Handler
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onConfigUpdate(ConfigurationUpdate event) {
        for (var path : List.of("/org.jgrapes.mail/Component",
            "/org.jgrapes.mail/" + getClass().getSimpleName(),
            "/org_jgrapes_mail/Component",
            "/org_jgrapes_mail/" + getClass().getSimpleName(),
            componentPath())) {
            event.values(path + "/mail").ifPresent(c -> {
                for (var e : c.entrySet()) {
                    mailProps.put("mail." + e.getKey(), e.getValue());
                }
            });
            event.values(path).ifPresent(v -> {
                Optional.ofNullable(v.get("password"))
                    .ifPresent(p -> setPassword(new Password(p.toCharArray())));
                configureComponent(v);
            });
        }
    }

    /**
     * Configure the component specific values.
     *
     * @param values the values
     */
    protected abstract void configureComponent(Map<String, String> values);

    /**
     * A sub-channel for mail connections.
     */
    protected abstract class AbstractMailChannel
            extends ConnectionManager<C>.Connection implements MailChannel {

        private final O openEvent;

        /**
         * Instantiates a new mail channel instance.
         *
         * @param event the main channel
         */
        public AbstractMailChannel(O event, Channel channel) {
            super(channel);
            openEvent = event;
        }

        /**
         * Returns the event that caused this connection to be opened.
         * 
         * @return the event
         */
        public O openEvent() {
            return openEvent;
        }

    }
}
