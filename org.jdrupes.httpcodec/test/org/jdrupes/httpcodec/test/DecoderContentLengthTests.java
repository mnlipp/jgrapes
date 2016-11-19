package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpSetCookieListField;
import org.junit.Test;

public class DecoderContentLengthTests {

	/**
	 * Response with body determined by length.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyLengthAtOnce()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
				+ "Content-Length: 12\r\n"
				+ "Keep-Alive: timeout=5, max=100\r\n"
				+ "Connection: Keep-Alive\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpResponseDecoder.Result result = decoder.decode(in, body, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body determined by length (first header then body).
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodySeparatePhases()
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
				+ "set-cookie:autorf=deleted; "
				+ "expires=Sun, 26-Jul-2015 12:32:17 GMT; "
				+ "path=/; domain=www.test.com\r\n"
				+ "Set-Cookie:MUIDB=13BEF4C6DC68E5; path=/; "
				+ "httponly; expires=Wed, 25-Jul-2018 12:42:14 GMT\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		HttpResponseDecoder.Result result = decoder.decode(in, null, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertTrue(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(in.hasRemaining());
		ByteBuffer body = ByteBuffer.allocate(1024);
		// Decode body
		result = decoder.decode(in, body, false);
		assertFalse(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		// Check result
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
		// Set-Cookies
		HttpSetCookieListField field = decoder.getHeader()
		        .getField(HttpSetCookieListField.class, HttpField.SET_COOKIE);
		assertEquals(2, field.size());
		assertEquals("deleted", field.valueForName("autorf"));
		assertEquals("13BEF4C6DC68E5", field.valueForName("MUIDB"));
	}

	/**
	 * Response with body determined by length.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyLengthTiny()
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
				+ "set-cookie:autorf=deleted; "
				+ "expires=Sun, 26-Jul-2015 12:32:17 GMT; "
				+ "path=/; domain=www.test.com\r\n"
				+ "Set-Cookie:MUIDB=13BEF4C6DC68E5; path=/; "
				+ "httponly; expires=Wed, 25-Jul-2018 12:42:14 GMT\r\n"
				+ "\r\n"
				+ "Hello World!";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder(null);
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpResponseDecoder.Result result = Common.tinyDecodeLoop(decoder, in,
		        body);
		assertTrue(decoder.getHeader().messageHasBody());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().getStatusCode());
		assertFalse(result.getCloseConnection());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
		// Set-Cookies
		HttpSetCookieListField field = decoder.getHeader()
		        .getField(HttpSetCookieListField.class, HttpField.SET_COOKIE);
		assertEquals(2, field.size());
		assertEquals("deleted", field.valueForName("autorf"));
		assertEquals("13BEF4C6DC68E5", field.valueForName("MUIDB"));
	}


}
