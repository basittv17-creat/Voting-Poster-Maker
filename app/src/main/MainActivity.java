package com.awisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText("Voting Poster Maker");
        text.setTextSize(28);
        text.setTextColor(Color.WHITE);
        text.setGravity(17);
        text.setBackgroundColor(Color.DKGRAY);

        setContentView(text);
    }
}
