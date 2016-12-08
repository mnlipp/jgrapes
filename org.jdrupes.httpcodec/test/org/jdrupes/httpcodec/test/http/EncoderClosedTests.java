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
 * using collected content.
 * 
 * @author Michael N. Lipp
 */
public class EncoderClosedTests {
	
	@Test
	public void testResponseClosedAtOnce()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		// Encode rest
		Encoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 12\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseClosedSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
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
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 12\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseClosedTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		// Encode rest
		Common.tinyEncodeLoop(encoder, in, out);
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 12\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseCollectedOverflowAtOnce()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.setPendingLimit(4);
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		// Encode rest
		Encoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(result.getCloseConnection());
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(!encoded.contains("\r\nContent-Length:"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseClosedOverflowSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.setPendingLimit(4);
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
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
		assertTrue(result.getCloseConnection());
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(!encoded.contains("\r\nContent-Length:"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testResponseClosedOverflowTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.setPendingLimit(4);
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		// Encode rest
		Encoder.Result lastResult = Common.tinyEncodeLoop(encoder, in, out);
		// Check result
		assertFalse(lastResult.isOverflow());
		assertFalse(lastResult.isUnderflow());
		assertTrue(lastResult.getCloseConnection());
		// assertTrue(lastResult.getCloseConnection());
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(!encoded.contains("\r\nContent-Length:"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testCharResponseClosedAtOnce()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		// Encode rest
		Encoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 31\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\näöü€ Hello World! ÄÖÜß"));
	}

	@Test
	public void testCharResponseClosedSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
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
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 31\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\näöü€ Hello World! ÄÖÜß"));
	}

	@Test
	public void testCharResponseClosedTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		// Encode rest
		Common.tinyEncodeLoop(encoder, in, out);
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 31\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\näöü€ Hello World! ÄÖÜß"));
	}

	@Test
	public void testCharResponseClosedOverflowAtOnce()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.setPendingLimit(4);
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		// Encode rest
		Encoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(result.getCloseConnection());
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(!encoded.contains("\r\nContent-Length:"));
		assertTrue(encoded.endsWith("\r\n\r\näöü€ Hello World! ÄÖÜß"));
	}

	@Test
	public void testCharResponseClosedOverflowSeparatePhases()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.setPendingLimit(4);
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Encoder.Result result = encoder.encode(Codec.EMPTY_IN, out, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
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
		assertTrue(result.getCloseConnection());
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(!encoded.contains("\r\nContent-Length:"));
		assertTrue(encoded.endsWith("\r\n\r\näöü€ Hello World! ÄÖÜß"));
	}

	@Test
	public void testCharResponseClosedOverflowTiny()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.setPendingLimit(4);
		// Encode header
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		CharBuffer in = CharBuffer.wrap("äöü€ Hello World! ÄÖÜß");
		// Encode rest
		Common.tinyEncodeLoop(encoder, in, 1, out, 3, false);
		// Check result
		out.flip();
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.remaining());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(!encoded.contains("\r\nContent-Length:"));
		assertTrue(encoded.endsWith("\r\n\r\näöü€ Hello World! ÄÖÜß"));
	}

}
