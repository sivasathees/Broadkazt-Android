
package com.nfcsnapper.nfcsnapper.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {

    private String logo;
    private String name;
    private AssetDataModel asset;
    private ArrayList<Video> videos = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Video> getVideos() {
        return videos;
    }

    public void setVideos(ArrayList<Video> videos) {
        this.videos = videos;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setAsset(AssetDataModel obj){
        this.asset = obj;
    }

    public AssetDataModel getAsset(){
        return this.asset;
    }


}
