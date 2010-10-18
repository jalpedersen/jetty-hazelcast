package org.signaut.jetty.server.security;

import java.security.Principal;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.RoleRunAsToken;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.server.UserIdentity;

/**
 * IdentityService implementation which produces a serializable UserIdentity
 * object. Other than the serializable property of the returned identity, this
 * service is identical to the DefaultIdentityService.
 * 
 * @author jalp
 * 
 */
public class SerializableIdentityService implements IdentityService {

    @Override
    public Object associate(UserIdentity user) {
        // Ignore
        return null;
    }

    @Override
    public void disassociate(Object previous) {
        // Ignore
    }

    @Override
    public Object setRunAs(UserIdentity user, RunAsToken token) {
        return token;
    }

    @Override
    public void unsetRunAs(Object token) {
        // Ignore
    }

    @Override
    public UserIdentity newUserIdentity(Subject subject, Principal userPrincipal, String[] roles) {
        return new SerializableIdentity(userPrincipal, subject, roles);
    }

    @Override
    public RunAsToken newRunAsToken(String runAsName) {
        return new RoleRunAsToken(runAsName);
    }

    @Override
    public UserIdentity getSystemUserIdentity() {
        // Not implemented
        return null;
    }
}
