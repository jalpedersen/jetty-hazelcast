package org.signaut.jetty.server.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.signaut.jetty.server.session.ClusterSessionManager.ClusterSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

public class ClusterSessionIdManager extends AbstractSessionIdManager implements ClusterSessionMapProvider {

    private final MultiMap<String, String> sessionIdMap;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HazelcastInstance hazelcastInstance;
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
	logger.info("SessionIdManager worker name: " + workerName);
	//This is probably not the place to put this
	System.setProperty("hazelcast.logging.type", "log4j");
	this.hazelcastInstance = loadHazelcastInstance(hazelcastConfiguration);
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
	sessionIdMap.put(getClusterId(session.getId()),
	        ((ClusterSession) session).getClusterId());
    }

    public void removeSession(HttpSession session) {
	sessionIdMap.remove(getClusterId(session.getId()),
	        ((ClusterSession) session).getClusterId());
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
    
    private HazelcastInstance loadHazelcastInstance(String filename) {
	//Load file from current location
	final File file = new File("./"+filename);
	final XmlConfigBuilder configBuilder;
	if (file.exists()) {
	    try {
	        configBuilder = new XmlConfigBuilder(new FileInputStream(file));
            } catch (FileNotFoundException e) {
        	throw new IllegalArgumentException("Failed to load file " + filename, e);
            }
            return Hazelcast.newHazelcastInstance(configBuilder.build());
	} else {
	    //If this fails, load from classpath
	    final InputStream resource = this.getClass().getResourceAsStream(filename);
	    if (resource != null) {
		configBuilder = new XmlConfigBuilder(resource);
		return Hazelcast.newHazelcastInstance(configBuilder.build());
	    }
	}
	//Bail out if all else fails
	throw new IllegalStateException("Failed to load hazelcast configuration from " + file);
    }

}
