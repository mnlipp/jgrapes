package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.ResponseEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.junit.Test;

/**
 * Tests focusing on encoding the response header.
 * 
 * @author Michael N. Lipp
 */
public class ResponseEncoderTests {

	@Test
	public void testSimpleResponse() {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ResponseEncoder.Result result = encoder.encode(out);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.getCloseConnection());
		String encoded = new String(out.array(), 0, out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
	}

	@Test
	public void testSimpleResponseTinyOut() {
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
		        HttpStatus.OK, false);
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		encoder.encode(response);
		ByteBuffer out = ByteBuffer.allocate(1024*1024);
		ByteBuffer tinyOut = ByteBuffer.allocate(1);
		while (true) {
			ResponseEncoder.Result result = encoder.encode(tinyOut);
			assertFalse(result.isUnderflow());
			assertFalse(result.getCloseConnection());
			tinyOut.flip();
			if (tinyOut.hasRemaining()) {
				out.put(tinyOut);
				tinyOut.compact();
			}
			if (!result.isOverflow()) {
				break;
			}
		}
		String encoded = new String(out.array(), out.arrayOffset(),
		        out.position());
		assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
		assertTrue(encoded.endsWith("\r\n\r\n"));
	}

}
