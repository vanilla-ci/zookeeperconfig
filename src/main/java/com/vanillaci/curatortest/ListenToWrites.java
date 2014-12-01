package com.vanillaci.curatortest;

import org.apache.curator.framework.*;
import org.apache.curator.framework.api.*;
import org.apache.curator.retry.*;

import java.util.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public class ListenToWrites {
	public static void main(String[] args) throws Exception {
		try(CuratorFramework client = getZookeeperClient()) {
			client.start();

			CuratorListener curatorListener = (client1, event) -> {
				System.out.println("Recieved update to: " + event.getPath());

			};
			client.getCuratorListenable().addListener(curatorListener);

			//client.getChildren().watched().forPath("/test");
			client.getData().inBackground().forPath("/test");

			Thread.sleep(1000 * 60 * 3);
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
