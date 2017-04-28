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
import org.jgrapes.http.SessionManager
import org.jgrapes.http.events.GetRequest
import org.jgrapes.http.events.Request
import org.jgrapes.http.events.Response

import groovy.transform.CompileStatic
import spock.lang.*

class SessionTests extends Specification {
	
	App app = new App();
	
	class App extends Component {
		
		Request lastRequest;
		
		public App() {
			attach(new SessionManager(channel())
				.setMaxSessionAge(1))
			fire(new Start()).get()
		}
		
		@Handler
		public onRequest(Request event) {
			lastRequest = event;
		}
	}

	void "Session Timeout Test"() {
		when:
		HttpRequest request = new HttpRequest(
			"GET", new URI("/"), HttpProtocol.HTTP_1_0, false)
			.setHostAndPort("localhost", 8080)
			.setResponse(new HttpResponse(HttpProtocol.HTTP_1_0, 
				HttpStatus.NOT_IMPLEMENTED, false))
		Request evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then:
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		
		when:
		Session newSession = app.lastRequest.associated(Session.class).get()
		app.lastRequest = null;
		String sessionId = request.response().get().findValue(
			HttpField.SET_COOKIE, Converters.SET_COOKIE)
			.get().findAll { cookie -> "id".equals(cookie.getName()) }[0]
			.getValue();
		request.computeIfAbsent(HttpField.COOKIE, 
			CookieList.metaClass.&invokeConstructor).value()
				.add(new HttpCookie("id", sessionId));
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		
		then:
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		app.lastRequest.associated(Session.class).get().is(newSession)
		
		when:
		app.lastRequest = null;
		Thread.sleep(1500);
		evt = Request.fromHttpRequest(request, false, 0)
		app.fire(evt).get()
		Session newerSession = app.lastRequest.associated(Session.class).get()
		
		then:
		app.lastRequest != null
		app.lastRequest.associated(Session.class).isPresent()
		!Utils.equals(newSession, newerSession)
	}
	
}