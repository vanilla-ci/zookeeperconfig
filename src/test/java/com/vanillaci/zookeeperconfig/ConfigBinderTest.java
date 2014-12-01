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
	private ConfigBinder<ConfigObject> binder;

	@Before
	public void setUp() throws Exception {
		server = new TestingServer(true);
		client = CuratorFrameworkFactory.newClient(server.getConnectString(), new ExponentialBackoffRetry(1000, 3));
		client.start();
	}

	@After
	public void tearDown() throws IOException {
		CloseableUtils.closeQuietly(binder);
		CloseableUtils.closeQuietly(client);
		CloseableUtils.closeQuietly(server);
	}

	@Test
	public void testSimpleBinding() throws Exception {
		binder = ConfigBinder.bind(client, ConfigObject.class, "/test/path/");

		client.create().forPath("/test/path/hostname", "someData".getBytes());
		client.create().forPath("/test/path/hostport", String.valueOf(8080).getBytes());
		waitForZookeeper();

		Assert.assertEquals("someData", binder.getConfig().hostname());
		Assert.assertEquals(8080, binder.getConfig().port());
		Assert.assertEquals(1, binder.getConfig().withDefaultValue());
	}

	@Test
	public void testMultiSimpleBinding() throws Exception {
		binder = ConfigBinder.bind(client, ConfigObject.class, "/test/path", "/test/otherpath");

		client.create().forPath("/test/otherpath/hostname", "someOtherData".getBytes());
		waitForZookeeper();

		Assert.assertEquals("someOtherData", binder.getConfig().hostname());
	}

	@Test
	public void testDefaultsFromServer() throws Exception {
		client.create().creatingParentsIfNeeded().forPath("/test/path");
		client.create().forPath("/test/path/hostname", "someOtherData".getBytes());
		client.create().forPath("/test/path/hostport", String.valueOf(8181).getBytes());

		waitForZookeeper();

		binder = ConfigBinder.bind(client, ConfigObject.class, "/test/path/");

		Assert.assertEquals("someOtherData", binder.getConfig().hostname());
		Assert.assertEquals(8181, binder.getConfig().port());
		Assert.assertEquals(1, binder.getConfig().withDefaultValue());
	}

	private void waitForZookeeper() throws InterruptedException {
		Thread.sleep(5);
	}
}
