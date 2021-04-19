package com.zombio_odev.zspectrum.modules.SpectrumVeiw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.zombio_odev.zspectrum.modules.SpectrumVeiw.Modules.SpectrumDrawThread;

public class SpectrumView extends SurfaceView implements SurfaceHolder.Callback {
    ////////
    public static final float DX_TO_SCALE = 5.3F;
    public static final float DX_TO_SWIPE = 5.3F;
    public static final float SWIPE_COEFF = 2.1F;
    private float _first_X = 0.0F; // first pressed x or left part of grid
    private boolean _first_pressed = false; // shows that one finger is presed or not

    private float _dX_length = 0.0F; // 2 floats, used in swipe mode functions
    private float _dX_prev_length = 0.0F;

    private boolean _scaleMode = false; // shows, that 2 fingers are presed

    private float _init_length_P01 = 0.0F; // 3 floats, used in scale mode functions
    private float _actual_length_P01 = 0.0F;
    private float _previous_length_P01 = 0.0F;
    ////////


    private SpectrumDrawThread drawThread;

    public static class SpectrumData {
        public int[] spectrum;
        public boolean isUpdated;
        public SpectrumData() {
            spectrum = new int[4096];
            for (int i = 0; i < 4096; i++)
                spectrum[i] = 0;
            isUpdated = false;
        }
    }
    public static class SpectrumSurfaceData {
        public int channelsToShow;
        public int startChannel;
        public boolean isUpdated;
        public float xPoint;
        public boolean oneFingerPressed;
        public boolean scaleMode;
        public SpectrumSurfaceData() {
            channelsToShow = 4000;
            startChannel = 0;
            isUpdated = false;
            xPoint = 0;
            oneFingerPressed = false;
            scaleMode = false;
        }
        public SpectrumSurfaceData(SpectrumSurfaceData c) {
            channelsToShow = c.channelsToShow;
            startChannel = c.startChannel;
            isUpdated = c.isUpdated;
            xPoint = c.xPoint;
            oneFingerPressed = c.oneFingerPressed;
            scaleMode = c.scaleMode;
        }
    }


    private final SpectrumData spectrumData;
    private final SpectrumSurfaceData surfaceData;

    public SpectrumView(@NonNull Context context)
    {
        super(context);
        spectrumData = new SpectrumData();
        surfaceData = new SpectrumSurfaceData();
        getHolder().addCallback(this);
    }

    private static int normalizeInt_1(int value) {
        return value & 0x000000FF;
    }

    private static int normalizeInt_2(int value) {
        return value & 0x0000FF00;
    }

    private static int normalizeInt_3(int value) {
        return value & 0x00FF0000;
    }

    private static int normalizeInt_4(int value) {
        return value & 0xFF000000;
    }

    public int addReceivedData(@NonNull byte[] bleResponse) {
        if (bleResponse.length < 87) {
            return 0x01;
        }
        int cmd = normalizeInt_2( bleResponse[1] << 8) + normalizeInt_1(bleResponse[2]);
        if (cmd != 0x0008)
            return 0x02;
        int cmdResponse = normalizeInt_1(bleResponse[0]);
        if (cmdResponse != 0)
            return 0x03;

        int startIndex = normalizeInt_2(bleResponse[3] << 8) + normalizeInt_1(bleResponse[4]);
        int len = normalizeInt_2( bleResponse[5] << 8) + normalizeInt_1(bleResponse[6]);

        if (startIndex + len > 4096)
            return 0x04;
            //if (len > 20)
            // return 0x05;

        for (int i = 0; i < len; i++) {
            int value =  normalizeInt_4(bleResponse[7 + i * 4] << 24) + normalizeInt_3(bleResponse[7 + i * 4 + 1] << 16)
                    + normalizeInt_2(bleResponse[7 + i * 4 + 2] << 8) + normalizeInt_1(bleResponse[7 + i * 4 + 3]);
            spectrumData.spectrum[startIndex + i] = value;
        }
        spectrumData.isUpdated = true;
        return 0;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scale_change = false;
        boolean scale_direction = true;
        boolean swipe_change = false;
        boolean swipe_direction = false;
        float _ddX;
        float _ddY;
        int width = this.getWidth();
        int height = this.getHeight();
        float startXpoint = width * SpectrumDrawThread.START_X_GRID_COEFFICIENT;
        float startYpoint = height * SpectrumDrawThread.START_Y_GRID_COEFFICIENT;

        int actionMask = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        if (pointerCount > 2) // Если количество касаний больше двух, то выходим
            return true;

        switch (actionMask) {
            case MotionEvent.ACTION_DOWN: // Первое нажатие
                _first_X = event.getX(0);
                if (_first_X < startXpoint || event.getY(0) < startYpoint)
                    return false;
                _first_pressed = true;
                surfaceData.xPoint = _first_X;
                _dX_length = 0.0F;
                _dX_prev_length = 0.0F;
                _actual_length_P01 = 0.0F;
                _previous_length_P01 = 0.0F;
                _scaleMode = false;
                //_secondPressed = false;
                break;
            //-------------------------------------
            case MotionEvent.ACTION_POINTER_DOWN: // Псследующее нажатие
                _actual_length_P01 = 0.0F;
                _previous_length_P01 = 0.0F;
                _first_pressed = false;
                float _init_ddX = event.getX(1) - event.getX(0);
                float _init_ddY = event.getY(1) - event.getY(0);
                _init_length_P01 = (float) Math.sqrt((_init_ddX*_init_ddX + _init_ddY*_init_ddY));
                _scaleMode = true;
                //_secondPressed = true;
                break;
            //-------------------------------------
            case MotionEvent.ACTION_POINTER_UP: // 2 нажатие отпущено
                //_ddX = 0.0F;
                //_ddY = 0.0F;
                _actual_length_P01 = 0.0F;
                _previous_length_P01 = 0.0F;
                //_secondPressed = false;
                break;
            //-------------------------------------
            case MotionEvent.ACTION_UP: // 1 нажатие отпущено
                _dX_length = 0.0F;
                _dX_prev_length = 0.0F;
                _first_pressed = false;
                break;
            //-------------------------------------
            case MotionEvent.ACTION_MOVE:
                surfaceData.xPoint = Math.max(event.getX(0), startXpoint);
                if (pointerCount == 2) {
                    _dX_length = 0.0F;
                    _dX_prev_length = 0.0F;
                    _ddX = event.getX(1) - event.getX(0);
                    _ddY = event.getY(1) - event.getY(0);
                    if (Math.abs(_previous_length_P01 - _actual_length_P01) > DX_TO_SCALE) {
                        if (_previous_length_P01 < _actual_length_P01)
                            scale_direction = false;
                        _previous_length_P01 = _actual_length_P01;
                        scale_change = true;
                    }
                    _actual_length_P01 = ((float) Math.sqrt((_ddX*_ddX + _ddY*_ddY))) - _init_length_P01;
                }
                else {
                    if (Math.abs(_dX_prev_length - _dX_length) > DX_TO_SWIPE) {
                        if (_dX_prev_length < _dX_length)
                            swipe_direction = true;
                        swipe_change = true;
                        _dX_prev_length = _dX_length;
                    }
                    _dX_length = event.getX(0) - _first_X;
                }
                break;
        }

        synchronized (surfaceData) {
            surfaceData.oneFingerPressed = _first_pressed;
            surfaceData.scaleMode = _scaleMode;

            if (swipe_change) {
                int coeff = (int)Math.sqrt(surfaceData.channelsToShow / SWIPE_COEFF);
                if (!swipe_direction) {
                    if (surfaceData.startChannel + surfaceData.channelsToShow < 4096 - coeff)
                        surfaceData.startChannel += coeff;
                }
                else {
                    if (surfaceData.startChannel > coeff)
                        surfaceData.startChannel -= coeff;
                }
            }

            if (scale_change) {
                int delta = Math.max((int)1, (int)((float)Math.sqrt(surfaceData.channelsToShow) - 7));
                if (scale_direction) {
                    if (surfaceData.channelsToShow < 4096 - delta)
                        surfaceData.channelsToShow += delta;
                }
                else {
                    if (surfaceData.channelsToShow > 64 + delta)
                        surfaceData.channelsToShow -= delta;
                }
            }

            surfaceData.isUpdated = true;
        }

        return true;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        drawThread = new SpectrumDrawThread(getHolder(), spectrumData, surfaceData);
        spectrumData.isUpdated = true;
        drawThread.setRunning(true);
        drawThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        boolean retry = true;
        drawThread.setRunning(false);
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
    }

}
