package com.vanillaci.zookeeperconfig;

/**
 * Created by joeljohnson on 11/30/14.
 */
public interface ConfigObject {
	@Config // value/path is optional
	public String hostname();

	@Config
	public void hostname(String hostname);

	@Config("hostport")
	public int port();

	@Config(path = "somethingElse", defaultValue = "1")
	public int withDefaultValue();
}
