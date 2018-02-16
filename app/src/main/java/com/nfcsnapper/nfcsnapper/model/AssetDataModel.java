package com.nfcsnapper.nfcsnapper.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by macbookpro on 14/02/2018.
 */

public class AssetDataModel implements Serializable{

    private String name;
    private String caseUrl;
    @SerializedName("case")
    private String caseBool;

    public AssetDataModel(){

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaseUrl() {
        return caseUrl;
    }

    public void setCaseUrl(String caseUrl) {
        this.caseUrl = caseUrl;
    }

    public String getCaseBool() {
        return caseBool;
    }

    public void setCaseBool(String caseBool) {
        this.caseBool = caseBool;
    }
}
