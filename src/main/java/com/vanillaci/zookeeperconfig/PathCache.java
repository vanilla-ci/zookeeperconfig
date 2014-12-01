package com.vanillaci.zookeeperconfig;

import org.apache.curator.framework.recipes.cache.*;

/**
 * Created by joeljohnson on 12/1/14.
 */
public class PathCache {
	private final String basePath;
	private final PathChildrenCache cache;

	public PathCache(String basePath, PathChildrenCache cache) {
		this.basePath = basePath;
		this.cache = cache;
	}

	public String getBasePath() {
		return basePath;
	}

	public PathChildrenCache getCache() {
		return cache;
	}
}
