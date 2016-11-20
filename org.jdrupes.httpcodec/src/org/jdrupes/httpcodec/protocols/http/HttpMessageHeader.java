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
package org.jdrupes.httpcodec.protocols.http;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.fields.HttpField;

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
	 */
	public void setField(HttpField<?> value) {
		headers.put(value.getName(), value);
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
	 * Returns the header field with the given type and name or {@code null}
	 * if no such header is set.
	 * 
	 * @param <T> the header field class
	 * @param type the header field type
	 * @param name the field name
	 * @return the header field or {@code null}
	 */
	public <T extends HttpField<?>> T getField(Class<T> type, String name) {
		return type.cast(headers.get(name));
	}
	
	/**
	 * Set the flag that indicates whether this header is followed by a body.
	 * 
	 * @param messageHasBody new value
	 */
	public void setMessageHasBody(boolean messageHasBody) {
		this.messageHasBody = messageHasBody;
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
