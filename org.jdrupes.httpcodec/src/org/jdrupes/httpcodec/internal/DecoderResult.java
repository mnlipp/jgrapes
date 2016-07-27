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
 * The result from invoking the decoder. Compared to the general codec
 * result, all decoders return the information whether the message header
 * has been decoded completely during the last invocation. 
 * 
 * @author Michael N. Lipp
 */
public abstract class DecoderResult extends CodecResult {

	private boolean headerCompleted;

	/**
	 * Creates a new result.
	 * 
	 * @param headerCompleted indicates that the message header has been 
	 * completed and the message (without body) is available
	 * @param overflow {@code true} if the data didn't fit in the out buffer
	 * @param underflow {@code true} if more data is expected
	 */
	protected DecoderResult(boolean headerCompleted, boolean overflow, 
			boolean underflow) {
		super(overflow, underflow);
		this.headerCompleted = headerCompleted;
	}

	/**
	 * Returns {@code true} if the message header has been decoded 
	 * completely during the decoder invocation that returned this 
	 * result and is now available. 
	 * 
	 * @return the result
	 */
	public boolean isHeaderCompleted() {
		return headerCompleted;
	}
	
}
