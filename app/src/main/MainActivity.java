package com.awisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    private PosterView posterView;
    private static final int PICK_A = 101;
    private static final int PICK_B = 102;
    private int currentPick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        posterView = new PosterView(this);

        Button photoA = new Button(this);
        photoA.setText("Add Photo A");

        Button photoB = new Button(this);
        photoB.setText("Add Photo B");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(photoA);
        layout.addView(photoB);
        layout.addView(posterView);

        setContentView(layout);

        photoA.setOnClickListener(v -> {
            currentPick = PICK_A;
            openGallery();
        });

        photoB.setOnClickListener(v -> {
            currentPick = PICK_B;
            openGallery();
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, currentPick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            if (requestCode == PICK_A) {
                posterView.setImageA(imageUri);
            }

            if (requestCode == PICK_B) {
                posterView.setImageB(imageUri);
            }
        }
    }
}
