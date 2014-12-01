package com.vanillaci.zookeeperconfig;

import java.util.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public class ConfigObject {
	@Config // value/path is optional
	private String hostname;

	@Config("hostport")
	private int port;

	@Config(path = "somethingElse", defaultValue = "1")
	private int defaultValue;

	@Config
	private Map<String, String> complex;

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public int getDefaultValue() {
		return defaultValue;
	}

	public Map<String, String> getComplex() {
		return complex;
	}
}
