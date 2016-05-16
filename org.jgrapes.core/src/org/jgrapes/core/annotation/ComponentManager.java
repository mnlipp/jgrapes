package org.jgrapes.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler.NO_CHANNEL;

/**
 * This annotation marks a component's attribute of type 
 * {@link Manager} as a slot for its manager. A value is automatically 
 * assigned to such an attribute when a component is attached to the 
 * component tree or by {@link org.jgrapes.core.Utils#manager(Component)}.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD)
public @interface ComponentManager {
	
	/**
	 * Specifies the channel to be associated with the component
	 * as a {@link ClassChannel}'s key.
	 * 
	 * @return the channel
	 */
	Class<? extends Channel> channel() default NO_CHANNEL.class;

	/**
	 * Specifies the channel to be associated with the component
	 * as a {@link org.jgrapes.core.NamedChannel}'s key (a <code>String</code>).
	 * 
	 * @return the channel
	 */
	String namedChannel() default "";
}