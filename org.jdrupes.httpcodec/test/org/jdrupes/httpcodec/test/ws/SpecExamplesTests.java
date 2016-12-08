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

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.websocket.WsDecoder;
import org.jdrupes.httpcodec.protocols.websocket.WsEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.protocols.websocket.WsPingFrame;
import org.jdrupes.httpcodec.protocols.websocket.WsPongFrame;
import org.junit.Test;

public class SpecExamplesTests {

	@Test
	public void testDecodeSingleUnmaskedText() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x81, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		CharBuffer txt = CharBuffer.allocate(20);
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, txt, true);
		txt.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().isPresent());
		assertTrue(decoder.getHeader().get() instanceof WsMessageHeader);
		WsMessageHeader hdr = (WsMessageHeader)decoder.getHeader().get();
		assertTrue(hdr.isTextMode());
		assertTrue(hdr.hasPayload());
		assertEquals("Hello", txt.toString());
	}
	
	@Test
	public void testEncodeSingleUnmaskedText() throws ProtocolException {
		byte[] expectedBytes = new byte[] 
				{(byte)0x81, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		WsEncoder encoder = new WsEncoder(false);
		CharBuffer txt = CharBuffer.allocate(20);
		txt.put("Hello");
		txt.flip();
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsMessageHeader(true, true));
		Encoder.Result result = encoder.encode(txt, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertEquals(expectedBytes.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(expectedBytes, msgBytes);
	}
	
	@Test
	public void testDecodeSingleMaskedText() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x81, (byte)0x85, 0x37, (byte)0xfa, 0x21, 0x3d,
						0x7f, (byte)0x9f, 0x4d, 0x51, 0x58};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		CharBuffer txt = CharBuffer.allocate(20);
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, txt, true);
		txt.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().isPresent());
		assertTrue(decoder.getHeader().get() instanceof WsMessageHeader);
		WsMessageHeader hdr = (WsMessageHeader)decoder.getHeader().get();
		assertTrue(hdr.isTextMode());
		assertTrue(hdr.hasPayload());
		assertEquals("Hello", txt.toString());
	}
	
	@Test
	public void testDecodeFragmentedUnmaskedText() throws ProtocolException {
		byte[] msgBytes1 = new byte[] {0x01, 0x03, 0x48, 0x65, 0x6c};
		byte[] msgBytes2 = new byte[] {(byte)0x80, 0x02, 0x6c, 0x6f};
		ByteBuffer msg = ByteBuffer.allocate(40);
		msg.put(msgBytes1);
		msg.flip();
		CharBuffer txt = CharBuffer.allocate(20);
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, txt, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().isPresent());
		assertTrue(decoder.getHeader().get() instanceof WsMessageHeader);
		WsMessageHeader hdr = (WsMessageHeader)decoder.getHeader().get();
		assertTrue(hdr.isTextMode());
		assertTrue(hdr.hasPayload());
		msg.clear();
		msg.put(msgBytes2);
		msg.flip();
		result = decoder.decode(msg, txt, false);
		txt.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.isHeaderCompleted());
		assertEquals("Hello", txt.toString());
	}
	
	@Test
	public void testEncodeFragmentedUnmaskedText() throws ProtocolException {
		byte[] msgBytes1 = new byte[] {0x01, 0x03, 0x48, 0x65, 0x6c};
		byte[] msgBytes2 = new byte[] {(byte)0x80, 0x02, 0x6c, 0x6f};
		WsEncoder encoder = new WsEncoder(false);
		CharBuffer txt = CharBuffer.allocate(20);
		txt.put("Hel");
		txt.flip();
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsMessageHeader(true, true));
		Encoder.Result result = encoder.encode(txt, msg, false);
		msg.flip();
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertEquals(msgBytes1.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);		
		assertArrayEquals(msgBytes1, msgBytes);

		// Part 2
		txt.clear();
		txt.put("lo");
		txt.flip();
		msg.clear();
		result = encoder.encode(txt, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());			
		assertEquals(msgBytes2.length, msg.remaining());
		msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(msgBytes2, msgBytes);
	}
	
	@Test
	public void testDecodeUnmaskedPing() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().isPresent());
		assertTrue(decoder.getHeader().get() instanceof WsPingFrame);
		WsPingFrame pingHdr = (WsPingFrame)decoder.getHeader().get();
		CharBuffer txt = Charset.forName("utf-8")
				.decode(pingHdr.getApplicationData());
		assertEquals("Hello", txt.toString());
		assertNotNull(result.getResponse());
		WsPongFrame pongHdr = (WsPongFrame)result.getResponse();
		txt = Charset.forName("utf-8")
				.decode(pongHdr.getApplicationData());
		assertEquals("Hello", txt.toString());
	}
	
	@Test
	public void testEncodeUnmaskedPing() 
			throws ProtocolException, UnsupportedEncodingException {
		byte[] pingBytes = new byte[] 
				{(byte)0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		WsEncoder encoder = new WsEncoder(false);
		ByteBuffer appData = ByteBuffer.allocate(20);
		appData.put("Hello".getBytes("utf-8"));
		appData.flip();
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsPingFrame(appData));
		Encoder.Result result = encoder.encode(null, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertEquals(pingBytes.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(pingBytes, msgBytes);
	}
	
	@Test
	public void testDecodeMaskedPong() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x8a, (byte)0x85, 0x37, (byte)0xfa, 0x21, 0x3d,
					(byte)0x7f, (byte)0x9f, 0x4d, 0x51, 0x58};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().isPresent());
		assertTrue(decoder.getHeader().get() instanceof WsPongFrame);
		WsPongFrame hdr = (WsPongFrame)decoder.getHeader().get();
		CharBuffer txt = Charset.forName("utf-8")
				.decode(hdr.getApplicationData());
		assertEquals("Hello", txt.toString());
	}
	
	
}
