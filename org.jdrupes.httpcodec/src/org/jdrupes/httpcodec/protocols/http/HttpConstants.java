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

/**
 * @author Michael N. Lipp
 *
 */
public interface HttpConstants {

    public static enum HttpProtocol { 
    	HTTP_1_0("HTTP/1.0"), HTTP_1_1("HTTP/1.1");
    	
    	private String repr;
    	
    	HttpProtocol(String repr) {
    		this.repr = repr;
    	}

    	@Override
		public String toString() {
			return repr;
		}
    }

    public static enum HttpStatus {
    	CONTINUE(100, "Continue"),
    	SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    	OK(200, "OK"),
    	CREATED(201, "Created"),
    	ACCEPTED(202, "Accepted"),
    	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    	NO_CONTENT(204, "No Content"),
    	RESET_CONTENT(205, "Reset Content"),
    	PARTIAL_CONTENT(206, "Partial Content"),
    	MULTIPLE_CHOICES(300, "Multiple Choices"),
    	MOVED_PERMANENTLY(301, "Moved Permanently"),
    	FOUND(302, "Found"),
    	SEE_OTHER(303, "See Other"),
    	NOT_MODIFIED(304, "Not Modified"),
    	USE_PROXY(305, "Use Proxy"),
    	TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    	BAD_REQUEST(400, "Bad Request"),
    	UNAUTHORIZED(401, "Unauthorized"),
    	PAYMENT_REQUIRED(402, "Payment Required"),
    	FORBIDDEN(403, "Forbidden"),
    	NOT_FOUND(404, "Not Found"),
    	METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    	NOT_ACCEPTABLE(406, "Not Acceptable"),
    	PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    	REQUEST_TIME_OUT(408, "Request Time-out"),
    	CONFLICT(409, "Conflict"),
    	GONE(410, "Gone"),
    	LENGTH_REQUIRED(411, "Length Required"),
    	PRECONDITION_FAILED(412, "Precondition Failed"),
    	REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    	REQUEST_URI_TOO_LARGE(414, "Request-URI Too Large"),
    	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    	REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
    	EXPECTATION_FAILED(417, "Expectation Failed"),
    	INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    	NOT_IMPLEMENTED(501, "Not Implemented"),
    	BAD_GATEWAY(502, "Bad Gateway"),
    	SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    	GATEWAY_TIME_OUT(504, "Gateway Time-out"),
    	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported");
    	
    	private int statusCode;
    	private String reasonPhrase;
    	
    	HttpStatus(int statusCode, String reasonPhrase) {
    		this.statusCode = statusCode;
    		this.reasonPhrase = reasonPhrase;
    	}
		/**
		 * @return the status code
		 */
		public int getStatusCode() {
			return statusCode;
		}

		/**
		 * @return the reason phrase
		 */
		public String getReasonPhrase() {
			return reasonPhrase;
		}
    }
    
    public enum TransferCoding { CHUNKED("chunked"), COMPRESS("compress"),
    	DEFLATE("deflate"), GZIP("gzip");
    	
    	private String repr;
    	
    	TransferCoding(String repr) {
    		this.repr = repr;
    	}

    	@Override
		public String toString() {
			return repr;
		}
    }

	public final static String TOKEN_CHARS 
		= "!#$%&'*+-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_"
			+ "^`abcdefghijklmnopqrstuvwxyz|~";

}
