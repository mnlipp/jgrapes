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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;

/**
 * Represents a complte HTTP request with all received header data.
 * 
 * @author Michael N. Lipp
 */
public class HttpRequest {

	public static final URI ASTERISK_REQUEST 
		= createUri("http://127.0.0.1/");
	private static URI createUri(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private HttpProtocol httpProtocol;
	private String method;
	private URI requestUri;
	private Map<String,String> headers = new HashMap<>();
	
	/**
	 * Creates a new request with basic data.
	 * 
	 * @param method the method
	 * @param requestUri the requested resource
	 * @param httpProtocol the HTTP protocol version
	 */
	public HttpRequest(String method, URI requestUri, 
			HttpProtocol httpProtocol) {
		super();
		this.method = method;
		this.requestUri = requestUri;
		this.httpProtocol = httpProtocol;
	}

	/**
	 * Return the method.
	 * 
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Return the URI of the requested resource.
	 * 
	 * @return the requestUri
	 */
	public URI getRequestUri() {
		return requestUri;
	}

	/**
	 * Return the protocol.
	 * 
	 * @return the HTTP protocol
	 */
	public HttpProtocol getProtocol() {
		return httpProtocol;
	}

	/**
	 * Add another header to the request data.
	 * 
	 * @param key the header's key
	 * @param value the header's value
	 */
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
