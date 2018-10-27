/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Event;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.InputStreamPipeline;

/**
 * Provides methods that support the creation of a {@link Response} events.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public abstract class ResponseCreationSupport {

    /** A default implementation for the max-age calculator. */
    @SuppressWarnings("PMD.LongVariable")
    public static final MaxAgeCalculator DEFAULT_MAX_AGE_CALCULATOR
        = new DefaultMaxAgeCalculator();

    /**
     * Send a response to the given request with the given status code 
     * and reason phrase, including a `text/plain` body with the status 
     * code and reason phrase. 
     *
     * @param request the request
     * @param channel for responding; events will be sent using
     * {@link IOSubchannel#respond(org.jgrapes.core.Event)}
     * @param statusCode the status code to send
     * @param reasonPhrase the reason phrase to send
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static void sendResponse(HttpRequest request,
            IOSubchannel channel, int statusCode, String reasonPhrase) {
        HttpResponse response = request.response().get();
        response.setStatusCode(statusCode).setReasonPhrase(reasonPhrase)
            .setHasPayload(true).setField(
                HttpField.CONTENT_TYPE,
                MediaType.builder().setType("text", "plain")
                    .setParameter("charset", "utf-8").build());
        // Act like a sub-component, i.e. generate events that are
        // handled by this HTTP server as if sent from a sub-component.
        channel.respond(new Response(response));
        try {
            channel.respond(Output.from((statusCode + " " + reasonPhrase)
                .getBytes("utf-8"), true));
        } catch (UnsupportedEncodingException e) {
            // Supported by definition
        }
    }

    /**
     * Shorthand for invoking 
     * {@link #sendResponse(HttpRequest, IOSubchannel, int, String)}
     * with a predefined HTTP status.
     *
     * @param request the request
     * @param channel the channel
     * @param status the status
     */
    public static void sendResponse(HttpRequest request,
            IOSubchannel channel, HttpStatus status) {
        sendResponse(request, channel, status.statusCode(),
            status.reasonPhrase());
    }

    /**
     * Creates and sends a response with static content. The content 
     * is looked up by invoking the resolver with the path from the request.
     * 
     * The response includes a max-age header with a default value of
     * 600. The value may be modified by specifying validity infos.
     *
     * @param request the request
     * @param channel the channel
     * @param resolver the resolver
     * @param maxAgeCalculator the max age calculator, if `null`
     * the default calculator is used.
     * @return `true` if a response was sent
     */
    @SuppressWarnings({ "PMD.NcssCount",
        "PMD.UseStringBufferForStringAppends" })
    public static boolean sendStaticContent(
            HttpRequest request, IOSubchannel channel,
            Function<String, URL> resolver, MaxAgeCalculator maxAgeCalculator) {
        String path = request.requestUri().getPath();
        URL resourceUrl = resolver.apply(path);
        ResourceInfo info;
        URLConnection resConn;
        InputStream resIn;
        try {
            if (resourceUrl == null) {
                throw new IOException();
            }
            info = ResponseCreationSupport.resourceInfo(resourceUrl);
            if (Boolean.TRUE.equals(info.isDirectory())) {
                throw new IOException();
            }
            resConn = resourceUrl.openConnection();
            resIn = resConn.getInputStream();
        } catch (IOException e1) {
            try {
                if (!path.endsWith("/")) {
                    path += "/";
                }
                path += "index.html";
                resourceUrl = resolver.apply(path);
                if (resourceUrl == null) {
                    return false;
                }
                info = ResponseCreationSupport.resourceInfo(resourceUrl);
                resConn = resourceUrl.openConnection();
                resIn = resConn.getInputStream();
            } catch (IOException e2) {
                return false;
            }
        }
        HttpResponse response = request.response().get();
        response.setField(HttpField.LAST_MODIFIED,
            Optional.ofNullable(info.getLastModifiedAt())
                .orElseGet(() -> Instant.now()));

        // Get content type and derive max age
        MediaType mediaType = HttpResponse.contentType(
            ResponseCreationSupport.uriFromUrl(resourceUrl));
        setMaxAge(response,
            (maxAgeCalculator == null ? DEFAULT_MAX_AGE_CALCULATOR
                : maxAgeCalculator).maxAge(request, mediaType));

        // Check if sending is really required.
        Optional<Instant> modifiedSince = request
            .findValue(HttpField.IF_MODIFIED_SINCE, Converters.DATE_TIME);
        if (modifiedSince.isPresent() && info.getLastModifiedAt() != null
            && !info.getLastModifiedAt().isAfter(modifiedSince.get())) {
            response.setStatus(HttpStatus.NOT_MODIFIED);
            channel.respond(new Response(response));
        } else {
            response.setContentType(mediaType);
            response.setStatus(HttpStatus.OK);
            channel.respond(new Response(response));
            // Start sending content (Output events as resonses)
            (new InputStreamPipeline(resIn, channel).suppressClose()).run();
        }
        return true;
    }

    /**
     * Shorthand for invoking 
     * {@link #sendStaticContent(HttpRequest, IOSubchannel, Function, MaxAgeCalculator)}
     * with the {@link HttpRequest} from the event. Also sets the result
     * of the event to `true` and invokes {@link Event#stop()} 
     * if a response was sent.
     *
     * @param event the event
     * @param channel the channel
     * @param resolver the resolver
     * @param maxAgeCalculator the max age calculator, if `null`
     * the default calculator is used.
     * @return `true` if a response was sent
     * @throws ParseException the parse exception
     */
    public static boolean sendStaticContent(
            Request.In event, IOSubchannel channel,
            Function<String, URL> resolver, MaxAgeCalculator maxAgeCalculator) {
        if (sendStaticContent(
            event.httpRequest(), channel, resolver, maxAgeCalculator)) {
            event.setResult(true);
            event.stop();
            return true;
        }
        return false;
    }

    /**
     * Combines the known information about a resource.
     */
    public static class ResourceInfo {
        public Boolean isDirectory;
        public Instant lastModifiedAt;

        /**
         * @param isDirectory
         * @param lastModifiedAt
         */
        public ResourceInfo(Boolean isDirectory, Instant lastModifiedAt) {
            this.isDirectory = isDirectory;
            this.lastModifiedAt = lastModifiedAt;
        }

        /**
         * @return the isDirectory
         */
        public Boolean isDirectory() {
            return isDirectory;
        }

        /**
         * @return the lastModifiedAt
         */
        public Instant getLastModifiedAt() {
            return lastModifiedAt;
        }
    }

    /**
     * Attempts to lookup the additional resource information for the
     * given URL. 
     * 
     * If a {@link URL} references a file, it is easy to find out if 
     * the resource referenced is a directory and to get its last 
     * modification time. Getting the same information
     * for a {@link URL} that references resources in a jar is a bit
     * more difficult. This method handles both cases.
     *
     * @param resource the resource URL
     * @return the resource info
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static ResourceInfo resourceInfo(URL resource) {
        try {
            Path path = Paths.get(resource.toURI());
            return new ResourceInfo(Files.isDirectory(path),
                Files.getLastModifiedTime(path).toInstant()
                    .with(ChronoField.NANO_OF_SECOND, 0));
        } catch (FileSystemNotFoundException | IOException
                | URISyntaxException e) {
            // Fall through
        }
        if ("jar".equals(resource.getProtocol())) {
            try {
                JarURLConnection conn
                    = (JarURLConnection) resource.openConnection();
                JarEntry entry = conn.getJarEntry();
                return new ResourceInfo(entry.isDirectory(),
                    entry.getLastModifiedTime().toInstant()
                        .with(ChronoField.NANO_OF_SECOND, 0));
            } catch (IOException e) {
                // Fall through
            }
        }
        try {
            URLConnection conn = resource.openConnection();
            long lastModified = conn.getLastModified();
            if (lastModified != 0) {
                return new ResourceInfo(null, Instant.ofEpochMilli(
                    lastModified).with(ChronoField.NANO_OF_SECOND, 0));
            }
        } catch (IOException e) {
            // Fall through
        }
        return new ResourceInfo(null, null);
    }

    /**
     * Create a {@link URI} from a path. This is similar to calling
     * `new URI(null, null, path, null)` with the {@link URISyntaxException}
     * converted to a {@link IllegalArgumentException}.
     * 
     * @param path the path
     * @return the uri
     * @throws IllegalArgumentException if the string violates 
     * RFC 2396
     */
    public static URI uriFromPath(String path) throws IllegalArgumentException {
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a {@link URI} from a {@link URL}. This is similar to calling
     * `url.toURI()` with the {@link URISyntaxException}
     * converted to a {@link IllegalArgumentException}.
     * 
     * @param url the url
     * @return the uri
     * @throws IllegalArgumentException if the url violates RFC 2396
     */
    public static URI uriFromUrl(URL url) throws IllegalArgumentException {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Sets the cache control header in the given response.
     *
     * @param response the response
     * @param maxAge the max age
     * @return the value set
     */
    public static long setMaxAge(HttpResponse response, int maxAge) {
        List<Directive> directives = new ArrayList<>();
        directives.add(new Directive("max-age", maxAge));
        response.setField(HttpField.CACHE_CONTROL, directives);
        return maxAge;
    }

    /**
     * Describes a calculator for the max-age property.
     */
    @FunctionalInterface
    public interface MaxAgeCalculator {

        /**
         * Calculate a max age value for a response using the given 
         * request and the media type of the repsonse.
         *
         * @param request the request, usually only the URI is
         * considered for the calculation
         * @param mediaType the media type of the response
         * @return the max age value to be used in the response
         */
        int maxAge(HttpRequest request, MediaType mediaType);
    }

    /**
     * DefaultMaxAgeCalculator provides an implementation that 
     * tries to guess a good max age value by looking at the
     * path of the requested resource. If the path contains
     * the pattern "dash, followed by a number, followed by
     * a dot and a number" it is assumed that the resource
     * is versioned, i.e. its path changes if the resource
     * changes. In this case a max age of one year is returned.
     * In all other cases, a max age value of 60 (one minute)
     * is returned.
     */
    public static class DefaultMaxAgeCalculator implements MaxAgeCalculator {

        public static final Pattern VERSION_PATTERN
            = Pattern.compile("-[0-9]+\\.[0-9]+");

        @Override
        public int maxAge(HttpRequest request, MediaType mediaType) {
            if (VERSION_PATTERN.matcher(
                request.requestUri().getPath()).find()) {
                return 365 * 24 * 3600;
            }
            return 60;
        }

    }
}
