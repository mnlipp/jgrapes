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
import java.util.Iterator;

import org.jdrupes.httpcodec.fields.HttpStringListField;
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
		assertEquals("How, are, you, \"out there\"", fv.valueToString());;
	}

}
