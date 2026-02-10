package com.anode.zabbix;

@SuppressWarnings("serial")
public class ZabbixException extends RuntimeException {
	public ZabbixException() {
		super();
	}
	public ZabbixException(String message, Throwable cause) {
		super(message, cause);
	}
	public ZabbixException(String message) {
		super(message);
	}
	public ZabbixException(Throwable cause) {
		super(cause);
	}
}
