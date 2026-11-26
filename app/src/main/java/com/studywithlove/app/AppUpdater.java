package com.studywithlove.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class AppUpdater {
    private static final String RELEASE_API =
            "https://api.github.com/repos/badalovnazim22-glitch/study-with-love-3/releases/latest";
    private static final String APK_URL =
            "https://github.com/badalovnazim22-glitch/study-with-love-3/releases/latest/download/app-debug.apk";
    private static final String PREFS = "study_app_updates";
    private static final String PENDING_APK = "pending_apk";
    private static final String LAST_AUTO_CHECK = "last_auto_check";
    private static final String LAST_OFFERED_VERSION = "last_offered_version";
    private static volatile String latestReleaseNotes = "";
    private static final int MAX_APK_BYTES = 100 * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final String UPDATE_CHANNEL = "app_updates";
    private static final String UPDATE_ACTION = "com.studywithlove.app.CHECK_UPDATES";
    private static final String LAST_NOTIFIED_VERSION = "last_notified_version";

    public static void scheduleAutomaticChecks(android.content.Context context) {
        try {
            android.app.AlarmManager alarm=(android.app.AlarmManager)context.getSystemService(android.content.Context.ALARM_SERVICE);
            if(alarm==null)return;
            Intent intent=new Intent(context,UpdateCheckReceiver.class).setAction(UPDATE_ACTION);
            int flags=PendingIntent.FLAG_UPDATE_CURRENT;
            if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending=PendingIntent.getBroadcast(context,7732,intent,flags);
            alarm.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+android.app.AlarmManager.INTERVAL_HALF_DAY,android.app.AlarmManager.INTERVAL_HALF_DAY,pending);
        } catch(Exception ignored) {}
    }

    public static void checkAndNotify(android.content.Context context, Runnable completion) {
        if(context==null){if(completion!=null)completion.run();return;}
        new Thread(()->{
            try {
                String latest=fetchLatestVersion();
                android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
                String current=info.versionName==null?"0":info.versionName;
                if(compareVersions(latest,current)<=0)return;
                android.content.SharedPreferences p=context.getSharedPreferences(PREFS,android.content.Context.MODE_PRIVATE);
                if(latest.equals(p.getString(LAST_NOTIFIED_VERSION,"")))return;
                if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
                android.app.NotificationManager manager=(android.app.NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                if(manager==null)return;
                if(Build.VERSION.SDK_INT>=26){
                    android.app.NotificationChannel channel=new android.app.NotificationChannel(UPDATE_CHANNEL,"Обновления приложения",android.app.NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("Сообщения о новых версиях Study with Love");
                    manager.createNotificationChannel(channel);
                }
                Intent open=new Intent(context,MainActivity.class);
                open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
                open.putExtra("show_update_prompt",true);
                int piFlags=PendingIntent.FLAG_UPDATE_CURRENT;
                if(Build.VERSION.SDK_INT>=23)piFlags|=PendingIntent.FLAG_IMMUTABLE;
                PendingIntent tap=PendingIntent.getActivity(context,7733,open,piFlags);
                String note=latestReleaseNotes==null?"":latestReleaseNotes.trim();
                if(note.length()>120)note=note.substring(0,117)+"…";
                if(note.isEmpty())note="Установи последнюю версию с новыми улучшениями.";
                androidx.core.app.NotificationCompat.Builder builder=new androidx.core.app.NotificationCompat.Builder(context,UPDATE_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Доступна новая версия Study with Love")
                    .setContentText("Версия "+latest+" · "+note)
                    .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText("Версия "+latest+". "+note))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(tap);
                manager.notify(7732,builder.build());
                p.edit().putString(LAST_NOTIFIED_VERSION,latest).apply();
            } catch(Exception ignored) {} finally {
                if(completion!=null)completion.run();
            }
        },"app-update-notification-check").start();
    }

    private AppUpdater() {}

    public static void checkAndOffer(Activity activity) { check(activity, true); }

    public static void checkAutomatically(Activity activity) {
        if(activity==null || activity.isFinishing()) return;
        android.content.SharedPreferences p=activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE);
        long last=p.getLong(LAST_AUTO_CHECK,0L);
        if(System.currentTimeMillis()-last<12L*60L*60L*1000L)return;
        check(activity,false);
    }

    private static void check(Activity activity, boolean manual) {
        if(activity==null || activity.isFinishing())return;
        ProgressDialog progress=null;
        if(manual){progress=new ProgressDialog(activity);progress.setMessage("Проверяем наличие обновлений…");progress.setCancelable(false);progress.show();}
        final ProgressDialog shownProgress=progress;
        new Thread(() -> {
            String latestVersion=null;String error=null;
            try {
                latestVersion=fetchLatestVersion();
                if(latestVersion==null || latestVersion.trim().isEmpty())throw new Exception("Latest release version is empty");
            } catch(Exception e){error="Не удалось проверить обновления. Проверь подключение к интернету и попробуй ещё раз.";}
            final String foundVersion=latestVersion;final String failure=error;
            if(failure==null && !manual) activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).edit().putLong(LAST_AUTO_CHECK,System.currentTimeMillis()).apply();
            MAIN.post(()->{
                if(activity.isFinishing() || (Build.VERSION.SDK_INT>=17 && activity.isDestroyed()))return;
                if(shownProgress!=null)shownProgress.dismiss();
                if(failure!=null){if(manual)Toast.makeText(activity,failure,Toast.LENGTH_LONG).show();return;}
                String current=installedVersion(activity);
                if(compareVersions(foundVersion,current)<=0){
                    if(manual)new AlertDialog.Builder(activity).setTitle("Обновлений пока нет").setMessage("Установлена последняя версия "+current+" ♡").setPositiveButton("Хорошо",null).show();
                    return;
                }
                android.content.SharedPreferences p=activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE);
                if(!manual && foundVersion.equals(p.getString(LAST_OFFERED_VERSION,"")))return;
                p.edit().putString(LAST_OFFERED_VERSION,foundVersion).apply();
                showUpdateScreen(activity,foundVersion,manual);
            });
        },"app-update-check").start();
    }

    private static void showUpdateScreen(Activity activity,String version,boolean manual){
        int pad=(int)(activity.getResources().getDisplayMetrics().density*20);
        android.widget.LinearLayout panel=new android.widget.LinearLayout(activity);
        panel.setOrientation(android.widget.LinearLayout.VERTICAL);panel.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        panel.setPadding(pad,pad,pad,pad);
        panel.setBackground(roundRect(0xFFFFF9F7,28,0xFFF0DCE4,activity));
        android.widget.TextView icon=new android.widget.TextView(activity);icon.setText("✦");icon.setTextSize(34);icon.setTextColor(0xFFC97891);icon.setGravity(android.view.Gravity.CENTER);
        icon.setBackground(roundRect(0xFFFCEEF2,28,0,activity));panel.addView(icon,new android.widget.LinearLayout.LayoutParams(pad*3,pad*3));
        android.widget.TextView title=new android.widget.TextView(activity);title.setText("Для тебя есть обновление");title.setTextColor(0xFF3F3439);title.setTextSize(21);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);title.setGravity(android.view.Gravity.CENTER);title.setPadding(0,pad,0,pad/3);panel.addView(title,new android.widget.LinearLayout.LayoutParams(-1,-2));
        android.widget.TextView versionLabel=new android.widget.TextView(activity);versionLabel.setText("STUDY WITH LOVE  ·  ВЕРСИЯ "+version);versionLabel.setTextColor(0xFFC97891);versionLabel.setTextSize(12);versionLabel.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);versionLabel.setGravity(android.view.Gravity.CENTER);panel.addView(versionLabel,new android.widget.LinearLayout.LayoutParams(-1,-2));
        android.widget.TextView notes=new android.widget.TextView(activity);String note=latestReleaseNotes==null?"":latestReleaseNotes.trim();if(note.isEmpty())note="Новые улучшения, более удобные функции и исправления для комфортной учёбы.";notes.setText(note);notes.setTextColor(0xFF75666D);notes.setTextSize(14);notes.setGravity(android.view.Gravity.CENTER);notes.setPadding(0,pad,0,pad/2);panel.addView(notes,new android.widget.LinearLayout.LayoutParams(-1,-2));
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(activity).setView(panel).setNegativeButton("Позже",null).setPositiveButton("Обновить",null).create();
        dialog.setOnShowListener(v->{android.widget.Button positive=dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);positive.setTextColor(0xFFC97891);positive.setOnClickListener(b->{dialog.dismiss();download(activity);});});
        dialog.show();
    }

    private static android.graphics.drawable.GradientDrawable roundRect(int color,int radius,int stroke,Activity activity){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(radius*activity.getResources().getDisplayMetrics().density);if(stroke!=0)d.setStroke((int)(activity.getResources().getDisplayMetrics().density),stroke);return d;
    }

    private static String fetchLatestVersion() throws Exception {
        HttpURLConnection api = null;
        try {
            String uncachedApi = RELEASE_API + "?_=" + System.currentTimeMillis();
            api = (HttpURLConnection) new URL(uncachedApi).openConnection();
            api.setRequestMethod("GET");
            api.setConnectTimeout(10000);
            api.setReadTimeout(12000);
            api.setRequestProperty("Accept", "application/vnd.github+json");
            api.setRequestProperty("User-Agent", "Study-with-Love-Updater");
            api.setRequestProperty("Cache-Control", "no-cache");
            int status = api.getResponseCode();
            if (status == 200) {
                JSONObject release = new JSONObject(readText(api.getInputStream(), 1024 * 1024));
                String tag = release.optString("tag_name", "").trim();
                latestReleaseNotes=release.optString("body","").trim();
                if (!tag.isEmpty()) return stripVersionPrefix(tag);
            }
        } catch (Exception ignored) {
            // Continue with the public GitHub release redirect below.
        } finally {
            if (api != null) api.disconnect();
        }

        HttpURLConnection web = null;
        try {
            URL latestUrl = new URL("https://github.com/badalovnazim22-glitch/study-with-love-3/releases/latest");
            web = (HttpURLConnection) latestUrl.openConnection();
            web.setInstanceFollowRedirects(false);
            web.setConnectTimeout(10000);
            web.setReadTimeout(12000);
            web.setRequestMethod("GET");
            web.setRequestProperty("User-Agent", "Study-with-Love-Updater");
            int status = web.getResponseCode();
            String location = web.getHeaderField("Location");
            if (location == null || location.trim().isEmpty()) {
                if (status >= 200 && status < 300) {
                    location = web.getURL().toString();
                } else {
                    throw new Exception("GitHub release lookup HTTP " + status);
                }
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("/releases/tag/([^/?#]+)")
                    .matcher(location);
            if (!matcher.find()) throw new Exception("GitHub latest release tag not found");
            return stripVersionPrefix(java.net.URLDecoder.decode(matcher.group(1), "UTF-8"));
        } finally {
            if (web != null) web.disconnect();
        }
    }

    private static String stripVersionPrefix(String tag) {
        String value = tag == null ? "" : tag.trim();
        return value.startsWith("v") || value.startsWith("V") ? value.substring(1) : value;
    }

    private static void download(Activity activity) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setTitle("Обновление Study with Love");
        progress.setMessage("Скачиваем APK…");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            HttpURLConnection connection = null;
            File temporary = null;
            String error = null;
            try {
                connection = (HttpURLConnection) new URL(APK_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Study-with-Love-Updater");
                int status = connection.getResponseCode();
                if (status != 200) throw new Exception("HTTP " + status);
                int total = connection.getContentLength();
                if (total > MAX_APK_BYTES) throw new Exception("Файл обновления слишком большой.");
                File base = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (base == null) throw new Exception("Нет доступа к папке загрузок.");
                File dir = new File(base, "updates");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("Не удалось создать папку обновления.");
                temporary = new File(dir, "study-with-love-update.apk.part");
                File target = new File(dir, "study-with-love-update.apk");
                long copied = 0;
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        copied += count;
                        if (copied > MAX_APK_BYTES) throw new Exception("Файл обновления слишком большой.");
                        out.write(buffer, 0, count);
                        if (total > 0) {
                            int value = (int) Math.min(100, copied * 100 / total);
                            MAIN.post(() -> progress.setProgress(value));
                        }
                    }
                    out.getFD().sync();
                }
                if (copied < 100000 || !temporary.renameTo(target)) {
                    throw new Exception("Не удалось сохранить APK.");
                }
                activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                        .edit().putString(PENDING_APK, target.getAbsolutePath()).apply();
                MAIN.post(() -> {
                    if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
                    progress.dismiss();
                    installPending(activity);
                });
            } catch (Exception e) {
                error = "Не удалось скачать обновление. Проверь интернет и попробуй ещё раз.";
            } finally {
                if (connection != null) connection.disconnect();
                if (temporary != null && temporary.exists()) temporary.delete();
            }
            if (error != null) {
                String failure = error;
                MAIN.post(() -> {
                    if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
                    progress.dismiss();
                    Toast.makeText(activity, failure, Toast.LENGTH_LONG).show();
                });
            }
        }, "app-update-download").start();
    }

    public static void resumePendingInstall(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        String path = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                .getString(PENDING_APK, "");
        if (path.isEmpty()) return;
        File apk = new File(path);
        if (!apk.isFile() || apk.length() < 100000) {
            activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                    .edit().remove(PENDING_APK).apply();
            return;
        }
        if (Build.VERSION.SDK_INT >= 26 &&
                !activity.getPackageManager().canRequestPackageInstalls()) return;
        installFile(activity, apk);
    }

    private static void installPending(Activity activity) {
        String path = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                .getString(PENDING_APK, "");
        if (path.isEmpty()) return;
        File apk = new File(path);
        if (Build.VERSION.SDK_INT >= 26 &&
                !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Разрешение на установку")
                    .setMessage("Android попросит разрешить Study with Love устанавливать обновления. Включи разрешение, затем вернись в приложение.")
                    .setNegativeButton("Позже", null)
                    .setPositiveButton("Открыть настройки", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    }).show();
            return;
        }
        installFile(activity, apk);
    }

    private static void installFile(Activity activity, File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
            activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                    .edit().remove(PENDING_APK).apply();
        } catch (Exception e) {
            Toast.makeText(activity, "Не удалось открыть установщик Android.", Toast.LENGTH_LONG).show();
        }
    }

    private static String installedVersion(Activity activity) {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return info.versionName == null ? "0" : info.versionName;
        } catch (Exception ignored) {
            return "0";
        }
    }

    private static int compareVersions(String first, String second) {
        String[] a = first.split("\\.");
        String[] b = second.split("\\.");
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            int x = i < a.length ? parsePart(a[i]) : 0;
            int y = i < b.length ? parsePart(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int parsePart(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        try { return digits.isEmpty() ? 0 : Integer.parseInt(digits); }
        catch (Exception ignored) { return 0; }
    }

    private static String readText(InputStream stream, int maxBytes) throws Exception {
        try (InputStream in = stream;
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > maxBytes) throw new Exception("Ответ слишком большой.");
                out.write(buffer, 0, count);
            }
            return out.toString("UTF-8");
        }
    }
}
