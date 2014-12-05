package com.vanillaci.zookeeperconfig;

import java.lang.annotation.*;

/**
 * Identifies getters or setters that should be bound to znodes.
 * If a no-arg method with a non-void return value is annotated,
 * 		that method will be bound to the data in znode with the same name as the method (or, if desired, the value of 'value' or 'path').
 * 		The value is updated automatically and cached locally.
 * If a single-arg method with a void return value is annotated,
 * 		that method will be used to set the data in the znode with the path of the primary base path + /methodName (or, if desired, the value of 'value' or 'path').
 *
 * @author Joel Johnson
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Config {
	/**
	 * Shorthand for {@link #path()} when no other parameters are used.
	 */
	String value() default "";

	/**
	 * The path to the znode the value should bind to.
	 * If not specifed, the field name will be used.
	 */
	String path() default "";

	/**
	 * If no znode with the provided name is available, use this value instead.
	 * Will be serialized using default Jackson behavior, so it can represent most basic data types.
	 */
	String defaultValue() default "";
}
