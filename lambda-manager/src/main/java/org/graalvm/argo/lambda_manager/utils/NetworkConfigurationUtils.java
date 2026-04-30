package org.graalvm.argo.lambda_manager.utils;

import java.net.MalformedURLException;
import java.util.Queue;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;

import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class NetworkConfigurationUtils {

    private static final String LOCALHOST_IP = "127.0.0.1";

    public static void prepareContainerConnectionPool(Queue<LambdaConnection> pool, int connections) {
        try {
            for (int lambdaPort = Configuration.argumentStorage.getFirstLambdaPort(); lambdaPort < Configuration.argumentStorage.getFirstLambdaPort() + connections; lambdaPort++) {
                pool.add(new LambdaConnection(LOCALHOST_IP, null, lambdaPort));
            }
        } catch (MalformedURLException e) {
            Logger.log(Level.INFO, "Failed to prepare lambda connection", e);
        }
    }

}
