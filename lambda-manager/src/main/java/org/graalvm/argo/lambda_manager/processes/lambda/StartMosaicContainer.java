package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartMosaicContainer extends StartContainer {

    private final Function function;

    public StartMosaicContainer(Lambda lambda, Function function) {
        super(lambda);
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("mosaic-proxy:latest");
        // Convert memory to bytes for the "docker run --memory ..." option.
        if (function == null) {
            // Created from proactive pool, use default container size.
            command.add(String.valueOf(Configuration.argumentStorage.getMaxMemory() * 1024L * 1024L));
            command.add(String.valueOf(Configuration.argumentStorage.getCpuQuota()));
        } else {
            // Created for specific function, use function size.
            long functionMemory = function.getMemory();
            long functionCpuQuota = ((functionMemory * 1000 / 1024) / 2) * 100; // MiB to MB; divide by two due to ratio; multiply by 100 to get cgroups quota.
            command.add(String.valueOf(functionMemory * 1024L * 1024L));
            command.add(String.valueOf(functionCpuQuota));
        }
        return command;
    }
}
