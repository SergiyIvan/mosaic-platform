package org.graalvm.argo.lambda_manager.utils;

import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtils {

    public static String constructJsonResponseObject(Object response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode resultObject = mapper.createObjectNode();
            if (response instanceof String) {
                resultObject.put("data", (String) response);
            } else if (response instanceof JsonNode) {
                resultObject.set("data", (JsonNode) response);
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultObject);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Messages.INTERNAL_ERROR;
        }
    }
}
