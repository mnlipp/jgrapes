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

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.RequestDecoder;
import org.jdrupes.httpcodec.util.ByteBufferUtils;

/**
 * @author Michael N. Lipp
 *
 */
public class WsDecoder implements Decoder<WsFrameHeader> {

	private static enum State { READING_HEADER, READING_LENGTH,
		READING_MASK, READING_PAYLOAD };
	private static enum Opcode { CONT_FRAME, TEXT_FRAME, BIN_FRAME,
		CON_CLOSE, PING, PONG };
	private State state = State.READING_HEADER;
	private long bytesExpected = 2;
	private int headerHead = 0;
	private long maskingKey = 0;
	private long payloadLength = 0;
	private Opcode opcode;
	private boolean textMode = false;
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Decoder#getHeader()
	 */
	@Override
	public Optional<WsFrameHeader> getHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.RequestDecoder#decode(java.nio.ByteBuffer, java.nio.Buffer, boolean)
	 */
	@Override
	public RequestDecoder.Result<WsFrameHeader> 
		decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		RequestDecoder.Result<WsFrameHeader> result = null;
		while (in.hasRemaining()) {
			switch (state) {
			case READING_HEADER:
				headerHead = (headerHead << 8) | (in.get() & 0xFF);
				if (--bytesExpected == 0) {
					payloadLength = headerHead & 0x7f;
					if (payloadLength == 126) {
						payloadLength = 0;
						bytesExpected = 2;
						state = State.READING_LENGTH;
						continue;
					}
					if (payloadLength == 127) {
						payloadLength = 0;
						bytesExpected = 8;
						state = State.READING_LENGTH;
						continue;
					}
					if (isDataMasked()) {
						bytesExpected = 4;
						state = State.READING_MASK;
						continue;
					}
					result = enterPayloadState();
					break;
				}
				break;
				
			case READING_LENGTH:
				payloadLength = (payloadLength << 8) | (in.get() & 0xff);
				if (--bytesExpected == 0) {
					if (isDataMasked()) {
						bytesExpected = 4;
						state = State.READING_MASK;
						continue;
					}
					result = enterPayloadState();
					break;
				}
				break;
				
			case READING_MASK:
				maskingKey = (maskingKey << 8) | (in.get() & 0xff);
				if (--bytesExpected == 0) {
					result = enterPayloadState();
					break;
				}
				break;
				
			case READING_PAYLOAD:
				if (!isDataMasked() && !textMode) {
					ByteBufferUtils.putAsMuchAsPossible
						((ByteBuffer)out, in, payloadLength > Integer.MAX_VALUE
								? Integer.MAX_VALUE : (int)payloadLength);
				}
			}
			if (result != null) {
				return result;
			}
		}
		return new RequestDecoder.Result<WsFrameHeader>
			(false, null, false, false, true);
	}

	private RequestDecoder.Result<WsFrameHeader> enterPayloadState() {
		bytesExpected = payloadLength;
		state = State.READING_PAYLOAD;
		switch (headerHead >> 8 & 0xf) {
		case 0:
			opcode = Opcode.CONT_FRAME;
			break;
		case 1:
			opcode = Opcode.TEXT_FRAME;
			textMode = true;
			break;
		case 2:
			opcode = Opcode.BIN_FRAME;
			textMode = false;
			break;
		case 8:
			opcode = Opcode.CON_CLOSE;
			break;
		case 9:
			opcode = Opcode.PING;
			break;
		case 10:
			opcode = Opcode.PONG;
			break;
		}
		RequestDecoder.Result<WsFrameHeader> result = null;
		return result;
	}
	
	private boolean isDataMasked() {
		return (headerHead & 0x80) != 0;
	}
	
	
}
