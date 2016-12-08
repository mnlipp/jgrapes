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
package org.jdrupes.httpcodec.test.ws;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.websocket.WsDecoder;
import org.jdrupes.httpcodec.protocols.websocket.WsEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.test.Common;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class TinyTests {

	@Test
	public void testTinyDecodeSingleUnmaskedText() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x81, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		CharBuffer txt = CharBuffer.allocate(20);
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = Common.tinyDecodeLoop(decoder, msg, txt);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		txt.flip();
		assertTrue(decoder.getHeader().isPresent());
		assertTrue(decoder.getHeader().get() instanceof WsMessageHeader);
		WsMessageHeader hdr = (WsMessageHeader)decoder.getHeader().get();
		assertTrue(hdr.isTextMode());
		assertTrue(hdr.hasPayload());
		assertEquals("Hello", txt.toString());
	}
	
	@Test
	public void testTinyEncodeSingleUnmaskedText() throws ProtocolException {
		byte[] expectedBytes = new byte[] 
				{(byte)0x81, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		WsEncoder encoder = new WsEncoder(false);
		CharBuffer txt = CharBuffer.allocate(20);
		txt.put("Hello");
		txt.flip();
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsMessageHeader(true, true));
		Encoder.Result result = Common.tinyEncodeLoop
				(encoder, txt, txt.capacity(), msg, 1, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		msg.flip();
		assertEquals(expectedBytes.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(expectedBytes, msgBytes);
	}
	
	@Test
	public void testTinyEncodeTextFramed() throws ProtocolException {
		WsEncoder encoder = new WsEncoder(false);
		CharBuffer txt = CharBuffer.allocate(20);
		txt.put("Hello");
		txt.flip();
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsMessageHeader(true, true));
		Encoder.Result result = Common.tinyEncodeLoop 
				(encoder, txt, 1, msg, 1, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		msg.flip();
		assertEquals(15, msg.remaining());
		txt.clear();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> decRes = decoder.decode(msg, txt, true);
		assertFalse(decRes.isOverflow());
		assertFalse(decRes.isUnderflow());
		txt.flip();
		assertEquals("Hello", txt.toString());
	}
	
}
