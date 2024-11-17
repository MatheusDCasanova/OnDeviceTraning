package com.example.ondevicetraining;

public class EnergyMetric {
    public int epochs;
    public int batches;
    public int batchSize;
    public String modelLink;
    public String labelsLink;
    public String featuresLink;
    public long timeSpent;
    public int modelSize;
    public double energySpent;
    public double sampleEnergy;
    public double sampleTime;

    public EnergyMetric(){}
    public EnergyMetric(double energySpent, double sampleEnergy, double sampleTime, int epochs, int batches, int batchSize,
                        String modelLink, String labelsLink, String featuresLink, long timeSpent, int modelSize) {
        this.epochs = epochs;
        this.batches = batches;
        this.batchSize = batchSize;
        this.modelLink = modelLink;
        this.labelsLink = labelsLink;
        this.featuresLink = featuresLink;
        this.timeSpent = timeSpent;
        this.modelSize = modelSize;
        this.energySpent = energySpent;
        this.sampleEnergy = sampleEnergy;
        this.sampleTime = sampleTime;
    }
}


