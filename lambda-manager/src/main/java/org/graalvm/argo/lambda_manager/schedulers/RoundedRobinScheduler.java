package org.graalvm.argo.lambda_manager.schedulers;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.core.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import java.util.logging.Level;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    private Lambda findLambda(Function function, LambdaExecutionMode targetMode) {
        for (Lambda lambda : LambdaManager.lambdas) {

            boolean modeMatch = targetMode == lambda.getExecutionMode();
            // If lambda is decomissioned, not of the correct target, or cannot register this function, skip.
            if (lambda.isDecommissioned() || !modeMatch || !lambda.canRegisterInLambda(function)) {
                continue;
            }

            if (lambda.tryRegisterInLambda(function)) {
                return lambda;
            }
        }
        return null;
    }

    @Override
    public Lambda schedule(Function function, LambdaExecutionMode targetMode) {
        Lambda lambda = null;
        String username = Configuration.coder.decodeUsername(function.getName());
        boolean obtainedLambda = false;
        boolean coldStart = false;

        while (Environment.notShutdownHookActive()) {

            lambda = findLambda(function, targetMode);

            // If we didn't find a lambda, try to get a new one from the pool.
            if (lambda == null) {
                Lambda newLambda = Configuration.argumentStorage.getLambdaPool().getLambda(targetMode, function);
                if (newLambda != null) {
                    Logger.log(Level.INFO, "Obtained a new lambda from the pool.");
                    newLambda.updateLastUsed();
                    LambdaManager.lambdas.add(newLambda);
                    obtainedLambda = true;
                } else {
                    Logger.log(Level.FINE, String.format("[function=%s, mode=%s]: The lambda pool is currently empty, waiting.", function.getName(), targetMode));
                    coldStart = true;
                }
            } else {
                // We found a lambda, exit the loop.
                break;
            }

            if (!obtainedLambda) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // Ignored.
                }
            }
            obtainedLambda = false;
        }

        Logger.log(Level.INFO, String.format("Found lambda %d%s.", lambda.getLambdaID(), coldStart ? " (cold start)" : ""));
        return lambda;
    }

    @Override
    public void reschedule(Lambda lambda, Function function) {
        // Decrement the open request count.
        lambda.decOpenRequests();
    }
}
