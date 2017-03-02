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
 * A resource pattern can be used to filter URIs. A pattern looks  
 * similar to a URI (<code>scheme://host:port/paths</code>) with the 
 * following differences:
 * <ul>
 *   <li>The <em>scheme</em> may be a single protocol name or a list
 *   of protocol names separated by commas, or an asterisk, which is matched
 *   by URIs with any scheme. The scheme part ({@code scheme://}) is optional 
 *   in a pattern. Omitting it is equivalent to specifiying an asterisk.</li>
 *   <li>The <em>host</em> may be a host name or an IP address or an asterisk.
 *   Unless the value is an asterisk, filtered URIs must match the value
 *   literally.</li>
 *   <li>The <em>port</em> may be a number or an asterisk.
 *   Unless the value is an asterisk, filtered URIs must match it.</li>
 *   <li>Specifying a port ({@code :port}) is optional. If omitted, 
 *   it is equivalent to specifying an asterisk.</li>
 *   <li>If the scheme part is omitted, the {@code host:port} part may 
 *   completely be left out as well, which is
 *   equivalent to specifiying an asterisk for both host and port.</li>
 *   <li>The paths consist of one or more path separated by commas.
 *   Each path consists of a sequence of names and asterisks separated
 *   by slashes. A name must be matched by the corresponding path element of 
 *   filtered URIs, an asterisk is matched by any corresponding path element
 *   (which, however, must exist in the filtered URI). The final element in 
 *   the path of a pattern may be two asterisks ({@code **}), which matches 
 *   any remaining path elements in the filtered URI.</li>
 * </ul>
 * 
 * @author Michael N. Lipp
 */
public class ResourcePattern {

	private static Pattern resourcePattern = Pattern.compile
			("^((?<proto>[^:]+|\\*)://)?" // Optional protocol (2)
			+ "(" // Start of optional host/port part
				+ "(?<host>([^\\[/][^:/]*)|(\\[[^\\]]*\\]))" // Host (4)
				+ "(:(?<port>([0-9]+)|(\\*)))?" // Optional port (8)
			+ ")?" // End of optional host/port
			+ "(?<path>/.*)?"); // Finally path (11)
	
	private String protocol;
	private String host;
	private String port;
	private String path;
	private String[][] pathPatternElements;

	/**
	 * Creates a new resource pattern.
	 * 
	 * @param pattern the pattern to be used for matching
	 * @throws ParseException if an invalid pattern is specified
	 */
	ResourcePattern(String pattern) throws ParseException {
		Matcher m = resourcePattern.matcher(pattern);
		if (!m.matches()) {
			throw new ParseException("Invalid pattern: " + pattern, 0);
		}
		protocol = m.group("proto");
		host = m.group("host");
		port = m.group("port");
		path = m.group("path");
		if (path == null) {
			pathPatternElements = new String[0][];
		} else {
			String[] paths = path.split(",");
			pathPatternElements = new String[paths.length][];
			for (int i = 0; i < paths.length; i++) {
				pathPatternElements[i] = paths[i].split("/");
			}
		}
	}
	
	/**
	 * @return the protocol value specified in the pattern or {@code null}
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * @return the host value specified in the pattern or {@code null}
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the port value specified in the pattern or {@code null}
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @return the path value specified in the pattern or {@code null}
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Matches the given resource URI against the pattern.
	 * 
	 * @param resource the URI specifying the resource to match
	 * @return {@code true} if the resource URI matches
	 */
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
		String[] reqElements = resource.getPath().split("/");
		for (int pathIdx = 0; pathIdx < pathPatternElements.length; pathIdx++) {
			if (matchPath(pathPatternElements[pathIdx], reqElements)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchPath(String[] patternElements, String[] reqElements) {
		int pIdx = 0;
		int rIdx = 0;
		while (true) {
			if (pIdx == patternElements.length) {
				return rIdx == reqElements.length;
			}
			String matchElement = patternElements[pIdx++];
			if (matchElement.equals("**")) {
				return true;
			}
			if (rIdx == reqElements.length) {
				return false;
			}
			String reqElement = reqElements[rIdx++];
			if (!matchElement.equals("*") && !matchElement.equals(reqElement)) {
				return false;
			}
		}
		
	}
	
	/**
	 * Matches the given pattern against the given resource URI.
	 * 
	 * @param pattern the pattern to match
	 * @param resource the URI specifying the resource to match
	 * @return {@code true} if the resource URI matches
	 * @throws ParseException if an invalid pattern is specified
	 */
	public static boolean matches(String pattern, URI resource)
	        throws ParseException {
		return (new ResourcePattern(pattern)).matches(resource);
	}
}
