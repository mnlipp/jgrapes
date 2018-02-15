/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.http;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.MediaRange;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.InputStreamPipeline;

/**
 * Provides methods that support the creation of a {@link Response} events.
 */
public class ResponseCreationSupport {

	/**
	 * Creates and sends a response with static content. The content 
	 * is looked up by invoking the resolver with the path from the request.
	 * 
	 * The response includes a max-age header with a default value of
	 * 600. The value may be modified by specifying validity infos.
	 *
	 * @param event the event
	 * @param channel the channel
	 * @param resolver the resolver
	 * @param validityInfos the validity infos
	 * @return the from uri
	 * @throws ParseException the parse exception
	 */
	public static boolean sendStaticContent(
			Request event, IOSubchannel channel,  
			Function<String,URL> resolver, List<ValidityInfo> validityInfos)
					throws ParseException {
		String path = event.requestUri().getPath();
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
		
		// Get content type
		HttpResponse response = event.httpRequest().response().get();
		MediaType mediaType = HttpResponse.contentType(
				ResponseCreationSupport.uriFromUrl(resourceUrl));

		// Derive max-age
		setMaxAge(response, validityInfos, mediaType, 600);

		// Check if sending is really required.
		Optional<Instant> modifiedSince = event.httpRequest()
				.findValue(HttpField.IF_MODIFIED_SINCE, Converters.DATE_TIME);
		event.stop();
		if (modifiedSince.isPresent() && info.getLastModifiedAt() != null
				&& !info.getLastModifiedAt().isAfter(modifiedSince.get())) {
			response.setStatus(HttpStatus.NOT_MODIFIED);
			response.setField(HttpField.LAST_MODIFIED, info.getLastModifiedAt());
			channel.respond(new Response(response));
		} else {
			response.setContentType(mediaType);
			response.setStatus(HttpStatus.OK);
			response.setField(HttpField.LAST_MODIFIED, info.getLastModifiedAt());
			channel.respond(new Response(response));
			// Start sending content
			channel.responsePipeline().executorService()
				.submit(new InputStreamPipeline(resIn, channel));
		}
		return true;
	}

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
	public static ResourceInfo resourceInfo(URL resource) {
		try {
			Path path = Paths.get(resource.toURI());
			return new ResourceInfo(Files.isDirectory(path),
					Files.getLastModifiedTime(path).toInstant());
		} catch (FileSystemNotFoundException | IOException
				| URISyntaxException e) {
			// Fall through
		}
		if ("jar".equals(resource.getProtocol())) {
			try {
				JarURLConnection conn = (JarURLConnection)resource.openConnection();
				JarEntry entry = conn.getJarEntry();
				return new ResourceInfo(entry.isDirectory()	, 
						entry.getLastModifiedTime().toInstant());
			} catch (IOException e) {
				// Fall through
			}
		}
		return new ResourceInfo(null, null);
	}
	
	/**
	 * Removes the given number of segments (and their trailing slashes)
	 * from the beginning of the path. Segments may be empty. This implies 
	 * that invoking this method with a path that starts with a
	 * slash, the first removed segment is the empty segment
	 * preceding the slash and the starting slash. Put differently, 
	 * invoking this method with an absolute path and 1 makes the path
	 * relative.
	 *
	 * @param path the path
	 * @param segments the number of segments to remove
	 * @return the result
	 */
	public static String removeSegments(String path, int segments) {
		return Arrays.stream(path.split("/"))
	        .skip(segments).collect(Collectors.joining("/"));
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
	 * Sets the cache control header in the given response,
	 * looking up the value for the given media type or using
	 * the default value.
	 *
	 * @param response the response
	 * @param validityInfos the validity infos
	 * @param mediaType the media type
	 * @param defaultValue the default value
	 * @return the value set
	 */
	public static long setMaxAge(HttpResponse response, 
			Iterable<ValidityInfo> validityInfos,
			MediaType mediaType, long defaultValue) {
		long maxAge = defaultValue;
		if (validityInfos != null) {
			for (ValidityInfo info: validityInfos) {
				if (info.mediaRange().matches(mediaType)) {
					break;
				}
			}
		}
		List<Directive> directives = new ArrayList<>();
		directives.add(new Directive("max-age", maxAge));
		response.setField(HttpField.CACHE_CONTROL, directives);
		return maxAge;
	}
	
	/**
	 * Describes an association between a media range and a 
	 * maximum age in seconds. 
	 */
	public static class ValidityInfo {
		private MediaRange mediaRange;
		private long maxAge;

		/**
		 * Instantiates a new validity info.
		 *
		 * @param mediaRange the media range
		 * @param maxAge the max age
		 */
		public ValidityInfo(MediaRange mediaRange, long maxAge) {
			super();
			this.mediaRange = mediaRange;
			this.maxAge = maxAge;
		}

		/**
		 * The media range matches by this info.
		 * 
		 * @return the mediaRange
		 */
		public MediaRange mediaRange() {
			return mediaRange;
		}
		
		/**
		 * The time span in seconds until the response expires.
		 * 
		 * @return the time span
		 */
		public long maxAge() {
			return maxAge;
		}
	}
	
}
