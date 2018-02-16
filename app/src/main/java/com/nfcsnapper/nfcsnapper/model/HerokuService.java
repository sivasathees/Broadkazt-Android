
package com.nfcsnapper.nfcsnapper.model;

import java.util.HashMap;
import java.util.Map;

public class HerokuService {

    private boolean success;
    private Data data;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
