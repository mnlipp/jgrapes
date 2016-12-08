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
import java.nio.CharBuffer;
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
 * using chunked content.
 * 
 * @author Michael N. Lipp
 */
public class EncoderChunkedTests {
	
	@Test
	public void testResponseChunkedOneStep()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare Response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		encoder.encode(response);
		Encoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// Check result
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n"
				+ "c\r\n"
				+ "Hello World!\r\n"
				+ "0\r\n"
				+ "\r\n"));
	}

	@Test
	public void testResponseChunkedSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		// Encode header
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.contains("Transfer-Encoding: chunked\r\n"));
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
		assertTrue(encoded.endsWith("\r\n"
				+ "c\r\n"
				+ "Hello World!\r\n"
				+ "0\r\n"
				+ "\r\n"));
	}

	@Test
	public void testResponseChunkedTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare Response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		encoder.encode(response);
		Common.tinyEncodeLoop(encoder, in, out);
		// Check result
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n"
				+ "1\r\n"
				+ "!\r\n"
				+ "0\r\n"
				+ "\r\n"));
	}

	@Test
	public void testCharResponseChunkedOneStep()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare Response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// Check result
		String encoded = new String(out.array(), 0, out.position(), "utf-8");
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n"
				+ "1f\r\n"
				+ "äöü€ Hello World! ÄÖÜß\r\n"
				+ "0\r\n"
				+ "\r\n"));
	}
	
	@Test
	public void testCharResponseChunkedSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		// Encode header
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.contains("Transfer-Encoding: chunked\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
		// Encode body
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		result = encoder.encode(in, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// "Encode" end of input
		result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		encoded = new String(out.array(), 0, out.position(), "utf-8");
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n"
				+ "1f\r\n"
				+ "äöü€ Hello World! ÄÖÜß\r\n"
				+ "0\r\n"
				+ "\r\n"));
	}

	@Test
	public void testCharResponseChunkedTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare Response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		encoder.encode(response);
		Common.tinyEncodeLoop(encoder, in, 1, out, 3, false);
		// Check result
		String encoded = new String(out.array(), 0, out.position(), "utf-8");
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.contains("\r\n"
				+ "3\r\n"
				+ "€\r\n"));
		assertTrue(encoded.endsWith("\r\n"
				+ "2\r\n"
				+ "ß\r\n"
				+ "0\r\n"
				+ "\r\n"));
	}

}
