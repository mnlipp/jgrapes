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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A resource pattern can be used to filter URIs. A pattern looks  
 * similar to a URI (<code>scheme://host:port/path</code>) with the 
 * following differences:
 * 
 *  * The <em>scheme</em> may be a single protocol name or a list
 *    of protocol names separated by commas, or an asterisk, which is matched
 *    by URIs with any scheme. The scheme part ({@code scheme://}) is optional 
 *    in a pattern. Omitting it is equivalent to specifiying an asterisk.
 *    
 *  * The <em>host</em> may be a host name or an IP address or an asterisk.
 *    Unless the value is an asterisk, filtered URIs must match the value
 *    literally.
 *    
 *  * The <em>port</em> may be a number or an asterisk.
 *    Unless the value is an asterisk, filtered URIs must match it.
 *   
 *  * Specifying a port ({@code :port}) is optional. If omitted, 
 *    it is equivalent to specifying an asterisk.
 *    
 *  * If the scheme part is omitted, the {@code host:port} part may 
 *    completely be left out as well, which is
 *    equivalent to specifiying an asterisk for both host and port.
 *    
 *  * The optional path part consist of one or more path separated by commas.
 *  
 *    Each path consists of a sequence of names and asterisks separated
 *    by slashes or a vertical bar ("|"). A name must be matched by the 
 *    corresponding path element of filtered URIs, an asterisk is matched by 
 *    any corresponding path element
 *    (which, however, must exist in the filtered URI). The final element in 
 *    the path of a pattern may be two asterisks ({@code **}), which matches 
 *    any remaining path elements in the filtered URI.
 *    
 *    Using a vertical bar instead of a slash separates the path in a
 *    prefix part and the rest. The number of prefix segments is the
 *    value returned by the match methods.
 *
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
	
	private String pattern;
	private String protocol;
	private String host;
	private String port;
	private String path;
	private String[][] pathPatternElements;
	private int[] prefixSegs;

	/**
	 * Creates a new resource pattern.
	 * 
	 * @param pattern the pattern to be used for matching
	 * @throws ParseException if an invalid pattern is specified
	 */
	public ResourcePattern(String pattern) throws ParseException {
		this.pattern = pattern;
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
			prefixSegs = new int[paths.length];
			for (int i = 0; i < paths.length; i++) {
				List<String> segs = new ArrayList<>();
				prefixSegs[i] = 0;
				StringTokenizer st = new StringTokenizer(paths[i], "/|", true);
				while(st.hasMoreTokens()) {
					String token = st.nextToken();
					switch (token) {
					case "/":
						continue;
					case "|":
						prefixSegs[i] = segs.size();
						continue;
					default:
						segs.add(token);
						continue;
					}
				}
				pathPatternElements[i] = segs.toArray(new String[segs.size()]);
			}
		}
	}
	
	/**
	 * @return the pattern (string) that was used to create this 
	 * resource pattern.
	 */
	public String getPattern() {
		return pattern;
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
	 * @return -1 if the resource does not match, else the number
	 * of prefix segments (which may be 0)
	 */
	public int matches(URI resource) {
		if (protocol != null && !protocol.equals("*")) {
			if (resource.getScheme() == null) {
				return -1;
			}
			if (Arrays.stream(protocol.split(","))
			        .noneMatch(p -> p.equals(resource.getScheme()))) {
				return -1;
			}
		}
		if (host != null && !host.equals("*")) {
			if (resource.getHost() == null
			        || !resource.getHost().equals(host)) {
				return -1;
			}
		}
		if (port != null && !port.equals("*")) {
			if (Integer.parseInt(port) != resource.getPort()) {
				return -1;
			}
		}
		
		String[] reqElements = Collections.list
				(new StringTokenizer(resource.getPath(), "/"))
				.toArray(new String[0]);
		for (int pathIdx = 0; pathIdx < pathPatternElements.length; pathIdx++) {
			if (matchPath(pathPatternElements[pathIdx], reqElements)) {
				return prefixSegs[pathIdx];
			}
		}
		return -1;
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
		return (new ResourcePattern(pattern)).matches(resource) >= 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResourcePattern [");
		builder.append(pattern);
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourcePattern other = (ResourcePattern) obj;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		return true;
	}
	
	
}
