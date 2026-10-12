package com.awisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PosterView posterView = new PosterView(this);
        setContentView(posterView);
    }
}
