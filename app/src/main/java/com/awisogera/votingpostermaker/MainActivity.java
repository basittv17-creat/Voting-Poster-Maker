package com.awisogera.votingpostermaker;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int FILE_CHOOSER_REQUEST = 100;
    private android.webkit.ValueCallback<Uri[]> uploadMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    android.webkit.ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams params) {

                uploadMessage = filePathCallback;
                Intent intent = params.createIntent();
                startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST && uploadMessage != null) {

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }

            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
