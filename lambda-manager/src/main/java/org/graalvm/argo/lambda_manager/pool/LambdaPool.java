package org.graalvm.argo.lambda_manager.pool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.core.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public abstract class LambdaPool {

    /**
     * Type of lambdas that will be created.
     */
    protected final LambdaType lambdaType;

    /**
     * Maximum number of network taps that will be setup by the Lambda Manager.
     */
    protected final int maxLambdas;

    /**
     * Concurrent queue where the connections are pooled from.
     */
    protected final ConcurrentLinkedQueue<LambdaConnection> connectionPool;

    /**
     * Lambda pool used for amortization of lambda creation.
     */
    protected final Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool;

    public LambdaPool(LambdaType lambdaType, int maxLambdas) {
        this.lambdaType = lambdaType;
        this.maxLambdas = maxLambdas;
        this.connectionPool = new ConcurrentLinkedQueue<>();
        this.lambdaPool = new ConcurrentHashMap<>();
    }

    public abstract void setUp();

    public LambdaConnection nextLambdaConnection() {
        return connectionPool.poll();
    }

    public abstract Lambda getLambda(LambdaExecutionMode mode, Function function);

    public abstract void disposeLambda(Lambda lambda);

    public abstract void tearDown() throws InterruptedException;
}
