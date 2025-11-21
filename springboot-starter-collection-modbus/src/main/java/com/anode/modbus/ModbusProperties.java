package com.anode.modbus;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "modbus")
public class ModbusProperties {

    private List<Connection> connections = new ArrayList<>();

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }

    public static class Connection {

        private String name;
        private String host;     // tcp://ip:port, udp://ip:port, rtu://device:baud
        private int timeout = 2000;

        // RTU-specific fields (only used for rtu://)
        private String parity = "none";   // none, even, odd
        private int stopBits = 1;         // 1 or 2
        private int dataBits = 8;         // typically 7 or 8

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public String getParity() { return parity; }
        public void setParity(String parity) { this.parity = parity; }

        public int getStopBits() { return stopBits; }
        public void setStopBits(int stopBits) { this.stopBits = stopBits; }

        public int getDataBits() { return dataBits; }
        public void setDataBits(int dataBits) { this.dataBits = dataBits; }
    }
}
