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
package org.jgrapes.core.test.basic;

import org.jgrapes.core.Component;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.ComponentManager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

/**
 * @author mnl
 *
 */
public class TestComponent1 implements Component {

	private String name = "Unknown";
	@ComponentManager(namedChannel="Test")
	private Manager manager = null;
	
	public TestComponent1() {
	}
	
	public TestComponent1(String name) {
		this.name = name;
	}
	
	public Manager getManager() {
		return manager;
	}
	
	@Handler(events=Start.class)
	public void handler1() {
		return;
	}

	@Handler(events=Start.class, namedChannels="test")
	public void handler2() {
		return;
	}
	
	@Handler(events=Start.class, namedChannels={"test", "other"})
	public void handler3() {
		return;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
