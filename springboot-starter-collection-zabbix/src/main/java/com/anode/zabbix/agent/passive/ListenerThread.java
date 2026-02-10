package com.anode.zabbix.agent.passive;

import com.anode.zabbix.metrics.MetricsContainer;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ListenerThread extends Thread {

    private final MetricsContainer container;
    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    /**
     * Construct a new ListenerThread bound to any interface.
     */
    public ListenerThread(MetricsContainer container, int port) throws IOException {
        this.container = container;
        this.serverSocket = new ServerSocket(port, 5);
        this.serverSocket.setSoTimeout(1000);
    }

    /**
     * Construct a new ListenerThread bound to a specific address.
     */
    public ListenerThread(MetricsContainer container, InetAddress address, int port) throws IOException {
        this.container = container;
        this.serverSocket = new ServerSocket(port, 5, address);
        this.serverSocket.setSoTimeout(1000);
    }

    @Override
    public void run() {
        log.debug("ListenerThread Starting.");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new WorkerThread(container, clientSocket).start();

            } catch (SocketTimeoutException ignored) {
                // Timeout allows periodic check for shutdown flag

            } catch (Exception e) {
                log.error("Error accepting client connection.", e);
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            log.error("Error closing the server socket.", e);
        }

        log.debug("ListenerThread Stopped.");
    }

    /**
     * Schedule a shutdown of the listener.
     */
    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
