package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.server.HttpRequestDecoder;
import org.junit.Test;

public class PostRequestsTests {

	@Test
	public void testPostRequestInterrupted()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Cache-Control: max-age=0\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Upgrade-Insecure-Requests: 1\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastname=Grapes";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("POST", decoder.getHeader().getMethod());
		assertEquals("/form",
		        decoder.getHeader().getRequestUri().getPath());
		assertTrue(result.isOverflow());
		assertFalse(result.isUnderflow());
		// Get body
		ByteBuffer body = ByteBuffer.allocate(1024);
		result = decoder.decode(buffer, body);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(result.getCloseConnection());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	@Test
	public void testPostAtOnce()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Cache-Control: max-age=0\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Upgrade-Insecure-Requests: 1\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastname=Grapes";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("POST", decoder.getHeader().getMethod());
		assertEquals("/form",
		        decoder.getHeader().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	@Test
	public void testPostSplitOut()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Cache-Control: max-age=0\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Upgrade-Insecure-Requests: 1\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastname=Grapes";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(20);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("POST", decoder.getHeader().getMethod());
		assertEquals("/form",
		        decoder.getHeader().getRequestUri().getPath());
		assertTrue(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastnam", bodyText);
		// Remaining
		body.clear();
		result = decoder.decode(buffer, body);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(result.getCloseConnection());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(buffer.hasRemaining());
		body.flip();
		bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("e=Grapes", bodyText);
	}

	@Test
	public void testPostSplitIn()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Content-Length: 28\r\n"
		        + "Cache-Control: max-age=0\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Upgrade-Insecure-Requests: 1\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "firstname=J.&lastnam";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("POST", decoder.getHeader().getMethod());
		assertEquals("/form",
		        decoder.getHeader().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		// Rest
		buffer = ByteBuffer.wrap("e=Grapes".getBytes("ascii"));
		result = decoder.decode(buffer, body);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertFalse(result.getCloseConnection());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

	@Test
	public void testPostChunked()
	        throws UnsupportedEncodingException {
		String reqText = "POST /form HTTP/1.1\r\n"
		        + "Host: localhost:8888\r\n"
		        + "Connection: keep-alive\r\n"
		        + "Transfer-Encoding: chunked\r\n"
		        + "Cache-Control: max-age=0\r\n"
		        + "Origin: http://localhost:8888\r\n"
		        + "Upgrade-Insecure-Requests: 1\r\n"
		        + "Content-Type: application/x-www-form-urlencoded\r\n"
		        + "Referer: http://localhost:8888/form\r\n"
		        + "Accept-Encoding: gzip, deflate\r\n"
		        + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
		        + "\r\n"
		        + "14;dummy=0\r\n"
		        + "firstname=J.&lastnam\r\n"
		        + "8\r\n"
		        + "e=Grapes\r\n"
		        + "0\r\n"
		        + "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		HttpRequestDecoder.Result result = decoder.decode(buffer, body);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.hasResponse());
		assertTrue(decoder.getHeader().messageHasBody());
		assertFalse(result.getCloseConnection());
		assertEquals("POST", decoder.getHeader().getMethod());
		assertEquals("/form",
		        decoder.getHeader().getRequestUri().getPath());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(!buffer.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("firstname=J.&lastname=Grapes", bodyText);
	}

}
