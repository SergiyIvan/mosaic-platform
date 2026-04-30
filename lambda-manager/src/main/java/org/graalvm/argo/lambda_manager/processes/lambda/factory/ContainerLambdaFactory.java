package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartMosaicContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartNativeContainer;

public class ContainerLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createMosaic(Lambda lambda, Function function) {
        return new StartMosaicContainer(lambda, function);
    }

    @Override
    public StartLambda createNative(Lambda lambda, Function function) {
        return new StartNativeContainer(lambda, function);
    }

}
