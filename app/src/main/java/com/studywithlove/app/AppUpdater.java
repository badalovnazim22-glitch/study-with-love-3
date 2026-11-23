package com.studywithlove.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
            "https://github.com/badalovnazim22-glitch/study-with-love-3/releases/latest/download/study-with-love.apk";
    private static final String PREFS = "study_app_updates";
    private static final String PENDING_APK = "pending_apk";
    private static final int MAX_APK_BYTES = 100 * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppUpdater() {}

    public static void checkAndOffer(Activity activity) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMessage("Проверяем наличие обновлений…");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            String latestVersion = null;
            String notes = "";
            String error = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(RELEASE_API).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "Study-with-Love-Updater");
                int status = connection.getResponseCode();
                if (status != 200) throw new Exception("HTTP " + status);
                String json = readText(connection.getInputStream(), 1024 * 1024);
                JSONObject release = new JSONObject(json);
                String tag = release.optString("tag_name", "").trim();
                latestVersion = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
                notes = release.optString("body", "").trim();
                if (latestVersion.isEmpty()) throw new Exception("Не удалось определить версию.");
            } catch (Exception e) {
                error = "Не удалось проверить обновления. Попробуй позже.";
            } finally {
                if (connection != null) connection.disconnect();
            }

            final String foundVersion = latestVersion;
            final String releaseNotes = notes;
            final String failure = error;
            MAIN.post(() -> {
                if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
                progress.dismiss();
                if (failure != null) {
                    Toast.makeText(activity, failure, Toast.LENGTH_LONG).show();
                    return;
                }
                String current = installedVersion(activity);
                if (compareVersions(foundVersion, current) <= 0) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Обновлений пока нет")
                            .setMessage("Установлена последняя версия " + current + " ♡")
                            .setPositiveButton("Хорошо", null)
                            .show();
                    return;
                }
                String message = releaseNotes.isEmpty()
                        ? "Доступна новая версия приложения."
                        : releaseNotes;
                new AlertDialog.Builder(activity)
                        .setTitle("Доступно обновление " + foundVersion)
                        .setMessage(message)
                        .setNegativeButton("Позже", null)
                        .setPositiveButton("Скачать и установить", (dialog, which) -> download(activity))
                        .show();
            });
        }, "app-update-check").start();
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
