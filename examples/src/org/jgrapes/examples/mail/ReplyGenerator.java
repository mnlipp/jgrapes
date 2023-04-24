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

import jakarta.mail.Flags.Flag;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Subchannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.mail.events.FoldersUpdated;
import org.jgrapes.mail.events.SendMessage;

/**
 * Wait for mail with subject stop. Delete all other mails.
 */
public class ReplyGenerator extends Component {

    public ReplyGenerator(Channel componentChannel) {
        super(componentChannel);
    }

    @Handler
    public void onMail(FoldersUpdated event, Subchannel channel)
            throws MessagingException {
        if (event.newMessages().isEmpty()) {
            return;
        }
        var msg = event.newMessages().get(0);
        msg.setFlag(Flag.DELETED, true);
        var response = msg.reply(false);
        var bp1 = new MimeBodyPart();
        bp1.setText("Your message has been received.", "utf-8");
        fire(new SendMessage()
            .setFrom(new InternetAddress("Auto-Reply@jgrapes.org"))
            .setTo(response.getRecipients(RecipientType.TO))
            .setSubject(response.getSubject()).addContent(bp1));
        var subject = msg.getSubject();
        if ("Stop".equals(subject)) {
            fire(new Stop());
        }
    }

}
