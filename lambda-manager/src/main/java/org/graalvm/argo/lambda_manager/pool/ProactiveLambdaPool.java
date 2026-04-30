package org.graalvm.argo.lambda_manager.pool;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.core.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.pool.utils.LambdaPoolUtils;
import org.graalvm.argo.lambda_manager.utils.NetworkConfigurationUtils;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

public class ProactiveLambdaPool extends LambdaPool {

    // TODO - add documentation.
    private final LambdaManagerPool poolConfiguration;

    public ProactiveLambdaPool(LambdaType lambdaType, int maxTaps, LambdaManagerPool poolConfiguration) {
        super(lambdaType, maxTaps);
        this.poolConfiguration = poolConfiguration;
    }

    @Override
    public void setUp() {
        this.lambdaPool.putAll(Map.ofEntries(
            Map.entry(LambdaExecutionMode.MOSAIC.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.NATIVE.name(), new ConcurrentLinkedQueue<>())));

        NetworkConfigurationUtils.prepareContainerConnectionPool(connectionPool, maxLambdas);

        LambdaPoolUtils.prepareLambdaPool(lambdaPool, poolConfiguration);
        LambdaPoolUtils.startLambdaReclaimingDaemon(lambdaPool, poolConfiguration);
    }

    @Override
    public Lambda getLambda(LambdaExecutionMode mode, Function function) {
        return lambdaPool.get(mode.name()).poll();
    }

    /**
     * Dispose the used lambda and replenish the pool with a new lambda.
     */
    @Override
    public void disposeLambda(Lambda lambda) {
        if (lambda.isIntact() && Environment.notShutdownHookActive()) {
            // The lambda was not used, we can add it to the pool right away.
            lambdaPool.get(lambda.getExecutionMode().name()).add(lambda);
        } else {
            boolean success = LambdaPoolUtils.shutdownLambda(lambda, lambdaType);
            connectionPool.add(lambda.getConnection());
            // To avoid deadlock when new lambdas are forcefully terminated and created again.
            // Only replenish if managed to terminate the previous lambda successfully.
            if (success && Environment.notShutdownHookActive()) {
                Lambda newLambda = new Lambda(lambda.getExecutionMode());
                LambdaPoolUtils.startLambda(lambdaPool.get(lambda.getExecutionMode().name()), newLambda, lambda.getExecutionMode(), null, null);
            }
        }
    }

    @Override
    public void tearDown() {
        // Shutdown lambdas inside pool and starting lambdas.
        lambdaPool.values().forEach(LambdaPoolUtils::shutdownLambdas);

        // Close any lasting connection.
        for (LambdaConnection connection : connectionPool) {
            connection.client.close();
        }

        // Clearing the connection pool.
        connectionPool.clear();
    }
}
