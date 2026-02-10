package com.anode.zabbix.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Result {

	int processed;
	int failed;
	int total;

	float spentSeconds;

	/**
	 * sometimes zabbix server will return "[]".
	 */
	boolean bReturnEmptyArray = false;

	/**
	 * if all sended data are processed, will return true, else return false.
	 */
	public boolean success() {
		return !bReturnEmptyArray && processed == total;
	}

	public int getProcessed() {
		return processed;
	}

	public void setProcessed(int processed) {
		this.processed = processed;
	}

	public int getFailed() {
		return failed;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public float getSpentSeconds() {
		return spentSeconds;
	}

	public void setSpentSeconds(float spentSeconds) {
		this.spentSeconds = spentSeconds;
	}

	public void setbReturnEmptyArray(boolean bReturnEmptyArray) {
		this.bReturnEmptyArray = bReturnEmptyArray;
	}

	@Override
	public String toString() {
		ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			return objectWriter.writeValueAsString(this);
		} catch (JsonProcessingException jsonpexception) {
			log.error("Unable to Parse the object", jsonpexception);
			return null;
		}
	}
}
