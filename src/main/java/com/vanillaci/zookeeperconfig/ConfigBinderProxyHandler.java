package com.vanillaci.zookeeperconfig;

import com.fasterxml.jackson.databind.*;
import com.google.common.collect.*;
import com.vanillaci.exceptions.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.zookeeper.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Created by joeljohnson on 12/1/14.
 */
public class ConfigBinderProxyHandler implements InvocationHandler {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final CuratorFramework client;
	private final List<PathCache> pathCaches;

	public ConfigBinderProxyHandler(CuratorFramework client, List<PathCache> pathCaches) {
		this.client = client;
		this.pathCaches = ImmutableList.copyOf(pathCaches);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(args == null || args.length == 0) {
			return handleGet(method);
		}

		if(args != null && args.length == 1) {
			handleSet(method, args[0]);
			return null;
		}

		throw new UnsupportedOperationException("Only methods with no arguments and a return value (getters), or methods with a single argument and void return value (setters) can be invoked.");
	}

	private Object handleGet(Method method) {
		final String pathName = findPathName(method);

		Class<?> returnType = method.getReturnType();

		for (PathCache pathCache : pathCaches) {
			final String fullPath = pathCache.getBasePath() + "/" + pathName;

			ChildData currentData = pathCache.getCache().getCurrentData(fullPath);
			if (currentData != null) {
				Object result = new String(currentData.getData());
				return convertResultToReturnType(result, returnType);
			}
		}

		return defaultValue(method);
	}

	private void handleSet(Method method, Object value) {
		final String pathName = findPathName(method);
		PathCache primaryPathCache = pathCaches.get(0);

		final String fullPath = primaryPathCache.getBasePath() + "/" + pathName;
		String data = objectMapper.convertValue(value, String.class);

		try {
			client.create()
				.creatingParentsIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath(fullPath);
		} catch (Exception ignore) {
			// TODO: better way to check if path exists
		}

		try {
			client.setData().forPath(fullPath, data.getBytes());
		} catch (Exception e) {
			throw new CuratorException(e);
		}
	}

	private boolean isPrimitive(Class<?> returnType) {
		return	returnType.isPrimitive() ||
				returnType == Boolean.class ||
				returnType == Character.class ||
				returnType == Byte.class ||
				returnType == Short.class ||
				returnType == Integer.class ||
				returnType == Long.class ||
				returnType == Float.class ||
				returnType == Double.class;
	}

	private Object defaultValue(Method method) {
		Class<?> returnType = method.getReturnType();

		Config annotation = method.getAnnotation(Config.class);

		Object result;
		if(annotation != null && annotation.defaultValue() != null && !annotation.defaultValue().isEmpty()) {
			result = annotation.defaultValue();
			result = convertResultToReturnType(result, returnType);
		} else {
			result = defaultValue(returnType);
		}
		return result;
	}

	private Object convertResultToReturnType(Object result, Class<?> returnType) {
		if(isPrimitive(returnType)) {
			return objectMapper.convertValue(result, returnType);
		} else if(returnType != String.class) {
			throw new UnsupportedOperationException("@com.vanillaci.zookeeperconfig.Config currently only works with primitives, boxed primitives, and Strings");
		}

		return result;
	}

	private Object defaultValue(Class<?> returnType) {
		if(returnType.isPrimitive()) {
			if(boolean.class.isAssignableFrom(returnType)) {
				return false;
			} else {
				return 0;
			}
		} else {
			return null;
		}
	}

	private static String findPathName(Method method) {
		String path = method.getName();

		Config annotation = method.getAnnotation(Config.class);
		if(annotation != null) {
			if(annotation.path() != null && !annotation.path().isEmpty()) {
				path = annotation.path();
			} else if(annotation.value() != null && !annotation.value().isEmpty()) {
				path = annotation.value();
			}
		}

		return path;
	}
}