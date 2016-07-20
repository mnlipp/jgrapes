package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.server.HttpRequestDecoder;
import org.junit.Test;

public class ProtocolErrorsTests {

	@Test
	public void testVersion() throws UnsupportedEncodingException {
		String reqText 
			= "GET / HTTP/0.8\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer);
		assertFalse(result.hasMessage());
		assertTrue(result.hasResponse());
		assertEquals(505, result.getResponse().getStatusCode());
	}

	/**
	 * RFC 7230 3.1.1
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testRequestWithWhitespace()
	        throws UnsupportedEncodingException {
		String reqText 
			= "GET /tricky/Resource HTTP/1.1 HTTP/1.1\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer);
		assertFalse(result.hasMessage());
		assertTrue(result.hasResponse());
		assertEquals(400, result.getResponse().getStatusCode());
	}

	/**
	 * RFC 7230 3.2.4
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testRequestWithSpaceBeforeColon()
	        throws UnsupportedEncodingException {
		String reqText 
			= "GET / HTTP/1.1\r\n"
			+ "Host : localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer);
		assertFalse(result.hasMessage());
		assertTrue(result.hasResponse());
		assertEquals(400, result.getResponse().getStatusCode());
	}

}
