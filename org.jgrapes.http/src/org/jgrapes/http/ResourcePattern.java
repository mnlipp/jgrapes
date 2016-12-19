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
package org.jgrapes.http;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael N. Lipp
 */
public class ResourcePattern {

	private static Pattern resourcePattern = Pattern.compile
			("^(([^:]+|\\*)://)?" // Optional protocol (2)
			+ "(" // Start of optional host/port part
				+ "(([^\\[/][^:/]*)|(\\[[^\\]]*\\]))" // Host (4)
				+ "(:(([0-9]+)|(\\*)))?" // Optional port (8)
			+ ")?" // End of optional host/port
			+ "(/.*)?"); // Finally path (11)
	
	private String protocol;
	private String host;
	private String port;
	private String path;

	ResourcePattern(String pattern) throws ParseException {
		Matcher m = resourcePattern.matcher(pattern);
		if (!m.matches()) {
			throw new ParseException("Invalid pattern: " + pattern, 0);
		}
		protocol = m.group(2);
		host = m.group(4);
		port = m.group(8);
		path = m.group(11);
	}
	
	/**
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	public boolean matches(URI resource) {
		if (protocol != null && !protocol.equals("*")) {
			if (resource.getScheme() == null) {
				return false;
			}
			if (Arrays.stream(protocol.split(","))
			        .noneMatch(p -> p.equals(resource.getScheme()))) {
				return false;
			}
		}
		if (host != null && !host.equals("*")) {
			if (resource.getHost() == null
			        || !resource.getHost().equals(host)) {
				return false;
			}
		}
		if (port != null && !port.equals("*")) {
			if (Integer.parseInt(port) != resource.getPort()) {
				return false;
			}
		}
		StringTokenizer patternElements = new StringTokenizer(path, "/");
		StringTokenizer reqElements = new StringTokenizer(resource.getPath(),
		        "/");
		while (true) {
			if (!patternElements.hasMoreTokens()) {
				return !reqElements.hasMoreElements();
			}
			String matchElement = patternElements.nextToken();
			if (matchElement.equals("**")) {
				return true;
			}
			if (!reqElements.hasMoreTokens()) {
				return false;
			}
			String reqElement = reqElements.nextToken();
			if (!matchElement.equals("*") && !matchElement.equals(reqElement)) {
				return false;
			}
		}
	}
	
	public static boolean matches(String pattern, URI resource)
	        throws ParseException {
		return (new ResourcePattern(pattern)).matches(resource);
	}
}
