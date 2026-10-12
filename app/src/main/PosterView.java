package com.awisogera.votingpostermaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;

public class PosterView extends View {

    private Bitmap image;

    public PosterView(Context context) {
        super(context);
    }

    public void setImage(Uri uri) {
        try {
            image = BitmapFactory.decodeStream(
                    getContext().getContentResolver().openInputStream(uri)
            );
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.rgb(239, 103, 210));

        if (image != null) {
            canvas.drawBitmap(image, null,
                    new android.graphics.Rect(100, 100, 700, 700),
                    null);
        }
    }
}
