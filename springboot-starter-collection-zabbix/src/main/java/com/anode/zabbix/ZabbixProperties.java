package com.anode.zabbix;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "zabbix")
public class ZabbixProperties {

    private boolean enabled = true;

    private Passive passive = new Passive();
    private Active active = new Active();

    @Getter
    @Setter
    public static class Passive {
        private boolean enabled = true;
        private String listenAddress;
        private int listenPort = 10050;
    }

    @Getter
    @Setter
    public static class Active {
        private boolean enabled = false;
        private String hostName;
        private String serverAddress;
        private int serverPort = 10051;
        private int refreshInterval = 120;
        private String pskIdentity;
        private String psk;
    }

    @Getter
    @Setter
    public static class Sender {
        private String host;
        private int port = 10051;
        private int connectTimeout = 3000;
        private int socketTimeout = 3000;
    }

    private Sender sender = new Sender();
}
