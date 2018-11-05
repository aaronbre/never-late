package com.aaronbrecher.neverlate.models.retrofitmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Version {

    @SerializedName("version")
    @Expose
    private int version;

    @SerializedName("message")
    @Expose
    private String message;

    @SerializedName("needsUpdate")
    @Expose
    private Boolean needsUpdate;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getNeedsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate(Boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }
}
