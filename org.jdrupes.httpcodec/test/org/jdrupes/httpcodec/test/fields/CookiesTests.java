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

import org.jdrupes.httpcodec.protocols.http.fields.HttpCookieListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpSetCookieListField;
import org.junit.Test;

public class CookiesTests {

	@Test
	public void testCookiesFromString() throws ParseException {
		String header = "ui.portal_session=\"4001d5958e8ad80\"; "
				+ "ui.session=\"425ea9b9241f479f8b134b32ad80\"; "
				+ "_pk_id.10.1fff=95d519dd36c53ec9.; "
				+ "gsScrollPos=";
		HttpCookieListField field = HttpCookieListField.fromString(header);
		assertEquals(4, field.size());
		assertEquals("4001d5958e8ad80", 
					field.valueForName("ui.portal_session").get());
		assertEquals("425ea9b9241f479f8b134b32ad80", 
					field.valueForName("ui.session").get());
		assertEquals("95d519dd36c53ec9.", 
					field.valueForName("_pk_id.10.1fff").get());
		assertEquals("", field.valueForName("gsScrollPos").get());
	}
	
	public void testSetCookieFromString() throws ParseException {
		String header = "set-cookie:autorf=deleted; "
				+ "expires=Sun, 26-Jul-2015 12:32:17 GMT; "
				+ "path=/; domain=www.test.com";
		HttpSetCookieListField field = new HttpSetCookieListField();
		field.addFromString(header);
		header = "Set-Cookie:SRCHUID=V=2&GUID=2853211950;"
				+ " expires=Wed, 25-Jul-2018 12:42:14 GMT; path=/";
		field.addFromString(header);
		header = "Set-Cookie:MUIDB=13BEF4C6DC68E5; path=/; "
				+ "httponly; expires=Wed, 25-Jul-2018 12:42:14 GMT";
		field.addFromString(header);
		assertEquals(3, field.size());
		assertEquals("deleted", field.valueForName("autorf").get());
		assertEquals("V=2&GUID=2853211950", field.valueForName("SRCHUID").get());
		assertEquals("13BEF4C6DC68E5", field.valueForName("MUIDB").get());
	}
}