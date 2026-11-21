package com.aero.control.helpers.PerApp.AppMonitor;

/**
 * Custom exception class for App Monitor module errors. Provides predefined
 * error messages for common configuration and validation failures.
 */
public class ExceptionHandler extends RuntimeException {
    /** Error message when attempting to override an already initialized app name. */
    public static String EX_APP_NAME_OVERRIDE = "AppName already initialized, overriding is forbidden!";

    /** Error message when no valid module identifier is found. */
    public static String EX_NO_IDENTIFIER_FOUND = "There was no valid identifier found for this module!";

    /** Error message when attempting to redefine an existing module identifier. */
    public static String EX_IDENTIFIER_ALREADY_DEFINED = "The module identifier was already defined, overriding is forbidden!";

    /** Error message when attempting to add a module that doesn't exist. */
    public static String EX_MODULE_NOT_FOUND = "An attempt was made to add a non-exisiting module!";

    /**
     * Constructs a new exception with no detail message.
     */
    public ExceptionHandler() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ExceptionHandler(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ExceptionHandler(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ExceptionHandler(Throwable cause) {
        super(cause);
    }
}
