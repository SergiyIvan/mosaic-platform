package org.graalvm.argo.lambda_manager.core;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class Environment {

    private Environment() { }

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    private static volatile boolean shutdownHookActive = false;

    // Project Directories.
    public static final String CODEBASE = "codebase";
    public static final String MANAGER_LOGS = "manager_logs";
    public static final String LAMBDA_LOGS = "lambda_logs";
    public static final String MANAGER_METRICS = "manager_metrics";

    // Filenames.
    public static final String DEFAULT_FILENAME = "default_filename.log";
    public static final String OUTPUT = "output.log";
    public static final String ERROR = "error.log";
    public static final String MEMORY = "memory.log";
    public static final String MANAGER_LOG_FILENAME = Paths.get(MANAGER_LOGS, "lambda_manager.log").toString();

    public static final String MANAGER_METRICS_FILENAME = Paths.get(MANAGER_METRICS, "metrics.log").toString();

    // Runtime identifiers.
    public static final String NATIVE_RUNTIME = "native";
    public static final String MOSAIC_RUNTIME = "mosaic";

    public static long pid() {
        return NEXT_ID.incrementAndGet();
    }

    public static boolean notShutdownHookActive() {
        return !shutdownHookActive;
    }

    public static void setShutdownHookActive(boolean shutdownHookActive) {
        Environment.shutdownHookActive = shutdownHookActive;
    }
}
