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

import org.jdrupes.internal.EventBase;
import org.jdrupes.internal.Matchable;

/**
 * The base class for all events. Events form a hierarchy and by default
 * their class is used for matching, i.e. a handler is invoked if
 * its event class is equal to or a base class of the class of the
 * event to be handled. 
 * 
 * This default behavior may be changed by overriding the methods
 * from {@link Matchable}. See {@link NamedEvent} as an example.
 * 
 * @author mnl
 */
public class Event extends EventBase implements Matchable {

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.MatchKeyProvider#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return getClass();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return Class.class.isInstance(handlerKey)
				&& ((Class<?>)handlerKey).isAssignableFrom(getClass());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getMatchKey().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		if (getMatchKey() == null) {
			if (other.getMatchKey() != null)
				return false;
		} else if (!getMatchKey().equals(other.getMatchKey()))
			return false;
		return true;
	}
}
