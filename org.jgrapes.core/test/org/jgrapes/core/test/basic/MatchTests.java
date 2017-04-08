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

package org.jgrapes.core.test.basic;

import org.jgrapes.core.ClassChannel;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 */
public class MatchTests {

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

}
