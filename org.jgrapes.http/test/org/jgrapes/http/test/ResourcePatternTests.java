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

package org.jgrapes.http.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.jgrapes.http.ResourcePattern;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class ResourcePatternTests {

	@Test
	public void testWildcards1() throws URISyntaxException, ParseException {
		URI request = new URI(
				"http", null, "localhost", 80, "/test", null, null);
		assertTrue(ResourcePattern.matches("/test", request));
		assertTrue(ResourcePattern.matches("*/test", request));
		assertTrue(ResourcePattern.matches("*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/**", request));
		assertTrue(ResourcePattern.matches("http://*:*/test", request));
		assertTrue(ResourcePattern.matches("http,https://*:*/test", request));
		assertFalse(ResourcePattern.matches("https://*:*/test", request));
		assertTrue(ResourcePattern.matches("*://localhost/test", request));
		assertFalse(ResourcePattern.matches("*://otherhost/test", request));
		assertTrue(ResourcePattern.matches("localhost:*/test", request));
		assertFalse(ResourcePattern.matches("otherhost:*/test", request));
		assertTrue(ResourcePattern.matches("*:80/test", request));
		assertFalse(ResourcePattern.matches("*:8080/test", request));
	}

	@Test
	public void testWildcards2() throws URISyntaxException, ParseException {
		URI request = new URI(
				null, null, "localhost", 80, "/test", null, null);
		assertTrue(ResourcePattern.matches("/test", request));
		assertTrue(ResourcePattern.matches("*/test", request));
		assertTrue(ResourcePattern.matches("*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/**", request));
		assertFalse(ResourcePattern.matches("http://*:*/test", request));
		assertFalse(ResourcePattern.matches("https://*:*/test", request));
	}

	@Test
	public void testWildcards3() throws URISyntaxException, ParseException {
		URI request = new URI(
				null, null, null, 80, "/test", null, null);
		assertTrue(ResourcePattern.matches("/test", request));
		assertTrue(ResourcePattern.matches("*/test", request));
		assertTrue(ResourcePattern.matches("*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/**", request));
		assertFalse(ResourcePattern.matches("*://localhost/test", request));
		assertFalse(ResourcePattern.matches("*://localhost:*/test", request));
	}

	@Test
	public void testWildcards4() throws URISyntaxException, ParseException {
		URI request = new URI(
				null, null, null, -1, "/test", null, null);
		assertTrue(ResourcePattern.matches("/test", request));
		assertTrue(ResourcePattern.matches("*/test", request));
		assertTrue(ResourcePattern.matches("*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/test", request));
		assertTrue(ResourcePattern.matches("*://*:*/**", request));
		assertFalse(ResourcePattern.matches("*://*:80/test", request));
	}

	@Test
	public void testPath1() throws URISyntaxException, ParseException {
		URI request = new URI(
				null, null, null, -1, "/test", null, null);
		assertTrue(ResourcePattern.matches("/test", request));
		assertFalse(ResourcePattern.matches("/test/", request));
		assertFalse(ResourcePattern.matches("/", request));
		assertTrue(ResourcePattern.matches("/*", request));
		assertTrue(ResourcePattern.matches("/**", request));
		assertFalse(ResourcePattern.matches("/test/*", request));
		assertFalse(ResourcePattern.matches("/test/**", request));
		assertFalse(ResourcePattern.matches("/test1/**", request));
		assertTrue(ResourcePattern.matches("/test,/test/**", request));
	}

	@Test
	public void testPath2() throws URISyntaxException, ParseException {
		URI request = new URI(
				null, null, null, -1, "/test/", null, null);
		assertFalse(ResourcePattern.matches("/test", request));
		assertTrue(ResourcePattern.matches("/test/", request));
		assertFalse(ResourcePattern.matches("/", request));
		assertFalse(ResourcePattern.matches("/*", request));
		assertTrue(ResourcePattern.matches("/**", request));
		assertTrue(ResourcePattern.matches("/test/*", request));
		assertTrue(ResourcePattern.matches("/test/**", request));
		assertFalse(ResourcePattern.matches("/test1/**", request));
		assertTrue(ResourcePattern.matches("/test,/test/**", request));
	}

	@Test
	public void testPath3() throws URISyntaxException, ParseException {
		URI request = new URI(
				null, null, null, -1, "/test/Test.html", null, null);
		assertTrue(ResourcePattern.matches("/test/Test.html", request));
		assertFalse(ResourcePattern.matches("/", request));
		assertFalse(ResourcePattern.matches("/*", request));
		assertTrue(ResourcePattern.matches("/**", request));
		assertTrue(ResourcePattern.matches("/test/*", request));
		assertTrue(ResourcePattern.matches("/test/**", request));
	}

	@Test
	public void testPathSep() throws URISyntaxException, ParseException {
		URI request = URI.create("/test/Test.html");
		assertEquals(0, new ResourcePattern("/test/Test.html").matches(request));
		assertEquals(1, new ResourcePattern("/test|Test.html").matches(request));
		assertEquals(0, new ResourcePattern("|test/Test.html").matches(request));
		assertEquals(-1, new ResourcePattern("/test").matches(request));
		assertEquals(1, new ResourcePattern("/test|*").matches(request));
		request = URI.create("/test");
		assertEquals(1, new ResourcePattern("/test|").matches(request));
		assertEquals(0, new ResourcePattern("/test,/test|").matches(request));
		request = URI.create("/test/");
		assertEquals(1, new ResourcePattern("/test|").matches(request));
		assertEquals(1, new ResourcePattern("/test,/test|").matches(request));
	}

	@Test 
	public void testRemoveSegments() {
		assertEquals("/prefix/and/rest", ResourcePattern.removeSegments(
				"/prefix/and/rest", 0));
		assertEquals("prefix/and/rest", ResourcePattern.removeSegments(
				"/prefix/and/rest", 1));
		assertEquals("rest", ResourcePattern.removeSegments(
				"/prefix/and/rest", 3));
	}

	@Test 
	public void testSplit() {
		String[] result = ResourcePattern.split("/prefix/and/rest", 0);
		assertEquals("", result[0]);
		assertEquals("/prefix/and/rest", result[1]);
		result = ResourcePattern.split("/prefix/and/rest", 1);
		assertEquals("", result[0]);
		assertEquals("prefix/and/rest", result[1]);
		result = ResourcePattern.split("/prefix/and/rest", 3);
		assertEquals("/prefix/and", result[0]);
		assertEquals("rest", result[1]);
		result = ResourcePattern.split("/prefix/and/rest/", 3);
		assertEquals("/prefix/and", result[0]);
		assertEquals("rest/", result[1]);
		result = ResourcePattern.split("/prefix/and/rest", 4);
		assertEquals("/prefix/and/rest", result[0]);
		assertEquals("", result[1]);
		result = ResourcePattern.split("/prefix/and/rest/", 4);
		assertEquals("/prefix/and/rest", result[0]);
		assertEquals("", result[1]);
	}
	
	@Test 
	public void testSplitResource() throws ParseException {
		ResourcePattern pattern = new ResourcePattern("|prefix/and/**");
		String[] result = pattern.splitPath(URI.create("/prefix/and/rest")).get();
		assertEquals("", result[0]);
		assertEquals("prefix/and/rest", result[1]);
		pattern = new ResourcePattern("/prefix|and/**");
		result = pattern.splitPath(URI.create("/prefix/and/rest")).get();
		assertEquals("/prefix", result[0]);
		assertEquals("and/rest", result[1]);
	}
	
	@Test 
	public void testRemainder() throws ParseException {
		ResourcePattern pattern = new ResourcePattern("|prefix/and/**");
		String result = pattern.pathRemainder(URI.create("/prefix/and/rest")).get();
		assertEquals("prefix/and/rest", result);
		pattern = new ResourcePattern("/prefix|and/**");
		result = pattern.pathRemainder(URI.create("/prefix/and/rest")).get();
		assertEquals("and/rest", result);
	}
}
