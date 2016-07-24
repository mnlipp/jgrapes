package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.internal.Codec.HttpStatus;
import org.junit.Test;

public class ResponseDecoderTests {

	/**
	 * Response with body determined by length.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyLength()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Vary: Accept-Encoding\r\n"
				+ "Content-Encoding: gzip\r\n"
				+ "Content-Length: 12\r\n"
				+ "Keep-Alive: timeout=5, max=100\r\n"
				+ "Connection: Keep-Alive\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpResponseDecoder.Result result = decoder.decode(buffer, body);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body terminated by close.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyClosed()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Vary: Accept-Encoding\r\n"
				+ "Content-Encoding: gzip\r\n"
				+ "Keep-Alive: timeout=5, max=100\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpResponseDecoder.Result result = decoder.decode(buffer, body);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		// Close
		result = decoder.decode(null, body);
		assertFalse(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

}
