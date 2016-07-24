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

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;

/**
 * @author Michael N. Lipp
 *
 */
public class ProtocolException extends Exception {

	private static final long serialVersionUID = 1L;

	private HttpProtocol httpProtocol;
	private int statusCode;
	private String reasonPhrase;

	/**
	 * @param httpProtocol
	 * @param statusCode
	 * @param reasonPhrase
	 */
	public ProtocolException(HttpProtocol httpProtocol, int statusCode,
	        String reasonPhrase) {
		super(String.format("%03d %s", statusCode, reasonPhrase));
		this.httpProtocol = httpProtocol;
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	public ProtocolException(HttpProtocol httpProtocol, HttpStatus status) {
		super(String.format("%03d %s", status.getStatusCode(),
				status.getReasonPhrase()));
		this.httpProtocol = httpProtocol;
		this.statusCode = status.getStatusCode();
		this.reasonPhrase = status.getReasonPhrase();
	}
	
	/**
	 * @return the httpVersion
	 */
	public HttpProtocol getHttpVersion() {
		return httpProtocol;
	}
	
	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @return the reasonPhrase
	 */
	public String getReasonPhrase() {
		return reasonPhrase;
	}
	
}
