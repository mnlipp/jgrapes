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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.util.ByteBufferOutputStream;
import org.jdrupes.httpcodec.util.ByteBufferUtils;

/**
 * @author Michael N. Lipp
 */
public class WsEncoder implements Encoder<WsFrameHeader> {

	private static enum State { WRITING_HEADER,  
		WRITING_LENGTH, WRITING_MASK, WRITING_PAYLOAD };
	private static float bytesPerCharUtf8		
		= Charset.forName("utf-8").newEncoder().averageBytesPerChar();		
	private SecureRandom randoms = new SecureRandom();
	private State state;
	private boolean firstFrame;
	private WsFrameHeader messageHeader;
	private int headerHead;
	private boolean textMode = false;
	private long bytesToSend;
	private long payloadSize;
	private int payloadBytes;
	private boolean doMask = false;
	private byte[] maskingKey = new byte[4];
	private int maskIndex;
	private ByteBufferOutputStream convData = new ByteBufferOutputStream();

	/**
	 * Creates new encoder.
	 * 
	 * @param mask set if the data is to be masked (client)
	 */
	public WsEncoder(boolean mask) {
		super();
		this.doMask = mask;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.ResponseEncoder#encode(org.jdrupes.httpcodec.MessageHeader)
	 */
	@Override
	public void encode(WsFrameHeader messageHeader) {
		this.messageHeader = messageHeader;
		if (messageHeader instanceof WsMessageHeader) {
			textMode = ((WsMessageHeader) messageHeader).isTextMode();
			if (textMode) {
				headerHead = (1 << 8);
			} else {
				headerHead = (2 << 8);
			}
		} else if (messageHeader instanceof WsCloseFrame) {
			headerHead = (8 << 8);
		} else if (messageHeader instanceof WsPingFrame) {
			headerHead = (9 << 8);
		} else if (messageHeader instanceof WsPongFrame) {
			headerHead = (10 << 8);
		} else {
			throw new IllegalArgumentException("Invalid hessage header type");
		}
		bytesToSend = 2;
		state = State.WRITING_HEADER;
		firstFrame = true;
	}

	@Override
	public Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		Result result = null;
		while (out.remaining() > 0) {
			switch(state) {
			case WRITING_HEADER:
				if (bytesToSend == 2) {
					prepareHeaderHead(in, endOfInput);
				}
				out.put((byte)(headerHead >> 8 * --bytesToSend));
				if (bytesToSend > 0) {
					continue;
				}
				if (payloadBytes > 0) {
					state = State.WRITING_LENGTH;
					bytesToSend = payloadBytes;
					continue;
				}
				// Length written
				result = nextAfterLength();
				break;
			case WRITING_LENGTH:
				out.put((byte)(payloadSize >> 8 * --bytesToSend));
				if (bytesToSend > 0) {
					continue;
				}
				result = nextAfterLength();
				break;
			case WRITING_MASK:
				out.put(maskingKey[4 - (int)bytesToSend]);
				if (--bytesToSend > 0) {
					continue;
				}
				result = nextAfterMask();
				break;
			case WRITING_PAYLOAD:
				int posBefore = out.position();
				outputPayload(in, out);
				bytesToSend -= (out.position() - posBefore);
				if (bytesToSend == 0) {
					convData.clear();
					return newResult(false, false, 
							(messageHeader instanceof WsCloseFrame));
				}
				return newResult(!out.hasRemaining(),
						(messageHeader instanceof WsMessageHeader) 
							&& !in.hasRemaining(), false);
			}
			if (result != null) {
				return result;
			}
		}
		return newResult(true, false, false);
	}

	private void prepareHeaderHead(Buffer in, boolean endOfInput) {
		if (!firstFrame) {
			headerHead = 0;
		}
		firstFrame = false;
		if (endOfInput) {
			headerHead |= 0x8000;
		}
		if (doMask) {
			headerHead |= 0x80;
			randoms.nextBytes(maskingKey);
		}

		// Prepare payload
		if (messageHeader instanceof WsMessageHeader) {
			if (!textMode) {
				payloadSize = in.remaining();
			} else {
				convData.clear();
				convTextData(in);
			}
		} else if (messageHeader instanceof WsCloseFrame) {
			payloadSize = 0;
			if (((WsCloseFrame)messageHeader).getStatusCode() != null) {
				convData.clear();
				int code = ((WsCloseFrame)messageHeader).getStatusCode();
				try {
					convData.write(code >> 8);
					convData.write(code & 0xff);
					payloadSize = 2;
				} catch (IOException e) {
					// Formally thrown, cannot happen
				}
				if (((WsCloseFrame)messageHeader).getReason() != null) {
					convTextData(((WsCloseFrame)messageHeader).getReason());
				}
			}
		} else if (messageHeader instanceof WsDefaultControlFrame) {
			payloadSize = ((WsDefaultControlFrame)messageHeader)
					.getApplicationData().remaining();
		}
		
		// Code payload size
		if (payloadSize <= 125) {
			headerHead |= payloadSize;
			payloadBytes = 0;
		} else if (payloadSize < 0x10000) {
			headerHead |= 126;
			payloadBytes = 2;
		} else {
			headerHead |= 127;
			payloadBytes = 8;
		}
	}

	private void convTextData(Buffer in) {
		convData.setOverflowBufferSize
			((int) (in.remaining() * bytesPerCharUtf8));
		try {
			OutputStreamWriter charWriter = new OutputStreamWriter(
			        convData, "utf-8");
			if (in.hasArray()) {
				// more efficient than CharSequence
				charWriter.write(((CharBuffer) in).array(),
				        in.arrayOffset() + in.position(),
				        in.remaining());
			} else {
				charWriter.append((CharBuffer) in);
			}
			in.position(in.limit());
			charWriter.flush();
			payloadSize = convData.bytesWritten();
		} catch (IOException e) {
			// Formally thrown, cannot happen
		}
	}
	
	private Result nextAfterLength() {
		if (doMask) {
			bytesToSend = 4;
			state = State.WRITING_MASK;
			return null;
		}
		return nextAfterMask();
	}
	
	private Result nextAfterMask() {
		if (payloadSize == 0) {
			return newResult(false, false, false);
		}
		maskIndex = 0;
		bytesToSend = payloadSize;
		state = State.WRITING_PAYLOAD;
		return null;
	}
	
	private void outputPayload(Buffer in, ByteBuffer out) {
		if (!doMask) {
			if (textMode) {
				convData.assignBuffer(out);
			} else {
				ByteBufferUtils
					.putAsMuchAsPossible(out, (ByteBuffer) in);
			}
		} else {
			if (textMode) {
				in = ByteBuffer.allocate(out.remaining());
				convData.assignBuffer((ByteBuffer)in);
				in.flip();
			}
			while (bytesToSend > 0
			        && in.hasRemaining() && out.hasRemaining()) {
				out.put((byte) (((ByteBuffer) in)
				        .get() ^ maskingKey[maskIndex]));
				maskIndex = (maskIndex + 1) % 4;
			}
		}
	}

}
