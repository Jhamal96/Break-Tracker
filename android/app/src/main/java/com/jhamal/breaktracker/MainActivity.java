package com.jhamal.breaktracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Locale;

/**
 * Android WebView wrapper + native picture-in-picture timer.
 *
 * Native PiP does not pass normal touch events to content. Android exposes
 * previous/next design through the system's PiP RemoteAction controls.
 * All 14 styles are selectable from the webpage Settings menu via a native preview gallery.
 */
public class MainActivity extends Activity {
    private static final String URL = "https://tracker.ajdapp.org/";
    private static final String HOST = "tracker.ajdapp.org";
    private static final String IN_APP_SETUP_JS = "(function(){\n  try{\n    const root=document.querySelector('.container');\n    if(root){\n      const cards=[...root.children].filter(e=>e.classList&&e.classList.contains('card'));\n      const heading=e=>e.querySelector('.card-title')?.textContent?.trim()||'';\n      const tracker=cards.find(e=>heading(e)==='Active Break Tracker');\n      const shift=cards.find(e=>heading(e)==='Shift Setup');\n      if(tracker&&shift&&tracker!==shift&&tracker.compareDocumentPosition(shift)&Node.DOCUMENT_POSITION_PRECEDING){\n        root.insertBefore(tracker,shift);\n      }\n    }\n    const panel=document.getElementById('settings-panel');\n    if(panel&&!document.getElementById('native-pip-style-entry')){\n      const button=document.createElement('button');\n      button.type='button';button.id='native-pip-style-entry';button.className='secondary';\n      button.textContent='◉ PiP Styles & Previews';\n      button.addEventListener('click',function(){window.AndroidBreakTracker?.openStylePicker();});\n      panel.appendChild(button);\n    }\n  }catch(e){console.warn('Native layout:',e);}\n})();";
    private static final String CHANNEL_ID = "breaks";
    private static final String PREFS = "break_tracker_native";
    private static final String PREF_STYLE = "pip_style";
    private static final String PREF_END = "break_end";
    private static final String PREF_LABEL = "break_label";
    private static final String PREF_DURATION = "break_duration";
    private static final String ACTION_CHANGE_STYLE = "com.jhamal.breaktracker.CHANGE_PIP_STYLE";
    private static WeakReference<MainActivity> visibleActivity = new WeakReference<>(null);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web;
    private PiPTimerView pipTimer;
    private long endMs;
    private long durationMs = 15 * 60000L;
    private String breakName = "";
    private boolean warned = false;
    private int styleIndex = 0;

    public static final class StyleActionReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_CHANGE_STYLE.equals(intent.getAction())) return;
            final int delta = intent.getIntExtra("direction", 1);
            MainActivity activity = visibleActivity.get();
            if (activity != null) {
                activity.runOnUiThread(() -> activity.changeStyle(delta));
            } else {
                android.content.SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                int style = Math.floorMod(p.getInt(PREF_STYLE, 0) + delta, PiPTimerView.STYLES.length);
                p.edit().putInt(PREF_STYLE, style).apply();
            }
        }
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long remaining = Math.max(0L, endMs - System.currentTimeMillis());
            long seconds = (remaining + 999L) / 1000L;
            pipTimer.setCountdown(seconds, Math.max(1L, durationMs / 1000L), breakName);
            if (endMs > 0 && remaining > 0 && remaining <= 180000L && !warned) {
                warned = true;
                sendWarning();
            }
            if (endMs > 0 && remaining <= 0L) {
                endMs = 0L;
                breakName = "";
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREF_END)
                    .remove(PREF_LABEL).apply();
                updatePipParams();
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        visibleActivity = new WeakReference<>(this);

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        styleIndex = Math.floorMod(prefs.getInt(PREF_STYLE, 0), PiPTimerView.STYLES.length);
        endMs = prefs.getLong(PREF_END, 0L);
        durationMs = prefs.getLong(PREF_DURATION, 15 * 60000L);
        breakName = prefs.getString(PREF_LABEL, "");
        warned = endMs > 0 && endMs - System.currentTimeMillis() <= 180000L;

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return routeUrl(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUrl(Uri.parse(url));
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Uri uri = Uri.parse(url);
                if ("https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())) {
                    view.evaluateJavascript(IN_APP_SETUP_JS, null);
                }
            }
        });
        // Only the trusted production origin should receive this privileged JS bridge.
        web.addJavascriptInterface(new Bridge(), "AndroidBreakTracker");
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        pipTimer = new PiPTimerView(this);
        pipTimer.setStyle(styleIndex);
        pipTimer.setVisibility(View.GONE);
        root.addView(pipTimer, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(
            new NotificationChannel(CHANNEL_ID, "Break reminders", NotificationManager.IMPORTANCE_HIGH));
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        web.loadUrl(URL);
        updatePipParams();
        handler.post(ticker);
    }

    private boolean routeUrl(Uri uri) {
        if ("https".equalsIgnoreCase(uri.getScheme()) &&
            HOST.equalsIgnoreCase(uri.getHost())) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            // Do not permit untrusted pages into the bridged WebView.
        }
        return true;
    }

    private int dp(float dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    public final class Bridge {
        @JavascriptInterface public void openStylePicker() {
            runOnUiThread(() -> showStylePicker());
        }
        @JavascriptInterface public void startBreak(String label, long endEpochMs) {
            runOnUiThread(() -> {
                if (endEpochMs <= System.currentTimeMillis()) return;
                long remaining = endEpochMs - System.currentTimeMillis();
                // 30-minute meal, a custom preset, or the default 15-minute break.
                long minutes = 15;
                String name = label == null ? "" : label;
                if (endMs == endEpochMs && name.equals(breakName)) return;
                if (name.toLowerCase(Locale.US).contains("lunch") ||
                    name.toLowerCase(Locale.US).contains("dinner") ||
                    name.toLowerCase(Locale.US).contains("meal")) minutes = 30;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)m\\)").matcher(name);
                if (m.find()) {
                    try { minutes = Long.parseLong(m.group(1)); } catch (Exception ignored) {}
                }
                durationMs = Math.max(remaining, Math.max(1, minutes) * 60000L);
                endMs = endEpochMs;
                breakName = name;
                warned = false;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putLong(PREF_END, endMs)
                    .putLong(PREF_DURATION, durationMs)
                    .putString(PREF_LABEL, breakName).apply();
                updatePipParams();
            });
        }
        @JavascriptInterface public void stopBreak() {
            runOnUiThread(() -> {
                endMs = 0L; breakName = ""; warned = false;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(PREF_END).remove(PREF_LABEL).apply();
                updatePipParams();
            });
        }
    }

    private void showStylePicker() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(dp(12), dp(4), dp(12), dp(8));
        scroll.addView(choices);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Choose PiP Style")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .create();

        for (int i = 0; i < PiPTimerView.STYLES.length; i++) {
            final int selectedStyle = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(7), dp(8), dp(7));
            row.setBackgroundColor(i == styleIndex ? Color.rgb(224, 238, 255) : Color.rgb(250, 250, 250));

            PiPTimerView preview = new PiPTimerView(this);
            preview.setStyle(i);
            preview.setCountdown(765, 900, "Lunch (30m)");
            row.addView(preview, new LinearLayout.LayoutParams(dp(166), dp(94)));

            TextView name = new TextView(this);
            name.setText(PiPTimerView.STYLES[i] + (i == styleIndex ? "  ✓" : ""));
            name.setTextColor(Color.rgb(15, 23, 42));
            name.setTextSize(13);
            name.setPadding(dp(9), 0, 0, 0);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(name, nameParams);
            row.setContentDescription("Preview " + PiPTimerView.STYLES[i]);
            row.setOnClickListener(v -> {
                saveStyle(selectedStyle);
                dialog.dismiss();
            });
            choices.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            View gap = new View(this);
            choices.addView(gap, new LinearLayout.LayoutParams(1, dp(5)));
        }
        dialog.show();
    }

    private void changeStyle(int direction) {
        saveStyle(Math.floorMod(styleIndex + direction, PiPTimerView.STYLES.length));
    }

    private void saveStyle(int index) {
        styleIndex = Math.floorMod(index, PiPTimerView.STYLES.length);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(PREF_STYLE, styleIndex).apply();
        pipTimer.setStyle(styleIndex);
        updatePipParams();
    }

    private PendingIntent styleActionIntent(int delta) {
        Intent intent = new Intent(this, StyleActionReceiver.class);
        intent.setAction(ACTION_CHANGE_STYLE);
        intent.putExtra("direction", delta);
        return PendingIntent.getBroadcast(this, delta < 0 ? 110 : 111, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PictureInPictureParams buildPipParams() {
        RemoteAction previous = new RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_previous),
            "Previous style", "Previous countdown style", styleActionIntent(-1));
        RemoteAction next = new RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Next style", "Next countdown style", styleActionIntent(1));

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
            .setAspectRatio(new Rational(16, 9))
            .setActions(Arrays.asList(previous, next));
        if (Build.VERSION.SDK_INT >= 31)
            builder.setAutoEnterEnabled(endMs > System.currentTimeMillis());
        return builder.build();
    }

    private void updatePipParams() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return;
        try { setPictureInPictureParams(buildPipParams()); } catch (Exception ignored) {}
    }

    private void sendWarning() {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Break almost over")
            .setContentText(breakName + " ends in about 3 minutes")
            .setAutoCancel(true)
            .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(301, notification);
    }

    @Override public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (endMs > System.currentTimeMillis() &&
            getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            !isInPictureInPictureMode()) {
            try {
                enterPictureInPictureMode(buildPipParams());
            } catch (Exception ignored) {}
        }
    }

    @Override public void onPictureInPictureModeChanged(boolean inPip, Configuration config) {
        super.onPictureInPictureModeChanged(inPip, config);
        pipTimer.setVisibility(inPip ? View.VISIBLE : View.GONE);
        web.setVisibility(inPip ? View.INVISIBLE : View.VISIBLE);
        if (inPip) updatePipParams();
    }

    @Override protected void onResume() {
        super.onResume();
        visibleActivity = new WeakReference<>(this);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (visibleActivity.get() == this) visibleActivity.clear();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
