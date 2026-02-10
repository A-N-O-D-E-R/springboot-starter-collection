package com.anode.zabbix.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anode.zabbix.ZabbixException;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PassiveAgentClient {

    private final InetAddress agentAddress;
    private final int port;

    public Map<String, Object> getValues(List<String> keys) {
        Map<String, Object> values = new HashMap<>();

        try {
            for (String key : keys) {
                Object value = queryValue(key);
                if (value != null) {
                    values.put(key, value);
                }
            }
            return values;

        } catch (IOException e) {
            throw new ZabbixException(e);
        }
    }

    private Object queryValue(String key) throws IOException {
        try (
                Socket socket = new Socket(agentAddress, port);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                OutputStream output = socket.getOutputStream()
        ) {
            output.write((key + "\n").getBytes(StandardCharsets.UTF_8));
            output.flush();

            String line = input.readLine();
            if (line == null) {
                log.warn("Empty response for key '{}'", key);
                return null;
            }

            return parseValue(stripHeader(line));
        }
    }

    private static String stripHeader(String line) {
        return (line.length() >= 4 && line.startsWith("ZBXD"))
                ? line.substring(13)
                : line;
    }

    private static Object parseValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
        }

        return value;
    }
}
