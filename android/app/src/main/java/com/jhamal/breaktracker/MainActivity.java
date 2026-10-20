package com.jhamal.breaktracker;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.app.Notification;

public class MainActivity extends Activity {
 private static final String URL = "https://break-tracker.jhamal16.workers.dev/";
 private WebView web;
 private TextView pipTimer;
 private final Handler handler = new Handler(Looper.getMainLooper());
 private long endMs = 0;
 private String breakName = "";
 private boolean warned = false;
 private final Runnable tick = new Runnable() {
  @Override public void run() {
   long remain = endMs - System.currentTimeMillis();
   if (remain > 0) {
    long secs = (remain + 999) / 1000;
    pipTimer.setText(String.format(java.util.Locale.US, "%02d:%02d\n%s", secs/60, secs%60, breakName));
    if (!warned && remain <= 180000) { warned=true; sendWarning(); }
   } else { pipTimer.setText("00:00\nNo active break"); }
   handler.postDelayed(this,1000);
  }
 };
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  FrameLayout root = new FrameLayout(this);
  web = new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.setWebViewClient(new WebViewClient());
  web.addJavascriptInterface(new Bridge(), "AndroidBreakTracker");
  root.addView(web, new FrameLayout.LayoutParams(-1,-1));
  pipTimer = new TextView(this);
  pipTimer.setTextColor(0xffffffff);pipTimer.setBackgroundColor(0xff111827);
  pipTimer.setTextSize(22);pipTimer.setGravity(Gravity.CENTER);
  pipTimer.setVisibility(View.GONE);
  root.addView(pipTimer,new FrameLayout.LayoutParams(-1,-1));
  setContentView(root);
  if (Build.VERSION.SDK_INT >= 26) {
   ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("breaks","Break reminders",NotificationManager.IMPORTANCE_HIGH));
  }
  if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
  web.loadUrl(URL);
  handler.post(tick);
 }
 public final class Bridge {
  @JavascriptInterface public void startBreak(String label,long endEpochMs) { runOnUiThread(()->{breakName=label;endMs=endEpochMs;warned=false;}); }
  @JavascriptInterface public void stopBreak() { runOnUiThread(()->{endMs=0;breakName="";}); }
 }
 private void sendWarning(){
  Notification n=new Notification.Builder(this,"breaks").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Break almost over").setContentText(breakName+" ends in about 3 minutes").setAutoCancel(true).build();
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(301,n);
 }
 @Override public void onUserLeaveHint(){super.onUserLeaveHint();if(endMs>System.currentTimeMillis() && Build.VERSION.SDK_INT>=26 && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)){
  try{enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build());}catch(Exception ignored){}
 }}
 @Override public void onPictureInPictureModeChanged(boolean inPip,android.content.res.Configuration config){super.onPictureInPictureModeChanged(inPip,config);pipTimer.setVisibility(inPip?View.VISIBLE:View.GONE);web.setVisibility(inPip?View.INVISIBLE:View.VISIBLE);}
 @Override public void onDestroy(){handler.removeCallbacks(tick);web.destroy();super.onDestroy();}
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
