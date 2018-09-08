/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.test.basic;

import java.util.HashMap;
import java.util.Map;

import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class ComponentsTest {

	public static class DerivedChannel extends ClassChannel {
	}
	
	public static class DerivedDerivedChannel extends DerivedChannel {
	}
	
	@Test
	public void testChannels() {
		ClassChannel derived = new DerivedChannel();
		ClassChannel derivedDerived = new DerivedDerivedChannel();
		
		assertTrue(derivedDerived.isEligibleFor(DerivedChannel.class));
		assertTrue(!derived.isEligibleFor(DerivedDerivedChannel.class));
		assertTrue(derived.isEligibleFor(ClassChannel.BROADCAST.defaultCriterion()));
		assertTrue(derivedDerived.isEligibleFor(ClassChannel.BROADCAST.defaultCriterion()));
		assertTrue(ClassChannel.BROADCAST.isEligibleFor(DerivedChannel.class));
		assertTrue(ClassChannel.BROADCAST.isEligibleFor(DerivedDerivedChannel.class));
	}

	@Test
	public void testMapOf() {
		Map<String,Integer> map = Components.mapOf(
				"Value1", 1,
				"Value2", 2,
				"Value3", 3,
				"Value4", 4,
				"Value5", 5,
				"Value6", 6,
				"Value7", 7,
				"Value8", 8,
				"Value9", 9,
				"Value10", 10);
		assertEquals(1, map.get("Value1").intValue());
		assertEquals(2, map.get("Value2").intValue());
		assertEquals(3, map.get("Value3").intValue());
		assertEquals(4, map.get("Value4").intValue());
		assertEquals(5, map.get("Value5").intValue());
		assertEquals(6, map.get("Value6").intValue());
		assertEquals(7, map.get("Value7").intValue());
		assertEquals(8, map.get("Value8").intValue());
		assertEquals(9, map.get("Value9").intValue());
		assertEquals(10, map.get("Value10").intValue());
	}
	
	@Test
	public void testMapPut() {
		Map<String, Integer> map 
			= Components.put(Components.put(Components.put(new HashMap<>(),
					"Value1", 1),
					"Value2", 2),
					"Value3", 3);
		assertEquals(1, map.get("Value1").intValue());
		assertEquals(2, map.get("Value2").intValue());
		assertEquals(3, map.get("Value3").intValue());
	}
}
