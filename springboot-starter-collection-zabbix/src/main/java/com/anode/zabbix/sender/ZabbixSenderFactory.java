package com.anode.zabbix.sender;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ZabbixSenderFactory {

	public static ZabbixSender newSender(String pathstring) throws IOException {
		String serverconf = loadZabbixProperties(pathstring).getProperty("ServerActive");
		if (serverconf == null) {
			throw new IOException("Configuration has no reference to ServerActive");
		}
		if (serverconf.contains(":")) {
			return new ZabbixSender(serverconf.split(":")[0], Integer.valueOf(serverconf.split(":")[1]));
		} else {
			return new ZabbixSender(serverconf, 10051);
		}
	}

	private static Properties loadZabbixProperties(String path) throws IOException {
		Properties prop = new Properties();
		try (InputStream in = new FileInputStream(path)) {
			prop.load(in);
		}
		return prop;
	}
}
