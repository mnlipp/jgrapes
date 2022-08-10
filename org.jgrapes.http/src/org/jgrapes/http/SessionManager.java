/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2022 Michael N. Lipp
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

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.CacheControlDirectives;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.Directive;
import org.jgrapes.core.Associator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.DiscardSession;
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.IOSubchannel;

/**
 * A base class for session managers. A session manager associates 
 * {@link Request} events with a 
 * {@link Supplier {@code Supplier<Optional<Session>>}}
 * for a {@link Session} using `Session.class` as association identifier
 * (see {@link Session#from}). Note that the `Optional` will never by
 * empty. The return type has been chosen to be in accordance with
 * {@link Associator#associatedGet(Class)}.
 * 
 * The {@link Request} handler has a default priority of 1000.
 * 
 * Managers track requests using a cookie with a given name and path. The 
 * path is a prefix that has to be matched by the request, often "/".
 * If no cookie with the given name (see {@link #idName()}) is found,
 * a new cookie with that name and the specified path is created.
 * The cookie's value is the unique session id that is used to lookup
 * the session object.
 * 
 * Session managers provide additional support for web sockets. If a
 * web socket is accepted, the session associated with the request
 * is automatically made available to the {@link IOSubchannel} that
 * is subsequently used for the web socket events. This allows
 * handlers for web socket messages to access the session like
 * {@link Request} handlers (see {@link #onProtocolSwitchAccepted}).
 * 
 * @see EventBase#setAssociated(Object, Object)
 * @see "[OWASP Session Management Cheat Sheet](https://www.owasp.org/index.php/Session_Management_Cheat_Sheet)"
 */
@SuppressWarnings({ "PMD.DataClass", "PMD.AvoidPrintStackTrace",
    "PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods" })
public abstract class SessionManager extends Component {

    private static SecureRandom secureRandom = new SecureRandom();

    private String idName = "id";
    @SuppressWarnings("PMD.ImmutableField")
    private String path = "/";
    private long absoluteTimeout = 9 * 60 * 60 * 1000;
    private long idleTimeout = 30 * 60 * 1000;
    private int maxSessions = 1000;
    private Timer nextPurge;

    /**
     * Creates a new session manager with its channel set to
     * itself and the path set to "/". The manager handles
     * all {@link Request} events.
     */
    public SessionManager() {
        this("/");
    }

    /**
     * Creates a new session manager with its channel set to
     * itself and the path set to the given path. The manager
     * handles all requests that match the given path, using the
     * same rules as browsers do for selecting the cookies that
     * are to be sent.
     * 
     * @param path the path
     */
    public SessionManager(String path) {
        this(Channel.SELF, path);
    }

    /**
     * Creates a new session manager with its channel set to
     * the given channel and the path to "/". The manager handles
     * all {@link Request} events.
     * 
     * @param componentChannel the component channel
     */
    public SessionManager(Channel componentChannel) {
        this(componentChannel, "/");
    }

    /**
     * Creates a new session manager with the given channel and path.
     * The manager handles all requests that match the given path, using
     * the same rules as browsers do for selecting the cookies that
     * are to be sent.
     *  
     * @param componentChannel the component channel
     * @param path the path
     */
    public SessionManager(Channel componentChannel, String path) {
        this(componentChannel, derivePattern(path), 1000, path);
    }

    /**
     * Returns the path.
     *
     * @return the string
     */
    public String path() {
        return path;
    }

    /**
     * Derives the resource pattern from the path.
     *
     * @param path the path
     * @return the pattern
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected static String derivePattern(String path) {
        String pattern;
        if ("/".equals(path)) {
            pattern = "/**";
        } else {
            String patternBase = path;
            if (patternBase.endsWith("/")) {
                patternBase = path.substring(0, path.length() - 1);
            }
            pattern = patternBase + "|," + patternBase + "/**";
        }
        return pattern;
    }

    /**
     * Creates a new session manager using the given channel and path.
     * The manager handles only requests that match the given pattern.
     * The handler is registered with the given priority.
     * 
     * This constructor can be used if special handling of top level
     * requests is needed.
     *
     * @param componentChannel the component channel
     * @param pattern the path part of a {@link ResourcePattern}
     * @param priority the priority
     * @param path the path
     */
    public SessionManager(Channel componentChannel, String pattern,
            int priority, String path) {
        super(componentChannel);
        this.path = path;
        RequestHandler.Evaluator.add(this, "onRequest", pattern, priority);
        MBeanView.addManager(this);
    }

    private Optional<Long> minTimeout() {
        if (absoluteTimeout > 0 && idleTimeout > 0) {
            return Optional.of(Math.min(absoluteTimeout, idleTimeout));
        }
        if (absoluteTimeout > 0) {
            return Optional.of(absoluteTimeout);
        }
        if (idleTimeout > 0) {
            return Optional.of(idleTimeout);
        }
        return Optional.empty();
    }

    private void startPurger() {
        synchronized (this) {
            if (nextPurge == null) {
                minTimeout().ifPresent(timeout -> Components
                    .schedule(this::purgeAction, Duration.ofMillis(timeout)));
            }
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void purgeAction(Timer timer) {
        nextPurge = startDiscarding(absoluteTimeout, idleTimeout)
            .map(nextAt -> Components.schedule(this::purgeAction, nextAt))
            .orElse(null);
    }

    /**
     * The name used for the session id cookie. Defaults to "`id`".
     * 
     * @return the id name
     */
    public String idName() {
        return idName;
    }

    /**
     * @param idName the id name to set
     * 
     * @return the session manager for easy chaining
     */
    public SessionManager setIdName(String idName) {
        this.idName = idName;
        return this;
    }

    /**
     * Set the maximum number of sessions. If the value is zero or less,
     * an unlimited number of sessions is supported. The default value
     * is 1000.
     * 
     * If adding a new session would exceed the limit, first all
     * sessions older than {@link #absoluteTimeout()} are removed.
     * If this doesn't free a slot, the least recently used session
     * is removed.
     * 
     * @param maxSessions the maxSessions to set
     * @return the session manager for easy chaining
     */
    public SessionManager setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
        return this;
    }

    /**
     * @return the maxSessions
     */
    public int maxSessions() {
        return maxSessions;
    }

    /**
     * Sets the absolute timeout for a session. The absolute
     * timeout is the time after which a session is invalidated (relative
     * to its creation time). Defaults to 9 hours. Zero or less disables
     * the timeout.
     * 
     * @param timeout the absolute timeout
     * @return the session manager for easy chaining
     */
    public SessionManager setAbsoluteTimeout(Duration timeout) {
        this.absoluteTimeout = timeout.toMillis();
        return this;
    }

    /**
     * @return the absolute session timeout (in seconds)
     */
    public Duration absoluteTimeout() {
        return Duration.ofMillis(absoluteTimeout);
    }

    /**
     * Sets the idle timeout for a session. Defaults to 30 minutes.
     * Zero or less disables the timeout. 
     * 
     * @param timeout the absolute timeout
     * @return the session manager for easy chaining
     */
    public SessionManager setIdleTimeout(Duration timeout) {
        this.idleTimeout = timeout.toMillis();
        return this;
    }

    /**
     * @return the idle timeout
     */
    public Duration idleTimeout() {
        return Duration.ofMillis(idleTimeout);
    }

    /**
     * Associates the event with a {@link Session} object
     * using `Session.class` as association identifier.
     * 
     * @param event the event
     */
    @RequestHandler(dynamic = true)
    public void onRequest(Request.In event) {
        if (event.associated(Session.class).isPresent()) {
            return;
        }
        final HttpRequest request = event.httpRequest();
        Optional<String> requestedSessionId = request.findValue(
            HttpField.COOKIE, Converters.COOKIE_LIST)
            .flatMap(cookies -> cookies.stream().filter(
                cookie -> cookie.getName().equals(idName()))
                .findFirst().map(HttpCookie::getValue));
        if (requestedSessionId.isPresent()) {
            String sessionId = requestedSessionId.get();
            synchronized (this) {
                Optional<Session> session = lookupSession(sessionId);
                if (session.isPresent()) {
                    setSessionSupplier(event, sessionId);
                    session.get().updateLastUsedAt();
                    return;
                }
            }
        }
        Session session = createSession(
            addSessionCookie(request.response().get(), createSessionId()));
        setSessionSupplier(event, session.id());
        startPurger();
    }

    /**
     * Associated the associator with a session supplier for the 
     * given session id and note `this` as session manager.
     *
     * @param holder the channel
     * @param sessionId the session id
     */
    protected void setSessionSupplier(Associator holder, String sessionId) {
        holder.setAssociated(SessionManager.class, this);
        holder.setAssociated(Session.class,
            new SessionSupplier(holder, sessionId));
    }

    /**
     * Supports obtaining a {@link Session} from an {@link IOSubchannel}. 
     */
    private class SessionSupplier implements Supplier<Optional<Session>> {

        private final Associator holder;
        private final String sessionId;

        /**
         * Instantiates a new session supplier.
         *
         * @param holder the channel
         * @param sessionId the session id
         */
        public SessionSupplier(Associator holder, String sessionId) {
            this.holder = holder;
            this.sessionId = sessionId;
        }

        @Override
        public Optional<Session> get() {
            Optional<Session> session = lookupSession(sessionId);
            if (session.isPresent()) {
                session.get().updateLastUsedAt();
                return session;
            }
            Session newSession = createSession(createSessionId());
            setSessionSupplier(holder, newSession.id());
            return Optional.of(newSession);
        }

    }

    /**
     * Creates a session id and adds the corresponding cookie to the
     * response.
     * 
     * @param response the response
     * @return the session id
     */
    protected String addSessionCookie(HttpResponse response, String sessionId) {
        HttpCookie sessionCookie = new HttpCookie(idName(), sessionId);
        sessionCookie.setPath(path);
        sessionCookie.setHttpOnly(true);
        response.computeIfAbsent(HttpField.SET_COOKIE, CookieList::new)
            .value().add(sessionCookie);
        response.computeIfAbsent(
            HttpField.CACHE_CONTROL, CacheControlDirectives::new).value()
            .add(new Directive("no-cache", "SetCookie, Set-Cookie2"));
        return sessionId;
    }

    private String createSessionId() {
        StringBuilder sessionIdBuilder = new StringBuilder();
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        for (byte b : bytes) {
            sessionIdBuilder.append(Integer.toHexString(b & 0xff));
        }
        return sessionIdBuilder.toString();
    }

    /**
     * Checks if the absolute or idle timeout has been reached.
     *
     * @param session the session
     * @return true, if successful
     */
    protected boolean hasTimedOut(Session session) {
        Instant now = Instant.now();
        return absoluteTimeout > 0 && Duration
            .between(session.createdAt(), now).toMillis() > absoluteTimeout
            || idleTimeout > 0 && Duration.between(session.lastUsedAt(),
                now).toMillis() > idleTimeout;
    }

    /**
     * Start discarding all sessions (generate {@link DiscardSession} events)
     * that have reached their absolute or idle timeout. Do not
     * make the sessions unavailable yet. 
     * 
     * Returns the time when the next timeout occurs. This method is 
     * called only if at least one of the timeouts has been specified.
     * 
     * Implementations have to take care that sessions are only discarded
     * once. As they must remain available while the {@link DiscardSession}
     * event is handled this may require marking them as being discarded. 
     *
     * @param absoluteTimeout the absolute timeout
     * @param idleTimeout the idle timeout
     * @return the next timeout (empty if no sessions left)
     */
    protected abstract Optional<Instant> startDiscarding(long absoluteTimeout,
            long idleTimeout);

    /**
     * Creates a new session with the given id.
     * 
     * @param sessionId
     * @return the session
     */
    protected abstract Session createSession(String sessionId);

    /**
     * Lookup the session with the given id. Lookup will fail if
     * the session has timed out.
     * 
     * @param sessionId
     * @return the session
     */
    protected abstract Optional<Session> lookupSession(String sessionId);

    /**
     * Removes the given session from the cache.
     * 
     * @param sessionId the session id
     */
    protected abstract void removeSession(String sessionId);

    /**
     * Return the number of established sessions.
     * 
     * @return the result
     */
    protected abstract int sessionCount();

    /**
     * Discards the given session. The handler has a priority of -1000,
     * thus allowing other handler to make use of the session (for a
     * time) before it becomes unavailable.
     * 
     * @param event the event
     */
    @Handler(channels = Channel.class, priority = -1000)
    public void onDiscard(DiscardSession event) {
        removeSession(event.session().id());
        event.session().close();
    }

    /**
     * Associates the channel with a 
     * {@link Supplier {@code Supplier<Optional<Session>>}} 
     * for the session. Initially, the associated session is the session
     * associated with the protocol switch event. If this session times out,
     * a new session is returned as a fallback, thus making sure that
     * the `Optional` is never empty. The new session is, however, created 
     * independently of any new session created by {@link #onRequest}.
     * 
     * Applications should avoid any ambiguity by executing a proper 
     * cleanup of the web application in response to a 
     * {@link DiscardSession} event (including reestablishing the web
     * socket connections from new requests).
     * 
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = 1000)
    public void onProtocolSwitchAccepted(
            ProtocolSwitchAccepted event, IOSubchannel channel) {
        Request.In request = event.requestEvent();
        request.associated(SessionManager.class).filter(sm -> sm == this)
            .ifPresent(
                sm -> setSessionSupplier(channel, Session.from(request).id()));
    }

    /**
     * An MBean interface for getting information about the 
     * established sessions.
     */
    @SuppressWarnings("PMD.CommentRequired")
    public interface SessionManagerMXBean {

        String getComponentPath();

        String getPath();

        int getMaxSessions();

        long getAbsoluteTimeout();

        long getIdleTimeout();

        int getSessionCount();
    }

    /**
     * The session manager information.
     */
    public static class SessionManagerInfo implements SessionManagerMXBean {

        private static MBeanServer mbs
            = ManagementFactory.getPlatformMBeanServer();

        private ObjectName mbeanName;
        private final WeakReference<SessionManager> sessionManagerRef;

        /**
         * Instantiates a new session manager info.
         *
         * @param sessionManager the session manager
         */
        @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
            "PMD.EmptyCatchBlock" })
        public SessionManagerInfo(SessionManager sessionManager) {
            try {
                mbeanName = new ObjectName("org.jgrapes.http:type="
                    + SessionManager.class.getSimpleName() + ",name="
                    + ObjectName.quote(Components.simpleObjectName(
                        sessionManager)));
            } catch (MalformedObjectNameException e) {
                // Won't happen
            }
            sessionManagerRef = new WeakReference<>(sessionManager);
            try {
                mbs.unregisterMBean(mbeanName);
            } catch (Exception e) {
                // Just in case, should not work
            }
            try {
                mbs.registerMBean(this, mbeanName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                    | NotCompliantMBeanException e) {
                // Have to live with that
            }
        }

        /**
         * Returns the session manager.
         *
         * @return the optional session manager
         */
        @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
            "PMD.EmptyCatchBlock" })
        public Optional<SessionManager> manager() {
            SessionManager manager = sessionManagerRef.get();
            if (manager == null) {
                try {
                    mbs.unregisterMBean(mbeanName);
                } catch (MBeanRegistrationException
                        | InstanceNotFoundException e) {
                    // Should work.
                }
            }
            return Optional.ofNullable(manager);
        }

        @Override
        public String getComponentPath() {
            return manager().map(mgr -> mgr.componentPath())
                .orElse("<removed>");
        }

        @Override
        public String getPath() {
            return manager().map(mgr -> mgr.path).orElse("<unknown>");
        }

        @Override
        public int getMaxSessions() {
            return manager().map(mgr -> mgr.maxSessions()).orElse(0);
        }

        @Override
        public long getAbsoluteTimeout() {
            return manager().map(mgr -> mgr.absoluteTimeout().toMillis())
                .orElse(0L);
        }

        @Override
        public long getIdleTimeout() {
            return manager().map(mgr -> mgr.idleTimeout().toMillis())
                .orElse(0L);
        }

        @Override
        public int getSessionCount() {
            return manager().map(mgr -> mgr.sessionCount()).orElse(0);
        }
    }

    /**
     * An MBean interface for getting information about all session
     * managers.
     * 
     * There is currently no summary information. However, the (periodic)
     * invocation of {@link SessionManagerSummaryMXBean#getManagers()} ensures
     * that entries for removed {@link SessionManager}s are unregistered.
     */
    public interface SessionManagerSummaryMXBean {

        /**
         * Gets the managers.
         *
         * @return the managers
         */
        Set<SessionManagerMXBean> getManagers();
    }

    /**
     * The MBean view.
     */
    private static class MBeanView implements SessionManagerSummaryMXBean {
        private static Set<SessionManagerInfo> managerInfos = new HashSet<>();

        /**
         * Adds a manager.
         *
         * @param manager the manager
         */
        public static void addManager(SessionManager manager) {
            synchronized (managerInfos) {
                managerInfos.add(new SessionManagerInfo(manager));
            }
        }

        @Override
        public Set<SessionManagerMXBean> getManagers() {
            Set<SessionManagerInfo> expired = new HashSet<>();
            synchronized (managerInfos) {
                for (SessionManagerInfo managerInfo : managerInfos) {
                    if (!managerInfo.manager().isPresent()) {
                        expired.add(managerInfo);
                    }
                }
                managerInfos.removeAll(expired);
            }
            @SuppressWarnings("unchecked")
            Set<SessionManagerMXBean> result
                = (Set<SessionManagerMXBean>) (Object) managerInfos;
            return result;
        }
    }

    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName("org.jgrapes.http:type="
                + SessionManager.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Does not happen
            e.printStackTrace();
        }
    }
}
