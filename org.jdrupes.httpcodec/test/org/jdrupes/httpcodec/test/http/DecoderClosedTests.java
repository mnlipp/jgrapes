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
package org.jdrupes.httpcodec.test.http;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.test.Common;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class DecoderClosedTests {

	/**
	 * Response with body terminated by close.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws HttpProtocolException 
	 */
	@Test
	public void testBodyClosedAtOnce()
	        throws UnsupportedEncodingException, HttpProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		ByteBuffer body = ByteBuffer.allocate(1024);
		Decoder.Result<?> result = decoder.decode(buffer, body, true);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(), 
				decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body terminated by close (delayed close).
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws HttpProtocolException 
	 */
	@Test
	public void testBodyClosedSeparatePhases()
	        throws UnsupportedEncodingException, HttpProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		ByteBuffer body = ByteBuffer.allocate(1024);
		Decoder.Result<?> result = decoder.decode(buffer, body, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
				decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		// Close
		result = decoder.decode(buffer, body, true);
		assertFalse(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body terminated by close.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws HttpProtocolException 
	 */
	@Test
	public void testBodyClosedTiny()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		ByteBuffer out = ByteBuffer.allocate(1024);
		Common.tinyDecodeLoop(decoder, in, out);
		Decoder.Result<?> result = decoder.decode(in, out, true);
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		out.flip();
		String bodyText = new String(out.array(), out.position(),
		        out.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body terminated by close.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws HttpProtocolException 
	 */
	@Test
	public void testCharBodyClosedAtOnce()
	        throws UnsupportedEncodingException, HttpProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Vary: Accept-Encoding\r\n"
				+ "Content-Type: text/plain; charset=utf-8\r\n"
				+ "\r\n"
				+ "ÄÖÜaöüß€.";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("utf-8"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		CharBuffer body = CharBuffer.allocate(1024);
		Decoder.Result<?> result = decoder.decode(buffer, body, true);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		assertEquals("ÄÖÜaöüß€.", body.toString());
	}

	/**
	 * Response with body terminated by close and small output buffer.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws HttpProtocolException 
	 */
	@Test
	public void testCharBodyClosedTinyOut()
	        throws UnsupportedEncodingException, HttpProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Content-Type: text/plain; charset=utf-8\r\n"
				+ "\r\n"
				+ "ÄÖÜaöüß€.";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("utf-8"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		CharBuffer body = CharBuffer.allocate(1);
		Decoder.Result<?> result = decoder.decode(buffer, body, true);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().get().getStatusCode());
		assertFalse(result.isUnderflow());
		assertTrue(result.isOverflow());
		assertTrue(buffer.hasRemaining());
		StringBuilder bodyText = new StringBuilder();
		while (true) {
			body.flip();
			bodyText.append(body.toString());
			if (!result.isOverflow()) {
				assertTrue(result.getCloseConnection());
				break;
			}
			body.clear();
			result = decoder.decode(buffer, body, true);
		}
		assertEquals("ÄÖÜaöüß€.", bodyText.toString());
	}

	/**
	 * Response with body terminated by close and small output buffer.
	 * 
	 * @throws HttpProtocolException 
	 * @throws IOException 
	 */
	@Test
	public void testCharBodyClosedTinyIn()
	        throws HttpProtocolException, IOException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Keep-Alive: timeout=5, max=100\r\n"
				+ "Content-Type: text/plain; charset=utf-8\r\n"
				+ "\r\n"
				+ "ÄÖÜaöüß€.";
		InputStream is = new ByteArrayInputStream(reqText.getBytes("utf-8"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		CharBuffer body = CharBuffer.allocate(1024);
		byte[] ba = new byte[1];
		Decoder.Result<?> result;
		while (true) {
			int b = is.read();
			if (b == -1) {
				break;
			}
			ba[0] = (byte)b;
			ByteBuffer buffer = ByteBuffer.wrap(ba);
			result = decoder.decode(buffer, body, false);
			assertTrue(result.isUnderflow());
		}
		result = decoder.decode(Codec.EMPTY_IN, body, true);
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		body.flip();
		assertEquals("ÄÖÜaöüß€.", body.toString());
	}

}
