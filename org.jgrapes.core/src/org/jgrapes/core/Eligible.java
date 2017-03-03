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

/**
 * This interface provides a mechanism for matching an object against a 
 * criterion. The criteria supported depend on the class that implements
 * this interface.
 * 
 * Instances of classes that implement this interface must also provide 
 * a default criterion that can be used to select the instance.
 * This criterion is usually some unique key that identifies the object.
 * Note, however, that there is no requirement for the key to be unique.
 * The only requirement is that `obj.isEligibleFor(obj.getDefaultCrtiterion())`
 * returns `true` for any implentation of `Eligible`.
 * 
 * @author Michael N. Lipp
 */
public interface Eligible {
	
	/**
	 * Returns <code>true</code> if this {@link Eligible}
	 * is met by the provided criterion.
	 * 
	 * @param criterion the criterion
	 * @return {@code true} if this meets the criterion
	 */
	boolean isEligibleFor(Object criterion);

	/**
	 * Returns a sample criterion that this {@link Eligible} meets.
	 * 
	 * @return the criterion
	 */
	Object getDefaultCriterion();
}
