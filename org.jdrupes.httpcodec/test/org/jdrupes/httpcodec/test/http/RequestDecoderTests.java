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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.fields.HttpCookieListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class RequestDecoderTests {

	/**
	 * Simple GET request.
	 */
	@Test
	public void testBasicGetRequestAtOnce()
	        throws UnsupportedEncodingException {
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: localhost:8888\r\n"
			+ "Connection: keep-alive\r\n"
			+ "User-Agent: JUnit\r\n"
			+ "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,"
			+ "image/webp,*/*;q=0.8\r\n"
			+ "Accept-Encoding: gzip, deflate, sdch\r\n"
			+ "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
			+ "Cookie: _test.=yes; gsScrollPos=\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(decoder.getHeader().get().messageHasBody());
		assertEquals("GET", decoder.getHeader().get().getMethod());
		assertEquals("/test",
		        decoder.getHeader().get().getRequestUri().getPath());
		Optional<HttpCookieListField> field = decoder.getHeader()
				.flatMap(h -> h.getField
						(HttpCookieListField.class, HttpField.COOKIE));
		assertEquals(2, field.get().size());
		assertEquals("yes", field.get().valueForName("_test.").get());
		assertEquals("", field.get().valueForName("gsScrollPos").get());
	}

	
	/**
	 * Request with header in two parts.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testRequestSplitHeader() throws UnsupportedEncodingException {
		// Partial header
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: local";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		// Continue header
		reqText 
			= "host:8888\r\n"
			+ "\r\n";
		buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(decoder.getHeader().get().messageHasBody());
		assertEquals("GET", decoder.getHeader().get().getMethod());
		assertEquals("localhost", decoder.getHeader().get().getHost());
		assertEquals(8888, decoder.getHeader().get().getPort());
		assertEquals("/test",
		        decoder.getHeader().get().getRequestUri().getPath());
	}

	/**
	 * POST with "expected" body (all in one call).
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testHeaderAndBodyAtOnce()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastname=Grapes";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertEquals("POST", decoder.getHeader().get().getMethod());
		assertEquals("/form",
		        decoder.getHeader().get().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	/**
	 * POST with "unexpected" body (no out buffer).
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testInterruptedBody()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastname=Grapes";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertEquals("POST", decoder.getHeader().get().getMethod());
		assertEquals("/form",
		        decoder.getHeader().get().getRequestUri().getPath());
		assertTrue(result.isOverflow());
		assertFalse(result.isUnderflow());
		// Get body
		ByteBuffer body = ByteBuffer.allocate(1024);
		result = decoder.decode(buffer, body, false);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	/**
	 * POST with too small out buffer.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testPostSplitOut()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastname=Grapes";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(20);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertEquals("POST", decoder.getHeader().get().getMethod());
		assertEquals("/form",
		        decoder.getHeader().get().getRequestUri().getPath());
		assertTrue(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastnam", bodyText);
		// Remaining
		body.clear();
		result = decoder.decode(buffer, body, false);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("e=Grapes", bodyText);
	}

	/**
	 * POST with input split in body.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testPostSplitIn()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastnam";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertEquals("POST", decoder.getHeader().get().getMethod());
		assertEquals("/form",
		        decoder.getHeader().get().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		// Rest
		buffer = ByteBuffer.wrap("e=Grapes".getBytes("ascii"));
		result = decoder.decode(buffer, body, false);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	/**
	 * POST with chunked body.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testPostChunked()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Transfer-Encoding: chunked\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "14;dummy=0\r\n"
		        + "firstname=J.&lastnam\r\n"
		        + "8\r\n"
		        + "e=Grapes\r\n"
		        + "0\r\n"
		        + "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertEquals("POST", decoder.getHeader().get().getMethod());
		assertEquals("/form",
		        decoder.getHeader().get().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	/**
	 * POST with chunked body and trailer part.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testPostChunkedWithTrailer()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Transfer-Encoding: chunked\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "1c;dummy=0\r\n"
		        + "firstname=J.&lastname=Grapes\r\n"
		        + "0\r\n"
		        + "X-Test-Field: Valid\r\n"
		        + "X-Summary-Field: Good\r\n"
		        + "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertEquals("POST", decoder.getHeader().get().getMethod());
		assertEquals("/form",
		        decoder.getHeader().get().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
		// Trailer
		Optional<HttpStringListField> trailer = decoder.getHeader()
				.flatMap(f -> f.getField
						(HttpStringListField.class, HttpField.TRAILER));
		assertEquals(2, trailer.get().size());
		trailer.get().containsIgnoreCase("X-Test-Field");
		trailer.get().containsIgnoreCase("X-Summary-Field");
		Optional<HttpStringField> testField = decoder.getHeader()
		        .flatMap(h -> h.getField
		        		(HttpStringField.class, "X-Test-Field"));
		assertEquals("Valid", testField.get().getValue());
		testField = decoder.getHeader().flatMap
				(h -> h.getField(HttpStringField.class, "X-Summary-Field"));
		assertEquals("Good", testField.get().getValue());
	}
}
