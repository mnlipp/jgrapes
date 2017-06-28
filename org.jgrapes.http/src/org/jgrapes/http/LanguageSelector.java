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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.DefaultMultiValueConverter;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.io.IOSubchannel;

/**
 * 
 */
public class LanguageSelector extends Component {

	private static final DefaultMultiValueConverter<List<Locale>, Locale> 
		LOCALE_LIST = new DefaultMultiValueConverter<>(
				ArrayList<Locale>::new, Converters.LANGUAGE);
	private String cookieName = LanguageSelector.class.getName();
	
	/**
	 * Creates a new language selector component with its channel set to
	 * itself.
	 */
	public LanguageSelector() {
	}

	/**
	 * Creates a new language selector component with its channel set 
	 * to the given channel.
	 * 
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
		final Selection selection = event.associated(Session.class)
				.map(session -> (Selection)session.computeIfAbsent(
					Selection.class, k -> new Selection()))
				.orElseGet(Selection::new);
		selection.setCurrentEvent(event);
		event.setAssociated(Selection.class, selection);
		if (selection.isExplicitlySet()) {
			return;
		}
		
		// Try to get locale from cookies
		Optional<String> localeNames = request.findValue(
				HttpField.COOKIE, Converters.COOKIE_LIST)
				.flatMap(cl -> cl.valueForName(cookieName));
		if (localeNames.isPresent()) {
			try {
				List<Locale> cookieLocales = LOCALE_LIST
						.fromFieldValue(localeNames.get());
				if (cookieLocales.size() > 0) {
					Collections.reverse(cookieLocales);
					cookieLocales.stream().forEach(l -> selection.prefer(l));
					return;
				}
			} catch (ParseException e) {
				// Unusable
			}
		}

		// Last resport: Accept-Language header field
		Optional<List<ParameterizedValue<Locale>>> accepted = request.findValue(
				HttpField.ACCEPT_LANGUAGE, Converters.LANGUAGE_LIST);
		if (accepted.isPresent()) {
			Locale[] locales = accepted.get().stream()
				.sorted(ParameterizedValue.WEIGHT_COMPARATOR)
				.map(pv -> pv.value()).toArray(Locale[]::new);
			selection.updateFallbacks(locales);
		}
	}
	
	@Handler
	public void onWebSocketAccepted(
			WebSocketAccepted event, IOSubchannel channel) {
		event.requestEvent().associated(Selection.class)
			.ifPresent(selection -> 
				channel.setAssociated(Selection.class, selection));
	}
	
	public class Selection {
		private WeakReference<Request> currentEvent;
		private boolean explicitlySet;
		private Locale[] locales = new Locale[0];

		/**
		 */
		private Selection() {
			this.currentEvent = new WeakReference<>(null);
			explicitlySet = false;
		}

		/**
		 * @return the explicitlySet
		 */
		public boolean isExplicitlySet() {
			return explicitlySet;
		}

		/**
		 * 
		 * @param locales
		 */
		private void updateFallbacks(Locale[] locales) {
			if (explicitlySet) {
				return;
			}
			this.locales = locales;
		}
		
		/**
		 * @param currentEvent the currentEvent to set
		 */
		private Selection setCurrentEvent(Request currentEvent) {
			this.currentEvent = new WeakReference<>(currentEvent);
			return this;
		}

		/**
		 * Return the current locale.
		 * 
		 * @return the value;
		 */
		public Locale[] get() {
			return locales;
		}

		/**
		 * Updates the current locale.
		 * 
		 * @param locale
		 * @return
		 */
		public Selection prefer(Locale locale) {
			explicitlySet = true;
			List<Locale> list = new ArrayList<>(Arrays.asList(locales));
			list.remove(locale);
			list.add(0, locale);
			this.locales = list.toArray(new Locale[list.size()]);
			HttpCookie localesCookie = new HttpCookie(cookieName,
					LOCALE_LIST.asFieldValue(list));
			Request req = currentEvent.get();
			if (req != null) {
				req.httpRequest().response().ifPresent(resp -> {
					resp.computeIfAbsent(
						HttpField.SET_COOKIE, CookieList::new)
					.value().add(localesCookie);
				});
			}
			return this;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Selection [");
			if (locales != null) {
				builder.append("locales=");
				builder.append(Arrays.toString(locales));
				builder.append(", ");
			}
			builder.append("explicitlySet=");
			builder.append(explicitlySet);
			builder.append("]");
			return builder.toString();
		}
		
	}
}
