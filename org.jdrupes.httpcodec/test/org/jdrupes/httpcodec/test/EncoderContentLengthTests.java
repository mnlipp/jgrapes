package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;

import org.jdrupes.httpcodec.HttpCodec;
import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.HttpResponse;
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
		HttpResponseEncoder encoder = new HttpResponseEncoder(null);
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpResponseEncoder.Result result = encoder.encode(in, out, true);
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
		HttpResponseEncoder encoder = new HttpResponseEncoder(null);
		encoder.encode(response);
		// Encode header
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpResponseEncoder.Result result = encoder.encode(HttpCodec.EMPTY_IN,
		        out, false);
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
		HttpResponseEncoder encoder = new HttpResponseEncoder(null);
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		Common.tinyEncodeLoop(encoder, in, out);
		// Check result
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

}
