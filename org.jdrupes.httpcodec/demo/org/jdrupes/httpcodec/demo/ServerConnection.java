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
package org.jdrupes.httpcodec.demo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.ParseException;

import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.util.FormUrlDecoder;

/**
 * @author Michael N. Lipp
 *
 */
public class ServerConnection extends Thread {

	private SocketChannel channel;
	private HttpRequestDecoder decoder;
	private HttpRequestDecoder.Result decoderResult;
	private HttpResponseEncoder encoder;
	private HttpResponseEncoder.Result encoderResult;
	private ByteBuffer in;
	private ByteBuffer out;
	
	public ServerConnection(SocketChannel channel) {
		this.channel = channel;
		decoder = new HttpRequestDecoder();
		encoder = new HttpResponseEncoder();
		in = ByteBuffer.allocate(2048);
		out = ByteBuffer.allocate(2048);
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try (SocketChannel channel = this.channel) {
			while (channel.isOpen()) {
				in.clear();
				channel.read(in);
				in.flip();
				decoderResult = decoder.decode(in, null);
				if (decoderResult.hasResponse()) {
					sendResponseHeader(decoderResult.getResponse());
					break;
				}
				if (decoderResult.isHeaderCompleted()) {
					handleRequest();
				}
				if (decoderResult.getCloseConnection()) {
					break;
				}
			}
		} catch (IOException e) {
		}
	}

	private void handleRequest() throws IOException {
		HttpRequest request = decoder.getHeader();
		if (request.getMethod().equalsIgnoreCase("GET")
				&& request.getRequestUri().getPath().equals("/form")) {
			handleGetForm(request);
			return;
		}
		if (request.getMethod().equalsIgnoreCase("POST")
				&& request.getRequestUri().getPath().equals("/form")) {
			handlePostForm(request);
			return;
		}
		// fall back
		HttpResponse response = decoder.getHeader().getResponse();
		response.setStatus(HttpStatus.NOT_FOUND);
		response.setMessageHasBody(true);
		HttpMediaTypeField media;
		try {
			media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "plain");
			media.setParameter("charset", "utf-8");
			response.setHeader(media);
		} catch (ParseException e) {
		}
		sendResponseHeader(response);
		ByteBuffer body = ByteBuffer.wrap("Not Found".getBytes("utf-8"));
		sendResponseBody(body);
		sendResponseBody(null);
	}

	private void handleGetForm(HttpRequest request) throws IOException {
		HttpResponse response = decoder.getHeader().getResponse();
		response.setStatus(HttpStatus.OK);
		response.setMessageHasBody(true);
		HttpMediaTypeField media;
		try {
			media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "html");
			media.setParameter("charset", "utf-8");
			response.setHeader(media);
		} catch (ParseException e) {
		}
		sendResponseHeader(response);
		String form = "<!DOCTYPE html>"
		        + "<html>"
		        + "<body>"
		        + ""
		        + "<form method=\"post\">"
		        + "  First name:<br>"
		        + "  <input type=\"text\" name=\"firstname\">"
		        + "  <br>"
		        + "  Last name:<br>"
		        + "  <input type=\"text\" name=\"lastname\">"
		        + "  <input type=\"submit\" value=\"Submit\">"
		        + "</form>"
		        + ""
		        + "</body>"
		        + "</html>";
		ByteBuffer body = ByteBuffer.wrap(form.getBytes("utf-8"));
		sendResponseBody(body);
		sendResponseBody(null);
	}

	private void handlePostForm(HttpRequest request) throws IOException {
		HttpResponse response = decoder.getHeader().getResponse();
		FormUrlDecoder fieldDecoder = new FormUrlDecoder();
		while (true) {
			out.clear();
			decoderResult = decoder.decode(in, out);
			out.flip();
			fieldDecoder.addData(out);
			if (decoderResult.isOverflow()) {
				continue;
			}
			if (decoderResult.isUnderflow()) {
				in.clear();
				channel.read(in);
				in.flip();
				continue;
			}
			break;
		}
		response.setStatus(HttpStatus.OK);
		response.setMessageHasBody(true);
		HttpMediaTypeField media;
		try {
			media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "plain");
			media.setParameter("charset", "utf-8");
			response.setHeader(media);
		} catch (ParseException e) {
		}
		sendResponseHeader(response);
		String data = "First name: " + fieldDecoder.getFields().get("firstname")
		        + "\r\n" + "Last name: "
		        + fieldDecoder.getFields().get("lastname");
		ByteBuffer body = ByteBuffer.wrap(data.getBytes("utf-8"));
		sendResponseBody(body);
		sendResponseBody(null);
	}

	private void sendResponseHeader(HttpResponse response) throws IOException {
		encoder.encode(response);
		out.clear();
		while (true) {
			encoderResult = encoder.encode(out);
			out.flip();
			if (out.hasRemaining()) {
				channel.write(out);
				out.clear();
			}
			if (encoderResult.isOverflow()) {
				continue;
			}
			break;
		}
	}

	private void sendResponseBody(ByteBuffer in) throws IOException {
		while (true) {
			encoderResult = encoder.encode(in, out);
			out.flip();
			if (out.hasRemaining()) {
				channel.write(out);
				out.clear();
			}
			if (encoderResult.isOverflow()) {
				continue;
			}
			break;
		}
	}

}
