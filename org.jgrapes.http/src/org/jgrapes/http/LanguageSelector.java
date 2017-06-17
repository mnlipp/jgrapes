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

package org.jgrapes.http;

import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.types.Converters;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.Request;

/**
 * 
 */
public class LanguageSelector extends Component {

	private String cookieName = LanguageSelector.class.getName();
	
	/**
	 * 
	 */
	public LanguageSelector() {
	}

	/**
	 * @param componentChannel
	 */
	public LanguageSelector(Channel componentChannel) {
		super(componentChannel);
	}

	/**
	 * Sets the name of the cookie used to store the locale.
	 * 
	 * @param cookieName the cookie name to use
	 * @return the locale selector for easy chaining
	 */
	public LanguageSelector setCookieName(String cookieName) {
		this.cookieName = cookieName;
		return this;
	}

	/**
	 * Returns the cookie name.
	 * 
	 * @return the cookie name
	 */
	public String cookieName() {
		return cookieName;
	}

	/**
	 * Associates the event with a {@link Selector} object
	 * using `Selector.class` as association identifier.
	 * 
	 * @param event the event
	 */
	@Handler(priority=990)
	public void onRequest(Request event) {
		final HttpRequest request = event.httpRequest();
		Optional<Session> optSession 
			= (Optional<Session>)event.associated(Session.class);
		if (optSession.isPresent()) {
			// Using cached selector is fastest
			Session session = optSession.get();
			Selection selection = (Selection)session.get(Selection.class);
			if (selection != null) {
				selection.setCurrentEvent(event);
				event.setAssociated(Selection.class, selection);
				return;
			}
		}
		
		// Try to get locale from cookies
		Optional<String> locale = request.findValue(
		        HttpField.COOKIE, Converters.COOKIE_LIST)
		        .flatMap(cookies -> cookies.stream().filter(
		                cookie -> cookie.getName().equals(cookieName))
		                .findFirst().map(HttpCookie::getValue));
		if (locale.isPresent()) {
			try {
				Selection selection = new Selection(event, 
						Converters.LANGUAGE.fromFieldValue(locale.get()));
				optSession.ifPresent(session -> {
					session.put(Selection.class, selection); });
				event.setAssociated(Selection.class, selection);
				return;
			} catch(ParseException e) {
				// fall through
			}
		}
		
		
	}
	
	public class Selection {
		private WeakReference<Request> currentEvent;
		private Locale locale;

		/**
		 * @param currentEvent
		 */
		private Selection(Request currentEvent, Locale locale) {
			super();
			this.currentEvent = new WeakReference<>(currentEvent);
			this.locale = locale;
		}

		/**
		 * @param currentEvent the currentEvent to set
		 */
		private void setCurrentEvent(Request currentEvent) {
			this.currentEvent = new WeakReference<>(currentEvent);
		}

		/**
		 * Return the current locale.
		 * 
		 * @return the value;
		 */
		public Locale get() {
			return locale;
		}

		/**
		 * Updates the current locale.
		 * 
		 * @param locale
		 * @return
		 */
		public Selection set(Locale locale) {
			this.locale = locale;
			return this;
		}
	}
}
