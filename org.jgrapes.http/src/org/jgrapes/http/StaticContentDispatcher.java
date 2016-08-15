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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.EndOfResponse;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Eos;
import org.jgrapes.io.events.StreamFile;

/**
 * @author Michael N. Lipp
 */
public class StaticContentDispatcher extends Component {

	private Path prefix;
	private Path contentDirectory;
	private Map<IOSubchannel, Object> handling;
	
	/**
	 * @see Component#Component()
	 */
	public StaticContentDispatcher(Path prefix, Path contentDirectory) {
		this(Channel.SELF, prefix, contentDirectory);
	}

	/**
	 * @see Component#Component(Channel)
	 */
	public StaticContentDispatcher(Channel componentChannel, Path prefix,
	        Path contentDirectory) {
		super(componentChannel);
		this.prefix = prefix;
		this.contentDirectory = contentDirectory;
		handling = Collections.synchronizedMap(new WeakHashMap<>());
	}

	@Handler
	public void onGet(GetRequest event) throws ParseException, IOException {
		Path requestPath = Paths
		        .get(event.getRequest().getRequestUri().getPath());
		if (!requestPath.startsWith(prefix)) {
			return;
		}
		Path resourcePath = contentDirectory
		        .resolve(prefix.relativize(requestPath));
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
		IOSubchannel channel = event.firstChannel(IOSubchannel.class);
		handling.put(channel, null);
		String mimeTypeName = Files.probeContentType(resourcePath);
		if (mimeTypeName == null) {
			mimeTypeName = "application/octet-stream";
		}
		HttpMediaTypeField contentType = HttpMediaTypeField
		        .fromString(HttpField.CONTENT_TYPE, mimeTypeName);
		if (contentType.getBaseType().equals("text")) {
			contentType.setParameter("charset",
			        System.getProperty("file.encoding", "UTF-8"));
		}
		final HttpResponse response = event.getRequest().getResponse();
		response.setStatus(HttpStatus.OK);
		response.setMessageHasBody(true);
		response.setField(contentType);
		channel.fire (new Response(response));
		channel.fire(new StreamFile(resourcePath, StandardOpenOption.READ));
		event.stop();
	}

	@Handler
	public void onEof(Eos event) {
		IOSubchannel channel = event.firstChannel(IOSubchannel.class);
		if (!handling.containsKey(channel)) {
			return;
		}
		channel.fire (new EndOfResponse());
		handling.remove(channel);
	}
	
}
