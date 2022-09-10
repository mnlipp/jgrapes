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
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.mail.SimpleMailMonitor;
import org.jgrapes.mail.SimpleMailSender;
import org.jgrapes.util.JsonConfigurationStore;

/**
 * An application that replies to all received mails.
 */
public class AutoResponder extends Component {

    /**
     * @param args
     * @throws IOException 
     * @throws InterruptedException 
     */
    public static void main(String[] args)
            throws IOException, InterruptedException {
        var app = new AutoResponder();
        app.attach(new JsonConfigurationStore(app,
            new File("mail-examples-config.json")));
        app.attach(new SimpleMailMonitor(app));
        app.attach(new SimpleMailSender(app));
        app.attach(new ReplyGenerator(app));
        Components.start(app);
        Components.awaitExhaustion();
    }

}
