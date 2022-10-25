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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.Opened;
import org.jgrapes.mail.MailChannel;
import org.jgrapes.mail.MailStoreMonitor;
import org.jgrapes.mail.events.MessagesRetrieved;
import org.jgrapes.mail.events.OpenMailMonitor;
import org.jgrapes.util.ConfigurationStore;
import org.jgrapes.util.Password;
import org.jgrapes.util.TomlConfigurationStore;

import jakarta.mail.MessagingException;
import jakarta.mail.Flags.Flag;

/**
 * An application that deletes all received mails.
 */
public class CleanUserMailsUntilStop extends Component {

    private static CountDownLatch waitForOpen = new CountDownLatch(1);

    /**
     * Wait for mail with subject stop. Delete all other mails.
     */
    public static class WaitForStopMail extends Component {

        public WaitForStopMail(Channel componentChannel) {
            super(componentChannel);
        }

        @Handler
        public void onOpened(Opened<?> event) {
            waitForOpen.countDown();
        }

        @Handler
        public void onMail(MessagesRetrieved event, MailChannel channel)
                throws MessagingException {
            for (var msg : event.newMessages()) {
                var subject = msg.getSubject();
                System.out.println("Subject: " + subject);
                if ("Stop".equals(subject)) {
                    fire(new Stop());
                }
                msg.setFlag(Flag.DELETED, true);
            }
        }

    }

    /**
     * @param args
     * @throws IOException 
     * @throws InterruptedException 
     */
    public static void main(String[] args)
            throws IOException, InterruptedException {
        var app = new CleanUserMailsUntilStop();
        ConfigurationStore config = new TomlConfigurationStore(app,
            new File("user-wftest-config.toml"));
        app.attach(config);
        app.attach(new MailStoreMonitor(app));
        app.attach(new WaitForStopMail(app));
        Components.start(app);
        Map<String, String> user1 = config.values("/user1").get();
        app.fire(new OpenMailMonitor()
            .setMailProperty("mail.user", user1.get("user"))
            .setPassword(new Password(user1.get("password").toCharArray())));
        waitForOpen.await();
        Components.awaitExhaustion();
    }

}
