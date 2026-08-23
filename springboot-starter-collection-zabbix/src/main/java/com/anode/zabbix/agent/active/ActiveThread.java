package com.anode.zabbix.agent.active;

import java.io.*;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.*;

import org.bouncycastle.tls.BasicTlsPSKIdentity;
import org.bouncycastle.tls.PSKTlsClient;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.json.JSONArray;
import org.json.JSONObject;

import com.anode.zabbix.ZabbixException;
import com.anode.zabbix.metrics.MetricsContainer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ActiveThread extends Thread {

    private final MetricsContainer metricsContainer;
    private final String hostName;
    private final String serverAddress;
    private final int serverPort;
    private final int refreshInterval;
    private final String pskIdentity;
    private final String psk;

    private volatile boolean running = true;
    private final Map<Integer, List<String>> checks = new HashMap<>();
    private final Map<Integer, Long> lastChecked = new HashMap<>();
    private long lastRefresh;

    @Override
    public void run() {
        log.debug("ActiveThread Starting.");

        try {
            log.debug("Starting initial refresh of active checks.");
            requestActiveChecks();
            log.debug("Initial refresh of active checks completed.");
        } catch (Exception e) {
            log.error("Initial refresh failed.", e);
        }

        while (running) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                return;
            }

            long now = System.currentTimeMillis() / 1000;

            if ((now - lastRefresh) >= refreshInterval) {
                try {
                    requestActiveChecks();
                } catch (Exception e) {
                    log.error("Unable to refresh.", e);
                }
            }

            checks.forEach((delay, keyList) -> {
                long last = lastChecked.getOrDefault(delay, 0L);
                if (now - last >= delay) {
                    try {
                        sendMetrics(delay, keyList);
                    } catch (Exception e) {
                        log.error("Unable to send metrics.", e);
                    }
                }
            });
        }
    }

    private void requestActiveChecks() throws Exception {
        log.debug("Requesting a list of active checks from the server.");

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            InputStream input;
            OutputStream output;
            TlsClientProtocol protocol = null;

            if (pskIdentity != null && psk != null) {
                protocol = setupTls(socket);
                input = protocol.getInputStream();
                output = protocol.getOutputStream();
            } else {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            }

            JSONObject request = new JSONObject();
            request.put("request", "active checks");
            request.put("host", hostName);

            output.write(getRequest(request));
            output.flush();

            byte[] responseBytes = readFully(input);
            if (protocol != null) protocol.close();

            JSONObject response = getResponse(responseBytes);

            if ("success".equals(response.getString("response"))) {
                refreshFromActiveChecksResponse(response);
            } else {
                log.warn("Server reported failure: {}", response.optString("info"));
            }

            lastRefresh = System.currentTimeMillis() / 1000;
        }
    }

    private void refreshFromActiveChecksResponse(JSONObject response) {
        ActiveChecksResponseIndex index = getActiveChecksResponseIndex(response);
        insertNewChecks(index);
        pruneChangedChecks(index);
        pruneUnusedDelays(index);
    }

    private ActiveChecksResponseIndex getActiveChecksResponseIndex(JSONObject response) {
        ActiveChecksResponseIndex index = new ActiveChecksResponseIndex();
        JSONArray data = response.getJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject check = data.getJSONObject(i);
            index.add(check.getString("key"), check.getInt("delay"));
        }
        return index;
    }

    private void insertNewChecks(ActiveChecksResponseIndex index) {
        long now = System.currentTimeMillis() / 1000;
        index.getIndex().forEach((key, delay) -> {
            checks.computeIfAbsent(delay, k -> new ArrayList<>());
            List<String> keysForDelay = checks.get(delay);
            if (!keysForDelay.contains(key)) keysForDelay.add(key);
            lastChecked.putIfAbsent(delay, now);
        });
    }

    private void pruneChangedChecks(ActiveChecksResponseIndex index) {
        checks.forEach((delay, keyList) ->
                keyList.removeIf(key -> {
                    Integer currentDelay = index.getIndex().get(key);
                    boolean remove = currentDelay == null || currentDelay != delay;
                    if (remove) log.debug("Removing '{}' from delay '{}'", key, delay);
                    return remove;
                })
        );
    }

    private void pruneUnusedDelays(ActiveChecksResponseIndex index) {
        lastChecked.keySet().removeIf(delay -> !index.getDelays().contains(delay));
        checks.keySet().removeIf(delay -> !index.getDelays().contains(delay));
    }

    private void sendMetrics(int delay, List<String> keyList) throws Exception {
        log.debug("Sending metrics for delay '{}' with keys: {}", delay, keyList);

        long clock = System.currentTimeMillis() / 1000;
        JSONObject metrics = new JSONObject();
        metrics.put("request", "agent data");

        JSONArray data = new JSONArray();
        for (String keyName : keyList) {
            JSONObject key = new JSONObject();
            key.put("host", hostName);
            key.put("key", keyName);

            try {
                Object value = metricsContainer.getMetric(keyName);
                key.put("value", String.valueOf(value));
            } catch (Exception e) {
                key.put("value", "ZBX_NOTSUPPORTED");
            }

            key.put("clock", clock);
            data.put(key);
        }

        metrics.put("data", data);
        metrics.put("clock", clock);

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            InputStream input;
            OutputStream output;
            TlsClientProtocol protocol = null;

            if (pskIdentity != null && psk != null) {
                protocol = setupTls(socket);
                input = protocol.getInputStream();
                output = protocol.getOutputStream();
            } else {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            }

            output.write(getRequest(metrics));
            output.flush();

            byte[] responseBytes = readFully(input);
            if (protocol != null) protocol.close();

            JSONObject response = getResponse(responseBytes);
            if (!"success".equals(response.getString("response"))) {
                log.error("Failed to send metrics.");
            }
        }

        lastChecked.put(delay, clock);
    }

    /** Shut down this thread */
    public void shutdown() {
        running = false;
        interrupt();
    }

    private static class ActiveChecksResponseIndex {
        private final Map<String, Integer> index = new HashMap<>();
        private final List<Integer> delays = new ArrayList<>();

        void add(String key, int delay) {
            index.put(key, delay);
            if (!delays.contains(delay)) delays.add(delay);
        }

        Map<String, Integer> getIndex() { return index; }
        List<Integer> getDelays() { return delays; }
    }

    private byte[] readFully(InputStream input) throws Exception {
        byte[] header = input.readNBytes(13); // "ZBXD\1" (5 bytes) + length (8 bytes)
        if (header.length < 13) {
            throw new ZabbixException("Incomplete Zabbix response header");
        }

        byte[] sizeBuffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            sizeBuffer[7 - i] = header[5 + i];
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(sizeBuffer);
        DataInputStream dis = new DataInputStream(bais);
        long size = dis.readLong();
        dis.close();

        byte[] data = input.readNBytes((int) size);
        byte[] result = new byte[13 + data.length];
        System.arraycopy(header, 0, result, 0, 13);
        System.arraycopy(data, 0, result, 13, data.length);
        return result;
    }

    private TlsClientProtocol setupTls(Socket socket) throws Exception {
        byte[] pskBytes = hexStringToByteArray(psk);
        BcTlsCrypto crypto = new BcTlsCrypto(new SecureRandom());
        BasicTlsPSKIdentity pskIdentityObj = new BasicTlsPSKIdentity(pskIdentity, pskBytes);
        PSKTlsClient client = new PSKTlsClient(crypto, pskIdentityObj);
        TlsClientProtocol protocol = new TlsClientProtocol(
                socket.getInputStream(), socket.getOutputStream());
        protocol.connect(client);
        return protocol;
    }

    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private byte[] getRequest(JSONObject jsonObject) throws Exception {
        byte[] requestBytes = jsonObject.toString().getBytes();

        String header = "ZBXD\1";
        byte[] headerBytes = header.getBytes();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeLong(requestBytes.length);
        dos.flush();
        dos.close();
        bos.close();
        byte[] requestLengthBytes = bos.toByteArray();

        byte[] allBytes = new byte[headerBytes.length + requestLengthBytes.length + requestBytes.length];

        int index = 0;
        for (int i = 0; i < headerBytes.length; i++) {
            allBytes[index++] = headerBytes[i];
        }
        for (int i = 0; i < requestLengthBytes.length; i++) {
            allBytes[index++] = requestLengthBytes[7 - i];
        }
        for (int i = 0; i < requestBytes.length; i++) {
            allBytes[index++] = requestBytes[i];
        }

        return allBytes;
    }

    private JSONObject getResponse(byte[] responseBytes) throws Exception {
        byte[] sizeBuffer = new byte[8];
        int index = 0;
        for (int i = 12; i > 4; i--) {
            sizeBuffer[index++] = responseBytes[i];
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(sizeBuffer);
        DataInputStream dis = new DataInputStream(bais);
        long size = dis.readLong();
        dis.close();
        bais.close();

        byte[] jsonBuffer = new byte[responseBytes.length - 13];
        if (jsonBuffer.length != size) {
            throw new ZabbixException("Reported and actual buffer sizes differ!");
        }

        index = 0;
        for (int i = 13; i < responseBytes.length; i++) {
            jsonBuffer[index++] = responseBytes[i];
        }

        JSONObject response = new JSONObject(new String(jsonBuffer));

        return response;
    }
}
