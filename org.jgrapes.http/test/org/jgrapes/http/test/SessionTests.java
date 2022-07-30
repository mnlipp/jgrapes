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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
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
        public List<Request<?>> lastRequests = new ArrayList<>();
        public List<Session> discarded = new ArrayList<>();
        public List<Instant> discardedAt = new ArrayList<>();

        public App(int absoluteTimeout, int idleTimeout) throws Exception {
            sessionManager = new InMemorySessionManager(channel())
                .setAbsoluteTimeout(absoluteTimeout)
                .setIdleTimeout(idleTimeout);
            attach(sessionManager);
            fire(new Start()).get();
        }

        @Handler
        public void onRequest(Request<?> event) {
            event.setAssociated("frozen", Session.from(event));
            lastRequests.add(0, event);
        }

        public Request<?> lastRequest() {
            return lastRequests.size() > 0 ? lastRequests.get(0) : null;
        }

        @Handler
        public void onDiscardSession(DiscardSession event) {
            discarded.add(0, event.session());
            discardedAt.add(0, Instant.now());
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
        int absoluteTimeout = 1;
        int idleTimeout = 0;
        App app = new App(absoluteTimeout, idleTimeout);

        // Send first request
        HttpRequest request = createRequest();
        Request.In evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();
        Components.awaitExhaustion();
        assertNotNull(app.lastRequest());
        Request<?> firstRequest = app.lastRequest();
        assertTrue(
            firstRequest.associated(Session.class, Supplier.class).isPresent());
        Session firstSession = Session.from(firstRequest);
        assertTrue(Duration.between(Instant.now(), firstSession.createdAt())
            .abs().toMillis() < 250);
        firstSession.transientData().put("resource", new TestResource());

        // Request with the new session id from first request
        Thread.sleep(absoluteTimeout * 1000 / 2);
        setSessionId(request, request.response().get());
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequests.clear();
        app.fire(evt).get();
        Components.awaitExhaustion();

        // Session must have been found again
        assertNotNull(app.lastRequest());
        assertTrue(app.lastRequest().associated(Session.class, Supplier.class)
            .isPresent());
        assertEquals(firstSession, Session.from(app.lastRequest()));
        assertTrue(app.discarded.isEmpty());

        // Request after absolute timeout
        Thread.sleep(absoluteTimeout * 1000 + 500);
        Components.awaitExhaustion();
        assertEquals(firstSession, app.discarded.get(0));
        assertTrue(
            Duration.between(app.discardedAt.get(0), firstSession.createdAt())
                .abs().toMillis() < (absoluteTimeout * 1000 + 250));

        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequests.clear();
        app.fire(evt).get();
        Components.awaitExhaustion();
        assertTrue(((TestResource) firstSession.transientData()
            .get("resource")).closed);
        assertNotNull(app.lastRequest());
        assertTrue(app.lastRequest().associated(Session.class, Supplier.class)
            .isPresent());
        Session newerSession = Session.from(app.lastRequest());
        assertNotEquals(firstSession, newerSession);
    }

    @Test
    public void testIdleTimeout() throws Exception {
        // Create app
        int absoluteTimeout = 0;
        int idleTimeout = 1;
        App app = new App(absoluteTimeout, idleTimeout);

        // Send first request
        HttpRequest request = createRequest();
        Request.In evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();
        Components.awaitExhaustion();
        assertNotNull(app.lastRequest());
        Request<?> firstRequest = app.lastRequest();
        assertTrue(
            firstRequest.associated(Session.class, Supplier.class).isPresent());
        Session firstSession = Session.from(firstRequest);
        firstSession.transientData().put("resource", new TestResource());

        // Request with the new session id from first request
        Thread.sleep(idleTimeout * 1000 / 2);
        setSessionId(request, request.response().get());
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequests.clear();
        app.fire(evt).get();
        Components.awaitExhaustion();

        // Session must have been found again, last used updated
        assertNotNull(app.lastRequest());
        assertTrue(app.lastRequest().associated(Session.class, Supplier.class)
            .isPresent());
        assertEquals(firstSession, Session.from(app.lastRequest()));
        assertTrue(Duration.between(Instant.now(), firstSession.lastUsedAt())
            .abs().toMillis() < 250);
        assertTrue(app.discarded.isEmpty());

        // Request after idle timeout
        Thread.sleep(idleTimeout * 1000 + 500);
        Components.awaitExhaustion();
        assertEquals(firstSession, app.discarded.get(0));
        assertTrue(Duration.between(firstSession.lastUsedAt(),
            app.discardedAt.get(0)).toMillis() < (idleTimeout * 1000 + 250));
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.lastRequests.clear();
        app.fire(evt).get();
        assertTrue(((TestResource) firstSession.transientData()
            .get("resource")).closed);
        assertNotNull(app.lastRequest());
        assertTrue(app.lastRequest().associated(Session.class, Supplier.class)
            .isPresent());
        Session newerSession = Session.from(app.lastRequest());
        assertNotEquals(firstSession, newerSession);
    }

    @Test
    public void testSeveral() throws Exception {
        int absoluteTimeout = 2;
        int idleTimeout = 0;
        App app = new App(absoluteTimeout, idleTimeout);

        final Instant startTime = Instant.now();
        HttpRequest request = createRequest();
        Request.In evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt);
        Thread.sleep(1000);
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt);
        Thread.sleep(1000);
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt);
        Thread.sleep(2500);
        Components.awaitExhaustion();

        assertEquals(
            app.lastRequests.get(2).associated("frozen", Session.class).get(),
            app.discarded.get(2));
        assertTrue(Math.abs(Duration.between(startTime,
            app.discardedAt.get(2)).toMillis() - 2000) < 250);
        assertEquals(
            app.lastRequests.get(1).associated("frozen", Session.class).get(),
            app.discarded.get(1));
        assertTrue(Math.abs(Duration.between(startTime,
            app.discardedAt.get(1)).toMillis() - 3000) < 250);
        assertEquals(
            app.lastRequests.get(0).associated("frozen", Session.class).get(),
            app.discarded.get(0));
        assertTrue(Math.abs(Duration.between(startTime,
            app.discardedAt.get(0)).toMillis() - 4000) < 250);
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
        assertNotNull(app.lastRequest());
        Request<?> firstRequest = app.lastRequest();
        assertTrue(
            firstRequest.associated(Session.class, Supplier.class).isPresent());
        Session firstSession = Session.from(firstRequest);
        firstSession.transientData().put("resource", new TestResource());

        // Send discard event
        app.lastRequests.clear();
        app.fire(new DiscardSession(firstSession)).get();

        // Must be closed
        assertTrue(((TestResource) firstSession.transientData()
            .get("resource")).closed);

        // Request again
        evt = Request.In.fromHttpRequest(request, false, 0);
        app.fire(evt).get();
        assertTrue(app.lastRequest().associated(Session.class, Supplier.class)
            .isPresent());
        Session newerSession = Session.from(app.lastRequest());

        // New session must have been created
        assertNotNull(app.lastRequest());
        assertNotEquals(firstSession, newerSession);
    }
}