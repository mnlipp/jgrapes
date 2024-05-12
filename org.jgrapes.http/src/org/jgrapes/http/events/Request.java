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
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionEvent;
import org.jgrapes.core.Components;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResourcePattern.PathSpliterator;
import org.jgrapes.net.SocketIOChannel;

/**
 * The base class for all HTTP requests such as {@link Request.In.Get},
 * {@link Request.In.Post} etc.
 *
 * @param <R> the generic type
 */
public class Request<R> extends MessageReceived<R> {

    /**
     * @param channels
     */
    protected Request(Channel... channels) {
        super(channels);
    }

    /**
     * The base class for all incoming HTTP requests. Incoming
     * request flow downstream and are served by the framework. 
     * 
     * A result of `true` indicates that the request has been processed, 
     * i.e. a response has been sent or will sent. Händlers MUST
     * check that a request has not been {@link fulfilled} before
     * firing a {@link Response} event to avoid duplicate response
     * events. Handlers that have fired a response event and all
     * related {@link Output} events SHOULD {@link Event#stop} the
     * request event to avoid unnecessary subsequent invocations of
     * handlers. Handlers that want to do "postprocessing" MUST
     * therefore listen for the corresponding {@link Completed}
     * event instead of defining a handler for the request event
     * with low priority.
     */
    @SuppressWarnings("PMD.ShortClassName")
    public static class In extends Request<Boolean> {

        @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
        private final HttpRequest request;
        private final int matchLevels;
        private final MatchValue matchValue;
        private final URI resourceUri;
        private URI uri;

        /**
         * Creates a new request event with the associated {@link Completed}
         * event.
         * 
         * @param protocol the protocol as reported by {@link #requestUri()}
         * @param request the request data
         * @param matchLevels the number of elements from the request path
         * to use in the match value (see {@link #matchValue})
         * @param channels the channels associated with this event
         * @throws URISyntaxException 
         */
        @SuppressWarnings({ "PMD.UselessParentheses",
            "PMD.ConstructorCallsOverridableMethod" })
        public In(String protocol, HttpRequest request,
                int matchLevels, Channel... channels)
                throws URISyntaxException {
            super(channels);
            new Completed(this);
            this.request = request;
            this.matchLevels = matchLevels;

            // Do any required request specific processing of the original
            // request URI
            URI requestUri = effectiveRequestUri(protocol, request);

            // Clean the request URI's path, keeping the segments for matchValue
            List<String> segs = pathToSegs(requestUri);
            requestUri = new URI(requestUri.getScheme(),
                requestUri.getUserInfo(), requestUri.getHost(),
                requestUri.getPort(),
                "/" + segs.stream().collect(Collectors.joining("/")),
                requestUri.getQuery(), null);
            setRequestUri(requestUri);

            // The URI for handler selection ignores user info and query
            resourceUri = new URI(requestUri.getScheme(), null,
                requestUri.getHost(), requestUri.getPort(),
                requestUri.getPath(), null, null);
            @SuppressWarnings("PMD.InefficientStringBuffering")
            StringBuilder matchPath = new StringBuilder("/" + segs.stream()
                .limit(matchLevels).collect(Collectors.joining("/")));
            if (segs.size() > matchLevels) {
                if (!matchPath.toString().endsWith("/")) {
                    matchPath.append('/');
                }
                matchPath.append('…');
            }
            matchValue = new MatchValue(getClass(),
                (requestUri.getScheme() == null ? ""
                    : (requestUri.getScheme() + "://"))
                    + (requestUri.getHost() == null ? ""
                        : (requestUri.getHost()
                            + (requestUri.getPort() == -1 ? ""
                                : (":" + requestUri.getPort()))))
                    + matchPath);
        }

        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        private List<String> pathToSegs(URI requestUri)
                throws URISyntaxException {
            Iterator<String> origSegs = PathSpliterator.stream(
                requestUri.getPath()).iterator();
            // Path must not be empty and must be absolute
            if (!origSegs.hasNext() || !origSegs.next().isEmpty()) {
                throw new URISyntaxException(
                    requestUri().getPath(), "Must be absolute");
            }
            // Remove dot segments and check for "...//..."
            @SuppressWarnings({ "PMD.LooseCoupling",
                "PMD.ReplaceVectorWithList" })
            Stack<String> segs = new Stack<>();
            while (origSegs.hasNext()) {
                if (!segs.isEmpty() && segs.peek().isEmpty()) {
                    // Empty segment followed by more means "//"
                    segs.clear();
                }
                String seg = origSegs.next();
                if (".".equals(seg)) {
                    continue;
                }
                if ("..".equals(seg)) {
                    if (!segs.isEmpty()) {
                        segs.pop();
                    }
                    continue;
                }
                segs.push(seg);
            }
            return segs;
        }

        /**
         * Builds the URI that represents this request. The default
         * implementation checks that request URI in the HTTP request
         * is directed at this server as specified in the "Host"-header
         * and adds the protocol, host and port if not specified 
         * in the request URI. 
         *
         * @param protocol the protocol
         * @param request the request
         * @return the URI
         * @throws URISyntaxException if the request is not acceptable
         */
        protected URI effectiveRequestUri(String protocol, HttpRequest request)
                throws URISyntaxException {
            URI serverUri = new URI(protocol, null,
                request.host(), request.port(), "/", null, null);
            URI origRequest = request.requestUri();
            URI result = serverUri.resolve(new URI(null, null, null, -1,
                origRequest.getPath(), origRequest.getQuery(), null));
            if (!result.getScheme().equals(protocol)
                || !result.getHost().equals(request.host())
                || result.getPort() != request.port()) {
                throw new URISyntaxException(origRequest.toString(),
                    "Scheme, host or port not allowed");
            }
            return result;
        }

        /**
         * Creates the appropriate derived request event type from
         * a given {@link HttpRequest}.
         * 
         * @param request the HTTP request
         * @param secure whether the request was received over a secure channel
         * @param matchLevels the match levels
         * @return the request event
         * @throws URISyntaxException 
         */
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public static In fromHttpRequest(
                HttpRequest request, boolean secure, int matchLevels)
                throws URISyntaxException {
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
         * Sets the request URI.
         *
         * @param uri the new request URI
         */
        protected final void setRequestUri(URI uri) {
            this.uri = uri;
        }

        /**
         * Returns an absolute URI of the request. For incoming requests, the 
         * URI is built from the information provided by the decoder.
         * 
         * @return the URI
         */
        public final URI requestUri() {
            return uri;
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
         * The match value is used as key in a map that speeds up
         * the lookup of handlers. Having the complete URI in the match
         * value would inflate this map.
         *
         * @return the object
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
                return ((ResourcePattern) mval.resource).matches(resourceUri,
                    matchLevels) >= 0;
            }
            return mval.resource.equals(matchValue.resource);
        }

        /**
         * Checks if the request has been processed, i.e. a response has 
         * been sent.
         *
         * @return true, if fulfilled
         */
        public boolean fulfilled() {
            return currentResults().size() > 0 && currentResults().get(0);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public String toString() {
            StringBuilder builder = new StringBuilder(50);
            builder.append(Components.objectName(this))
                .append(" [\"");
            String path = Optional.ofNullable(request.requestUri().getPath())
                .orElse("");
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
             * @throws URISyntaxException 
             */
            public Connect(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }

            /**
             * Builds the URI that represents this request. This
             * implementation returns the request URI without
             * path and query component. 
             *
             * @param protocol the protocol
             * @param request the request
             * @return the uri
             * @throws URISyntaxException the URI syntax exception
             * @see "[RFC 7230, Section 5.5](https://datatracker.ietf.org/doc/html/rfc7230#section-5.5)"
             */
            @Override
            protected URI effectiveRequestUri(String protocol,
                    HttpRequest request) throws URISyntaxException {
                URI req = request.requestUri();
                return new URI(req.getScheme(), req.getUserInfo(),
                    req.getHost(), req.getPort(), null, null, null);
            }
        }

        /**
         * The Class Delete.
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
             * @throws URISyntaxException 
            */
            public Delete(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
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
             * @throws URISyntaxException 
             */
            public Get(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
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
             * @throws URISyntaxException 
             */
            public Head(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
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
             * @throws URISyntaxException 
             */
            public Options(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
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
             * @throws URISyntaxException 
             */
            public Post(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
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
             * @throws URISyntaxException 
             */
            public Put(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
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
             * @throws URISyntaxException 
             */
            public Trace(HttpRequest request, boolean secure, int matchLevels,
                    Channel... channels) throws URISyntaxException {
                super(secure ? "https" : "http", request, matchLevels,
                    channels);
            }
        }
    }

    /**
     * The base class for all outgoing HTTP requests. Outgoing
     * request flow upstream and are served externally.
     */
    @SuppressWarnings("PMD.ShortClassName")
    public static class Out extends Request<Void> {

        private final HttpRequest request;
        private BiConsumer<Request.Out, SocketIOChannel> connectedCallback;

        /**
         * Instantiates a new request.
         *
         * @param method the method
         * @param url the url
         */
        public Out(String method, URL url) {
            try {
                request = new HttpRequest(method, url.toURI(),
                    HttpProtocol.HTTP_1_1, false);
            } catch (URISyntaxException e) {
                // This should not happen because every valid URL can be
                // converted to a URI.
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Sets a "connected callback". When the {@link Out} event is
         * created, the network connection is not yet known. Some
         * header fields' values, however, need e.g. the port information
         * from the connection. Therefore a callback may be set which is
         * invoked when the connection has been obtained that will be used
         * to send the request.
         *
         * @param connectedCallback the connected callback
         * @return the out
         */
        public Out setConnectedCallback(
                BiConsumer<Request.Out, SocketIOChannel> connectedCallback) {
            this.connectedCallback = connectedCallback;
            return this;
        }

        /**
         * Returns the connected callback.
         *
         * @return the connected callback, if set
         */
        public Optional<BiConsumer<Request.Out, SocketIOChannel>>
                connectedCallback() {
            return Optional.ofNullable(connectedCallback);
        }

        /**
         * The HTTP request that will be sent by the event.
         *
         * @return the http request
         */
        public HttpRequest httpRequest() {
            return request;
        }

        /**
         * Returns an absolute URI of the request.
         * 
         * @return the URI
         */
        public URI requestUri() {
            return request.requestUri();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(Components.objectName(this))
                .append(" [").append(request.toString());
            if (channels().length > 0) {
                builder.append(", channels=");
                builder.append(Channel.toString(channels()));
            }
            builder.append(']');
            return builder.toString();
        }

        /**
         * Represents a HTTP CONNECT request.
         */
        public static class Connect extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Connect(URL uri) {
                super("CONNECT", uri);
            }
        }

        /**
         * Represents a HTTP DELETE request.
         */
        public static class Delete extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Delete(URL uri) {
                super("DELETE", uri);
            }
        }

        /**
         * Represents a HTTP GET request.
         */
        public static class Get extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Get(URL uri) {
                super("GET", uri);
            }
        }

        /**
         * Represents a HTTP HEAD request.
         */
        public static class Head extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Head(URL uri) {
                super("HEAD", uri);
            }
        }

        /**
         * Represents a HTTP OPTIONS request.
         */
        public static class Options extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Options(URL uri) {
                super("OPTIONS", uri);
            }
        }

        /**
         * Represents a HTTP POST request.
         */
        public static class Post extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Post(URL uri) {
                super("POST", uri);
            }
        }

        /**
         * Represents a HTTP PUT request.
         */
        public static class Put extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Put(URL uri) {
                super("PUT", uri);
            }
        }

        /**
         * Represents a HTTP TRACE request.
         */
        public static class Trace extends Out {

            /**
             * Instantiates a new request.
             *
             * @param uri the uri
             */
            public Trace(URL uri) {
                super("TRACE", uri);
            }
        }
    }
}
