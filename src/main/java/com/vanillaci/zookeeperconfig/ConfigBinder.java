package com.vanillaci.zookeeperconfig;

import com.fasterxml.jackson.databind.*;
import com.google.common.collect.*;
import com.vanillaci.exceptions.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.utils.*;
import org.apache.log4j.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
public final class ConfigBinder<T> implements Closeable {
	private static final Logger logger = Logger.getLogger(ConfigBinder.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final PathChildrenCache pathCache;
	private final T objectToPopulate;
	private final Map<String, Field> watchedFields;

	private ConfigBinder(PathChildrenCache pathCache, T objectToPopulate, Map<String, Field> watchedFields) {
		this.pathCache = pathCache;
		this.objectToPopulate = objectToPopulate;
		this.watchedFields = ImmutableMap.copyOf(watchedFields);
	}

	public static <T> ConfigBinder<T> bind(CuratorFramework client, T objectToPopulate, String basePath) {
		return bind(client, objectToPopulate, basePath, objectMapper);
	}

	public static <T> ConfigBinder<T> bind(CuratorFramework client, T objectToPopulate, String basePath, ObjectMapper objectMapper) {
		if (client == null) {
			throw new IllegalArgumentException("null client");
		}
		if (objectToPopulate == null) {
			throw new IllegalArgumentException("null objectToPopulate");
		}
		if (basePath == null || basePath.isEmpty()) {
			throw new IllegalArgumentException("null or empty basePath");
		}

		basePath = cleanBasePath(basePath);

		Map<String, Field> watchedFields = findWatchedFields(objectToPopulate);

		PathChildrenCache pathCache = new PathChildrenCache(client, basePath, true);

		ConfigBinder configBinder = new ConfigBinder(pathCache, objectToPopulate, watchedFields);

		PathChildrenCacheListener listener = (curatorFramework, event) -> {
			Field field = null;
			try {
				ChildData data = event.getData();
				String nodeName = ZKPaths.getNodeFromPath(data.getPath());

				field = watchedFields.get(nodeName);
				if (field != null) {
					switch (event.getType()) {
						case CHILD_ADDED:
						case CHILD_UPDATED:
							byte[] newData = data.getData();
							Object result = new String(newData);
							Class<?> fieldType = field.getType();

							if (fieldType != String.class) {
								result = objectMapper.convertValue(result, fieldType);
							}

							field.set(objectToPopulate, result);
							break;
						case CHILD_REMOVED:
							field.set(objectToPopulate, null);
							break;
					}
				}
			} catch (Exception e) {
				logger.error("Unable to set field value in event: " + field, e);
			}
		};

		pathCache.getListenable().addListener(listener);

		try {
			pathCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
		} catch (Exception e) {
			throw new CuratorException(e);
		}

		populateWithDefaults(objectToPopulate, pathCache, watchedFields, basePath);

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

	private static <T> void populateWithDefaults(T objectToPopulate, PathChildrenCache pathCache, Map<String, Field> watchedFields, String basePath) {
		for (Map.Entry<String, Field> entry : watchedFields.entrySet()) {
			ChildData currentData = pathCache.getCurrentData(basePath + "/" + entry.getKey());

			Object result = null;
			if(currentData == null) {
				String defaultValue = entry.getValue().getAnnotation(Config.class).defaultValue();
				if(defaultValue != null && !defaultValue.isEmpty()) {
					result = defaultValue;
				}
			} else {
				byte[] data = currentData.getData();
				result = new String(data);
			}

			if(result != null) {
				try {
					Class<?> fieldType = entry.getValue().getType();
					if(fieldType != String.class) {
						result = objectMapper.convertValue(result, fieldType);
					}

					entry.getValue().set(objectToPopulate, result);
				} catch (IllegalAccessException e) {
					logger.error("unable to populate default values " + entry.getValue(), e);
				}
			}
		}
	}

	private static <T> Map<String, Field> findWatchedFields(T objectToPopulate) {
		Map<String, Field> result = new HashMap<>();

		Class<?> currentClass = objectToPopulate.getClass();
		while (currentClass != Object.class) {
			Field[] declaredFields = currentClass.getDeclaredFields();
			for (Field declaredField : declaredFields) {
				declaredField.setAccessible(true);

				Config annotation = declaredField.getAnnotation(Config.class);
				if (annotation != null) {
					String path = findPathName(declaredField, annotation);
					result.put(path, declaredField);
				}
			}

			currentClass = currentClass.getSuperclass();
		}
		return result;
	}

	private static String findPathName(Field declaredField, Config annotation) {
		String path = annotation.path();
		if (path == null || path.isEmpty()) {
			path = annotation.value();

			if (path == null || path.isEmpty()) {
				path = declaredField.getName();
			}
		}
		return path;
	}

	@Override
	public void close() throws IOException {
		pathCache.close();
	}

	public T getObjectToPopulate() {
		return objectToPopulate;
	}
}
