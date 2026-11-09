package com.jhamal.breaktracker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Device-local, best-effort reminders independent of the WebView process.
 * setAndAllowWhileIdle does not require the restricted exact-alarm permission,
 * so Android may defer these reminders during Doze or heavy battery optimization.
 */
public final class BreakAlarmReceiver extends BroadcastReceiver {
    private static final String PREFS = "break_tracker_native";
    private static final String CHANNEL = "breaks";
    private static final String END = "break_end";
    private static final String LABEL = "break_label";
    private static final String ENABLED = "notifications_enabled";
    private static final String ACTION_WARNING = "com.jhamal.breaktracker.BREAK_WARNING";
    private static final String ACTION_DONE = "com.jhamal.breaktracker.BREAK_DONE";

    private static PendingIntent pending(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, BreakAlarmReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancel(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.cancel(pending(context, ACTION_WARNING, 701));
        alarm.cancel(pending(context, ACTION_DONE, 702));
    }

    public static void schedule(Context context, long endEpochMs) {
        cancel(context);
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, true)) return;
        final long now = System.currentTimeMillis();
        if (endEpochMs <= now) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;

        long warningAt = endEpochMs - 180_000L;
        // A newly started short break still deserves a warning; send via ticker when already within 3 min.
        if (warningAt > now) set(alarm, warningAt, pending(context, ACTION_WARNING, 701));
        set(alarm, endEpochMs, pending(context, ACTION_DONE, 702));
    }

    private static void set(AlarmManager alarm, long when, PendingIntent intent) {
        if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, intent);
        else alarm.set(AlarmManager.RTC_WAKEUP, when, intent);
    }

    public static void send(Context context, String title, String body, int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(ENABLED, true)) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return;
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Break reminders", NotificationManager.IMPORTANCE_HIGH));
        PendingIntent open = PendingIntent.getActivity(context, 703,
                new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
        try { manager.notify(id, notification); }
        catch (SecurityException ignored) { /* Android 13+ permission not granted. */ }
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!ACTION_WARNING.equals(action) && !ACTION_DONE.equals(action)) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(ENABLED, true)) return;
        long end = prefs.getLong(END, 0);
        long now = System.currentTimeMillis();
        if (end <= 0) return;
        String label = prefs.getString(LABEL, "Break");
        if (ACTION_WARNING.equals(action) && end > now && end - now <= 210_000L) {
            send(context, "Break almost over", label + " ends in about 3 minutes.", 301);
        }
        if (ACTION_DONE.equals(action) && now >= end - 60_000L) {
            send(context, "Break finished", label + " has ended. Time to return.", 302);
        }
    }
}
