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
package org.jgrapes.core.annotation;

import org.jgrapes.core.Criterion;
import org.jgrapes.core.annotation.HandlerDefinition.Evaluator;

/**
 * This interface allows to verify whether a given
 * event fired on given channels is handled by a handler.
 * {@link Evaluator}s use this interface to provide the information
 * about the events being handled.
 * 
 * @author Michael N. Lipp
 */
public interface HandlerScope {

	boolean includes (Criterion event, Criterion[] channels);
	
}
