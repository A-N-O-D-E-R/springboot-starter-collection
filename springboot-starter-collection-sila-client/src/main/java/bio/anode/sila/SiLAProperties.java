package bio.anode.sila;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sila")
public class SiLAProperties {

    private boolean enabled = true;

    private List<ServerConnection> servers = new ArrayList<>();

    private Discovery discovery = new Discovery();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ServerConnection> getServers() {
        return servers;
    }

    public void setServers(List<ServerConnection> servers) {
        this.servers = servers;
    }

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    public static class ServerConnection {

        private String name;

        private String host;

        private int port = 50052;

        private boolean useTls = false;

        private String clientCertPath;

        private String clientKeyPath;

        private String caCertPath;

        private int connectTimeout = 5000;

        private boolean eagerConnect = true;

        private boolean autoReconnect = true;

        private long reconnectIntervalMs = 5000;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isUseTls() {
            return useTls;
        }

        public void setUseTls(boolean useTls) {
            this.useTls = useTls;
        }

        public String getClientCertPath() {
            return clientCertPath;
        }

        public void setClientCertPath(String clientCertPath) {
            this.clientCertPath = clientCertPath;
        }

        public String getClientKeyPath() {
            return clientKeyPath;
        }

        public void setClientKeyPath(String clientKeyPath) {
            this.clientKeyPath = clientKeyPath;
        }

        public String getCaCertPath() {
            return caCertPath;
        }

        public void setCaCertPath(String caCertPath) {
            this.caCertPath = caCertPath;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public boolean isEagerConnect() {
            return eagerConnect;
        }

        public void setEagerConnect(boolean eagerConnect) {
            this.eagerConnect = eagerConnect;
        }

        public boolean isAutoReconnect() {
            return autoReconnect;
        }

        public void setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
        }

        public long getReconnectIntervalMs() {
            return reconnectIntervalMs;
        }

        public void setReconnectIntervalMs(long reconnectIntervalMs) {
            this.reconnectIntervalMs = reconnectIntervalMs;
        }
    }

    public static class Discovery {

        private boolean enabled = false;

        private String serviceType = "_sila._tcp.local.";

        private int timeoutMs = 10000;

        private boolean autoConnect = false;

        private String serverNameFilter;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public void setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
        }

        public String getServerNameFilter() {
            return serverNameFilter;
        }

        public void setServerNameFilter(String serverNameFilter) {
            this.serverNameFilter = serverNameFilter;
        }
    }
}
