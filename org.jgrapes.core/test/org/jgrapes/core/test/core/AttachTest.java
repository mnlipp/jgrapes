/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
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

package org.jgrapes.core.test.core;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Attached;
import org.jgrapes.core.events.Detached;
import static org.junit.Assert.*;
import org.junit.Test;

public class AttachTest {

    public static class Trigger extends Event<Void> {
    }

    public static class AttachCatcher extends Component {

        public AttachCatcher() {
            super(Channel.BROADCAST);
            Handler.Evaluator.add(this, "onTrigger", this);
        }

        public ComponentType attachRoot = null;
        public ComponentType attachParent = null;
        public ComponentType attachChild = null;
        public ComponentType detachParent = null;
        public ComponentType detachChild = null;

        @Handler
        public void onAttached(Attached evt) {
            if (evt.parent() == null) {
                attachRoot = evt.node();
            }
            attachParent = evt.parent();
            attachChild = evt.node();
        }

        @Handler
        public void onDetached(Detached evt) {
            detachParent = evt.parent();
            detachChild = evt.node();
        }

        @Handler(dynamic = true)
        public void onTrigger(Trigger event) {
            Components.manager(children().get(0)).detach();
        }
    }

    @Test
    public void testDetachExternal()
            throws InterruptedException {
        AttachCatcher c1 = new AttachCatcher();
        AttachCatcher c2 = new AttachCatcher();
        c1.attach(c2);
        Components.start(c1);
        assertEquals(c1, c1.attachRoot);
        assertEquals(c1, c2.attachRoot);
        assertEquals(c1, c1.attachParent);
        assertEquals(c2, c1.attachChild);
        assertEquals(c1, c2.attachParent);
        assertEquals(c2, c2.attachChild);
        c2.detach();
        Components.awaitExhaustion();
        assertEquals(c1, c1.detachParent);
        assertEquals(c2, c1.detachChild);
        assertEquals(c1, c2.detachParent);
        assertEquals(c2, c2.detachChild);
    }

    @Test
    public void testDetachInternal()
            throws InterruptedException {
        AttachCatcher c1 = new AttachCatcher();
        AttachCatcher c2 = new AttachCatcher();
        c1.attach(c2);
        Components.start(c1);
        assertEquals(c1, c1.attachRoot);
        assertEquals(c1, c2.attachRoot);
        assertEquals(c1, c1.attachParent);
        assertEquals(c2, c1.attachChild);
        assertEquals(c1, c2.attachParent);
        assertEquals(c2, c2.attachChild);
        c1.fire(new Trigger(), c1);
        Components.awaitExhaustion();
        assertEquals(c1, c1.detachParent);
        assertEquals(c2, c1.detachChild);
        assertEquals(c1, c2.detachParent);
        assertEquals(c2, c2.detachChild);
    }

}
