/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.http.events;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionEvent;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResourcePattern.PathSpliterator;

/**
 * The base class for all HTTP requests such as {@link Request.In.Get},
 * {@link Request.In.Post} etc.
 */
public class Request<R> extends Event<R> {

    private URI uri;

    /**
     * @param channels
     */
    protected Request(Channel... channels) {
        super(channels);
    }

    /**
     * Sets the request URI.
     *
     * @param uri the new request URI
     */
    protected void setRequestUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns an absolute URI of the request. For incoming requests, the 
     * URI is built from the information provided by the decoder.
     * 
     * @return the URI
     */
    public URI requestUri() {
        return uri;
    }

    /**
     * The base class for all incoming HTTP requests. Incoming
     * request flow downstream and are served. 
     * 
     * A result of `true` indicates that the request has been processed, 
     * i.e. a response has been sent or will sent.
     */
    public static class In extends Request<Boolean> {

        @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
        private final HttpRequest request;
        private MatchValue matchValue;
        private URI matchUri;

        /**
         * Creates a new request event with the associated {@link Completed}
         * event.
         * 
         * @param protocol the protocol as reported by {@link #requestUri()}
         * @param request the request data
         * @param matchLevels the number of elements from the request path
         * to use in the match value (see {@link #matchValue})
         * @param channels the channels associated with this event
         */
        @SuppressWarnings("PMD.UselessParentheses")
        public In(String protocol, HttpRequest request,
                int matchLevels, Channel... channels) {
            super(channels);
            new Completed(this);
            this.request = request;
            try {
                URI headerInfo = new URI(protocol, null,
                    request.host(), request.port(), null, null, null);
                setRequestUri(headerInfo.resolve(request.requestUri()));
                Iterator<String> segs = PathSpliterator.stream(
                    requestUri().getPath()).skip(1).iterator();
                StringBuilder pattern = new StringBuilder(20);
                for (int i = 0; i < matchLevels && segs.hasNext(); i++) {
                    pattern.append('/')
                        .append(segs.next());
                }
                if (segs.hasNext()) {
                    pattern.append("/**");
                }
                String matchPath = pattern.toString();
                URI uri = requestUri();
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
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Creates the appropriate derived request event type from
         * a given {@link HttpRequest}.
         * 
         * @param request the HTTP request
         * @param secure whether the request was received over a secure channel
         * @param matchLevels the match levels
         * @return the request event
         */
        public static In fromHttpRequest(
                HttpRequest request, boolean secure, int matchLevels) {
            switch (request.method()) {
            case "OPTIONS":
                return new Options(request, secure, matchLevels);
            case "GET":
                return new Get(request, secure, matchLevels);
            case "HEAD":
                return new Head(request, secure, matchLevels);
            case "POST":
                return new Post(request, secure, matchLevels);
            case "PUT":
                return new Put(request, secure, matchLevels);
            case "DELETE":
                return new Delete(request, secure, matchLevels);
            case "TRACE":
                return new Trace(request, secure, matchLevels);
            case "CONNECT":
                return new Connect(request, secure, matchLevels);
            default:
                return new In(secure ? "https" : "http", request, matchLevels);
            }
        }

        /**
         * Returns the "raw" request as provided by the HTTP decoder.
         * 
         * @return the request
         */
        public HttpRequest httpRequest() {
            return request;
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
         * @see org.jgrapes.core.Event#defaultCriterion()
         */
        @Override
        public Object defaultCriterion() {
            return matchValue;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.core.Event#isMatchedBy(java.lang.Object)
         */
        @Override
        public boolean isEligibleFor(Object value) {
            if (!(value instanceof MatchValue)) {
                return super.isEligibleFor(value);
            }
            MatchValue mval = (MatchValue) value;
            if (!mval.type.isAssignableFrom(matchValue.type)) {
                return false;
            }
            if (mval.resource instanceof ResourcePattern) {
                return ((ResourcePattern) mval.resource).matches(matchUri) >= 0;
            }
            return mval.resource.equals(matchValue.resource);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(Components.objectName(this))
                .append(" [\"");
            String path = request.requestUri().getPath();
            if (path.length() > 15) {
                builder.append("...")
                    .append(path.substring(path.length() - 12));
            } else {
                builder.append(path);
            }
            builder.append('\"');
            if (channels().length > 0) {
                builder.append(", channels=");
                builder.append(Channel.toString(channels()));
            }
            builder.append(']');
            return builder.toString();
        }

        /**
         * Creates the match value.
         *
         * @param type the type
         * @param resource the resource
         * @return the object
         */
        public static Object createMatchValue(
                Class<?> type, ResourcePattern resource) {
            return new MatchValue(type, resource);
        }

        /**
         * Represents a match value.
         */
        private static class MatchValue {
            private final Class<?> type;
            private final Object resource;

            /**
             * @param type
             * @param resource
             */
            public MatchValue(Class<?> type, Object resource) {
                super();
                this.type = type;
                this.resource = resource;
            }

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Object#hashCode()
             */
            @Override
            @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
            public int hashCode() {
                @SuppressWarnings("PMD.AvoidFinalLocalVariable")
                final int prime = 31;
                int result = 1;
                result = prime * result
                    + ((resource == null) ? 0 : resource.hashCode());
                result
                    = prime * result + ((type == null) ? 0 : type.hashCode());
                return result;
            }

            /*
             * (non-Javadoc)
             * 
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

        /**
         * The associated completion event.
         */
        public static class Completed extends CompletionEvent<In> {

            /**
             * Instantiates a new event.
             *
             * @param monitoredEvent the monitored event
             * @param channels the channels
             */
            public Completed(In monitoredEvent, Channel... channels) {
                super(monitoredEvent, channels);
            }
        }

        /**
         * Represents a HTTP CONNECT request.
         */
        public static class Connect extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Connect(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }

        }

        /**
         *
         */
        public static class Delete extends In {

            /**
            * Create a new event.
            * 
            * @param request the request data
            * @param secure indicates whether the request was received on a
            * secure channel
            * @param matchLevels the number of elements from the request path
            * to use in the match value
            * @param channels the channels on which the event is to be 
            * fired (optional)
            */
            public Delete(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }

        }

        /**
         * Represents a HTTP GET request.
         */
        public static class Get extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Get(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }
        }

        /**
         * Represents a HTTP HEAD request.
         */
        public static class Head extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Head(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }
        }

        /**
         * Represents a HTTP OPTIONS request.
         */
        public static class Options extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Options(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }
        }

        /**
         * Represents a HTTP POST request.
         */
        public static class Post extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Post(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }

        }

        /**
         * Represents a HTTP PUT request.
         */
        public static class Put extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Put(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }
        }

        /**
         * Represents a HTTP TRACE request.
         */
        public static class Trace extends In {

            /**
             * Create a new event.
             * 
             * @param request the request data
             * @param secure indicates whether the request was received on a
             * secure channel
             * @param matchLevels the number of elements from the request path
             * to use in the match value
             * @param channels the channels on which the event is to be 
             * fired (optional)
             */
            public Trace(HttpRequest request, boolean secure,
                    int matchLevels, Channel... channels) {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }
        }
    }
}
