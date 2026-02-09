package bio.anode.sila.connection;

import bio.anode.sila.SiLAProperties;
import bio.anode.sila.exception.SilaConnectionException;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class SilaChannelFactory {

    public SilaChannel create(SiLAProperties.ServerConnection config) {
        ManagedChannel channel;
        if (config.isUseTls()) {
            channel = buildTlsChannel(config);
        } else {
            channel = buildPlaintextChannel(config);
        }
        return new SilaChannel(config.getName(), config.getHost(), config.getPort(), channel);
    }

    public SilaChannel create(String name, String host, int port) {
        ManagedChannel channel = NettyChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        return new SilaChannel(name, host, port, channel);
    }

    private ManagedChannel buildPlaintextChannel(SiLAProperties.ServerConnection config) {
        return NettyChannelBuilder
                .forAddress(config.getHost(), config.getPort())
                .usePlaintext()
                .build();
    }

    private ManagedChannel buildTlsChannel(SiLAProperties.ServerConnection config) {
        try {
            SslContextBuilder sslBuilder = GrpcSslContexts.forClient();

            if (config.getCaCertPath() != null) {
                sslBuilder.trustManager(new File(config.getCaCertPath()));
            }

            if (config.getClientCertPath() != null && config.getClientKeyPath() != null) {
                sslBuilder.keyManager(
                        new File(config.getClientCertPath()),
                        new File(config.getClientKeyPath())
                );
            }

            SslContext sslContext = sslBuilder.build();

            return NettyChannelBuilder
                    .forAddress(config.getHost(), config.getPort())
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            throw new SilaConnectionException(
                    "Failed to build TLS channel for " + config.getName(), e);
        }
    }
}
