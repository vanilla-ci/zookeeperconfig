package com.vanillaci.zookeeperconfig;

import com.vanillaci.exceptions.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.log4j.*;

import java.io.*;
import java.lang.reflect.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public final class ConfigBinder<T> implements Closeable {
	private static final Logger logger = Logger.getLogger(ConfigBinder.class);

	private final PathChildrenCache pathCache;
	private final T config;

	private ConfigBinder(PathChildrenCache pathCache, T config) {
		this.pathCache = pathCache;
		this.config = config;
	}

	public static <T> ConfigBinder<T> bind(CuratorFramework client, Class<T> configInterface, String basePath) {
		if (client == null) {
			throw new IllegalArgumentException("null client");
		}
		if (configInterface == null) {
			throw new IllegalArgumentException("null config");
		}
		if (basePath == null || basePath.isEmpty()) {
			throw new IllegalArgumentException("null or empty basePath");
		}

		basePath = cleanBasePath(basePath);

		PathChildrenCache pathCache = new PathChildrenCache(client, basePath, true);

		try {
			pathCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
		} catch (Exception e) {
			throw new CuratorException(e);
		}

		ConfigBinderProxyHandler handler = new ConfigBinderProxyHandler(basePath, pathCache);
		T proxy = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class[] { configInterface }, handler);

		ConfigBinder<T> configBinder = new ConfigBinder<T>(pathCache, proxy);
		return configBinder;
	}

	private static String cleanBasePath(String basePath) {
		if (!basePath.startsWith("/")) {
			basePath = "/" + basePath;
		}

		if (basePath.endsWith("/")) {
			basePath = basePath.substring(0, basePath.length() - 1);
		}
		return basePath;
	}

	@Override
	public void close() throws IOException {
		pathCache.close();
	}

	public T getConfig() {
		return config;
	}
}
