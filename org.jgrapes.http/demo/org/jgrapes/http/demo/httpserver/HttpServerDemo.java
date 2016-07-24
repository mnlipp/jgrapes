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
package org.jgrapes.http.demo.httpserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.text.ParseException;

import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.internal.Codec.HttpStatus;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.events.EndOfResponse;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.Write;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServerDemo extends Component {

	@Handler
	public void onGet(GetRequest event) throws ParseException {
		if (!event.getRequest().getRequestUri().getPath().equals("/form")) {
			return;
		}
		final HttpResponse response = event.getRequest().getResponse();
		final DataConnection connection = event.getConnection();
		response.setStatus(HttpStatus.OK);
		response.setMessageHasBody(true);
		HttpMediaTypeField media = new HttpMediaTypeField(
		        HttpField.CONTENT_TYPE, "text", "html");
		media.setParameter("charset", "utf-8");
		response.setHeader(media);
		(new Response(connection, response)).fire();
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
		try {
			Write.wrap(connection, form.getBytes("utf-8")).fire();
		} catch (UnsupportedEncodingException e) {
		}
		(new EndOfResponse(connection)).fire();
		event.stop();
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) 
			throws IOException, InterruptedException {
		HttpServerDemo app = new HttpServerDemo();
		app.attach(new NioDispatcher());
		app.attach(new HttpServer(app.getChannel(), 
		        new InetSocketAddress(8888), GetRequest.class,
		        PostRequest.class));
		Components.start(app);
	}

}
