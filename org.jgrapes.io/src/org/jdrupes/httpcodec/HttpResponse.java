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
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.fields.HttpStringListField;

/**
 * @author Michael N. Lipp
 */
public class HttpResponse {

	private HttpProtocol httpProtocol;
	private boolean hasBody;
	private int statusCode = -1;
	private String reasonPhrase;
	private Map<String,HttpField<?>> headers 
		= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	public HttpResponse(HttpProtocol protocol,
			HttpStatus status, boolean hasBody) {
		httpProtocol = protocol;
		setStatus(status);
		this.hasBody = hasBody;
	}
	
	public HttpResponse(HttpRequest request,
			HttpStatus status, boolean hasBody) throws ParseException {
		httpProtocol = request.getProtocol();
		HttpStringListField conField = request
		        .getHeader(HttpStringListField.class, HttpField.CONNECTION);
		if (conField != null && conField.containsIgnoreCase("close")) {
			conField = new HttpStringListField
					(HttpField.CONNECTION, "close");
			setHeader(conField);
		}
		setStatus(status);
		this.hasBody = hasBody;
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
	 * Returns true if body data will be delivered to the encoder
	 * after the header.
	 * 
	 * @return {@code true} if body data follows
	 */
	public boolean hasBody() {
		return hasBody;
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
	 * Set a header for the response data.
	 * 
	 * @param name the header field's name
	 * @param value the header field's value
	 */
	public void setHeader(HttpField<?> field) {
		headers.put(field.getName(), field);
	}

	/**
	 * Returns the header field with the given type and name or {@code null}
	 * if no such header is set.
	 * 
	 * @param type the header field type
	 * @param name the field name
	 * @return the header field or {@code null}
	 */
	public <T extends HttpField<?>> T getHeader(Class<T> type, String name) {
		return type.cast(headers.get(name));
	}
	
	/**
	 * Returns all headers as unmodifiable map.
	 * 
	 * @return the headers
	 */
	public Map<String, HttpField<?>> headers() {
		return Collections.unmodifiableMap(headers);
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
		HttpMediaTypeField mt = new HttpMediaTypeField(type, subtype);
		mt.setParameter("charset", charset);
	}
}
