package com.example.myapplication;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.List;

public class ModelConfig {
    private int epochs;
    private int batches;
    private int batchSize;
    private List<Integer> dimensions;
    private String modelLink;
    private String labelsLink;
    private String featuresLink;
    private int time;

    public ModelConfig() {
        // Default constructor
    }

    private double energy;

    // Getters and Setters


    public int getEpochs() {
        return epochs;
    }

    public void setEpochs(int epochs) {
        this.epochs = epochs;
    }

    public int getBatches() {
        return batches;
    }

    public void setBatches(int batches) {
        this.batches = batches;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<Integer> dimensions) {
        this.dimensions = dimensions;
    }

    public String getModelLink() {
        return modelLink;
    }

    public void setModelLink(String modelLink) {
        this.modelLink = modelLink;
    }

    public String getFeaturesLink() {
        return featuresLink;
    }

    public void setFeaturesLink(String datasetLink) {
        this.featuresLink = featuresLink;
    }

    public String getLabelsLink() {
        return labelsLink;
    }

    public void setLabelsLink(String labelsLink) {
        this.labelsLink = labelsLink;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

}
