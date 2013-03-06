package me.shenfeng;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
@SuppressLint("SetJavaScriptEnabled")
public class Dictionary extends Activity {

    WebView mWebView;
    Lookup l = null;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        try {
            l = new Lookup(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mWebView = (WebView) findViewById(R.id.b);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        // mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.addJavascriptInterface(l, "LOOK");
        // mWebView.setWebViewClient(new WebViewClient(){
        // public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // super.onPageStarted(view, url, favicon);
        // view.addJavascriptInterface(l, "LOOK");
        // }
        // });
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("D", cm.message() + " " + cm.sourceId() + ":" + cm.lineNumber());
                return true;
            }
        });

        // mWebView.loadUrl("file:///android_asset/index.html");
        mWebView.loadUrl("http://192.168.1.101:8000/index.html");

    }
}
