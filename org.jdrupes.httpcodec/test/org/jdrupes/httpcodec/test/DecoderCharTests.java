package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
				+ "ÄÖÜaöüß€.";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("utf-8"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		CharBuffer body = CharBuffer.allocate(1024);
		HttpResponseDecoder.Result result = decoder.decode(buffer, body, true);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		assertEquals("ÄÖÜaöüß€.", body.toString());
	}

	/**
	 * Response with body terminated by close and small output buffer.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyCollectedTinyOut()
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
				+ "ÄÖÜaöüß€.";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("utf-8"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		CharBuffer body = CharBuffer.allocate(1);
		HttpResponseDecoder.Result result = decoder.decode(buffer, body, true);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isUnderflow());
		assertTrue(result.isOverflow());
		assertTrue(buffer.hasRemaining());
		StringBuilder bodyText = new StringBuilder();
		while (true) {
			body.flip();
			bodyText.append(body.toString());
			if (!result.isOverflow()) {
				assertTrue(result.getCloseConnection());
				break;
			}
			body.clear();
			result = decoder.decode(buffer, body, true);
		}
		assertEquals("ÄÖÜaöüß€.", bodyText.toString());
	}

	/**
	 * Response with body terminated by close and small output buffer.
	 * 
	 * @throws ProtocolException 
	 * @throws IOException 
	 */
	@Test
	public void testWithBodyCollectedTinyIn()
	        throws ProtocolException, IOException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Vary: Accept-Encoding\r\n"
				+ "Content-Encoding: gzip\r\n"
				+ "Keep-Alive: timeout=5, max=100\r\n"
				+ "Content-Type: text/plain; charset=utf-8\r\n"
				+ "\r\n"
				+ "ÄÖÜaöüß€.";
		InputStream is = new ByteArrayInputStream(reqText.getBytes("utf-8"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		CharBuffer body = CharBuffer.allocate(1024);
		byte[] ba = new byte[1];
		HttpResponseDecoder.Result result;
		while (true) {
			int b = is.read();
			if (b == -1) {
				break;
			}
			ba[0] = (byte)b;
			ByteBuffer buffer = ByteBuffer.wrap(ba);
			result = decoder.decode(buffer, body, false);
			assertTrue(result.isUnderflow());
		}
		result = decoder.decode(HttpCodec.EMPTY_IN, body, true);
		assertTrue(decoder.getHeader().messageHasBody());
		assertTrue(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		body.flip();
		assertEquals("ÄÖÜaöüß€.", body.toString());
	}

}
