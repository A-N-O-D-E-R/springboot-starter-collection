package com.anode.modbus.protocol;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;

public class TcpModbusMaster implements ModbusMaster {

    private final TCPMasterConnection connection;

    public TcpModbusMaster(TCPMasterConnection connection) {
        this.connection = connection;
    }

    @Override
    public Register[] readHoldingRegisters(int slaveId, int offset, int quantity) throws ModbusException {
        ModbusTCPTransaction tx = new ModbusTCPTransaction(connection);
        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(offset, quantity);
        req.setUnitID(slaveId);

        tx.setRequest(req);
        tx.execute();
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) tx.getResponse();
        return res.getRegisters();
    }

    @Override
    public void writeHoldingRegisters(int slaveId, int offset, Register[] registers) throws ModbusException {
        ModbusTCPTransaction tx = new ModbusTCPTransaction(connection);
        WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(offset, registers);
        req.setUnitID(slaveId);

        tx.setRequest(req);
        tx.execute();
    }
}