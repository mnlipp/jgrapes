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
import java.util.Iterator;

import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class FieldValuesTests {

	@Test
	public void testStringList() throws ParseException {
		HttpStringListField fv = HttpStringListField.fromString("Test", 
		        "How, are,you,  \"out there\"");
		Iterator<String> iter = fv.iterator();
		assertEquals("How", iter.next());
		assertEquals("are", iter.next());
		assertEquals("you", iter.next());
		assertEquals("out there", iter.next());
		assertFalse(iter.hasNext());
		assertEquals("How, are, you, \"out there\"", fv.asFieldValue());;
	}

	@Test
	public void testClone() throws ParseException {
		HttpStringListField field = HttpStringListField.fromString("Test",
				"one, two, three");
		HttpStringListField field2 = field.clone();
		field2.removeIgnoreCase("Two");
		assertTrue(field.size() == 3);
		assertTrue(field2.size() == 2);
		assertTrue(field2.containsIgnoreCase("one"));
		assertTrue(field2.containsIgnoreCase("three"));
	}
}
