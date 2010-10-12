package org.signaut.jetty.server.session;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

class ClusterSessionData implements Serializable {
    private static final long serialVersionUID = -2410271220399050750L;
    private long created;
    private boolean idChanged;
    private long maxIdleMs;
    private long accessed;
    private boolean keepAlive;

    private Set<String> keys = new HashSet<String>();

    public long getCreated() {
	return created;
    }

    public void setCreated(long created) {
	this.created = created;
	this.accessed = created;
    }

    public boolean isIdChanged() {
	return idChanged;
    }

    public void setIdChanged(boolean idChanged) {
	this.idChanged = idChanged;
    }

    public long getMaxIdleMs() {
	return maxIdleMs;
    }

    public void setMaxIdleMs(long maxIdleMs) {
	this.maxIdleMs = maxIdleMs;
    }

    public long getAccessed() {
	return accessed;
    }

    public void setAccessed(long accessed) {
	this.accessed = accessed;
    }

    public Set<String> getKeys() {
	return keys;
    }

    public void setKeys(Set<String> keys) {
	this.keys = keys;
    }

    public void setKeepAlive(boolean keepAlive) {
	this.keepAlive = keepAlive;
    }

    public boolean isKeepAlive() {
	return keepAlive;
    }
}
