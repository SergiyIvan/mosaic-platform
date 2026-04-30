package org.graalvm.argo.lambda_manager.function_storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graalvm.argo.lambda_manager.core.Function;

public class LocalFunctionStorage extends SimpleFunctionStorage {

    /**
     * This method expects the byte array parameter to contain
     * path to the code on a local file system.
     */
    @Override
    public void register(String functionName, Function function, byte[] codePathEncoded) throws Exception {
        String codePath = new String(codePathEncoded).trim();
        if (!codePath.startsWith("http") && !codePath.startsWith("knative-")) {
            Path src = Paths.get(codePath);
            Path dst = function.buildFunctionSourceCodePath();
            if (dst.getParent().toFile().mkdirs()) {
                Files.copy(src, dst);
            }
        }
        functions.put(function.getName(), function);
    }

}
