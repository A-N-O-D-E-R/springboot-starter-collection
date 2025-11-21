package com.anode.modbus.protocol;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.procimg.Register;

public class RtuModbusMaster implements ModbusMaster {

    private final SerialConnection connection;

    public RtuModbusMaster(SerialConnection connection) {
        this.connection = connection;
    }

    @Override
    public Register[] readHoldingRegisters(int slaveId, int offset, int quantity) throws ModbusException {
        ModbusSerialTransaction tx = new ModbusSerialTransaction(connection);
        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(offset, quantity);
        req.setUnitID(slaveId);

        tx.setRequest(req);
        tx.execute();
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) tx.getResponse();
        return res.getRegisters();
    }

    @Override
    public void writeHoldingRegisters(int slaveId, int offset, Register[] registers) throws ModbusException {
        ModbusSerialTransaction tx = new ModbusSerialTransaction(connection);
        WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(offset, registers);
        req.setUnitID(slaveId);

        tx.setRequest(req);
        tx.execute();
    }
}
