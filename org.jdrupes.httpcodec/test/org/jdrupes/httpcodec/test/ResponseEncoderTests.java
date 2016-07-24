package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;

import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.internal.Codec.HttpProtocol;
import org.jdrupes.httpcodec.internal.Codec.HttpStatus;
import org.jdrupes.httpcodec.server.HttpResponseEncoder;
import org.junit.Test;

public class ResponseEncoderTests {

	@Test
	public void testSimpleResponse() {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpResponseEncoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
	}

	@Test
	public void testGetResponseWithContentLength()
	        throws UnsupportedEncodingException, ParseException {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		response.setContentLength(12);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpResponseEncoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));

		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		result = encoder.encode(in, out);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		result = encoder.encode(null, out);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		
		encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

	@Test
	public void testGetResponseWithChunked()
	        throws UnsupportedEncodingException, ParseException {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpResponseEncoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.contains("Transfer-Encoding: chunked\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));

		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		result = encoder.encode(in, out);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		result = encoder.encode(null, out);
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
	public void testGetResponseWithCollectedBody()
	        throws UnsupportedEncodingException, ParseException {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		response.setMessageHasBody(true);
		response.setContentType("text", "plain");
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		HttpResponseEncoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());

		// Provide body
		ByteBuffer in = ByteBuffer.wrap("Hello World!".getBytes("ascii"));
		result = encoder.encode(in, out);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		result = encoder.encode(null, out);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
		assertTrue(encoded.contains("Content-Length: 12\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\nHello World!"));
	}

}
