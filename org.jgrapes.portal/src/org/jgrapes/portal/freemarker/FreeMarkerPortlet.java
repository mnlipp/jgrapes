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

package org.jgrapes.portal.freemarker;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.jgrapes.core.Channel;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.AbstractPortlet;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.RenderSupport;
import org.jgrapes.portal.events.RenderPortletRequestBase;

/**
 * 
 */
public abstract class FreeMarkerPortlet extends AbstractPortlet {

	private Configuration fmConfig = null;
	private Map<String,Object> fmModel = null;
	
	/**
	 * @param componentChannel
	 */
	public FreeMarkerPortlet(Channel componentChannel) {
		super(componentChannel);
	}

	protected Configuration freemarkerConfig() {
		if (fmConfig == null) {
			fmConfig = new Configuration(Configuration.VERSION_2_3_26);
			fmConfig.setClassLoaderForTemplateLoading(
					getClass().getClassLoader(), getClass().getPackage()
					.getName().replace('.', '/'));
			fmConfig.setDefaultEncoding("utf-8");
			fmConfig.setTemplateExceptionHandler(
					TemplateExceptionHandler.RETHROW_HANDLER);
	        fmConfig.setLogTemplateExceptions(false);
		}
		return fmConfig;
	}
	
	/**
	 * Creates the request independent part of the freemarker model. The
	 * result is cached as unmodifiable map as it can safely be assumed
	 * that the render support does not change for a given portal.
	 * 
	 * This model provides:
	 *  * The function `portletResource` that makes 
	 *    {@link RenderSupport#portletResource(String, java.net.URI)}
	 *    available in the template. The first argument is set to the name
	 *    of the portlet, only the second must be supplied when the function
	 *    is invoked in a template.
	 * 
	 * @param renderSupport the render support from the portal
	 * @return the result
	 */
	protected Map<String,Object> fmTypeModel(
			RenderSupport renderSupport) {
		if (fmModel == null) {
			fmModel = new HashMap<>();
			fmModel.put("portletResource", new TemplateMethodModelEx() {
				@Override
				public Object exec(@SuppressWarnings("rawtypes") List arguments)
						throws TemplateModelException {
					@SuppressWarnings("unchecked")
					List<TemplateModel> args = (List<TemplateModel>)arguments;
					if (!(args.get(0) instanceof SimpleScalar)) {
						throw new TemplateModelException("Not a string.");
					}
					return renderSupport.portletResource(
							FreeMarkerPortlet.this.getClass().getName(),
							PortalView.uriFromPath(
									((SimpleScalar)args.get(0)).getAsString()))
							.getRawPath();
				}
			});
			fmModel = Collections.unmodifiableMap(fmModel);
		}
		return fmModel;
	}

	/**
	 * Build a freemarker model holdiung the information associated 
	 * with the session.
	 * 
	 * This model provides:
	 *  * The `locale` (of type {@link Locale}).
	 *  * A function `_` that looks up the given key in the portlet's
	 *    resource bundle.
	 *    
	 * @param channel the channel
	 * @return the model
	 */
	protected Map<String,Object> fmSessionModel(IOSubchannel channel) {
		final Map<String,Object> model = new HashMap<>();
		Locale locale = locale(channel);
		model.put("locale", locale);
		final ResourceBundle resourceBundle = resourceBundle(locale);
		model.put("_", new TemplateMethodModelEx() {
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
	 * Build a freemarker model for the current request.
	 * 
	 * This model provides:
	 *  * The `event` property (of type {@link RenderPortletRequest}).
	 *  * The `portlet` property (of type {@link PortletBaseModel}).
	 *    
	 * @param event the event
	 * @param channel the channel
	 * @param portletModel the portlet model
	 * @return the model
	 */
	protected Map<String,Object> fmPortletModel(RenderPortletRequestBase event, 
			IOSubchannel channel, PortletBaseModel portletModel) {
		final Map<String,Object> model = new HashMap<>();
		model.put("event", event);
		model.put("portlet", portletModel);
		return model;
	}
	
	/**
	 * Build a freemarker model that combines {@link #fmTypeModel},
	 * {@link #fmSessionModel} and {@link #fmPortletModel}.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param portletModel the portlet model
	 * @return the model
	 */
	protected Map<String,Object> fmModel(RenderPortletRequestBase event,
			IOSubchannel channel, PortletBaseModel portletModel) {
		final Map<String,Object> model = fmSessionModel(channel);
		model.putAll(fmTypeModel(event.renderSupport()));
		model.putAll(fmPortletModel(event, channel, portletModel));
		return model;
	}
	
	/**
	 * Creates a reader that delivers the result of processing the given
	 * template with the given data.
	 * 
	 * @param template the template
	 * @param dataModel the data
	 * @return the stream with the resulting data
	 */
	public Reader templateProcessor(Template template, Object dataModel) {
		try {
			PipedReader reader = new PipedReader();
			Writer writer = new PipedWriter(reader);
			new Thread(new Runnable() {

				/* (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				@Override
				public void run() {
					try (Writer out = writer) {
						template.process(dataModel, out);
					} catch (TemplateException | IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
			return reader;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
