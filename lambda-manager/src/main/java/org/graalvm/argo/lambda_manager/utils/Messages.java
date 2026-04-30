package org.graalvm.argo.lambda_manager.utils;

public class Messages {

    // General messages.
    public static final String INTERNAL_ERROR = "Internal server error!";
    public static final String HTTP_TIMEOUT = "HTTP request timeout!";
    public static final String LOG_REDIRECTION = "Log is redirected to file -> %s";
    public static final String UNDELIVERABLE_EXCEPTION = "Undeliverable exception received!";

    // Lambda Manager configuration messages.
    public static final String NO_CONFIGURATION_UPLOADED = "No configuration has been uploaded!";
    public static final String CONFIGURATION_ALREADY_UPLOADED = "Configuration has been already uploaded!";
    public static final String SUCCESS_CONFIGURATION_UPLOAD = "Successfully uploaded lambda manager configuration!";
    public static final String ERROR_CONFIGURATION_UPLOAD = "Error during uploading new configuration!";
    public static final String ERROR_LAMBDA_TYPE = "Lambda type not found - %s!";

    // Upload function messages.
    public static final String SUCCESS_FUNCTION_UPLOAD = "Successfully uploaded function - %s!";
    public static final String ERROR_FUNCTION_LANG = "Function language not found - %s!";
    public static final String ERROR_FUNCTION_UPLOAD = "Error during uploading new function - %s!";
    public static final String ERROR_FUNCTION_DELETE = "Error during removing function - %s!";

    // Remove function messages.
    public static final String SUCCESS_FUNCTION_REMOVE = "Successfully removed function - %s!";

    // Request messages.
    public static final String FUNCTION_NOT_FOUND = "Function - %s - has not been uploaded!";
    public static final String TIME_REQUEST = "Time (user=%s, function_name=%s, mode=%s, id=%d) req: %d; infr: %d\t[us]";

    // Tap messages.
    public static final String ERROR_TAP_REMOVAL = "Error during cleaning taps!";

    // Process messages.
    public static final String PROCESS_START_FINE = "PID -> %d | Command -> %s | Output -> %s";
    public static final String PROCESS_START_INFO = "PID -> %d | Type -> %s | Output -> %s";
    public static final String PROCESS_EXIT_FINE = "PID -> %d | Command -> %s | Exit code -> %d";
    public static final String PROCESS_EXIT_INFO = "PID -> %d | Type -> %s | Exit code -> %d";
    public static final String PROCESS_RAISE_EXCEPTION_FINE = "PID -> %d | Command -> %s | Raised exception!";
    public static final String PROCESS_RAISE_EXCEPTION_INFO = "PID -> %d | Type -> %s | Raised exception!";
    public static final String PROCESS_SHUTDOWN_EXCEPTION_FINE = "PID -> %d | | Command -> %s | " + "Raise exception in shutdown callback!";
    public static final String PROCESS_SHUTDOWN_EXCEPTION_INFO = "PID -> %d | | Type -> %s | " + "Raise exception in shutdown callback!";

    // Parse/Serialize JSON.
    public static final String ERROR_PARSING_JSON = "Error during parsing JSON configuration file!";
    public static final String ERROR_SERIALIZE_JSON = "Error during serialization of JSON object!";
    public static final String SUCCESS_SERIALIZE_JSON = "Serialized object is: %n%s";

}
