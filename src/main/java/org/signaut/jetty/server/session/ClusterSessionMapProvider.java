package org.signaut.jetty.server.session;

import java.util.concurrent.ConcurrentMap;

interface ClusterSessionMapProvider {
    ConcurrentMap<String, ClusterSessionData> getSessionMap();
    ConcurrentMap<String, Object> getAttributeMap();
}
