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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.text.Collator;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import javax.json.Json;
import javax.json.JsonReader;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionLock;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.Session;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedCharBuffer;
import org.jgrapes.portal.events.JsonInput;
import org.jgrapes.portal.events.JsonOutput;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.SetLocale;
import org.jgrapes.portal.events.SetTheme;
import org.jgrapes.portal.themes.base.Provider;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * 
 */
public class PortalView extends Component {

	private Portal portal;
	private ServiceLoader<ThemeProvider> themeLoader 
		= ServiceLoader.load(ThemeProvider.class);
	private static Configuration fmConfig = null;
	
	private Function<Locale,ResourceBundle> resourceSupplier;
	private Set<Locale> supportedLocales;
	
	private ThemeProvider baseTheme;
	private Map<String,Object> portalBaseModel = new HashMap<>();
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
		
		supportedLocales = new HashSet<>();
		for (Locale locale: Locale.getAvailableLocales()) {
			if (locale.getLanguage().equals("")) {
				continue;
			}
			if (resourceSupplier != null) {
				ResourceBundle rb = resourceSupplier.apply(locale);
				if (rb.getLocale().equals(locale)) {
					supportedLocales.add(locale);
				}
			}
			ResourceBundle rb = ResourceBundle.getBundle(getClass()
					.getPackage().getName()	+ ".l10n", locale);
			if (rb.getLocale().equals(locale)) {
				supportedLocales.add(locale);
			}
		}
		
		RequestHandler.Evaluator.add(this, "onGet",	portal.prefix() + "**");
		RequestHandler.Evaluator.add(this, "onGetRedirect",
				portal.prefix().getPath().substring(
						0, portal.prefix().getPath().length() - 1));
		
		// Create portal model
		portalBaseModel.put("resourceUrl", new TemplateMethodModelEx() {
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
		
		portalBaseModel.put("themeInfos", 
				StreamSupport.stream(themeLoader.spliterator(), false)
				.map(t -> new ThemeInfo(t.themeId(), t.themeName()))
				.sorted().toArray(size -> new ThemeInfo[size]));
		
		portalBaseModel = Collections.unmodifiableMap(portalBaseModel);

		// Handlers attached to the portal side channel
		Handler.Evaluator.add(this, "onPortalReady", portal.channel());
		Handler.Evaluator.add(this, "onKeyValueStoreData", portal.channel());
		Handler.Evaluator.add(
				this, "onPortletResourceResponse", portal.channel());
		Handler.Evaluator.add(this, "onJsonOutput", portal.channel());
		Handler.Evaluator.add(this, "onSetLocale", portal.channel());
		Handler.Evaluator.add(this, "onSetTheme", portal.channel());
	}

	void setResourceSupplier(
			Function<Locale,ResourceBundle> resourceSupplier) {
		this.resourceSupplier = resourceSupplier;
	}
	
	RenderSupport renderSupport() {
		return renderSupport;
	}
	
	private LinkedIOSubchannel portalChannel(IOSubchannel channel) {
		@SuppressWarnings("unchecked")
		Optional<LinkedIOSubchannel> portalChannel
			= (Optional<LinkedIOSubchannel>)LinkedIOSubchannel
				.downstreamChannel(portal, channel);
		return portalChannel.orElseGet(
				() -> new LinkedIOSubchannel(portal, channel));
	}

	@RequestHandler(dynamic=true)
	public void onGetRedirect(GetRequest event, IOSubchannel channel) 
			throws InterruptedException, IOException, ParseException {
		HttpResponse response = event.httpRequest().response().get();
		response.setStatus(HttpStatus.MOVED_PERMANENTLY)
			.setContentType("text", "plain", "utf-8")
			.setField(HttpField.LOCATION, portal.prefix());
		fire(new Response(response), channel);
		try {
			fire(Output.wrap(portal.prefix().toString()
					.getBytes("utf-8"), true), channel);
		} catch (UnsupportedEncodingException e) {
			// Supported by definition
		}
		event.stop();
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
		
		// Because language is changed via websocket, locale cookie 
		// may be out-dated
		event.associated(Selection.class)
			.ifPresent(s ->	s.prefer(s.get()[0]));
		
		// Prepare response
		HttpResponse response = event.httpRequest().response().get();
		MediaType mediaType = MediaType.builder().setType("text", "html")
				.setParameter("charset", "utf-8").build();
		response.setField(HttpField.CONTENT_TYPE, mediaType);
		response.setStatus(HttpStatus.OK);
		response.setHasPayload(true);
		channel.respond(new Response(response));
		try (Writer out = new OutputStreamWriter(new ByteBufferOutputStream(
				channel, channel.responsePipeline()), "utf-8")) {
			Map<String,Object> portalModel = new HashMap<>(portalBaseModel);

			// Add locale
			final Locale locale = event.associated(Selection.class).map(
					s -> s.get()[0]).orElse(Locale.getDefault());
			portalModel.put("locale", locale);

			// Add supported locales
			final Collator coll = Collator.getInstance(locale);
			final Comparator<LanguageInfo> comp 
				= new Comparator<PortalView.LanguageInfo>() {
				@Override
				public int compare(LanguageInfo o1,  LanguageInfo o2) {
					return coll.compare(o1.getLabel(), o2.getLabel());
				}
			};
			LanguageInfo[] languages = supportedLocales.stream()
					.map(l -> new LanguageInfo(l))
					.sorted(comp).toArray(size -> new LanguageInfo[size]);
			portalModel.put("supportedLanguages", languages);

			// Add localization
			final ResourceBundle additionalResources = resourceSupplier == null
					? null : resourceSupplier.apply(locale);
			final ResourceBundle baseResources = ResourceBundle.getBundle(
					getClass().getPackage().getName() + ".l10n", locale,
					ResourceBundle.Control.getNoFallbackControl(
							ResourceBundle.Control.FORMAT_DEFAULT));
			portalModel.put("_", new TemplateMethodModelEx() {
				@Override
				public Object exec(@SuppressWarnings("rawtypes") List arguments)
						throws TemplateModelException {
					@SuppressWarnings("unchecked")
					List<TemplateModel> args = (List<TemplateModel>)arguments;
					if (!(args.get(0) instanceof SimpleScalar)) {
						throw new TemplateModelException("Not a string.");
					}
					String key = ((SimpleScalar)args.get(0)).getAsString();
					try {
						return additionalResources.getString(key);
					} catch (MissingResourceException e) {
						// try base resources
					}
					try {
						return baseResources.getString(key);
					} catch (MissingResourceException e) {
						// no luck
					}
					return key;
				}
			});
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
		optPortalInfo.get().toEvent(portalChannel(channel),
				event.buffer().backingBuffer(), event.isEndOfRecord());
	}
	
	@Handler(dynamic=true)
	public void onPortalReady(PortalReady event, IOSubchannel channel) {
		KeyValueStoreQuery query = new KeyValueStoreQuery(
				"/themeProvider", true);
		channel.setAssociated(this, new CompletionLock(event, 3000));
		fire(query, channel);
	}

	@Handler(dynamic=true)
	public void onKeyValueStoreData(
			KeyValueStoreData event, IOSubchannel channel) 
					throws JsonDecodeException {
		if (!event.event().query().equals("/themeProvider")) {
			return;
		}
		channel.associated(this, CompletionLock.class)
			.ifPresent(lock -> lock.remove());
		String themeId = event.data().values().iterator().next();
		ThemeProvider themeProvider = channel.associated(Session.class)
				.map(session -> (ThemeProvider)session.get("themeProvider"))
				.orElse(baseTheme);
		if (!themeProvider.themeId().equals(themeId)) {
			fire(new SetTheme(themeId), channel);
		}
	}
	
	@Handler(dynamic=true)
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

	@Handler(dynamic=true)
	public void onSetLocale(SetLocale event, LinkedIOSubchannel channel)
			throws InterruptedException, IOException {
		supportedLocales.stream()
			.filter(l -> l.equals(event.locale())).findFirst()
			.ifPresent(l ->	channel.associated(Selection.class)
					.map(s -> s.prefer(l)));
		fire(new JsonOutput("reload"), channel);
	}
	
	@Handler(dynamic=true)
	public void onSetTheme(SetTheme event, LinkedIOSubchannel channel)
			throws InterruptedException, IOException {
		ThemeProvider themeProvider = StreamSupport
			.stream(themeLoader.spliterator(), false)
			.filter(t -> t.themeId().equals(event.theme())).findFirst()
			.orElse(baseTheme);
		channel.associated(Session.class).ifPresent(session ->
					session.put("themeProvider", themeProvider));
		channel.respond(new KeyValueStoreUpdate().update(
				"/themeProvider", themeProvider.themeId())).get();
		fire(new JsonOutput("reload"), channel);
	}
	
	@Handler(dynamic=true)
	public void onJsonOutput(JsonOutput event, LinkedIOSubchannel channel)
			throws InterruptedException, IOException {
		IOSubchannel upstream = channel.upstreamChannel();
		@SuppressWarnings("resource")
		CharBufferWriter out = new CharBufferWriter(upstream, 
				upstream.responsePipeline()).suppressClose();
		event.toJson(out);
		out.close();
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
					fire(new JsonInput(reader.readObject()), channel);
				} catch (IOException e) {
					// Shouldn't happen
				}
			}
		}
	}

	public static class LanguageInfo {
		private Locale locale;

		/**
		 * @param locale
		 */
		public LanguageInfo(Locale locale) {
			this.locale = locale;
		}

		/**
		 * @return the locale
		 */
		public Locale getLocale() {
			return locale;
		}
		
		public String getLabel() {
			String str = locale.getDisplayName(locale);
			return Character.toUpperCase(str.charAt(0)) + str.substring(1);
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
