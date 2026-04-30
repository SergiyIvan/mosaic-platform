package org.graalvm.argo.lambda_manager.function_storage;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.exceptions.user.FunctionNotFound;
import org.graalvm.argo.lambda_manager.utils.FileUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class SimpleFunctionStorage implements FunctionStorage {

    protected final ConcurrentHashMap<String, Function> functions = new ConcurrentHashMap<>();

    @Override
    public void register(String functionName, Function function, byte[] functionCode) throws Exception {
        functions.put(function.getName(), function);
    }

    @Override
    public void unregister(String functionName) {
        functions.remove(functionName);
        FileUtils.purgeDirectory(new File(Paths.get(Environment.CODEBASE, functionName).toString()));
    }

    @Override
    public Function get(String functionName) throws FunctionNotFound {
        Function function = functions.get(functionName);
        if (function == null) {
            throw new FunctionNotFound(String.format(Messages.FUNCTION_NOT_FOUND, functionName));
        }
        return function;
    }

    @Override
    public Map<String, Function> getAll() {
        return functions;
    }
}
