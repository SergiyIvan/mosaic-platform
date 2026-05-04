package org.graalvm.argo.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "gateway",
                "maxMemory",
                "maxTaps",
                "timeout",
                "healthCheck",
                "lambdaPort",
                "lambdaType",
                "lambdaConsole",
                "managerConsole",
                "lambdaPool"
})
public class LambdaManagerConfiguration implements Serializable {
    @JsonProperty("gateway") private String gateway;
    @JsonProperty("maxMemory") private int maxMemory;
    @JsonProperty("maxTaps") private int maxTaps;
    @JsonProperty("timeout") private int timeout;
    @JsonProperty("healthCheck") private int healthCheck;
    @JsonProperty("lambdaPort") private int lambdaPort;
    @JsonProperty("lambdaType") private String lambdaType;
    @JsonProperty("lambdaConsole") private boolean lambdaConsole;
    @JsonProperty("managerConsole") private LambdaManagerConsole lambdaManagerConsole;
    @JsonProperty("lambdaPool") private LambdaManagerPool lambdaPool;
    private final static long serialVersionUID = -6081673374812554207L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaManagerConfiguration() {
    }

    /**
     *
     * @param gateway - The default PC's gateway address.
     * @param maxMemory - Maximum memory that can be used by lambdas in total (MBs).
     * @param maxTaps - Maximum number of taps available for lambdas.
     * @param timeout - Time during which lambda can stay inactive (in seconds).
     * @param healthCheck - Lambda's health will be checked in this time-span, after the first
     *            health response, no more checks are made.
     * @param lambdaPort - In which port the lambda will receive its requests.
     * @param lambdaType - Type of the workers. Accepted values: [container,vm_firecracker].
     * @param lambdaConsole - Is console active during qemu's run.
     * @param lambdaManagerConsole - The class with information about manager logging.
     * @param lambdaPool - The class that describes the pool of lambdas to be used.
     */
    public LambdaManagerConfiguration(String gateway, int maxMemory, int maxTaps, int timeout, int healthCheck, int lambdaPort, String lambdaType,
                    boolean lambdaConsole, LambdaManagerConsole lambdaManagerConsole, LambdaManagerPool lambdaPool) {
        super();
        this.gateway = gateway;
        this.maxMemory = maxMemory;
        this.maxTaps = maxTaps;
        this.timeout = timeout;
        this.healthCheck = healthCheck;
        this.lambdaPort = lambdaPort;
        this.lambdaType = lambdaType;
        this.lambdaConsole = lambdaConsole;
        this.lambdaManagerConsole = lambdaManagerConsole;
        this.lambdaPool = lambdaPool;
    }

    @JsonProperty("gateway")
    public String getGateway() {
        return gateway;
    }

    @JsonProperty("gateway")
    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @JsonProperty("maxMemory")
    public int getMaxMemory() {
        return maxMemory;
    }

    @JsonProperty("maxMemory")
    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    @JsonProperty("maxTaps")
    public int getMaxTaps() {
        return maxTaps;
    }

    @JsonProperty("maxTaps")
    public void setMaxTaps(int maxTaps) {
        this.maxTaps = maxTaps;
    }

    @JsonProperty("timeout")
    public int getTimeout() {
        return timeout;
    }

    @JsonProperty("timeout")
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @JsonProperty("healthCheck")
    public int getHealthCheck() {
        return healthCheck;
    }

    @JsonProperty("healthCheck")
    public void setHealthCheck(int healthCheck) {
        this.healthCheck = healthCheck;
    }

    @JsonProperty("lambdaPort")
    public int getLambdaPort() {
        return lambdaPort;
    }

    @JsonProperty("lambdaPort")
    public void setLambdaPort(int lambdaPort) {
        this.lambdaPort = lambdaPort;
    }

    @JsonProperty("lambdaType")
    public String getLambdaType() {
        return lambdaType;
    }

    @JsonProperty("lambdaType")
    public void setLambdaType(String lambdaType) {
        this.lambdaType = lambdaType;
    }

    @JsonProperty("lambdaConsole")
    public boolean isLambdaConsole() {
        return lambdaConsole;
    }

    @JsonProperty("lambdaConsole")
    public void setLambdaConsole(boolean lambdaConsole) {
        this.lambdaConsole = lambdaConsole;
    }

    @JsonProperty("managerConsole")
    public LambdaManagerConsole getManagerConsole() {
        return lambdaManagerConsole;
    }

    @JsonProperty("managerConsole")
    public void setManagerConsole(LambdaManagerConsole lambdaManagerConsole) {
        this.lambdaManagerConsole = lambdaManagerConsole;
    }

    @JsonProperty("lambdaPool")
    public LambdaManagerPool getLambdaPool() {
        return lambdaPool;
    }

    @JsonProperty("lambdaPool")
    public void setLambdaPool(LambdaManagerPool lambdaPool) {
        this.lambdaPool = lambdaPool;
    }
}
