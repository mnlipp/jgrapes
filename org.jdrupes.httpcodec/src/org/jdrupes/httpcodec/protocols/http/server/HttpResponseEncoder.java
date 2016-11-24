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
package org.jdrupes.httpcodec.protocols.http.server;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jdrupes.httpcodec.ResponseEncoder;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;
import org.jdrupes.httpcodec.protocols.http.HttpEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;

/**
 * @author Michael N. Lipp
 */
public class HttpResponseEncoder 
	extends HttpEncoder<HttpResponse, ResponseEncoder.Result>
	implements ResponseEncoder<HttpResponse>{

	private static ServiceLoader<ProtocolProvider> pluginLoader 
		= ServiceLoader.load(ProtocolProvider.class);
	private Map<String,ProtocolProvider> plugins = new HashMap<>();

	/**
	 * Creates a new encoder that belongs to the given HTTP engine.
	 */
	public HttpResponseEncoder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#startMessage(java.io.Writer)
	 */
	@Override
	protected void startMessage(HttpResponse response, Writer writer)
	        throws IOException {
		writer.write(response.getProtocol().toString());
		writer.write(" ");
		writer.write(Integer.toString(response.getStatusCode()));
		writer.write(" ");
		writer.write(response.getReasonPhrase());
		writer.write("\r\n");
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public ResponseEncoder.Result encode
		(Buffer in, ByteBuffer out, boolean endOfInput) {
		if ((messageHeader instanceof HttpResponse)
				&& ((HttpResponse)messageHeader).getStatusCode()
					== HttpStatus.SWITCHING_PROTOCOLS.getStatusCode()) {
			prepareSwitchProtocol((HttpResponse)messageHeader);
		}
		return super.encode(in, out, endOfInput);
	}

	private void prepareSwitchProtocol(HttpResponse response) {
		ProtocolProvider plugin = null;
		String protocol = response.getField(HttpStringListField.class, 
				HttpField.UPGRADE).map(l -> l.get(0)).orElse("(pass-through)");
		synchronized (pluginLoader) {
			if (plugins.containsKey(protocol)) {
				plugin = plugins.get(plugin);
			} else {
				plugin = StreamSupport
						.stream(pluginLoader.spliterator(), false)
						.filter(p -> p.supportsProtocol(protocol))
						.findFirst().get();
				plugins.put(protocol, plugin);
			}
		}
		if (plugin == null) {
			// TODO
		}
		plugin.augmentInitialResponse(response);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#newResult(boolean, boolean)
	 */
	@Override
	protected ResponseEncoder.Result newResult
		(boolean overflow, boolean underflow) {
		return new ResponseEncoder.Result(overflow, underflow, isClosed(),
				null, null, null);
	}

}
