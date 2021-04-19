package com.zombio_odev.zspectrum.modules.CpsGraphics.Modules;

public class SurfaceData {
    //**************<CONSTANTS>**************
    public static final float START_X_COEFFICIENT = 0.2F;
    public static final float START_Y_COEFFICIENT = 0.3F;
    //*************</CONSTANTS>**************
    public boolean isUpdated = false;
    public float XPOS = 0.0F;
    public boolean scaleMode = false;
    public boolean oneFingerPressed = false;

    public SurfaceData() {
    }

    public SurfaceData(SurfaceData sr) {
        this.isUpdated = sr.isUpdated;
        this.XPOS = sr.XPOS;
        this.scaleMode = sr.scaleMode;
        this.oneFingerPressed = sr.oneFingerPressed;
    }

}
