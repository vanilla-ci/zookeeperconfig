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
 * Used to bind a path's children to an interface.
 *
 * @author Joel Johnson
 */
public final class ConfigBinder<T> implements Closeable {
	private final List<PathCache> pathCache;
	private final T boundObject;

	private ConfigBinder(List<PathCache> pathCache, T boundObject) {
		this.pathCache = ImmutableList.copyOf(pathCache);
		this.boundObject = boundObject;
	}

	/**
	 * @param client The instance of the curator framework client to be used to fetch/set data
	 * @param configInterface The interface that will be implemented by the binder to expose the values from the zookeeper instance.
	 * @param primaryBasePath The first path to check for requested values. Also the base path to use when setting values.
	 * @param basePaths Additional base paths to bind to. These paths treated as read-only and are never updated. If the primary base path doesn't have a value for a node with a requested name, these paths are also checked.
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

	/**
	 * Should be called when done syncing, typically on application shutdown.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		for (PathCache cache : pathCache) {
			CloseableUtils.closeQuietly(cache.getCache());
		}
	}

	/**
	 * @return The object to interface with the data stored on zookeeper.
	 */
	public T getBoundObject() {
		return boundObject;
	}
}
