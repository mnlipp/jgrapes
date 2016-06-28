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
package org.jdrupes.httpcodec;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpRequest {

	private HttpProtocol httpProtocol;
	private String method;
	private URI requestUri;
	private Map<String,String> headers = new HashMap<>();
	
	/**
	 * @param method
	 * @param requestUri
	 * @param httpProtocol
	 */
	public HttpRequest(String method, URI requestUri, 
			HttpProtocol httpProtocol) {
		super();
		this.method = method;
		this.requestUri = requestUri;
		this.httpProtocol = httpProtocol;
	}

	/**
	 * Return the protocol.
	 * 
	 * @return the HTTP protocol
	 */
	public HttpProtocol getProtocol() {
		return httpProtocol;
	}




	void addHeader(String key, String value) {
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HttpRequest [");
		if (method != null) {
			builder.append("method=");
			builder.append(method);
			builder.append(", ");
		}
		if (requestUri != null) {
			builder.append("requestUri=");
			builder.append(requestUri);
			builder.append(", ");
		}
		if (httpProtocol != null) {
			builder.append("httpVersion=");
			builder.append(httpProtocol);
		}
		builder.append("]");
		return builder.toString();
	}
	

}
