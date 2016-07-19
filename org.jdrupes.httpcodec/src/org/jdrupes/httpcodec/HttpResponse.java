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

import java.text.ParseException;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;

/**
 * @author Michael N. Lipp
 */
public class HttpResponse extends HttpMessage {

	private int statusCode = -1;
	private String reasonPhrase;
	
	public HttpResponse(HttpProtocol protocol,
			HttpStatus status, boolean hasBody) {
		super(protocol, hasBody);
		setStatus(status);
	}
	
	/**
	 * @return the responseCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode the responseCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the reason phrase
	 */
	public String getReasonPhrase() {
		return reasonPhrase;
	}

	/**
	 * @param reasonPhrase the reason phrase to set
	 */
	public void setReasonPhrase(String reasonPhrase) {
		this.reasonPhrase = reasonPhrase;
	}

	/**
	 * Sets both status code and reason phrase from the given 
	 * http status value.
	 * 
	 * @param status the status value
	 */
	public void setStatus(HttpStatus status) {
		statusCode = status.getStatusCode();
		reasonPhrase = status.getReasonPhrase();
	}
	
	/**
	 * A convenience method for setting the "Content-Type" header.
	 * 
	 * @param type the type
	 * @param subtype the subtype
	 * @throws ParseException
	 */
	public void setContentType(String type, String subtype) 
			throws ParseException {
		setHeader(new HttpMediaTypeField
				(HttpField.CONTENT_TYPE, type, subtype));
	}

	/**
	 * A convenience method for setting the "Content-Type" header (usually
	 * of type "text") together with its charset parameter.
	 * 
	 * @param type the type
	 * @param subtype the subtype
	 * @param charset the charset
	 * @throws ParseException
	 */
	public void setContentType(String type, String subtype,
			String charset) throws ParseException {
		HttpMediaTypeField mt = new HttpMediaTypeField(HttpField.CONTENT_TYPE,
		        type, subtype);
		mt.setParameter("charset", charset);
	}
}
