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
		HttpResponseEncoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		// Check result
		String encoded = new String(out.array(), 0, out.position());
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
		HttpResponseEncoder.Result result = encoder.encode(HttpCodec.EMPTY_IN, 
				out, false);
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
		String encoded = new String(out.array(), 0, out.position());
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
		String encoded = new String(out.array(), 0, out.position());
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
		HttpResponseEncoder.Result result = encoder.encode(in, out, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(result.getCloseConnection());
		// Check result
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
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
		HttpResponseEncoder.Result result = encoder.encode(HttpCodec.EMPTY_IN, 
				out, false);
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
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
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
		HttpResponseEncoder.Result lastResult = Common.tinyEncodeLoop(encoder,
		        in, out);
		// Check result
		assertFalse(lastResult.isOverflow());
		assertFalse(lastResult.isUnderflow());
		assertTrue(lastResult.getCloseConnection());
		// assertTrue(lastResult.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

}
