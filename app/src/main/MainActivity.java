package com.awisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private PosterView posterView;
    private File savedPosterFile;
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

        Button addText = new Button(this);
        addText.setText("Add Text");

        EditText textInput = new EditText(this);
        textInput.setHint("Enter poster text");

        Button increaseSize = new Button(this);
        increaseSize.setText("Increase Size");

        Button decreaseSize = new Button(this);
        decreaseSize.setText("Decrease Size");

        Button redColor = new Button(this);
        redColor.setText("Red Text");

        Button blueColor = new Button(this);
        blueColor.setText("Blue Text");

        Button greenColor = new Button(this);
        greenColor.setText("Green Text");

        Button whiteColor = new Button(this);
        whiteColor.setText("White Text");

        Button yellowColor = new Button(this);
        yellowColor.setText("Yellow Text");

        Button leftAlign = new Button(this);
        leftAlign.setText("Left");

        Button centerAlign = new Button(this);
        centerAlign.setText("Center");

        Button rightAlign = new Button(this);
        rightAlign.setText("Right");

        Button normalFont = new Button(this);
        normalFont.setText("Normal");

        Button boldFont = new Button(this);
        boldFont.setText("Bold");

        Button italicFont = new Button(this);
        italicFont.setText("Italic");

        Button shadowOn = new Button(this);
        shadowOn.setText("Shadow ON");

        Button shadowOff = new Button(this);
        shadowOff.setText("Shadow OFF");

        Button strokeOn = new Button(this);
        strokeOn.setText("Stroke ON");

        Button strokeOff = new Button(this);
        strokeOff.setText("Stroke OFF");

        Button whiteOutline = new Button(this);
        whiteOutline.setText("White Outline");

        Button blackOutline = new Button(this);
        blackOutline.setText("Black Outline");

        Button outlineOff = new Button(this);
        outlineOff.setText("Outline OFF");

        Button deleteText = new Button(this);
        deleteText.setText("Delete Text");

        Button savePoster = new Button(this);
        savePoster.setText("Save Poster");

        Button sharePoster = new Button(this);
        sharePoster.setText("Share Poster");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20,20,20,20);

        TextView photoTitle = new TextView(this);
        photoTitle.setText("PHOTO CONTROLS");
        photoTitle.setTextSize(18);

        TextView textTitle = new TextView(this);
        textTitle.setText("TEXT CONTROLS");
        textTitle.setTextSize(18);

        TextView styleTitle = new TextView(this);
        styleTitle.setText("STYLE CONTROLS");
        styleTitle.setTextSize(18);

        TextView exportTitle = new TextView(this);
        exportTitle.setText("EXPORT CONTROLS");
        exportTitle.setTextSize(18);

        layout.addView(photoTitle);

        layout.addView(photoA);
        layout.addView(photoB);
        layout.addView(textTitle);

        layout.addView(textInput);
        layout.addView(addText);
        layout.addView(increaseSize);
        layout.addView(decreaseSize);
        layout.addView(redColor);
        layout.addView(blueColor);
        layout.addView(greenColor);
        layout.addView(whiteColor);
        layout.addView(yellowColor);
        layout.addView(leftAlign);
        layout.addView(centerAlign);
        layout.addView(rightAlign);
        layout.addView(styleTitle);

        layout.addView(normalFont);
        layout.addView(boldFont);
        layout.addView(italicFont);
        layout.addView(shadowOn);
        layout.addView(shadowOff);
        layout.addView(strokeOn);
        layout.addView(strokeOff);
        layout.addView(whiteOutline);
        layout.addView(blackOutline);
        layout.addView(outlineOff);
        layout.addView(deleteText);
        layout.addView(exportTitle);

        layout.addView(savePoster);
        layout.addView(sharePoster);
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

        addText.setOnClickListener(v -> {
            posterView.addText(textInput.getText().toString());
        });

        increaseSize.setOnClickListener(v -> {
            posterView.increaseTextSize();
        });

        decreaseSize.setOnClickListener(v -> {
            posterView.decreaseTextSize();
        });

        redColor.setOnClickListener(v -> {
            posterView.setTextColor(0xffff0000);
        });

        blueColor.setOnClickListener(v -> {
            posterView.setTextColor(0xff0000ff);
        });

        greenColor.setOnClickListener(v -> {
            posterView.setTextColor(0xff00ff00);
        });

        whiteColor.setOnClickListener(v -> {
            posterView.setWhiteText();
        });

        yellowColor.setOnClickListener(v -> {
            posterView.setYellowText();
        });

        leftAlign.setOnClickListener(v -> {
            posterView.setLeftAlign();
        });

        centerAlign.setOnClickListener(v -> {
            posterView.setCenterAlign();
        });

        rightAlign.setOnClickListener(v -> {
            posterView.setRightAlign();
        });

        normalFont.setOnClickListener(v -> {
            posterView.setNormalText();
        });

        boldFont.setOnClickListener(v -> {
            posterView.setBoldText();
        });

        italicFont.setOnClickListener(v -> {
            posterView.setItalicText();
        });

        shadowOn.setOnClickListener(v -> {
            posterView.enableTextShadow();
        });

        shadowOff.setOnClickListener(v -> {
            posterView.disableTextShadow();
        });

        strokeOn.setOnClickListener(v -> {
            posterView.enableTextStroke();
        });

        strokeOff.setOnClickListener(v -> {
            posterView.disableTextStroke();
        });

        whiteOutline.setOnClickListener(v -> {
            posterView.setWhiteOutline();
        });

        blackOutline.setOnClickListener(v -> {
            posterView.setBlackOutline();
        });

        outlineOff.setOnClickListener(v -> {
            posterView.disableOutline();
        });

        deleteText.setOnClickListener(v -> {
            posterView.deleteActiveText();
        });


        sharePoster.setOnClickListener(v -> {

            if (savedPosterFile != null && savedPosterFile.exists()) {

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");

                shareIntent.putExtra(
                        Intent.EXTRA_STREAM,
                        Uri.fromFile(savedPosterFile)
                );

                startActivity(
                        Intent.createChooser(
                                shareIntent,
                                "Share Poster"
                        )
                );

            } else {

                Toast.makeText(
                        this,
                        "Save Poster First",
                        Toast.LENGTH_SHORT
                ).show();

            }

        });

        savePoster.setOnClickListener(v -> {

            Bitmap bitmap = posterView.savePosterBitmap();

            File folder = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
            );

            savedPosterFile = new File(folder, "VotingPoster.png");

            File file = savedPosterFile;

            try {

                FileOutputStream out = new FileOutputStream(file);

                bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        out
                );

                out.flush();
                out.close();

                Toast.makeText(
                        this,
                        "Poster Saved",
                        Toast.LENGTH_SHORT
                ).show();

            } catch(Exception e) {

                Toast.makeText(
                        this,
                        "Save Failed",
                        Toast.LENGTH_SHORT
                ).show();

            }

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
