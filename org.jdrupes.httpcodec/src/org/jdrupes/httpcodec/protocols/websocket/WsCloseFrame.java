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
package org.jdrupes.httpcodec.protocols.websocket;

import java.nio.CharBuffer;

/**
 * @author Michael N. Lipp
 *
 */
public class WsCloseFrame extends WsFrameHeader {

	private Integer statusCode;
	private CharBuffer reason;
	
	/**
	 * Creates a new close control frame.
	 * 
	 * @param statusCode the status code (if any)
	 * @param reason the reason
	 */
	public WsCloseFrame(Integer statusCode, CharBuffer reason) {
		super();
		this.statusCode = statusCode;
		this.reason = reason;
	}
	
	/**
	 * @return the statusCode
	 */
	public Integer getStatusCode() {
		return statusCode;
	}
	/**
	 * @return the reason
	 */
	public CharBuffer getReason() {
		return reason;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WsCloseFrame [");
		if (statusCode != null) {
			builder.append("statusCode=");
			builder.append(statusCode);
			builder.append(", ");
		}
		if (reason != null) {
			builder.append("reason=");
			builder.append(reason.toString());
		}
		builder.append("]");
		return builder.toString();
	}
	
}
