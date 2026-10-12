package com.awisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

public class MainActivity extends Activity {

    private PosterView posterView;
    private static final int PICK_IMAGE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        posterView = new PosterView(this);
        setContentView(posterView);

        posterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            posterView.setImage(imageUri);
        }
    }
}
