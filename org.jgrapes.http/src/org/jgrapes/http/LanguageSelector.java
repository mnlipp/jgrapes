/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.http;

import java.beans.ConstructorProperties;
import java.io.Serializable;
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
import org.jgrapes.core.Associator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.IOSubchannel;

/**
 * A component that attempts to derive information about language preferences
 * from requests in the specified scope (usually "/") and make them 
 * available as a {@link Selection} object associated with the request 
 * event using `Selection.class` as association identifier.
 * 
 * The component first checks if the event has an associated {@link Session}
 * and whether that session has a value with key `Selection.class`. If
 * such an entry exists, its value is assumed to be a {@link Selection} object
 * which is (re-)used for all subsequent operations. Else, a new
 * {@link Selection} object is created (and associated with the session, if
 * a session exists).
 * 
 * If the {@link Selection} represents explicitly set values, it is used as
 * result (i.e. as object associated with the event by `Selection.class`).
 * 
 * Else, the selector tries to derive the language preferences from the
 * request. It first checks for a cookie with the specified name
 * (see {@link #cookieName()}). If a cookie is found, its value is
 * used to set the preferred locales. If no cookie is found, 
 * the values derived from the `Accept-Language header` are set
 * as fall-backs.
 * 
 * Whenever the language preferences 
 * change (see {@link Selection#prefer(Locale)}), a cookie with the
 * specified name and a path value set to the scope is created or
 * updated accordingly. 
 */
public class LanguageSelector extends Component {

    private String path;
    private static final DefaultMultiValueConverter<List<Locale>,
            Locale> LOCALE_LIST = new DefaultMultiValueConverter<>(
                ArrayList<Locale>::new, Converters.LANGUAGE);
    private String cookieName = LanguageSelector.class.getName();

    /**
     * Creates a new language selector component with its channel set to
     * itself and the scope set to "/".
     */
    public LanguageSelector() {
        this("/");
    }

    /**
     * Creates a new language selector component with its channel set to
     * itself and the scope set to the given value.
     * 
     * @param scope the scope
     */
    public LanguageSelector(String scope) {
        this(Channel.SELF, scope);
    }

    /**
     * Creates a new language selector component with its channel set 
     * to the given channel and the scope to "/".
     * 
     * @param componentChannel the component channel
     */
    public LanguageSelector(Channel componentChannel) {
        this(componentChannel, "/");
    }

    /**
     * Creates a new language selector component with its channel set 
     * to the given channel and the scope to the given scope.
     * 
     * @param componentChannel the component channel
     * @param scope the scope
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public LanguageSelector(Channel componentChannel, String scope) {
        super(componentChannel);
        if ("/".equals(scope) || !scope.endsWith("/")) {
            path = scope;
        } else {
            path = scope.substring(0, scope.length() - 1);
        }
        String pattern;
        if ("/".equals(path)) {
            pattern = "/**";
        } else {
            pattern = path + "," + path + "/**";
        }
        RequestHandler.Evaluator.add(this, "onRequest", pattern);
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
     * Returns the cookie name. Defaults to the class name.
     * 
     * @return the cookie name
     */
    public String cookieName() {
        return cookieName;
    }

    /**
     * Associates the event with a {@link Selection} object
     * using `Selection.class` as association identifier.
     * 
     * @param event the event
     */
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.EmptyCatchBlock" })
    @RequestHandler(priority = 990, dynamic = true)
    public void onRequest(Request event) {
        @SuppressWarnings("PMD.AccessorClassGeneration")
        final Selection selection = event.associated(Session.class)
            .map(session -> (Selection) session.computeIfAbsent(
                Selection.class, newKey -> new Selection(cookieName, path)))
            .orElseGet(() -> new Selection(cookieName, path));
        selection.setCurrentEvent(event);
        event.setAssociated(Selection.class, selection);
        if (selection.isExplicitlySet()) {
            return;
        }

        // Try to get locale from cookies
        final HttpRequest request = event.httpRequest();
        Optional<String> localeNames = request.findValue(
            HttpField.COOKIE, Converters.COOKIE_LIST)
            .flatMap(cookieList -> cookieList.valueForName(cookieName));
        if (localeNames.isPresent()) {
            try {
                List<Locale> cookieLocales = LOCALE_LIST
                    .fromFieldValue(localeNames.get());
                if (!cookieLocales.isEmpty()) {
                    Collections.reverse(cookieLocales);
                    cookieLocales.stream()
                        .forEach(locale -> selection.prefer(locale));
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
                .map(value -> value.value()).toArray(Locale[]::new);
            selection.updateFallbacks(locales);
        }
    }

    /**
     * Handles a procotol switch by associating the language selection
     * with the channel.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 1000)
    public void onProtocolSwitchAccepted(
            ProtocolSwitchAccepted event, IOSubchannel channel) {
        event.requestEvent().associated(Selection.class)
            .ifPresent(
                selection -> channel.setAssociated(Selection.class, selection));
    }

    /**
     * Convenience method to retrieve a locale from an associator.
     * 
     * @param assoc the associator
     * @return the locale
     */
    public static Locale associatedLocale(Associator assoc) {
        return assoc.associated(Selection.class)
            .map(sel -> sel.get()[0]).orElse(Locale.getDefault());
    }

    /**
     * Represents a locale selection.
     */
    @SuppressWarnings("serial")
    public static class Selection implements Serializable {
        private transient WeakReference<Request> currentEvent;
        private final String cookieName;
        private final String cookiePath;
        private boolean explicitlySet;
        private Locale[] locales;

        @ConstructorProperties({ "cookieName", "cookiePath" })
        private Selection(String cookieName, String cookiePath) {
            this.cookieName = cookieName;
            this.cookiePath = cookiePath;
            this.currentEvent = new WeakReference<>(null);
            explicitlySet = false;
            locales = new Locale[] { Locale.getDefault() };
        }

        /**
         * Gets the cookie name.
         *
         * @return the cookieName
         */
        public String getCookieName() {
            return cookieName;
        }

        /**
         * Gets the cookie path.
         *
         * @return the cookiePath
         */
        public String getCookiePath() {
            return cookiePath;
        }

        /**
         * Checks if is explicitly set.
         *
         * @return the explicitlySet
         */
        public boolean isExplicitlySet() {
            return explicitlySet;
        }

        /**
         * 
         * @param locales
         */
        @SuppressWarnings("PMD.UseVarargs")
        private void updateFallbacks(Locale[] locales) {
            if (explicitlySet) {
                return;
            }
            this.locales = Arrays.copyOf(locales, locales.length);
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
            return Arrays.copyOf(locales, locales.length);
        }

        /**
         * Updates the current locale.
         * 
         * @param locale the locale
         * @return the selection for easy chaining
         */
        public Selection prefer(Locale locale) {
            explicitlySet = true;
            List<Locale> list = new ArrayList<>(Arrays.asList(locales));
            list.remove(locale);
            list.add(0, locale);
            this.locales = list.toArray(new Locale[0]);
            HttpCookie localesCookie = new HttpCookie(cookieName,
                LOCALE_LIST.asFieldValue(list));
            localesCookie.setPath(cookiePath);
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

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(50);
            builder.append("Selection [");
            if (locales != null) {
                builder.append("locales=");
                builder.append(Arrays.toString(locales));
                builder.append(", ");
            }
            builder.append("explicitlySet=")
                .append(explicitlySet)
                .append(']');
            return builder.toString();
        }

    }
}
