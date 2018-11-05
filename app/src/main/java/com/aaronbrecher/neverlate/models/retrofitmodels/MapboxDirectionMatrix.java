package com.aaronbrecher.neverlate.models.retrofitmodels;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MapboxDirectionMatrix {

    @SerializedName("code")
    @Expose
    private String code;
    @SerializedName("destinations")
    @Expose
    private List<Coordinate> destinations = null;
    @SerializedName("distances")
    @Expose
    private List<List<Double>> distances = null;
    @SerializedName("durations")
    @Expose
    private List<List<Double>> durations = null;
    @SerializedName("sources")
    @Expose
    private List<Coordinate> sources = null;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Coordinate> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Coordinate> destinations) {
        this.destinations = destinations;
    }

    public List<List<Double>> getDistances() {
        return distances;
    }

    public void setDistances(List<List<Double>> distances) {
        this.distances = distances;
    }

    public List<List<Double>> getDurations() {
        return durations;
    }

    public void setDurations(List<List<Double>> durations) {
        this.durations = durations;
    }

    public List<Coordinate> getSources() {
        return sources;
    }

    public void setSources(List<Coordinate> sources) {
        this.sources = sources;
    }

}