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
package org.jgrapes.io.demo.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Request.HandlingResult;
import org.jgrapes.io.NioDispatcher;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServerDemo extends AbstractComponent {

	@Handler
	public void onGet(GetRequest event) {
		event.setResult(HandlingResult.RESOURCE_NOT_FOUND);
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
				new InetSocketAddress(8888)));
		Utils.start(app);
	}

}
