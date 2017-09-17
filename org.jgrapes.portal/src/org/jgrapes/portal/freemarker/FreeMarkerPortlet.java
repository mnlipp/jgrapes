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
import org.jgrapes.portal.events.RenderPortletFromProvider.ContentProvider;

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
	 * Creates the request independant part of the freemarker model. The
	 * result is cached as unmodifiable map as it can safely be assumed
	 * that the render support does not change for a given portal.
	 * 
	 * @param renderSupport the render support from the portal
	 * @return the result
	 */
	protected Map<String,Object> freemarkerBaseModel(
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
	 * Build a freemarker model from the given base model, portlet model
	 * and the information associated with the channel.
	 * 
	 * @param baseModel the base model
	 * @param portletModel the portlet model
	 * @param channel the channel
	 * @return the model
	 */
	protected Map<String,Object> freemarkerModel(Map<String,Object> baseModel,
			PortletBaseModel portletModel, IOSubchannel channel) {
		final Map<String,Object> model = new HashMap<>(baseModel);
		model.put("portlet", portletModel);
		Locale locale = locale(channel);
		model.put("locale", locale);
		final ResourceBundle resourceBundle = resourceSupplier().apply(locale);
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
	
	protected ContentProvider newContentProvider(
			Template template, Map<String,Object> dataModel) {
		return new ContentProvider() {
			
			@Override
			public void writeTo(Writer out) throws IOException {
				try {
					template.process(dataModel, out);
				} catch (TemplateException e) {
					throw new IOException(e);
				}
			}
		};
	}
	
}
