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

import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class BasicProtocolTests {

	/**
	 * RFC 7230 3.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testLeadingWhitespace() throws UnsupportedEncodingException {
		String reqText 
			= "\r\n"
			+ "\r\n"
			+ "GET / HTTP/1.0\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(decoder.getHeader().get().messageHasBody());
		assertEquals("GET", decoder.getHeader().get().getMethod());
		assertEquals("localhost", decoder.getHeader().get().getHost());
		assertEquals(8888, decoder.getHeader().get().getPort());
	}

	@Test
	public void testUnsupportedVersion() throws UnsupportedEncodingException {
		String reqText 
			= "GET / HTTP/0.8\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertFalse(result.isHeaderCompleted());
		assertTrue(result.hasResponse());
		assertEquals(505, result.getResponse().getStatusCode());
	}

	/**
	 * RFC 7230 3.1.1
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testRequestWithWhitespace()
	        throws UnsupportedEncodingException {
		String reqText 
			= "GET /tricky/Resource HTTP/1.1 HTTP/1.1\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertFalse(result.isHeaderCompleted());
		assertTrue(result.hasResponse());
		assertEquals(400, result.getResponse().getStatusCode());
	}

	/**
	 * RFC 7230 3.2.4
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testRequestWithSpaceBeforeColon()
	        throws UnsupportedEncodingException {
		String reqText 
			= "GET / HTTP/1.1\r\n"
			+ "Host : localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertFalse(result.isHeaderCompleted());
		assertTrue(result.hasResponse());
		assertEquals(400, result.getResponse().getStatusCode());
	}


}
