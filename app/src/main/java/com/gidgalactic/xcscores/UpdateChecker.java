package com.gidgalactic.xcscores;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {
 private static final String API="https://api.github.com/repos/GidGalactic/XC-Scores-/releases/latest";
 private static final String RELEASES="https://github.com/GidGalactic/XC-Scores-/releases";
 private static final long CHECK_INTERVAL=6L*60*60*1000;
 private UpdateChecker(){}

 public static void check(Activity activity,boolean manual){
  SharedPreferences prefs=activity.getSharedPreferences("updates",Context.MODE_PRIVATE);
  if(!manual && System.currentTimeMillis()-prefs.getLong("last_checked",0)<CHECK_INTERVAL)return;
  new Thread(()->{
   try{
    Latest latest=fetchLatest();
    prefs.edit().putLong("last_checked",System.currentTimeMillis()).apply();
    int installed=installedVersionCode(activity);
    boolean newer=latest.versionCode>installed;
    activity.runOnUiThread(()->{
     if(newer){
      new AlertDialog.Builder(activity).setTitle("XC Scores update available")
       .setMessage("Version "+latest.versionName+" is available. You have version "+installedVersionName(activity)+". Download and install the update?")
       .setNegativeButton("Later",(d,w)->{}).setPositiveButton("Download update",(d,w)->open(activity,latest.url)).show();
     }else if(manual){
      new AlertDialog.Builder(activity).setTitle("XC Scores is up to date")
       .setMessage("Installed version: "+installedVersionName(activity)+". Latest public release: "+latest.versionName+".")
       .setPositiveButton("OK",(d,w)->{}).show();
     }
    });
   }catch(Exception e){
    if(manual)activity.runOnUiThread(()->new AlertDialog.Builder(activity)
     .setTitle("Could not check for updates")
     .setMessage("There may not be a public release yet, or the network is unavailable. You can also open the public releases page.")
     .setNegativeButton("Cancel",(d,w)->{})
     .setPositiveButton("Open releases",(d,w)->open(activity,RELEASES)).show());
   }
  }).start();
 }

 public static void checkInBackground(Context context){
  SharedPreferences prefs=context.getSharedPreferences("updates",Context.MODE_PRIVATE);
  if(System.currentTimeMillis()-prefs.getLong("last_checked",0)<CHECK_INTERVAL)return;
  try{
   Latest latest=fetchLatest();
   prefs.edit().putLong("last_checked",System.currentTimeMillis()).apply();
   if(latest.versionCode<=installedVersionCode(context))return;
   String previouslyNotified=prefs.getString("notified","");
   if(previouslyNotified.equals(latest.versionName))return;
   prefs.edit().putString("notified",latest.versionName).apply();
   NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
   if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(
    new NotificationChannel("app_updates","XC Scores app updates",NotificationManager.IMPORTANCE_DEFAULT));
   if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
   Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(latest.url));
   PendingIntent pi=PendingIntent.getActivity(context,445,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder b=Build.VERSION.SDK_INT>=26?
    new Notification.Builder(context,"app_updates"):new Notification.Builder(context);
   nm.notify(445,b.setSmallIcon(android.R.drawable.stat_sys_download_done)
    .setContentTitle("XC Scores "+latest.versionName+" available")
    .setContentText("Tap to download and install the update")
    .setContentIntent(pi).setAutoCancel(true).build());
  }catch(Exception e){android.util.Log.w("XC Scores","Update check failed",e);}
 }

 private static Latest fetchLatest() throws Exception {
  HttpURLConnection connection=(HttpURLConnection)new URL(API).openConnection();
  connection.setConnectTimeout(12000);connection.setReadTimeout(12000);
  connection.setRequestProperty("Accept","application/vnd.github+json");
  connection.setRequestProperty("User-Agent","XC-Scores-Android");
  try{
   if(connection.getResponseCode()!=200)throw new Exception("Release unavailable");
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   try(InputStream in=connection.getInputStream()){
    byte[] buf=new byte[4096];int n;
    while((n=in.read(buf))!=-1&&bytes.size()<500000)bytes.write(buf,0,n);
   }
   JSONObject release=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
   String tag=release.optString("tag_name");
   String version=tag.replaceFirst("^[vV]","");
   int code=versionCodeFromTag(version);
   if(code<0)throw new Exception("Unrecognized version tag: "+tag);
   String link=release.optString("html_url",RELEASES);
   JSONArray assets=release.optJSONArray("assets");
   if(assets!=null){
    for(int i=0;i<assets.length();i++){
     JSONObject asset=assets.getJSONObject(i);
     String name=asset.optString("name").toLowerCase(java.util.Locale.ROOT);
     if(name.endsWith(".apk")){link=asset.optString("browser_download_url",link);break;}
    }
   }
   return new Latest(code,version,link);
  }finally{connection.disconnect();}
 }
 // Release tags v1.3, v1.4 etc. must correspond to build.gradle versionCode,
 // where v1.2 -> 3 and v1.3 -> 4. Use numeric comparison, not string comparison.
 private static int versionCodeFromTag(String v){
  String[] p=v.split("\\.");
  if(p.length!=2)return -1;
  try{
   int major=Integer.parseInt(p[0]),minor=Integer.parseInt(p[1]);
   if(major!=1 || minor<0 || minor>1000)return -1;
   return minor+1;
  }catch(NumberFormatException e){return -1;}
 }
 private static int installedVersionCode(Context c){
  try{
   PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
   return Build.VERSION.SDK_INT>=28?(int)p.getLongVersionCode():p.versionCode;
  }catch(Exception e){return 0;}
 }
 private static String installedVersionName(Context c){
  try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}
  catch(Exception e){return "unknown";}
 }
 private static void open(Context c,String uri){
  c.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 }
 private static final class Latest {
  final int versionCode;final String versionName;final String url;
  Latest(int code,String name,String link){versionCode=code;versionName=name;url=link;}
 }
}
