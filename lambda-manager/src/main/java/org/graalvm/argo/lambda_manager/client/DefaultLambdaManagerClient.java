package org.graalvm.argo.lambda_manager.client;

import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class DefaultLambdaManagerClient implements LambdaManagerClient {

    private static final long TIMEOUT_SECONDS = 60;

    private String sendRequest(String path, byte[] payload, Lambda lambda) {
        return sendRequest(path, payload, lambda, TIMEOUT_SECONDS);
    }

    private String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        return lambda.getConnection().sendRequest(path, payload, lambda, timeout);
    }

    @Override
    public String registerFunction(Lambda lambda, Function function) {
            String path = null;
            byte[] payload = null;
            // Intentionally triggering 404 as a way to ensure that the webserver is up.
            sendRequest("/ping", payload, lambda, 1);

            if (lambda.getExecutionMode().isMosaic()) {
                path = String.format("/init?url=%s", function.getFunctionCode());
                payload = function.getFunctionMetadata();
            } else if (lambda.getExecutionMode().isNative()) {
                path = String.format("/init?url=%s", function.getFunctionCode());
            } else {
                Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
            }
            return sendRequest(path, payload, lambda);
    }

    @Override
    public String deregisterFunction(Lambda lambda, Function function) {
        if (lambda.getExecutionMode().isMosaic() || lambda.getExecutionMode().isNative()) {
            Logger.log(Level.WARNING, String.format("Deregistering functions (%s) is not yet supported for the existing runtimes!", function.getName()));
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
        }

        return String.format(Messages.ERROR_FUNCTION_DELETE,  function.getName());
    }

    @Override
    public String invokeFunction(Lambda lambda, Function function, String arguments) {
        // Only use the warmup endpoint if intend to do sandbox snapshotting.
        String path = null;
        byte[] payload = null;

        if (lambda.getExecutionMode().isMosaic() || lambda.getExecutionMode().isNative()) {
            path = "/run";
            payload = arguments.getBytes();
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when invoking function %s!", lambda.getExecutionMode(), function.getName()));
        }
        return sendRequest(path, payload, lambda);
    }
}
