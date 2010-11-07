/*
Copyright (c) 2010, Jesper André Lyngesen Pedersen
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.signaut.jetty.server.session;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Clusterable session manager using Hazelcast.
 * </p>
 * 
 * Requires {@link ClusterSessionIdManager} Session ID manager
 * 
 * 
 * @author jalp
 * 
 */
public class ClusterSessionManager extends AbstractSessionManager implements SessionManager, Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private boolean invalidOnRedeploy;

    private final ConcurrentMap<String, ClusterSessionData> sessionMap;
    private final ConcurrentMap<String, Object> attributeMap;
    private final long cleanupTaskDelay = 120;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cleanupTask;
    private String stickySessionKey = "_signaut.stickySession";

    public ClusterSessionManager(ClusterSessionIdManager sessionIdManager) {
        super();
        invalidOnRedeploy = false;
        setIdManager(sessionIdManager);
        this.sessionMap = sessionIdManager.getSessionMap();
        this.attributeMap = sessionIdManager.getAttributeMap();
    }

    public boolean isInvalidOnRedeploy() {
        return invalidOnRedeploy;
    }

    public void setInvalidOnRedeploy(boolean invalidOnRedeploy) {
        this.invalidOnRedeploy = invalidOnRedeploy;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupTask = scheduler.scheduleWithFixedDelay(this, cleanupTaskDelay, cleanupTaskDelay, TimeUnit.SECONDS);
    }

    @Override
    public void doStop() throws Exception {
        super.doStop();
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public Map<String, ClusterSession> getSessionMap() {
        final Map<String, ClusterSession> sessions = new HashMap<String, ClusterSessionManager.ClusterSession>();
        for (Entry<String, ClusterSessionData> d : sessionMap.entrySet()) {
            sessions.put(d.getKey(), new ClusterSession(d.getValue(), d.getKey()));
        }
        return sessions;
    }

    @Override
    protected void addSession(AbstractSessionManager.Session session) {
        final ClusterSession clusterSession = (ClusterSession) session;
        final ClusterSessionData data = new ClusterSessionData();
        data.setMaxIdleMs(clusterSession.getMaxInactiveInterval() * 1000);
        data.setCreated(clusterSession.getCreationTime());
        data.setKeys(new HashSet<String>());
        sessionMap.put(clusterSession.getClusterId(), data);
    }

    @Override
    public AbstractSessionManager.Session getSession(String idInCluster) {
        final ClusterSessionData data = sessionMap.get(idInCluster);
        if (data == null) {
            return null;
        }
        return new ClusterSession(data, idInCluster);
    }

    @Override
    protected void invalidateSessions() {
        if (invalidOnRedeploy) {
            log.info("Removing all sessions");
            for (String idInCluster : sessionMap.keySet()) {
                removeSession(idInCluster);
            }
        }
    }

    @Override
    protected Session newSession(HttpServletRequest request) {
        return new ClusterSession(request);
    }

    @Override
    protected void removeSession(String idInCluster) {
        log.debug("Removing session:" + idInCluster);
        final ClusterSessionData data = sessionMap.get(idInCluster);
        for (String key : data.getKeys()) {
            attributeMap.remove(idInCluster + "#" + key);
        }
        sessionMap.remove(idInCluster);

    }

    public class ClusterSession extends AbstractSessionManager.Session {
        private static final long serialVersionUID = 3657090660140999739L;

        public ClusterSession(ClusterSessionData data, String clusterId) {
            super(data.getCreated(), data.getAccessed(), clusterId);
        }

        protected ClusterSession(HttpServletRequest request) {
            super(request);
        }

        public String getClusterId() {
            return _clusterId;
        }

        @Override
        public void setAttribute(String name, Object value) {
            super.setAttribute(name, value);
            if (value != null) {
                final ClusterSessionData data = sessionMap.get(_clusterId);
                data.getKeys().add(name);
                if (stickySessionKey.equals(name)) {
                    data.setKeepAlive((Boolean) value);
                }
                sessionMap.put(_clusterId, data);
                attributeMap.put(_clusterId + "#" + name, value);
            }
        }

        @Override
        public Object getAttribute(String name) {
            return attributeMap.get(_clusterId + "#" + name);
        }

        @Override
        public void removeAttribute(String name) {
            final ClusterSessionData data = sessionMap.get(_clusterId);
            if (data != null) {
                if (data.getKeys().contains(name)) {
                    data.getKeys().remove(name);
                    if (stickySessionKey.equals(name)) {
                        data.setKeepAlive(false);
                    }
                    attributeMap.remove(_clusterId + "#" + name);
                    sessionMap.put(_clusterId, data);
                }
            }

        }

        @Override
        public Enumeration<String> getAttributeNames() {
            final Set<String> keys = sessionMap.get(_clusterId).getKeys();
            if (keys == null) {
                return Collections.enumeration(Collections.<String> emptySet());
            }
            return Collections.enumeration(keys);
        }

        @Override
        public int getMaxInactiveInterval() {
            final ClusterSessionData data = sessionMap.get(_clusterId);
            if (data != null) {
                _maxIdleMs = data.getMaxIdleMs();
            }
            return super.getMaxInactiveInterval();
        }

        @Override
        public void setIdChanged(boolean changed) {
            final ClusterSessionData data = sessionMap.get(_clusterId);
            if (data != null) {
                data.setIdChanged(changed);
                sessionMap.put(_clusterId, data);
            }
            super.setIdChanged(changed);
        }

        @Override
        public void setMaxInactiveInterval(int secs) {
            final ClusterSessionData data = sessionMap.get(_clusterId);
            if (data != null) {
                data.setMaxIdleMs(secs * 1000);
                sessionMap.put(_clusterId, data);
            }
            super.setMaxInactiveInterval(secs);
        }

        @Override
        protected Map<String, Object> newAttributeMap() {
            return new HashMap<String, Object>();
        }
    }

    @Override
    public void run() {
        cleanupSessions();
    }

    private void cleanupSessions() {
        if (isStopping() || isStopped())
            return;

        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();

        if (_loader != null) {
            thread.setContextClassLoader(_loader);
        }

        try {
            long now = System.currentTimeMillis();
            for (Entry<String, ClusterSessionData> entry : sessionMap.entrySet()) {
                final ClusterSessionData data = entry.getValue();
                if (data.isKeepAlive()) {
                    // Should we put the session into cryostasis?
                    continue;
                }
                final long idleTime = data.getMaxIdleMs();

                if (idleTime > 0 && data.getAccessed() + idleTime < now) {
                    log.debug("Removing idle session: " + entry.getKey());
                    removeSession(entry.getKey());
                }
            }
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }
}
