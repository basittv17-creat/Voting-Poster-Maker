package com.awasigogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PICK_A_IMAGE = 1;
    private static final int PICK_B_IMAGE = 2;

    ImageView imageA;
    ImageView imageB;

    Button addAButton;
    Button addBButton;

    TextView appTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        appTitle = findViewById(R.id.appTitle);

        imageA = findViewById(R.id.imageA);
        imageB = findViewById(R.id.imageB);

        addAButton = findViewById(R.id.addAButton);
        addBButton = findViewById(R.id.addBButton);


        addAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_A_IMAGE);

            }
        });


        addBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_B_IMAGE);

            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);


        if(resultCode == RESULT_OK && data != null){

            Uri imageUri = data.getData();


            if(requestCode == PICK_A_IMAGE){

                imageA.setImageURI(imageUri);

            }


            if(requestCode == PICK_B_IMAGE){

                imageB.setImageURI(imageUri);

            }

        }

    }

}
