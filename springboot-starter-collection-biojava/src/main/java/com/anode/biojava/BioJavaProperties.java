package com.anode.biojava;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biojava")
public class BioJavaProperties {

    private final Cache cache = new Cache();

    public Cache getCache() {
        return cache;
    }

    public static class Cache {
        private int caffeineMaxSize = 100;
        private Redis redis = new Redis();
        private B2 b2 = new B2();

        public int getCaffeineMaxSize() {
            return caffeineMaxSize;
        }

        public void setCaffeineMaxSize(int caffeineMaxSize) {
            this.caffeineMaxSize = caffeineMaxSize;
        }

        public Redis getRedis() {
            return redis;
        }

        public void setRedis(Redis redis) {
            this.redis = redis;
        }

        public B2 getB2() {
            return b2;
        }

        public void setB2(B2 b2) {
            this.b2 = b2;
        }
    }

    public static class Redis {
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }

    public static class B2 {
        private boolean enabled = false;
        private String bucket;
        private String keyId;
        private String applicationKey;
        private String endpoint = "https://s3.us-west-004.backblazeb2.com";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getApplicationKey() {
            return applicationKey;
        }

        public void setApplicationKey(String applicationKey) {
            this.applicationKey = applicationKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
