package com.vanillaci.curatortest;

import org.apache.curator.*;
import org.apache.curator.framework.*;
import org.apache.curator.retry.*;

import java.util.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public class WriteJunk {
	public static void main(String[] args) throws Exception {
		try(CuratorFramework client = getZookeeperClient()) {
			client.start();

			client.setData().forPath("/test/1", ("hello world " + new Random().nextInt()).getBytes());
			//client.create().forPath("/test/1", "hello world 1".getBytes());
			//client.create().forPath("/test/2", "hello world 2".getBytes());
			//client.create().forPath("/test/3", "hello world 3".getBytes());

			List<String> strings = client.getChildren().forPath("/test");
			System.out.println(new String(client.getData().forPath("/test")));
			strings.stream().forEach(i -> {
				try {
					System.out.println(new String(client.getData().forPath("/test/" + i)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	private static CuratorFramework getZookeeperClient() {
		return CuratorFrameworkFactory.builder()
			.connectString("127.0.0.1:2181")
			.retryPolicy(new ExponentialBackoffRetry(1000, 3))
			.connectionTimeoutMs(15_000)
			.sessionTimeoutMs(60_000)
			.build();
	}
}
