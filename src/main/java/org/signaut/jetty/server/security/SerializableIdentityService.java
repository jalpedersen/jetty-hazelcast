package org.signaut.jetty.server.security;

import java.io.Serializable;
import java.security.Principal;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.RoleRunAsToken;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.server.UserIdentity;

/**
 * IdentityService implementation which produces a serializable UserIdentity object.
 * Other than the serializable property of the returned identity, this service is 
 * identical to the DefaultIdentityService.
 * 
 * @author jalp
 *
 */
public class SerializableIdentityService implements IdentityService {
    
    public static class MontyIdentity implements UserIdentity, Serializable {
        private static final long serialVersionUID = 493118349878757632L;

        private final Principal principal;
	private final Subject subject;
	private final String roles[];
	                           
	public MontyIdentity(Principal principal, Subject subject, String[] roles) {
	    this.principal = principal;
	    this.subject = subject;
	    this.roles = roles;
        }

	@Override
        public Subject getSubject() {
	    return subject;
	}

	@Override
        public Principal getUserPrincipal() {
	    return principal;
	}

	@Override
	public boolean isUserInRole(String role, Scope scope) {
	    if (scope!=null && scope.getRoleRefMap()!=null) {
		role=scope.getRoleRefMap().get(role);
	    }
	    for (String r: roles) {
		if (r.equals(role)) {
		    return true;
		}
	    }
	    return false;
	}
	
	public String[] getRoles() {
	    return roles;
	}
	
    }
    
    @Override
    public Object associate(UserIdentity user) {
	//Ignore
	return null;
    }

    @Override
    public void disassociate(Object previous) {
	//Ignore
    }

    @Override
    public Object setRunAs(UserIdentity user, RunAsToken token) {
	return token;
    }

    @Override
    public void unsetRunAs(Object token) {
	//Ignore
    }

    @Override
    public UserIdentity newUserIdentity(Subject subject, Principal userPrincipal, String[] roles) {
	return new MontyIdentity(userPrincipal, subject, roles);
    }

    @Override
    public RunAsToken newRunAsToken(String runAsName) {
	return new RoleRunAsToken(runAsName);
    }

    @Override
    public UserIdentity getSystemUserIdentity() {
	//Not implemented
	return null;
    }
}
