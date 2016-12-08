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
package org.jdrupes.httpcodec;

/**
 * Represents the base class of all exceptions thrown due to protocol
 * violations.
 * 
 * @author Michael N. Lipp
 */
public class ProtocolException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ProtocolException() {
	}

	/**
	 * @param message the message
	 */
	public ProtocolException(String message) {
		super(message);
	}

	/**
	 * @param cause the cause
	 */
	public ProtocolException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message the message
	 * @param cause the cause
	 */
	public ProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message the message
	 * @param cause the cause
	 * @param enableSuppression whether to enable suppression
	 * @param writableStackTrace whether the stack trace is writable
	 */
	public ProtocolException(String message, Throwable cause,
	        boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
