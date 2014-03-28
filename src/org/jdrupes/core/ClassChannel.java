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
package org.jdrupes.core;

/**
 * The root base class for channels that use their class (type) as key
 * for matching channels with handlers.
 * 
 * @author mnl
 */
public class ClassChannel implements Channel {

	/**
	 * Return the class of this channel as its key.
	 * 
	 * @return the class of this channel
	 * 
	 * @see org.jdrupes.core.internal.Matchable#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return getClass();
	}

	/**
	 * Returns <code>true</code> if the <code>handlerKey</code>
	 * is the same class or a base class of this channel's class.
	 * 
	 * @see org.jdrupes.core.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return Class.class.isInstance(handlerKey) && ((Class<?>)handlerKey)
				.isAssignableFrom((Class<?>)getMatchKey());
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
		ClassChannel other = (ClassChannel) obj;
		if (getMatchKey() == null) {
			if (other.getMatchKey() != null)
				return false;
		} else if (!getMatchKey().equals(other.getMatchKey()))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() 
				+ " [matchKey=" + getMatchKey() + "]";
	}
}
