package com.awaisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_A = 1;
    private static final int PICK_IMAGE_B = 2;

    ImageView imageA;
    ImageView imageB;

    Button addButton;
    Button addBButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageA = findViewById(R.id.imageA);
        imageB = findViewById(R.id.imageB);

        addButton = findViewById(R.id.addButton);
        addBButton = findViewById(R.id.addBButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_A);
            }
        });

        addBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_B);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {

            Uri imageUri = data.getData();

            if (requestCode == PICK_IMAGE_A) {
                imageA.setImageURI(imageUri);
            }

            if (requestCode == PICK_IMAGE_B) {
                imageB.setImageURI(imageUri);
            }
        }
    }
}
