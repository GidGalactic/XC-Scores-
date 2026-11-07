package com.gidgalactic.xcscores;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Html;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultsWorker extends Worker {
 static final String[] NAMES={"Girls A","Girls AA","Girls AAA","Boys A","Boys AA","Boys AAA"};
 static final String[] SUFFIX={"AB","AC","AD","AE","AF","AG"};
 public ResultsWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
 @NonNull @Override public Result doWork(){downloadAll(getApplicationContext(),false);return Result.success();}
 static void downloadAll(Context context,boolean manual){
  SharedPreferences prefs=context.getSharedPreferences("race_cache",Context.MODE_PRIVATE);
  for(int i=0;i<NAMES.length;i++){
   try{
    HttpURLConnection cn=(HttpURLConnection)new URL("https://www.runhigh.com/2026RESULTS/R110726"+SUFFIX[i]+".html").openConnection();
    cn.setConnectTimeout(12000);cn.setReadTimeout(15000);
    cn.setRequestProperty("User-Agent","Mozilla/5.0 (compatible; XC-Scores-Android/1.0)");
    try{
     if(cn.getResponseCode()!=200)continue;
     ByteArrayOutputStream out=new ByteArrayOutputStream();
     try(InputStream in=cn.getInputStream()){
      byte[] b=new byte[8192];int n;
      while((n=in.read(b))!=-1&&out.size()<1500000)out.write(b,0,n);
     }
     String html=out.toString(StandardCharsets.UTF_8.name());
     // RunHigh places its aligned result rows inside PRE tags. Stripping tags with
     // a single space loses the line structure, so avoid row-specific regex checks here.
     String normalized=Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY)
       .toString().replace((char)160,' ');
     String upper=normalized.toUpperCase(java.util.Locale.ROOT);
     // Accept authentic results headers regardless of spacing, split layout or the
     // precise number of athletes. JavaScript will validate parsed finishers too.
     if(!(upper.contains("PLACE") && upper.contains("FINISHER") && upper.length()>800))continue;
     boolean posted=prefs.getBoolean("posted_"+i,false);
     prefs.edit().putString("page_"+i,html).putBoolean("posted_"+i,true).apply();
     if(!posted)notifyPosted(context,i);
    }finally{cn.disconnect();}
   }catch(Exception e){android.util.Log.w("XC Scores","Could not check "+NAMES[i],e);}
  }
  prefs.edit().putLong("last_check",System.currentTimeMillis()).apply();
  UpdateChecker.checkInBackground(context);
 }
 static void notifyPosted(Context c,int i){
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("race_updates","Race result updates",NotificationManager.IMPORTANCE_DEFAULT));
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  Intent intent=new Intent(c,MainActivity.class);
  intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent pi=PendingIntent.getActivity(c,i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(c,"race_updates").setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle(NAMES[i]+" results posted").setContentText("Open XC Scores to view new rankings.")
   .setContentIntent(pi).setAutoCancel(true).build();
  nm.notify(200+i,n);
 }
}
