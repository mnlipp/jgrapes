package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;

import org.jdrupes.httpcodec.HttpCodec;
import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpResponseEncoder;
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
		HttpResponseEncoder.Result result = encoder.encode(in, out, true);
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
		HttpResponseEncoder.Result result = encoder.encode(HttpCodec.EMPTY_IN,
		        out, false);
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

}
