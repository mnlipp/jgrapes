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
package org.jdrupes.httpcodec.protocols.websocket;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.ResponseEncoder;

/**
 * @author Michael N. Lipp
 *
 */
public class WsResponseEncoder implements ResponseEncoder<WsFrameHeader> {

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.ResponseEncoder#encode(org.jdrupes.httpcodec.MessageHeader)
	 */
	@Override
	public void encode(WsFrameHeader messageHeader) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.ResponseEncoder#encode(java.nio.Buffer, java.nio.ByteBuffer, boolean)
	 */
	@Override
	public org.jdrupes.httpcodec.ResponseEncoder.Result encode(Buffer in,
	        ByteBuffer out, boolean endOfInput) {
		// TODO Auto-generated method stub
		return null;
	}

}
