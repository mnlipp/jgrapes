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
public class DecoderResult {

	private HttpRequest request;
	private HttpResponse response;
	private boolean mustBeClosed;
	private boolean payloadBytes;
	private boolean payloadChars;

	/**
	 * @param request
	 * @param response
	 * @param closed
	 * @param payloadBytes
	 * @param payloadChars
	 */
	public DecoderResult(HttpRequest request, HttpResponse response,
	        boolean needsClose, boolean payloadBytes, boolean payloadChars) {
		super();
		this.request = request;
		this.response = response;
		this.mustBeClosed = needsClose;
		this.payloadBytes = payloadBytes;
		this.payloadChars = payloadChars;
	}

	/**
	 * Returns {@code true} if the result includes a request.
	 * 
	 * @return the result
	 */
	public boolean hasRequest() {
		return request != null;
	}
	
	/**
	 * @return the request
	 */
	public HttpRequest getRequest() {
		return request;
	}

	/**
	 * Returns {@code true} if the result includes a response.
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

	/**
	 * @return the closed
	 */
	public boolean mustBeClosed() {
		return mustBeClosed;
	}

	/**
	 * @return the payloadBytes
	 */
	public boolean hasPayloadBytes() {
		return payloadBytes;
	}

	/**
	 * @return the payloadChars
	 */
	public boolean hasPayloadChars() {
		return payloadChars;
	}
}
