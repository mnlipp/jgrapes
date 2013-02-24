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

import org.jdrupes.Utils;
import org.jdrupes.events.Started;
import org.jdrupes.test.core.components.StartReporter;
import org.junit.Test;

/**
 * @author mnl
 *
 */
public class StartTest {

	@Test
	public void test() {
		StartReporter app = new StartReporter();
		Utils.manager(app).fire(new Started());
		assertTrue(app.started);
	}

}
