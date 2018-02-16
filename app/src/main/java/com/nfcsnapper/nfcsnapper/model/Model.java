package com.nfcsnapper.nfcsnapper.model;

/**
 * Created by eivarsso on 07.05.2017.
 */

public class Model {
    private int icon;
    private String title;
    private String awsUrl;
    private String counter;

    private boolean isGroupHeader = false;

    public Model(String title) {
        this(-1,title, "",null);
        isGroupHeader = true;
    }
    public Model(int icon, String title, String awsUrl, String counter) {
        super();
        this.icon = icon;
        this.title = title;
        this.awsUrl = awsUrl;
        this.counter = counter;
    }

    public boolean isGroupHeader() {
        return isGroupHeader;
    }

    public int getIcon() {
        return icon;
    }

    public String getTitle(){
        return title;
    }

    public String getCounter(){
        return counter;
    }

    public String getAWSUrl() { return awsUrl; }
}
