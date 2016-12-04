package org.jdrupes.httpcodec.test.http;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Map;

import org.jdrupes.httpcodec.util.FormUrlDecoder;
import org.junit.Test;

public class FormUrlDecoderTests {

	@Test
	public void test() throws UnsupportedEncodingException {
		FormUrlDecoder decoder = new FormUrlDecoder();
		ByteBuffer in = ByteBuffer.wrap(("first=" + URLEncoder
		        .encode("ValueÄ", "utf-8") + "&second=Val").getBytes("utf-8"));
		decoder.addData(in);
		in = ByteBuffer.wrap((URLEncoder.encode("ueÖ", "utf-8") + "&third="
		        + URLEncoder.encode("ValueÜ", "utf-8"))
		                .getBytes("utf-8"));
		decoder.addData(in);
		Map<String, String> fields = decoder.getFields();
		assertEquals(3, fields.size());
		assertEquals("ValueÄ", fields.get("first"));
		assertEquals("ValueÖ", fields.get("second"));
		assertEquals("ValueÜ", fields.get("third"));
	}

}
