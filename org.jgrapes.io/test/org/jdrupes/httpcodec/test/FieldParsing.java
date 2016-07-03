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

import org.jdrupes.httpcodec.util.HttpFieldValue;
import org.jdrupes.httpcodec.util.HttpStringFieldValue;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class FieldParsing {

	@Test
	public void testSingle() throws ParseException {
		HttpFieldValue fv = new HttpStringFieldValue("Hello");
		assertEquals("Hello", fv.nextElement());
		assertNull(fv.nextElement());
	}

	@Test
	public void testSeveral() throws ParseException {
		HttpFieldValue fv = new HttpStringFieldValue(
		        "How, are,you,  out, there");
		assertEquals("How", fv.nextElement());
		assertEquals("are", fv.nextElement());
		assertEquals("you", fv.nextElement());
		assertEquals("out", fv.nextElement());
		assertEquals("there", fv.nextElement());
		assertNull(fv.nextElement());
	}

	@Test
	public void testQuoted() throws ParseException {
		HttpFieldValue fv = new HttpStringFieldValue
				("\"How \\\"are\",you,  \"out, there\"");
		assertEquals("\"How \\\"are\"", fv.nextElement());
		assertEquals("you", fv.nextElement());
		assertEquals("\"out, there\"", fv.nextElement());
		assertNull(fv.nextElement());
	}

	@Test
	public void testUnquote() throws ParseException {
		HttpFieldValue fv = new HttpStringFieldValue("How are you?");
		assertEquals("How are you?", fv.unquote());
		fv = new HttpStringFieldValue("\"How \\\"are\"");
		assertEquals("How \"are", fv.unquote());
	}
}
