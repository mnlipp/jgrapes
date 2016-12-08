/*******************************************************************************
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.jdrupes.httpcodec.protocols.http;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;

/**
 * Represents an HTTP message header (either request or response).
 * 
 * @author Michael N. Lipp
 */
public abstract class HttpMessageHeader 
	implements MessageHeader, HttpConstants {

	private HttpProtocol httpProtocol;
	private Map<String,HttpField<?>> headers 
		= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private boolean messageHasBody;

	/**
	 * Creates a new message header.
	 * 
	 * @param httpProtocol the HTTP protocol
	 * @param messageHasBody indicates that a body is expected after the header
	 */
	public HttpMessageHeader(HttpProtocol httpProtocol, boolean messageHasBody) {
		this.httpProtocol = httpProtocol;
		this.messageHasBody = messageHasBody;
	}

	/**
	 * Return the protocol.
	 * 
	 * @return the HTTP protocol
	 */
	public HttpProtocol getProtocol() {
		return httpProtocol;
	}

	/**
	 * Returns all header fields as unmodifiable map.
	 * 
	 * @return the headers
	 */
	public Map<String, HttpField<?>> fields() {
		return Collections.unmodifiableMap(headers);
	}
	
	/**
	 * Set a header field for the message.
	 * 
	 * @param value the header field's value
	 * @return the message header for easy chaining
	 */
	public HttpMessageHeader setField(HttpField<?> value) {
		headers.put(value.getName(), value);
		// Check some consistency rules
		if (value.getName().equalsIgnoreCase(HttpField.UPGRADE)) {
			computeIfAbsent(HttpStringListField.class, HttpField.CONNECTION,
					n -> new HttpStringListField(n))
				.appendIfNotContained(HttpField.UPGRADE);
		}
		return this;
	}

	/**
	 * Clear all headers.
	 * 
	 * @return the message header for easy chaining
	 */
	public HttpMessageHeader clearHeaders() {
		headers.clear();
		return this;
	}
	
	/**
	 * Removes a header field from the message.
	 * 
	 * @param name the header field's name
	 */
	public void removeField(String name) {
		headers.remove(name);
	}

	/**
	 * Returns the header field with the given type if it exists.
	 * The well known header fields are always parsed with their
	 * proper type. Unknown header fields will be parsed as string fields.
	 * <P>
	 * String fields will be automatically converted to more specific types
	 * if they are requested as integer fields or string list fields.
	 * 
	 * @param <T> the header field class
	 * @param type the header field type
	 * @param name the field name
	 * @return the header field if it exists
	 */
	public <T extends HttpField<?>> Optional<T> 
		getField(Class<T> type, String name) {
		HttpField<?> field = headers.get(name);
		if (field == null || type.isAssignableFrom(field.getClass()) ) {
			return Optional.ofNullable(type.cast(field));
		}
		if (!(field instanceof HttpStringField)) {
			return Optional.empty();
		}
		if (HttpIntField.class.isAssignableFrom(type)) {
			long value = Long.parseLong(((HttpStringField) field).getValue());
			try {
				T result = type.getConstructor(String.class, long.class)
				        .newInstance(name, value);
				removeField(name);
				setField(result);
				return Optional.of(type.cast(result));
			} catch (InstantiationException | IllegalAccessException
			        | IllegalArgumentException | InvocationTargetException
			        | NoSuchMethodException | SecurityException e) {
			}
		}
		if (HttpStringListField.class.isAssignableFrom(type)) {
			try {
				T result = type.getConstructor
						(String.class, String.class, boolean.class)
						.newInstance(name, ((HttpStringField) field).getValue(),
								true);
				removeField(name);
				setField(result);
				return Optional.of(type.cast(result));
			} catch (InstantiationException | IllegalAccessException
			        | IllegalArgumentException | InvocationTargetException
			        | NoSuchMethodException | SecurityException e) {
			}
		}
		if (HttpIntListField.class.isAssignableFrom(type)) {
			try {
				T result = type.getConstructor(String.class, String.class)
					.newInstance(name, ((HttpStringField) field).getValue());
				removeField(name);
				setField(result);
				return Optional.of(type.cast(result));
			} catch (InstantiationException | IllegalAccessException
			        | IllegalArgumentException | InvocationTargetException
			        | NoSuchMethodException | SecurityException e) {
			}
		}
		return Optional.empty();
	}

	/**
	 * Convenience method for getting a field with a string value.
	 * 
	 * @param name the field name
	 * @return the header field if it exists
	 * @see #getField(Class, String)
	 */
	public Optional<HttpStringField> getStringField(String name) {
		return getField(HttpStringField.class, name);
	}
	
	/**
	 * Returns the header field with the given type, computing 
	 * and adding it if it doesn't exist.
	 * 
	 * @param <T> the header field class
	 * @param type the header field type
	 * @param name the field name
	 * @param computeFunction the function that computes the filed if
	 * it doesn't exist
	 * @return the header field if it exists
	 */
	public <T extends HttpField<?>> T computeIfAbsent
			(Class<T> type, String name, Function<String, T> computeFunction) {
		Optional<T> result = getField(type, name);
		if (result.isPresent()) {
			return result.get();
		}
		T value = computeFunction.apply(name);
		setField(value);
		return value;
	}
	
	/**
	 * Set the flag that indicates whether this header is followed by a body.
	 * 
	 * @param messageHasBody new value
	 * @return the message for easy chaining
	 */
	public MessageHeader setMessageHasBody(boolean messageHasBody) {
		this.messageHasBody = messageHasBody;
		return this;
	}
	
	/**
	 * Returns {@code true} if the header is followed by a body.
	 * 
	 * @return {@code true} if body data follows
	 */
	public boolean messageHasBody() {
		return messageHasBody;
	}

}
