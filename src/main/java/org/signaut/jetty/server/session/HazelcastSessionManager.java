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
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * <p>
 * Clusterable session manager using Hazelcast.
 * </p>
 *
 * Requires {@link HazelcastSessionIdManager} Session ID manager
 *
 *
 * @author jalp
 *
 */
public class HazelcastSessionManager extends AbstractSessionManager implements SessionManager, Runnable {

    private final Logger log = Log.getLogger(getClass());

    private ConcurrentMap<String, SessionData> sessionMap;
    private ConcurrentMap<String, Object> attributeMap;
    private final long cleanupTaskDelay = 120;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cleanupTask;
    private String stickySessionKey = "_signaut.stickySession";
    private final ClassLoader hzLoader = getClass().getClassLoader();
    private boolean invalidatesOnRedeploy = false;
    private final HazelcastSessionIdManager hazelcastSessionIdManager;

    public HazelcastSessionManager(HazelcastSessionIdManager sessionIdManager) {
        super();
        this.hazelcastSessionIdManager = sessionIdManager;
        setSessionIdManager(sessionIdManager);
    }

    public String getStickySessionKey() {
        return stickySessionKey;
    }

    public void setStickySessionKey(String stickySessionKey) {
        this.stickySessionKey = stickySessionKey;
    }

    public boolean isInvalidatesOnRedeploy() {
        return invalidatesOnRedeploy;
    }

    public void setInvalidatesOnRedeploy(boolean invalidatesOnRedeploy) {
        this.invalidatesOnRedeploy = invalidatesOnRedeploy;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        this.sessionMap = hazelcastSessionIdManager.getSessionMap();
        this.attributeMap = hazelcastSessionIdManager.getAttributeMap();

        clearScheduler();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupTask = scheduler.scheduleWithFixedDelay(this, cleanupTaskDelay,
                cleanupTaskDelay, TimeUnit.SECONDS);
    }

    private void clearScheduler() {
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public void doStop() throws Exception {
        clearScheduler();
        this.sessionMap = null;
        this.attributeMap = null;
        super.doStop();
    }

    @Override
    public Map<String, HazelcastSession> getSessionMap() {
        final Map<String, HazelcastSession> sessions = 
                new HashMap<String, HazelcastSessionManager.HazelcastSession>();
        for (Entry<String, SessionData> d : entrySet(sessionMap)) {
            sessions.put(d.getKey(), new HazelcastSession(d.getValue(), d.getKey()));
        }
        return sessions;
    }

    @Override
    protected void addSession(AbstractSession session) {
        final HazelcastSession clusterSession = (HazelcastSession) session;
        final SessionData data = new SessionData();
        data.setMaxIdleMs(clusterSession.getMaxInactiveInterval() * 1000);
        data.setCreated(clusterSession.getCreationTime());
        data.setKeys(new HashSet<String>());
        put(sessionMap, clusterSession.getClusterId(), data);
    }

    @Override
    public AbstractSession getSession(String idInCluster) {
        final SessionData data = get(sessionMap, idInCluster);
        if (data == null) {
            return null;
        }
        return new HazelcastSession(data, idInCluster);
    }

    @Override
    protected void shutdownSessions() {
        if (this.invalidatesOnRedeploy) {
            log.info("Removing all sessions");
            for (String idInCluster : keySet(sessionMap)) {
                removeSession(idInCluster);
            }
        }
    }

    @Override
    protected AbstractSession newSession(HttpServletRequest request) {
        return new HazelcastSession(request);
    }

    @Override
    protected boolean removeSession(String idInCluster) {
        log.debug("Removing session:" + idInCluster);
        final SessionData data = get(sessionMap, idInCluster);
        for (String key : data.getKeys()) {
            attributeMap.remove(idInCluster + "#" + key);
        }
        hazelcastSessionIdManager.removeSession(getSession(idInCluster));
        return remove(sessionMap, idInCluster) != null;

    }

    public class HazelcastSession extends AbstractSession {

        public HazelcastSession(SessionData data, String clusterId) {
            super(HazelcastSessionManager.this, data.getCreated(), data.getAccessed(), clusterId);
        }

        protected HazelcastSession(HttpServletRequest request) {
            super(HazelcastSessionManager.this, request);
        }

        public void setAttribute(String name, Object value) {
            super.setAttribute(name, value);
            if (value != null) {
                final SessionData data = get(sessionMap, getClusterId());
                data.getKeys().add(name);
                if (stickySessionKey.equals(name)) {
                    data.setKeepAlive((Boolean) value);
                }
                put(sessionMap, getClusterId(), data);
                attributeMap.put(getClusterId() + "#" + name, value);
            }
        }

        @Override
        public Object getAttribute(String name) {
            return attributeMap.get(getClusterId() + "#" + name);
        }

        @Override
        public void removeAttribute(String name) {
            final SessionData data = get(sessionMap, getClusterId());
            if (data != null) {
                if (data.getKeys().contains(name)) {
                    data.getKeys().remove(name);
                    if (stickySessionKey.equals(name)) {
                        data.setKeepAlive(false);
                    }
                    attributeMap.remove(getClusterId() + "#" + name);
                    put(sessionMap, getClusterId(), data);
                }
            }

        }

        @Override
        public Enumeration<String> getAttributeNames() {
            final Set<String> keys = get(sessionMap, getClusterId()).getKeys();
            if (keys == null) {
                return Collections.enumeration(Collections.<String>emptySet());
            }
            return Collections.enumeration(keys);
        }

        @Override
        public int getMaxInactiveInterval() {
            final SessionData data = get(sessionMap, getClusterId());
            if (data != null) {
                setMaxInactiveInterval((int) data.getMaxIdleMs() / 1000);
            }
            return super.getMaxInactiveInterval();
        }

        @Override
        public void setIdChanged(boolean changed) {
            final SessionData data = get(sessionMap, getClusterId());
            if (data != null) {
                data.setIdChanged(changed);
                put(sessionMap, getClusterId(), data);
            }
            super.setIdChanged(changed);
        }

        @Override
        public void setMaxInactiveInterval(int secs) {
            final SessionData data = get(sessionMap, getClusterId());
            if (data != null) {
                data.setMaxIdleMs(secs * 1000);
                put(sessionMap, getClusterId(), data);
            }
            super.setMaxInactiveInterval(secs);
        }

    }

    private <K, V> Set<Map.Entry<K, V>> entrySet(Map<K, V> map) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(hzLoader);
            return map.entrySet();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private <K, V> Set<K> keySet(Map<K, V> map) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(hzLoader);
            return map.keySet();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private <K, V> V get(Map<K, V> map, K key) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(hzLoader);
            return map.get(key);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private <K, V> V put(Map<K, V> map, K key, V value) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(hzLoader);
            return map.put(key, value);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private <K, V> V remove(Map<K, V> map, K key) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(hzLoader);
            return map.remove(key);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public void run() {
        cleanupSessions();
    }

    private void cleanupSessions() {
        if (isStopping() || isStopped()) {
            return;
        }

        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();

        if (hzLoader != null) {
            thread.setContextClassLoader(hzLoader);
        }

        try {
            long now = System.currentTimeMillis();
            for (Entry<String, SessionData> entry : sessionMap.entrySet()) {
                final SessionData data = entry.getValue();
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

    @Override
    public void renewSessionId(String oldClusterId, String oldNodeId, String newClusterId, String newNodeId) {
        try {
            if (sessionMap == null) {
                return;
            }

            SessionData session = sessionMap.remove(oldClusterId);
            if (session == null) {
                return;
            }
            sessionMap.put(newClusterId, session);
        } catch (Exception e) {
            log.warn("Error renewing session", e);
        }
    }
}
