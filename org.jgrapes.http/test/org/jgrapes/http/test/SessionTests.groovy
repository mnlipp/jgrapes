package org.jgrapes.http.test;

import java.net.HttpCookie
import java.net.URI

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus
import org.jdrupes.httpcodec.types.Converters
import org.jdrupes.httpcodec.types.CookieList
import org.hamcrest.core.IsInstanceOf
import org.jdrupes.httpcodec.protocols.http.HttpField
import org.jdrupes.httpcodec.protocols.http.HttpRequest
import org.jdrupes.httpcodec.protocols.http.HttpResponse
import org.jgrapes.core.Component
import org.jgrapes.core.Event
import org.jgrapes.core.annotation.Handler
import org.jgrapes.core.events.Start
import org.jgrapes.http.Session
import org.jgrapes.http.InMemorySessionManager
import org.jgrapes.http.events.DiscardSession
import org.jgrapes.http.events.GetRequest
import org.jgrapes.http.events.Request
import org.jgrapes.http.events.Response

import groovy.transform.CompileStatic
import spock.lang.*

class SessionTests extends Specification {
	
	class App extends Component {
		
		InMemorySessionManager sessionManager;
		Request lastRequest;
		
		public App(int absoluteTimeout, int idleTimeout) {
			sessionManager = new InMemorySessionManager(channel())
				.setAbsoluteTimeout(absoluteTimeout)
				.setIdleTimeout(idleTimeout);
			attach(sessionManager)
			fire(new Start()).get()
		}
		
		@Handler
		public onRequest(Request event) {
			lastRequest = event;
		}
	}

	private HttpRequest createRequest() {
		new HttpRequest(
			"GET", new URI("/"), HttpProtocol.HTTP_1_0, false)
			.setHostAndPort("localhost", 8080)
			.setResponse(new HttpResponse(HttpProtocol.HTTP_1_0,
				HttpStatus.NOT_IMPLEMENTED, false))
	}
	
	private String setSessionId(request, response) {
		String sessionId = response.findValue(
			HttpField.SET_COOKIE, Converters.SET_COOKIE)
			.get().findAll { cookie -> "id".equals(cookie.getName()) }[0]
			.getValue();
		request.computeIfAbsent(HttpField.COOKIE,
			CookieList.metaClass.&invokeConstructor).value()
				.add(new HttpCookie("id", sessionId));
		return sessionId
	}
	
	void "Absolute Timeout Test"() {
		setup: "Create app"
		App app = new App(1, 0);
		
		when: "Send first request"
		HttpRequest request = createRequest()
		Request evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then: "Session must have been created"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		
		when: "Request with the newly created session id"
		Session newSession = app.lastRequest.associated(Session.class).get()
		app.lastRequest = null;
		setSessionId(request, request.response().get())
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then: "Session must be found again"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		app.lastRequest.associated(Session.class).get().is(newSession)
		
		when: "Request after absolute timeout"
		app.lastRequest = null;
		Thread.sleep(1500);
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		Session newerSession = app.lastRequest.associated(Session.class).get()
		
		then: "New session must have been created"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		!Utils.equals(newSession, newerSession)
	}
	
	void "Idle Timeout Test"() {
		setup: "Create app"
		App app = new App(0, 1);
		
		when: "Send first request"
		HttpRequest request = createRequest()
		Request evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then: "Session must have been created"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		
		when: "Request with new session id"
		Session newSession = app.lastRequest.associated(Session.class).get()
		app.lastRequest = null;
		setSessionId(request, request.response().get())
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then: "Session must be found again"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		app.lastRequest.associated(Session.class).get().is(newSession)
		
		when: "Request after idle timeout"
		app.lastRequest = null;
		Thread.sleep(1500);
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		Session newerSession = app.lastRequest.associated(Session.class).get()
		
		then: "New session must have been created"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		!Utils.equals(newSession, newerSession)
	}
	
	void "Discard Test"() {
		setup: "Create app"
		App app = new App(0, 0);
		
		when: "Send first request"
		HttpRequest request = createRequest()
		Request evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then: "Session must have been created"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		
		when: "Send discard event"
		Session newSession = app.lastRequest.associated(Session.class).get()
		app.lastRequest = null;
		app.fire(new DiscardSession(newSession)).get()
		
		and: "Request again"
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		Session newerSession = app.lastRequest.associated(Session.class).get()
		
		then: "New session must have been created"
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		!Utils.equals(newSession, newerSession)
	}
	
}