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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.Connection;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.net.events.Accepted;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpCodec extends AbstractComponent {

	private Map<Connection, HttpRequestDecoder> decoders = new WeakHashMap<>();
	private Map<Connection, HttpResponseEncoder> encoders = new WeakHashMap<>();
	
	/**
	 * @param componentChannel
	 */
	public HttpCodec(Channel componentChannel) {
		super(componentChannel);
	}

	@Handler
	public void onAccepted(Accepted<ByteBuffer> event) {
		decoders.put(event.getConnection(), new HttpRequestDecoder());
		encoders.put(event.getConnection(), new HttpResponseEncoder());
	}
	
	@Handler
	public void onRead(Read<ByteBuffer> event) {
		try {
			HttpRequestDecoder httpDecoder 
				= decoders.get(event.getConnection());
			if (httpDecoder == null) {
				System.out.println("");
			}
			httpDecoder.decode(event.getBuffer());
			HttpRequest request = httpDecoder.decodedRequest();
			if (request != null) {
				fire((new Request(event.getConnection(), request)));
			}
		} catch (ProtocolException e) {
			HttpResponse response = new HttpResponse(e.getHttpVersion());
			response.setResponseCode(HttpStatus.BAD_REQUEST.getCode());
			response.setResponseMessage(e.getMessage());
			fire (new Response(event.getConnection(), response));
		}
	}
	
	@Handler
	public void onRequestCompleted(Request.Completed event) 
			throws InterruptedException {
		Request requestEvent = event.getCompleted();
		DataConnection<ByteBuffer> connection = requestEvent.getConnection();

		HttpResponse response = null;
		switch (requestEvent.get()) {
		case UNHANDLED:
			response = new HttpResponse
				(requestEvent.getRequest().getProtocol());
			response.setResponseStatus(HttpStatus.NOT_IMPLEMENTED);
			break;
		case RESOURCE_NOT_FOUND:
			response = new HttpResponse
				(requestEvent.getRequest().getProtocol());
			response.setResponseStatus(HttpStatus.NOT_FOUND);
			break;
		case RESOURCE_FOUND:
			return;
		}
		fire (new Response(connection, response));
	}
	
	@Handler
	public void onResponse(Response event) throws InterruptedException {
		DataConnection<ByteBuffer> connection = event.getConnection();
		HttpResponse response = event.getResponse();
		HttpResponseEncoder encoder = encoders.get(connection);
		EventPipeline pipeline = newEventPipeline();
		
		while (true) {
			ByteBuffer buffer = connection.acquireWriteBuffer();
			boolean more = encoder.encode(response, buffer);
			pipeline.add(new Write<>(connection, buffer), getChannel());
			if (!more) {
				break;
			}
		}
	}
}
