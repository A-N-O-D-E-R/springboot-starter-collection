package com.anode.zabbix.metrics;

@SuppressWarnings("serial")
public class MetricsException extends RuntimeException {
	public MetricsException() {
		super();
	}
	public MetricsException(String message, Throwable cause) {
		super(message, cause);
	}
	public MetricsException(String message) {
		super(message);
	}
	public MetricsException(Throwable cause) {
		super(cause);
	}
}
