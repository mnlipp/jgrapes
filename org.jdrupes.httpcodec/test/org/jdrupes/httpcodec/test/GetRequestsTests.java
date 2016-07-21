package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.server.HttpRequestDecoder;
import org.junit.Test;

public class GetRequestsTests {

	@Test
	public void testGetRequest() throws UnsupportedEncodingException {
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: localhost:8888\r\n"
			+ "Connection: keep-alive\r\n"
			+ "Upgrade-Insecure-Requests: 1\r\n"
			+ "User-Agent: JUnit\r\n"
			+ "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,"
			+ "image/webp,*/*;q=0.8\r\n"
			+ "Accept-Encoding: gzip, deflate, sdch\r\n"
			+ "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
			+ "Cookie: _test.; gsScrollPos=\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("GET", decoder.getHeader().getMethod());
		assertEquals("/test",
		        decoder.getHeader().getRequestUri().getPath());
	}

}
