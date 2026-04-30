package org.graalvm.argo.lambda_manager.utils.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "mosaic",
                "native"
})
public class LambdaManagerPool implements Serializable {
    @JsonProperty("mosaic") private int mosaicLambdas;
    @JsonProperty("native") private int nativeLambdas;
    private final static long serialVersionUID = -620888442421704577L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaManagerPool() {
    }

    /**
     * @param mosaic - Maximum number of Mosaic lambdas in the pool.
     * @param nativeLambdas - Maximum number of Native lambdas in the pool.
     */
    public LambdaManagerPool(int mosaic, int nativeLambdas) {
        super();
        this.mosaicLambdas = mosaic;
        this.nativeLambdas = nativeLambdas;
    }

    @JsonProperty("mosaic")
    public int getMosaic() {
        return mosaicLambdas;
    }

    @JsonProperty("mosaic")
    public void setMosaic(int mosaicLambdas) {
        this.mosaicLambdas = mosaicLambdas;
    }

    @JsonProperty("native")
    public int getNative() {
        return nativeLambdas;
    }

    @JsonProperty("native")
    public void setNative(int nativeLambdas) {
        this.nativeLambdas = nativeLambdas;
    }
}
