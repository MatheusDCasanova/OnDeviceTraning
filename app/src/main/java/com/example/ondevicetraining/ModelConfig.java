package com.example.ondevicetraining;

import java.util.List;

public class ModelConfig {
    private int epochs=1;
    private int batches=100;
    private int batchSize=64;
    private String dimensions = TypeConverter.listToString(List.of(28,28));
    private String modelLink="https://link_to_model/model.tflite";
    private String labelsLink="https://link_to_model/labels.bin";
    private String featuresLink="https://link_to_features/features.bin";
    private int time;
    private int modelSize;

    public ModelConfig() {
        // Default constructor
    }

    private double energy;

    private double sampleEnergy;

    private double sampleTime;

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

    public void setFeaturesLink(String featuresLink) {
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

    public double getSampleEnergy() {
        return sampleEnergy;
    }

    public void setSampleEnergy(double sampleEnergy) {
        this.sampleEnergy = sampleEnergy;
    }

    public double getSampleTime() {
        return sampleTime;
    }

    public void setSampleTime(double sampleTime) {
        this.sampleTime = sampleTime;
    }

    public int getModelSize() {return this.modelSize;}

    public void setModelSize(int modelSize) {this.modelSize = modelSize;}

}
