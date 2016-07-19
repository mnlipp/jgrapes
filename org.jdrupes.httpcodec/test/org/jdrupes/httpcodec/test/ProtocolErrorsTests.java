package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.DecoderResult;
import org.jdrupes.httpcodec.HttpRequestDecoder;
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
		DecoderResult result = decoder.decode(buffer);
		assertFalse(result.hasRequest());
		assertFalse(result.hasPayloadBytes());
		assertFalse(result.hasPayloadChars());
		assertTrue(result.hasResponse());
		assertEquals(505, result.getResponse().getStatusCode());
	}

}
