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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.fields.HttpField;

/**
 * Represents an HTTP request header.
 * 
 * @author Michael N. Lipp
 */
public class HttpRequest extends HttpMessageHeader {

	public static final URI ASTERISK_REQUEST 
		= createUri("http://127.0.0.1/");
	private static URI createUri(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private String method;
	private URI requestUri;
	private String host;
	private int port;
	private HttpResponse response;
	
	/**
	 * Creates a new request with basic data. 
	 * 
	 * @param method the method
	 * @param requestUri the requested resource
	 * @param httpProtocol the HTTP protocol version
	 * @param messageHasBody indicates that the message has a body
	 */
	public HttpRequest(String method, URI requestUri, 
			HttpProtocol httpProtocol, boolean messageHasBody) {
		super(httpProtocol, messageHasBody);
		this.method = method;
		this.requestUri = requestUri;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpMessageHeader#setField(org.jdrupes.httpcodec.fields.HttpField)
	 */
	@Override
	public HttpRequest setField(HttpField<?> value) {
		super.setField(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpMessageHeader#setMessageHasBody(boolean)
	 */
	@Override
	public HttpRequest setMessageHasBody(boolean messageHasBody) {
		super.setMessageHasBody(messageHasBody);
		return this;
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
	 * Set the host and port attributes.
	 * 
	 * @param host the host
	 * @param port the port
	 * @return the request for easy chaining
	 */
	public HttpRequest setHostAndPort (String host, int port) {
		this.host = host;
		this.port = port;
		return this;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Associates the request with a response. This method is
	 * invoked by the request decoder that initializes the response with
	 * basic information that can be derived from the request 
	 * (e.g. by default the HTTP version is copied). The status code
	 * of the preliminary response is 501 "Not implemented".
	 * <P>
	 * Although not strictly required, users of the API are encouraged to 
	 * modify this prepared request and use it when building the response.
	 *  
	 * @param response the prepared response
	 * @return the request for easy chaining
	 */
	public HttpRequest setResponse(HttpResponse response) {
		this.response = response;
		return this;
	}
	
	/**
	 * Returns the prepared response.
	 * 
	 * @return the prepared response
	 * @see #setResponse(HttpResponse)
	 */
	public Optional<HttpResponse> getResponse() {
		return Optional.ofNullable(response);
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
		if (getProtocol() != null) {
			builder.append("httpVersion=");
			builder.append(getProtocol());
		}
		builder.append("]");
		return builder.toString();
	}
	

}
