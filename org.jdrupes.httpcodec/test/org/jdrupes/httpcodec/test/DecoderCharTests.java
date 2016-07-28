package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.HttpCodec;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.HttpResponseDecoder;
import org.junit.Test;

public class DecoderCharTests {

	/**
	 * Response with body terminated by close.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyCollected()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Vary: Accept-Encoding\r\n"
				+ "Content-Encoding: gzip\r\n"
				+ "Keep-Alive: timeout=5, max=100\r\n"
				+ "Content-Type: text/plain; charset=utf-8\r\n"
				+ "\r\n"
				+ "ÄÖÜaöüß";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		CharBuffer body = CharBuffer.allocate(1024);
//		HttpResponseDecoder.Result result = decoder.decode(buffer, body);
//		assertTrue(result.isHeaderCompleted());
//		assertTrue(decoder.getHeader().messageHasBody());
//		assertFalse(result.getCloseConnection());
//		assertEquals(HttpStatus.OK.getStatusCode(),
//		        decoder.getHeader().getStatusCode());
//		assertFalse(result.isOverflow());
//		assertTrue(result.isUnderflow());
//		assertFalse(buffer.hasRemaining());
//		// Close
//		result = decoder.decode(HttpCodec.END_OF_BODY, body);
//		assertFalse(result.isHeaderCompleted());
//		assertTrue(decoder.getHeader().messageHasBody());
//		assertTrue(result.getCloseConnection());
//		assertEquals(HttpStatus.OK.getStatusCode(),
//		        decoder.getHeader().getStatusCode());
//		assertFalse(result.isOverflow());
//		assertFalse(result.isUnderflow());
//		assertFalse(buffer.hasRemaining());
//		body.flip();
//		String bodyText = new String(body.array(), body.position(),
//		        body.limit());
//		assertEquals("Hello World!", bodyText);
	}

}
