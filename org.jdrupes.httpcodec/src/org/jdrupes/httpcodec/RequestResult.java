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

/**
 * @author Michael N. Lipp
 *
 */
public class RequestResult extends DecoderResult<HttpRequest> {

	private HttpResponse response;

	/**
	 * @param request
	 * @param payloadBytes
	 * @param payloadChars
	 * @param response
	 * @param closeConnection
	 */
	public RequestResult(HttpRequest request, boolean payloadBytes,
	        boolean payloadChars, HttpResponse response,
	        boolean closeConnection) {
		super(request, payloadBytes, payloadChars, closeConnection);
		this.response = response;
	}

	/**
	 * @return the decoded message as request
	 */
	public HttpRequest getMessage() {
		return (HttpRequest)super.getMessage();
	}

	
	/**
	 * Returns {@code true} if the result includes a response. A response in
	 * the decoder result indicates that some problem occurred that
	 * must be signaled back to the client.
	 * 
	 * @return the result
	 */
	public boolean hasResponse() {
		return response != null;
	}
	
	/**
	 * @return the response
	 */
	public HttpResponse getResponse() {
		return response;
	}

}
