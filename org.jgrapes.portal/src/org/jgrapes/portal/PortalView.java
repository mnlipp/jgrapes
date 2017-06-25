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
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedCharBuffer;
import org.jgrapes.portal.Portlet.RenderMode;
import org.jgrapes.portal.events.AddPortletResources;
import org.jgrapes.portal.events.ChangePortletModel;
import org.jgrapes.portal.events.JsonRequest;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletFromProvider;
import org.jgrapes.portal.events.RenderPortletFromString;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.themes.base.Provider;

/**
 * 
 */
public class PortalView extends Component {

	private Portal portal;
	private static ServiceLoader<ThemeProvider> themeLoader 
		= ServiceLoader.load(ThemeProvider.class);
	private static Configuration fmConfig = null;

	private ThemeProvider baseTheme;
	private Map<String,Object> portalModel = new HashMap<>();
	private RenderSupport renderSupport = new RenderSupportImpl();

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
		baseTheme = new Provider();
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
						((SimpleScalar)args.get(0)).getAsString()).getRawPath();
			}
		});
		
		portalModel.put("themeInfos", 
				StreamSupport.stream(themeLoader.spliterator(), false)
				.map(t -> new ThemeInfo(t.themeId(), t.themeName()))
				.sorted().toArray(size -> new ThemeInfo[size]));
	}

	private LinkedIOSubchannel portalChannel(IOSubchannel channel) {
		Optional<? extends LinkedIOSubchannel> portalChannel
			= LinkedIOSubchannel.downstreamChannel(portal, channel);
		if (portalChannel.isPresent()) {
			return portalChannel.get();
		}
		LinkedIOSubchannel newPortalChannel
			= new LinkedIOSubchannel(portal, channel);
		return newPortalChannel;
	}
	
	@RequestHandler(dynamic=true)
	public void onGet(GetRequest event, IOSubchannel channel) 
			throws InterruptedException, IOException {
		URI requestUri = event.requestUri();
		// Append trailing slash, if missing
		if ((requestUri.getRawPath() + "/").equals(
				portal.prefix().getRawPath())) {
			requestUri = portal.prefix();
		}
		
		// Request for portal?
		if (!requestUri.getRawPath().startsWith(portal.prefix().getRawPath())) {
			return;
		}
		
		// Normalize and evaluate
		requestUri = portal.prefix().relativize(
				URI.create(requestUri.getRawPath()));
		if (requestUri.getRawPath().isEmpty()) {
			if (event.httpRequest().findField(
					HttpField.UPGRADE, Converters.STRING_LIST)
					.map(f -> f.value().containsIgnoreCase("websocket"))
					.orElse(false)) {
				channel.setAssociated(this, new PortalInfo());
				channel.respond(new WebSocketAccepted(event));
				event.stop();
				return;
			}
			
			renderPortal(event, channel);
			return;
		}
		URI subUri = uriFromPath("portal-resource/").relativize(requestUri);
		if (!subUri.equals(requestUri)) {
			sendPortalResource(event, channel, subUri.getPath());
			return;
		}
		subUri = uriFromPath("theme-resource/").relativize(requestUri);
		if (!subUri.equals(requestUri)) {
			sendThemeResource(event, channel, subUri.getPath());
			return;
		}
		subUri = uriFromPath("portlet-resource/").relativize(requestUri);
		if (!subUri.equals(requestUri)) {
			requestPortletResource(event, channel, subUri);
			return;
		}
	}

	private void renderPortal(GetRequest event, IOSubchannel channel)
		throws IOException, InterruptedException {
		event.stop();
		HttpResponse response = event.httpRequest().response().get();
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
		} catch (TemplateException e) {
			throw new IOException(e);
		}
	}

	private void sendPortalResource(GetRequest event, IOSubchannel channel,
			String resource) {
		// Look for content
		InputStream in = this.getClass().getResourceAsStream(resource);
		if (in == null) {
			return;
		}
		
		// Send header
		HttpResponse response = event.httpRequest().response().get();
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
			ThemeProvider themeProvider = event.associated(Session.class)
					.map(session -> (ThemeProvider)session.get("themeProvider"))
					.orElse(baseTheme);
			InputStream resIn;
			try {
				resIn = themeProvider.getResourceAsStream(resource);
			} catch (ResourceNotFoundException e) {
				resIn = baseTheme.getResourceAsStream(resource);
			}
			
			// Send header
			HttpResponse response = event.httpRequest().response().get();
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

	public static void prepareResourceResponse(
			HttpResponse response, URI request) {
		response.setContentType(request);
		// Set max age in cache-control header
		List<Directive> directives = new ArrayList<>();
		directives.add(new Directive("max-age", 600));
		response.setField(HttpField.CACHE_CONTROL, directives);
		response.setField(HttpField.LAST_MODIFIED, Instant.now());

		response.setStatus(HttpStatus.OK);
	}

	private void requestPortletResource(GetRequest event, IOSubchannel channel,
			URI resource) throws InterruptedException {
		String resPath = resource.getPath();
		int sep = resPath.indexOf('/');
		// Send events to portlets on portal's channel
		if (Boolean.TRUE.equals(newEventPipeline().fire(
				new PortletResourceRequest(resPath.substring(0, sep), 
						uriFromPath(resPath.substring(sep + 1)),
						event.httpRequest(), channel), portalChannel(channel))
				.get())) {
			event.stop();
		}
	}
	
	@Handler
	public void onInput(Input<ManagedCharBuffer> event, IOSubchannel channel)
			throws IOException {
		Optional<PortalInfo> optPortalInfo 
			= channel.associated(this, PortalInfo.class);
		if (!optPortalInfo.isPresent()) {
			return;
		}
		optPortalInfo.get().toEvent(channel,
				event.buffer().backingBuffer(), event.isEndOfRecord());
	}
	
	@Handler
	public void onJsonRequest(JsonRequest event, IOSubchannel channel) 
			throws InterruptedException, IOException {
		Optional<PortalInfo> optPortalInfo 
			= channel.associated(this, PortalInfo.class);
		if (!optPortalInfo.isPresent()) {
			return;
		}
		// Send events to portlets on portal's channel
		LinkedIOSubchannel portalChannel = portalChannel(channel);
		switch (event.method()) {
		case "portalReady": {
			fire(new PortalReady(renderSupport), portalChannel);
			break;
		}
		case "renderPortlet": {
			JsonArray params = (JsonArray)event.params();
			fire(new RenderPortletRequest(renderSupport, params.getString(0),
					RenderMode.valueOf(params.getString(1))), portalChannel);
			break;
		}
		case "setTheme": {
			JsonArray params = (JsonArray)event.params();
			setTheme(channel, params.getString(0));
			sendNotificationResponse(portalChannel, "reload");
			break;
		}
		case "sendToPortlet": {
			JsonArray params = (JsonArray)event.params();
			fire(new ChangePortletModel(renderSupport, params.getString(0),
					params.getString(1), params.size() <= 2
					? JsonValue.EMPTY_JSON_ARRAY : params.getJsonArray(2)), 
					portalChannel);
			break;
		}
		}		
	}
	
	private void setTheme(IOSubchannel channel, String theme) {
		StreamSupport.stream(themeLoader.spliterator(), false)
			.filter(t -> t.themeId().equals(theme)).findFirst()
			.ifPresent(themeProvider ->  
				channel.associated(Session.class).map(session ->
					session.put("themeProvider", themeProvider)));
	}
	
	void onAddPortletResources(
			AddPortletResources event, LinkedIOSubchannel channel)
					throws InterruptedException, IOException {
		sendNotificationResponse(channel, "addPortletResources",
				Arrays.stream(event.cssUris()).map(uri -> 
				renderSupport.portletResource(
						event.portletType(), uri).toString())
				.toArray(String[]::new),
				Arrays.stream(event.scriptUris()).map(uri -> 
				renderSupport.portletResource(event.portletType(), uri).toString())
				.toArray(String[]::new));
	}
	
	void onRenderPortlet(
			RenderPortletFromString event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		sendNotificationResponse(channel, "updatePortlet",
				event.portletId(), event.title(), event.renderMode().name(),
				event.supportedRenderModes().stream().map(RenderMode::name)
				.toArray(size -> new String[size]),
				((RenderPortletFromString)event).content());
	}

	void onRenderPortlet(
			RenderPortletFromProvider event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		StringWriter content = new StringWriter();
		event.provider().writeTo(content);
		sendNotificationResponse(channel, "updatePortlet",
				event.portletId(), event.title(), event.renderMode().name(),
				event.supportedRenderModes().stream().map(RenderMode::name)
				.toArray(size -> new String[size]),
				content.toString());
	}

	public void onPortletResourceResponse(PortletResourceResponse event,
	        LinkedIOSubchannel channel) {
		HttpRequest request = event.request().httpRequest();
		// Send header
		HttpResponse response = request.response().get();
		prepareResourceResponse(response, request.requestUri());
		channel.upstreamChannel().respond(new Response(response));
		
		// Send content
		activeEventPipeline().executorService().submit(
				new InputStreamPipeline(
						event.stream(), channel.upstreamChannel()));
	}

	private void sendNotificationResponse(LinkedIOSubchannel channel,
	        String method, Object... params)
	        		throws InterruptedException, IOException {
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder notification = factory.createObjectBuilder()
				.add("jsonrpc", "2.0").add("method", method);
		if (params.length > 0) {
			notification.add("params", toJsonArray(factory, null, params));
		}
		IOSubchannel upstream = channel.upstreamChannel();
		@SuppressWarnings("resource")
		CharBufferWriter out = new CharBufferWriter(upstream, 
				upstream.responsePipeline()).suppressClose();
		Json.createWriter(out).write(notification.build());
		out.close();
	}

	private JsonArrayBuilder toJsonArray(JsonBuilderFactory factory,
			JsonArrayBuilder array, Object item) {
		if (item instanceof Object[]) {
			JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
			for (Object nested: (Object[])item) {
				toJsonArray(factory, arrayBuilder, nested);
			}
			if (array == null) {
				return arrayBuilder;
			}
			array.add(arrayBuilder);
			return array;
		}
		if (array == null) {
			array = factory.createArrayBuilder();
		}
		array.add(item.toString());
		return array;
	}

	private class PortalInfo {

		private PipedWriter decodeWriter;
		
		public void toEvent(IOSubchannel channel, CharBuffer buffer,
				boolean last) throws IOException {
			if (decodeWriter == null) {
				decodeWriter = new PipedWriter();
				PipedReader reader = new PipedReader(
						decodeWriter, buffer.capacity());
				activeEventPipeline().executorService()
					.submit(new DecodeTask(reader, channel));
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

	public static class ThemeInfo implements Comparable<ThemeInfo> {
		private String id;
		private String name;
		
		/**
		 * @param id
		 * @param name
		 */
		public ThemeInfo(String id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
		
		/**
		 * @return the id
		 */
		public String id() {
			return id;
		}
		
		/**
		 * @return the name
		 */
		public String name() {
			return name;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(ThemeInfo other) {
			return name().compareToIgnoreCase(other.name());
		}
	}
	
	/**
	 * Create a {@link URI} from a path. This is similar to calling
	 * `new URI(null, null, path, null)` with the {@link URISyntaxException}
	 * converted to a {@link IllegalArgumentException}.
	 * 
	 * @param path the path
	 * @return the uri
	 * @throws IllegalArgumentException if the string violates 
	 * RFC 2396
	 */
	public static URI uriFromPath(String path) throws IllegalArgumentException {
		try {
			return new URI(null, null, path, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private class RenderSupportImpl implements RenderSupport {

		/* (non-Javadoc)
		 * @see org.jgrapes.portal.RenderSupport#portletResource(java.lang.String, java.net.URI)
		 */
		@Override
		public URI portletResource(String portletType, URI uri) {
			return portal.prefix().resolve(uriFromPath(
					"portlet-resource/" + portletType + "/")).resolve(uri);
		}
	}

}
