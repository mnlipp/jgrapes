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
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletFromProvider.ContentProvider;

/**
 * 
 */
public class AbstractPortlet extends Component {

	private Configuration fmConfig = null;
	private Map<String,Object> fmModel = null;
	
	/**
	 * 
	 */
	public AbstractPortlet() {
	}

	/**
	 * @param componentChannel
	 */
	public AbstractPortlet(Channel componentChannel) {
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
	
	protected Map<String,Object> freemarkerModel(RenderSupport renderSupport) {
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
							AbstractPortlet.this.getClass().getName(),
							PortalView.uriFromPath(
									((SimpleScalar)args.get(0)).getAsString()))
							.getRawPath();
				}
			});
		}
		return fmModel;
	}
	
	@Handler
	public void onResourceRequest(
			PortletResourceRequest event, IOSubchannel channel) {
		// For me?
		if (!event.portletType().equals(getClass().getName())) {
			return;
		}
		
		// Look for content
		InputStream in = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (in == null) {
			return;
		}

		// Respond
		channel.respond(new PortletResourceResponse(event, in));
		
		// Done
		event.setResult(true);
	}

	protected ContentProvider newContentProvider(
			Template template, Object dataModel) {
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
