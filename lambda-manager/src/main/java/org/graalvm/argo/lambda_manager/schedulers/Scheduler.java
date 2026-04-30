package org.graalvm.argo.lambda_manager.schedulers;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaExecutionMode;

/**
 * The scheduler decides where (in which Lambda) to execution a function invocation.
 */
public interface Scheduler {

	/**
	 * Returns the Lambda that should host the function invocation and that respects the given target mode.
	 * @param function
	 * @param targetMode
	 * @return
	 */
    Lambda schedule(Function function, LambdaExecutionMode targetMode);

    /**
     * Reschedules the Lambda after executing the lambda invocation.
     * @param lambda
     */
    void reschedule(Lambda lambda, Function function);
}
