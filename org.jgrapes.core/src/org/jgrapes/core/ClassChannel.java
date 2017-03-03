/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrapes.core;

import org.jgrapes.core.internal.Common;

/**
 * This class is the root base class for channels that use their class (type)
 * as value for matching (see {@link Eligible}).
 * 
 * @author Michael N. Lipp
 */
public class ClassChannel implements Channel {

	/**
	 * Returns the class of this channel as value.
	 * 
	 * @return the class of this channel
	 * 
	 * @see org.jgrapes.core.Eligible#getDefaultCriterion()
	 */
	@Override
	public Object getDefaultCriterion() {
		return getClass();
	}

	/**
	 * Returns <code>true</code> if the <code>value</code>
	 * is the same class or a base class of this channel's class.
	 * 
	 * @see org.jgrapes.core.Eligible#isEligibleFor(java.lang.Object)
	 */
	@Override
	public boolean isEligibleFor(Object value) {
		return Class.class.isInstance(value) 
				&& ((Class<?>)value)
					.isAssignableFrom((Class<?>)getDefaultCriterion());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getDefaultCriterion().hashCode();
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
		if (getDefaultCriterion() == null) {
			if (other.getDefaultCriterion() != null)
				return false;
		} else if (!getDefaultCriterion().equals(other.getDefaultCriterion()))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Common.classToString(getClass())
				+ " [criterion=" 
				+ ((getDefaultCriterion() instanceof Class)
					?  Common.classToString((Class<?>)getDefaultCriterion())
					: getDefaultCriterion())
				+ "]";
	}
}
