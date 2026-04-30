package org.graalvm.argo.lambda_manager.pool.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.*;
import org.graalvm.argo.lambda_manager.core.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

// TODO (rbruno): this class should be merged with LambdaPool.
public class LambdaPoolUtils {

    private static final Set<Lambda> startingLambdas = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static final int EXECUTOR_THREAD_COUNT = Runtime.getRuntime().availableProcessors() / 2;

    public static void prepareLambdaPool(Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        ExecutorService executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);

        startLambdasPerMode(lambdaPool, LambdaExecutionMode.MOSAIC, poolConfiguration.getMosaic(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.NATIVE, poolConfiguration.getNative(), executor);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private static void startLambdasPerMode(Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaExecutionMode mode, int amount, ExecutorService executor) {
        for (int i = 0; i < amount; ++i) {
            executor.execute(() -> {
                Lambda lambda = new Lambda(mode);
                // This is a blocking call that waits until the lambda is created.
                startLambda(lambdaPool.get(mode.name()), lambda, mode, null, null);
            });
        }
    }

    public static void startLambda(Queue<Lambda> lambdaPool, Lambda lambda, LambdaExecutionMode targetMode, Function function, AtomicBoolean lambdaCreationLock) {
        try {
            startingLambdas.add(lambda);
            long timeBefore = System.currentTimeMillis();
            LambdaConnection connection = Configuration.argumentStorage.getLambdaPool().nextLambdaConnection();
            lambda.setConnection(connection);
            Logger.log(Level.INFO, "Starting new " + targetMode + " lambda.");
            ProcessBuilder process = whomToSpawn(lambda, targetMode, function).build();
            process.start();
            if (connection.waitUntilReady()) {
                lambdaPool.add(lambda);
                Logger.log(Level.INFO, "Added new lambda with mode " + targetMode + ". It took " + (System.currentTimeMillis() - timeBefore) + " ms.");
            } else {
                new DefaultLambdaShutdownHandler(lambda, "failed to add").run();
                Logger.log(Level.SEVERE, "Failed to add new lambda with mode " + targetMode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            startingLambdas.remove(lambda);
            if (lambdaCreationLock != null) {
                lambdaCreationLock.set(false);
            }
        }
    }

    private static StartLambda whomToSpawn(Lambda lambda, LambdaExecutionMode targetMode, Function function) {
        switch (targetMode) {
            case MOSAIC:
                return Configuration.argumentStorage.getLambdaFactory().createMosaic(lambda, function);
            case NATIVE:
                return Configuration.argumentStorage.getLambdaFactory().createNative(lambda, function);
            default:
                throw new IllegalStateException("Unexpected value: " + targetMode);
        }
    }

    public static void shutdownLambdas(Queue<Lambda> lambdaPool) {
        ExecutorService executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);
        // Shutdown lambdas being currently started.
        for (Lambda lambda : startingLambdas) {
            executor.execute(new DefaultLambdaShutdownHandler(lambda, "pool tear down (starting)"));
        }
        startingLambdas.clear();
        // Shutdown lambdas from the pool.
        for (Lambda lambda : lambdaPool) {
            executor.execute(new DefaultLambdaShutdownHandler(lambda, "pool tear down (from the pool)"));
        }
        lambdaPool.clear();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static boolean shutdownLambda(Lambda lambda, LambdaType lambdaType) {
        boolean success;
        try {
            if (lambdaType == LambdaType.CONTAINER || lambdaType == LambdaType.CONTAINER_DEBUG) {
                success = shutdownContainerLambda(lambda);
            } else {
                Logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", lambda.getLambdaID(), lambda.getExecutionMode()));
                success = false;
            }
        } catch (Throwable t) {
            Logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", lambda.getLambdaID(), t.getMessage()));
            t.printStackTrace();
            success = false;
        }

        return success;
    }

    private static void printStream(Level level, InputStream stream) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = is.readLine()) != null) {
            Logger.log(level, line);
        }
    }

    private static boolean shutdownContainerLambda(Lambda lambda) throws Throwable {
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_container.sh", lambda.getLambdaName()).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
            return false;
        }
        return true;
    }

    public static void startLambdaReclaimingDaemon(Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        Runnable task = new LambdaReclaimingDaemon(poolConfiguration, lambdaPool);
        Thread daemon = new Thread(task);
        daemon.start();
    }

    private static class LambdaReclaimingDaemon implements Runnable {

        private final Map<String, Integer> maxLambdas;
        private final Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool;

        private LambdaReclaimingDaemon(LambdaManagerPool poolConfiguration, Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool) {
            this.maxLambdas = new HashMap<>();
            this.maxLambdas.put(LambdaExecutionMode.MOSAIC.name(), poolConfiguration.getMosaic());
            this.maxLambdas.put(LambdaExecutionMode.NATIVE.name(), poolConfiguration.getNative());
            this.lambdaPool = lambdaPool;
        }

        @Override
        public void run() {
            while (Environment.notShutdownHookActive()) {
                try {
                    lambdaPool.forEach(this::reclaim);
                    Thread.sleep(Configuration.argumentStorage.getReclamationInterval());
                } catch (InterruptedException ie) {
                    // Ignore.
                } catch (Throwable th) {
                    Logger.log(Level.WARNING, String.format("A problem occurred during the lambda reclaiming process: %s", th.getMessage()));
                }
            }
        }

        private void reclaim(String mode, ConcurrentLinkedQueue<Lambda> lambdas) {
            int total = maxLambdas.get(mode);
            int minimalThreshold = (int) (total * Configuration.argumentStorage.getReclamationThreshold());
            int lambdasInPool = lambdas.size();
            if (lambdasInPool < minimalThreshold) {
                // Use Math.ceil to always reclaim at least one lambda.
                int lambdasToReclaim = (int) Math.ceil(total * Configuration.argumentStorage.getReclamationPercentage());
                long ts = System.currentTimeMillis();
                LambdaManager.lambdas.stream().filter(l -> l.getExecutionMode().name().equals(mode) && l.getOpenRequestCount() <= 0 && ts - l.getLastUsedTimestamp() > Configuration.argumentStorage.getLruReclamationPeriod()).sorted(LambdaPoolUtils::compare)
                        .limit(lambdasToReclaim).parallel().forEach(l -> new DefaultLambdaShutdownHandler(l, "reclaiming").run());
            }
        }
    }

    public static int compare(Lambda l1, Lambda l2) {
        return (int) (l1.getLastUsedTimestamp() - l2.getLastUsedTimestamp());
    }
}
