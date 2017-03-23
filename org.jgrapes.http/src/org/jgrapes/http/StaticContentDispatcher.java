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
import java.util.Arrays;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
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

	@RequestHandler(dynamic=true)
	public void onGet(GetRequest event) throws ParseException, IOException {
		int prefixSegs = resourcePattern.matches(event.getRequestUri());
		if (prefixSegs < 0) {
			return;
		}
		// Final wrapper for usage in closure
		final Path[] assembly = new Path[] { contentDirectory };
		Arrays.stream(event.getRequestUri().getPath().split("/"))
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
		// Check if sending is really required.
		Instant lastModified = Files.getLastModifiedTime(resourcePath)
				.toInstant().with(ChronoField.NANO_OF_SECOND, 0);
		Optional<Instant> modifiedSince = event.getRequest()
				.getValue(HttpField.IF_MODIFIED_SINCE, Converters.DATE_TIME);
		HttpResponse response = event.getRequest().getResponse().get();
		IOSubchannel channel = event.firstChannel(IOSubchannel.class);
		if (modifiedSince.isPresent() 
				&& !lastModified.isAfter(modifiedSince.get())) {
			response.setStatus(HttpStatus.NOT_MODIFIED);
			channel.fire(new Response(response));
		} else {
			String mimeTypeName = Files.probeContentType(resourcePath);
			if (mimeTypeName == null) {
				mimeTypeName = "application/octet-stream";
			}
			MediaType mediaType = Converters.MEDIA_TYPE
					.fromFieldValue(mimeTypeName);
			if ("text".equals(mediaType.getTopLevelType())) {
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

}
