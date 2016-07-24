package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.HttpResponseEncoder;
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

}
