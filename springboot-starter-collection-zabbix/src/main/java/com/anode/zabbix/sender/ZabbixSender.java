package com.anode.zabbix.sender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ZabbixSender {
	private static final Pattern PATTERN = Pattern.compile("[^0-9\\.]+");
	private static final Charset UTF8 = Charset.forName("UTF-8");

	String host;
	int port;
	int connectTimeout = 3 * 1000;
	int socketTimeout = 3 * 1000;

	public ZabbixSender(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public ZabbixSender(String host, int port, int connectTimeout, int socketTimeout) {
		this(host, port);
		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
	}

	public Result send(DataObject dataObject) throws IOException {
		return send(dataObject, System.currentTimeMillis() / 1000);
	}

	public Result send(DataObject dataObject, long clock) throws IOException {
		return send(Collections.singletonList(dataObject), clock);
	}

	public Result send(List<DataObject> dataObjectList) throws IOException {
		return send(dataObjectList, System.currentTimeMillis() / 1000);
	}

	public Result send(List<DataObject> dataObjectList, long clock) throws IOException {
		Result senderResult = new Result();

		Socket socket = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			socket = new Socket();

			socket.setSoTimeout(socketTimeout);
			socket.connect(new InetSocketAddress(host, port), connectTimeout);

			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();

			Request senderRequest = new Request();
			senderRequest.setData(dataObjectList);
			senderRequest.setClock(clock);

			outputStream.write(senderRequest.toBytes());

			outputStream.flush();

			byte[] responseData = new byte[512];

			int readCount = 0;

			while (true) {
				int read = inputStream.read(responseData, readCount, responseData.length - readCount);
				if (read <= 0) {
					break;
				}
				readCount += read;
			}

			if (readCount < 13) {
				senderResult.setbReturnEmptyArray(true);
			}

			// header('ZBXD\1') + len + 0 --> 5 + 4 + 4
			String jsonString = new String(responseData, 13, readCount - 13, UTF8);

			String info = (String) new ObjectMapper().readValue(jsonString, Map.class).get("info");
			String[] split = PATTERN.split(info);

			senderResult.setProcessed(Integer.parseInt(split[1]));
			senderResult.setFailed(Integer.parseInt(split[2]));
			senderResult.setTotal(Integer.parseInt(split[3]));
			senderResult.setSpentSeconds(Float.parseFloat(split[4]));

		} finally {
			if (socket != null) {
				socket.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}

		return senderResult;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
}
