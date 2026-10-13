package com.awisogera.votingpostermaker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    ImageView imageA;
    ImageView imageB;

    Button addButton;
    Button addBButton;

    ActivityResultLauncher<Intent> launcherA;
    ActivityResultLauncher<Intent> launcherB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageA = findViewById(R.id.imageA);
        imageB = findViewById(R.id.imageB);

        addButton = findViewById(R.id.addButton);
        addBButton = findViewById(R.id.addBButton);

        // Initialize modern Image Picker for Candidate A
        launcherA = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            imageA.setImageURI(imageUri);
                        }
                    }
                }
        );

        // Initialize modern Image Picker for Candidate B
        launcherB = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            imageB.setImageURI(imageUri);
                        }
                    }
                }
        );

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                launcherA.launch(intent);
            }
        });

        addBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                launcherB.launch(intent);
            }
        });
    }
}
