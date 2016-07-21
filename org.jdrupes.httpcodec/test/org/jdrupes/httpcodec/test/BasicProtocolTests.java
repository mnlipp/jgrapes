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

import org.jdrupes.httpcodec.server.HttpRequestDecoder;
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
		HttpRequestDecoder.Result result = decoder.decode(buffer, null);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("GET", decoder.getHeader().getMethod());
		assertEquals("localhost", decoder.getHeader().getHost());
		assertEquals(8888, decoder.getHeader().getPort());
	}


}
