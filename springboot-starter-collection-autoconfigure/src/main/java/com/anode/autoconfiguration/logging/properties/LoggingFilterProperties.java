package com.anode.autoconfiguration.logging.properties;

/**
 * Configuration properties for MDC filter.
 */
public class LoggingFilterProperties {
    private boolean enabled = true;
    private String userKey = "user";
    private String roleKey = "role";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }
}
