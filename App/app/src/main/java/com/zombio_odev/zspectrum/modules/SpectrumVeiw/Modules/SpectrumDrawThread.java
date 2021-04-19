package com.zombio_odev.zspectrum.modules.SpectrumVeiw.Modules;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;

import com.zombio_odev.zspectrum.MainActivity;
import com.zombio_odev.zspectrum.R;
import com.zombio_odev.zspectrum.modules.CpsGraphics.Modules.Renderer;
import com.zombio_odev.zspectrum.modules.SpectrumVeiw.SpectrumView;

public class SpectrumDrawThread extends Thread {

    public static final float START_X_GRID_COEFFICIENT = 0.2F;
    public static final float START_Y_GRID_COEFFICIENT = 0.1F;



    private final SurfaceHolder surfaceHolder;
    private final SpectrumView.SpectrumData spectrumData;
    private final SpectrumView.SpectrumSurfaceData surfaceData;
    private boolean runFlag = false;

    public SpectrumDrawThread(@NonNull SurfaceHolder surfaceHolder, @NonNull SpectrumView.SpectrumData sData, @NonNull SpectrumView.SpectrumSurfaceData surfaceData) {
        this.surfaceHolder = surfaceHolder;
        this.surfaceData = surfaceData;
        spectrumData = sData;
        super.setName("SpectrumDrawThread");
    }

    public void setRunning(boolean running) {this.runFlag = running;}

    @Override
    public void run() {
        Canvas canvas;
        SpectrumView.SpectrumData localSpectrumData = new SpectrumView.SpectrumData();
        SpectrumView.SpectrumSurfaceData localSurfaceData;

        Paint gridPainter = new Paint();
        gridPainter.setColor(Color.GRAY);
        gridPainter.setStrokeWidth(3);
        gridPainter.setAlpha(127);

        Paint fontPainter1 = new Paint();
        boolean fontSet1 = false;
        int fontSize1 = 10;
        fontPainter1.setColor(Color.WHITE);

        while(runFlag) {
            if (spectrumData.isUpdated || surfaceData.isUpdated) {
                canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        int width = canvas.getWidth();
                        int height = canvas.getHeight();

                        PointF gridStartPoint = new PointF(width * START_X_GRID_COEFFICIENT, height * START_Y_GRID_COEFFICIENT);
                        PointF gridStopPoint = new PointF(width, height);
                        PointF gridSize = new PointF(gridStopPoint.x - gridStartPoint.x, gridStopPoint.y - gridStartPoint.y);


                        synchronized (spectrumData) {
                            spectrumData.isUpdated = false;
                            for (int i = 0; i < 4096; i++)
                                localSpectrumData.spectrum[i] = spectrumData.spectrum[i];
                        }

                        synchronized (surfaceData) {
                            surfaceData.isUpdated = false;
                            if (surfaceData.channelsToShow > gridSize.x)
                                surfaceData.channelsToShow = (int)gridSize.x;
                            if (surfaceData.channelsToShow + surfaceData.startChannel > 4096)
                                surfaceData.startChannel = 4096 - surfaceData.channelsToShow;
                            localSurfaceData = new SpectrumView.SpectrumSurfaceData(surfaceData);
                        }

                        while (!fontSet1) {
                            if (fontPainter1.measureText("000000000") < gridStartPoint.x) {
                                fontSize1 += 2;
                                fontPainter1.setTextSize(fontSize1);
                                continue;
                            }
                            fontPainter1.setTextSize(fontSize1 - 4);
                            fontSet1 = true;
                        }

                        int maxVal = 0;
                        for (int val : localSpectrumData.spectrum)
                            maxVal = Math.max(val, maxVal);

                        int maxYOnGraphic = maxVal;
                        if (maxYOnGraphic < 5)
                            maxYOnGraphic = 5;

                        float dX = gridSize.x / localSurfaceData.channelsToShow;
                        float dY = gridSize.y / maxYOnGraphic;

                        Paint p = new Paint();
                        RectF r = new RectF();
                        p.setColor(Color.RED);


                        canvas.drawColor(Color.BLACK);

                        Renderer.drawGrid(canvas, gridStartPoint, gridStopPoint, 10, gridPainter);
                        Renderer.drawYVals(canvas, gridStartPoint.y, gridStopPoint.y, gridStartPoint.x, 0, maxYOnGraphic, Float.MAX_VALUE, Float.MAX_VALUE, fontPainter1);

                        for (int i = localSurfaceData.startChannel; i < localSurfaceData.startChannel + localSurfaceData.channelsToShow - 1; i++) {
                            r.left = (i - localSurfaceData.startChannel) * dX + gridStartPoint.x;
                            r.right = (i - localSurfaceData.startChannel + 1) * dX + gridStartPoint.x;
                            r.top = (float) height - ((float)localSpectrumData.spectrum[i % 4096]) / ((float)maxYOnGraphic) * gridSize.y;
                            r.bottom = (float) height;
                            canvas.drawRect(r, p);
                        }

                        canvas.drawText(Integer.toString(localSurfaceData.startChannel), gridStartPoint.x, gridStartPoint.y, fontPainter1);
                        float lLen = fontPainter1.measureText(Integer.toString(localSurfaceData.startChannel + localSurfaceData.channelsToShow));
                        canvas.drawText(Integer.toString(localSurfaceData.startChannel + localSurfaceData.channelsToShow), gridStopPoint.x - lLen, gridStartPoint.y, fontPainter1);

                        if (localSurfaceData.oneFingerPressed)
                        {
                            Paint chosenVal = new Paint();
                            chosenVal.setColor(Color.GREEN);
                            chosenVal.setStrokeWidth(3);
                            int chosenIndex = (int)((localSurfaceData.xPoint - gridStartPoint.x) / dX);
                            canvas.drawLine(dX * chosenIndex + gridStartPoint.x + dX / 2.0F, gridStartPoint.y, dX * chosenIndex + gridStartPoint.x + dX / 2.0F, gridStopPoint.y, chosenVal);
                            fontPainter1.setColor(Color.GREEN);
                            int val = localSpectrumData.spectrum[(localSurfaceData.startChannel + chosenIndex) % 4096];
                            canvas.drawText(Integer.toString(localSurfaceData.startChannel + chosenIndex) + "(" +
                                    Integer.toString(val) + ")", gridStartPoint.x + gridSize.x / 2.0F, gridStartPoint.y, fontPainter1);

                            fontPainter1.setColor(Color.WHITE);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

    }
}
