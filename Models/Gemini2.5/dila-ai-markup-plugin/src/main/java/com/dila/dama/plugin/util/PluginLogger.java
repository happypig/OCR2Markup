package com.dila.dama.plugin.util;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * Centralized logging utility for DILA AI Markup Assistant Plugin.
 * Provides consistent logging format and debug mode management across all plugin classes.
 * 
 * <p>Debug mode can be enabled via:
 * <ul>
 *   <li>Environment variable: DILA_DEBUG=true</li>
 *   <li>System property: -Ddila.debug=true</li>
 * </ul>
 * 
 * @author DILA Plugin Team
 * @version 1.0
 */
public class PluginLogger {
    
    /**
     * Debug mode flag - initialized once at class loading time
     */
    private static final boolean DEBUG = determineDebugMode();
    
    /**
     * Log level enumeration
     */
    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private PluginLogger() {
        throw new AssertionError("PluginLogger is a utility class and should not be instantiated");
    }
    
    /**
     * Determine debug mode from environment or system properties.
     * This method is called once during class initialization.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    private static boolean determineDebugMode() {
        try {
            String debugEnv = System.getenv("DILA_DEBUG");
            if (debugEnv != null) {
                return "true".equalsIgnoreCase(debugEnv);
            }
        } catch (Exception e) {
            // Continue to system property check
        }
        
        try {
            String debugProp = System.getProperty("dila.debug");
            if (debugProp != null) {
                return "true".equalsIgnoreCase(debugProp);
            }
        } catch (Exception e) {
            // Use default
        }
        
        return false; // Default value
    }
    
    /**
     * Check if debug mode is enabled.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    public static boolean isDebugEnabled() {
        return DEBUG;
    }
    
    /**
     * Log a debug message (only if debug mode is enabled).
     * 
     * @param message the message to log
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    /**
     * Log a debug message with class context (only if debug mode is enabled).
     * 
     * @param clazz the class from which the log is called
     * @param message the message to log
     */
    public static void debug(Class<?> clazz, String message) {
        log(LogLevel.DEBUG, formatWithClass(clazz, message), null);
    }
    
    /**
     * Log an info message.
     * 
     * @param message the message to log
     */
    public static void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    /**
     * Log an info message with class context.
     * 
     * @param clazz the class from which the log is called
     * @param message the message to log
     */
    public static void info(Class<?> clazz, String message) {
        log(LogLevel.INFO, formatWithClass(clazz, message), null);
    }
    
    /**
     * Log a warning message.
     * 
     * @param message the message to log
     */
    public static void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    /**
     * Log a warning message with class context.
     * 
     * @param clazz the class from which the log is called
     * @param message the message to log
     */
    public static void warn(Class<?> clazz, String message) {
        log(LogLevel.WARN, formatWithClass(clazz, message), null);
    }
    
    /**
     * Log an error message.
     * 
     * @param message the message to log
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    /**
     * Log an error message with class context.
     * 
     * @param clazz the class from which the log is called
     * @param message the message to log
     */
    public static void error(Class<?> clazz, String message) {
        log(LogLevel.ERROR, formatWithClass(clazz, message), null);
    }
    
    /**
     * Log an error message with exception.
     * 
     * @param message the message to log
     * @param throwable the exception to log
     */
    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    /**
     * Log an error message with class context and exception.
     * 
     * @param clazz the class from which the log is called
     * @param message the message to log
     * @param throwable the exception to log
     */
    public static void error(Class<?> clazz, String message, Throwable throwable) {
        log(LogLevel.ERROR, formatWithClass(clazz, message), throwable);
    }
    
    /**
     * Core logging method that handles all log levels.
     * 
     * @param level the log level
     * @param message the message to log
     * @param throwable optional exception to log (can be null)
     */
    private static void log(LogLevel level, String message, Throwable throwable) {
        // Skip debug messages if debug mode is not enabled
        if (level == LogLevel.DEBUG && !DEBUG) {
            return;
        }
        
        try {
            PrintStream ps = new PrintStream(System.err, true, "UTF-8");
            String formattedMessage = formatMessage(level, message);
            ps.println(formattedMessage);
            
            if (throwable != null) {
                throwable.printStackTrace(ps);
            }
        } catch (UnsupportedEncodingException e) {
            // Fallback to default encoding
            String formattedMessage = formatMessage(level, message);
            System.err.println(formattedMessage);
            
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }
    
    /**
     * Format a log message with timestamp and level.
     * 
     * @param level the log level
     * @param message the message to format
     * @return the formatted message
     */
    private static String formatMessage(LogLevel level, String message) {
        return String.format("[%s] [%s] %s", new Date(), level, message);
    }
    
    /**
     * Format a message with class context.
     * 
     * @param clazz the class from which the log is called
     * @param message the message to format
     * @return the formatted message with class name
     */
    private static String formatWithClass(Class<?> clazz, String message) {
        return String.format("[%s] %s", clazz.getSimpleName(), message);
    }
}
