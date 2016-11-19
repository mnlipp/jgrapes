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
package org.jdrupes.httpcodec.internal;

import org.jdrupes.httpcodec.HttpConstants;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;

/**
 * @author Michael N. Lipp
 *
 */
public class Codec<T extends MessageHeader> implements HttpConstants {

	protected T messageHeader = null;
	
	protected String bodyCharset() {
		HttpMediaTypeField contentType = messageHeader
		        .getField(HttpMediaTypeField.class, HttpField.CONTENT_TYPE);
		if (contentType == null
				|| contentType.getParameter("charset") == null) {
			return "utf-8";
		}
		return contentType.getParameter("charset");
	}
	
}
