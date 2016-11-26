package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpSetCookieListField;
import org.junit.Test;

public class ResponseDecoderTests {

	/**
	 * Response with body determined by length.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws HttpProtocolException 
	 */
	@Test
	public void testSetCookie()
	        throws UnsupportedEncodingException, HttpProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Accept-Ranges: bytes\r\n"
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
		ResponseDecoder.Result result = decoder.decode(in, body, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.getHeader().get().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals(HttpStatus.OK.getStatusCode(),
		        decoder.getHeader().get().getStatusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
		// Set-Cookies
		Optional<HttpSetCookieListField> field = decoder.getHeader()
				.flatMap(h -> h.getField
						(HttpSetCookieListField.class, HttpField.SET_COOKIE));
		assertEquals(2, field.get().size());
		assertEquals("deleted", field.get().valueForName("autorf").get());
		assertEquals("13BEF4C6DC68E5", field.get().valueForName("MUIDB").get());
	}

}
