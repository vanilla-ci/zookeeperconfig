package com.vanillaci.zookeeperconfig;

import com.google.common.collect.*;
import com.vanillaci.exceptions.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public final class ConfigBinder<T> implements Closeable {
	private final List<PathCache> pathCache;
	private final T config;

	private ConfigBinder(List<PathCache> pathCache, T config) {
		this.pathCache = ImmutableList.copyOf(pathCache);
		this.config = config;
	}

	/**
	 * @param basePaths The base paths to bind to. The order provided will be the order checked, so the most important roots should be listed first.
	 */
	public static <T> ConfigBinder<T> bind(CuratorFramework client, Class<T> configInterface, String ... basePaths) {
		if (client == null) {
			throw new IllegalArgumentException("null client");
		}
		if (configInterface == null) {
			throw new IllegalArgumentException("null config");
		}
		if (basePaths == null || basePaths.length <= 0) {
			throw new IllegalArgumentException("null or empty basePath");
		}

		cleanBasePaths(basePaths);

		ImmutableList.Builder<PathCache> pathCachesBuilder = ImmutableList.builder();
		for (String basePath : basePaths) {

			PathChildrenCache cache = new PathChildrenCache(client, basePath, true);

			try {
				cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
			} catch (Exception e) {
				throw new CuratorException(e);
			}

			PathCache pathCache = new PathCache(basePath, cache);
			pathCachesBuilder.add(pathCache);
		}
		ImmutableList<PathCache> pathCaches = pathCachesBuilder.build();

		ConfigBinderProxyHandler handler = new ConfigBinderProxyHandler(pathCaches);
		T proxy = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class[] { configInterface }, handler);

		ConfigBinder<T> configBinder = new ConfigBinder<T>(pathCaches, proxy);
		return configBinder;
	}

	private static void cleanBasePaths(String... basePaths) {
		for (int i = 0; i < basePaths.length; i++) {
			if (!basePaths[i].startsWith("/")) {
				basePaths[i] = "/" + basePaths[i];
			}

			if (basePaths[i].endsWith("/")) {
				basePaths[i] = basePaths[i].substring(0, basePaths[i].length() - 1);
			}
		}
	}

	@Override
	public void close() throws IOException {
		for (PathCache cache : pathCache) {
			CloseableUtils.closeQuietly(cache.getCache());
		}
	}

	public T getConfig() {
		return config;
	}
}
