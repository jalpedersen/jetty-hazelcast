package org.signaut.jetty.server.security;

import java.io.Serializable;
import java.security.Principal;

public class SerializablePrincipal implements Principal, Serializable {
    private static final long serialVersionUID = -2877462296027623856L;
    private final String name;

    public SerializablePrincipal(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}