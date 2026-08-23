package com.anode.zabbix.agent.passive;

import com.anode.zabbix.metrics.MetricsContainer;
import com.anode.zabbix.metrics.MetricsException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WorkerThread extends Thread {

    private final MetricsContainer container;
    private final Socket socket;

    @Override
    public void run() {
        String client = socket.getInetAddress().getHostAddress();
        log.debug("Accepted Connection From: {}", client);

        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            socket.setSoTimeout(1000);

            String inputLine = in.readLine();
            if (inputLine != null && inputLine.startsWith("ZBXD")) {
                inputLine = inputLine.substring(13);
            }

            if (inputLine != null) {
                try {
                    Object value = container.getMetric(inputLine);
                    out.print(value.toString());
                    out.flush();
                } catch (MetricsException me) {
                    log.error("Client: {} Sent Unknown Key: {}", client, inputLine);
                    out.print("ZBX_NOTSUPPORTED");
                    out.flush();
                }
            }

        } catch (SocketTimeoutException ste) {
            log.debug("{}: Timeout detected.", client);
        } catch (Exception e) {
            log.error("{}: Error: {}", client, e.toString(), e);
        } finally {
            log.debug("{}: Disconnected.", client);
        }
    }
}
