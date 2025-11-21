package com.anode.modbus.protocol;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.Register;

public interface ModbusMaster {
    Register[] readHoldingRegisters(int slaveId, int offset, int quantity) throws ModbusException;

    void writeHoldingRegisters(int slaveId, int offset, Register[] registers) throws ModbusException;
}
