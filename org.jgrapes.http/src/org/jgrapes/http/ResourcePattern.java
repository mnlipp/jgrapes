/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A resource pattern can be used to filter URIs. A pattern looks  
 * similar to a URI (<code>scheme://host:port/path</code>) with the 
 * following differences:
 * 
 *  * The <em>scheme</em> may be a single protocol name or a list
 *    of protocol names separated by commas, or an asterisk, which is matched
 *    by URIs with any scheme. The scheme part ({@code scheme://}) is optional 
 *    in a pattern. Omitting it is equivalent to specifying an asterisk.
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
 *    equivalent to specifying an asterisk for both host and port.
 *    
 *  * The optional path part consist of one or more path separated by commas.
 *  
 *    Each path consists of a sequence of names and asterisks separated
 *    by slashes or a vertical bar ("|"). A name must be matched by the 
 *    corresponding path element of filtered URIs, an asterisk is matched by 
 *    any corresponding path element (which, however, must exist in the 
 *    filtered URI). The final element in 
 *    the path of a pattern may be two asterisks ({@code **}), which matches 
 *    any remaining path elements in the filtered URI.
 *    
 *    Using a vertical bar instead of a slash separates the path in a
 *    prefix part and the rest. The number of prefix segments is the
 *    value returned by the match methods.
 *    
 *    If a path ends with a vertical bar and the URI matched with the
 *    path does not end with a slash, a slash is appended to the URI
 *    before matching. This causes both `/foo` and `/foo/` to match
 *    a path `/foo|`. This is usually intended. If you do want to
 *    treat `/foo` as a leaf, specify `/foo,/foo|` in your pattern.
 *
 */
@SuppressWarnings("PMD.GodClass")
public class ResourcePattern {

	@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
	private static Pattern resourcePattern = Pattern.compile(
			"^((?<proto>[^:]+|\\*)://)?" // Optional protocol (2)
			+ "(" // Start of optional host/port part
				+ "(?<host>([^\\[/\\|][^:/\\|]*)|(\\[[^\\]]*\\]))" // Host (4)
				+ "(:(?<port>([0-9]+)|(\\*)))?" // Optional port (8)
			+ ")?" // End of optional host/port
			+ "(?<path>[/\\|].*)?"); // Finally path (11)
	
	private final String pattern;
	private final String protocol;
	private final String host;
	private final String port;
	private final String path;
	private final String[][] pathPatternElements;
	private int[] prefixSegs;

	/**
	 * Creates a new resource pattern.
	 * 
	 * @param pattern the pattern to be used for matching
	 * @throws ParseException if an invalid pattern is specified
	 */
	@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
	public ResourcePattern(String pattern) throws ParseException {
		this.pattern = pattern;
		Matcher rpm = resourcePattern.matcher(pattern);
		if (!rpm.matches()) {
			throw new ParseException("Invalid pattern: " + pattern, 0);
		}
		protocol = rpm.group("proto");
		host = rpm.group("host");
		port = rpm.group("port");
		path = rpm.group("path");
		if (path == null) {
			pathPatternElements = new String[0][];
		} else {
			String[] paths = path.split(",");
			pathPatternElements = new String[paths.length][];
			prefixSegs = new int[paths.length];
			for (int i = 0; i < paths.length; i++) {
				@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
				List<String> segs = new ArrayList<>();
				prefixSegs[i] = 0;
				StringTokenizer tokenizer = new StringTokenizer(
						paths[i], "/|", true);
				while(tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
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
				if (paths[i].endsWith("/") || paths[i].endsWith("|")) {
					segs.add("");
				}
				pathPatternElements[i] = segs.toArray(new String[0]);
			}
		}
	}
	
	/**
	 * @return the pattern (string) that was used to create this 
	 * resource pattern.
	 */
	public String pattern() {
		return pattern;
	}
	
	/**
	 * @return the protocol value specified in the pattern or {@code null}
	 */
	public String protocol() {
		return protocol;
	}

	/**
	 * @return the host value specified in the pattern or {@code null}
	 */
	public String host() {
		return host;
	}

	/**
	 * @return the port value specified in the pattern or {@code null}
	 */
	public String port() {
		return port;
	}

	/**
	 * @return the path value specified in the pattern or {@code null}
	 */
	public String path() {
		return path;
	}

	/**
	 * Matches the given resource URI against the pattern.
	 * 
	 * @param resource the URI specifying the resource to match
	 * @return -1 if the resource does not match, else the number
	 * of prefix segments (which may be 0)
	 */
	@SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.NPathComplexity",
	        "PMD.CollapsibleIfStatements", "PMD.DataflowAnomalyAnalysis" })
	public int matches(URI resource) {
		if (protocol != null && !protocol.equals("*")) {
			if (resource.getScheme() == null) {
				return -1;
			}
			if (Arrays.stream(protocol.split(","))
			        .noneMatch(proto -> proto.equals(resource.getScheme()))) {
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

		String[] reqElements = PathSpliterator.stream(resource.getPath())
				.skip(1).toArray(size -> new String[size]);
		String[] reqElementsPlus = null; // Created lazily
		for (int pathIdx = 0; pathIdx < pathPatternElements.length; pathIdx++) {
			String[] pathPattern = pathPatternElements[pathIdx];
			if (prefixSegs[pathIdx] == pathPattern.length - 1
					&& lastIsEmpty(pathPattern)) {
				// Special case, pattern ends with vertical bar
				if (reqElementsPlus == null) {
					reqElementsPlus = reqElements;
					if (!lastIsEmpty(reqElementsPlus)) {
						reqElementsPlus = Arrays.copyOf(
								reqElementsPlus, reqElementsPlus.length + 1);
						reqElementsPlus[reqElementsPlus.length - 1] = "";
					}
				}
				if (matchPath(pathPattern, reqElementsPlus)) {
					return prefixSegs[pathIdx];
				}
			} else {
				if (matchPath(pathPattern, reqElements)) {
					return prefixSegs[pathIdx];
				}
			}
		}
		return -1;
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

	@SuppressWarnings("PMD.UseVarargs")
	private static boolean lastIsEmpty(String[] elements) {
		return elements.length > 0 && elements[elements.length-1].length() == 0;
	}
	
	@SuppressWarnings({ "PMD.UseVarargs", "PMD.DataflowAnomalyAnalysis",
	        "PMD.PositionLiteralsFirstInComparisons" })
	private boolean matchPath(String[] patternElements, String[] reqElements) {
		int pathIdx = 0;
		int reqIdx = 0;
		while (true) {
			if (pathIdx == patternElements.length) {
				return reqIdx == reqElements.length;
			}
			if (reqIdx == reqElements.length) {
				return false;
			}
			String matchElement = patternElements[pathIdx++];
			if ("**".equals(matchElement)) {
				return true;
			}
			String reqElement = reqElements[reqIdx++];
			if (!matchElement.equals("*") && !matchElement.equals(reqElement)) {
				return false;
			}
		}
		
	}
	
	/**
	 * Removes the given number of segments (and their trailing slashes)
	 * from the beginning of the path. Segments may be empty. This implies 
	 * that invoking this method with a path that starts with a
	 * slash, the first removed segment is the empty segment
	 * preceding the slash and the starting slash. Put differently, 
	 * invoking this method with an absolute path and 1 makes the path
	 * relative.
	 *
	 * @param path the path
	 * @param segments the number of segments to remove
	 * @return the result
	 */
	public static String removeSegments(String path, int segments) {
		return PathSpliterator.stream(path)
	        .skip(segments).collect(Collectors.joining("/"));
	}
	
	/**
	 * Splits the given path in a prefix with the given number of
	 * segments and the rest. Like {{@link #removeSegments(String, int)}
	 * but additionally returning the removed segments.
	 *
	 * @param path the path
	 * @param segments the number of segments in the prefi
	 * @return the prefix and the rest
	 */
	@SuppressWarnings({ "PMD.AssignmentInOperand",
	        "PMD.AvoidLiteralsInIfCondition" })
	public static String[] split(String path, int segments) {
		StringBuilder prefix = new StringBuilder();
		StringBuilder suffix = new StringBuilder();
		int[] count = { 0 };
		PathSpliterator.stream(path).forEach(seg -> {
			if (count[0]++ < segments) {
				if (count[0] > 1) {
					prefix.append('/');
				}
				prefix.append(seg);				
			} else {
				if (count[0] > segments + 1) {
					suffix.append('/');
				}
				suffix.append(seg);
			}
		});
		return new String[] { prefix.toString(), suffix.toString() };
	}
	
	/**
	 * If the URI matches, returns the path split according to 
	 * the matched pattern (see {@link #split(String, int)}).
	 *
	 * @param resource the resource
	 * @return the result.
	 */
	public Optional<String[]> splitPath(URI resource) {
		int matchRes = matches(resource);
		if (matchRes < 0) {
			return Optional.empty();
		}
		return Optional.of(split(resource.getPath(), matchRes + 1));
	}
	
	/**
	 * If the URI matches, returns the path without prefix as specified
	 * by the matched pattern (see {@link #split(String, int)}).
	 *
	 * @param resource the resource
	 * @return the result.
	 */
	@SuppressWarnings("PMD.AssignmentInOperand")
	public Optional<String> pathRemainder(URI resource) {
		int matchRes = matches(resource);
		if (matchRes < 0) {
			return Optional.empty();
		}
		int segments = matchRes + 1;
		StringBuilder suffix = new StringBuilder();
		int[] count = { 0 };
		PathSpliterator.stream(resource.getPath()).forEach(seg -> {
			if (count[0]++ >= segments) {
				if (count[0] > segments + 1) {
					suffix.append('/');
				}
				suffix.append(seg);
			}
		});
		return Optional.of(suffix.toString());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(30);
		builder.append("ResourcePattern [")
			.append(pattern)
			.append(']');
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public int hashCode() {
		@SuppressWarnings("PMD.AvoidFinalLocalVariable")
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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ResourcePattern other = (ResourcePattern) obj;
		if (pattern == null) {
			if (other.pattern != null) {
				return false;
			}
		} else if (!pattern.equals(other.pattern)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the segments of the path. If the path starts with a slash,
	 * an empty string is returned as first segment. If the path ends 
	 * with a slash, an empty string is returned as final segment.
	 */
	public static class PathSpliterator extends AbstractSpliterator<String> {
		private StringTokenizer tokenizer;
		private boolean pendingLeadingEmpty;
		private boolean pendingTrailingEmpty;

		/**
		 * Creates a new stream for the given path, using "/"
		 * as path separator.
		 * 
		 * @param path the path
		 */
		public static Stream<String> stream(String path) {
			return StreamSupport.stream(new PathSpliterator(path), false);
		}

		/**
		 * Creates a new stream for the given path, using 
		 * the characters from delimiters as seperators.
		 * 
		 * @param path the path
		 * @param delimiters the delimiters
		 */
		public static Stream<String> stream(String path, String delimiters) {
			return StreamSupport.stream(
					new PathSpliterator(path, delimiters), false);
		}
		
		/**
		 * Creates a new spliterator for the given path, using "/"
		 * as path separator.
		 * 
		 * @param path the path
		 */
		public PathSpliterator(String path) {
			this(path, "/");
		}
		
		/**
		 * Creates a new spliterator for the given path, using 
		 * the characters from delimiters as seperators.
		 * 
		 * @param path the path
		 * @param delimiters the delimiters
		 */
		public PathSpliterator(String path, String delimiters) {
			super(Long.MAX_VALUE, Spliterator.ORDERED 
					| Spliterator.IMMUTABLE);
			tokenizer = new StringTokenizer(path, delimiters);
			pendingLeadingEmpty = path.startsWith("/");
			pendingTrailingEmpty = path.endsWith("/");
		}

		/* (non-Javadoc)
		 * @see java.util.Spliterator#tryAdvance(java.util.function.Consumer)
		 */
		@Override
		public boolean tryAdvance(Consumer<? super String> consumer) {
			if (tokenizer == null) {
				return false;
			}
			if (pendingLeadingEmpty) {
				pendingLeadingEmpty = false;
				consumer.accept("");
				return true;
			}
			if (tokenizer.hasMoreTokens()) {
				consumer.accept(tokenizer.nextToken());
				return true;
			}
			tokenizer = null;
			if (pendingTrailingEmpty) {
				consumer.accept("");
				return true;
			}
			return false;
		}
		
	}
}
