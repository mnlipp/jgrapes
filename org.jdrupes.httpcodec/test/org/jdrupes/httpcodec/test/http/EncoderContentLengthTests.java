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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.test.Common;
import org.junit.Test;

/**
 * Tests focusing on the body, applicable to both requests and responses,
 * using content length.
 * 
 * @author Michael N. Lipp
 */
public class EncoderContentLengthTests {

	@Test
	public void testResponseContentLengthOneStep()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		response.setContentLength(12);
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(in, out, true);
		// Check result
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseContentLengthSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		response.setContentLength(12);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		// Encode header
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
		// Encode body
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		result = encoder.encode(in, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// "Encode" end of input
		result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseContentLengthTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		response.setContentLength(12);
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Common.tinyEncodeLoop(encoder, in, out);
		// Check result
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

}
