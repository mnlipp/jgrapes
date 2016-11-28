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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.util.Optional;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.util.ByteBufferUtils;
import org.jdrupes.httpcodec.util.OptimizedCharsetDecoder;

/**
 * @author Michael N. Lipp
 *
 */
public abstract class WsDecoder implements Decoder<WsFrameHeader> {

	private static enum State { READING_HEADER, READING_LENGTH,
		READING_MASK, READING_PAYLOAD };
	private static enum Opcode { CONT_FRAME, TEXT_FRAME, BIN_FRAME,
		CON_CLOSE, PING, PONG };
	private State state = State.READING_HEADER;
	private long bytesExpected = 2;
	private int headerHead = 0;
	private byte[] maskingKey = new byte[4];
	private int maskIndex;
	private long payloadLength = 0;
	private Opcode opcode;
	private boolean textMode = false;
	private OptimizedCharsetDecoder charDecoder = null;
	private WsReceiveInfo receivedHeader = null;
	private WsReceiveInfo reportedHeader = null;
	
	private void reset() {
		state = State.READING_HEADER;
		bytesExpected = 2;
		headerHead = 0;
		payloadLength = 0;
		receivedHeader = null;
		if (charDecoder != null) {
			charDecoder.reset();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Decoder#getHeader()
	 */
	@Override
	public Optional<WsFrameHeader> getHeader() {
		return Optional.ofNullable(receivedHeader);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Decoder#newResult(boolean, boolean)
	 */
	@Override
	public Result newResult(boolean overflow, boolean underflow) {
		if (receivedHeader != null && receivedHeader != reportedHeader) {
			reportedHeader = receivedHeader;
			return newResult(overflow, underflow, true);
		}
		return newResult(overflow, underflow, false);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.RequestDecoder#decode(java.nio.ByteBuffer, java.nio.Buffer, boolean)
	 */
	@Override
	public Decoder.Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		Decoder.Result result = null;
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
						continue; // shortcut, no need to check result
					}
					if (payloadLength == 127) {
						payloadLength = 0;
						bytesExpected = 8;
						state = State.READING_LENGTH;
						continue; // shortcut, no need to check result
					}
					if (isDataMasked()) {
						bytesExpected = 4;
						state = State.READING_MASK;
						continue; // shortcut, no need to check result
					}
					result = maybeEnterPayloadState();
					break;
				}
				break;
				
			case READING_LENGTH:
				payloadLength = (payloadLength << 8) | (in.get() & 0xff);
				if (--bytesExpected == 0) {
					if (isDataMasked()) {
						bytesExpected = 4;
						state = State.READING_MASK;
						continue; // shortcut, no need to check result
					}
					result = maybeEnterPayloadState();
					break;
				}
				break;
				
			case READING_MASK:
				maskingKey[4 - (int)bytesExpected] = in.get();
				if (--bytesExpected == 0) {
					maskIndex = 0;
					result = maybeEnterPayloadState();
					break;
				}
				continue; // shortcut, no need to check result
				
			case READING_PAYLOAD:
				if (out == null) {
					return newResult(true, !in.hasRemaining());
				}
				int initiallyAvaible = in.remaining();
				CoderResult decRes = copyData(out, in,
				        bytesExpected > Integer.MAX_VALUE
				                ? Integer.MAX_VALUE : (int) bytesExpected);
				bytesExpected -= (initiallyAvaible - in.remaining());
				if (bytesExpected == 0) {
					reset();
					return newResult(false, false);
				}
				return newResult(
				        (in.hasRemaining() && !out.hasRemaining())
				                || (decRes != null && decRes.isOverflow()),
				        !in.hasRemaining()
				                || (decRes != null && decRes.isUnderflow()));
			}
			if (result != null) {
				return result;
			}
		}
		return newResult(false, true);
	}

	private Decoder.Result maybeEnterPayloadState() {
		bytesExpected = payloadLength;
		switch (headerHead >> 8 & 0xf) {
		case 0:
			opcode = Opcode.CONT_FRAME;
			break;
		case 1:
			opcode = Opcode.TEXT_FRAME;
			textMode = true;
			if (charDecoder == null) {
				charDecoder = new OptimizedCharsetDecoder(
				        Charset.forName("UTF-8").newDecoder());
			}
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
		receivedHeader = new WsReceiveInfo(textMode);
		if (bytesExpected == 0) {
			return newResult(false, false);
		}
		state = State.READING_PAYLOAD;
		return null;
	}
	
	private boolean isDataMasked() {
		return (headerHead & 0x80) != 0;
	}
	
	private CoderResult copyData(Buffer out, ByteBuffer in, int limit) {
		if (out instanceof ByteBuffer) {
			if (!isDataMasked()) {
				ByteBufferUtils.putAsMuchAsPossible((ByteBuffer) out, in, limit);
				return null;
			}
			while (limit > 0 && in.hasRemaining() && out.hasRemaining()) {
				((ByteBuffer) out).put
					((byte)(in.get() ^ maskingKey[maskIndex]));
				maskIndex = (maskIndex + 1) % 4;
			}
			return null;
		} 
		if (out instanceof CharBuffer) {
			if (isDataMasked()) {
				ByteBuffer unmasked = ByteBuffer.allocate(1);
				CoderResult res = null;
				while (limit > 0 && in.hasRemaining() && out.hasRemaining()) {
					unmasked.put((byte)(in.get() ^ maskingKey[maskIndex]));
					maskIndex = (maskIndex + 1) % 4;
					limit -= 1;
					unmasked.flip();
					res = charDecoder.decode(unmasked, (CharBuffer)out, false);
					unmasked.clear();
				}
				return res;
			}
			int oldLimit = in.limit();
			try {
				if (in.remaining() > limit) {
					in.limit(in.position() + limit);
				}
				return charDecoder.decode(in, (CharBuffer)out, false);
			} finally {
				in.limit(oldLimit);
			}
		} else {
			throw new IllegalArgumentException(
			        "Only Byte- or CharBuffer are allowed.");
		}
	}

}
