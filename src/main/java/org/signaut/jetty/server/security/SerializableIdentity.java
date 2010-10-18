package org.signaut.jetty.server.security;

import java.io.Serializable;
import java.security.Principal;

import javax.security.auth.Subject;

import org.eclipse.jetty.server.UserIdentity;

public class SerializableIdentity implements UserIdentity, Serializable {
    private static final long serialVersionUID = 493118349878757632L;

    private final Principal principal;
    private final Subject subject;
    private final String roles[];

    public SerializableIdentity(Principal principal, Subject subject, String[] roles) {
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
        if (scope != null && scope.getRoleRefMap() != null) {
            role = scope.getRoleRefMap().get(role);
        }
        for (String r : roles) {
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