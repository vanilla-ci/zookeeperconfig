package com.vanillaci.zookeeperconfig;

import java.lang.annotation.*;

/**
 * Created by joeljohnson on 11/30/14.
 */
@Target({ElementType.FIELD})
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
