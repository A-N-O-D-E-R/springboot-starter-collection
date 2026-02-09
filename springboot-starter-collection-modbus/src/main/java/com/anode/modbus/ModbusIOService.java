package com.anode.modbus;

import com.anode.modbus.protocol.ModbusMaster;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import java.util.Map;

public class ModbusIOService {

    private final Map<String, ModbusMaster> masters;

    public ModbusIOService(Map<String, ModbusMaster> masters) {
        this.masters = masters;
    }

    /**
     * Select the connection by name: "connection1", "connection2", etc.
     */
    private ModbusMaster getMaster(String connectionName) {
        ModbusMaster master = masters.get(connectionName);
        if (master == null) {
            throw new IllegalArgumentException("Unknown Modbus connection: " + connectionName);
        }
        return master;
    }

    public synchronized Object readHoldingRegisters(
            String connectionName,
            int slaveId,
            int offset) throws ModbusException {

        ModbusMaster master = getMaster(connectionName);
        return master.readHoldingRegisters(slaveId, offset, 1)[0].getValue();
    }

    public synchronized void writeHoldingRegisters(
            String connectionName,
            int slaveId,
            int offset,
            int value) throws ModbusException {

        ModbusMaster master = getMaster(connectionName);

        Register[] registers = new Register[1];
        registers[0] = new SimpleInputRegister(value);

        master.writeHoldingRegisters(slaveId, offset, registers);
    }
}
