package com.zombio_odev.zspectrum.modules.CpsGraphics;

import android.support.annotation.Nullable;

/**
 * This class is used when displaying graphics
 * It contains bool info about what to show/do:
 * dynamic resolution, auto scroll, locked,
 * smoothing, start from zero, show CPS graphic.
 * It also contains info about: dynamic res.
 * threshold value, warning level value, critical
 * level value, uRoentgen coefficient
 */
public class SwitchSetting {

    public static final int DYNAMIC_THR_VAL_CMD = 0xFF01;
    public static final int WARNING_LVL_VAL_CMD = 0xFF02;
    public static final int CRITICAL_LVL_VAL_CMD = 0xFF03;



    //********CONSTANTS*********
    public static final int PARTICLES_PER_dY = 0;
    public static final int ZIVERT_dY = 1;
    //**************************

    /**
     * Switch callback is used in situations like
     * that: you want to set auto scroll switch and
     * at the same time lock switch is already set.
     * Then setLockToFalse callback initiated to inform
     * user, that he should set lock switch to false
     */
    public interface SwitchCallback {
        void setAutoscrollToFalse();
        void setLockToFalse();
    }

    private boolean DynamicResolution = true;
    private int DynamicResolutionThresholdValue = 1000;
    private int WarningLevelValue = 1200;
    private int CriticalLevelValue = 1500;
    private boolean AutoScroll = true;
    private boolean Locked = false;
    private boolean Smoothing = false;
    private int ResolutionType = PARTICLES_PER_dY;
    private float uRentgenCoefficient = 1.0F;
    private boolean isUpdated = false;
    private boolean startFromZero = false;
    private boolean showCPSgraphic = false;
    private SwitchCallback callback = null;

    public SwitchSetting(@Nullable SwitchCallback CallbackTo) {
        callback = CallbackTo;
    }

    public SwitchSetting(SwitchSetting rs) {
        DynamicResolution = rs.DynamicResolution;
        DynamicResolutionThresholdValue = rs.DynamicResolutionThresholdValue;
        AutoScroll = rs.AutoScroll;
        Locked = rs.Locked;
        Smoothing = rs.Smoothing;
        ResolutionType = rs.ResolutionType;
        uRentgenCoefficient = rs.uRentgenCoefficient;
        isUpdated = rs.isUpdated;
        callback = rs.callback;
        startFromZero = rs.startFromZero;
        showCPSgraphic = rs.showCPSgraphic;
        WarningLevelValue = rs.WarningLevelValue;
        CriticalLevelValue = rs.CriticalLevelValue;
    }

    public int getWarningLevelValue() {
        return WarningLevelValue;
    }

    public void setWarningLevelValue(int warningLevelValue) {
        WarningLevelValue = warningLevelValue;
    }

    public int getCriticalLevelValue() {
        return CriticalLevelValue;
    }

    public void setCriticalLevelValue(int criticalLevelValue) {
        CriticalLevelValue = criticalLevelValue;
    }

    public void setShowCPSgraphic(boolean showCPSgraphic) {
        this.showCPSgraphic =showCPSgraphic;
    }
    public boolean getShowCPSgraphic() {
        return this.showCPSgraphic;
    }
    public void setStartFromZero(boolean startFromZero) {
        this.startFromZero = startFromZero;
    }
    public boolean getStartFromZero() {
        return startFromZero;
    }
    public void setDynamicResolution(boolean dynamicResolution) {
        this.DynamicResolution = dynamicResolution;
    }
    public boolean getDynamicResolution() {
        return this.DynamicResolution;
    }
    public void setDynamicResolutionThresholdValue(int ThresholdVal) {
        DynamicResolutionThresholdValue = ThresholdVal;
    }
    public int getDynamicResolutionThresholdValuee() {
        return DynamicResolutionThresholdValue;
    }
    public synchronized void setAutoScroll(boolean autoScroll) {
        AutoScroll = autoScroll;
        if (Locked && AutoScroll) {
            Locked = false;
            if (callback != null)
                callback.setLockToFalse();
        }
    }
    public boolean getAutoScroll() {
        return AutoScroll;
    }
    public synchronized void setLocked(boolean locked) {
        Locked = locked;
        if (Locked && AutoScroll) {
            AutoScroll = false;
            if (callback != null)
                callback.setAutoscrollToFalse();
        }
    }
    public boolean getLocked() {
        return Locked;
    }
    public void setSmoothing(boolean smoothing) {
        Smoothing = smoothing;
    }
    public boolean getSmoothing() {
        return Smoothing;
    }
    public void setResolutionType(int Type_PER_dY) {
        ResolutionType = Type_PER_dY;
    }
    public int getResolutionType() {
        return ResolutionType;
    }
    public void setuRentgenCoefficient(float CPS_TO_uRentgen_COEFF) {
        uRentgenCoefficient = CPS_TO_uRentgen_COEFF;
    }
    public void setUpdated(boolean updated) {
        isUpdated = updated;
    }
    public boolean getUpdated() {
        return isUpdated;
    }



}
