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
public abstract class DecoderResult<T extends Message> extends CodecResult {

	private T message;

	/**
	 * Creates a new result.
	 * 
	 * @param message the decoded message
	 * @param overflow {@code true} if the data didn't fit in the out buffer
	 * @param underflow {@code true} if more data is expected
	 * @param closeConnection {@code true} if the connection should be closed
	 */
	protected DecoderResult(T message, boolean overflow, 
			boolean underflow, boolean closeConnection) {
		super(overflow, underflow, closeConnection);
		this.message = message;
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
}
