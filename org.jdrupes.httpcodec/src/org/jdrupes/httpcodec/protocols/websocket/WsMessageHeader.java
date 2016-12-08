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

/**
 * @author Michael N. Lipp
 *
 */
public class WsMessageHeader extends WsFrameHeader {

	private boolean textMode;
	private boolean hasPayload;

	/**
	 * @param textMode indicates whether the data is sent as text  
	 */
	public WsMessageHeader(boolean textMode, boolean hasPayload) {
		super();
		this.textMode = textMode;
		this.hasPayload = hasPayload;
	}

	/**
	 * @return whether the data is sent as text
	 */
	public boolean isTextMode() {
		return textMode;
	}

	/**
	 * @return whether the message has a payload
	 */
	public boolean hasPayload() {
		return hasPayload;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WsMessageHeader [textMode=");
		builder.append(textMode);
		builder.append(", hasPayload=");
		builder.append(hasPayload);
		builder.append("]");
		return builder.toString();
	}
	
}
