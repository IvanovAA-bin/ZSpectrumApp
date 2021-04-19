package com.zombio_odev.zspectrum.modules.CpsGraphics.Modules;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import java.util.ArrayDeque;

public class Renderer {


    public static void drawGrid(@NonNull Canvas canvas,@NonNull PointF startPoint,@NonNull PointF stopPoint, int GridSize, @NonNull Paint paint) {
        float dx = (stopPoint.x - startPoint.x) / GridSize;
        float dy = (stopPoint.y - startPoint.y) / GridSize;
        float PointX = startPoint.x;
        float PointY = startPoint.y;
        for (int i = 0; i <= GridSize; i++) {
            canvas.drawLine(PointX, startPoint.y, PointX, stopPoint.y, paint);
            canvas.drawLine(startPoint.x, PointY, stopPoint.x, PointY, paint);
            PointX += dx;
            PointY += dy;
        }
    }

    public static void drawGraphic(@NonNull Canvas canvas,@NonNull PointF lowestPoint,@NonNull PointF gridSize,
                                   int DataToShow, float MaxYOnGraphic, float MinYOnGraphic,
                                   @NonNull ArrayDeque<Float> DataDeque, @NonNull Paint paint) {
        float DX = gridSize.x / (DataToShow - 1);
        float startX = lowestPoint.x;
        float stopX = lowestPoint.x - DX;

        float yPrev = lowestPoint.y / 2.0F;
        if (!DataDeque.isEmpty()) {
            float val = DataDeque.pop();
            yPrev = lowestPoint.y - gridSize.y * ((val-MinYOnGraphic) / MaxYOnGraphic);
        }
        while (!DataDeque.isEmpty()) {
            float val = DataDeque.pop() - MinYOnGraphic;
            float yNext = lowestPoint.y - gridSize.y * (val / MaxYOnGraphic);
            canvas.drawLine(startX, yPrev, stopX, yNext, paint);
            startX = stopX;
            stopX -= DX;
            yPrev = yNext;
        }
    }

    public static void drawMaxMarker(@NonNull Canvas canvas, @NonNull PointF lowestPoint, @NonNull PointF gridSize,
                                     int DataToShow, float MaxYOnGraphic, float MinYOnGraphic, float Data, int DataIndex, @NonNull Paint paint) {
        float X = gridSize.x / (DataToShow - 1) * DataIndex;
        float Y = lowestPoint.y - gridSize.y * ((Data-MinYOnGraphic) / MaxYOnGraphic);
        float dY = gridSize.y / 40.0F;
        canvas.drawLine(lowestPoint.x - X, Y - dY,lowestPoint.x - X, Y + dY, paint);
    }

    public static void prepareDataGrid(@NonNull Canvas canvas, float stopPointX, float stopPointY, Paint paint) {
        float dy = stopPointY / 7.0F;
        for (int i = 0; i <= 7; i++) {
            canvas.drawLine(0, dy * i, stopPointX, dy* i, paint);
        }
        canvas.drawLine(0, 0, 0, stopPointY, paint);
        canvas.drawLine(stopPointX, 0, stopPointX, stopPointY, paint);
        canvas.drawLine(stopPointX / 2.0F, 0, stopPointX / 2.0F, dy * 6, paint);
    }

    public static void drawNone(@NonNull Canvas canvas, float Y, float startX, float stopX, String text, @NonNull Paint paint) {
        float xSize = paint.measureText(DrawThread.NONE_STRING);
        canvas.drawText(DrawThread.NONE_STRING, stopX - xSize, Y, paint);
        Path path = new Path();
        path.moveTo(startX, Y);
        path.rLineTo(stopX - xSize, 0.0F);
        path.close();
        canvas.drawTextOnPath(text, path, 0, 0, paint);
    }

    public static void drawTime(@NonNull Canvas canvas, float Y, float startX, float stopX, String text,
                                int startIndex, int currentIndex, int TBM, boolean addSystemTime, @NonNull Paint paint) {
        long ltime = (startIndex + currentIndex) * TBM;
        if (addSystemTime)
            ltime = System.currentTimeMillis() - ltime;
        int millis = (int)(ltime % 1000);
        ltime /= 1000;
        ltime %= 86400;
        int time = (int)ltime;
        int sec = time % 60;
        int min = (time / 60) % 60;
        int hou = (time / 3600) % 24;
        StringBuilder stringBuilder = new StringBuilder();
        String helper;

        helper = Integer.toString(hou);
        if (helper.length() < 2)
            stringBuilder.append("0");
        stringBuilder.append(helper);
        stringBuilder.append(":");

        helper = Integer.toString(min);
        if (helper.length() < 2)
            stringBuilder.append("0");
        stringBuilder.append(helper);
        stringBuilder.append(":");

        helper = Integer.toString(sec);
        if (helper.length() < 2)
            stringBuilder.append("0");
        stringBuilder.append(helper);
        if (!addSystemTime) {
            stringBuilder.append(".");
            helper = Integer.toString(millis);
            if (helper.length() == 1)
                stringBuilder.append("00");
            else if (helper.length() == 2)
                stringBuilder.append("0");
            stringBuilder.append(helper);
        }
        String data = stringBuilder.toString();
        float xSize = paint.measureText(data);
        canvas.drawText(data, stopX - xSize, Y, paint);
        Path path = new Path();
        path.moveTo(startX, Y);
        path.rLineTo(stopX - xSize, 0.0F);
        path.close();
        canvas.drawTextOnPath(text, path, 0, 0, paint);
    }

    public static void drawData(@NonNull Canvas canvas, float Y, float startX, float stopX, String text,
                                float data, float warningLvl, float criticalLvl, @NonNull Paint paint) {
        int startColor = paint.getColor();
        StringBuilder dataBuilder = new StringBuilder();
        if (data > warningLvl)
            paint.setColor(DrawThread.WARNING_LEVEL_COLOR);
        if (data > criticalLvl)
            paint.setColor(DrawThread.CRITICAL_LEVEL_COLOR);
        String ch = Float.toString(data);
        int dotIndex = ch.indexOf(".") + 1;
        for (int i = 0; i <= dotIndex; i++)
            dataBuilder.append(ch.charAt(i));
        String dataToShow = dataBuilder.toString();
        float xSize = paint.measureText(dataToShow);
        canvas.drawText(dataToShow, stopX - xSize, Y, paint);
        Path path = new Path();
        path.moveTo(startX, Y);
        path.rLineTo(stopX - xSize, 0.0F);
        path.close();
        paint.setColor(startColor);
        canvas.drawTextOnPath(text, path, 0, 0, paint);
    }

    public static void drawYVals(@NonNull Canvas canvas, float startY, float stopY, float stopX,
                                 float MinYonGrpahic, float MaxYonGrpahic, float warningLvl, float criticalLvl, @NonNull Paint paint) {
        StringBuilder dataBuilder = new StringBuilder();


        Rect rect = new Rect();
        paint.getTextBounds("0", 0, 1, rect);
        int startColor = paint.getColor();
        int symHeight = rect.height();
        float halfHeight = symHeight / 2.0F;
        float MaxValShow = MaxYonGrpahic + MinYonGrpahic;
        float dyVAL = MaxYonGrpahic / 10.0F;
        float dyGR = (stopY - startY) / 10.0F;
        String dataString = Float.toString(MaxValShow);
        int dotIndex = dataString.indexOf(".") + 1;
        for (int i = 0; i <= dotIndex; i++)
            dataBuilder.append(dataString.charAt(i));
        dataString = dataBuilder.toString();
        float xSize = paint.measureText(dataString);
        if (MaxValShow > warningLvl)
            paint.setColor(DrawThread.WARNING_LEVEL_COLOR);
        if (MaxValShow > criticalLvl)
            paint.setColor(DrawThread.CRITICAL_LEVEL_COLOR);
        canvas.drawText(dataString, stopX - xSize, startY + symHeight, paint);
        for (int i = 1; i < 10; i++) {
            paint.setColor(startColor);
            dataBuilder.delete(0, dataBuilder.length());
            MaxValShow -= dyVAL;
            dataString = Float.toString(MaxValShow);
            dotIndex = dataString.indexOf(".") + 1;
            for (int j = 0; j <= dotIndex; j++)
                dataBuilder.append(dataString.charAt(j));
            dataString = dataBuilder.toString();
            xSize = paint.measureText(dataString);
            if (MaxValShow > warningLvl)
                paint.setColor(DrawThread.WARNING_LEVEL_COLOR);
            if (MaxValShow > criticalLvl)
                paint.setColor(DrawThread.CRITICAL_LEVEL_COLOR);
            canvas.drawText(dataString, stopX - xSize, startY + dyGR * i + halfHeight, paint);
        }
        paint.setColor(startColor);
        dataBuilder.delete(0, dataBuilder.length());
        MaxValShow -= dyVAL;
        if (MaxValShow < 0.0001)
            MaxValShow = 0.0F;
        dataString = Float.toString(MaxValShow);
        dotIndex = dataString.indexOf(".") + 1;
        for (int j = 0; j <= dotIndex; j++)
            dataBuilder.append(dataString.charAt(j));
        dataString = dataBuilder.toString();
        xSize = paint.measureText(dataString);
        if (MaxValShow > warningLvl)
            paint.setColor(DrawThread.WARNING_LEVEL_COLOR);
        if (MaxValShow > criticalLvl)
            paint.setColor(DrawThread.CRITICAL_LEVEL_COLOR);
        canvas.drawText(dataString, stopX - xSize, stopY, paint);
        paint.setColor(startColor);
    }

    public static void drawXVals(@NonNull Canvas canvas, float stop1X, float stop2X, float Y, int StartIndex, int DataToShow, int TBM, @NonNull Paint paint) {
        StringBuilder sbr = new StringBuilder();
        boolean sec = false;
        float deltaX = (DataToShow - 1) * TBM / 10.0F;
        if (deltaX >= 999.999F) {
            deltaX /= 1000.0F;
            sec = true;
        }
        String dataStr = Float.toString(deltaX);
        int dotIndex = dataStr.indexOf(".") + 1;
        for (int i=0;i<= dotIndex;i++)
            sbr.append(dataStr.charAt(i));
        if (sec)
            sbr.append("s");
        else
            sbr.append("ms");
        Path path = new Path();
        path.moveTo(0, Y);
        path.rLineTo(stop1X, 0);
        path.close();
        canvas.drawTextOnPath(sbr.toString(), path, 0, 0, paint);
        sbr.delete(0, sbr.length());
        long _startTime = System.currentTimeMillis() - StartIndex * TBM;
        long _stopTime = _startTime - (DataToShow - 1) * TBM;
        _startTime /= 1000; _stopTime /= 1000;
        _startTime %= 86400; _stopTime %= 86400;
        int startTime = (int) _startTime;
        int stopTime = (int) _stopTime;
        String startDataString = Renderer.prepareDataString(startTime);
        String stopDataString = Renderer.prepareDataString(stopTime);
        canvas.drawText(stopDataString, stop1X, Y, paint);
        float xSize = paint.measureText(startDataString);
        canvas.drawText(startDataString, stop2X - xSize, Y, paint);
    }

    private static String prepareDataString(int time) {
        StringBuilder stringBuilder = new StringBuilder();
        String helper;
        int sec = time % 60;
        int min = (time / 60) % 60;
        int hou = (time / 3600) % 24;

        helper = Integer.toString(hou);
        if (helper.length() < 2)
            stringBuilder.append("0");
        stringBuilder.append(helper);
        stringBuilder.append(":");

        helper = Integer.toString(min);
        if (helper.length() < 2)
            stringBuilder.append("0");
        stringBuilder.append(helper);
        stringBuilder.append(":");

        helper = Integer.toString(sec);
        if (helper.length() < 2)
            stringBuilder.append("0");
        stringBuilder.append(helper);
        return stringBuilder.toString();
    }

}

