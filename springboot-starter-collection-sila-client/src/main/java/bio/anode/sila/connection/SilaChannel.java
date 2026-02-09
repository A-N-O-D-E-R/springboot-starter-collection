package bio.anode.sila.connection;

import io.grpc.ManagedChannel;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public class SilaChannel implements Closeable {

    private final String name;
    private final String host;
    private final int port;
    private final ManagedChannel channel;
    private volatile boolean connected;

    public SilaChannel(String name, String host, int port, ManagedChannel channel) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.channel = channel;
        this.connected = true;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isConnected() {
        return connected && !channel.isShutdown() && !channel.isTerminated();
    }

    @Override
    public void close() {
        connected = false;
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
