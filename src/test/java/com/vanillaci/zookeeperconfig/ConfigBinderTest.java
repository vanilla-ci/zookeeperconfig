package com.vanillaci.zookeeperconfig;

import org.apache.curator.framework.*;
import org.apache.curator.retry.*;
import org.apache.curator.test.*;
import org.apache.curator.utils.*;
import org.junit.*;

import java.io.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public class ConfigBinderTest {
	private TestingServer server;
	private CuratorFramework client;
	private ConfigObject configObject;
	private ConfigBinder<ConfigObject> binder;

	@Before
	public void setUp() throws Exception {
		server = new TestingServer(true);
		client = CuratorFrameworkFactory.newClient(server.getConnectString(), new ExponentialBackoffRetry(1000, 3));
		client.start();

		configObject = new ConfigObject();
	}

	@After
	public void tearDown() throws IOException {
		CloseableUtils.closeQuietly(binder);
		CloseableUtils.closeQuietly(client);
		CloseableUtils.closeQuietly(server);
	}

	@Test
	public void testSimpleBinding() throws Exception {
		binder = ConfigBinder.bind(client, configObject, "/test/path/");

		client.create().forPath("/test/path/hostname", "someData".getBytes());
		client.create().forPath("/test/path/hostport", String.valueOf(8080).getBytes());
		waitForZookeeper();

		Assert.assertEquals("someData", configObject.getHostname());
		Assert.assertEquals(8080, configObject.getPort());
		Assert.assertEquals(1, configObject.getDefaultValue());
	}

	@Test
	public void testDefaultsFromServer() throws Exception {
		client.create().creatingParentsIfNeeded().forPath("/test/path");
		client.create().forPath("/test/path/hostname", "someData".getBytes());
		client.create().forPath("/test/path/hostport", String.valueOf(8080).getBytes());

		waitForZookeeper();

		binder = ConfigBinder.bind(client, configObject, "/test/path/");

		Assert.assertEquals("someData", configObject.getHostname());
		Assert.assertEquals(8080, configObject.getPort());
		Assert.assertEquals(1, configObject.getDefaultValue());
	}

	@Test
	public void testComplexObjects() throws Exception {
		binder = ConfigBinder.bind(client, configObject, "/test/path/");

		client.create().forPath("/test/path/complex", "{\"someKey\":\"someValue\"}".getBytes());
		waitForZookeeper();

		Assert.assertNotNull(configObject.getComplex());
		Assert.assertEquals("someValue", configObject.getComplex().get("someKey"));
		Assert.assertEquals(1, configObject.getComplex().size());
	}

	private void waitForZookeeper() throws InterruptedException {
		Thread.sleep(5);
	}
}
