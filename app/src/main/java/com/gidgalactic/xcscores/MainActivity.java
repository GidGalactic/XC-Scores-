package com.gidgalactic.xcscores;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
 WebView web;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},55);
  web=new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.addJavascriptInterface(new Bridge(),"Android");
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,android.webkit.WebResourceRequest req){
    if(req.getUrl().toString().startsWith("https://www.runhigh.com/")){
     startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,req.getUrl()));
     return true;
    }
    return false;
   }
  });
  setContentView(web);
  PeriodicWorkRequest periodic=new PeriodicWorkRequest.Builder(ResultsWorker.class,15,TimeUnit.MINUTES).build();
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("xc-results",ExistingPeriodicWorkPolicy.KEEP,periodic);
  web.loadUrl("file:///android_asset/index.html");
 }
 @Override protected void onResume(){super.onResume();if(web!=null)web.evaluateJavascript("window.receiveCached&&window.receiveCached()",null);}
 @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
 public class Bridge {
  @JavascriptInterface public String getCachedPage(int i){
   if(i<0||i>5)return "";
   return getSharedPreferences("race_cache",MODE_PRIVATE).getString("page_"+i,"");
  }
  @JavascriptInterface public void openUpdates(){
   runOnUiThread(()->startActivity(new android.content.Intent(
      android.content.Intent.ACTION_VIEW,
      android.net.Uri.parse("https://github.com/GidGalactic/XC-Scores-/actions/workflows/android-apk.yml"))));
  }
  @JavascriptInterface public void checkNow(){
   new Thread(()->{
    ResultsWorker.downloadAll(getApplicationContext(),true);
    runOnUiThread(()->web.evaluateJavascript("window.receiveCached&&window.receiveCached()",null));
   }).start();
  }
 }
}
