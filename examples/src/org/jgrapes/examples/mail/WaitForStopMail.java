/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.examples.mail;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.mail.events.ReceivedMailMessage;

import jakarta.mail.Flags.Flag;
import jakarta.mail.MessagingException;

/**
 * Wait for mail with subject stop. Delete all other mails.
 */
public class WaitForStopMail extends Component {

    public WaitForStopMail(Channel componentChannel) {
        super(componentChannel);
    }

    @Handler
    public void onMail(ReceivedMailMessage event) throws MessagingException {
        var msg = event.message();
        var subject = msg.getSubject();
        System.out.println("Subject: " + subject);
        if ("Stop".equals(subject)) {
            fire(new Stop());
        }
        msg.setFlag(Flag.DELETED, true);
    }

}
