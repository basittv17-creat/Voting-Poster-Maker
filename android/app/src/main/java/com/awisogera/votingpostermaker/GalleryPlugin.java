package com.awisogera.votingpostermaker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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
        String fileName = call.getString(
            "fileName", "Voting-Poster-Maker.png"
        );

        if (data == null || data.isEmpty()) {
            call.reject("Image data is missing.");
            return;
        }

        Uri imageUri = null;

        try {
            byte[] imageBytes = Base64.decode(data, Base64.DEFAULT);
            ContentResolver resolver = getContext().getContentResolver();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/VotingPosterMaker"
                );
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            );

            if (imageUri == null) {
                throw new Exception("Could not create Gallery image.");
            }

            try (OutputStream output = resolver.openOutputStream(imageUri)) {
                if (output == null) {
                    throw new Exception("Could not open image output.");
                }
                output.write(imageBytes);
                output.flush();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues completed = new ContentValues();
                completed.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(imageUri, completed, null, null);
            }

            JSObject result = new JSObject();
            result.put("saved", true);
            result.put("uri", imageUri.toString());
            call.resolve(result);

        } catch (Exception e) {
            if (imageUri != null) {
                try {
                    getContext().getContentResolver()
                        .delete(imageUri, null, null);
                } catch (Exception ignored) {
                }
            }
            call.reject("Could not save poster: " + e.getMessage(), e);
        }
    }
}
