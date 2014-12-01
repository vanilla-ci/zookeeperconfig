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
	 * @param primaryBasePath The first path to check for requested values.
	 * @param basePaths Additional base paths to bind to. If the primary base path doesn't have a value for a node with a requested name, these paths are also checked.
	 */
	public static <T> ConfigBinder<T> bind(CuratorFramework client, Class<T> configInterface, String primaryBasePath, String ... basePathsArray) {
		if (client == null) {
			throw new IllegalArgumentException("null client");
		}
		if (configInterface == null) {
			throw new IllegalArgumentException("null config");
		}

		ImmutableList.Builder<String> builder = ImmutableList.builder();
		builder.add(primaryBasePath);
		builder.add(basePathsArray);
		List<String> basePaths = builder.build();

		ImmutableList.Builder<PathCache> pathCachesBuilder = ImmutableList.builder();
		for (String basePath : basePaths) {
			basePath = cleanBasePath(basePath);
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

		ConfigBinderProxyHandler handler = new ConfigBinderProxyHandler(client, pathCaches);
		T proxy = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class[] { configInterface }, handler);

		ConfigBinder<T> configBinder = new ConfigBinder<T>(pathCaches, proxy);
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
		for (PathCache cache : pathCache) {
			CloseableUtils.closeQuietly(cache.getCache());
		}
	}

	public T getConfig() {
		return config;
	}
}
