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

package org.jgrapes.portal;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.stream.StreamSupport;

import javax.activation.MimetypesFileTypeMap;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedCharBuffer;
import org.jgrapes.portal.events.JsonRequest;
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portal.events.RenderPortletResult;
import org.jgrapes.portal.events.SimpleRenderPortletResult;

/**
 * 
 */
public class PortalView extends Component {

	private Portal portal;
	private static ServiceLoader<ThemeProvider> themeLoader 
		= ServiceLoader.load(ThemeProvider.class);
	private static Configuration fmConfig = null;
	private static MimetypesFileTypeMap typesMap = new MimetypesFileTypeMap();

	private ThemeProvider themeProvider;
	private Map<String,Object> portalModel = new HashMap<>();

	private Map<IOSubchannel, WsConnection> openChannels = new WeakHashMap<>();
	
	/**
	 * @param componentChannel
	 */
	public PortalView(Portal portal, Channel componentChannel) {
		super(componentChannel);
		this.portal = portal;
		if (fmConfig == null) {
			fmConfig = new Configuration(Configuration.VERSION_2_3_26);
			fmConfig.setClassLoaderForTemplateLoading(
					getClass().getClassLoader(), "org/jgrapes/portal");
			fmConfig.setDefaultEncoding("utf-8");
			fmConfig.setTemplateExceptionHandler(
					TemplateExceptionHandler.RETHROW_HANDLER);
	        fmConfig.setLogTemplateExceptions(false);
		}
		setTheme("default");
		RequestHandler.Evaluator.add(this, "onGet", portal.prefix() + "/**");
		
		// Create portal model
		portalModel.put("resourceUrl", new TemplateMethodModelEx() {
			@Override
			public Object exec(@SuppressWarnings("rawtypes") List arguments)
					throws TemplateModelException {
				@SuppressWarnings("unchecked")
				List<TemplateModel> args = (List<TemplateModel>)arguments;
				if (!(args.get(0) instanceof SimpleScalar)) {
					throw new TemplateModelException("Not a string.");
				}
				return portal.prefix().resolve(
						((SimpleScalar)args.get(0)).getAsString()).getPath();
			}
		});
	}

	private void setTheme(String theme) {
		StreamSupport.stream(themeLoader.spliterator(), false)
			.filter(t -> t.providesTheme(theme)).findFirst()
			.ifPresent(t -> { themeProvider = t; });
	}
	
	@RequestHandler(dynamic=true)
	public void onGet(GetRequest event, IOSubchannel channel) 
			throws InterruptedException, IOException {
		String requestPath = event.requestUri().getPath();
		// Append trailing slash, if missing
		if ((requestPath + "/").equals(portal.prefix().getPath())) {
			requestPath = portal.prefix().getPath();
		}
		
		// Request for portal?
		if (!requestPath.startsWith(portal.prefix().getPath())) {
			return;
		}
		
		// Normalize and evaluate
		requestPath = portal.prefix()
				.relativize(URI.create(requestPath)).getPath();
		if (requestPath.isEmpty()) {
			if (event.request().findField(
					HttpField.UPGRADE, Converters.STRING_LIST)
					.map(f -> f.value().containsIgnoreCase("websocket"))
					.orElse(false)) {
				openChannels.put(channel, new WsConnection(channel));
				channel.respond(new WebSocketAccepted(event.requestUri(),
						event.request().response().get())).get();
				event.stop();
				return;
			}
			
			renderPortal(event, channel);
			return;
		}
		if (requestPath.startsWith("portal-resource/")) {
			sendPortalResource(event, channel, requestPath
					.substring("portal-resource/".length()));
			return;
		}
		if (requestPath.startsWith("theme-resource/")) {
			sendThemeResource(event, channel, requestPath
					.substring("theme-resource/".length()));
			return;
		}
	}

	private void renderPortal(GetRequest event, IOSubchannel channel) {
		HttpResponse response = event.request().response().get();
		MediaType mediaType = MediaType.builder().setType("text", "html")
				.setParameter("charset", "utf-8").build();
		response.setField(HttpField.CONTENT_TYPE, mediaType);
		response.setStatus(HttpStatus.OK);
		response.setHasPayload(true);
		channel.respond(new Response(response));
		try (Writer out = new OutputStreamWriter(new ByteBufferOutputStream(
				channel, channel.responsePipeline()), "utf-8")) {
			Template tpl = fmConfig.getTemplate("portal.ftlh");
			tpl.process(portalModel, out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		event.stop();
	}

	private void sendPortalResource(GetRequest event, IOSubchannel channel,
			String resource) {
		// Look for content
		InputStream in = this.getClass().getResourceAsStream(resource);
		if (in == null) {
			return;
		}
		
		// Send header
		HttpResponse response = event.request().response().get();
		prepareResourceResponse(response, event.requestUri());
		channel.respond(new Response(response));
		
		// Send content
		activeEventPipeline().executorService()
			.submit(new InputStreamPipeline(in, channel));
		
		// Done
		event.stop();
	}

	private void sendThemeResource(GetRequest event, IOSubchannel channel,
			String resource) {
		try {
			// Get resource
			InputStream resIn = themeProvider.getResourceAsStream(resource);
			
			// Send header
			HttpResponse response = event.request().response().get();
			prepareResourceResponse(response, event.requestUri());
			channel.respond(new Response(response));
		
			// Send content
			activeEventPipeline().executorService()
				.submit(new InputStreamPipeline(resIn, channel));
		
			// Done
			event.stop();
		} catch (ResourceNotFoundException e) {
			return;
		}
	}

	private void prepareResourceResponse(HttpResponse response, URI request) {
		// Get content type
		String mimeTypeName;
		try {
			// probeContentType is most advanced, but may fail if it tries
			// to look at the file's content (which doesn't exist).
			mimeTypeName = Files.probeContentType(Paths.get(request.getPath()));
		} catch (IOException e) {
			mimeTypeName = null;
		}
		if (mimeTypeName == null) {
			mimeTypeName = typesMap.getContentType(request.getPath());
		}
		MediaType mediaType = null;
		try {
			mediaType = Converters.MEDIA_TYPE.fromFieldValue(mimeTypeName);
		} catch (ParseException e) {
			// Cannot happen
		}
		
		// Set max age in cache-control header
		List<Directive> directives = new ArrayList<>();
		directives.add(new Directive("max-age", 600));
		response.setField(HttpField.CACHE_CONTROL, directives);

		// Send response 
		if ("text".equals(mediaType.topLevelType())) {
			mediaType = MediaType.builder().from(mediaType)
					.setParameter("charset", System.getProperty(
							"file.encoding", "UTF-8")).build();
		}
		response.setField(HttpField.CONTENT_TYPE, mediaType);
		response.setStatus(HttpStatus.OK);
		response.setHasPayload(true);
		response.setField(HttpField.LAST_MODIFIED, Instant.now());
	}

	@Handler
	public void onInput(Input<ManagedCharBuffer> event, IOSubchannel channel)
			throws IOException {
		WsConnection channelData = openChannels.get(channel);
		if (channelData == null) {
			return;
		}
		channelData.decode(event.buffer().backingBuffer(), event.isEndOfRecord());
	}
	
	@Handler
	public void onJsonRequest(JsonRequest event, IOSubchannel channel) {
		switch (event.method()) {
		case "portalReady": {
			LinkedIOSubchannel reqChannel 
				= new LinkedIOSubchannel(portal, channel);
			fire(new RenderPortlet(), reqChannel);
			break;
		}
		}
	}
	
	void renderPortletResult(RenderPortletResult event,
	        LinkedIOSubchannel channel) 
	        		throws InterruptedException, IOException {
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObject notification = null;
		if (event instanceof SimpleRenderPortletResult) {
			notification = factory.createObjectBuilder()
					.add("jsonrpc", "2.0").add("method", "updatePortlet")
					.add("params", factory.createArrayBuilder()
							.add(event.portletId())
							.add(event.title())
							.add(((SimpleRenderPortletResult)event).result()))
				.build();
		}
		IOSubchannel upstream = channel.upstreamChannel();
		@SuppressWarnings("resource")
		CharBufferWriter out = new CharBufferWriter(upstream, 
				upstream.responsePipeline()).suppressClose();
		Json.createWriter(out).write(notification);
		out.close();
	}
	
	@Handler
	public void onClosed(Closed event, IOSubchannel channel) {
		openChannels.remove(channel);
	}
	
	private class WsConnection {

		private WeakReference<IOSubchannel> channel;
		private PipedWriter decodeWriter;
		
		public WsConnection(IOSubchannel channel) throws IOException {
			this.channel = new WeakReference<>(channel);
		}

		public void decode(CharBuffer buffer, boolean last) throws IOException {
			if (decodeWriter == null) {
				decodeWriter = new PipedWriter();
				PipedReader reader = new PipedReader(
						decodeWriter, buffer.capacity());
				activeEventPipeline().executorService()
					.submit(new DecodeTask(reader, channel.get()));
			}
			decodeWriter.append(buffer);
			if (last) {
				decodeWriter.close();
				decodeWriter = null;
			}
		}
		
		private class DecodeTask implements Runnable {

			IOSubchannel channel;
			private Reader reader;
			
			public DecodeTask(Reader reader, IOSubchannel channel) {
				this.reader = reader;
				this.channel = channel;
			}

			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				try (Reader in = reader) {
					JsonReader reader = Json.createReader(in);
					fire(new JsonRequest(reader.readObject()), channel);
				} catch (IOException e) {
					// Shouldn't happen
				}
			}
			
		}
	}

//	private void renderTemplate(GetRequest request, IOSubchannel channel,
//			RockerRenderer rockerRenderer) throws InterruptedException, IOException {
//		HttpResponse response = request.request().response().get();
//		response.setStatus(HttpStatus.OK);
//		response.setHasPayload(true);
//		response.setField(HttpField.CONTENT_TYPE,
//		        MediaType.builder().setType("text", "html")
//		                .setParameter("charset", "utf-8").build());
//		channel.respond(new Response(response));
//		
//		ArrayOfByteArraysOutput data = rockerRenderer
//				.render(ArrayOfByteArraysOutput.FACTORY);
//		ByteBufferOutputStream out = new ByteBufferOutputStream(
//				channel, channel.responsePipeline());
//		for (byte[] array: data.getArrays()) {
//			out.write(array);
//		}
//		out.close();
//	}
	
}
