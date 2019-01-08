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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.HandlerScope;

/**
 * A reference to a method that handles an event.
 */
class HandlerReference implements Comparable<HandlerReference> {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final Logger handlerTracking
        = Logger.getLogger(ComponentType.class.getPackage().getName()
            + ".handlerTracking");

    private final HandlerScope filter;
    protected MethodHandle method;
    private final int priority;

    /**
     * Create a new handler reference to a component's method that 
     * handles events matching the filter.
     * 
     * @param component the component
     * @param method the method to be invoked
     * @param priority the handler's priority
     * @param filter the filter
     */
    protected HandlerReference(ComponentType component, Method method,
            int priority, HandlerScope filter) {
        super();
        this.filter = filter;
        this.priority = priority;
        try {
            this.method = MethodHandles.lookup().unreflect(method);
            this.method = this.method.bindTo(component);
        } catch (IllegalAccessException e) {
            throw (RuntimeException) (new IllegalArgumentException("Method "
                + component.getClass().getName()
                + "." + method.getName()
                + " annotated as handler has wrong signature"
                + " or class is not accessible"))
                    .initCause(e);
        }
    }

    @Override
    public int compareTo(HandlerReference other) {
        if (getPriority() < other.getPriority()) {
            return 1;
        }
        if (getPriority() > other.getPriority()) {
            return -1;
        }
        return 0;
    }

    /**
     * Returns {@code true} if this handler handles the given event
     * fired on the given channels. 
     * 
     * @param event the event
     * @param channels the channels
     * @return the result
     */
    @SuppressWarnings("PMD.UseVarargs")
    public boolean handles(Eligible event, Eligible[] channels) {
        return filter.includes(event, channels);
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Invoke the handler with the given event as parameter. 
     * 
     * @param event the event
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void invoke(EventBase<?> event) throws Throwable {
        // ADAPT VERBOSEHANDLERREFERENCE TO ANY CHANGES MADE HERE
        switch (method.type().parameterCount()) {
        case 0:
            // No parameters
            method.invoke();
            break;

        case 1:
            // Event parameter
            method.invoke(event);
            break;

        case 2:
            // Event and channel
            Class<?> channelParam = method.type().parameterType(1);
            for (Channel channel : event.channels()) {
                if (channelParam.isAssignableFrom(channel.getClass())) {
                    method.invoke(event, channel);
                }
            }
            break;

        default:
            throw new IllegalStateException("Handle not usable");
        }
    }

    /**
     * Returns a string representation of the method.
     *
     * @return the string
     */
    protected String methodToString() {
        return method.toString();
    }

    /**
     * A factory for creating {@link HandlerReference} objects.
     */
    /* default */ abstract static class HandlerRefFactory {
        /* default */ abstract HandlerReference createHandlerRef(
                Object eventKey, Object channelKey,
                ComponentType component, Method method, boolean eventParam,
                int priority);
    }

    /**
     * Create a new {@link HandlerReference} from the given values.
     *
     * @param component the component
     * @param method the method
     * @param priority the priority
     * @param filter the filter
     * @return the handler reference
     */
    public static HandlerReference newRef(
            ComponentType component, Method method,
            int priority, HandlerScope filter) {
        if (handlerTracking.isLoggable(Level.FINE)) {
            return new VerboseHandlerReference(
                component, method, priority, filter);
        } else {
            return new HandlerReference(component, method, priority, filter);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public int hashCode() {
        @SuppressWarnings("PMD.AvoidFinalLocalVariable")
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + priority;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HandlerReference other = (HandlerReference) obj;
        if (filter == null) {
            if (other.filter != null) {
                return false;
            }
        } else if (!filter.equals(other.filter)) {
            return false;
        }
        if (method == null) {
            if (other.method != null) {
                return false;
            }
        } else if (!method.equals(other.method)) {
            return false;
        }
        if (priority != other.priority) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append("Handler [");
        if (method != null) {
            builder.append("method=");
            builder.append(methodToString());
            builder.append(", ");
        }
        if (filter != null) {
            builder.append("filter=");
            builder.append(filter);
            builder.append(", ");
        }
        builder.append("priority=")
            .append(priority)
            .append(']');
        return builder.toString();
    }

}
