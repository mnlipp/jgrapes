package org.jdrupes.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdrupes.Channel;
import org.jdrupes.ClassChannel;
import org.jdrupes.Component;
import org.jdrupes.Manager;
import org.jdrupes.annotation.Handler.NO_CHANNEL;

/**
 * This annotation marks a component's attribute of type 
 * {@link Manager} as a slot for the
 * manager automatically associated with the component 
 * (see {@link Component}).
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
	 * as a {@link org.jdrupes.NamedChannel}'s key (a <code>String</code>).
	 * 
	 * @return the channel
	 */
	String namedChannel() default "";
}