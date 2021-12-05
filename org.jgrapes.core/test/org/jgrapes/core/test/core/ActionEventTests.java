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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import static org.junit.Assert.*;
import org.junit.Test;

public class ActionEventTests {

    public static class App extends Component {
    }

    @Test
    public void testComplete() throws InterruptedException, ExecutionException {
        App app = new App();
        Components.start(app);
        Future<Integer> result = app.activeEventPipeline().submit(
            new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return 42;
                }
            });
        assertEquals(42, result.get().intValue());
        Components.awaitExhaustion();
    }

}
