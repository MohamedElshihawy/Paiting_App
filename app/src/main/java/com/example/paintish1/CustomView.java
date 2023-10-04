package com.example.paintish1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Range;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomView extends View {

    public final static int BRUSH_SIZE = 14;
    public final static int BRUSH_DEFAULT_COLOR = Color.BLUE;
    public final static int BG_DEFAULT_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    private final static int CIRCLE_RADIUS = 20;
    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    Range<Float> xRange;
    Range<Float> yRange;
    int loopLetterPath = 0;
    int canvasWidth, canvasHeight, cellWidthHeight;
    int xStartDrawingPoint, yStartDrawingPoint;
    //store your drawing lines as paths here
    List<Stroke> mPaths = new ArrayList<>();
    List<ArrayList<Point>> allLetters = new ArrayList<>();
    ArrayList<Point> letterPath;
    ArrayList<Point> displayedLetter = new ArrayList<>();
    List<Point> drawnPath = new ArrayList<>();
    List<Stroke> redo_Paths = new ArrayList<>();
    ArrayList<Float> xPoints = new ArrayList<>(), yPoints = new ArrayList<>();
    ArrayList<Integer> circleOrder = new ArrayList<>();
    private boolean undoClearScreen = false;
    private int currentColor;
    private float Mx, My;
    private int currentStrokeWidth;
    //a clipboard to put your canvas on like attaching your drawing to an blank image
    private Bitmap bitmap;
    // to use for drawing
    private Paint paint;
    private Paint paintLetter;
    private Paint fillCircles;
    //to store each line you draw as a collection of points
    private Path path;
    //to use as a paper sheet to draw on
    private Canvas canvas;


    public CustomView(Context context) {
        super(context, null);
    }


    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // this is used to smoothen your drawings and give it attribute like color and size
        paintLetter = new Paint();
        // paintLetter.setColor(Color.BLACK);
        paintLetter.setStyle(Paint.Style.STROKE);
        paintLetter.setStrokeWidth(5);
        fillCircles = new Paint();
        //  fillCircles.setColor(Color.GREEN);
        fillCircles.setStyle(Paint.Style.FILL);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAlpha(0xff);
    }

    public void initialize(int height, int width) {

        //initialise your bitmap with screen width , height and color system
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //put the canvas on the bitmap
        canvas = new Canvas(bitmap);
        // give it default attribute
        currentColor = BRUSH_DEFAULT_COLOR;
        currentStrokeWidth = BRUSH_SIZE;
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();
    }

    // brush color
    public void setColor(int color) {
        currentColor = color;
    }

    // brush size
    public void setStrokeSize(int size) {
        currentStrokeWidth = size;

    }

    //undo your last drawing action
    public void undoChanges() {
        if (mPaths.size() != 0) {
            mPaths.remove(mPaths.size() - 1);

            //to repaint the screen after deleting the last line you draw
            invalidate();
            Log.i("TAG", "undoChanges: mpaths  " + mPaths.size() + "redo paths " + redo_Paths.size());
        }

    }


    public void redoChanges() {

        if (mPaths.size() < redo_Paths.size()) {

            if (undoClearScreen) {
                mPaths.addAll(redo_Paths);
                invalidate();
            } else {
                mPaths.add(redo_Paths.get(mPaths.size()));
                invalidate();
                Log.i("TAG", "undoChanges: mpaths  " + mPaths.size() + "redo paths " + redo_Paths.size());
            }
        }
    }

    public void clearScreen() {
        undoClearScreen = true;
        mPaths.clear();
        // setBackgroundColor(Color.WHITE);
        invalidate();
    }

    //return the bitmap u draw to save it later
    public Bitmap saveDrawing() {
        return bitmap;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //save the canvas state
        canvas.save();

        //background color of the canvas.
        canvas.drawColor(BG_DEFAULT_COLOR);

        for (Stroke mstroke : mPaths) {
            paint.setColor(mstroke.color);
            paint.setStrokeWidth(mstroke.width);
            canvas.drawPath(mstroke.path, paint);

        }
        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);

        canvas.restore();
    }

    private void onTouchStart(float x, float y) {

        path = new Path();

        mPaths.add(new Stroke(currentColor, currentStrokeWidth, path));
        redo_Paths.add(new Stroke(currentColor, currentStrokeWidth, path));

        path.reset();

        path.moveTo(x, y);

        Mx = x;
        My = y;

        drawnPath.add(new Point((int) x, (int) y));

    }

    private void onTouchMove(float x, float y) {
        float dx = Math.abs(x - Mx);
        float dy = Math.abs(y - My);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(Mx, My, (x + Mx) / 2, (y + My) / 2);

            drawnPath.add(new Point((int) x, (int) y));
            Mx = x;
            My = y;
        }

    }

    public void drawLetters() {
        letterPath = new ArrayList<>();
        float xCenter, yCenter;
        int circleCount;

        String[] letter, line;
        String letterFromTxt = readFromFile();
        letter = letterFromTxt.split("\n");
        Log.i("TAG", "items : " + letter.length);
        for (int i = 0; i < letter.length; i++) {
            line = letter[i].split("\\s+");
            xCenter = Float.parseFloat(line[0].substring(line[0].indexOf("=") + 1));
            yCenter = Float.parseFloat(line[1].substring(line[1].indexOf("=") + 1));
            circleCount = Integer.parseInt(line[2].substring(line[2].indexOf("=") + 1));
            xPoints.add(xCenter);
            yPoints.add(yCenter);
            // circle order to be filled
            circleOrder.add(circleCount);
        }
        float minX = Collections.min(xPoints);
        float maxX = Collections.max(xPoints);
        float minY = Collections.min(yPoints);
        float maxY = Collections.max(yPoints);
        Log.i("TAG", "min max: " + minY + "     " + minX + xPoints.size());

        float panelStartH = (float) (canvasHeight - 1200) / 2;
        float panelStartW = (float) (canvasWidth - 1536) / 2;
        float letterStartW = panelStartW + ((1536 - (maxX - minX)) / 2);
        float letterStartH = panelStartH + ((1200 - (maxY - minX)) / 2);
        Log.i("TAG", "drawLetters: " + letterStartH + "   w  " + letterStartW);


        for (int i = 0; i < xPoints.size(); i++) {
            xPoints.set(i, (xPoints.get(i) - minX) + letterStartW);
            yPoints.set(i, (yPoints.get(i) - minY) + letterStartH);
            canvas.drawCircle(xPoints.get(i), yPoints.get(i), CIRCLE_RADIUS, paintLetter);
            invalidate();
            Log.i("TAG", "drawLetters: " + xPoints.get(i) + "     " + yPoints.get(i) + "     " + i);

        }
    }


    public String readFromFile() {
        String data = "";
        Context context = getContext();
        String filename = "taa.txt";
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader
                    (context.getAssets().open(filename), StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                data = data + line + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }

    private void fillCircleOnTouch(float x, float y) {
        float xStart, xEnd, yStart, yEnd;
        for (int i = 0; i < xPoints.size(); i++) {
            fillCircles.setColor(blendColors("#FF0000", "#0000FF", i * ((float) (1.0 / xPoints.size()))));
            xStart = xPoints.get(i) - CIRCLE_RADIUS;
            xEnd = xPoints.get(i) + CIRCLE_RADIUS;
            yStart = yPoints.get(i) - CIRCLE_RADIUS;
            yEnd = yPoints.get(i) + CIRCLE_RADIUS;
            xRange = new Range<>(xStart, xEnd);
            yRange = new Range<>(yStart, yEnd);
            if (xRange.contains(x) && yRange.contains(y)) {
                canvas.drawCircle(xPoints.get(i), yPoints.get(i), CIRCLE_RADIUS, fillCircles);
            }
            //Log.i("TAG", "fillCircleOnTouch:  s" + xStart + " e " + xEnd + " x " + x + "  s" + yStart + " e " + yEnd + " x " + y);


        }
    }

    public void makeNewWhiteScreen() {
        // new paper to draw on
        bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        clearScreen();
        canvas.setBitmap(bitmap);
    }

    private void onTouchUp() {
        path.lineTo(Mx, My);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchStart(x, y);
                fillCircleOnTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(x, y);
                fillCircleOnTouch(x, y);
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                onTouchUp();
                fillCircleOnTouch(x, y);
                invalidate();
                break;
        }

        return true;
    }

    private int blendColors(String color, String color0, float ratio) {
        final float inverseRation = 1f - ratio;
        int color1 = Color.parseColor(color);
        int color2 = Color.parseColor(color0);
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

}
