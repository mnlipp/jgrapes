/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes.test.core;

import static org.junit.Assert.*;

import org.jdrupes.NamedChannel;
import org.jdrupes.NamedEvent;
import org.jdrupes.Utils;
import org.jdrupes.events.Started;
import org.jdrupes.test.core.components.EventCounter;
import org.junit.Test;

/**
 * @author mnl
 *
 */
public class MatchTest {

	@Test
	public void test() {
		EventCounter app = new EventCounter();
		Utils.manager(app).fire(new Started());
		assertEquals(1, app.startedGlobal);
		assertEquals(0, app.startedTest1);
		assertEquals(0, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(1, app.all);
		Utils.manager(app).fire(new Started(), new NamedChannel("test1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(1, app.startedTest1);
		assertEquals(0, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(2, app.all);
		Utils.manager(app).fire(new NamedEvent("named1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(1, app.startedTest1);
		assertEquals(1, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(3, app.all);
		Utils.manager(app).fire(new NamedEvent("named1"), 
				new NamedChannel("test1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(1, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(1, app.named1Test1);
		assertEquals(4, app.all);
	}

}
