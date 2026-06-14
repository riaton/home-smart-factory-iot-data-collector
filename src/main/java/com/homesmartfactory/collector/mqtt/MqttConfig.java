package com.homesmartfactory.collector.mqtt;

public class MqttConfig {

    private final String endpoint;

    private final int port;

    private final String clientId;

    private final String caCertPath;

    private final String clientCertPath;

    private final String privateKeyPath;

    public MqttConfig(String endpoint, int port, String clientId,
            String caCertPath, String clientCertPath, String privateKeyPath) {
        this.endpoint = endpoint;
        this.port = port;
        this.clientId = clientId;
        this.caCertPath = caCertPath;
        this.clientCertPath = clientCertPath;
        this.privateKeyPath = privateKeyPath;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getPort() {
        return port;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCaCertPath() {
        return caCertPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
}
