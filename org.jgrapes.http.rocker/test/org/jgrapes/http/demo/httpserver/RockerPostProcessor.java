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

package org.jgrapes.http.demo.httpserver;

//import java.io.UnsupportedEncodingException;
//
//import org.jdrupes.httpcodec.protocols.http.HttpRequest;
//import org.jdrupes.httpcodec.util.FormUrlDecoder;
//import org.jgrapes.core.Channel;
//import org.jgrapes.core.Component;
//import org.jgrapes.core.annotation.Handler;
//import org.jgrapes.http.annotation.RequestHandler;
//import org.jgrapes.http.events.PostRequest;
//import org.jgrapes.http.rocker.events.Render;
//import org.jgrapes.io.ContextSupplier;
//import org.jgrapes.io.IOSubchannel;
//import org.jgrapes.io.events.Input;
//import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * 
 */
//public class RockerPostProcessor extends Component 
//	implements ContextSupplier<RockerPostProcessor.FormContext> {
//
//	protected static class FormContext {
//		public HttpRequest request;
//		public FormUrlDecoder fieldDecoder = new FormUrlDecoder();
//	}
//
//	/**
//	 * @param componentChannel
//	 */
//	public RockerPostProcessor(Channel componentChannel) {
//		super(componentChannel);
//	}
//
//	/* (non-Javadoc)
//	 * @see org.jgrapes.io.ContextSupplier#createContext()
//	 */
//	@Override
//	public FormContext createContext() {
//		return new FormContext();
//	}
//
//	@RequestHandler(patterns="/form1")
//	public void onPost(PostRequest event, IOSubchannel channel) {
//		channel.context(this, true).get().request = event.request();
//		event.stop();
//	}
//	
//	@Handler
//	public void onInput(Input<ManagedByteBuffer> event, IOSubchannel channel) 
//			throws InterruptedException, UnsupportedEncodingException {
//		channel.context(this).ifPresent(ctx -> {
//			ctx.fieldDecoder.addData(event.buffer().backingBuffer());
//			if (!event.isEndOfRecord()) {
//				return;
//			}
//			fire(new Render(ctx.request, SubmitResult.template(
//					ctx.fieldDecoder.fields())), channel);
//		});
//	}
//
//}
