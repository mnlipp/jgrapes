/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.text.ParseException;

import org.jdrupes.httpcodec.HttpFieldValue;
import org.jdrupes.httpcodec.HttpStringFieldValue;
import org.jdrupes.httpcodec.HttpListFieldValue;
import org.jdrupes.httpcodec.HttpMediaTypeFieldValue;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class FieldParsingTests {

	@Test
	public void testString() throws ParseException {
		HttpFieldValue fv = new HttpStringFieldValue("Hello");
		assertEquals("Hello", fv.asString());
	}

	@Test
	public void testStringList() throws ParseException {
		HttpListFieldValue fv = new HttpListFieldValue(
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
		HttpListFieldValue fv = new HttpListFieldValue
				("\"How \\\"are\",you,  \"out, there\"");
		assertEquals("How \"are", fv.get(0));
		assertEquals("you", fv.get(1));
		assertEquals("out, there", fv.get(2));
		assertEquals(3, fv.size());
	}

	@Test
	public void testUnquote() throws ParseException {
		HttpFieldValue fv = new HttpStringFieldValue("How are you?");
		assertEquals("How are you?", fv.unquote());
		fv = new HttpStringFieldValue("\"How \\\"are\"");
		assertEquals("How \"are", fv.unquote());
	}
	
	@Test
	public void testMediaType() throws ParseException {
		HttpMediaTypeFieldValue 
			mt = new HttpMediaTypeFieldValue("text/html;charset=utf-8");
		assertEquals("text/html;charset=utf-8", mt.asString());
		mt = new HttpMediaTypeFieldValue("text/html;charset=UTF-8");
		assertEquals("text/html;charset=utf-8", mt.asString());
		mt = new HttpMediaTypeFieldValue("Text/HTML;Charset=\"utf-8\"");
		assertEquals("text/html;charset=utf-8", mt.asString());
		mt = new HttpMediaTypeFieldValue("text/html; charset=\"utf-8\"");
		assertEquals("text/html;charset=utf-8", mt.asString());
	}
}
