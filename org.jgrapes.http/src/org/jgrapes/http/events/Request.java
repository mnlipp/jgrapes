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

package org.jgrapes.http.events;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletedEvent;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.internal.Common;
import org.jgrapes.http.ResourcePattern;

/**
 * @author Michael N. Lipp
 *
 */
public class Request extends Event<Void> {

	public static class Completed extends CompletedEvent<Request> {
	}
	
	private HttpRequest request;
	private URI uri;
	private MatchValue matchValue;
	private URI matchUri;
	
	/**
	 * Creates a new request event with the associated {@link Completed}
	 * event.
	 * 
	 * @param protocol the protocol as reported by {@link #getRequestUri()}
	 * @param request the request data
	 * @param matchLevels the number of elements from the request path
	 * to use in the match value (see {@link #matchValue})
	 * @param channels the channels associated with this event
	 */
	public Request(String protocol, HttpRequest request, 
			int matchLevels, Channel... channels) {
		super(new Completed(), channels);
		this.request = request;
		try {
			URI headerInfo = new URI(protocol, null, 
					request.host(), request.port(), null, null, null);
			uri = headerInfo.resolve(request.requestUri());
			StringTokenizer st = new StringTokenizer(uri.getPath(), "/");
			StringBuilder mp = new StringBuilder();
			for (int i = 0; i < matchLevels && st.hasMoreTokens(); i++) {
				mp.append("/");
				mp.append(st.nextToken());
			}
			if (st.hasMoreTokens()) {
				mp.append("/**");
			}
			String matchPath = mp.toString();
			matchUri = new URI(uri.getScheme(), null, uri.getHost(),
			        uri.getPort(), uri.getPath(), null, null);
			matchValue = new MatchValue(getClass(), 
					(uri.getScheme() == null ? "" 
						: (uri.getScheme() + "://"))
					 + (uri.getHost() == null ? "" 
						: (uri.getHost() + (uri.getPort() == -1 ? "" 
							: (":" + uri.getPort()))))
					 + matchPath);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Returns the "raw" request as provided by the HTTP decoder.
	 * 
	 * @return the request
	 */
	public HttpRequest getRequest() {
		return request;
	}

	/**
	 * Returns a URI that is built from the information provided by the
	 * decoder.
	 * 
	 * @return the URI
	 */
	public URI getRequestUri() {
		return uri;
	}
	
	/**
	 * The match value consists of the event class and a URI.
	 * The URI is similar to the request URI but its path elements
	 * are shortened as specified in the constructor.
	 * 
	 * As the match value is used as key in a map that speeds up
	 * the lookup of handlers, having the complete URI in the match
	 * value would inflate this map.
	 * 
	 * @see org.jgrapes.core.Event#getDefaultCriterion()
	 */
	@Override
	public Object getDefaultCriterion() {
		return matchValue;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Event#isMatchedBy(java.lang.Object)
	 */
	@Override
	public boolean isEligibleFor(Object value) {
		if (!(value instanceof MatchValue)) {
			return super.isEligibleFor(value);
		}
		MatchValue mv = (MatchValue)value;
		if (!mv.type.isAssignableFrom(matchValue.type)) {
			return false;
		}
		if (mv.resource instanceof ResourcePattern) {
			return ((ResourcePattern)mv.resource).matches(matchUri) >= 0;
		}
		return mv.resource.equals(matchValue.resource);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [\"");
		String path = request.requestUri().getPath();
		if (path.length() > 15) {
			builder.append("...");
			builder.append(path.substring(path.length() - 12));
		} else {
			builder.append(path);
		}
		builder.append("\"");
		if (channels != null) {
			builder.append(", channels=");
			builder.append(Common.channelsToString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
	
	public static Object createMatchValue(
			Class<?> type, ResourcePattern resource) {
		return new MatchValue(type, resource);
	}
	
	private static class MatchValue {
		private Class<?> type;
		private Object resource;

		/**
		 * @param type
		 * @param resource
		 */
		public MatchValue(Class<?> type, Object resource) {
			super();
			this.type = type;
			this.resource = resource;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
			        + ((resource == null) ? 0 : resource.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MatchValue other = (MatchValue) obj;
			if (resource == null) {
				if (other.resource != null) {
					return false;
				}
			} else if (!resource.equals(other.resource)) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			return true;
		}
		
	}
}
