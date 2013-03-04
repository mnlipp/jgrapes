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
package org.jdrupes.test.core.components;

import org.jdrupes.ClassChannel;
import org.jdrupes.Component;
import org.jdrupes.Event;
import org.jdrupes.Manager;
import org.jdrupes.annotation.ComponentManager;
import org.jdrupes.annotation.Handler;
import org.jdrupes.events.Start;

/**
 * @author mnl
 *
 */
public class ComponentWithClassChannel implements Component {

	public static class MyChannel extends ClassChannel {
	}
	
	@ComponentManager(channel=MyChannel.class)
	private Manager manager;
	
	public int count = 0;
	
	@Handler(events=Start.class)
	public void onStarted(Event event) {
		count += 1;
	}
}
