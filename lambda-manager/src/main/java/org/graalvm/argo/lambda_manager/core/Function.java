package org.graalvm.argo.lambda_manager.core;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Function {

    /** Name of the function. The name of a function is a unique identifier. */
    private final String name;

    /** Memory required to run a function invocation (in MBs). */
    private final long memory;

    /** The runtime where this function should be executed. Accepted values include:
     * - mosaic;
     * - native.
     * */
    private final String runtime;

    /** URL of the function code to be downloaded by the runtime. */
    private final String functionCode;

    /** Optional metadata used by Mosaic to hold dependency specification.
     * Is expected to be short (e.g., JSON) and is not designed for holding big chunks of data.
     * */
    private final byte[] functionMetadata;

    public Function(String name, String memory, String runtime, String functionCode, byte[] functionMetadata) throws Exception {
        this.name = name;
        this.memory = Long.parseLong(memory);
        this.runtime = runtime;
        this.functionCode = functionCode;
        this.functionMetadata = functionMetadata;
    }

    public String getName() {
        return name;
    }

    public long getMemory() {
        return memory;
    }

    public Path buildFunctionSourceCodePath() {
        return Paths.get(Environment.CODEBASE, name, name);
    }

    public String getRuntime() {
        return this.runtime;
    }

    public LambdaExecutionMode getLambdaExecutionMode() {
        if (getRuntime().equals(Environment.MOSAIC_RUNTIME)) {
            return LambdaExecutionMode.MOSAIC;
        } else if (getRuntime().equals(Environment.NATIVE_RUNTIME)) {
            return LambdaExecutionMode.NATIVE;
        } else {
            throw new IllegalStateException("Unexpected runtime: " + getRuntime());
        }
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public byte[] getFunctionMetadata() {
        return functionMetadata;
    }
}
