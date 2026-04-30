package org.graalvm.argo.lambda_manager.socketserver;

import org.graalvm.argo.lambda_manager.core.LambdaManager;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestUtils {

    /**
     * This pattern splits strings on spaces except if between quotes ('...');
     * Source: https://stackoverflow.com/a/7804472
     */
    private static final String TOKEN_PATTERN = "([^']\\S*|'[\\s\\S]+?')\\s*";
    private static final String REQUEST_TYPE_UPLOAD = "upload";
    private static final String REQUEST_TYPE_INVOCATION = "invocation";

    /**
     * This method acts as a controller in LambdaManagerController.
     */
    public static String processOperation(Map<String, String> parameters) {
        String result;
        if (REQUEST_TYPE_UPLOAD.equals(parameters.get("type"))) {
            String username = parameters.get("username");
            String functionName = parameters.get("function_name");
            String functionMemory = parameters.get("function_memory");
            String functionRuntime = parameters.get("function_runtime");
            String functionCodeUrl = parameters.get("function_code_url");
            byte[] functionMetadata = parameters.get("payload").getBytes();

            result = LambdaManager.uploadFunction(username, functionName, functionMemory,
                    functionRuntime, functionCodeUrl, functionMetadata);
        } else if (REQUEST_TYPE_INVOCATION.equals(parameters.get("type"))) {
            String username = parameters.get("username");
            String functionName = parameters.get("function_name");
            String arguments = parameters.get("payload");

            result = LambdaManager.processRequest(username, functionName, arguments);
        } else {
            throw new IllegalArgumentException("Unknown request type. Parameters:\n" + parameters);
        }
        return result;
    }

    static Map<String, String> parsePayload(String payload) {
        Map<String, String> parameters = new HashMap<>();
        // Check request type.
        if (payload.startsWith("u ")) {
            parameters.put("type", REQUEST_TYPE_UPLOAD);
        } else if (payload.startsWith("i ")) {
            parameters.put("type", REQUEST_TYPE_INVOCATION);
        } else {
            throw new IllegalArgumentException("Unknown request type. Payload:\n" + payload);
        }
        // Parse the rest of the payload.
        Matcher m = Pattern.compile(TOKEN_PATTERN).matcher(payload.substring(2));
        boolean parsedPayload = false;
        while (m.find()) {
            String token = m.group(1).replace("'", "");
            if (token.contains("=")) {
                String[] keyValue = token.split("=");
                parameters.put(keyValue[0], keyValue[1]);
            } else {
                if (parsedPayload) {
                    throw new IllegalArgumentException("There should be at most one payload token in the message surrounded by quotes ('...').");
                }
                parameters.put("payload", token);
                parsedPayload = true;
            }
        }
        return parameters;
    }
}
