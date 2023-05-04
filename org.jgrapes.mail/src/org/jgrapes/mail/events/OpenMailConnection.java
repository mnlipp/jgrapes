/*
 * JGrapes Event driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jgrapes.core.Event;
import org.jgrapes.mail.MailConnectionManager;
import org.jgrapes.util.Password;

/**
 * Common base class for events that open a mail connection for
 * sending or receiving mail. Note that all configuration information
 * (such as mail server, protocol, user name) is provided as mail 
 * properties (see {@link #setMailProperties(Map)).
 * 
 * Also note that a component receiving the event may have default
 * or system wide values configured for the properties.
 */
public abstract class OpenMailConnection extends Event<Void> {

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, String> mailProps = new HashMap<>();
    private Password password;

    /**
     * Specifies mail properties that override defaults set for
     * the handling {@link MailConnectionManager}. Merges the given
     * properties with properties already set for the event.
     *
     * @param props the props
     * @return the event
     */
    public OpenMailConnection setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
        return this;
    }

    /**
     * Sets a single mail property, see {@link #setMailProperties(Map)}.
     *
     * @param name the name
     * @param value the value
     * @return the open mail connection
     */
    public OpenMailConnection setMailProperty(String name, String value) {
        mailProps.put(name, value);
        return this;
    }

    /**
     * Returns the mail properties.
     *
     * @return the map
     */
    public Map<String, String> mailProperties() {
        return Collections.unmodifiableMap(mailProps);
    }

    /**
     * Sets the password used for opening the connection.
     *
     * @param password the password
     * @return the open mail connection
     */
    public OpenMailConnection setPassword(Password password) {
        this.password = password;
        return this;
    }

    /**
     * Returns the password.
     *
     * @return the password
     */
    public Optional<Password> password() {
        return Optional.ofNullable(password);
    }
}
