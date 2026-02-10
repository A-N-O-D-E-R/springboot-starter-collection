package com.anode.zabbix.sender;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Request {

	static final byte header[] = { 'Z', 'B', 'X', 'D', '\1' };

	String request = "sender data";

	/**
	 * TimeUnit is SECONDS.
	 */
	long clock;

	List<DataObject> data;

	public byte[] toBytes() throws JsonProcessingException {
		ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonString = objectWriter.writeValueAsString(this);
		byte[] jsonBytes = jsonString.getBytes();
		byte[] result = new byte[header.length + 4 + 4 + jsonBytes.length];

		System.arraycopy(header, 0, result, 0, header.length);

		result[header.length] = (byte) (jsonBytes.length & 0xFF);
		result[header.length + 1] = (byte) ((jsonBytes.length >> 8) & 0x00FF);
		result[header.length + 2] = (byte) ((jsonBytes.length >> 16) & 0x0000FF);
		result[header.length + 3] = (byte) ((jsonBytes.length >> 24) & 0x000000FF);

		System.arraycopy(jsonBytes, 0, result, header.length + 4 + 4, jsonBytes.length);
		return result;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public long getClock() {
		return clock;
	}

	public void setClock(long clock) {
		this.clock = clock;
	}

	public List<DataObject> getData() {
		return data;
	}

	public void setData(List<DataObject> data) {
		this.data = data;
	}
}
