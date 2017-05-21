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
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Component;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.ByteBufferOutputStream;

/**
 * 
 */
public class PortalView extends Component {

	private static ServiceLoader<ThemeProvider> themeLoader 
		= ServiceLoader.load(ThemeProvider.class);
	private static Configuration fmConfig = null;

	private Portal portal;
	private ThemeProvider themeProvider;
	
	/**
	 * @param componentChannel
	 */
	public PortalView(Portal portal) {
		super(portal.channel());
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
		try {
			URI resSrc = this.getClass().getResource("").toURI();
			if ("jar".equals(resSrc.getScheme())
					|| "zip".equals(resSrc.getScheme())) {
				// Make compressed file available as file system.
				Map<String, String> env = new HashMap<>(); 
				env.put("create", "true");
				FileSystems.newFileSystem(resSrc, env);
			}
			attach(new StaticContentDispatcher(channel(),
					portal.prefix() + "/portal-resource|**", resSrc));
		} catch (URISyntaxException e) {
			// Cannot happen.
		} catch (IOException e) {
			// Cannot happen.
		}
	}

	private void setTheme(String theme) {
		StreamSupport.stream(themeLoader.spliterator(), false)
			.filter(t -> t.providesTheme(theme)).findFirst()
			.ifPresent(t -> { themeProvider = t; });
	}
	
	@RequestHandler(dynamic=true)
	public void onGet(GetRequest event, IOSubchannel channel) 
			throws InterruptedException, IOException {
		if (event.requestUri().getPath().equals(portal.prefix())) {
			renderPortal(event, channel);
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
			Map<String,Object> dataModel = new HashMap<>();
			Template tpl = fmConfig.getTemplate("portal.ftlh");
			tpl.process(dataModel, out);
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
