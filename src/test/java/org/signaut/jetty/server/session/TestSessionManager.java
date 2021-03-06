package org.signaut.jetty.server.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;
import org.signaut.common.hazelcast.HazelcastFactory;
import static org.mockito.Mockito.*;

public class TestSessionManager {

    @Test
    public void testIdManager() throws Exception {
        final HazelcastFactory hazelcastFactory = new HazelcastFactory();
        final Server server = new Server();
        HazelcastSessionIdManager idManager = new HazelcastSessionIdManager(server, "idWorker", 
                                                                        hazelcastFactory.loadHazelcastInstance("/test-session-cluster.xml", getClass()));
        HazelcastSessionManager sessionManager = new HazelcastSessionManager(idManager);

        idManager.start();
        sessionManager.start();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final String id = idManager.newSessionId(request, System.currentTimeMillis());
        Assert.assertNotNull("No id", id);
        HttpSession session = sessionManager.newHttpSession(request);
        Assert.assertNotNull("No session", session);
        Assert.assertTrue("Id not in use: " + session.getId(), idManager.idInUse(session.getId()));
        sessionManager.stop();
        idManager.stop();
    }
}
