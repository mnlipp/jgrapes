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
package org.jdrupes.httpcodec.protocols.http;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpMediaTypeField;

/**
 * @author Michael N. Lipp
 *
 */
public abstract class HttpCodec<T extends HttpMessageHeader>
	implements Codec, HttpConstants {

	protected T messageHeader = null;
	
	protected String bodyCharset() {
		return messageHeader
			.getField(HttpMediaTypeField.class, HttpField.CONTENT_TYPE)
			.map(f -> f.getParameter("charset")).orElse("utf-8");
	}
	
}
