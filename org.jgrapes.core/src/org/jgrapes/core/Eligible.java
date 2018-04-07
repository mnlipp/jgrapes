/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
 * This interface provides a mechanism for matching objects, using
 * a filter on the object's "kind" as criterion. How the kind is 
 * represented depends completely on the class that implements this 
 * interface.
 * 
 * Every instance of a class that implement this interface must provide 
 * a default criterion (filter) that accepts the instance (though, of course,
 * not *only* this particular instance). Formally: for every instance "`obj`"
 * of `Eligible`, the expression 
 * `obj.isEligibleFor(obj.getDefaultCrtiterion())` must return `true`.
 * 
 * The default criterion can therefore be interpreted as the 
 * representation of the kind of the object.
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
    Object defaultCriterion();
}
