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
package org.jgrapes.core.test.core.components;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

/**
 * @author mnl
 *
 */
public class EventCounter extends AbstractComponent {

	public int all = 0;
	public int startedGlobal = 0;
	public int startedTest1 = 0;
	public int named1Global = 0;
	public int named1Test1 = 0;
	public int startedComponent = 0;

	/**
	 * 
	 */
	public EventCounter() {
		super(Channel.BROADCAST);
		addHandler(Start.class, this, "onStartedComponent");
	}

	public void onStartedComponent(Start event) {
		startedComponent += 1;
	}
	
	@Handler(events=Event.class, channels=Channel.class)
	public void onAll(Event<?> event) {
		all += 1;
	}

	@Handler(events=Start.class)
	public void onStart(Start event) {
		startedGlobal += 1;
	}

	@Handler(events=Start.class, namedChannels="test1")
	public void onStartTest1(Start event) {
		startedTest1 += 1;
	}

	@Handler(namedEvents="named1")
	public void onNamed1(Event<?> event) {
		named1Global += 1;
	}
	
	@Handler(namedEvents="named1", namedChannels="test1")
	public void onNamed1Test1(Event<?> event) {
		named1Test1 += 1;
	}
}
