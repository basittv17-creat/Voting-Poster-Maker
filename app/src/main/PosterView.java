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
                    new android.graphics.Rect(50, 100, 550, 600),
                    null);
        }

        if (imageB != null) {
            canvas.drawBitmap(imageB, null,
                    new android.graphics.Rect(650, 100, 1150, 600),
                    null);
        }
    }
}
