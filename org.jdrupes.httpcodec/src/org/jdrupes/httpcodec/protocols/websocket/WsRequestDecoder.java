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
import java.util.Optional;

import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.RequestDecoder;

/**
 * @author Michael N. Lipp
 *
 */
public class WsRequestDecoder implements RequestDecoder
	<WsMessageHeader, WsMessageHeader> {

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Decoder#getHeader()
	 */
	@Override
	public Optional<WsMessageHeader> getHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.RequestDecoder#decode(java.nio.ByteBuffer, java.nio.Buffer, boolean)
	 */
	@Override
	public org.jdrupes.httpcodec.RequestDecoder.Result<WsMessageHeader> decode(
	        ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

}
