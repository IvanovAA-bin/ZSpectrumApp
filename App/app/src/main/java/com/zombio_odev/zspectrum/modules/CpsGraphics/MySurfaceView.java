package com.zombio_odev.zspectrum.modules.CpsGraphics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.zombio_odev.zspectrum.modules.CpsGraphics.Modules.DrawThread;
import com.zombio_odev.zspectrum.modules.CpsGraphics.Modules.RenderData;
import com.zombio_odev.zspectrum.modules.CpsGraphics.Modules.SurfaceData;

@SuppressLint("ViewConstructor")
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    // https://habr.com/ru/post/126316/
    ////////
    public static final float DX_TO_SCALE = 5.3F;
    public static final float DX_TO_SWIPE = 5.3F;
    public static final float SWIPE_COEFF = 10.1F;
    private float _first_X = 0.0F;
    private float _dX_length = 0.0F;
    private float _dX_prev_length = 0.0F;
    private float _init_length_P01 = 0.0F;
    private float _actual_length_P01 = 0.0F;
    private float _previous_length_P01 = 0.0F;
    private boolean _scaleMode = false;
    private boolean _first_pressed = false;
    ////////

    private final SwitchSetting switchSettings;
    private final RenderData renderData;
    private final SurfaceData surfaceData;
    private DrawThread drawThread;


    public MySurfaceView(@NonNull Context context, @NonNull SwitchSetting switchSettings, int TimeBtwCnv) {
        super(context);
        this.switchSettings = switchSettings;
        this.renderData = new RenderData();
        renderData.TimeBetweenMeasurements = TimeBtwCnv;
        surfaceData = new SurfaceData();
        getHolder().addCallback(this);
    }

    public void addData(Integer data) {
        synchronized (renderData) {
            renderData.DataArray.add(data);
            renderData.FilteredDataArray.add(MySurfaceView.SimpleKalmanFilter.simpleKalman(data));
            int arraysize = renderData.DataArray.size();
            if (arraysize > RenderData.MAX_ARRAY_SIZE) {
                renderData.DataArray.remove(0);
                renderData.FilteredDataArray.remove(0);
                --arraysize;
            }

            if ((switchSettings.getLocked() || !switchSettings.getAutoScroll()) && (renderData.DataToShow + renderData.StartIndex) < arraysize) {
                ++renderData.StartIndex;
                //return; // ????
            }
            if (switchSettings.getAutoScroll())
                renderData.StartIndex = 0;
            renderData.DataAvailable = true;
        }

    }
    public boolean setTimeBetweenMeasurements(int millis) {
        if (1000 % millis != 0)
            return true;
        renderData.FilteredDataArray.clear();
        renderData.DataArray.clear();
        renderData.TimeBetweenMeasurements = millis;
        return false;
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
        float startXpoint = width * SurfaceData.START_X_COEFFICIENT;
        float startYpoint = height * SurfaceData.START_Y_COEFFICIENT;

        int actionMask = event.getActionMasked();
        //int pointerIndex = event.getActionIndex();
        int pointerCount = event.getPointerCount();

        if (pointerCount > 2) // Если количество касаний больше двух, то выходим
            return true;

        switch (actionMask) {
            case MotionEvent.ACTION_DOWN: // Первое нажатие
                _first_X = event.getX(0);
                if (_first_X < startXpoint || event.getY(0) < startYpoint)
                    return false;
                _first_pressed = true;
                surfaceData.XPOS = _first_X;
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
                surfaceData.XPOS = Math.max(event.getX(0), startXpoint);
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

        boolean sw_locked = switchSettings.getLocked();
        boolean sw_autoscroll = switchSettings.getAutoScroll();

        synchronized (renderData) {
            surfaceData.oneFingerPressed = _first_pressed;
            surfaceData.scaleMode = _scaleMode;
            if (sw_locked) {
                surfaceData.isUpdated = true;
                return true;
            }

            if (sw_autoscroll) {
                renderData.StartIndex = 0;
                if (scale_change) {
                    if (scale_direction) {
                        if (renderData.DataToShow < RenderData.MAX_DATA_TO_SHOW) {
                            ++renderData.DataToShow;
                            //surfaceData.isUpdated = true;
                        }
                    }
                    else {
                        if (renderData.DataToShow > RenderData.MIN_DATA_TO_SHOW){
                            --renderData.DataToShow;
                            //surfaceData.isUpdated = true;
                        }
                    }
                }
                surfaceData.isUpdated = true;
                return true;
            }

            if (scale_change) {
                if (scale_direction) {
                    if (renderData.DataToShow < RenderData.MAX_DATA_TO_SHOW) {
                        if (renderData.StartIndex > 0 && renderData.DataToShow + renderData.StartIndex > renderData.DataArray.size()) {
                            --renderData.StartIndex;
                        }
                        ++renderData.DataToShow;
                    }
                }
                else {
                    if (renderData.DataToShow > RenderData.MIN_DATA_TO_SHOW){
                        --renderData.DataToShow;
                    }
                }
            }


            if (swipe_change) {
                int delta = (int)Math.sqrt(renderData.DataToShow / SWIPE_COEFF);
                if (swipe_direction) {
                    if (renderData.StartIndex + renderData.DataToShow < renderData.DataArray.size() - delta)
                        renderData.StartIndex += delta;
                }
                else {
                    if (renderData.StartIndex > delta)
                        renderData.StartIndex -= delta;
                }
            }
            surfaceData.isUpdated = true;
            return true;
        }




        //Log.d("MEOW", "DataToShow: " + Integer.toString(surfaceData.DataToShow) + " StartIndex: " + Integer.toString(surfaceData.StartIndex));
        //Log.d("MEOW", "Length 1: " + Float.toString(_length_P0) + " Length 2: " + Float.toString(_actual_length_P01));
        //Log.d("MEOW", "");



        //surfaceData.XPOS = touchXpos_0;
        //surfaceData.isUpdated = true;

        //return true;
    }




    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        //Log.d("MEOW", "Height = " + Integer.toString(this.getHeight()) + "Width = " + Integer.toString(this.getWidth()));
        drawThread = new DrawThread(getHolder(), switchSettings, renderData, surfaceData);
        surfaceData.isUpdated = true;
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

    public static class SimpleKalmanFilter { //https://alexgyver.ru/lessons/filters/
        private final static float _err_measure = 0.8F;  // примерный шум измерений
        private final static float _q = 0.01F;   // скорость изменения значений 0.001-1, варьировать самому

        static float _err_estimate = _err_measure;
        static float _last_estimate = 0.0F;

        static float simpleKalman(float newVal) {
            float _kalman_gain, _current_estimate;
            _kalman_gain = _err_estimate / (_err_estimate + _err_measure);
            _current_estimate = _last_estimate + _kalman_gain * (newVal - _last_estimate);
            _err_estimate =  (1.0F - _kalman_gain) * _err_estimate + Math.abs(_last_estimate - _current_estimate) * _q;
            _last_estimate = _current_estimate;
            return _current_estimate;
        }
    }
}

