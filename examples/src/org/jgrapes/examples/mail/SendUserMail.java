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

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.mail.MailSender;
import org.jgrapes.mail.events.SendMessage;
import org.jgrapes.util.TomlConfigurationStore;

/**
 * An application that deletes all received mails.
 */
public class SendUserMail extends Component {

    /**
     * @param args
     * @throws IOException 
     * @throws InterruptedException 
     * @throws MessagingException 
     */
    public static void main(String[] args)
            throws IOException, InterruptedException, MessagingException {
        var app = new SendUserMail();
        app.attach(new TomlConfigurationStore(app,
            new File("mail-examples-config.toml")));
        app.attach(new MailSender(app));
        Components.start(app);
        var bp1 = new MimeBodyPart();
        bp1.setText("Test mail.");
        app.fire(new SendMessage()
            .setFrom(new InternetAddress("test@jgrapes.org"))
            .setTo(List.of(new InternetAddress("mnl@mnl.de")))
            .setSubject("Mail Test")
            .addContent(bp1));
        Components.awaitExhaustion();
    }

}
