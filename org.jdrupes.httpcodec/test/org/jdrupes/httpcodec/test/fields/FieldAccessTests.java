package org.jdrupes.httpcodec.test.fields;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpMessageHeader;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.junit.Test;

public class FieldAccessTests {

	@Test
	public void testGetInt() throws URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpStringField("Test", "42"));
		Optional<HttpIntField> field = hdr.getField(HttpIntField.class, "Test");
		assertTrue(field.isPresent());
		assertEquals(42, field.get().asInt());
	}

	@Test
	public void testGetStringList() throws URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpStringField("Test", "one, two"));
		Optional<HttpStringListField> field = hdr
		        .getField(HttpStringListField.class, "Test");
		assertTrue(field.isPresent());
		assertEquals(2, field.get().size());
		assertEquals("one", field.get().get(0));
		assertEquals("two", field.get().get(1));
	}

	@Test
	public void testGetIntList() throws URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpStringField("Test", "1, 2, 3"));
		Optional<HttpIntListField> field = hdr
		        .getField(HttpIntListField.class, "Test");
		assertTrue(field.isPresent());
		assertEquals(3, field.get().size());
		assertEquals(1, field.get().get(0).longValue());
		assertEquals(2, field.get().get(1).longValue());
		assertEquals(3, field.get().get(2).longValue());
	}

}
