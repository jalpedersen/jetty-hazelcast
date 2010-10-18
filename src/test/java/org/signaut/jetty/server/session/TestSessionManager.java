package org.signaut.jetty.server.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Test;

public class TestSessionManager {

    @Test
    public void testIdManager() throws Exception {
        ClusterSessionIdManager idManager = new ClusterSessionIdManager("idWorker", "/test-session-cluster.xml");
        ClusterSessionManager sessionManager = new ClusterSessionManager(idManager);

        idManager.start();
        sessionManager.start();
        final HttpServletRequest request = new MockHttpServletRequest();
        final String id = idManager.newSessionId(request, System.currentTimeMillis());
        Assert.assertNotNull("No id", id);
        HttpSession session = sessionManager.newHttpSession(request);
        Assert.assertNotNull("No session", session);
        Assert.assertTrue("Id not in use: " + session.getId(), idManager.idInUse(session.getId()));
        sessionManager.stop();
        idManager.stop();
    }

    private class MockHttpServletRequest implements HttpServletRequest {

        @Override
        public Object getAttribute(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCharacterEncoding() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
            // TODO Auto-generated method stub

        }

        @Override
        public int getContentLength() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getContentType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getParameter(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getParameterNames() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map getParameterMap() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getProtocol() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getScheme() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getServerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getServerPort() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRemoteAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRemoteHost() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setAttribute(String name, Object o) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeAttribute(String name) {
            // TODO Auto-generated method stub

        }

        @Override
        public Locale getLocale() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getLocales() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isSecure() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRealPath(String path) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getRemotePort() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getLocalName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getLocalAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getLocalPort() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getAuthType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getDateHeader(String name) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getHeader(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getHeaders(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getHeaderNames() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getIntHeader(String name) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getMethod() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPathInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPathTranslated() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getContextPath() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getQueryString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRemoteUser() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRequestURI() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getServletPath() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HttpSession getSession(boolean create) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HttpSession getSession() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            // TODO Auto-generated method stub
            return false;
        }

    }
}
