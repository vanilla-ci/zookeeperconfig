package com.vanillaci.exceptions;

/**
 * Thrown when an unhandled exception is thrown by the Curator framework
 *
 * @author Joel Johnson
 */
public class CuratorException extends RuntimeException {
	public CuratorException(Throwable cause) {
		super(cause);
	}
}
