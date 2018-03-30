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

package org.jgrapes.core.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.jgrapes.core.ComponentType;
import org.jgrapes.core.annotation.HandlerDefinition;
import org.jgrapes.core.annotation.HandlerDefinition.Evaluator;

/**
 * Common utility methods.
 */
public class Common {

	private Common() {
	}

	static final Logger coreLogger 
		= Logger.getLogger(Common.class.getPackage().getName());	
	
	/** Handler factory cache. */
	private static Map<Class<? extends HandlerDefinition.Evaluator>,
			HandlerDefinition.Evaluator> definitionEvaluators 
			= Collections.synchronizedMap(new HashMap<>());
	private static AssertionError assertionError = null;
	
	public static Evaluator definitionEvaluator(
	        HandlerDefinition hda) {
		return definitionEvaluators.computeIfAbsent(hda.evaluator(), key -> {
			try {
				return hda.evaluator().newInstance();
			} catch (InstantiationException
			        | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	static void setAssertionError(AssertionError error) {
		if (assertionError == null) {
			assertionError = error;
		}
	}

	public static void checkAssertions() {
		if (assertionError != null) {
			AssertionError error = assertionError;
			assertionError = null;
			throw error;
		}
	}
	
	public static final Logger classNames 
		= Logger.getLogger(ComponentType.class.getPackage().getName() 
			+ ".classNames");
	
}
