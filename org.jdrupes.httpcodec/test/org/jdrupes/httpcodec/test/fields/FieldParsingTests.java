/*******************************************************************************
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.jdrupes.httpcodec.test.fields;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.jdrupes.httpcodec.protocols.http.fields.HttpDateField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class FieldParsingTests {

	@Test
	public void testString() throws ParseException {
		HttpField<?> fv = HttpStringField.fromString("Test", "Hello");
		assertEquals("Hello", fv.getValue());
	}

	@Test
	public void testStringList() throws ParseException {
		HttpStringListField fv = HttpStringListField.fromString("Test",
		        "How, are,you,  out, there");
		assertEquals("How", fv.get(0));
		assertEquals("are", fv.get(1));
		assertEquals("you", fv.get(2));
		assertEquals("out", fv.get(3));
		assertEquals("there", fv.get(4));
		assertEquals(5, fv.size());
	}

	@Test
	public void testQuoted() throws ParseException {
		HttpStringListField fv = HttpStringListField.fromString("Test",
				"\"How \\\"are\",you,  \"out, there\"");
		assertEquals("How \"are", fv.get(0));
		assertEquals("you", fv.get(1));
		assertEquals("out, there", fv.get(2));
		assertEquals(3, fv.size());
	}

	@Test
	public void testUnquote() throws ParseException {
		HttpField<?> fv = HttpStringField.fromString("Test", "How are you?");
		assertEquals("How are you?", fv.getValue());
		fv = HttpStringField.fromString("Test", "\"How \\\"are\"");
		assertEquals("How \"are", fv.getValue());
	}
	
	@Test
	public void testMediaType() throws ParseException {
		HttpMediaTypeField mt = HttpMediaTypeField.fromString("Test",
		        "text/html;charset=utf-8");
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
		mt = HttpMediaTypeField.fromString("Test",
		        "Text/HTML;Charset=\"utf-8\"");
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
		mt = HttpMediaTypeField.fromString("Test",
		        "text/html; charset=\"utf-8\"");
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
	}
	
	@Test
	public void testParseDateType() throws ParseException {
		String dateTime = "Tue, 15 Nov 1994 08:12:31 GMT";
		HttpDateField field = HttpDateField.fromString("Date", dateTime);
		ZonedDateTime value = field.getValue().atZone(ZoneId.of("GMT"));
		assertEquals(15, value.getDayOfMonth());
		assertEquals(Month.NOVEMBER, value.getMonth());
		assertEquals(1994, value.getYear());
		assertEquals(8, value.getHour());
		assertEquals(12, value.getMinute());
		assertEquals(31, value.getSecond());
		HttpDateField back = new HttpDateField("Date", value.toInstant());
		assertEquals(dateTime, back.asFieldValue());
	}
	
	@Test
	public void testIntFromString() throws ParseException {
		HttpIntField field = HttpIntField.fromString("test", "42");
		assertEquals(42, field.getValue().longValue());
	}
}
