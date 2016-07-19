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
package org.jdrupes.httpcodec.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.RequestResult;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class DecodingTests {

	@Test
	public void testSplitHeader() throws UnsupportedEncodingException {
		// Partial header
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: local";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		RequestResult result = decoder.decode(buffer);
		assertFalse(result.hasMessage());
		assertFalse(result.hasResponse());
		assertFalse(result.hasPayloadBytes());
		assertFalse(result.hasPayloadChars());
		assertFalse(result.getCloseConnection());
		// Continue header
		reqText 
			= "host:8888\r\n"
			+ "\r\n";
		buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		result = decoder.decode(buffer);
		assertTrue(result.hasMessage());
		assertFalse(result.hasResponse());
		assertFalse(result.hasPayloadBytes());
		assertFalse(result.hasPayloadChars());
		assertFalse(result.getCloseConnection());
		assertEquals("GET", result.getMessage().getMethod());
		assertEquals("localhost", result.getMessage().getHost());
		assertEquals(8888, result.getMessage().getPort());
		assertEquals("/test", result.getMessage().getRequestUri().getPath());
	}
	
}
