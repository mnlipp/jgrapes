/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.http.ResponseCreationSupport.MaxAgeCalculator;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.StreamFile;

/**
 * A dispatcher for requests for static content, usually files.
 */
public class StaticContentDispatcher extends Component {

	private ResourcePattern resourcePattern;
	private URI contentRoot = null;
	private Path contentDirectory = null;
	private MaxAgeCalculator maxAgeCalculator = 
			(request, mediaType) -> 365*24*3600;
	
	/**
	 * Creates new dispatcher that tries to fulfill requests matching 
	 * the given resource pattern from the given content root.
	 * 
	 * An attempt is made to convert the content root to a {@link Path}
	 * in a {@link FileSystem}. If this fails, the content root is
	 * used as a URL against which requests are resolved and data
	 * is obtained by open an input stream from the resulting URL.
	 * In the latter case modification times aren't available. 
	 * 
	 * @param componentChannel this component's channel
	 * @param resourcePattern the pattern that requests must match 
	 * in order to be handled by this component 
	 * (see {@link ResourcePattern})
	 * @param contentRoot the location with content to serve 
	 * @see Component#Component(Channel)
	 */
	public StaticContentDispatcher(Channel componentChannel, 
			String resourcePattern, URI contentRoot) {
		super(componentChannel);
		try {
			this.resourcePattern = new ResourcePattern(resourcePattern);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		try {
			this.contentDirectory = Paths.get(contentRoot);
		} catch (FileSystemNotFoundException e) {
			this.contentRoot = contentRoot;
		}
		RequestHandler.Evaluator.add(this, "onGet", resourcePattern);
	}

	/**
	 * Creates a new component base with its channel set to
	 * itself.
	 * 
	 * @param resourcePattern the pattern that requests must match with to 
	 * be handled by this component 
	 * (see {@link ResourcePattern#matches(String, java.net.URI)})
	 * @param contentRoot the location with content to serve 
	 * @see Component#Component()
	 */
	public StaticContentDispatcher(String resourcePattern, URI contentRoot) {
		this(Channel.SELF, resourcePattern, contentRoot);
	}

	/**
	 * @return the maxAgeCalculator
	 */
	public MaxAgeCalculator maxAgeCalculator() {
		return maxAgeCalculator;
	}

	/**
	 * Sets the {@link MaxAgeCalculator} for generating the `Cache-Control` 
	 * (`max-age`) header of the response. The default max age calculator 
	 * used simply returns a max age of one year, since this component
	 * is intended to serve static content.
	 * 
	 * @param maxAgeCalculator the maxAgeCalculator to set
	 */
	public void setMaxAgeCalculator(MaxAgeCalculator maxAgeCalculator) {
		this.maxAgeCalculator = maxAgeCalculator;
	}

	@RequestHandler(dynamic=true)
	public void onGet(GetRequest event, IOSubchannel channel)
			throws ParseException, IOException {
		int prefixSegs = resourcePattern.matches(event.requestUri());
		if (prefixSegs < 0) {
			return;
		}
		if (contentDirectory != null) {
			getFromFileSystem(event, channel, prefixSegs);
		} else {
			getFromUri(event, channel, prefixSegs);
		}
	}

	private boolean getFromFileSystem(GetRequest event, IOSubchannel channel,
	        int prefixSegs) throws IOException, ParseException {
		// Final wrapper for usage in closure
		final Path[] assembly = new Path[] { contentDirectory };
		Arrays.stream(event.requestUri().getPath().split("/"))
		        .skip(prefixSegs + 1)
		        .forEach(e -> assembly[0] = assembly[0].resolve(e));
		Path resourcePath = assembly[0];
		if (Files.isDirectory(resourcePath)) {
			Path indexPath = resourcePath.resolve("index.html");
			if (Files.isReadable(indexPath)) {
				resourcePath = indexPath;
			} else {
				return false;
			}
		}
		if (!Files.isReadable(resourcePath)) {
			return false;
		}
		
		// Get content type
		HttpResponse response = event.httpRequest().response().get();
		MediaType mediaType = HttpResponse.contentType(resourcePath.toUri());
		
		// Derive max-age
		ResponseCreationSupport.setMaxAge(
				response, maxAgeCalculator, event.httpRequest(), mediaType);

		// Check if sending is really required.
		Instant lastModified = Files.getLastModifiedTime(resourcePath)
				.toInstant().with(ChronoField.NANO_OF_SECOND, 0);
		Optional<Instant> modifiedSince = event.httpRequest()
				.findValue(HttpField.IF_MODIFIED_SINCE, Converters.DATE_TIME);
		event.setResult(true);
		event.stop();
		if (modifiedSince.isPresent() 
				&& !lastModified.isAfter(modifiedSince.get())) {
			response.setStatus(HttpStatus.NOT_MODIFIED);
			response.setField(HttpField.LAST_MODIFIED, lastModified);
			channel.respond(new Response(response));
		} else {
			response.setContentType(mediaType);
			response.setStatus(HttpStatus.OK);
			response.setField(HttpField.LAST_MODIFIED, lastModified);
			channel.respond(new Response(response));
			fire(new StreamFile(resourcePath, StandardOpenOption.READ), channel);
		}
		return true;
	}

	private boolean getFromUri(GetRequest event, IOSubchannel channel,
	        int prefixSegs) throws ParseException {
		return ResponseCreationSupport.sendStaticContent(
				event, channel, path -> {
					try {
						return contentRoot.resolve(
								ResponseCreationSupport.removeSegments(
										path, prefixSegs + 1)).toURL();
					} catch (MalformedURLException e) {
						throw new IllegalArgumentException(e);
					}
				}, maxAgeCalculator);
	}
	
}
