/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.http.freemarker;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.ResponseCreationSupport.MaxAgeCalculator;
import org.jgrapes.http.Session;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.ByteBufferOutputStream;

/**
 * A base class for components that generate responses to
 * HTTP requests which are based on a FreeMarker template.
 */
public class FreeMarkerRequestHandler extends Component {
	static final Pattern templatePattern 
		= Pattern.compile(".*\\.ftl\\.[a-z]+$");

	private ClassLoader contentLoader;
	private String contentPath;
	private String prefix;
	private Configuration fmConfig = null;
	private MaxAgeCalculator maxAgeCalculator = null;

	/**
	 * Instantiates a new free marker request handler.
	 * 
	 * The prefix is removed from the request paths before resolving
	 * them against the content root. A prefix must start with a
	 * slash and must not end with a slash.
	 *
	 * @param componentChannel the component channel
	 * @param contentLoader the content loader
	 * @param contentPath the content path
	 * @param prefix the prefix used in requests
	 */
	public FreeMarkerRequestHandler(Channel componentChannel, 
			ClassLoader contentLoader, String contentPath, String prefix) {
		super(componentChannel);
		this.contentLoader = contentLoader;
		if (contentPath.startsWith("/")) {
			contentPath = contentPath.substring(1);
		}
		if (contentPath.endsWith("/")) {
			contentPath = contentPath.substring(0, contentPath.length() - 1);
		}
		this.contentPath = contentPath;
		this.prefix = prefix;
	}

	/**
	 * @return the maxAgeCalculator
	 */
	public MaxAgeCalculator maxAgeCalculator() {
		return maxAgeCalculator;
	}

	/**
	 * Sets the {@link MaxAgeCalculator} for generating the `Cache-Control` 
	 * (`max-age`) header of the response. The default is `null`. This
	 * causes 0 to be provided for responses generated from templates and the 
	 * {@link ResponseCreationSupport#DEFAULT_MAX_AGE_CALCULATOR} to be
	 * used for static content.
	 * 
	 * @param maxAgeCalculator the maxAgeCalculator to set
	 */
	public void setMaxAgeCalculator(MaxAgeCalculator maxAgeCalculator) {
		this.maxAgeCalculator = maxAgeCalculator;
	}

	/**
	 * Removes the prefix specified in the constructor from the
	 * path in the request. Checks if the resulting path  
	 * ends with `*.ftl.*`. If so, processes the template with the
	 * {@link #sendProcessedTemplate(Request, IOSubchannel, String)} (which 
	 * uses {@link #fmSessionModel(Optional)}) and sends the result. 
	 * Else, tries to serve static content with the optionally 
	 * shortened path.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @throws ParseException the parse exception
	 */
	protected void doGet(Request event, IOSubchannel channel)
			throws ParseException {
		final HttpRequest request = event.httpRequest();
		URI subUri = ResponseCreationSupport.uriFromPath(prefix)
				.relativize(request.requestUri());
		if (subUri.equals(request.requestUri())) {
			return;
		}
		if (!templatePattern.matcher(event.requestUri().getPath()).matches()) {
			ResponseCreationSupport.sendStaticContent(
					event, channel, path -> contentLoader.getResource(
							FreeMarkerRequestHandler.this.contentPath 
							+ "/" + subUri.getPath()), maxAgeCalculator);
			return;
		}
		sendProcessedTemplate(event, channel, subUri.getPath());
	}

	/**
	 * Render a response using the given template.
	 *
	 * @param event
	 *            the event
	 * @param channel
	 *            the channel
	 * @param tpl
	 *            the template
	 */
	protected void sendProcessedTemplate(
			Request event, IOSubchannel channel, Template tpl) {
		// Prepare response
		HttpResponse response = event.httpRequest().response().get();
		MediaType mediaType = contentType(event.requestUri());
		response.setContentType(mediaType);

		// Send response 
		response.setStatus(HttpStatus.OK);
		response.setField(HttpField.LAST_MODIFIED, Instant.now());
		if (maxAgeCalculator == null) {
			ResponseCreationSupport.setMaxAge(response, 
					(req, mt) -> 0, event.httpRequest(), mediaType);
		} else {
			ResponseCreationSupport.setMaxAge(
					response, maxAgeCalculator, event.httpRequest(), mediaType);
		}
		event.stop();
		channel.respond(new Response(response));

		// Send content
		try (Writer out = new OutputStreamWriter(
		        new ByteBufferOutputStream(channel, channel.responsePipeline()),
		        "utf-8")) {
			Map<String, Object> model = fmSessionModel(
			        event.associated(Session.class));
			tpl.setLocale((Locale)model.get("locale"));
			tpl.process(model, out);
		} catch (IOException | TemplateException e) {
			// Too late to do anything about this (header was sent).
		}
	}

	/**
	 * Render a response using the template obtained from the config with
	 * {@link Configuration#getTemplate(String)} and the given path.
	 *
	 * @param event
	 *            the event
	 * @param channel
	 *            the channel
	 * @param path
	 *            the path
	 */
	protected void sendProcessedTemplate(
			Request event, IOSubchannel channel, String path) {
		try {
			// Get template (no need to continue if this fails).
			Template tpl = freemarkerConfig().getTemplate(path);
			sendProcessedTemplate(event, channel, tpl);
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Creates the configuration for freemarker template processing.
	 * 
	 * @return the configuration
	 */
	protected Configuration freemarkerConfig() {
		if (fmConfig == null) {
			fmConfig = new Configuration(Configuration.VERSION_2_3_26);
			fmConfig.setClassLoaderForTemplateLoading(
			        contentLoader, contentPath);
			fmConfig.setDefaultEncoding("utf-8");
			fmConfig.setTemplateExceptionHandler(
			        TemplateExceptionHandler.RETHROW_HANDLER);
			fmConfig.setLogTemplateExceptions(false);
		}
		return fmConfig;
	}

	/**
	 * Build a freemarker model holding the information associated with the
	 * session.
	 * 
	 * This model provides: 
	 * 
	 * * The `locale` (of type {@link Locale}). 
	 * 
	 * * The `resourceBundle` (of type {@link ResourceBundle}). 
	 * 
	 * * A function "`_`" that looks up the given key in the 
	 *   resource bundle.
	 * 
	 * @param session
	 *            the session
	 * @return the model
	 */
	protected Map<String, Object> fmSessionModel(Optional<Session> session) {
		final Map<String, Object> model = new HashMap<>();
		Locale locale = session.map(s -> s.locale()).orElse(Locale.getDefault());
		model.put("locale", locale);
		final ResourceBundle resourceBundle = resourceBundle(locale);
		model.put("resourceBundle", resourceBundle);
		model.put("_", new TemplateMethodModelEx() {
			@Override
			public Object exec(@SuppressWarnings("rawtypes") List arguments)
			        throws TemplateModelException {
				@SuppressWarnings("unchecked")
				List<TemplateModel> args = (List<TemplateModel>) arguments;
				if (!(args.get(0) instanceof SimpleScalar)) {
					throw new TemplateModelException("Not a string.");
				}
				String key = ((SimpleScalar) args.get(0)).getAsString();
				try {
					return resourceBundle.getString(key);
				} catch (MissingResourceException e) {
					// no luck
				}
				return key;
			}
		});
		return model;
	}

	/**
	 * Used to get the content type when generating a response with
	 * {@link #sendProcessedTemplate(Request, IOSubchannel, Template)}. May be
	 * overridden by derived classes. This implementation simply invokes
	 * {@link HttpResponse#contentType(URI)}.
	 *
	 * @param request the request
	 * @return the content type
	 */
	protected MediaType contentType(URI request) {
		return HttpResponse.contentType(request);
	}

	/**
	 * Provides a resource bundle for localization.
	 * The default implementation looks up a bundle using the
	 * package name plus "l10n" as base name.
	 * 
	 * @return the resource bundle
	 */
	protected ResourceBundle resourceBundle(Locale locale) {
		return ResourceBundle.getBundle(
			contentPath.replace('/', '.') + ".l10n", locale, 
			contentLoader, ResourceBundle.Control.getNoFallbackControl(
					ResourceBundle.Control.FORMAT_DEFAULT));
	}
}
