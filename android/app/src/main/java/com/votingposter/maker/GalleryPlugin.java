package com.votingposter.maker;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;

@CapacitorPlugin(name = "Gallery")
public class GalleryPlugin extends Plugin {

    @PluginMethod
    public void saveImage(PluginCall call) {
        String data = call.getString("data");
        String fileName = call.getString("fileName", "Voting-Maker-Poster.png");

        if (data == null || data.isEmpty()) {
            call.reject("Image data is empty");
            return;
        }

        Uri uri = null;

        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            Context context = getContext();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Voting Poster Maker");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            uri = context.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            );

            if (uri == null) {
                throw new Exception("Android could not create the gallery image");
            }

            try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                if (output == null) {
                    throw new Exception("Could not open image output stream");
                }
                output.write(bytes);
                output.flush();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, ready, null, null);
            }

            JSObject result = new JSObject();
            result.put("saved", true);
            result.put("uri", uri.toString());
            call.resolve(result);

        } catch (Exception e) {
            if (uri != null) {
                try {
                    getContext().getContentResolver().delete(uri, null, null);
                } catch (Exception ignored) {}
            }
            call.reject("Gallery save failed: " + e.getMessage(), e);
        }
    }
}
