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
package org.jdrupes.httpcodec.client;

import java.io.IOException;
import java.io.Writer;

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.internal.Encoder;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpRequestEncoder extends Encoder<HttpRequest> {

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#startMessage(org.jdrupes.httpcodec.internal.MessageHeader, java.io.Writer)
	 */
	@Override
	protected void startMessage(HttpRequest messageHeader, Writer writer)
	        throws IOException {
		writer.write(messageHeader.getMethod());
		writer.write(" ");
		writer.write(messageHeader.getRequestUri().toString());
		writer.write(" ");
		writer.write(messageHeader.getProtocol().toString());
		writer.write("\r\n");
	}


}
