/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core;

import org.jgrapes.core.annotation.HandlerDefinition.Evaluator;

/**
 * This interface allows to verify whether a given
 * event fired on one of the given channels is handled by a handler.
 * Instances of this interface are provided by the 
 * {@link Evaluator}s for handler annotations.
 */
public interface HandlerScope {

    /**
     * Matches the given event and channels against the criteria
     * for events and channels of this handler scope.
     *
     * @param event the event
     * @param channels the channels
     * @return true, if successful
     */
    @SuppressWarnings("PMD.UseVarargs")
    boolean includes(Eligible event, Eligible[] channels);

}
