package org.signaut.jetty.server.session;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.signaut.common.hazelcast.HazelcastFactory;
import org.signaut.jetty.server.session.ClusterSessionManager.ClusterSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

public class ClusterSessionIdManager extends AbstractSessionIdManager implements ClusterSessionMapProvider {

    private final MultiMap<String, String> sessionIdMap;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastFactory hazelcastFactory = new HazelcastFactory();
    public static final String SESSION_ID_MAP = "signaut.sessionIdMap";
    public static final String SESSION_MAP = "signaut.sessionMap";
    public static final String SESSION_ATTRIBUTE_MAP = "signaut.sessionAttrMap";

    public ClusterSessionIdManager(String workerName, String hazelcastConfiguration) {
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
        log.info("SessionIdManager worker name: " + workerName);
        // This is probably not the place to put this
        System.setProperty("hazelcast.logging.type", "log4j");
        this.hazelcastInstance = hazelcastFactory.loadHazelcastInstance(hazelcastConfiguration, getClass());
        this.sessionIdMap = hazelcastInstance.getMultiMap("signaut.sessionIdMap");
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
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        // Do not clear map as others may be using it.
        super.doStop();
    }

    public boolean idInUse(String id) {
        return sessionIdMap.containsKey(id);
    }

    public void addSession(HttpSession session) {
        sessionIdMap.put(getClusterId(session.getId()), ((ClusterSession) session).getClusterId());
    }

    public void removeSession(HttpSession session) {
        sessionIdMap.remove(getClusterId(session.getId()), ((ClusterSession) session).getClusterId());
    }

    public void invalidateAll(String id) {
        // Not implemented
    }

    @Override
    public ConcurrentMap<String, ClusterSessionData> getSessionMap() {
        return hazelcastInstance.getMap(SESSION_MAP);
    }

    @Override
    public ConcurrentMap<String, Object> getAttributeMap() {
        return hazelcastInstance.getMap(SESSION_ATTRIBUTE_MAP);
    }

}
