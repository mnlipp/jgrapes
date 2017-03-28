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
import java.net.InetSocketAddress;
import java.nio.file.Paths;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServerDemo extends Component {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) 
			throws IOException, InterruptedException {
		HttpServerDemo app = new HttpServerDemo();
		app.attach(new NioDispatcher());
		app.attach(new HttpServer(app.channel(), 
		        new InetSocketAddress(8888), GetRequest.class,
		        PostRequest.class));
		app.attach(new FileStorage(app.channel(), 65536));
		app.attach(new StaticContentDispatcher(app.channel(),
		        "/**", Paths.get("demo-resources/static-content")));
		app.attach(new StaticContentDispatcher(app.channel(),
		        "/doc|**", Paths.get("../../jgrapes.gh-pages/javadoc")));
		Components.start(app);
	}

}
