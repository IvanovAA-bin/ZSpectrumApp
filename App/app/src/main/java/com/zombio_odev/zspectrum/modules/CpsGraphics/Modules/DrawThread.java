package com.zombio_odev.zspectrum.modules.CpsGraphics.Modules;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;

import com.zombio_odev.zspectrum.modules.CpsGraphics.SwitchSetting;

import java.util.ArrayDeque;
import java.util.ListIterator;

public class DrawThread extends Thread {
    private static final int BACKGROUND_COLOR = Color.BLACK;
    private static final float COEFF_MUL_TO_MAX_Y_VAL = 1.3F;
    private static final float COEFF_MUL_TO_MAX_Y_OVM = 1.1F;
    private static final float DATA_LINE_DY = 5.0F;
    private static final float DATA_LINE_DX = 10.0F;
    private static final int COLOR_FILTERED_GR = Color.YELLOW;
    private static final int COLOR_NONFILTERED_GR = Color.MAGENTA;
    private static final int COLOR_FCPS_FILTERED_GR = Color.YELLOW;
    private static final int COLOR_FCPS_NONFILTERED_GR = Color.MAGENTA;
    private static final int COLOR_CHOSEN_CLR = Color.GREEN;
    private static final int COLOR_MAX_MARKER_COLOR = Color.CYAN;
    private static final int GRAPHIC_STROKE_WIDTH = 4;
    private static final int CHOSEN_STROKE_WIDTH = 5;
    private static final int GRID_COLOR = Color.GRAY;
    private static final int GRID_ALPHA = 127;
    private static final int GRID_STROKE_WIDTH = 3;
    private static final int BLOCK1_INIT_COLOR = Color.WHITE;
    private static final int BLOCK2_INIT_COLOR = Color.WHITE;

    private static final String TEXT_MAX_VALUE = "MaxVal";
    private static final String TEXT_MAX_VAL_TIME = "Time";
    private static final String TEXT_MAX_VAL_dT = "dT";
    private static final String TEXT_FILTERED_MAX_VAL = "FMaxVAl";
    private static final String TEXT_FILTERED_MAX_VAL_TIME = "FTime";
    private static final String TEXT_FILTERED_MAX_VAL_dT = "FdT";
    private static final String TEXT_CURRENT_VALUE = "CurVal";
    private static final String TEXT_FILTERED_CURRENT_VALUE = "FCurVal";
    private static final String TEXT_CHOSEN_VALUE = "CVal";
    private static final String TEXT_CHOSEN_TIME = "CTime";
    private static final String TEXT_FILTERED_CHOSEN_VALUE = "FCV";
    private static final String TEXT_FILTERED_CHOSEN_V_TIME = "FCVT";
    private static final String TEXT_FCPS_VALUE = "FCPSV";
    private static final String TEXT_FCPS_MAX_VALUE = "FCPSMV";
    private static final String TEXT_FCPS_FILTERED_VALUE = "FFCPV";
    private static final String TEXT_FCPS_FILTERED_MAX_VALUE = "FFCPMV";

    public static final int WARNING_LEVEL_COLOR = Color.YELLOW;
    public static final int CRITICAL_LEVEL_COLOR = Color.RED;
    public static final String NONE_STRING = "none";


    private boolean runFlag = false;
    private final SurfaceHolder surfaceHolder;
    private final SwitchSetting switchSettings;
    private final SurfaceData surfaceData;
    private final RenderData renderData;


    public DrawThread(@NonNull SurfaceHolder surfaceHolder, @NonNull SwitchSetting switchSettings, @NonNull RenderData renderData, @NonNull SurfaceData surfaceData) {
        this.surfaceHolder = surfaceHolder;
        this.renderData = renderData;
        this.switchSettings = switchSettings;
        this.surfaceData = surfaceData;
        super.setName("DrawThread");
    }

    public void setRunning(boolean running) {
        this.runFlag = running;
    }

    @Override
    public void run() {
        Canvas canvas;
        /* ***/
        int fontSize1 = 10;
        boolean fontSet1 = false;
        Paint fontPainter1 = new Paint();
        fontPainter1.setTextSize(fontSize1);
        fontPainter1.setColor(BLOCK1_INIT_COLOR);
        /* ***/
        int fontSize2 = 10;
        boolean fontSet2 = false;
        Paint fontPainter2 = new Paint();
        fontPainter2.setColor(BLOCK2_INIT_COLOR);
        fontPainter2.setTextSize(fontSize2);
        /* ***/

        Paint graphicPainter = new Paint();
        graphicPainter.setStrokeWidth(GRAPHIC_STROKE_WIDTH);

        Paint maxMarkerPainter = new Paint();
        maxMarkerPainter.setStrokeWidth(GRAPHIC_STROKE_WIDTH);
        maxMarkerPainter.setColor(COLOR_MAX_MARKER_COLOR);

        Paint gridPainter = new Paint();
        gridPainter.setColor(GRID_COLOR);
        gridPainter.setAlpha(GRID_ALPHA);
        gridPainter.setStrokeWidth(GRID_STROKE_WIDTH);

        ArrayDeque<Float> DataDeque = new ArrayDeque<>();
        ArrayDeque<Float> DataFilteredDeque = new ArrayDeque<>();

        ArrayDeque<Float> _FCPS_DataDeque = new ArrayDeque<>();
        ArrayDeque<Float> _FCPS_Filtered_DataDeue = new ArrayDeque<>();


        while(runFlag) { // Пока потоку нужно работать
            if (renderData.DataAvailable || switchSettings.getUpdated() || surfaceData.isUpdated) { // Если есть обновления по данным
                canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas(); // Пытаемся получить canvas
                    if (canvas != null) { // Если он получен
                        synchronized (surfaceHolder) {
                            /* ************************<Блок отрисовки>****************************/
                            /* *************************<Обнуление флагов>*************************/
                            renderData.DataAvailable = false;
                            switchSettings.setUpdated(false);
                            surfaceData.isUpdated = false;
                            /* *************************</Обнуление флагов>************************/
                            /* ***********************<Копирование данных>************************/
                            SurfaceData localSurfaceData;
                            synchronized (surfaceData) {
                                localSurfaceData = new SurfaceData(surfaceData);
                            }
                            SwitchSetting localSwitchSettings;
                            synchronized (switchSettings) {
                                localSwitchSettings = new SwitchSetting(switchSettings);
                            }
                            /* ***********************</Копирование данных>************************/
                            /* *************************<Очистка deque>****************************/
                            DataDeque.clear();
                            DataFilteredDeque.clear();
                            _FCPS_DataDeque.clear();
                            _FCPS_Filtered_DataDeue.clear();
                            /* *************************</Очистка deque>***************************/
                            /* *************************<Объявление переменных>********************/
                            int StartIndex, DataToShow, TBM;
                            float MaxValue = 0.0F;
                            int MaxValueIndex = 0;
                            float FilteredMaxValue = 0.0F;
                            int FilteredMaxValueIndex = 0;
                            float _FCPS_MaxValue = 0.0F;
                            int _FCPS_MaxValueIndex = 0;
                            float _FCPS_FilteredMaxValue = 0.0F;
                            int _FCPS_FilteredMaxValueIndex = 0;
                            float CurrentValue = 0.0F;
                            float CurrentFilteredValue = 0.0F;
                            float _FCPS_CurrentValue = 0.0F;
                            float _FCPS_CurrentFilteredValue = 0.0F;
                            float ChosenValue = -1.0F;
                            int ChosenValueIndex;
                            float ChosenFilteredValue = -1.0F;


                            float MinYOnGraphic = Float.MAX_VALUE;
                            int DataCollectToFixedCPS;
                            int Width = canvas.getWidth();
                            int Height = canvas.getHeight();

                            PointF gridStartPoint = new PointF(Width * SurfaceData.START_X_COEFFICIENT, Height * SurfaceData.START_Y_COEFFICIENT);
                            PointF gridStopPoint = new PointF(Width, Height);
                            PointF gridSize = new PointF(gridStopPoint.x - gridStartPoint.x, gridStopPoint.y - gridStartPoint.y);
                            /* ************************</Объявление переменных>********************/
                            /* ************************<SYNCHRONIZED BLOCK>************************/
                            /*
                             * Главная задача synchronized(renderData) снизу - как можно быстрее отпустить renderData, чтобы другой поток мог воспользоваться данными
                             * Да, в нем можно сразу же создать линии графика, чтобы потом по новой по deque не ходить, однако это лишние возможные
                             * миллисекунды задержки основного потока, чего допускать нежелательно
                             */
                            synchronized (renderData) {
                                StartIndex = renderData.StartIndex;
                                DataToShow = renderData.DataToShow;
                                TBM = renderData.TimeBetweenMeasurements;
                                DataCollectToFixedCPS = 1000 / TBM;
                                //float _cvi = localSurfaceData.XPOS / (gridSize.x / (DataToShow - 1));
                                float _cvi = (localSurfaceData.XPOS - gridStartPoint.x) / (gridSize.x / (DataToShow - 1));
                                if (_cvi < 0.0F)
                                    _cvi = 0.0F;
                                if ((_cvi * 1.0 - Math.floor(_cvi * 1.0)) > 0.5)
                                    _cvi = (float) Math.ceil(_cvi * 1.0);
                                else
                                    _cvi = (float) Math.floor(_cvi * 1.0);
                                ChosenValueIndex = (int)_cvi;
                                ChosenValueIndex = DataToShow - 1 - ChosenValueIndex;

                                int DataArray_size = renderData.DataArray.size();
                                int FilteredDataArray_size = renderData.FilteredDataArray.size();
                                /* *************************<_FCPS_BLOCK>**************************/
                                if (localSwitchSettings.getShowCPSgraphic() && DataCollectToFixedCPS != 1) {
                                    int ValCounter = 0;
                                    int PointCounter = 0;
                                    ListIterator<Integer> _fcps_li = renderData.DataArray.listIterator(DataArray_size - renderData.StartIndex);
                                    ListIterator<Float> _fcpsf_li = renderData.FilteredDataArray.listIterator(FilteredDataArray_size - renderData.StartIndex);
                                    float ValAccumulator = 0.0F;
                                    float FilteredValAccumulator = 0.0F;
                                    while (_fcps_li.hasPrevious() && ValCounter < (DataCollectToFixedCPS * 11)) {
                                        if (PointCounter < DataCollectToFixedCPS) {
                                            ++PointCounter;
                                            ValAccumulator += _fcps_li.previous();
                                            FilteredValAccumulator += _fcpsf_li.previous();
                                        }
                                        else {
                                            if (_FCPS_MaxValue < ValAccumulator) {
                                                _FCPS_MaxValueIndex = ValCounter / DataCollectToFixedCPS - 1; ///???? valcounter
                                                _FCPS_MaxValue = ValAccumulator;
                                            }
                                            if (_FCPS_FilteredMaxValue < FilteredValAccumulator) {
                                                _FCPS_FilteredMaxValue = FilteredValAccumulator;
                                                _FCPS_FilteredMaxValueIndex = ValCounter / DataCollectToFixedCPS - 1; ///???? valcounter
                                            }
                                            _FCPS_DataDeque.add(ValAccumulator);
                                            _FCPS_Filtered_DataDeue.add(FilteredValAccumulator);
                                            PointCounter = 1;
                                            ValAccumulator = _fcps_li.previous();
                                            FilteredValAccumulator = _fcpsf_li.previous();
                                        }
                                        ++ValCounter;
                                    }
                                    if (PointCounter == DataCollectToFixedCPS && ValCounter == (DataCollectToFixedCPS * 11)) {
                                        _FCPS_DataDeque.add(ValAccumulator);
                                        _FCPS_Filtered_DataDeue.add(FilteredValAccumulator);
                                        if (_FCPS_MaxValue < ValAccumulator) {
                                            _FCPS_MaxValueIndex = ValCounter / DataCollectToFixedCPS - 1; ///???? valcounter
                                            _FCPS_MaxValue = ValAccumulator;
                                        }
                                        if (_FCPS_FilteredMaxValue < FilteredValAccumulator) {
                                            _FCPS_FilteredMaxValue = FilteredValAccumulator;
                                            _FCPS_FilteredMaxValueIndex = ValCounter / DataCollectToFixedCPS - 1; ///???? valcounter
                                        }
                                    }
                                }
                                /* *************************</_FCPS_BLOCK>*************************/

                                //ListIterator<Integer> listIterator = renderData.DataArray.listIterator(renderData.DataArray.size());
                                ListIterator<Integer> listIterator = renderData.DataArray.listIterator(DataArray_size - renderData.StartIndex);
                                ListIterator<Float> listFilteredIterator = renderData.FilteredDataArray.listIterator(FilteredDataArray_size - renderData.StartIndex);

                                //int StartIndexCounter = 0;
                                int DataCounter = 0;
                                while (listIterator.hasPrevious()) { // @change после проверки на баги, добавь условие if-а в while
                                    //if (StartIndexCounter < StartIndex) {
                                    //    ++StartIndexCounter;
                                    //   listIterator.previous();
                                    //   continue;
                                    //}

                                    if (DataCounter < DataToShow) {
                                        int value = listIterator.previous();
                                        float filtered_value = listFilteredIterator.previous();
                                        if (DataCounter == ChosenValueIndex) {
                                            ChosenValue = value;
                                            ChosenFilteredValue = filtered_value;
                                        }

                                        if (MaxValue < value) {
                                            MaxValueIndex = DataCounter;
                                            MaxValue = value;
                                        }
                                        if (FilteredMaxValue < filtered_value) {
                                            FilteredMaxValue = filtered_value;
                                            FilteredMaxValueIndex = DataCounter;
                                        }
                                        if (value < MinYOnGraphic)
                                            MinYOnGraphic = value;
                                        DataDeque.add(value * 1.0F);
                                        DataFilteredDeque.add(filtered_value);
                                        ++DataCounter;
                                        continue;
                                    }
                                    break;
                                }
                            }
                            /* ***********************</SYNCHRONIZED BLOCK>************************/
                            /* ********<Выборка максимальных и минимальных значений по Y>**********/
                            if (!localSwitchSettings.getStartFromZero()) // Если нужна отрисовка графика начиная с нуля
                                MinYOnGraphic = 0;

                            float MaxYOnGraphic; // Это значение CPS или uRentgen, которое будет максимальным на графике
                            if (localSwitchSettings.getShowCPSgraphic() && DataCollectToFixedCPS != 1) { // Если нужно показывать график dX которого - 1с
                                if (localSwitchSettings.getDynamicResolution()) { // Если необходима динамическая подстройка по y
                                    if (localSwitchSettings.getSmoothing()) // Если включено сглаживание
                                        MaxYOnGraphic = Math.max(_FCPS_MaxValue, _FCPS_FilteredMaxValue) * COEFF_MUL_TO_MAX_Y_VAL - MinYOnGraphic;
                                        // То максимальное значение будет зависеть либо от _FCPS_MaxValue, либо от _FCPS_FilteredMaxValue
                                    else // Если сглаживание выключено
                                        MaxYOnGraphic = _FCPS_MaxValue * COEFF_MUL_TO_MAX_Y_VAL - MinYOnGraphic;
                                    // То максимальное значение зависит только от _FCPS_MaxValue
                                }
                                else { // Если динамической подстройки нет
                                    MaxYOnGraphic = localSwitchSettings.getDynamicResolutionThresholdValuee() - MinYOnGraphic;
                                    if (localSwitchSettings.getSmoothing()){
                                        if (MaxYOnGraphic < Math.max(_FCPS_MaxValue, _FCPS_FilteredMaxValue) - MinYOnGraphic)
                                            MaxYOnGraphic = Math.max(_FCPS_MaxValue, _FCPS_FilteredMaxValue) * COEFF_MUL_TO_MAX_Y_OVM - MinYOnGraphic;
                                    }
                                    else {
                                        if (MaxYOnGraphic < _FCPS_MaxValue - MinYOnGraphic)
                                            MaxYOnGraphic = _FCPS_MaxValue * COEFF_MUL_TO_MAX_Y_OVM - MinYOnGraphic;
                                    }

                                }
                            }
                            else {
                                if (localSwitchSettings.getDynamicResolution()) {
                                    if (localSwitchSettings.getSmoothing())
                                        MaxYOnGraphic = Math.max(MaxValue, FilteredMaxValue) * COEFF_MUL_TO_MAX_Y_VAL - MinYOnGraphic;
                                    else
                                        MaxYOnGraphic = MaxValue * COEFF_MUL_TO_MAX_Y_VAL - MinYOnGraphic;
                                }
                                else {
                                    MaxYOnGraphic = localSwitchSettings.getDynamicResolutionThresholdValuee() * 1.0F / DataCollectToFixedCPS - MinYOnGraphic;
                                    if (localSwitchSettings.getSmoothing()){
                                        if (MaxYOnGraphic < Math.max(MaxValue, FilteredMaxValue) - MinYOnGraphic)
                                            MaxYOnGraphic = Math.max(MaxValue, FilteredMaxValue) * COEFF_MUL_TO_MAX_Y_OVM - MinYOnGraphic;
                                    }
                                    else {
                                        if (MaxYOnGraphic < MaxValue - MinYOnGraphic)
                                            MaxYOnGraphic = MaxValue * COEFF_MUL_TO_MAX_Y_OVM - MinYOnGraphic;
                                    }
                                }
                            }
                            if (MaxYOnGraphic < 1.0F)
                                MaxYOnGraphic = Float.MAX_VALUE;
                            /* *******</Выборка максимальных и минимальных значений по Y>**********/
                            /* ******************<Получение текущих значений>**********************/
                            if (!DataDeque.isEmpty())
                                CurrentValue = DataDeque.getFirst();
                            if (!DataFilteredDeque.isEmpty())
                                CurrentFilteredValue = DataFilteredDeque.getFirst();
                            if (!_FCPS_DataDeque.isEmpty())
                                _FCPS_CurrentValue = _FCPS_DataDeque.getFirst();
                            if (!_FCPS_Filtered_DataDeue.isEmpty())
                                _FCPS_CurrentFilteredValue = _FCPS_Filtered_DataDeue.getFirst();
                            /* *****************</Получение текущих значений>**********************/
                            /* ******************<Настройка размеров шрифтов>**********************/
                            while (!fontSet1) {
                                if (fontPainter1.measureText("00000.0") < gridStartPoint.x) {
                                    fontSize1 += 5;
                                    fontPainter1.setTextSize(fontSize1);
                                    continue;
                                }
                                fontPainter1.setTextSize(fontSize1 - 10);
                                fontSet1 = true;
                            }
                            while (!fontSet2) {
                                Rect rect = new Rect();
                                fontPainter2.getTextBounds("|", 0, 1, rect);
                                if (gridStartPoint.y / rect.height()  > 7.0F) {
                                    fontSize2 += 5;
                                    fontPainter2.setTextSize(fontSize2);
                                    continue;
                                }
                                fontPainter2.setTextSize(fontSize2 - 10);
                                fontSet2 = true;
                            }
                            /* ******************</Настройка размеров шрифтов>*********************/
                            /* ***************<Отрисовка сетки и заполнение фона>******************/
                            canvas.drawColor(BACKGROUND_COLOR); // Заполняем фон
                            Renderer.drawGrid(canvas, gridStartPoint, gridStopPoint, 10, gridPainter); // Рисуем сетку
                            Renderer.prepareDataGrid(canvas, gridStopPoint.x, gridStartPoint.y, gridPainter);
                            /* ***************</Отрисовка сетки и заполнение фона>*****************/
                            /* ************************<Отрисовка графиков>************************/
                            graphicPainter.setColor(COLOR_NONFILTERED_GR);
                            Renderer.drawGraphic(canvas, gridStopPoint, gridSize, DataToShow, MaxYOnGraphic, MinYOnGraphic, DataDeque, graphicPainter);
                            Renderer.drawMaxMarker(canvas, gridStopPoint, gridSize, DataToShow, MaxYOnGraphic, MinYOnGraphic, MaxValue, MaxValueIndex, maxMarkerPainter);
                            if (localSwitchSettings.getSmoothing()) {
                                graphicPainter.setColor(COLOR_FILTERED_GR);
                                Renderer.drawGraphic(canvas, gridStopPoint, gridSize, DataToShow, MaxYOnGraphic, MinYOnGraphic, DataFilteredDeque, graphicPainter);
                                Renderer.drawMaxMarker(canvas, gridStopPoint, gridSize, DataToShow, MaxYOnGraphic, MinYOnGraphic, FilteredMaxValue, FilteredMaxValueIndex, maxMarkerPainter);
                            }
                            if (localSwitchSettings.getShowCPSgraphic() && DataCollectToFixedCPS != 1) {
                                graphicPainter.setColor(COLOR_FCPS_NONFILTERED_GR);
                                Renderer.drawGraphic(canvas, gridStopPoint, gridSize, 11, MaxYOnGraphic, MinYOnGraphic, _FCPS_DataDeque, graphicPainter);
                                Renderer.drawMaxMarker(canvas, gridStopPoint, gridSize, 11, MaxYOnGraphic, MinYOnGraphic, _FCPS_MaxValue, _FCPS_MaxValueIndex, maxMarkerPainter);
                                if (localSwitchSettings.getSmoothing()) {
                                    graphicPainter.setColor(COLOR_FCPS_FILTERED_GR);
                                    Renderer.drawGraphic(canvas, gridStopPoint, gridSize, 11, MaxYOnGraphic, MinYOnGraphic, _FCPS_Filtered_DataDeue, graphicPainter);
                                    Renderer.drawMaxMarker(canvas, gridStopPoint, gridSize, 11, MaxYOnGraphic, MinYOnGraphic, _FCPS_FilteredMaxValue, _FCPS_FilteredMaxValueIndex, maxMarkerPainter);
                                }
                            }
                            /* ***********************</Отрисовка графиков>************************/
                            /* ***********************<Отрисовка данных>***************************/
                            float dataDY = gridStartPoint.y / 7.0F;
                            float xCenter = gridStopPoint.x / 2.0F;
                            Renderer.drawData(canvas, dataDY - DATA_LINE_DY, 0, xCenter - DATA_LINE_DX, TEXT_MAX_VALUE, MaxValue,
                                    localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS,
                                    localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter2);
                            Renderer.drawTime(canvas, dataDY * 2 - DATA_LINE_DY, 0, xCenter - DATA_LINE_DX, TEXT_MAX_VAL_TIME,
                                    StartIndex, MaxValueIndex, TBM, true, fontPainter2);
                            Renderer.drawTime(canvas, dataDY * 3 - DATA_LINE_DY, 0, xCenter - DATA_LINE_DX, TEXT_MAX_VAL_dT,
                                    StartIndex, MaxValueIndex, TBM, false, fontPainter2);
                            Renderer.drawData(canvas, dataDY - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_CURRENT_VALUE, CurrentValue,
                                    localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS,
                                    localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter2);
                            graphicPainter.setColor(COLOR_NONFILTERED_GR);
                            canvas.drawLine(xCenter - DATA_LINE_DX / 2, 0, xCenter - DATA_LINE_DX / 2, dataDY * 3, graphicPainter);
                            canvas.drawLine(xCenter + DATA_LINE_DX / 2, 0, xCenter + DATA_LINE_DX / 2, dataDY, graphicPainter);
                            if (localSwitchSettings.getSmoothing()) {
                                Renderer.drawData(canvas, dataDY * 4 - DATA_LINE_DY, 0, xCenter - DATA_LINE_DX, TEXT_FILTERED_MAX_VAL, FilteredMaxValue,
                                        localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS,
                                        localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter2);
                                Renderer.drawTime(canvas, dataDY * 5 - DATA_LINE_DY, 0, xCenter - DATA_LINE_DX, TEXT_FILTERED_MAX_VAL_TIME,
                                        StartIndex, FilteredMaxValueIndex, TBM, true, fontPainter2);
                                Renderer.drawTime(canvas, dataDY * 6 - DATA_LINE_DY, 0, xCenter - DATA_LINE_DX, TEXT_FILTERED_MAX_VAL_dT,
                                        StartIndex, FilteredMaxValueIndex, TBM, false, fontPainter2);
                                Renderer.drawData(canvas, dataDY * 4 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FILTERED_CURRENT_VALUE, CurrentFilteredValue,
                                        localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS,
                                        localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter2);
                                graphicPainter.setColor(COLOR_FILTERED_GR);
                                canvas.drawLine(xCenter - DATA_LINE_DX / 2, dataDY * 3, xCenter - DATA_LINE_DX / 2, dataDY * 6, graphicPainter);
                                canvas.drawLine(xCenter + DATA_LINE_DX / 2, dataDY * 3, xCenter + DATA_LINE_DX / 2, dataDY * 4, graphicPainter);
                            }
                            else {
                                Renderer.drawNone(canvas, dataDY * 4 - DATA_LINE_DY,  0, xCenter - DATA_LINE_DX, TEXT_FILTERED_MAX_VAL, fontPainter2);
                                Renderer.drawNone(canvas, dataDY * 5 - DATA_LINE_DY,  0, xCenter - DATA_LINE_DX, TEXT_FILTERED_MAX_VAL_TIME, fontPainter2);
                                Renderer.drawNone(canvas, dataDY * 6 - DATA_LINE_DY,  0, xCenter - DATA_LINE_DX, TEXT_FILTERED_MAX_VAL_dT, fontPainter2);
                                Renderer.drawNone(canvas, dataDY * 4 - DATA_LINE_DY,  xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FILTERED_CURRENT_VALUE, fontPainter2);
                            }

                            if ((localSurfaceData.oneFingerPressed || localSwitchSettings.getLocked()) && localSurfaceData.XPOS > gridStartPoint.x) {
                                Renderer.drawData(canvas, dataDY * 2 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_CHOSEN_VALUE, ChosenValue,
                                        localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS,
                                        localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter2);
                                Renderer.drawTime(canvas, dataDY * 3 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_CHOSEN_TIME, StartIndex,
                                        ChosenValueIndex, TBM, true, fontPainter2);
                                graphicPainter.setColor(COLOR_CHOSEN_CLR);
                                canvas.drawLine(xCenter + DATA_LINE_DX / 2, dataDY * 1, xCenter + DATA_LINE_DX / 2, dataDY * 3, graphicPainter);
                                canvas.drawLine(xCenter + DATA_LINE_DX / 2, dataDY * 4, xCenter + DATA_LINE_DX / 2, dataDY * 6, graphicPainter);
                                if (localSwitchSettings.getSmoothing()) {
                                    Renderer.drawData(canvas, dataDY * 5 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FILTERED_CHOSEN_VALUE,ChosenFilteredValue,
                                            localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS,
                                            localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter2);
                                    Renderer.drawTime(canvas, dataDY * 6 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FILTERED_CHOSEN_V_TIME, StartIndex,
                                            ChosenValueIndex, TBM, true, fontPainter2);
                                }
                                else {
                                    Renderer.drawNone(canvas,dataDY * 5 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FILTERED_CHOSEN_VALUE, fontPainter2);
                                    Renderer.drawNone(canvas,dataDY * 6 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FILTERED_CHOSEN_V_TIME, fontPainter2);
                                }
                            }
                            else {
                                if (localSwitchSettings.getShowCPSgraphic() && DataCollectToFixedCPS != 1) {
                                    Renderer.drawData(canvas, dataDY * 2 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FCPS_VALUE, _FCPS_CurrentValue,
                                            localSwitchSettings.getWarningLevelValue(), localSwitchSettings.getCriticalLevelValue(), fontPainter2);
                                    Renderer.drawData(canvas, dataDY * 3 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FCPS_MAX_VALUE, _FCPS_MaxValue,
                                            localSwitchSettings.getWarningLevelValue(), localSwitchSettings.getCriticalLevelValue(), fontPainter2);
                                    graphicPainter.setColor(COLOR_FCPS_NONFILTERED_GR);
                                    canvas.drawLine(xCenter + DATA_LINE_DX / 2, dataDY * 1, xCenter + DATA_LINE_DX / 2, dataDY * 3, graphicPainter);
                                    if (localSwitchSettings.getSmoothing()) {
                                        Renderer.drawData(canvas, dataDY * 5 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FCPS_FILTERED_VALUE, _FCPS_CurrentFilteredValue,
                                                localSwitchSettings.getWarningLevelValue(), localSwitchSettings.getCriticalLevelValue(), fontPainter2);
                                        Renderer.drawData(canvas, dataDY * 6 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FCPS_FILTERED_MAX_VALUE, _FCPS_FilteredMaxValue,
                                                localSwitchSettings.getWarningLevelValue(), localSwitchSettings.getCriticalLevelValue(), fontPainter2);
                                        graphicPainter.setColor(COLOR_FCPS_FILTERED_GR);
                                        canvas.drawLine(xCenter + DATA_LINE_DX / 2, dataDY * 4, xCenter + DATA_LINE_DX / 2, dataDY * 6, graphicPainter);
                                    }
                                    else {
                                        Renderer.drawNone(canvas,dataDY * 5 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FCPS_FILTERED_VALUE, fontPainter2);
                                        Renderer.drawNone(canvas,dataDY * 6 - DATA_LINE_DY, xCenter + DATA_LINE_DX, gridStopPoint.x, TEXT_FCPS_FILTERED_MAX_VALUE, fontPainter2);
                                    }
                                }
                            }
                            /* ***********************</Отрисовка данных>**************************/
                            /* ********************<Отрисовка значений по Y>***********************/
                            if (localSwitchSettings.getShowCPSgraphic()) {
                                Renderer.drawYVals(canvas, gridStartPoint.y, gridStopPoint.y, gridStartPoint.x, MinYOnGraphic, MaxYOnGraphic,
                                        localSwitchSettings.getWarningLevelValue(), localSwitchSettings.getCriticalLevelValue(), fontPainter1);
                            }
                            else {
                                Renderer.drawYVals(canvas, gridStartPoint.y, gridStopPoint.y, gridStartPoint.x, MinYOnGraphic, MaxYOnGraphic,
                                        localSwitchSettings.getWarningLevelValue() * 1.0F / DataCollectToFixedCPS, localSwitchSettings.getCriticalLevelValue() * 1.0F / DataCollectToFixedCPS, fontPainter1);
                            }
                            /* *******************</Отрисовка значений по Y>***********************/
                            /* *****************<Отрисовка значений над графиком>******************/
                            Renderer.drawXVals(canvas, gridStartPoint.x, gridStopPoint.x, gridStartPoint.y - DATA_LINE_DY, StartIndex, DataToShow, TBM, fontPainter1);
                            /* *****************</Отрисовка значений над графиком>*****************/
                            /* ********************<Отрисовка выбранной линии>*********************/
                            Paint paint = new Paint();
                            paint.setColor(COLOR_CHOSEN_CLR);
                            paint.setStrokeWidth(CHOSEN_STROKE_WIDTH);
                            if (localSurfaceData.XPOS > gridStartPoint.x) {
                                if ((localSwitchSettings.getLocked() || localSurfaceData.oneFingerPressed) && !localSurfaceData.scaleMode && ChosenValue > 0.0F) {
                                    float xPos = gridSize.x / (DataToShow - 1) * ChosenValueIndex;
                                    canvas.drawLine(gridStopPoint.x - xPos, gridStartPoint.y, gridStopPoint.x - xPos, Height, paint);
                                }
                            }
                            /* *******************</Отрисовка выбранной линии>*********************/
                            /* ***********************</Блок отрисовки>****************************/
                        }
                    }
                }
                finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}

