package org.graalvm.argo.lambda_manager.core;

/**
 * This enum describes the type of runtime that the lambda is running.
 */
public enum LambdaExecutionMode {
    // Wasm-enabled lambda potentially running native code as well.
    MOSAIC,
    // Lambda running a native application.
    NATIVE;

    public boolean isMosaic() {
        return this == LambdaExecutionMode.MOSAIC;
    }

    public boolean isNative() {
        return this == LambdaExecutionMode.NATIVE;
    }
}
