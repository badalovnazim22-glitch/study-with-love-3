package com.studywithlove.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private final int BG = 0xFFFFFBFC, PINK = 0xFFD96C91, PALE = 0xFFFFF0F4, DARK = 0xFF553B46, MUTED = 0xFF927D86;
    private LinearLayout root;
    private String currentScreen = "home";
    private SharedPreferences prefs;
    private String selectedTopic = null, selectedChapter = null;
    private TextView topicProgressLabel;
    private ProgressBar topicProgressBar;
    private Button[] progressButtons = new Button[4];
    private final Handler timerHandler = new Handler();
    private Runnable timerTick;
    private TextView timerDisplay, timerStatus;
    private Button timerStartButton, timerPauseButton;
    private EditText customMinutesInput;
    private long appUsageStartedElapsed = 0L;
    private int selectedStudyMinutes = 25, selectedBreakMinutes = 5;
    private long timerEndElapsed = 0L, timerRemainingMs = 0L;
    private boolean timerRunning = false, timerIsBreak = false;
    private long timerSessionStarted = 0L;
    private static final String PREFS = "study_progress";
    private static final String[] DAY_NAMES = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    private final List<String> reminderTimes = new ArrayList<>();
    private final List<CheckBox> dayChecks = new ArrayList<>();
    private final Map<String, List<String>> chapters = new LinkedHashMap<>();
    private final String[] messages = {"Ты большая умничка! ❤️", "Я верю в тебя, у тебя всё получится! 🌸", "Не сдавайся: маленькие шаги приводят к большим результатам! 💗", "Ты справишься! И не забывай улыбаться ❤️"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setBackgroundColor(BG);
        getWindow().setStatusBarColor(0xFFFFF3F6);
        getWindow().setNavigationBarColor(0xFFFFF3F6);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // Migrate the last recorded study date from older app versions into the calendar history.
        String previousStudyDate = prefs.getString("last_study_date", "");
        if (!previousStudyDate.isEmpty()) {
            Set<String> history = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
            history.add(previousStudyDate);
            prefs.edit().putStringSet("study_days", history).apply();
        }
        loadChapters();
        scheduleReminders(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        showHome();
    }

    private void loadChapters() {
        add("1️⃣ Натуральные числа и действия с ними", "1.1. Натуральные числа", "1.2. Сравнение и упорядочивание", "1.3. Округление натуральных чисел", "1.4. Сложение и вычитание натуральных чисел", "1.5. Квадрат и куб натурального числа", "1.6. Умножение и деление натуральных чисел", "1.7. Числовые выражения", "1.8. Делители и кратные числа");
        add("2️⃣ Обыкновенные дроби", "2.1. Правильные и неправильные дроби", "2.2. Сравнение и упорядочивание", "2.3. Сложение и вычитание дробей с разными знаменателями", "2.4. Сложение смешанных чисел", "2.5. Вычитание смешанных чисел", "2.6. Умножение обыкновенных дробей", "2.7. Умножение смешанных чисел", "2.8. Деление обыкновенных дробей", "2.9. Деление смешанных чисел", "2.10. Нахождение части числа и числа по его части");
        add("3️⃣ Десятичные дроби", "3.1. Десятичные дроби", "3.2. Сравнение и упорядочивание", "3.3. Округление десятичных дробей", "3.4. Перевод обыкновенной дроби в десятичную и наоборот", "3.5. Сложение и вычитание десятичных дробей", "3.6. Умножение и деление десятичных дробей на степени числа 10", "3.7. Умножение десятичной дроби на натуральное число", "3.8. Умножение десятичных дробей", "3.9. Деление десятичной дроби на натуральное число", "3.10. Деление числа на десятичную дробь", "3.11. Действия над обыкновенными и десятичными дробями");
        add("4️⃣ Процент", "4.1. Процент, обыкновенная дробь, десятичная дробь", "4.2. Процент от числа", "4.3. Нахождение числа по проценту", "4.4. Увеличение и уменьшение величины на определённый процент");
    }
    private void add(String name, String... topics) { List<String> list = new ArrayList<>(); for (String t : topics) list.add(t); chapters.put(name, list); }

    private static String todayKey() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Baku"));
        return f.format(new Date());
    }

    private static int reminderRequestCode(int day, int hour, int minute) { return 5000 + day * 10000 + hour * 100 + minute; }

    public static void scheduleReminders(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        // Cancel alarms from the previous saved schedule.
        Set<String> oldKeys = p.getStringSet("scheduled_alarm_keys", new TreeSet<String>());
        for (String key : new TreeSet<>(oldKeys)) {
            try {
                String[] bits = key.split("_");
                int day = Integer.parseInt(bits[0]);
                String[] hm = bits[1].split(":");
                int hour = Integer.parseInt(hm[0]), minute = Integer.parseInt(hm[1]);
                int id = reminderRequestCode(day, hour, minute);
                Intent old = new Intent(context, ReminderReceiver.class).setAction("study_reminder_" + key);
                PendingIntent oldPi = PendingIntent.getBroadcast(context, id, old, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.cancel(oldPi); oldPi.cancel();
            } catch (Exception ignored) { }
        }
        // Cancel the four fixed reminders from version 1, if this is an upgrade.
        for (int i = 0; i < 4; i++) {
            Intent old = new Intent(context, ReminderReceiver.class).setAction(null);
            PendingIntent pi = PendingIntent.getBroadcast(context, 300 + i, old, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi); pi.cancel();
        }
        Set<String> days = p.getStringSet("reminder_days", null);
        if (days == null) { days = new TreeSet<>(); for (int d = 1; d <= 7; d++) days.add(String.valueOf(d)); }
        Set<String> times = p.getStringSet("reminder_times", null);
        if (times == null) times = new TreeSet<>(java.util.Arrays.asList("10:00", "15:00", "19:00", "21:00"));
        Set<String> newKeys = new TreeSet<>();
        TimeZone tz = TimeZone.getTimeZone("Asia/Baku");
        for (String dayValue : days) for (String timeValue : times) {
            try {
                int day = Integer.parseInt(dayValue);
                String[] parts = timeValue.split(":");
                int hour = Integer.parseInt(parts[0]), minute = Integer.parseInt(parts[1]);
                String key = day + "_" + String.format(Locale.US, "%02d:%02d", hour, minute);
                Calendar next = Calendar.getInstance(tz);
                next.set(Calendar.DAY_OF_WEEK, day);
                next.set(Calendar.HOUR_OF_DAY, hour); next.set(Calendar.MINUTE, minute);
                next.set(Calendar.SECOND, 0); next.set(Calendar.MILLISECOND, 0);
                if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 7);
                int id = reminderRequestCode(day, hour, minute);
                Intent intent = new Intent(context, ReminderReceiver.class).setAction("study_reminder_" + key)
                        .putExtra("message", "🌸 Пора немного позаниматься! Ты сможешь, Назка!").putExtra("id", id);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7L, pi);
                newKeys.add(key);
            } catch (Exception ignored) { }
        }
        p.edit().putStringSet("scheduled_alarm_keys", newKeys).apply();
    }

    private void base(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22), dp(20), dp(22), dp(28)); root.setBackgroundColor(BG);
        scroll.addView(root);
        scroll.setAlpha(0f);
        scroll.setTranslationY(dp(10));
        setContentView(scroll);
        scroll.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        scroll.animate().alpha(1f).translationY(0f).setDuration(240).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        TextView brand = text("STUDY WITH LOVE  🌷", 12, PINK, true); brand.setLetterSpacing(0.10f); root.addView(brand, lp(-1, -2, 0, 0, 0, 8));
        TextView h = text(title, 27, DARK, true); root.addView(h, lp(-1, -2, 0, 0, 0, 5));
        TextView s = text(subtitle, 14, MUTED, false); root.addView(s, lp(-1, -2, 0, 0, 0, 18));
    }
    private void showHome() {
        currentScreen = "home";
        base("Привет, Назка! ❤️", "Твой маленький помощник в учёбе. Каждый шаг — уже успех.");
        int done = prefs.getInt("done_count", 0);
        int streak = currentStreak();
        int record = prefs.getInt("longest_streak", 0);
        LinearLayout progress = card();
        addText(progress, "🌸  ТВОЙ ПРОГРЕСС", 12, PINK, true);
        addText(progress, "🔥  " + streak + " " + dayWord(streak) + " подряд!", 25, DARK, true);
        addText(progress, "Рекорд: " + record + " " + dayWord(record) + "  ·  Завершено: " + done + " " + word(done), 13, MUTED, false);
        addText(progress, "Занимайся каждый день и собирай свою серию!", 13, MUTED, false);
        root.addView(progress, lp(-1, -2, 0, 0, 0, 18));
        action("📐  Математика", "Выбрать раздел и тему", this::showChapters);
        action("📊  Мой прогресс", "Посмотреть прогресс по разделам и темам", this::showProgress);
        action("🍅  Таймер Pomodoro", "Занимайся небольшими подходами с перерывами", this::showPomodoro);
        action("📔  Дневник успехов", "Записывай свои маленькие и большие победы", this::showDiary);
        action("🌷  Мой цветочек", "Смотри, как растёт твой цветок", this::showFlower);
        action("🏅  Коллекция достижений", "Твои маленькие и большие победы", this::showAchievements);
        action("💌  Письмо себе в будущее", "Оставь себе доброе послание", this::showFutureLetters);
        action("🗓️  Календарь привычек", "Отмечай регулярность занятий", this::showProgress);
        action("🎧  Атмосфера учёбы", "Выбери уютный режим для занятий", this::showAtmosphere);
        action("💌  Послание для тебя", "Немного поддержки на сегодня", this::showMessage);
        action("🔔  Настроить напоминания", "Выбрать дни недели и удобное время", this::showReminderSettings);
        LinearLayout note = card(); addText(note, "⏰  НАПОМИНАНИЯ", 13, PINK, true); addText(note, reminderSummary(), 14, DARK, false); root.addView(note, lp(-1, -2, 0, 8, 0, 0));
        TextView made = text("Сделано с любовью ♡", 11, MUTED, false); made.setGravity(Gravity.CENTER); made.setAlpha(0.85f); root.addView(made, lp(-1, 34, 0, 16, 0, 0));
    }
    private String word(int n) { int x=n%10, y=n%100; if(x==1 && y!=11)return "урок"; if(x>=2&&x<=4&&(y<12||y>14))return "урока"; return "уроков"; }
    private void showChapters() {
        currentScreen = "chapters";
        base("Выбери раздел 📐", "Все темы из твоего учебного плана");
        int chapterNumber = 1;
        for (String chapter : chapters.keySet()) {
            final String selected = chapter;
            String prettyName = chapter.replaceFirst("^[^ ]+\\s*", "");
            action("🌸  Раздел " + chapterNumber + "\n" + prettyName, chapters.get(chapter).size() + " учебных тем", () -> { selectedChapter = selected; showTopics(selected); });
            chapterNumber++;
        }
        backHome();
    }
    private void showTopics(String chapter) {
        currentScreen = "topics";
        base(chapter, "Выбери тему, которую хочешь изучить:");
        for (String topic : chapters.get(chapter)) {
            int percent = topicProgress(topic);
            action((percent == 100 ? "✓  " : "♡  ") + topic, "Изучено на " + percent + "%", () -> { selectedTopic = topic; showTopic(topic); });
        }
        Button back = button("← К разделам", false); back.setOnClickListener(v -> showChapters()); root.addView(back, lp(-1, 52, 0, 12, 0, 0));
    }
    private void showTopic(String topic) {
        currentScreen = "topic";
        base("Твоя тема 📖", "Учись в своём темпе — каждый шаг важен");
        int percent = topicProgress(topic);
        LinearLayout box = card();
        addText(box, topic, 20, DARK, true);
        addText(box, "Открой соответствующий урок в учебнике, изучи материал и реши задания. Отмечай свой прогресс — он сохранится автоматически.", 15, MUTED, false);
        topicProgressLabel = text("ТВОЙ ПРОГРЕСС: " + percent + "%", 13, PINK, true); box.addView(topicProgressLabel, lp(-1, -2, 0, 0, 0, 5));
        topicProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); topicProgressBar.setMax(100); topicProgressBar.setProgress(percent);
        if (Build.VERSION.SDK_INT >= 21) { topicProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK)); topicProgressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA)); }
        box.addView(topicProgressBar, lp(-1, 8, 0, 0, 0, 5));
        root.addView(box, lp(-1, -2, 0, 0, 0, 14));

        addText(root, "ОТМЕТИТЬ ПРОГРЕСС", 12, PINK, true);
        LinearLayout levels = new LinearLayout(this);
        levels.setOrientation(LinearLayout.HORIZONTAL);
        int[] values = {25, 50, 75, 100};
        for (int index = 0; index < values.length; index++) {
            final int value = values[index];
            Button b = button(value + "%", percent >= value);
            progressButtons[index] = b;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
            params.setMargins(dp(2), dp(4), dp(2), dp(10));
            levels.addView(b, params);
            b.setOnClickListener(v -> {
                saveTopicProgress(topic, value);
                if (value == 100) recordStudyDay();
                refreshTopicProgress(value);
                if (value == 100) showPraise();
            });
        }
        root.addView(levels, lp(-1, -2, 0, 0, 0, 8));

        Button done = button("✅  Я позанималась сегодня", true);
        done.setOnClickListener(v -> markDone());
        root.addView(done, lp(-1, 54, 0, 4, 0, 10));
        Button back = button("← К темам", false);
        back.setOnClickListener(v -> showTopics(selectedChapter));
        root.addView(back, lp(-1, 50, 0, 0, 0, 0));
    }

    private void refreshTopicProgress(int value) {
        if (topicProgressLabel != null) topicProgressLabel.setText("ТВОЙ ПРОГРЕСС: " + value + "%");
        if (topicProgressBar != null) topicProgressBar.setProgress(value, true);
        int[] values = {25, 50, 75, 100};
        for (int i = 0; i < progressButtons.length; i++) {
            if (progressButtons[i] != null) progressButtons[i].setBackground(shape(value >= values[i] ? PINK : PALE, 18, value >= values[i] ? PINK : 0xFFFFDCE6));
            if (progressButtons[i] != null) progressButtons[i].setTextColor(value >= values[i] ? 0xFFFFFFFF : PINK);
        }
    }

    private void showPraise() {
        LinearLayout toastView = new LinearLayout(this);
        toastView.setGravity(Gravity.CENTER_VERTICAL);
        toastView.setPadding(dp(18), dp(10), dp(20), dp(10));
        toastView.setBackground(shape(0xFFFFF0F5, 28, 0xFFFFC6D7));
        TextView flower = text("🌸", 22, PINK, true);
        toastView.addView(flower, lp(34, 34, 0, 0, 9, 0));
        TextView praise = text("Умничка! ❤️", 16, DARK, true);
        toastView.addView(praise, lp(-2, 38, 0, 0, 0, 0));
        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(toastView);
        toast.show();
    }

    private int topicProgress(String topic) {
        if (prefs.getBoolean("done_" + topic, false)) return 100;
        return Math.max(0, Math.min(100, prefs.getInt("progress_" + topic, 0)));
    }

    private void saveTopicProgress(String topic, int value) {
        int old = topicProgress(topic);
        int next = Math.max(0, Math.min(100, value));
        SharedPreferences.Editor e = prefs.edit().putInt("progress_" + topic, next).putBoolean("done_" + topic, next == 100);
        if (old < 100 && next == 100) e.putInt("done_count", prefs.getInt("done_count", 0) + 1);
        if (old == 100 && next < 100) e.putInt("done_count", Math.max(0, prefs.getInt("done_count", 0) - 1));
        e.apply();
    }

    private void recordStudyDay() {
        String today = todayKey();
        String last = prefs.getString("last_study_date", "");
        Set<String> history = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        history.add(today);
        if (!today.equals(last)) {
            int streak = prefs.getInt("current_streak", 0);
            if (isYesterday(last)) streak++; else streak = 1;
            prefs.edit().putInt("current_streak", streak)
                    .putInt("longest_streak", Math.max(streak, prefs.getInt("longest_streak", 0)))
                    .putString("last_study_date", today)
                    .putStringSet("study_days", history).apply();
        } else {
            prefs.edit().putStringSet("study_days", history).apply();
        }
    }

    private void markDone() {
        if (selectedTopic != null) saveTopicProgress(selectedTopic, 100);
        recordStudyDay();
        if (selectedTopic != null) refreshTopicProgress(100);
        showPraise();
    }

    private void showProgress() {
        saveForegroundUsage();
        currentScreen = "progress";
        base("Мой прогресс 📊", "Здесь видно, сколько уже изучено по каждой теме");
        int totalTopics = 0, completedTopics = 0;
        for (String chapter : chapters.keySet()) {
            for (String topic : chapters.get(chapter)) {
                totalTopics++;
                if (topicProgress(topic) == 100) completedTopics++;
            }
        }
        int overall = totalTopics == 0 ? 0 : completedTopics * 100 / totalTopics;

        // Study calendar: every date on which the learner marked a study session.
        LinearLayout calendarCard = card();
        addText(calendarCard, "📅  ДНИ ЗАНЯТИЙ", 12, PINK, true);
        Calendar month = Calendar.getInstance(TimeZone.getTimeZone("Asia/Baku"));
        int todayDay = month.get(Calendar.DAY_OF_MONTH);
        int monthNumber = month.get(Calendar.MONTH);
        int yearNumber = month.get(Calendar.YEAR);
        String monthLabel = new SimpleDateFormat("LLLL yyyy", new Locale("ru")).format(month.getTime());
        addText(calendarCard, monthLabel.substring(0, 1).toUpperCase(new Locale("ru")) + monthLabel.substring(1), 18, DARK, true);
        LinearLayout weekHeader = new LinearLayout(this);
        weekHeader.setOrientation(LinearLayout.HORIZONTAL);
        String[] shortDays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String dayLabel : shortDays) {
            TextView cell = text(dayLabel, 11, MUTED, true);
            cell.setGravity(Gravity.CENTER);
            weekHeader.addView(cell, new LinearLayout.LayoutParams(0, dp(30), 1f));
        }
        calendarCard.addView(weekHeader, lp(-1, 30, 0, 4, 0, 2));
        Set<String> studiedDays = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        Calendar first = (Calendar) month.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Monday first
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        LinearLayout week = null;
        for (int slot = 0; slot < offset + daysInMonth; slot++) {
            if (slot % 7 == 0) {
                week = new LinearLayout(this);
                week.setOrientation(LinearLayout.HORIZONTAL);
                calendarCard.addView(week, lp(-1, 38, 0, 1, 0, 1));
            }
            TextView cell = text("", 12, DARK, true);
            cell.setGravity(Gravity.CENTER);
            if (slot >= offset) {
                int day = slot - offset + 1;
                String dateKey = String.format(Locale.US, "%04d-%02d-%02d", yearNumber, monthNumber + 1, day);
                boolean studied = studiedDays.contains(dateKey);
                cell.setText(studied ? "♥\n" + day : String.valueOf(day));
                cell.setTextColor(studied ? PINK : DARK);
                cell.setBackground(shape(studied ? 0xFFFFE7EF : (day == todayDay ? 0xFFFFF1F5 : 0xFFFFFFFF), 13, day == todayDay ? PINK : (studied ? 0xFFFFBFD1 : 0x00FFFFFF)));
                cell.setElevation(dp(1));
                if (day == todayDay) cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
            week.addView(cell, new LinearLayout.LayoutParams(0, dp(36), 1f));
        }
        addText(calendarCard, "Отмечено дней в этом месяце: " + countStudyDaysThisMonth(studiedDays, yearNumber, monthNumber), 13, MUTED, false);
        root.addView(calendarCard, lp(-1, -2, 0, 0, 0, 14));

        LinearLayout summary = card();
        addText(summary, "🌸  ОБЩИЙ РЕЗУЛЬТАТ", 12, PINK, true);
        addText(summary, overall + "% изучено", 27, DARK, true);
        addText(summary, completedTopics + " из " + totalTopics + " тем завершено", 14, MUTED, false);
        addText(summary, "⏱ Сегодня: " + prefs.getInt("app_minutes_" + todayKey(), 0) + " минут занятий", 14, PINK, true);
        addText(summary, "Всего: " + totalStudyMinutes() + " минут", 13, MUTED, false);
        addProgressLine(summary, "Весь учебный план", overall);
        root.addView(summary, lp(-1, -2, 0, 0, 0, 16));

        for (String chapter : chapters.keySet()) {
            List<String> topics = chapters.get(chapter);
            int sum = 0;
            for (String topic : topics) sum += topicProgress(topic);
            int chapterPercent = topics.isEmpty() ? 0 : sum / topics.size();
            LinearLayout section = card();
            TextView chapterBadge = text("✿  РАЗДЕЛ " + (new ArrayList<>(chapters.keySet()).indexOf(chapter) + 1), 12, PINK, true);
            chapterBadge.setBackground(shape(0xFFFFEEF3, 16, 0xFFFFD5E1));
            chapterBadge.setPadding(dp(10), dp(7), dp(10), dp(7));
            section.addView(chapterBadge, lp(-2, -2, 0, 0, 0, 8));
            addText(section, chapter.replaceFirst("^[^ ]+\\s*", ""), 17, DARK, true);
            addText(section, chapterPercent + "% · " + countCompleted(topics) + " из " + topics.size() + " тем полностью изучено", 13, MUTED, false);
            addProgressLine(section, "Прогресс раздела", chapterPercent);
            for (String topic : topics) {
                int value = topicProgress(topic);
                addProgressLine(section, topic + " — " + value + "%", value);
            }
            root.addView(section, lp(-1, -2, 0, 0, 0, 12));
        }
        Button back = button("← На главную", true);
        back.setOnClickListener(v -> showHome());
        root.addView(back, lp(-1, 52, 0, 4, 0, 0));
    }

    private int totalStudyMinutes() {
        int total = 0;
        for (String key : prefs.getAll().keySet()) if (key.startsWith("app_minutes_")) total += prefs.getInt(key, 0);
        return total;
    }

    private void showPomodoro() {
        currentScreen = "pomodoro";
        base("Таймер Pomodoro 🍅", "Выбери удобную продолжительность занятия");
        addText(root, "ВРЕМЯ УЧЁБЫ · МИНУТЫ", 12, PINK, true);
        customMinutesInput = new EditText(this);
        customMinutesInput.setSingleLine(true); customMinutesInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        customMinutesInput.setHint("Например, 35"); customMinutesInput.setText(String.valueOf(selectedStudyMinutes));
        customMinutesInput.setTextSize(18); customMinutesInput.setTextColor(DARK); customMinutesInput.setPadding(dp(14), dp(8), dp(14), dp(8));
        customMinutesInput.setBackground(shape(0xFFFFFFFF, 16, 0xFFFFD5E1));
        root.addView(customMinutesInput, lp(-1, 52, 0, 5, 0, 14));
        addText(root, "ПЕРЕРЫВ", 12, PINK, true);
        LinearLayout breakChoices = new LinearLayout(this);
        int[] breakOptions = {5, 10, 15};
        for (int minutes : breakOptions) {
            Button b = button(minutes + "м", selectedBreakMinutes == minutes);
            breakChoices.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1f));
            b.setOnClickListener(v -> { if (!timerRunning) { selectedBreakMinutes = minutes; b.setText("✓ " + minutes + "м"); } });
        }
        root.addView(breakChoices, lp(-1, 46, 0, 4, 0, 18));
        LinearLayout timerCard = card(); timerCard.setGravity(Gravity.CENTER);
        timerStatus = text(timerIsBreak ? "Время отдохнуть 🌷" : "Время для себя и учёбы 💗", 15, PINK, true); timerStatus.setGravity(Gravity.CENTER);
        timerCard.addView(timerStatus, lp(-1, 36, 0, 0, 0, 4));
        timerDisplay = text(formatTimer(timerRemainingMs > 0 ? timerRemainingMs : selectedStudyMinutes * 60000L), 48, DARK, true); timerDisplay.setGravity(Gravity.CENTER);
        timerCard.addView(timerDisplay, lp(-1, 84, 0, 0, 0, 4)); root.addView(timerCard, lp(-1, -2, 0, 0, 0, 14));
        timerStartButton = button("▶  Начать", true);
        timerStartButton.setOnClickListener(v -> {
            if (!timerRunning && !timerIsBreak) {
                String raw = customMinutesInput.getText().toString().trim();
                try { int chosen = Integer.parseInt(raw); if (chosen < 1 || chosen > 600) throw new NumberFormatException(); selectedStudyMinutes = chosen; timerRemainingMs = chosen * 60000L; }
                catch (NumberFormatException ex) { Toast.makeText(this, "Введи от 1 до 600 минут 💗", Toast.LENGTH_SHORT).show(); return; }
            }
            startTimer();
        }); root.addView(timerStartButton, lp(-1, 52, 0, 0, 0, 8));
        timerPauseButton = button("Ⅱ  Пауза", false); timerPauseButton.setEnabled(timerRunning);
        timerPauseButton.setOnClickListener(v -> pauseTimer()); root.addView(timerPauseButton, lp(-1, 48, 0, 0, 0, 8));
        Button reset = button("↺  Сбросить", false); reset.setOnClickListener(v -> { timerHandler.removeCallbacks(timerTick); timerRunning = false; timerIsBreak = false; timerRemainingMs = selectedStudyMinutes * 60000L; showPomodoro(); }); root.addView(reset, lp(-1, 48, 0, 0, 0, 8));
        Button back = button("← На главную", false); back.setOnClickListener(v -> showHome()); root.addView(back, lp(-1, 48, 0, 0, 0, 0));
        if (timerRemainingMs <= 0) timerRemainingMs = selectedStudyMinutes * 60000L;
        if (timerRunning) { updateTimerDisplay(); if (timerStartButton != null) timerStartButton.setEnabled(false); if (timerPauseButton != null) timerPauseButton.setEnabled(true); }
    }

    private String formatTimer(long ms) {
        long sec = Math.max(0, ms / 1000); return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
    }

    private void startTimer() {
        if (timerRunning) return;
        if (timerRemainingMs <= 0) timerRemainingMs = (timerIsBreak ? selectedBreakMinutes : selectedStudyMinutes) * 60000L;
        timerRunning = true; timerSessionStarted = SystemClock.elapsedRealtime(); timerEndElapsed = timerSessionStarted + timerRemainingMs;
        timerTick = new Runnable() { @Override public void run() { updateTimerDisplay(); long left = timerEndElapsed - SystemClock.elapsedRealtime(); if (left <= 0) finishTimerPhase(); else timerHandler.postDelayed(this, 250); } };
        if (timerStartButton != null) timerStartButton.setEnabled(false);
        if (timerPauseButton != null) timerPauseButton.setEnabled(true);
        timerHandler.post(timerTick);
    }

    private void pauseTimer() {
        if (!timerRunning) return;
        timerRemainingMs = Math.max(0, timerEndElapsed - SystemClock.elapsedRealtime());
        timerRunning = false; timerHandler.removeCallbacks(timerTick);
        if (timerDisplay != null) timerDisplay.setText(formatTimer(timerRemainingMs));
        if (timerStartButton != null) { timerStartButton.setEnabled(true); timerStartButton.setText("▶  Продолжить"); }
        if (timerPauseButton != null) timerPauseButton.setEnabled(false);
    }

    private void updateTimerDisplay() {
        if (timerDisplay != null) timerDisplay.setText(formatTimer(Math.max(0, timerEndElapsed - SystemClock.elapsedRealtime())));
    }

    private void finishTimerPhase() {
        timerHandler.removeCallbacks(timerTick); timerRunning = false; timerRemainingMs = 0;
        if (!timerIsBreak) { timerIsBreak = true; timerRemainingMs = selectedBreakMinutes * 60000L; Toast.makeText(this, "Ты большая умничка! Время отдохнуть 🌸", Toast.LENGTH_LONG).show(); }
        else { timerIsBreak = false; timerRemainingMs = selectedStudyMinutes * 60000L; Toast.makeText(this, "Перерыв окончен. Ты готова продолжать! 💗", Toast.LENGTH_LONG).show(); }
        if ("pomodoro".equals(currentScreen)) showPomodoro();
    }

    // Study time in Progress is foreground time spent in the app, not only Pomodoro time.
    private void saveForegroundUsage() {
        if (appUsageStartedElapsed == 0L || prefs == null) return;
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - appUsageStartedElapsed);
        appUsageStartedElapsed = SystemClock.elapsedRealtime();
        String date = todayKey();
        String remainderKey = "app_usage_remainder_" + date;
        long combined = prefs.getLong(remainderKey, 0L) + elapsed;
        int addMinutes = (int)(combined / 60000L);
        prefs.edit().putLong(remainderKey, combined % 60000L)
                .putInt("app_minutes_" + date, prefs.getInt("app_minutes_" + date, 0) + addMinutes).apply();
        if (addMinutes > 0) recordStudyDay();
    }

    @Override protected void onResume() { super.onResume(); appUsageStartedElapsed = SystemClock.elapsedRealtime(); }
    @Override protected void onPause() { saveForegroundUsage(); appUsageStartedElapsed = 0L; super.onPause(); }

    private void showMessage() {
        currentScreen = "message";
        base("Послание для тебя 💌", "Маленькое напоминание с любовью:");
        TextView message = text(messages[new Random().nextInt(messages.length)], 22, PINK, true);
        message.setGravity(Gravity.CENTER); message.setPadding(dp(12), dp(26), dp(12), dp(26));
        LinearLayout messageCard = card(); messageCard.setGravity(Gravity.CENTER); messageCard.addView(message, lp(-1, -2, 0, 0, 0, 0));
        root.addView(messageCard, lp(-1, -2, 0, 8, 0, 14));
        Button generate = button("✨  Сгенерировать ещё послание", true);
        generate.setOnClickListener(v -> message.setText(messages[new Random().nextInt(messages.length)]));
        root.addView(generate, lp(-1, 52, 0, 0, 0, 10)); backHome();
    }

    private void showDiary() {
        currentScreen = "diary";
        base("Дневник успехов 📔", "Здесь важен каждый твой маленький шаг");
        EditText note = new EditText(this); note.setHint("Сегодня у меня получилось..."); note.setTextSize(16); note.setTextColor(DARK); note.setHintTextColor(MUTED); note.setGravity(Gravity.TOP); note.setMinLines(4); note.setPadding(dp(14), dp(12), dp(14), dp(12)); note.setBackground(shape(0xFFFFFFFF, 18, 0xFFFFD5E1));
        root.addView(note, lp(-1, 130, 0, 0, 0, 10));
        TextView saved = text("♡  Запись сохранена", 14, PINK, true); saved.setGravity(Gravity.CENTER); saved.setBackground(shape(0xFFFFEEF3, 18, 0xFFFFD5E1)); saved.setVisibility(View.GONE);
        final String[] editingId = {null};
        Button save = button("♡  Сохранить запись", true);
        save.setOnClickListener(v -> {
            String value = note.getText().toString().trim();
            if (value.isEmpty()) { Toast.makeText(this, "Напиши хотя бы пару слов 💗", Toast.LENGTH_SHORT).show(); return; }
            Set<String> ids = new TreeSet<>(prefs.getStringSet("diary_ids", new TreeSet<String>()));
            String id = editingId[0] == null ? String.valueOf(System.currentTimeMillis()) : editingId[0];
            ids.add(id); prefs.edit().putString("diary_" + id, value).putStringSet("diary_ids", ids).apply();
            editingId[0] = null; note.setText(""); save.setText("♡  Сохранить запись"); saved.setVisibility(View.VISIBLE);
            saved.setAlpha(0f); saved.animate().alpha(1f).setDuration(250).start();
            Toast.makeText(this, "Запись сохранена ❤️", Toast.LENGTH_SHORT).show();
        }); root.addView(save, lp(-1, 52, 0, 0, 0, 8));
        root.addView(saved, lp(-1, 42, 0, 0, 0, 16));
        addText(root, "МОИ ЗАПИСИ", 12, PINK, true);
        Set<String> ids = new TreeSet<>(prefs.getStringSet("diary_ids", new TreeSet<String>()));
        List<String> sorted = new ArrayList<>(ids); java.util.Collections.sort(sorted, java.util.Collections.reverseOrder());
        if (sorted.isEmpty()) addText(root, "Пока записей нет. Сохрани свою первую маленькую победу 🌷", 14, MUTED, false);
        for (String id : sorted) {
            String value = prefs.getString("diary_" + id, ""); if (value.isEmpty()) continue;
            LinearLayout entry = card(); String date = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru")).format(new Date(Long.parseLong(id)));
            addText(entry, "🌸  " + date, 12, PINK, true); addText(entry, value, 15, DARK, false);
            Button edit = button("Изменить запись", true); edit.setOnClickListener(v -> { note.setText(prefs.getString("diary_" + id, "")); editingId[0] = id; save.setText("♡  Сохранить изменения"); note.requestFocus(); });
            entry.addView(edit, lp(-1, 42, 0, 6, 0, 4));
            Button delete = button("Удалить запись", false); delete.setOnClickListener(v -> { Set<String> updated = new TreeSet<>(prefs.getStringSet("diary_ids", new TreeSet<String>())); updated.remove(id); prefs.edit().remove("diary_" + id).putStringSet("diary_ids", updated).apply(); showDiary(); }); entry.addView(delete, lp(-1, 42, 0, 0, 0, 0)); root.addView(entry, lp(-1, -2, 0, 0, 0, 10));
        }
        Button back = button("← На главную", false); back.setOnClickListener(v -> showHome()); root.addView(back, lp(-1, 48, 0, 8, 0, 0));
    }

    private void showFlower() {
        currentScreen = "flower";
        Set<String> days = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        int count = days.size();
        String flower = count >= 30 ? "💐" : count >= 14 ? "🌸" : count >= 7 ? "🌷" : count >= 3 ? "🌿" : "🌱";
        String stage = count >= 30 ? "Букет" : count >= 14 ? "Расцвет" : count >= 7 ? "Цветение" : count >= 3 ? "Первые листья" : "Росток";
        base("Мой цветочек", "Он растёт вместе с твоими стараниями");
        LinearLayout c = card(); c.setGravity(Gravity.CENTER);
        TextView art = text(flower, 76, PINK, true); art.setGravity(Gravity.CENTER); c.addView(art, lp(-1, 130, 0, 0, 0, 8));
        addText(c, stage, 22, DARK, true); addText(c, count + " дней занятий", 14, MUTED, false);
        root.addView(c, lp(-1, -2, 0, 0, 0, 14));
        addText(root, "Каждый день, когда ты занимаешься, твой цветочек становится крепче. Не торопись — всему своё время. ♡", 15, MUTED, false);
        backHome();
    }

    private void showAchievements() {
        currentScreen = "achievements";
        base("Коллекция достижений", "Каждый маленький шаг достоин похвалы");
        int days = prefs.getStringSet("study_days", new TreeSet<String>()).size();
        int minutes = totalStudyMinutes();
        int completed = 0; for (List<String> topics : chapters.values()) completed += countCompleted(topics);
        String[][] a = {{"🌱", "Первые шаги", "Первый день занятий", days >= 1 ? "Открыто" : "Пока закрыто"}, {"🌷", "Неделя стараний", "7 дней занятий", days >= 7 ? "Открыто" : "Пока закрыто"}, {"🌸", "Две недели", "14 дней занятий", days >= 14 ? "Открыто" : "Пока закрыто"}, {"💐", "Месяц успеха", "30 дней занятий", days >= 30 ? "Открыто" : "Пока закрыто"}, {"📚", "Маленький математик", "Завершена первая тема", completed >= 1 ? "Открыто" : "Пока закрыто"}, {"⏳", "10 часов учёбы", "600 минут занятий", minutes >= 600 ? "Открыто" : "Пока закрыто"}};
        for (String[] x : a) { LinearLayout c = card(); addText(c, x[0] + "  " + x[1], 18, DARK, true); addText(c, x[2], 13, MUTED, false); addText(c, x[3], 12, x[3].equals("Открыто") ? PINK : MUTED, true); root.addView(c, lp(-1, -2, 0, 0, 0, 9)); }
        backHome();
    }

    private void showFutureLetters() {
        currentScreen = "letters";
        base("Письмо себе в будущее", "Оставь доброе напоминание для себя будущей");
        EditText body = new EditText(this); body.setHint("Дорогая я, хочу сказать тебе..."); body.setMinLines(4); body.setGravity(Gravity.TOP); body.setTextSize(16); body.setTextColor(DARK); body.setPadding(dp(14),dp(12),dp(14),dp(12)); body.setBackground(shape(0xFFFFFFFF,18,0xFFEADDE2)); root.addView(body, lp(-1,130,0,0,0,10));
        EditText date = new EditText(this); date.setHint("Дата открытия: ГГГГ-ММ-ДД"); date.setSingleLine(true); date.setInputType(android.text.InputType.TYPE_CLASS_DATETIME); date.setTextSize(15); date.setPadding(dp(12),dp(8),dp(12),dp(8)); date.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2)); root.addView(date,lp(-1,50,0,0,0,10));
        Button save = button("♡  Сохранить письмо", true); save.setOnClickListener(v -> { String msg=body.getText().toString().trim(), d=date.getText().toString().trim(); if(msg.isEmpty() || d.isEmpty()){Toast.makeText(this,"Заполни письмо и дату открытия",Toast.LENGTH_SHORT).show();return;} try { java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US); f.setLenient(false); f.parse(d); } catch(Exception e){Toast.makeText(this,"Введи дату в формате ГГГГ-ММ-ДД",Toast.LENGTH_SHORT).show();return;} String id=String.valueOf(System.currentTimeMillis()); Set<String> ids=new TreeSet<>(prefs.getStringSet("future_letter_ids",new TreeSet<String>())); ids.add(id); prefs.edit().putString("future_letter_"+id,msg).putString("future_date_"+id,d).putStringSet("future_letter_ids",ids).apply(); body.setText("");date.setText(""); Toast.makeText(this,"Письмо сохранено ❤️",Toast.LENGTH_SHORT).show(); showFutureLetters(); }); root.addView(save,lp(-1,50,0,0,0,14));
        Set<String> ids=new TreeSet<>(prefs.getStringSet("future_letter_ids",new TreeSet<String>())); List<String> sorted=new ArrayList<>(ids); java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());
        for(String id:sorted){String d=prefs.getString("future_date_"+id,""); String msg=prefs.getString("future_letter_"+id,""); LinearLayout c=card(); boolean open=d.compareTo(todayKey())<=0; addText(c,(open?"✉️  Можно открыть":"🔒  Открывается "+d),14,PINK,true); if(open)addText(c,msg,15,DARK,false); else addText(c,"Твоё письмо ждёт назначенного дня ♡",13,MUTED,false); root.addView(c,lp(-1,-2,0,0,0,8)); }
        backHome();
    }

    private void showAtmosphere() {
        currentScreen = "atmosphere";
        base("Атмосфера учёбы", "Выбери настроение для спокойной концентрации");
        String[] names={"🌧️  Дождь за окном","☕  Уютное кафе","🌿  Звуки природы","📚  Тихая библиотека"};
        String[] descriptions={"Мягкий дождливый настрой","Тёплая атмосфера кофейни","Спокойствие и свежесть","Минимум отвлечений"};
        String chosen=prefs.getString("study_atmosphere",names[0]);
        for(int i=0;i<names.length;i++){final String n=names[i]; LinearLayout c=card(); addText(c,n,18,DARK,true); addText(c,descriptions[i],13,MUTED,false); Button b=button(chosen.equals(n)?"Выбрано ✓":"Выбрать",chosen.equals(n)); b.setOnClickListener(v->{prefs.edit().putString("study_atmosphere",n).apply();showAtmosphere();}); c.addView(b,lp(-1,44,0,8,0,0)); root.addView(c,lp(-1,-2,0,0,0,9));}
        addText(root,"Сейчас доступны визуальные режимы. Для воспроизведения фоновых звуков потребуются встроенные аудиофайлы.",12,MUTED,false); backHome();
    }

    private int countStudyDaysThisMonth(Set<String> days, int year, int month) {
        int count = 0;
        String prefix = String.format(Locale.US, "%04d-%02d-", year, month + 1);
        for (String day : days) if (day.startsWith(prefix)) count++;
        return count;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Hardware/system Back navigates inside the app instead of closing it.
        switch (currentScreen) {
            case "topic":
                if (selectedChapter != null) showTopics(selectedChapter); else showHome();
                break;
            case "topics":
                showChapters();
                break;
            case "chapters":
            case "progress":
            case "reminders":
            case "message":
            case "pomodoro":
            case "diary":
            case "flower":
            case "achievements":
            case "letters":
            case "atmosphere":
                showHome();
                break;
            case "home":
            default:
                Toast.makeText(this, "Ты уже на главной странице ❤️", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private int countCompleted(List<String> topics) {
        int count = 0;
        for (String topic : topics) if (topicProgress(topic) == 100) count++;
        return count;
    }

    private void addProgressLine(LinearLayout parent, String label, int percent) {
        TextView caption = text(label, 12, MUTED, false);
        parent.addView(caption, lp(-1, -2, 0, 5, 0, 3));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(Math.max(0, Math.min(100, percent)));
        if (Build.VERSION.SDK_INT >= 21) {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA));
        }
        parent.addView(bar, lp(-1, 8, 0, 0, 0, 5));
    }

    private boolean isYesterday(String date) {
        if (date == null || date.isEmpty()) return false;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("Asia/Baku"));
            java.util.Date d = f.parse(date);
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Baku"));
            c.add(Calendar.DAY_OF_YEAR, -1);
            return d != null && f.format(c.getTime()).equals(f.format(d));
        } catch (Exception e) { return false; }
    }

    private int currentStreak() {
        String last = prefs.getString("last_study_date", "");
        if (todayKey().equals(last) || isYesterday(last)) return prefs.getInt("current_streak", 0);
        return 0;
    }

    private String dayWord(int n) {
        int x = Math.abs(n) % 10, y = Math.abs(n) % 100;
        if (x == 1 && y != 11) return "день";
        if (x >= 2 && x <= 4 && (y < 12 || y > 14)) return "дня";
        return "дней";
    }

    private Set<String> selectedDays() {
        Set<String> saved = prefs.getStringSet("reminder_days", null);
        if (saved != null) return new TreeSet<>(saved);
        Set<String> result = new TreeSet<>();
        for (int d = 1; d <= 7; d++) if (prefs.getBoolean("day_" + d, true)) result.add(String.valueOf(d));
        return result;
    }

    private List<String> getSavedTimes() {
        Set<String> saved = prefs.getStringSet("reminder_times", null);
        Set<String> values = saved == null ? new TreeSet<>(java.util.Arrays.asList("10:00", "15:00", "19:00", "21:00")) : new TreeSet<>(saved);
        return new ArrayList<>(values);
    }

    private String reminderSummary() {
        Set<String> days = selectedDays();
        List<String> times = getSavedTimes();
        if (times.isEmpty() || days.isEmpty()) return "Напоминания выключены. Открой настройки, чтобы включить.";
        List<String> labels = new ArrayList<>();
        for (String day : days) { int index = Integer.parseInt(day) - 1; labels.add(new String[]{"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"}[index]); }
        return android.text.TextUtils.join(", ", labels) + "  ·  " + android.text.TextUtils.join(", ", times) + " (Баку)";
    }

    private void showReminderSettings() {
        currentScreen = "reminders";
        base("Напоминания 🔔", "Выбери дни и время. Часовой пояс — Баку.");
        addText(root, "ДНИ НЕДЕЛИ", 12, PINK, true);
        dayChecks.clear();
        Set<String> savedDays = selectedDays();
        for (int d = 1; d <= 7; d++) {
            CheckBox cb = new CheckBox(this); cb.setText(DAY_NAMES[d - 1]); cb.setTextSize(15); cb.setTextColor(DARK);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK)); cb.setChecked(savedDays.contains(String.valueOf(d)));
            dayChecks.add(cb); root.addView(cb, lp(-1, 42, 0, 0, 0, 0));
        }
        addText(root, "ВРЕМЯ НАПОМИНАНИЙ", 12, PINK, true);
        reminderTimes.clear(); reminderTimes.addAll(getSavedTimes());
        LinearLayout timeList = new LinearLayout(this); timeList.setOrientation(LinearLayout.VERTICAL); root.addView(timeList, lp(-1, -2, 0, 4, 0, 8));
        final Runnable[] renderTimes = new Runnable[1];
        renderTimes[0] = () -> {
            timeList.removeAllViews();
            if (reminderTimes.isEmpty()) addText(timeList, "Время не выбрано — напоминания выключены", 14, MUTED, false);
            for (String time : new ArrayList<>(reminderTimes)) {
                LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = text("⏰  " + time, 17, DARK, true); row.addView(label, new LinearLayout.LayoutParams(0, dp(46), 1));
                Button remove = button("Убрать", false); remove.setOnClickListener(v -> { reminderTimes.remove(time); renderTimes[0].run(); });
                row.addView(remove, lp(100, 42, 6, 0, 0, 0)); timeList.addView(row, lp(-1, 46, 0, 2, 0, 2));
            }
        };
        renderTimes[0].run();
        Button addTime = button("＋  Добавить время", false);
        addTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Baku"));
            new TimePickerDialog(this, (TimePicker picker, int hour, int minute) -> {
                String value = String.format(Locale.US, "%02d:%02d", hour, minute);
                if (!reminderTimes.contains(value)) { reminderTimes.add(value); java.util.Collections.sort(reminderTimes); renderTimes[0].run(); }
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        });
        root.addView(addTime, lp(-1, 48, 0, 4, 0, 14));
        Button save = button("💗  Сохранить расписание", true);
        save.setOnClickListener(v -> {
            Set<String> days = new TreeSet<>();
            for (int i = 0; i < dayChecks.size(); i++) if (dayChecks.get(i).isChecked()) days.add(String.valueOf(i + 1));
            Set<String> times = new TreeSet<>(reminderTimes);
            SharedPreferences.Editor e = prefs.edit().putStringSet("reminder_days", days).putStringSet("reminder_times", times);
            for (int d = 1; d <= 7; d++) e.putBoolean("day_" + d, days.contains(String.valueOf(d)));
            e.apply(); scheduleReminders(this);
            Toast.makeText(this, days.isEmpty() || times.isEmpty() ? "Напоминания выключены" : "Расписание сохранено! ❤️", Toast.LENGTH_LONG).show();
            showHome();
        });
        root.addView(save, lp(-1, 54, 0, 4, 0, 8));
        Button back = button("← Назад", false); back.setOnClickListener(v -> showHome()); root.addView(back, lp(-1, 48, 0, 0, 0, 0));
    }
    private void backHome() { Button b = button("← Главное меню", false); b.setOnClickListener(v -> showHome()); root.addView(b, lp(-1, 50, 0, 18, 0, 0)); }
    private void action(String title, String sub, OnAction run) {
        LinearLayout c = card(); c.setOrientation(LinearLayout.VERTICAL); addText(c, title, 17, DARK, true); addText(c, sub, 13, MUTED, false); c.setOnClickListener(v -> run.go()); root.addView(c, lp(-1, -2, 0, 0, 0, 10));
    }
    private LinearLayout card() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(15), dp(16), dp(15)); l.setBackground(shape(0xFFFFFFFF, 20, 0xFFFFE2EA)); return l; }
    private void addText(LinearLayout parent, String value, int size, int color, boolean bold) { TextView t = text(value, size, color, bold); parent.addView(t, lp(-1, -2, 0, 0, 0, 5)); }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private Button button(String label, boolean primary) { Button b = new Button(this); b.setText(label); b.setTextSize(15); b.setAllCaps(false); b.setTextColor(primary ? 0xFFFFFFFF : PINK); b.setBackground(shape(primary ? PINK : PALE, 18, primary ? PINK : 0xFFFFDCE6)); return b; }
    private interface OnAction { void go(); }
    private GradientDrawable shape(int color, int radius, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), stroke); return d; }
    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
}
