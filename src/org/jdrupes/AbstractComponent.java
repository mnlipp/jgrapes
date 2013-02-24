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
package org.jdrupes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdrupes.annotation.Handler;
import org.jdrupes.internal.ComponentNode;
import org.jdrupes.internal.MatchKeyProvider;

/**
 * This is the base class for a new component. Components can be
 * created by deriving from this class or by implementing 
 * the interface {@link Component}.
 */
public class AbstractComponent extends ComponentNode 
	implements Component, MatchKeyProvider {

	/**
	 * 
	 */
	public AbstractComponent() {
		super();
		for (Method m: getClass().getMethods()) {
			Handler handlerAnnotation = m.getAnnotation(Handler.class);
			if (handlerAnnotation == null) {
				continue;
			}
			List<Object> events = new ArrayList<Object>();
			if (handlerAnnotation.events()[0] != Handler.NO_EVENT.class) {
				events.addAll(Arrays.asList(handlerAnnotation.events()));
			}
			if (handlerAnnotation.namedEvents()[0] != "") {
				events.addAll(Arrays.asList(handlerAnnotation.namedEvents()));
			}
			List <Object> channels = new ArrayList<Object>();
			if (handlerAnnotation.channels()[0] != Handler.NO_CHANNEL.class) {
				channels.addAll(Arrays.asList(handlerAnnotation.channels()));
			}
			if (handlerAnnotation.namedChannels()[0] != "") {
				channels.addAll
					(Arrays.asList(handlerAnnotation.namedChannels()));
			}
			for (Object eventKey: events) {
				for (Object channelKey: channels) {
					addHandler(eventKey, channelKey, m);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.ComponentBase#getComponent()
	 */
	@Override
	public Component getComponent() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.MatchKeyProvider#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return this;
	}

}
