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
package org.jdrupes.httpcodec.internal;

/**
 * The result of invoking the request decoder. Used to inform the
 * invoker about what to do next.
 * 
 * @author Michael N. Lipp
 */
public abstract class DecoderResult<T extends Message> {

	private T message;
	private boolean payloadBytes;
	private boolean payloadChars;
	private boolean closeConnection;

	/**
	 * Creates a new result.
	 * 
	 * @param message the decoded message
	 * @param response a response to send because an error occurred
	 * that must be signaled back to the client
	 * @param payloadBytes {@code true} if the request has a body with octets
	 * @param payloadChars {@code true} if the request has a body with text
	 */
	protected DecoderResult(T message, boolean payloadBytes, 
			boolean payloadChars, boolean closeConnection) {
		super();
		this.message = message;
		this.payloadBytes = payloadBytes;
		this.payloadChars = payloadChars;
		this.closeConnection = closeConnection;
	}

	/**
	 * Returns {@code true} if the result includes a request. 
	 * 
	 * @return the result
	 */
	public boolean hasMessage() {
		return message != null;
	}
	
	/**
	 * @return the message
	 */
	protected T getMessage() {
		return message;
	}

	/**
	 * @return the payloadBytes
	 */
	public boolean hasPayloadBytes() {
		return payloadBytes;
	}

	/**
	 * @return the payloadChars
	 */
	public boolean hasPayloadChars() {
		return payloadChars;
	}
	
	/**
	 * Returns {@code true} if the connection should be closed. If the
	 * result has a response, that response must be sent before
	 * closing the connection.
	 * 
	 * @return the result
	 */
	public boolean getCloseConnection() {
		return closeConnection;
	}
}
