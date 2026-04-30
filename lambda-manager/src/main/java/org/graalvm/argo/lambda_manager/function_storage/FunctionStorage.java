package org.graalvm.argo.lambda_manager.function_storage;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.exceptions.user.FunctionNotFound;

import java.util.Map;

public interface FunctionStorage {
    void register(String functionName, Function function, byte[] code) throws Exception;

    void unregister(String functionName);

    Function get(String functionName) throws FunctionNotFound;

    Map<String, Function> getAll();
}
