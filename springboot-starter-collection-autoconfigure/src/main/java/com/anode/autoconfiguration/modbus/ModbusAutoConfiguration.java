package com.anode.autoconfiguration.modbus;

import com.anode.modbus.ModbusIOService;
import com.anode.modbus.ModbusProperties;
import com.anode.modbus.protocol.ModbusMaster;
import com.anode.modbus.protocol.RtuModbusMaster;
import com.anode.modbus.protocol.TcpModbusMaster;
import com.anode.modbus.protocol.UdpModbusMaster;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.net.UDPMasterConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(TCPMasterConnection.class)
@EnableConfigurationProperties(ModbusProperties.class)
public class ModbusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ModbusIOService modbusIOService(Map<String, ModbusMaster> modbusConnections) {
        return new ModbusIOService(modbusConnections);
    }

    @Bean
    @ConditionalOnMissingBean
    public Map<String, ModbusMaster> modbusConnections(ModbusProperties props) {
        Map<String, ModbusMaster> clients = new HashMap<>();

        for (ModbusProperties.Connection config : props.getConnections()) {
            URI uri = URI.create(config.getHost());
            String scheme = uri.getScheme();
            ModbusMaster client;

            switch (scheme) {
                case "tcp":
                    client = createTcpClient(uri, config.getTimeout());
                    break;

                case "udp":
                    client = createUdpClient(uri, config.getTimeout());
                    break;

                case "rtu":
                    client = createRtuClient(uri, config);
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported protocol: " + scheme);
            }

            clients.put(config.getName(), client);
        }

        return clients;
    }

    private ModbusMaster createTcpClient(URI uri, int timeout) {
        try {
            TCPMasterConnection connection =
                new TCPMasterConnection(InetAddress.getByName(uri.getHost()));
            connection.setPort(uri.getPort());
            connection.setTimeout(timeout);
            connection.connect();
            return new TcpModbusMaster(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ModbusMaster createUdpClient(URI uri, int timeout) {
        try {
            UDPMasterConnection connection =
                new UDPMasterConnection(InetAddress.getByName(uri.getHost()));
            connection.setPort(uri.getPort());
            connection.setTimeout(timeout);
            connection.connect();
            return new UdpModbusMaster(connection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create UDP Modbus connection", e);
        }
    }

    private ModbusMaster createRtuClient(URI uri, ModbusProperties.Connection config) {
        try {
            String device = uri.getPath().substring(0,uri.getPath().lastIndexOf(":"));
            int baudRate = Integer.parseInt(uri.getPath().substring(uri.getPath().lastIndexOf(":")+1));

            SerialParameters params = new SerialParameters();
            params.setPortName(device);
            params.setBaudRate(baudRate);

            switch (config.getParity().toLowerCase()) {
                case "even":
                    params.setParity("Even");
                    break;
                case "odd":
                    params.setParity("Odd");
                    break;
                default:
                    params.setParity("None");
            }

            params.setStopbits(config.getStopBits());
            params.setDatabits(config.getDataBits());

            SerialConnection connection = new SerialConnection(params);
            connection.setTimeout(config.getTimeout());
            connection.open();

            return new RtuModbusMaster(connection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RTU Modbus connection: " + uri.getHost(), e);
        }
    }
}

