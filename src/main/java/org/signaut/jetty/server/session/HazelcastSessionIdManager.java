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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.signaut.jetty.server.session.HazelcastSessionManager.HazelcastSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

public class HazelcastSessionIdManager extends AbstractSessionIdManager implements HazelcastSessionMapProvider {

    private MultiMap<String, String> sessionIdMap;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HazelcastInstance hazelcastInstance;
    public static final String SESSION_ID_MAP = "signaut.sessionIdMap";
    public static final String SESSION_MAP = "signaut.sessionMap";
    public static final String SESSION_ATTRIBUTE_MAP = "signaut.sessionAttrMap";

    public HazelcastSessionIdManager(String workerName, HazelcastInstance hazelcastInstance) {
        super();

        if (workerName == null) {
            final String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
                setWorkerName(hostname);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Failed to get hostname", e);
            }
        } else {
            setWorkerName(workerName);
        }
        log.info("SessionIdManager worker name: " + getWorkerName());
        this.hazelcastInstance = hazelcastInstance;
        
    }

    public String getNodeId(String clusterId, HttpServletRequest request) {
        return clusterId + '.' + getWorkerName();
    }

    public String getClusterId(String nodeId) {
        int dot = nodeId.lastIndexOf('.');
        return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
    }

    @Override
    protected void doStart() throws Exception {
        this.sessionIdMap = hazelcastInstance.getMultiMap("signaut.sessionIdMap");
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        // Do not clear map as others may be using it.
        super.doStop();
        this.sessionIdMap = null;
    }

    public boolean idInUse(String id) {
        return sessionIdMap.containsKey(id);
    }

    public void addSession(HttpSession session) {
        sessionIdMap.put(getClusterId(session.getId()), ((HazelcastSession) session).getClusterId());
    }

    public void removeSession(HttpSession session) {
        sessionIdMap.remove(getClusterId(session.getId()), ((HazelcastSession) session).getClusterId());
    }

    public void invalidateAll(String id) {
        // Not implemented
    }

    @Override
    public ConcurrentMap<String, SessionData> getSessionMap() {
        return hazelcastInstance.getMap(SESSION_MAP);
    }

    @Override
    public ConcurrentMap<String, Object> getAttributeMap() {
        return hazelcastInstance.getMap(SESSION_ATTRIBUTE_MAP);
    }

}
