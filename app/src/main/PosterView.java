package com.awisogera.votingpostermaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;

public class PosterView extends View {

    private Bitmap imageA;
    private Bitmap imageB;

    private int ax = 50, ay = 100;
    private int bx = 650, by = 100;

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

    private Bitmap loadBitmap(Uri uri) {
        try {
            return BitmapFactory.decodeStream(
                getContext().getContentResolver().openInputStream(uri)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.rgb(239, 103, 210));

        if (imageA != null) {
            canvas.drawBitmap(imageA, null,
                    new android.graphics.Rect(ax, ay, ax + 500, ay + 500),
                    null);
        }

        if (imageB != null) {
            canvas.drawBitmap(imageB, null,
                    new android.graphics.Rect(bx, by, bx + 500, by + 500),
                    null);
        }
    }
}
