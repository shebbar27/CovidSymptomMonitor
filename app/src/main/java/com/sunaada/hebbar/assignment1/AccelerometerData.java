package com.sunaada.hebbar.assignment1;

import android.hardware.SensorEvent;

import java.util.ArrayList;

public class AccelerometerData {
    private final ArrayList<Float> xAxisValues;
    private final ArrayList<Float >yAxisValues;
    private final ArrayList<Float> zAxisValues;

    public AccelerometerData() {
        this.xAxisValues = new ArrayList<>();
        this.yAxisValues = new ArrayList<>();
        this.zAxisValues = new ArrayList<>();
    }

    public void appendAxesValues(SensorEvent SensorEvent) {
        if(SensorEvent.values.length == 3) {
            this.xAxisValues.add(SensorEvent.values[0]);
            this.yAxisValues.add(SensorEvent.values[1]);
            this.zAxisValues.add(SensorEvent.values[2]);
        }
    }

    public float[] getWeightedAverageAxisValues(int x, int y, int z)
    {
        float[] weightedAverageAxisValues = new float[this.xAxisValues.size()];
        if(this.isDataValid()) {
            for(int i=0; i<this.xAxisValues.size(); i++) {
                weightedAverageAxisValues[i] = (this.xAxisValues.get(i) * x
                                + this.yAxisValues.get(i) * y
                                + this.zAxisValues.get(i) * z)/(x+y+z);
            }
        }

        return weightedAverageAxisValues;
    }

    public boolean isDataValid() {
        return this.xAxisValues.size() == this.yAxisValues.size() &&
                this.yAxisValues.size() == this.zAxisValues.size();
    }
}
