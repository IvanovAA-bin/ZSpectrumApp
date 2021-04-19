package com.zombio_odev.zspectrum.modules.CpsGraphics.Modules;

import java.util.LinkedList;

public class RenderData {
    public static final int MAX_ARRAY_SIZE = 2000;
    public static final int MAX_DATA_TO_SHOW = 101;
    public static final int MIN_DATA_TO_SHOW = 11;

    public LinkedList<Integer> DataArray = new LinkedList<>();
    public LinkedList<Float> FilteredDataArray = new LinkedList<>();
    //public Integer LastDataReceived = 0;
    public boolean DataAvailable = false;
    //public boolean DataCopied = false;
    public int StartIndex = 0;
    public int DataToShow = MIN_DATA_TO_SHOW;
    public int TimeBetweenMeasurements = 1000;
    public RenderData() {
    }
}
