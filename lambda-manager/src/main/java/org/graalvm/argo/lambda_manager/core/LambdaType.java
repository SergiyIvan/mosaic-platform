package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.utils.Messages;

/**
 * This enum describes the type of the lambda virtualization technology.
 */
public enum LambdaType {
    // Lambda to be deployed as container.
    CONTAINER("CONTAINER"),
    // Lambda to be deployed as container.
    CONTAINER_DEBUG("CONTAINER_DEBUG");

    private final String type;

    LambdaType(String type) {
        this.type = type;
    }

    public boolean isContainer() {
        return this.equals(CONTAINER) || this.equals(CONTAINER_DEBUG);
    }

    public static LambdaType fromString(String text) throws RuntimeException {
        for (LambdaType b : LambdaType.values()) {
            if (b.type.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new RuntimeException(String.format(Messages.ERROR_LAMBDA_TYPE, text));
    }

    @Override
    public String toString() {
        return this.type;
    }
}
