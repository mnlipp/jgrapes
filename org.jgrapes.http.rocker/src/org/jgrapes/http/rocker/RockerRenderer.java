/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.http.rocker;

import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;

import java.io.IOException;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.rocker.events.Render;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.ByteBufferOutputStream;

/**
 * 
 */
public class RockerRenderer extends Component {

	/**
	 * @param componentChannel
	 */
	public RockerRenderer(Channel componentChannel) {
		super(componentChannel);
	}

	@Handler
	public void onRender(Render event, IOSubchannel channel) 
			throws InterruptedException, IOException {
		ArrayOfByteArraysOutput data = event.model()
				.render(ArrayOfByteArraysOutput.FACTORY);
		
		HttpResponse response = event.request().response().get();
		response.setStatus(HttpStatus.OK);
		response.setHasPayload(true);
		String subtype;
		switch (data.getContentType()) {
		case HTML:
			subtype = "html";
			break;
		default:
			subtype = "plain";
		}
		response.setField(HttpField.CONTENT_TYPE,
		        MediaType.builder().setType("text", subtype)
		                .setParameter("charset", data.getCharset().toString()
		                		.toLowerCase()).build());
		channel.respond(new Response(response));
		
		ByteBufferOutputStream out = new ByteBufferOutputStream(
				channel, channel.responsePipeline());
		for (byte[] array: data.getArrays()) {
			out.write(array);
		}
		out.close();
	}
}
