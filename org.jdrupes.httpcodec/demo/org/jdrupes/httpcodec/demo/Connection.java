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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.stream.Collectors;

import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.RequestDecoder;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.util.FormUrlDecoder;

/**
 * @author Michael N. Lipp
 *
 */
public class Connection extends Thread {

	private SocketChannel channel;
	private ServerEngine<?, ?> engine;
	private ByteBuffer in;
	private ByteBuffer out;
	
	public Connection(SocketChannel channel) {
		this.channel = channel;
		ServerEngine<HttpRequest,HttpResponse> 
			serverEngine = new ServerEngine<>
				(new HttpRequestDecoder(), new HttpResponseEncoder());
		engine = serverEngine;
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
				while (in.hasRemaining()) {
					RequestDecoder.Result<?> decoderResult 
						= engine.decode(in, null, false);
					if (decoderResult.hasResponse()) {
						sendResponseWithoutBody(decoderResult.getResponse());
						break;
					}
					if (decoderResult.isHeaderCompleted()) {
						MessageHeader hdr = engine.currentRequest().get();
						if (hdr instanceof HttpRequest) {
							handleHttpRequest((HttpRequest) hdr);
						}
					}
				}
			}
		} catch (IOException | ProtocolException e) {
		}
	}

	private void handleHttpRequest(HttpRequest request) throws IOException {
		if (request.getMethod().equalsIgnoreCase("GET")) {
			if (request.getRequestUri().getPath().equals("/form")) {
				handleGetForm(request);
				return;
			}
			if (request.getRequestUri().getPath().equals("/echo")
					|| request.getRequestUri().getPath().startsWith("/echo/")) {
				handleEcho(request);
				return;
			}
		}
		if (request.getMethod().equalsIgnoreCase("POST")
				&& request.getRequestUri().getPath().equals("/form")) {
			handlePostForm(request);
			return;
		}
		// fall back
		HttpResponse response = request.getResponse().get()
				.setStatus(HttpStatus.NOT_FOUND).setMessageHasBody(true);
		HttpMediaTypeField media;
		try {
			media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "plain");
			media.setParameter("charset", "utf-8");
			response.setField(media);
		} catch (ParseException e) {
		}
		ByteBuffer body = ByteBuffer.wrap("Not Found".getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void handleGetForm(HttpRequest request) throws IOException {
		HttpResponse response = request.getResponse().get()
				.setStatus(HttpStatus.OK).setMessageHasBody(true);
		HttpMediaTypeField media;
		try {
			media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "html");
			media.setParameter("charset", "utf-8");
			response.setField(media);
		} catch (ParseException e) {
		}
		String form = "";
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
		        getClass().getResourceAsStream("form.html"), "utf-8"))) {
			form = in.lines().collect(Collectors.joining("\r\n"));
		}
		ByteBuffer body = ByteBuffer.wrap(form.getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void handlePostForm(HttpRequest request) throws IOException {
		HttpResponse response = request.getResponse().get();
		FormUrlDecoder fieldDecoder = new FormUrlDecoder();
		while (true) {
			out.clear();
			RequestDecoder.Result<?> decoderResult = null;
			try {
				decoderResult = engine.decode(in, out, false);
			} catch (ProtocolException e) {
				return;
			}
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
			response.setField(media);
		} catch (ParseException e) {
		}
		String data = "First name: " + fieldDecoder.getFields().get("firstname")
		        + "\r\n" + "Last name: "
		        + fieldDecoder.getFields().get("lastname");
		ByteBuffer body = ByteBuffer.wrap(data.getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void handleEcho(HttpRequest request) throws IOException {
		if (request.getField(HttpStringListField.class, "upgrade")
				.map(f -> f.containsIgnoreCase("websocket")).orElse(false)) {
			upgradeEcho(request);
			return;
		}
		HttpResponse response = request.getResponse().get()
				.setStatus(HttpStatus.OK).setMessageHasBody(true);
		HttpMediaTypeField media;
		try {
			media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "html");
			media.setParameter("charset", "utf-8");
			response.setField(media);
		} catch (ParseException e) {
		}
		String page = "";
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
		        getClass().getResourceAsStream("echo.html"), "utf-8"))) {
			page = in.lines().collect(Collectors.joining("\r\n"));
		}
		ByteBuffer body = ByteBuffer.wrap(page.getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void upgradeEcho(HttpRequest request) throws IOException {
		HttpResponse response = request.getResponse().get()
			.setStatus(HttpStatus.SWITCHING_PROTOCOLS)
			.setField((new HttpStringListField(HttpField.UPGRADE))
						.append("websocket"));
		sendResponse(response, null, true);
	}
	
	private void sendResponseWithoutBody(MessageHeader response)
	        throws IOException {
		@SuppressWarnings("unchecked")
		ServerEngine<HttpRequest,HttpResponse> httpEngine
			= (ServerEngine<HttpRequest,HttpResponse>)engine;
		httpEngine.encode((HttpResponse)response);
		out.clear();
		while (true) {
			Encoder.Result encoderResult = engine.encode(out);
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

	private void sendResponse(HttpResponse response, ByteBuffer in,
	        boolean endOfInput) throws IOException {
		@SuppressWarnings("unchecked")
		ServerEngine<HttpRequest,HttpResponse> httpEngine
			= (ServerEngine<HttpRequest,HttpResponse>)engine;
		httpEngine.encode((HttpResponse)response);
		out.clear();
		while (true) {
			Encoder.Result encoderResult = engine.encode(in, out, endOfInput);
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
