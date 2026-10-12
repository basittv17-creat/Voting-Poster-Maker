package com.awisogera.votingpostermaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import android.graphics.Paint;

public class PosterView extends View {

    private Bitmap imageA;
    private Bitmap imageB;

    private ArrayList<TextItem> texts = new ArrayList<>();
    private Paint textPaint = new Paint();

    private float ax = 50, ay = 100;
    private float bx = 650, by = 100;

    private float scaleA = 1f;
    private float scaleB = 1f;

    private int activePhoto = 0;
    private int activeText = -1;
    private float lastX, lastY;
    private float startDistance;
    private float startScale;

    public PosterView(Context context) {
        super(context);
    }

    public void setImageA(Uri uri) {
        imageA = loadBitmap(uri);
        invalidate();
    }

    public void setImageB(Uri uri) {
        imageB = loadBitmap(uri);
        invalidate();
    }

    public void addText(String value) {
        texts.add(new TextItem(value));
        invalidate();
    }

    
public void increaseTextSize() {
    if (activeText >= 0 && activeText < texts.size()) {
        TextItem item = texts.get(activeText);
        item.size += 10;
        invalidate();
    }
}

public void decreaseTextSize() {
    if (activeText >= 0 && activeText < texts.size()) {
        TextItem item = texts.get(activeText);
        item.size = Math.max(10, item.size - 10);
        invalidate();
    }
}

private Bitmap loadBitmap(Uri uri) {

        try {
            return BitmapFactory.decodeStream(
                    getContext().getContentResolver().openInputStream(uri)
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastX = x;
            lastY = y;

            if (event.getPointerCount() == 2) {
                float dx = event.getX(1) - event.getX(0);
                float dy = event.getY(1) - event.getY(0);
                startDistance = (float)Math.sqrt(dx * dx + dy * dy);
                startScale = activePhoto == 1 ? scaleA : scaleB;
            }

            for (int i = 0; i < texts.size(); i++) {
                TextItem item = texts.get(i);
                if (new RectF(item.x, item.y - item.size, item.x + 400, item.y).contains(x, y)) {
                    activeText = i;
                    break;
                }
            }

            if (activeText == -1 && imageA != null && new RectF(ax, ay, ax + 500, ay + 500).contains(x, y)) {
                activePhoto = 1;
            } else if (imageB != null && new RectF(bx, by, bx + 500, by + 500).contains(x, y)) {
                activePhoto = 2;
            }
        }

        if (event.getPointerCount() == 2 && event.getAction() == MotionEvent.ACTION_MOVE) {

            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            float distance = (float)Math.sqrt(dx * dx + dy * dy);

            if (startDistance > 0) {
                float factor = distance / startDistance;

                if (activeText >= 0) {
                TextItem item = texts.get(activeText);
                item.x += dx;
                item.y += dy;
            }

            if (activeText < 0 && activePhoto == 1) {
                    scaleA = Math.max(0.5f, Math.min(startScale * factor, 3f));
                }

                if (activePhoto == 2) {
                    scaleB = Math.max(0.5f, Math.min(startScale * factor, 3f));
                }

                invalidate();
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE)

            float dx = x - lastX;
            float dy = y - lastY;

            if (activeText >= 0) {
                TextItem item = texts.get(activeText);
                item.x += dx;
                item.y += dy;
            }

            if (activeText < 0 && activePhoto == 1) {
                ax += dx;
                ay += dy;
            }

            if (activePhoto == 2) {
                bx += dx;
                by += dy;
            }

            lastX = x;
            lastY = y;
            invalidate();
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            activePhoto = 0;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.rgb(239, 103, 210));

        textPaint.setTextSize(50);
        for (TextItem item : texts) {
            textPaint.setColor(item.color);
            textPaint.setTextSize(item.size);
            canvas.drawText(item.text, item.x, item.y, textPaint);
        }

        if (imageA != null) {
            canvas.drawBitmap(imageA, null,
                    new RectF(ax, ay, ax + 500 * scaleA, ay + 500 * scaleA),
                    null);
        }

        if (imageB != null) {
            canvas.drawBitmap(imageB, null,
                    new RectF(bx, by, bx + 500 * scaleB, by + 500 * scaleB),
                    null);
        }
    }
}
