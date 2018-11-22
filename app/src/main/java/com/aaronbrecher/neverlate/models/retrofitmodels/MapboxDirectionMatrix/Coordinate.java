package com.aaronbrecher.neverlate.models.retrofitmodels.MapboxDirectionMatrix;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Coordinate {

    @SerializedName("location")
    @Expose
    private List<Double> location = null;
    @SerializedName("name")
    @Expose
    private String name;

    public List<Double> getLocation() {
        return location;
    }

    public void setLocation(List<Double> location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}