package org.graalvm.argo.lambda_manager;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.metrics.MetricsProvider;

@SuppressWarnings("unused")
@ExecuteOn(TaskExecutors.IO)
@Controller()
public class LambdaManagerController {

    @Post(value = "/{username}/{function_name}", consumes = MediaType.APPLICATION_JSON)
    public String processRequest(@PathVariable("username") String username,
                                         @PathVariable("function_name") String functionName,
                                         @Nullable @Body String arguments,
                                         @Nullable @QueryValue("count") String warmupCount) {
        // Note: by default, warmupCount is null. Only used for demos using the web interface.
        if (warmupCount != null) {
            for (int i = 0; i < Integer.parseInt(warmupCount); i++) {
                LambdaManager.processRequest(username, functionName, arguments);
            }
        }
        try {
            MetricsProvider.addConcurrentRequest();
            return LambdaManager.processRequest(username, functionName, arguments);
        } finally {
            MetricsProvider.removeConcurrentRequest();
        }
    }

    @Get("/get_functions")
    public String getFunctions() {
        return LambdaManager.getFunctions();
    }

    @Post(value = "/upload_function", consumes = MediaType.TEXT_PLAIN)
    public String uploadFunction(@QueryValue("username") String username,
                                         @QueryValue("function_name") String functionName,
                                         @QueryValue("function_memory") String functionMemory,
                                         @QueryValue("function_code_url") String functionCodeUrl,
                                         @Nullable @QueryValue("function_runtime") String functionRuntime,
                                         @Nullable @Body byte[] functionMetadata) {
        return LambdaManager.uploadFunction(username, functionName, functionMemory,
                functionRuntime, functionCodeUrl, functionMetadata);
    }

    @Post("/remove_function")
    public String removeFunction(@QueryValue("username") String username,
                                         @QueryValue("function_name") String functionName) {
        return LambdaManager.removeFunction(username, functionName);
    }

    @Get(value = "/metrics", produces = MediaType.TEXT_PLAIN)
    public String scrapeMetrics() {
        return MetricsProvider.getMetricsRecord();
    }

}
