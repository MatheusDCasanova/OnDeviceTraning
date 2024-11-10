package com.example.myapplication;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.FloatBuffer;
import java.util.List;
import com.example.myapplication.TypeConverter;

public class ModelConfig {
    private int epochs=1;
    private int batches=100;
    private int batchSize=64;
    private String dimensions = TypeConverter.listToString(List.of(28,28));
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

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
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
