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

package org.jgrapes.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.MediaRange;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.StreamFile;

/**
 * @author Michael N. Lipp
 */
public class StaticContentDispatcher extends Component {

	private ResourcePattern resourcePattern;
	private Path contentDirectory;
	private List<ValidityInfo> validityInfos = new ArrayList<>();
	
	/**
	 * @param resourcePattern the pattern that requests must match with to 
	 * be handled by this component 
	 * (see {@link ResourcePattern#matches(String, java.net.URI)})
	 * @param contentDirectory the directory with content to serve 
	 * @see Component#Component()
	 */
	public StaticContentDispatcher(String resourcePattern, 
			Path contentDirectory) {
		this(Channel.SELF, resourcePattern, contentDirectory);
	}

	/**
	 * @param componentChannel
	 *            this component's channel
	 * @param resourcePattern the pattern that requests must match 
	 * in order to be handled by this component 
	 * (see {@link ResourcePattern#matches(String, java.net.URI)})
	 * @param contentDirectory the directory with content to serve 
	 * @see Component#Component(Channel)
	 */
	public StaticContentDispatcher(Channel componentChannel, 
			String resourcePattern, Path contentDirectory) {
		super(componentChannel);
		try {
			this.resourcePattern = new ResourcePattern(resourcePattern);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		this.contentDirectory = contentDirectory;
		RequestHandler.Evaluator.add(this, "onGet", resourcePattern);
	}

	/**
	 * Returns the validity infos as unmodifiable list.
	 * 
	 * @return the validityInfos
	 * @see #validityInfos()
	 */
	public List<ValidityInfo> validityInfos() {
		return Collections.unmodifiableList(validityInfos);
	}

	/**
	 * Sets validity infos for generating the `Cache-Control` (`max-age`)
	 * header of the response. If the type of the resource served 
	 * matches an entry in the list, the entry's `maxAge` value is used 
	 * for generating the header. If no info matches or no infos are set, 
	 * a default of 600 seconds is used as time span.
	 * 
	 * @param validityInfos the validityInfos to set
	 */
	public void setValidityInfos(List<ValidityInfo> validityInfos) {
		this.validityInfos = validityInfos;
	}

	/**
	 * Add a validity info to the list.
	 * 
	 * @return this object for easy chaining
	 * @see #validityInfos()
	 */
	public StaticContentDispatcher addValidityInfo(ValidityInfo info) {
		validityInfos.add(info);
		return this;
	}

	@RequestHandler(dynamic=true)
	public void onGet(GetRequest event) throws ParseException, IOException {
		int prefixSegs = resourcePattern.matches(event.requestUri());
		if (prefixSegs < 0) {
			return;
		}
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
				return;
			}
		}
		if (!Files.isReadable(resourcePath)) {
			return;
		}
		// Get content type and derive max-age
		String mimeTypeName = Files.probeContentType(resourcePath);
		if (mimeTypeName == null) {
			mimeTypeName = "application/octet-stream";
		}
		MediaType mediaType = Converters.MEDIA_TYPE.fromFieldValue(mimeTypeName);
		long maxAge = 600;
		for (ValidityInfo info: validityInfos) {
			if (info.mediaRange().matches(mediaType)) {
				break;
			}
		}
		// Set max age in cache header and expires header (if HTTP 1.0)
		List<Directive> directives = new ArrayList<>();
		directives.add(new Directive("max-age", Long.toString(maxAge)));
		HttpResponse response = event.request().response().get();
		response.setField(HttpField.CACHE_CONTROL, directives);
		if (response.request().get().protocol()
				.compareTo(HttpProtocol.HTTP_1_1) < 0) {
			response.setField(HttpField.EXPIRES, 
					Instant.now().plusSeconds(maxAge));
		}
		// Check if sending is really required.
		Instant lastModified = Files.getLastModifiedTime(resourcePath)
				.toInstant().with(ChronoField.NANO_OF_SECOND, 0);
		Optional<Instant> modifiedSince = event.request()
				.findValue(HttpField.IF_MODIFIED_SINCE, Converters.DATE_TIME);
		IOSubchannel channel = event.firstChannel(IOSubchannel.class);
		if (modifiedSince.isPresent() 
				&& !lastModified.isAfter(modifiedSince.get())) {
			response.setStatus(HttpStatus.NOT_MODIFIED);
			channel.fire(new Response(response));
		} else {
			if ("text".equals(mediaType.topLevelType())) {
				mediaType = MediaType.builder().from(mediaType)
						.setParameter("charset", System.getProperty(
								"file.encoding", "UTF-8")).build();
			}
			response.setField(HttpField.CONTENT_TYPE, mediaType);
			response.setStatus(HttpStatus.OK);
			response.setMessageHasBody(true);
			response.setField(HttpField.LAST_MODIFIED, lastModified);
			channel.fire(new Response(response));
			channel.fire(new StreamFile(resourcePath, StandardOpenOption.READ));
		}
		event.stop();
	}

	/**
	 * Describes an association between a media range and a  
	 * maximum age in seconds. 
	 */
	public static class ValidityInfo {
		private MediaRange mediaRange;
		private long maxAge;

		/**
		 * @param mediaRange
		 * @param maxAge
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
