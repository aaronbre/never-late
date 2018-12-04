package com.aaronbrecher.neverlate.models.retrofitmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DirectionsDuration {
    @SerializedName("duration")
    @Expose
    private Double duration;

    @SerializedName("distance")
    @Expose
    private Double distance;

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
