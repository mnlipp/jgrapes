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

package org.jgrapes.core.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.jgrapes.core.ComponentType;
import org.jgrapes.core.annotation.HandlerDefinition;
import org.jgrapes.core.annotation.HandlerDefinition.Evaluator;

/**
 * Common utility methods.
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
public final class Common {

    @SuppressWarnings("PMD.VariableNamingConventions")
    public static final Logger classNames
        = Logger.getLogger(ComponentType.class.getPackage().getName()
            + ".classNames");

    @SuppressWarnings("PMD.VariableNamingConventions")
    /* default */ static final Logger fireRestrictionLogger
        = Logger.getLogger(Common.class.getPackage().getName()
            + ".fireRestriction");

    /** Handler factory cache. */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<Class<? extends HandlerDefinition.Evaluator>,
            HandlerDefinition.Evaluator> definitionEvaluators
                = Collections.synchronizedMap(new HashMap<>());
    private static AtomicReference<AssertionError> assertionError
        = new AtomicReference<>();

    private Common() {
    }

    /**
     * Create a new definition evaluator.
     *
     * @param hda the handler definition annotation
     * @return the evaluator
     */
    public static Evaluator definitionEvaluator(
            HandlerDefinition hda) {
        return definitionEvaluators.computeIfAbsent(hda.evaluator(), key -> {
            try {
                return hda.evaluator().getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /* default */ static void setAssertionError(AssertionError error) {
        assertionError.compareAndSet(null, error);
    }

    /**
     * Check if an assertion has been set and if, throw it.
     */
    public static void checkAssertions() {
        AssertionError error = assertionError.getAndSet(null);
        if (error != null) {
            throw error;
        }
    }

}
