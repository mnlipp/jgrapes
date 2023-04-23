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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.util.Password;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * A base class for mail handling components.
 */
public abstract class MailComponent extends Component {

    protected final Properties mailProps = new Properties();
    private Password password;

    /**
     * Creates a new component with its channel set to itself.
     */
    public MailComponent() {
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
    public MailComponent(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Creates a new component base like {@link #MailComponent(Channel)}
     * but with channel mappings for {@link Handler} annotations.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param channelReplacements the channel replacements to apply
     * to the `channels` elements of the {@link Handler} annotations
     */
    public MailComponent(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
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
    public MailComponent setMailProperties(Map<String, String> props) {
        mailProps.putAll(props);
        return this;
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public MailComponent setPassword(Password password) {
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Components.objectName(this);
    }
}
