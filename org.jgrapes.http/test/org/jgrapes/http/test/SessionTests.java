/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2022  Michael N. Lipp
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

package org.jgrapes.http.test;

import java.net.HttpCookie;
import java.net.URI;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.Session;
import org.jgrapes.http.SessionManager;
import org.jgrapes.http.events.DiscardSession;
import org.jgrapes.http.events.Request;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SessionTests {

    public static class App extends Component {

        public SessionManager sessionManager;
        public Request<?> lastRequest;

        public App(int absoluteTimeout, int idleTimeout) throws Exception {
            sessionManager = new InMemorySessionManager(channel())
                .setAbsoluteTimeout(absoluteTimeout)
                .setIdleTimeout(idleTimeout);
            attach(sessionManager);
            fire(new Start()).get();
        }

        @Handler
        public void onRequest(Request<?> event) {
            lastRequest = event;
        }
    }

    public static class TestResource implements AutoCloseable {

        public boolean closed = false;

        @Override
        public void close() throws Exception {
            closed = true;
        }

    }

    private HttpRequest createRequest() throws Exception {
        return new HttpRequest(
            "GET", new URI("/"), HttpProtocol.HTTP_1_0, false)
                .setHostAndPort("localhost", 8080)
                .setResponse(new HttpResponse(HttpProtocol.HTTP_1_0,
                    HttpStatus.NOT_IMPLEMENTED, false));
    }

    private String setSessionId(HttpRequest request, HttpResponse response) {
        String sessionId = response.findValue(
            HttpField.SET_COOKIE, Converters.SET_COOKIE)
            .get().stream().filter(cookie -> "id".equals(cookie.getName()))
            .findFirst().get().getValue();
        request.computeIfAbsent(HttpField.COOKIE,
            () -> (new CookieList()).add(new HttpCookie("id", sessionId)));
        return sessionId;
    }

    @Test
    public void testAbsoluteTimeout() throws Exception {
        // Create app
        App app = new App(1, 0);

        // Send first request
        HttpRequest request = createRequest();
        Request.In evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();
        assertNotNull(app.lastRequest);
        Request<?> firstRequest = app.lastRequest;
        assertTrue(firstRequest.associated(Session.class).isPresent());
        Session firstSession = firstRequest.associated(Session.class).get();
        firstSession.transientData().put("resource", new TestResource());

        // Request with the newly session id from first request
        setSessionId(request, request.response().get());
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequest = null;
        app.fire(evt).get();

        // Session must have been found again
        assertNotNull(app.lastRequest);
        assertTrue(app.lastRequest.associated(Session.class).isPresent());
        assertEquals(firstSession,
            app.lastRequest.associated(Session.class).get());

        // Request after absolute timeout
        Thread.sleep(1500);
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequest = null;
        app.fire(evt).get();
        assertTrue(((TestResource) firstSession.transientData()
            .get("resource")).closed);
        assertNotNull(app.lastRequest);
        Session newerSession = app.lastRequest.associated(Session.class).get();
        assertTrue(app.lastRequest.associated(Session.class).isPresent());
        assertNotEquals(firstSession, newerSession);
    }

    @Test
    public void testIdleTimeout() throws Exception {
        // Create app
        App app = new App(0, 1);

        // Send first request
        HttpRequest request = createRequest();
        Request.In evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();
        assertNotNull(app.lastRequest);
        Request<?> firstRequest = app.lastRequest;
        assertTrue(firstRequest.associated(Session.class).isPresent());
        Session firstSession = firstRequest.associated(Session.class).get();
        firstSession.transientData().put("resource", new TestResource());

        // Request with the newly session id from first request
        setSessionId(request, request.response().get());
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequest = null;
        app.fire(evt).get();

        // Session must have been found again
        assertNotNull(app.lastRequest);
        assertTrue(app.lastRequest.associated(Session.class).isPresent());
        assertEquals(firstSession,
            app.lastRequest.associated(Session.class).get());

        // Request after idle timeout
        Thread.sleep(1500);
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequest = null;
        app.fire(evt).get();
        assertTrue(((TestResource) firstSession.transientData()
            .get("resource")).closed);
        assertNotNull(app.lastRequest);
        Session newerSession = app.lastRequest.associated(Session.class).get();
        assertTrue(app.lastRequest.associated(Session.class).isPresent());
        assertNotEquals(firstSession, newerSession);
    }

    @Test
    public void testDiscard() throws Exception {
        // Create app
        App app = new App(0, 0);

        // Send first request
        HttpRequest request = createRequest();
        Request.In evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();

        // Session must have been created
        assertNotNull(app.lastRequest);
        Request<?> firstRequest = app.lastRequest;
        assertTrue(firstRequest.associated(Session.class).isPresent());
        Session firstSession = firstRequest.associated(Session.class).get();
        firstSession.transientData().put("resource", new TestResource());

        // Send discard event
        app.lastRequest = null;
        app.fire(new DiscardSession(firstSession)).get();

        // Must be closed
        assertTrue(((TestResource) firstSession.transientData()
            .get("resource")).closed);

        // Request again
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();
        Session newerSession = app.lastRequest.associated(Session.class).get();

        // New session must have been created
        assertNotEquals(null, app.lastRequest);
        assertTrue(app.lastRequest.associated(Session.class).isPresent());
        assertNotEquals(firstSession, newerSession);
    }
}