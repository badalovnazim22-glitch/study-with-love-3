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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.DatePicker;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
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
    public static volatile boolean activityVisible=false;
    private int BG = 0xFFFFF9F7, PINK = 0xFFC97891, PALE = 0xFFFCEEF2, DARK = 0xFF3F3439, MUTED = 0xFF95878C;
    private int progressMonthOffset = 0;
    private LinearLayout progressCalendarHost;
    private LinearLayout root;
    private String currentScreen = "home";
    private String renderedScreen = "";
    private final List<String> screenHistory = new ArrayList<>();
    private boolean navigatingBack = false;
    private long lastBackPressedAt=0L;
    private SharedPreferences prefs;
    private String selectedTopic = null, selectedChapter = null;
    private TextView topicProgressLabel;
    private ProgressBar topicProgressBar;
    private Button[] progressButtons = new Button[4];
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerTick;
    private TextView timerDisplay, timerStatus, studyTimeValue, breakTimeValue;
    private android.animation.ValueAnimator timerPulseAnimator;
    private android.animation.ObjectAnimator flowerBreatheAnimator;
    private final List<MiniQuestion> miniQuestions=new ArrayList<>();
    private final List<MiniQuestion> miniWrongQuestions=new ArrayList<>();
    private final Map<String,List<MiniQuestion>> questionBank=new HashMap<>();
    private int miniScore=0;
    private String miniTopic="";
    private boolean weaknessTrainingMode=false,miniResultSaved=false;
    private static class MiniQuestion { String prompt,explanation,type,answer,topic; String[] options; int correct; MiniQuestion(String p,String[] o,int c,String e){this(p,o,c,e,"choice",o!=null&&c>=0&&c<o.length?o[c]:"");} MiniQuestion(String p,String[] o,int c,String e,String t,String a){prompt=p;options=o;correct=c;explanation=e;type=t;answer=a;topic="";} }
    private Button timerStartButton, timerPauseButton;
    private EditText customMinutesInput;
    private long appUsageStartedElapsed = 0L;
    private int selectedStudyMinutes = 25, selectedBreakMinutes = 5;
    private long timerEndElapsed = 0L, timerRemainingMs = 0L;
    private boolean timerRunning = false, timerIsBreak = false;
    private long timerSessionStarted = 0L;
    private volatile boolean ambiencePlaying=false;
    private volatile boolean ambienceLoading=false;
    private android.media.AudioManager ambienceAudioManager;
    private Object ambienceFocusRequest;
    private volatile int ambienceGeneration=0;
    private android.graphics.Bitmap cachedBackgroundBitmap;
    private String cachedBackgroundUri="";
    private Thread ambienceThread;
    private android.media.AudioTrack ambienceTrack;
    private android.media.MediaPlayer ambiencePlayer;
    private final android.media.AudioManager.OnAudioFocusChangeListener ambienceFocusListener=change->{if(ambiencePlayer!=null){if(change==android.media.AudioManager.AUDIOFOCUS_LOSS||change==android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){try{if(ambiencePlayer.isPlaying())ambiencePlayer.pause();}catch(Exception ignored){}}else if(change==android.media.AudioManager.AUDIOFOCUS_GAIN){try{ambiencePlayer.start();}catch(Exception ignored){}}}};

    private String pendingReviewKey="";
    private static final String PREFS = "study_progress";
    private MistakeStore mistakeStore;
    private MistakeStore mistakeStore(){if(mistakeStore==null)mistakeStore=new MistakeStore(this);return mistakeStore;}
    private static final int PICK_BACKGROUND = 7412, PICK_TOPIC_PHOTO = 7415, PICK_TOPIC_CAMERA = 7418;
    private static final int BACKUP_EXPORT = 7413, BACKUP_IMPORT = 7414, WEEKLY_IMAGE_EXPORT = 7416, PDF_REPORT_EXPORT = 7417;
    private android.graphics.Bitmap pendingWeeklyBitmap;
    private static final String[] DAY_NAMES = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    private final List<String> reminderTimes = new ArrayList<>();
    private final List<CheckBox> dayChecks = new ArrayList<>();
    private final Map<String, List<String>> chapters = new LinkedHashMap<>();
    private final String[] messages = {"Ты отлично справляешься! ❤️", "Я верю в тебя, у тебя всё получится! 🌸", "Не сдавайся: маленькие шаги приводят к большим результатам! 💗", "Ты справишься! И не забывай улыбаться ❤️"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::handleBackNavigation);
        }
        getWindow().getDecorView().setBackgroundColor(BG);
        getWindow().setStatusBarColor(0xFFFFF9F7);
        getWindow().setNavigationBarColor(0xFFFFF9F7);
        updateSystemBarIcons();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences timerPrefs=getSharedPreferences("timer_runtime",MODE_PRIVATE);if(!timerPrefs.contains("timer_service_end_wall")&&prefs.contains("timer_service_end_wall")){SharedPreferences.Editor migrate=timerPrefs.edit();migrate.putLong("timer_service_end_wall",prefs.getLong("timer_service_end_wall",0)).putLong("timer_service_duration",prefs.getLong("timer_service_duration",60000)).putBoolean("timer_service_break",prefs.getBoolean("timer_service_break",false)).apply();prefs.edit().remove("timer_service_end_wall").remove("timer_service_duration").remove("timer_service_break").apply();}long restoredEnd=timerPrefs.getLong("timer_service_end_wall",0);if(restoredEnd>System.currentTimeMillis()){timerRunning=true;timerIsBreak=timerPrefs.getBoolean("timer_service_break",false);timerRemainingMs=restoredEnd-System.currentTimeMillis();timerEndElapsed=SystemClock.elapsedRealtime()+timerRemainingMs;long duration=timerPrefs.getLong("timer_service_duration",timerRemainingMs);timerSessionStarted=SystemClock.elapsedRealtime()-Math.max(0,duration-timerRemainingMs);}
        applySavedTheme();
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        Set<String> visits = new TreeSet<>(prefs.getStringSet("visit_days", new TreeSet<String>())); visits.add(todayKey()); prefs.edit().putStringSet("visit_days", visits).apply();
        // Migrate the last recorded study date from older app versions into the calendar history.
        String previousStudyDate = prefs.getString("last_study_date", "");
        if (!previousStudyDate.isEmpty()) {
            Set<String> history = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
            history.add(previousStudyDate);
            prefs.edit().putStringSet("study_days", history).apply();
        }
        loadChapters();
        loadQuestionBank();
        setupDynamicShortcuts();
        scheduleReminders(this); rescheduleFutureLetterReminders(this); WeeklyReportReceiver.schedule(this); StudyWidgetProvider.updateAll(this);
        if (prefs.getBoolean("notifications_enabled", true) && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        if (!prefs.getBoolean("welcome_done", false)) showWelcome(); else { showHome(); handleQuickAction(getIntent()); }
    }

    private void setupDynamicShortcuts(){
        if(Build.VERSION.SDK_INT<25)return;try{android.content.pm.ShortcutManager manager=getSystemService(android.content.pm.ShortcutManager.class);if(manager==null)return;List<android.content.pm.ShortcutInfo> items=new ArrayList<>();
            String[] ids={"focus25","future_letter","diary"};String[] labels={"Фокус 25 минут","Письмо себе","Дневник"};String[] actions={"com.studywithlove.app.SHORTCUT_FOCUS","com.studywithlove.app.SHORTCUT_LETTER","com.studywithlove.app.SHORTCUT_DIARY"};
            for(int i=0;i<ids.length;i++){Intent intent=new Intent(this,MainActivity.class).setAction(actions[i]).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);android.content.pm.ShortcutInfo info=new android.content.pm.ShortcutInfo.Builder(this,ids[i]).setShortLabel(labels[i]).setLongLabel(labels[i]).setIcon(android.graphics.drawable.Icon.createWithResource(this,R.mipmap.ic_launcher)).setIntent(intent).build();items.add(info);}manager.setDynamicShortcuts(items);
        }catch(Exception ignored){}
    }
    private void handleQuickAction(Intent intent){
        if(intent==null)return;String action=intent.getAction();if("com.studywithlove.app.SHORTCUT_FOCUS".equals(action)){intent.setAction(null);showPomodoro();timerIsBreak=false;selectedStudyMinutes=25;timerRemainingMs=25*60000L;updateTimerDisplay();startTimer();}
        else if("com.studywithlove.app.SHORTCUT_LETTER".equals(action)){intent.setAction(null);showFutureLetters();}
        else if("com.studywithlove.app.SHORTCUT_DIARY".equals(action)){intent.setAction(null);showDiary();}
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleQuickAction(intent);}
    private void launchAiTutor() {
        Intent intent = new Intent(this, AiTutorActivity.class);
        StringBuilder context = new StringBuilder();
        if (selectedChapter != null && selectedTopic != null) {
            context.append("Текущий урок: ").append(selectedChapter.replaceFirst("^[^ ]+\\s*", ""))
                    .append(" — ").append(selectedTopic).append(". ");
        }
        if (prefs.getBoolean("exam_topic_filter_enabled", false)) {
            Set<String> selected = new TreeSet<>(prefs.getStringSet("exam_topic_ids", new TreeSet<String>()));
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : chapters.entrySet()) {
                for (String topic : entry.getValue()) {
                    if (selected.contains(examTopicKey(entry.getKey(), topic))) {
                        names.add(topic);
                        if (names.size() >= 18) break;
                    }
                }
                if (names.size() >= 18) break;
            }
            if (names.isEmpty()) {
                context.append("Фильтр тем экзамена включён, но темы пока не выбраны. Предложи выбрать темы в разделе экзаменационных целей. ");
            } else {
                context.append("Выбранные темы для подготовки к экзамену: ")
                        .append(android.text.TextUtils.join(", ", names)).append(". ");
            }
        }
        if (context.length() > 0) {
            intent.putExtra("study_context", context.toString());
        }
        startActivity(intent);
    }

    private void showWelcome() {
        currentScreen="welcome"; base("Добро пожаловать ♡","Давай познакомимся и настроим приложение");
        LinearLayout panel=card();panel.setGravity(Gravity.CENTER_HORIZONTAL);panel.setPadding(dp(20),dp(20),dp(20),dp(20));
        TextView flower=text("🌷",64,PINK,true);flower.setGravity(Gravity.CENTER);panel.addView(flower,lp(-1,92,0,0,0,8));
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextSize(17);name.setTextColor(DARK);name.setHint("Как тебя зовут?");name.setPadding(dp(14),dp(8),dp(14),dp(8));name.setBackground(shape(0xFFFFFFFF,16,0xFFEADDE2));panel.addView(name,lp(-1,52,0,0,0,12));
        addText(panel,"КАК К ТЕБЕ ОБРАЩАТЬСЯ?",11,PINK,true);addText(panel,"Выбери обращение",13,MUTED,false);
        final String[] gender={""};LinearLayout choices=new LinearLayout(this);choices.setOrientation(LinearLayout.HORIZONTAL);
        Button[] opts={button("Девушка",false),button("Парень",false)};String[] vals={"female","male"};
        for(int i=0;i<2;i++){final int k=i;LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1f);p.setMargins(dp(3),0,dp(3),0);choices.addView(opts[i],p);opts[i].setOnClickListener(v->{gender[0]=vals[k];for(Button q:opts)q.setAlpha(.65f);opts[k].setAlpha(1f);opts[k].setBackground(shape(PALE,18,PINK));opts[k].setTextColor(PINK);opts[k].animate().scaleX(1.05f).scaleY(1.05f).setDuration(160).withEndAction(()->opts[k].animate().scaleX(1f).scaleY(1f).setDuration(130).start()).start();});}
        panel.addView(choices,lp(-1,52,0,5,0,16));Button start=button("Начать путешествие ✨",true);panel.addView(start,lp(-1,54,0,4,0,0));root.addView(panel,lp(-1,-2,0,0,0,18));
        start.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Введи имя");return;}if(gender[0].isEmpty()){Toast.makeText(this,"Выбери пол",Toast.LENGTH_SHORT).show();return;}prefs.edit().putString("display_name",n).putString("profile_gender",gender[0]).putBoolean("welcome_done",true).apply();panel.animate().alpha(0f).translationY(dp(28)).scaleX(.97f).scaleY(.97f).setDuration(950).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(()->{showHome();View decor=getWindow().getDecorView();decor.setAlpha(0f);decor.setScaleX(1.025f);decor.setScaleY(1.025f);decor.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1100).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}).start();});
        panel.setAlpha(0f);panel.setTranslationY(dp(24));panel.animate().alpha(1f).translationY(0).setDuration(520).start();
    }

    private void loadChapters() {
        chapters.clear();
        add("1️⃣ Натуральные числа и действия с ними", "1.1. Натуральные числа", "1.2. Сравнение и упорядочивание", "1.3. Округление натуральных чисел", "1.4. Сложение и вычитание натуральных чисел", "1.5. Квадрат и куб натурального числа", "1.6. Умножение и деление натуральных чисел", "1.7. Числовые выражения", "1.8. Делители и кратные числа", "1.9. Простые и составные числа", "1.10. Признаки делимости");
        add("2️⃣ Обыкновенные дроби", "2.1. Правильные и неправильные дроби", "2.2. Сравнение и упорядочивание", "2.11. Сокращение дробей", "2.12. Приведение дробей к общему знаменателю", "2.3. Сложение и вычитание дробей с разными знаменателями", "2.4. Сложение смешанных чисел", "2.5. Вычитание смешанных чисел", "2.6. Умножение обыкновенных дробей", "2.7. Умножение смешанных чисел", "2.8. Деление обыкновенных дробей", "2.9. Деление смешанных чисел", "2.10. Нахождение части числа и числа по его части");
        add("3️⃣ Десятичные дроби", "3.1. Десятичные дроби", "3.2. Сравнение и упорядочивание", "3.3. Округление десятичных дробей", "3.4. Перевод обыкновенной дроби в десятичную и наоборот", "3.5. Сложение и вычитание десятичных дробей", "3.6. Умножение и деление десятичных дробей на степени числа 10", "3.7. Умножение десятичной дроби на натуральное число", "3.8. Умножение десятичных дробей", "3.9. Деление десятичной дроби на натуральное число", "3.10. Деление числа на десятичную дробь", "3.11. Действия над обыкновенными и десятичными дробями", "3.12. Среднее арифметическое десятичных дробей", "3.13. Задачи на десятичные дроби");
        add("4️⃣ Процент", "4.1. Процент, обыкновенная дробь, десятичная дробь", "4.2. Процент от числа", "4.3. Нахождение числа по проценту", "4.4. Увеличение и уменьшение величины на определённый процент", "4.5. Отношение и пропорция", "4.6. Решение задач на проценты");
        Set<String> ids = prefs.getStringSet("custom_lesson_ids", new TreeSet<String>());
        for (String id : ids) {
            String name = prefs.getString("custom_lesson_name_" + id, "Мой урок");
            chapters.put("CUSTOM:" + id + ":" + name, new ArrayList<>(prefs.getStringSet("custom_lesson_topics_" + id, new TreeSet<String>())));
        }
        SharedPreferences.Editor reviewMigration=prefs.edit();boolean migratedReviewDates=false;for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue())if(topicProgressForChapter(entry.getKey(),topic)==100){String key="topic_completed_at_"+topicStorageSuffix(entry.getKey(),topic);if(!prefs.contains(key)){reviewMigration.putLong(key,System.currentTimeMillis());migratedReviewDates=true;}}if(migratedReviewDates)reviewMigration.apply();
    }
    private void add(String name, String... topics) { List<String> list = new ArrayList<>(); for (String t : topics) list.add(t); chapters.put(name, list); }

    private static String todayKey() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(AppTime.zone());
        return f.format(new Date());
    }

    private static int reminderRequestCode(int day, int hour, int minute) { return 5000 + day * 10000 + hour * 100 + minute; }

    private void chooseQuietTime(String key){String value=prefs.getString(key,key.endsWith("start")?"22:00":"08:00");String[] p=value.split(":");int h=22,m=0;try{h=Integer.parseInt(p[0]);m=Integer.parseInt(p[1]);}catch(Exception ignored){}new TimePickerDialog(this,(view,hour,minute)->{String time=String.format(Locale.US,"%02d:%02d",hour,minute);prefs.edit().putString(key,time).apply();scheduleReminders(this);showSettings();},h,m,true).show();}
    private static void scheduleEveningRest(Context context,SharedPreferences p,AlarmManager am){
        Intent intent=new Intent(context,ReminderReceiver.class).setAction("study_evening_rest").putExtra("message","На сегодня достаточно. Пора отдохнуть и восстановить силы 🌙").putExtra("id",9810);
        PendingIntent pi=PendingIntent.getBroadcast(context,9810,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(!p.getBoolean("quiet_hours_enabled",false)||!p.getBoolean("notifications_enabled",true)){am.cancel(pi);pi.cancel();return;}
        int hour=21,minute=0;try{String[] bits=p.getString("quiet_hours_start","22:00").split(":");hour=Integer.parseInt(bits[0]);minute=Integer.parseInt(bits[1]);if(hour<18){hour=21;minute=0;}}catch(Exception ignored){}
        Calendar next=Calendar.getInstance(AppTime.zone());next.set(Calendar.HOUR_OF_DAY,hour);next.set(Calendar.MINUTE,minute);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
        if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
    }
    private static boolean isQuietHour(int hour,int minute,SharedPreferences p){if(!p.getBoolean("quiet_hours_enabled",false))return false;try{String[] a=p.getString("quiet_hours_start","22:00").split(":"),b=p.getString("quiet_hours_end","08:00").split(":");int start=Integer.parseInt(a[0])*60+Integer.parseInt(a[1]),end=Integer.parseInt(b[0])*60+Integer.parseInt(b[1]),now=hour*60+minute;if(start==end)return true;return start<end?(now>=start&&now<end):(now>=start||now<end);}catch(Exception e){return false;}}
    public static void scheduleReminders(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        scheduleEveningRest(context,p,am);
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
        if (!p.getBoolean("notifications_enabled", true) || p.getBoolean("do_not_disturb", false)) { p.edit().putStringSet("scheduled_alarm_keys", new TreeSet<String>()).apply(); return; }
        Set<String> days = p.getStringSet("reminder_days", null);
        if (days == null) { days = new TreeSet<>(); for (int d = 1; d <= 7; d++) days.add(String.valueOf(d)); }
        Set<String> times = p.getStringSet("reminder_times", null);
        if (times == null) times = new TreeSet<>(java.util.Arrays.asList("10:00", "15:00", "19:00", "21:00"));
        Set<String> newKeys = new TreeSet<>();
        TimeZone tz = AppTime.zone();
        for (String dayValue : days) for (String timeValue : times) {
            try {
                int day = Integer.parseInt(dayValue);
                String[] parts = timeValue.split(":");
                int hour = Integer.parseInt(parts[0]), minute = Integer.parseInt(parts[1]);
                if(isQuietHour(hour,minute,p))continue;
                String key = day + "_" + String.format(Locale.US, "%02d:%02d", hour, minute);
                Calendar next = Calendar.getInstance(tz);
                next.set(Calendar.DAY_OF_WEEK, day);
                next.set(Calendar.HOUR_OF_DAY, hour); next.set(Calendar.MINUTE, minute);
                next.set(Calendar.SECOND, 0); next.set(Calendar.MILLISECOND, 0);
                if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 7);
                int id = reminderRequestCode(day, hour, minute);
                Intent intent = new Intent(context, ReminderReceiver.class).setAction("study_reminder_" + key)
                        .putExtra("message", "🌸 Пора немного позаниматься! Ты сможешь!").putExtra("id", id);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= 23) { if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi); else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi); } else am.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
                newKeys.add(key);
            } catch (Exception ignored) { }
        }
        p.edit().putStringSet("scheduled_alarm_keys", newKeys).apply();
    }

    private void base(String title, String subtitle) {
        final boolean backNavigation=navigatingBack;
        if (!"flower".equals(currentScreen) && flowerBreatheAnimator!=null) { flowerBreatheAnimator.cancel(); flowerBreatheAnimator=null; }
        if (!currentScreen.equals(renderedScreen)) { if (!renderedScreen.isEmpty() && !navigatingBack) screenHistory.add(renderedScreen); renderedScreen=currentScreen; }
        navigatingBack=false;
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setClipToPadding(false);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(26)); root.setBackgroundColor(BG);
        installThemeFontListener(root);
        String customBg=prefs.getString("custom_background_uri","");
        if(!customBg.isEmpty()){try{if(!customBg.equals(cachedBackgroundUri)||cachedBackgroundBitmap==null){java.io.InputStream in=getContentResolver().openInputStream(android.net.Uri.parse(customBg));if(in!=null){android.graphics.Bitmap decoded=android.graphics.BitmapFactory.decodeStream(in);in.close();cachedBackgroundBitmap=decoded;cachedBackgroundUri=customBg;}}if(cachedBackgroundBitmap!=null){Drawable photo=new android.graphics.drawable.BitmapDrawable(getResources(),cachedBackgroundBitmap);photo.setAlpha(42);root.setBackground(photo);}}catch(Exception ignored){}}
        scroll.addView(root);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        if (!currentScreen.equals("welcome")) shell.addView(bottomNavigation(), lp(-1, 78, 0, 0, 0, 0));
        setContentView(shell);
        if (Build.VERSION.SDK_INT >= 21) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                int top,bottom;
                if (Build.VERSION.SDK_INT >= 30) { android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()); top=bars.top; bottom=bars.bottom; }
                else { top=insets.getSystemWindowInsetTop(); bottom=insets.getSystemWindowInsetBottom(); }
                view.setPadding(0,top,0,bottom);
                return insets;
            });
            shell.requestApplyInsets();
        }
        scroll.setAlpha(0f); scroll.setTranslationY(dp(backNavigation?-12:14));
        scroll.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        scroll.animate().alpha(1f).translationY(0f).setDuration(290)
            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        if (currentScreen.equals("home")) {
        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        brandRow.setPadding(dp(12), dp(7), dp(8), dp(7));
        brandRow.setBackground(shape(0xFFFCEEF2, 30, 0));
        TextView flower = text("✿", 16, PINK, true);
        brandRow.addView(flower, lp(24, 24, 0, 0, 7, 0));
        TextView brand = text("STUDY WITH LOVE", 9, PINK, true);
        brand.setLetterSpacing(0.10f);
        brandRow.addView(brand, new LinearLayout.LayoutParams(0, dp(24), 1f));
        if (currentScreen.equals("home")) {
            TextView settings = text("⚙", 17, PINK, false); settings.setGravity(Gravity.CENTER);
            settings.setBackground(shape(PALE, 16, "night".equals(prefs.getString("app_theme","pink"))?0xFF65566A:0xFFF1DDE4));
            brandRow.addView(settings, lp(32, 30, 5, 0, 0, 0));
            settings.setOnClickListener(v -> {settings.animate().rotationBy(90f).scaleX(1.18f).scaleY(1.18f).setDuration(170).withEndAction(()->{settings.animate().rotationBy(90f).scaleX(1f).scaleY(1f).setDuration(180).start();showSettings();}).start();});
        }
        root.addView(brandRow, lp(-1, 48, 0, 0, 0, 16));
        }
        TextView h = text(title, 23, DARK, true);
        h.setLetterSpacing(-0.02f);
        root.addView(h, lp(-1, -2, 0, 0, 0, 6));
        TextView sub = text(subtitle, 13, MUTED, false);
        sub.setLineSpacing(dp(2), 1f);
        root.addView(sub, lp(-1, -2, 0, 0, 0, 16));
    }

    private View bottomNavigation() {
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER);bar.setPadding(dp(5),dp(7),dp(5),dp(7));bar.setBackground(shape(0xFFFFFFFF,0,0));
        String[] icons={"⌂","▦","▤","▣","♡","◷"};String[] labels={"Главная","Уроки","Прогресс","Экзамен","Дневник","Таймер"};
        String active=currentScreen.equals("home")?"home":(currentScreen.equals("chapters")||currentScreen.equals("math_chapters")||currentScreen.equals("topics")||currentScreen.equals("topic"))?"math":currentScreen.equals("progress")||currentScreen.equals("flower")||currentScreen.equals("achievements")||currentScreen.equals("stats")?"progress":currentScreen.equals("exam")?"exam":currentScreen.equals("diary")||currentScreen.equals("letters")?"diary":currentScreen.equals("pomodoro")?"timer":"";
        String[] keys={"home","math","progress","exam","diary","timer"};
        for(int i=0;i<keys.length;i++){final String key=keys[i];boolean selected=key.equals(active);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(dp(1),dp(4),dp(1),dp(3));item.setBackground(selected?shape(PALE,14,0):shape(0x00FFFFFF,14,0));
            TextView icon=text(icons[i],22,selected?PINK:MUTED,true);icon.setGravity(Gravity.CENTER);icon.setIncludeFontPadding(false);item.addView(icon,lp(-1,29,0,0,0,2));
            TextView label=text(labels[i],10,selected?PINK:MUTED,selected);label.setGravity(Gravity.CENTER);label.setMaxLines(1);label.setSingleLine(true);label.setIncludeFontPadding(false);item.addView(label,lp(-1,19,0,0,0,0));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1f);p.setMargins(dp(1),0,dp(1),0);bar.addView(item,p);
            item.setOnClickListener(v->{if(key.equals("home"))showHome();else if(key.equals("math"))showChapters();else if(key.equals("progress"))showProgress();else if(key.equals("exam"))showExam();else if(key.equals("diary"))showDiary();else if(key.equals("timer"))showPomodoro();});
        }return bar;
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PICK_TOPIC_CAMERA&&resultCode==RESULT_OK&&data!=null&&data.getExtras()!=null){try{android.graphics.Bitmap photo=(android.graphics.Bitmap)data.getExtras().get("data");if(photo!=null){String suffix=topicStorageSuffix(selectedChapter,selectedTopic);java.io.File file=new java.io.File(getFilesDir(),"topic_photo_"+Math.abs(suffix.hashCode())+".jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);photo.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,out);out.close();prefs.edit().putString("topic_photo_file_"+suffix,file.getAbsolutePath()).remove("topic_photo_"+suffix).apply();showTopic(selectedTopic);}}catch(Exception e){Toast.makeText(this,"Не удалось сохранить фото",Toast.LENGTH_SHORT).show();}return;}if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();if(requestCode==PICK_BACKGROUND){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}prefs.edit().putString("custom_background_uri",uri.toString()).apply();showSettings();Toast.makeText(this,"Фото установлено как фон ♡",Toast.LENGTH_SHORT).show();}else if(requestCode==PICK_TOPIC_PHOTO){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}prefs.edit().putString("topic_photo_"+topicStorageSuffix(selectedChapter,selectedTopic),uri.toString()).apply();showTopic(selectedTopic);}else if(requestCode==WEEKLY_IMAGE_EXPORT){try{java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null||pendingWeeklyBitmap==null)throw new Exception();pendingWeeklyBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);out.close();pendingWeeklyBitmap.recycle();pendingWeeklyBitmap=null;Toast.makeText(this,"Итоги недели сохранены ♡",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Не удалось сохранить изображение",Toast.LENGTH_LONG).show();}}else if(requestCode==PDF_REPORT_EXPORT){try{writeProgressPdf(uri);Toast.makeText(this,"PDF-отчёт сохранён ♡",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Не удалось создать PDF-отчёт",Toast.LENGTH_LONG).show();}}else if(requestCode==BACKUP_EXPORT){try{java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null)throw new Exception();out.write(createBackupJson().getBytes("UTF-8"));out.close();Toast.makeText(this,"Резервная копия сохранена ♡",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Не удалось сохранить копию",Toast.LENGTH_LONG).show();}}else if(requestCode==BACKUP_IMPORT){try{java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))>0)bytes.write(buffer,0,n);in.close();restoreBackupJson(new String(bytes.toByteArray(),"UTF-8"));Toast.makeText(this,"Данные восстановлены ♡",Toast.LENGTH_LONG).show();loadChapters();showHome();}catch(Exception e){Toast.makeText(this,"Файл копии не удалось прочитать",Toast.LENGTH_LONG).show();}}}

    private LinearLayout settingsGroup(String title) {
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0,dp(9),0,dp(13));
        addText(group,title,12,PINK,true);
        View line=new View(this);line.setBackgroundColor(isDarkAppearance()?0xFF49404B:PALE);
        group.addView(line,lp(-1,1,0,5,0,9));
        return group;
    }

    private void showSettings() {
        currentScreen="settings";
        base("Настройки ⚙","Всё нужное — в одном месте");

        LinearLayout profile=settingsGroup("ПРОФИЛЬ");
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextSize(16);name.setTextColor(DARK);
        name.setHint(translate("Как к тебе обращаться?"));name.setText(prefs.getString("display_name",""));
        name.setPadding(dp(14),dp(8),dp(14),dp(8));name.setBackground(shape(0xFFFFF9F7,15,0xFFEADDE2));
        profile.addView(name,lp(-1,50,0,2,0,8));
        addText(profile,"ФОРМА ОБРАЩЕНИЯ",11,PINK,true);
        String gender=prefs.getString("profile_gender","female");LinearLayout genderRow=new LinearLayout(this);genderRow.setOrientation(LinearLayout.HORIZONTAL);
        Button female=button("Девушка","female".equals(gender)),male=button("Парень","male".equals(gender));
        genderRow.addView(female,new LinearLayout.LayoutParams(0,dp(48),1f));genderRow.addView(male,new LinearLayout.LayoutParams(0,dp(48),1f));
        female.setOnClickListener(v->{prefs.edit().putString("profile_gender","female").apply();showSettings();});
        male.setOnClickListener(v->{prefs.edit().putString("profile_gender","male").apply();showSettings();});
        profile.addView(genderRow,lp(-1,48,0,4,0,8));
        Button saveName=button("Сохранить имя",true);saveName.setOnClickListener(v->{String value=name.getText().toString().trim();if(value.isEmpty()){name.setError("Введи имя");return;}prefs.edit().putString("display_name",value).apply();Toast.makeText(this,"Имя сохранено ♡",Toast.LENGTH_SHORT).show();});
        profile.addView(saveName,lp(-1,48,0,0,0,0));root.addView(profile,lp(-1,-2,0,0,0,4));

        LinearLayout appearance=settingsGroup("ВНЕШНИЙ ВИД");
        addText(appearance,"Тема приложения",14,DARK,true);
        addText(appearance,"Нажми на палитру, чтобы сразу применить оформление.",12,MUTED,false);
        String[] names={"Розовая","Лавандовая","Мятная","Ночная","Белая","Кофейная","Золотая","Люкс","Небесная","Персиковая","Красная","LV"};
        String[] keys={"pink","lavender","mint","night","white","brown","gold","luxury","sky","peach","red","lv"};
        int[] colors={0xFFC97891,0xFF8979B8,0xFF659B86,0xFF302735,0xFFFFFFFF,0xFF8B6046,0xFFC49318,0xFF30251F,0xFF5D91B5,0xFFD88968,0xFFB32635,0xFFD9C19A};
        android.widget.GridLayout palette=new android.widget.GridLayout(this);palette.setColumnCount(3);
        String activeTheme=prefs.getString("app_theme","pink");
        for(int i=0;i<keys.length;i++){final String key=keys[i];final int color=colors[i];final String label=names[i];LinearLayout option=new LinearLayout(this);option.setGravity(Gravity.CENTER_VERTICAL);option.setPadding(dp(8),dp(7),dp(5),dp(7));option.setBackground(shape(activeTheme.equals(key)?0xFFFFF0F4:0x00FFFFFF,15,activeTheme.equals(key)?PINK:0xFFEFE4E8));
            TextView swatch=text(activeTheme.equals(key)?"✓":"",12,0xFFFFFFFF,true);swatch.setGravity(Gravity.CENTER);swatch.setBackground(shape(color,30,activeTheme.equals(key)?PINK:0));option.addView(swatch,lp(28,28,0,0,7,0));TextView labelView=text(label,11,DARK,true);option.addView(labelView,new LinearLayout.LayoutParams(0,dp(30),1f));
            android.widget.GridLayout.LayoutParams gp=new android.widget.GridLayout.LayoutParams();gp.width=0;gp.height=dp(48);gp.columnSpec=android.widget.GridLayout.spec(i%3,1f);gp.setMargins(dp(2),dp(3),dp(2),dp(3));palette.addView(option,gp);
            option.setOnClickListener(v->{prefs.edit().putString("app_theme",key).apply();applySavedTheme();showSettings();});
        }
        appearance.addView(palette,lp(-1,-2,0,7,0,8));
        LinearLayout fontGroup=settingsGroup("ШРИФТ ПРИЛОЖЕНИЯ");
        addText(fontGroup,"Шрифт применяется ко всем экранам и полям ввода.",12,MUTED,false);
        String[] fontNames={"Современный","Округлый","С засечками","Моноширинный","Узкий","Средний"};
        String[] fontKeys={"sans","rounded","serif","mono","condensed","medium"};
        String[] fontFamilies={"sans-serif","sans-serif-rounded","serif","monospace","sans-serif-condensed","sans-serif-medium"};
        String activeFont=prefs.getString("app_font","sans");
        android.widget.GridLayout fontGrid=new android.widget.GridLayout(this);fontGrid.setColumnCount(2);
        for(int i=0;i<fontKeys.length;i++){final String key=fontKeys[i],family=fontFamilies[i];Button choice=button((activeFont.equals(key)?"✓  ":"")+fontNames[i],activeFont.equals(key));choice.setTypeface(Typeface.create(family,Typeface.BOLD));android.widget.GridLayout.LayoutParams fp=new android.widget.GridLayout.LayoutParams();fp.width=0;fp.height=dp(44);fp.columnSpec=android.widget.GridLayout.spec(i%2,1f);fp.setMargins(dp(3),dp(3),dp(3),dp(3));fp.height=dp(48);fontGrid.addView(choice,fp);choice.setOnClickListener(v->{prefs.edit().putString("app_font",key).apply();showSettings();});}
        fontGroup.addView(fontGrid,lp(-1,-2,0,4,0,0));appearance.addView(fontGroup,lp(-1,-2,0,4,0,6));
        Button systemTheme=button("◐  Цвета и тёмная тема — как в системе","system".equals(activeTheme));systemTheme.setOnClickListener(v->{prefs.edit().putString("app_theme","system").apply();applySavedTheme();showSettings();});appearance.addView(systemTheme,lp(-1, 48,0,3,0,0));
        Button choosePhoto=button("＋  Выбрать фото фона",false);choosePhoto.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,PICK_BACKGROUND);});
        appearance.addView(choosePhoto,lp(-1, 48,0,3,0,4));
        Button clearPhoto=button("Убрать фото фона",false);clearPhoto.setOnClickListener(v->{prefs.edit().remove("custom_background_uri").apply();showSettings();});appearance.addView(clearPhoto,lp(-1,48,0,0,0,0));
        root.addView(appearance,lp(-1,-2,0,0,0,4));

        LinearLayout language=settingsGroup("ЯЗЫК ПРИЛОЖЕНИЯ");
        addText(language,"После выбора язык применяется ко всем экранам приложения.",12,MUTED,false);
        String[] languageNames={"Русский","English","Azərbaycan dili"};String[] languageKeys={"ru","en","az"};String selectedLanguage=prefs.getString("app_language","ru");
        for(int i=0;i<languageKeys.length;i++){final String key=languageKeys[i];Button choice=button((selectedLanguage.equals(key)?"✓  ":"")+languageNames[i],selectedLanguage.equals(key));language.addView(choice,lp(-1,48,0,5,0,0));choice.setOnClickListener(v->{prefs.edit().putString("app_language",key).apply();showSettings();});}
        root.addView(language,lp(-1,-2,0,0,0,4));

        LinearLayout progress=settingsGroup("ОТОБРАЖЕНИЕ ПРОГРЕССА");
        addText(progress,"Выбери удобный формат отображения учебных результатов.",12,MUTED,false);
        String mode=prefs.getString("progress_display","percent");LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        Button percent=button("С процентами",!"slider".equals(mode));Button slider=button("Полосы прогресса","slider".equals(mode));
        LinearLayout.LayoutParams m1=new LinearLayout.LayoutParams(0,dp(48),1f);m1.setMargins(dp(2),dp(7),dp(3),0);modes.addView(percent,m1);
        LinearLayout.LayoutParams m2=new LinearLayout.LayoutParams(0,dp(48),1f);m2.setMargins(dp(3),dp(7),dp(2),0);modes.addView(slider,m2);
        percent.setOnClickListener(v->{prefs.edit().putString("progress_display","percent").apply();showSettings();});slider.setOnClickListener(v->{prefs.edit().putString("progress_display","slider").apply();showSettings();});
        progress.addView(modes,lp(-1,54,0,0,0,0));root.addView(progress,lp(-1,-2,0,0,0,4));

        LinearLayout weekly=settingsGroup("НЕДЕЛЬНАЯ ЦЕЛЬ");addText(weekly,"Сколько минут учёбы ты хочешь набрать за неделю?",12,MUTED,false);EditText weeklyInput=new EditText(this);weeklyInput.setSingleLine(true);weeklyInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);weeklyInput.setText(String.valueOf(prefs.getInt("weekly_goal_minutes",300)));weeklyInput.setHint("Например, 300");weeklyInput.setTextColor(DARK);weeklyInput.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));weekly.addView(weeklyInput,lp(-1,48,0,6,0,7));Button saveWeekly=button("Сохранить цель недели",true);saveWeekly.setOnClickListener(v->{try{int value=Integer.parseInt(weeklyInput.getText().toString().trim());if(value<30||value>10080)throw new Exception();prefs.edit().putInt("weekly_goal_minutes",value).apply();Toast.makeText(this,"Недельная цель обновлена ♡",Toast.LENGTH_SHORT).show();}catch(Exception e){weeklyInput.setError("Укажи от 30 до 10080 минут");}});weekly.addView(saveWeekly,lp(-1,43,0,0,0,0));root.addView(weekly,lp(-1,-2,0,0,0,4));

        LinearLayout reminders=settingsGroup("НАПОМИНАНИЯ");
        CheckBox enabled=new CheckBox(this);enabled.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));enabled.setText(translate("Включить напоминания о занятиях"));enabled.setTextSize(14);enabled.setTextColor(DARK);enabled.setChecked(prefs.getBoolean("notifications_enabled",true));reminders.addView(enabled,lp(-1,48,0,0,0,4));
        enabled.setOnCheckedChangeListener((buttonView,checked)->{prefs.edit().putBoolean("notifications_enabled",checked).apply();scheduleReminders(this);if(checked&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},20);});
        CheckBox quietToggle=new CheckBox(this);quietToggle.setText("Тихие часы для уведомлений");quietToggle.setTextColor(DARK);quietToggle.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));quietToggle.setChecked(prefs.getBoolean("quiet_hours_enabled",false));reminders.addView(quietToggle,lp(-1, 48,0,4,0,0));quietToggle.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("quiet_hours_enabled",on).apply();scheduleReminders(this);});
        Button quietStart=button("Начало тишины · "+prefs.getString("quiet_hours_start","22:00"),false);Button quietEnd=button("Конец тишины · "+prefs.getString("quiet_hours_end","08:00"),false);quietStart.setOnClickListener(v->chooseQuietTime("quiet_hours_start"));quietEnd.setOnClickListener(v->chooseQuietTime("quiet_hours_end"));reminders.addView(quietStart,lp(-1,48,0,3,0,3));reminders.addView(quietEnd,lp(-1, 48,0,0,0,5));
        Button reminderSettings=button("Настроить дни и время",false);reminderSettings.setOnClickListener(v->showReminderSettings());reminders.addView(reminderSettings,lp(-1, 48,0,0,0,0));root.addView(reminders,lp(-1,-2,0,0,0,4));

        LinearLayout focus=settingsGroup("РЕЖИМ ФОКУСА");
        addText(focus,"Временно отключает учебные напоминания.",12,MUTED,false);CheckBox focusToggle=new CheckBox(this);focusToggle.setText("Не отвлекать");focusToggle.setTextColor(DARK);focusToggle.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));focusToggle.setChecked(prefs.getBoolean("do_not_disturb",false));focus.addView(focusToggle,lp(-1, 48,0,0,0,0));focusToggle.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("do_not_disturb",on).apply();scheduleReminders(this);});root.addView(focus,lp(-1,-2,0,0,0,4));

        LinearLayout backup=settingsGroup("РЕЗЕРВНАЯ КОПИЯ");
        addText(backup,"Сохрани уроки, цели, дневник и прогресс в один файл.",12,MUTED,false);
        Button export=button("Экспортировать копию",true);export.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"study_with_love_backup.json");startActivityForResult(i,BACKUP_EXPORT);});backup.addView(export,lp(-1, 48,0,7,0,5));
        Button restore=button("Восстановить из копии",false);restore.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,BACKUP_IMPORT);});backup.addView(restore,lp(-1,48,0,0,0,0));root.addView(backup,lp(-1,-2,0,0,0,4));

        Button back=button("← Назад",true);back.setOnClickListener(v->handleBackNavigation());root.addView(back,lp(-1,48,0,8,0,4));
    }

    private void updateSystemBarIcons() {
        if (Build.VERSION.SDK_INT >= 23 && getWindow() != null) {
            String t=prefs==null?"pink":prefs.getString("app_theme","pink");
            boolean darkIcons=!("night".equals(t)||"luxury".equals(t)||("system".equals(t)&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES));
            if (Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c=getWindow().getInsetsController();
                if(c!=null){int flags=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS; c.setSystemBarsAppearance(darkIcons?flags:0,flags);}
            } else {
                getWindow().getDecorView().setSystemUiVisibility(darkIcons?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0);
            }
        }
    }

    private void applySavedTheme() {
        String theme = prefs == null ? "pink" : prefs.getString("app_theme", "pink");
        if ("system".equals(theme)) {
            boolean darkMode=(getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
            if(Build.VERSION.SDK_INT>=31){try{int accent=getResources().getIdentifier("system_accent1_600","color","android"),bg=getResources().getIdentifier(darkMode?"system_neutral1_900":"system_neutral1_10","color","android"),surface=getResources().getIdentifier(darkMode?"system_neutral1_800":"system_neutral1_50","color","android"),fg=getResources().getIdentifier(darkMode?"system_neutral1_50":"system_neutral1_900","color","android");PINK=getResources().getColor(accent,getTheme());BG=getResources().getColor(bg,getTheme());PALE=getResources().getColor(surface,getTheme());DARK=getResources().getColor(fg,getTheme());MUTED=darkMode?0xFFBDB5C1:0xFF827A83;}catch(Exception e){BG=darkMode?0xFF17151D:0xFFFFF9F7;PINK=0xFFC97891;PALE=darkMode?0xFF302735:0xFFFCEEF2;DARK=darkMode?0xFFF4EAF0:0xFF3F3439;MUTED=darkMode?0xFFB6A8B2:0xFF95878C;}}
            else if(darkMode){BG=0xFF17151D;PINK=0xFFE39AB5;PALE=0xFF302735;DARK=0xFFF4EAF0;MUTED=0xFFB6A8B2;}
            else{BG=0xFFFFF9F7;PINK=0xFFC97891;PALE=0xFFFCEEF2;DARK=0xFF3F3439;MUTED=0xFF95878C;}
        }
        else if ("night".equals(theme)) { BG=0xFF17151D; PINK=0xFFE39AB5; PALE=0xFF302735; DARK=0xFFF4EAF0; MUTED=0xFFB6A8B2; }
        else if ("lavender".equals(theme)) { BG=0xFFF8F6FF; PINK=0xFF8979B8; PALE=0xFFF0ECFA; DARK=0xFF393448; MUTED=0xFF8E879E; }
        else if ("mint".equals(theme)) { BG=0xFFF5FAF7; PINK=0xFF659B86; PALE=0xFFEAF5EF; DARK=0xFF34443D; MUTED=0xFF84958C; }
        else if ("white".equals(theme)) { BG=0xFFF7F8FA; PINK=0xFF65758B; PALE=0xFFE9EDF2; DARK=0xFF303844; MUTED=0xFF87919E; }
        else if ("brown".equals(theme)) { BG=0xFFF7F1EA; PINK=0xFF8B6046; PALE=0xFFEDE0D2; DARK=0xFF49382E; MUTED=0xFF9B8778; }
        else if ("gold".equals(theme)) { BG=0xFFFFF8DF; PINK=0xFFC49318; PALE=0xFFF5E2A6; DARK=0xFF49340B; MUTED=0xFF92712C; }
        else if ("luxury".equals(theme)) { BG=0xFF211B18; PINK=0xFFD6B36A; PALE=0xFF3A3028; DARK=0xFFF3E7D0; MUTED=0xFFB7A68B; }
        else if ("sky".equals(theme)) { BG=0xFFF1F8FC; PINK=0xFF5D91B5; PALE=0xFFE1EFF8; DARK=0xFF304656; MUTED=0xFF8199A8; }
        else if ("peach".equals(theme)) { BG=0xFFFFF5EF; PINK=0xFFD88968; PALE=0xFFFBE5D9; DARK=0xFF513D34; MUTED=0xFFA88D80; }
        else if ("red".equals(theme)) { BG=0xFFFFF5F4; PINK=0xFFB32635; PALE=0xFFF8E2E3; DARK=0xFF4B2026; MUTED=0xFF9D777B; }
        else if ("lv".equals(theme)) { BG=0xFFF4E8D5; PINK=0xFF76502F; PALE=0xFFE7D2B3; DARK=0xFF3B281C; MUTED=0xFF92775E; }
        else { BG=0xFFFFF9F7; PINK=0xFFC97891; PALE=0xFFFCEEF2; DARK=0xFF3F3439; MUTED=0xFF95878C; }
        applyAccentOverlay(theme);
        if (getWindow() != null) { if(Build.VERSION.SDK_INT>=30){getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);getWindow().setDecorFitsSystemWindows(false);if(Build.VERSION.SDK_INT>=29){getWindow().setStatusBarContrastEnforced(false);getWindow().setNavigationBarContrastEnforced(false);}}else{getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);} updateSystemBarIcons(); }
    }
    private void applyAccentOverlay(String theme){
        int style=R.style.AppAccentPink;
        if("lavender".equals(theme))style=R.style.AppAccentLavender;
        else if("mint".equals(theme))style=R.style.AppAccentMint;
        else if("night".equals(theme))style=R.style.AppAccentNight;
        else if("white".equals(theme))style=R.style.AppAccentWhite;
        else if("brown".equals(theme))style=R.style.AppAccentBrown;
        else if("gold".equals(theme))style=R.style.AppAccentGold;
        else if("luxury".equals(theme))style=R.style.AppAccentLuxury;
        else if("sky".equals(theme))style=R.style.AppAccentSky;
        else if("peach".equals(theme))style=R.style.AppAccentPeach;
        else if("red".equals(theme))style=R.style.AppAccentRed;
        else if("lv".equals(theme))style=R.style.AppAccentLv;
        getTheme().applyStyle(style,true);
    }

        private void showHome() {
        currentScreen = "home"; StudyWidgetProvider.updateAll(this);
        base("Привет, " + prefs.getString("display_name", "Назка") + "! ♡", "Сегодня — хороший день, чтобы сделать маленький шаг к своей цели.");
        int done = prefs.getInt("done_count", 0);
        int streak = currentStreak();
        int record = prefs.getInt("longest_streak", 0);
        int totalTopics = 0, completedTopics = 0;
        for (Map.Entry<String,List<String>> entry : chapters.entrySet()) for (String topic : entry.getValue()) {
            if(!isTopicInExamScope(entry.getKey(),topic))continue;
            totalTopics++;
            if (topicProgressForChapter(entry.getKey(),topic) == 100) completedTopics++;
        }
        int overall = totalTopics == 0 ? 0 : completedTopics * 100 / totalTopics;
        if(prefs.getBoolean("exam_topic_filter_enabled",false))done=completedTopics;

        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(20), dp(20), dp(18));
        hero.setBackground(shape(0xFFF8E8EE, 27, 0));
        addText(hero, "ТВОЙ ПРОГРЕСС", 12, PINK, true);
        addText(hero, "Ты справляешься.\nПродолжай в своём темпе.", 24, DARK, true);
        addText(hero, g("Каждое занятие — это забота о себе будущей.","Каждое занятие — это забота о себе будущем."), 14, MUTED, false);
         addText(hero, dailyMotivation(), 14, PINK, true);
         addText(hero, mascotMessage(), 14, DARK, true);
        LinearLayout progressHead = new LinearLayout(this);
        progressHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView progressTitle = text("Учебный план", 14, DARK, true);
        progressHead.addView(progressTitle, new LinearLayout.LayoutParams(0, dp(28), 1f));
        TextView progressValue = text("0%", 18, PINK, true);animateNumber(progressValue,overall,"%");
        progressHead.addView(progressValue);
        hero.addView(progressHead, lp(-1, 30, 0, 12, 0, 4));
        ProgressBar overallBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        overallBar.setMax(100); overallBar.setProgress(0);overallBar.post(()->animateProgress(overallBar,overall));
        if (Build.VERSION.SDK_INT >= 21) {
            overallBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
            overallBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFDCE6));
        }
        hero.addView(overallBar, lp(-1, 9, 0, 0, 0, 5));
        addText(hero, completedTopics + " из " + totalTopics + " тем завершено", 13, MUTED, false);
        root.addView(hero, lp(-1, -2, 0, 0, 0, 15));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        String[][] values = {{String.valueOf(streak), "дней подряд"}, {String.valueOf(done), "уроков"}, {String.valueOf(record), "личный рекорд"}};
        int[] colors = {0xFFFFF0F4, 0xFFF1F0FF, 0xFFEFF7F1};
        for (int i = 0; i < values.length; i++) {
            String[] value = values[i];
            LinearLayout stat = card(); stat.setGravity(Gravity.CENTER);
            stat.setPadding(dp(5), dp(7), dp(5), dp(7));
            stat.setBackground(shape(i == 0 || i == 2 ? 0xFFFFF0F4 : colors[i], 22, 0));
            TextView number = text("0", 24, PINK, true); number.setGravity(Gravity.CENTER);animateNumber(number,Integer.parseInt(value[0]),"");
            stat.addView(number, lp(-1, 30, 0, 0, 0, 3));
            TextView caption = text(value[1], 12, MUTED, false); caption.setGravity(Gravity.CENTER); caption.setMaxLines(2);
            stat.addView(caption, lp(-1, 38, 0, 0, 0, 0));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(102), 1f);
            p.setMargins(dp(3), 0, dp(3), 0); stats.addView(stat, p);
        }
        root.addView(stats, lp(-1, -2, 0, 0, 0, 12));
        int weeklyDone=weeklyMinutesLast7Days(),weeklyGoal=prefs.getInt("weekly_goal_minutes",300);LinearLayout weeklyCard=card();weeklyCard.setOrientation(LinearLayout.HORIZONTAL);weeklyCard.setGravity(Gravity.CENTER_VERTICAL);weeklyCard.setPadding(dp(14),dp(12),dp(14),dp(12));WeeklyGoalRing ring=new WeeklyGoalRing(this,weeklyDone,weeklyGoal);weeklyCard.addView(ring,lp(82,82,0,0,13,0));LinearLayout weeklyWords=new LinearLayout(this);weeklyWords.setOrientation(LinearLayout.VERTICAL);addText(weeklyWords,"ЦЕЛЬ НЕДЕЛИ",11,PINK,true);addText(weeklyWords,weeklyDone+" / "+weeklyGoal+" минут",17,DARK,true);addText(weeklyWords,"Осталось "+Math.max(0,weeklyGoal-weeklyDone)+" минут до цели ♡",11,MUTED,false);weeklyCard.addView(weeklyWords,new LinearLayout.LayoutParams(0,-2,1f));root.addView(weeklyCard,lp(-1,-2,0,0,0,16));

        TextView more = text("ТВОИ УЧЕБНЫЕ РИТУАЛЫ", 13, PINK, true);
        more.setLetterSpacing(0.08f);
        root.addView(more, lp(-1, -2, 0, 16, 0, 10));
        LinearLayout ritualRow1 = new LinearLayout(this); ritualRow1.setOrientation(LinearLayout.HORIZONTAL);
        ritualTile(ritualRow1, "♡", "Дневник успехов", "Записывай победы", this::showDiary);
        ritualTile(ritualRow1, "✿", "Мой цветочек", "Растёт с тобой", this::showFlower);
        root.addView(ritualRow1, lp(-1, -2, 0, 0, 0, 9));
        LinearLayout ritualRow2 = new LinearLayout(this); ritualRow2.setOrientation(LinearLayout.HORIZONTAL);
        ritualTile(ritualRow2, "✧", "Достижения", "Твои маленькие победы", this::showAchievements);
        ritualTile(ritualRow2, "✉", "Письмо себе", "Послание в будущее", this::showFutureLetters);
        root.addView(ritualRow2, lp(-1, -2, 0, 0, 0, 9));
        LinearLayout ritualRow3 = new LinearLayout(this); ritualRow3.setOrientation(LinearLayout.HORIZONTAL);
        ritualTile(ritualRow3, "▦", "Календарь привычек", "Отмечай свои занятия", this::showProgress);
        ritualTile(ritualRow3, "☁", "Атмосфера учёбы", "Уютное настроение", this::showAtmosphere);
        root.addView(ritualRow3, lp(-1, -2, 0, 0, 0, 9));
         LinearLayout ritualRow4 = new LinearLayout(this); ritualRow4.setOrientation(LinearLayout.HORIZONTAL);
         ritualTile(ritualRow4, "▣", "Планировщик", "План на сегодня", this::showPlanner);
         ritualTile(ritualRow4, "📊", "Статистика", "Подробный разбор", this::showDetailedStats);
         root.addView(ritualRow4, lp(-1, -2, 0, 0, 0, 14));
         LinearLayout ritualRow5 = new LinearLayout(this); ritualRow5.setOrientation(LinearLayout.HORIZONTAL);
         ritualTile(ritualRow5, "🧮", "Математическая викторина", "Играй, повышай уровни и открывай награды", this::showMathQuiz);
         root.addView(ritualRow5, lp(-1, -2, 0, 0, 0, 9));
         LinearLayout ritualRow6 = new LinearLayout(this); ritualRow6.setOrientation(LinearLayout.HORIZONTAL);
         ritualTile(ritualRow6, "♡", "Напарник по учёбе", "Общие недельные цели и тёплые стикеры", this::showStudyBuddy);
         root.addView(ritualRow6, lp(-1, -2, 0, 0, 0, 14));
         LinearLayout ritualRow7 = new LinearLayout(this); ritualRow7.setOrientation(LinearLayout.HORIZONTAL);
         ritualTile(ritualRow7, "✧", "ИИ-помощник", "Объясняет темы и разбирает задачи", () -> launchAiTutor());
         root.addView(ritualRow7, lp(-1, -2, 0, 0, 0, 14));

        LinearLayout reminder = card();
        reminder.setOrientation(LinearLayout.HORIZONTAL); reminder.setGravity(Gravity.CENTER_VERTICAL);
        reminder.setBackground(shape(0xFFF3F0FF, 22, 0));
        LinearLayout reminderWords = new LinearLayout(this); reminderWords.setOrientation(LinearLayout.VERTICAL);
        addText(reminderWords, "НАПОМИНАНИЯ", 12, PINK, true);
        addText(reminderWords, reminderSummary(), 13, DARK, false);
        reminder.addView(reminderWords, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView reminderArrow = text("›", 29, PINK, true); reminderArrow.setGravity(Gravity.CENTER);
        reminder.addView(reminderArrow, lp(30, 40, 5, 0, 0, 0));
        reminder.setOnClickListener(v -> showReminderSettings());
        root.addView(reminder, lp(-1, -2, 0, 2, 0, 0));

        TextView made = text("Сделано с любовью ♡", 12, MUTED, false);
        made.setGravity(Gravity.CENTER);
        root.addView(made, lp(-1, 48, 0, 12, 0, 0));
        animateHomeCascade();
    }

    // Endless arithmetic challenge. Best scores and unlocked awards are stored locally.
    private final Random quizRandom = new Random();
    private final Handler quizHandler = new Handler(Looper.getMainLooper());
    private Runnable quizTick;
    private boolean quizActive = false, quizTimed = true;
    private int quizDifficulty = 0, quizScore = 0, quizStreak = 0, quizBestStreak = 0;
    private int quizAnswer = 0, quizSeconds = 15, quizLevel = 1;
    private long quizDeadline = 0L;
    private TextView quizTimerLabel, quizQuestionLabel, quizScoreLabel, quizLevelLabel;
    private ProgressBar quizTimerBar;

    private int quizTimeForDifficulty() { return quizDifficulty == 0 ? 15 : quizDifficulty == 1 ? 10 : 6; }
    private String quizDifficultyName() { return quizDifficulty == 0 ? "Лёгкая" : quizDifficulty == 1 ? "Средняя" : "Тяжёлая"; }
    private String quizModeName() { return quizTimed ? "С таймером" : "Без времени"; }

    private void showMathQuiz() {
        stopQuizTimer();
        quizActive = false;
        currentScreen = "math_quiz";
        base("Математическая викторина 🧮", "Бесконечная практика в твоём темпе");
        LinearLayout intro = card();
        intro.setPadding(dp(18),dp(18),dp(18),dp(18));
        intro.setBackground(shape(0xFFFFF0F4,24,0));
        addText(intro,"ТВОЙ МАТЕМАТИЧЕСКИЙ ЧЕЛЛЕНДЖ",11,PINK,true);
        addText(intro,"Сколько примеров ты решишь подряд?",22,DARK,true);
        addText(intro,"Выбери режим и сложность. Набирай очки, проходи уровни и собирай достижения. Ошибка завершит забег.",13,MUTED,false);
        root.addView(intro,lp(-1,-2,0,0,0,14));
        addText(root,"РЕЖИМ ИГРЫ",12,PINK,true);
        LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        Button timedMode=button("⏱ С таймером",quizTimed),freeMode=button("☁ Без времени",!quizTimed);
        LinearLayout.LayoutParams modeLp=new LinearLayout.LayoutParams(0,dp(48),1f);modeLp.setMargins(0,0,dp(5),0);modes.addView(timedMode,modeLp);
        LinearLayout.LayoutParams freeLp=new LinearLayout.LayoutParams(0,dp(48),1f);freeLp.setMargins(dp(5),0,0,0);modes.addView(freeMode,freeLp);
        timedMode.setOnClickListener(v->{quizTimed=true;showMathQuiz();});freeMode.setOnClickListener(v->{quizTimed=false;showMathQuiz();});
        root.addView(modes,lp(-1,50,0,0,0,14));
        addText(root,"ВЫБЕРИ СЛОЖНОСТЬ",12,PINK,true);
        String[] names={"🌱  Лёгкая","⚡  Средняя","🔥  Тяжёлая"};
        String[] details={"До 20 · таблица умножения 2–5 · простые примеры","До 100 · умножение и деление на числа до 12","До 500 · крупные числа · умножение до 30"};
        for(int i=0;i<3;i++){
            final int d=i;
            LinearLayout item=card();item.setPadding(dp(15),dp(12),dp(15),dp(12));
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
            addText(words,names[i],16,DARK,true);addText(words,details[i],12,MUTED,false);
            line.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
            TextView arrow=text("›",27,PINK,true);line.addView(arrow);item.addView(line);
            item.setOnClickListener(v->{quizDifficulty=d;startMathQuiz();});
            root.addView(item,lp(-1,-2,0,0,0,9));
        }
        LinearLayout stats=card();stats.setPadding(dp(15),dp(13),dp(15),dp(13));
        addText(stats,"ТВОЙ РЕКОРД",11,PINK,true);
        addText(stats,prefs.getInt("quiz_best_score",0)+" очков  ·  "+prefs.getInt("quiz_total_correct",0)+" правильных ответов за всё время",14,DARK,true);
        root.addView(stats,lp(-1,-2,0,8,0,10));
        Button awards=button("🏅  Посмотреть достижения",false);awards.setOnClickListener(v->showMathQuizAchievements());root.addView(awards,lp(-1,48,0,0,0,6));
        Button back=button("← Назад",false);back.setOnClickListener(v->handleBackNavigation());root.addView(back,lp(-1,48,0,0,0,0));
    }

    private void startMathQuiz() {
        stopQuizTimer();quizActive=true;quizScore=0;quizStreak=0;quizBestStreak=0;quizLevel=1;
        showMathQuizQuestion();
    }

    private int[] makeQuizProblem() {
        int scale=Math.min(40,quizLevel-1), op=quizRandom.nextInt(4),a,b,result;
        int max=quizDifficulty==0?Math.min(30,20+scale):quizDifficulty==1?Math.min(180,100+scale*2):Math.min(700,300+scale*10);
        if(op==0){a=1+quizRandom.nextInt(max);b=1+quizRandom.nextInt(max);result=a+b;return new int[]{a,b,op,result};}
        if(op==1){a=1+quizRandom.nextInt(max);b=1+quizRandom.nextInt(a);result=a-b;return new int[]{a,b,op,result};}
        if(op==2){
            int low=quizDifficulty==0?2:quizDifficulty==1?2:12;
            int high=quizDifficulty==0?5:quizDifficulty==1?12:30;
            a=low+quizRandom.nextInt(high-low+1);b=low+quizRandom.nextInt(high-low+1);
            if(quizDifficulty==2&&quizRandom.nextBoolean())a+=quizRandom.nextInt(20);
            result=a*b;return new int[]{a,b,op,result};
        }
        int divisor,quotient;
        if(quizDifficulty==0){divisor=2+quizRandom.nextInt(4);quotient=2+quizRandom.nextInt(9);}
        else if(quizDifficulty==1){divisor=2+quizRandom.nextInt(11);quotient=5+quizRandom.nextInt(16);}
        else {divisor=12+quizRandom.nextInt(19);quotient=12+quizRandom.nextInt(29);}
        a=divisor*quotient;b=divisor;result=quotient;return new int[]{a,b,op,result};
    }

    private void showMathQuizQuestion() {
        currentScreen="math_quiz";
        int[] problem=makeQuizProblem();quizAnswer=problem[3];
        String sign=problem[2]==0?"+":problem[2]==1?"−":problem[2]==2?"×":"÷";
        base("Уровень "+quizLevel+"  ·  "+quizDifficultyName(),"Режим: "+quizModeName()+(quizTimed?" · реши до окончания времени":" · решай в своём темпе"));
        LinearLayout top=card();top.setPadding(dp(15),dp(12),dp(15),dp(12));
        LinearLayout topLine=new LinearLayout(this);topLine.setGravity(Gravity.CENTER_VERTICAL);
        quizScoreLabel=text("⭐ "+quizScore+" очков",14,DARK,true);topLine.addView(quizScoreLabel,new LinearLayout.LayoutParams(0,-2,1f));
        quizLevelLabel=text("УРОВЕНЬ "+quizLevel,12,PINK,true);topLine.addView(quizLevelLabel);top.addView(topLine);
        root.addView(top,lp(-1,-2,0,0,0,12));
        quizTimerLabel=null;quizTimerBar=null;
        if(quizTimed){
            LinearLayout timerCard=card();timerCard.setGravity(Gravity.CENTER);timerCard.setPadding(dp(14),dp(14),dp(14),dp(14));
            quizTimerLabel=text(quizTimeForDifficulty()+" сек",30,PINK,true);quizTimerLabel.setGravity(Gravity.CENTER);
            timerCard.addView(quizTimerLabel,lp(-1, 48,0,0,0,5));
            quizTimerBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);quizTimerBar.setMax(1000);quizTimerBar.setProgress(1000);
            if(Build.VERSION.SDK_INT>=21){quizTimerBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));quizTimerBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA));}
            timerCard.addView(quizTimerBar,lp(-1,8,0,0,0,0));root.addView(timerCard,lp(-1,-2,0,0,0,18));
        }else{
            LinearLayout relaxed=card();relaxed.setGravity(Gravity.CENTER);addText(relaxed,"☁  Время не ограничено",14,PINK,true);root.addView(relaxed,lp(-1,48,0,0,0,14));
        }
        LinearLayout question=card();question.setGravity(Gravity.CENTER);question.setPadding(dp(12),dp(25),dp(12),dp(25));
        addText(question,"СКОЛЬКО БУДЕТ?",11,MUTED,true);
        quizQuestionLabel=text(problem[0]+" "+sign+" "+problem[1]+" = ?",36,DARK,true);quizQuestionLabel.setGravity(Gravity.CENTER);
        question.addView(quizQuestionLabel,lp(-1,72,0,5,0,0));root.addView(question,lp(-1,-2,0,0,0,16));
        List<Integer> choices=new ArrayList<>();choices.add(quizAnswer);
        while(choices.size()<4){int delta=1+quizRandom.nextInt(Math.max(4,quizDifficulty==0?8:quizDifficulty==1?20:40));int candidate=quizAnswer+(quizRandom.nextBoolean()?delta:-delta);if(candidate>=0&&!choices.contains(candidate))choices.add(candidate);}
        java.util.Collections.shuffle(choices,quizRandom);
        for(int i=0;i<4;i+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=i;j<i+2;j++){final int answer=choices.get(j);Button option=button(String.valueOf(answer),false);option.setTextSize(20);option.setOnClickListener(v->{if(!quizActive)return;boolean correct=answer==quizAnswer;option.setBackground(shape(correct?0xFFE4F4E8:0xFFFFE8E8,22,correct?0xFF75B987:0xFFE58A8A));option.setTextColor(correct?0xFF287A43:0xFFB64242);if(!correct){for(int k=0;k<row.getChildCount();k++){View child=row.getChildAt(k);if(child instanceof Button&&((Button)child).getText().toString().equals(String.valueOf(quizAnswer))){child.setBackground(shape(0xFFE4F4E8,22,0xFF75B987));((Button)child).setTextColor(0xFF287A43);}}}haptic(option,correct);new Handler(Looper.getMainLooper()).postDelayed(()->submitMathQuizAnswer(answer),250);});LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,dp(62),1f);op.setMargins(dp(3),0,dp(3),0);row.addView(option,op);}
            root.addView(row,lp(-1,-2,0,0,0,8));
        }
        TextView streak=text("🔥 Серия правильных ответов: "+quizStreak,13,PINK,true);streak.setGravity(Gravity.CENTER);root.addView(streak,lp(-1, 48,0,8,0,0));
        if(quizTimed){
            quizSeconds=quizTimeForDifficulty();quizDeadline=SystemClock.elapsedRealtime()+quizSeconds*1000L;
            quizTick=()->{
                if(!quizActive||!"math_quiz".equals(currentScreen))return;
                long left=quizDeadline-SystemClock.elapsedRealtime();
                if(left<=0){if(quizTimerLabel!=null)quizTimerLabel.setText("0 сек");if(quizTimerBar!=null)quizTimerBar.setProgress(0);endMathQuiz(true);return;}
                int sec=(int)((left+999)/1000);if(quizTimerLabel!=null)quizTimerLabel.setText(sec+" сек");
                if(quizTimerBar!=null)quizTimerBar.setProgress((int)Math.max(0,Math.min(1000,left*1000L/(quizSeconds*1000L))));
                quizHandler.postDelayed(quizTick,80);
            };
            quizHandler.post(quizTick);
        }
    }

    private void submitMathQuizAnswer(int answer) {
        if(!quizActive)return;
        stopQuizTimer();
        if(answer!=quizAnswer){endMathQuiz(false);return;}
        quizScore+=10;quizStreak++;quizBestStreak=Math.max(quizBestStreak,quizStreak);haptic(root,true);
        prefs.edit().putInt("quiz_total_correct",prefs.getInt("quiz_total_correct",0)+1).apply();
        if(quizScore>prefs.getInt("quiz_best_score",0)){prefs.edit().putInt("quiz_best_score",quizScore).apply();celebrate();}
        quizLevel=quizScore/50+1;
        unlockQuizAwards();
        showMathQuizQuestion();
    }

    private void stopQuizTimer(){if(quizTick!=null){quizHandler.removeCallbacks(quizTick);quizTick=null;}}

    private void endMathQuiz(boolean timeout) {
        if(!quizActive)return;quizActive=false;stopQuizTimer();
        unlockQuizAwards();
        currentScreen="math_quiz_result";
        base(timeout?"Время вышло! ⏰":"Игра окончена",timeout?"Нужно отвечать чуть быстрее — попробуешь ещё раз?":"Ничего страшного! Ошибки помогают учиться.");
        LinearLayout result=card();result.setGravity(Gravity.CENTER);result.setPadding(dp(18),dp(22),dp(18),dp(22));
        TextView medal=text(quizScore>=200?"🏆":quizScore>=100?"🥇":quizScore>=50?"🥈":"🌸",54,PINK,true);medal.setGravity(Gravity.CENTER);result.addView(medal,lp(-1,76,0,0,0,8));
        addText(result,"ТВОЙ РЕЗУЛЬТАТ",12,PINK,true);
        TextView score=text(String.valueOf(quizScore),42,DARK,true);score.setGravity(Gravity.CENTER);result.addView(score,lp(-1,54,0,3,0,2));
        TextView points=text("очков",13,MUTED,false);points.setGravity(Gravity.CENTER);result.addView(points);
        addText(result,"Правильных ответов: "+(quizScore/10),14,DARK,true);
        addText(result,"Достигнутый уровень: "+quizLevel,14,DARK,true);
        addText(result,"Лучшая серия: "+quizBestStreak,14,DARK,true);
        addText(result,"Рекорд: "+prefs.getInt("quiz_best_score",0)+" очков",13,PINK,true);
        root.addView(result,lp(-1,-2,0,0,0,15));
        Button retry=button("↻  Играть ещё раз",true);retry.setOnClickListener(v->showMathQuiz());root.addView(retry,lp(-1,52,0,0,0,8));
        Button awards=button("🏅  Мои достижения",false);awards.setOnClickListener(v->showMathQuizAchievements());root.addView(awards,lp(-1,48,0,0,0,8));
        Button home=button("← Назад",false);home.setOnClickListener(v->handleBackNavigation());root.addView(home,lp(-1,48,0,0,0,0));
    }

    private void unlockQuizAward(String key){if(!prefs.getBoolean(key,false)){String date=new SimpleDateFormat("d MMMM yyyy",new Locale("ru")).format(new Date());prefs.edit().putBoolean(key,true).putString("achievement_date_"+key,date).apply();haptic(root,true);celebrate();}}
    private void unlockQuizAwards() {
        int total=prefs.getInt("quiz_total_correct",0),best=prefs.getInt("quiz_best_score",0);
        int[] thresholds={1,5,10,25,50,100};
        for(int i=0;i<thresholds.length;i++)if(total>=thresholds[i])unlockQuizAward("quiz_award_correct_"+thresholds[i]);
        if(quizBestStreak>=5)unlockQuizAward("quiz_award_streak_5");
        if(quizBestStreak>=10)unlockQuizAward("quiz_award_streak_10");
        if(best>=100)unlockQuizAward("quiz_award_score_100");
        if(best>=250)unlockQuizAward("quiz_award_score_250");
        if(quizDifficulty==2&&quizScore>=100)unlockQuizAward("quiz_award_hard");
    }

    private void showMathQuizAchievements() {
        stopQuizTimer();quizActive=false;currentScreen="math_quiz_awards";
        base("Награды викторины 🏅","Каждая открытая награда — твоя маленькая победа");
        int total=prefs.getInt("quiz_total_correct",0);
        addText(root,"ОТКРЫТО "+countQuizAwards()+" ИЗ 11",12,PINK,true);
        String[][] awards={
            {"🌱","Первый шаг","Решить первый пример","quiz_award_correct_1"},
            {"⭐","Пять побед","Дать 5 правильных ответов","quiz_award_correct_5"},
            {"🧠","Разминка окончена","Дать 10 правильных ответов","quiz_award_correct_10"},
            {"🔥","Двадцать пять","Дать 25 правильных ответов","quiz_award_correct_25"},
            {"💎","Полсотни","Дать 50 правильных ответов","quiz_award_correct_50"},
            {"👑","Сотня ответов","Дать 100 правильных ответов","quiz_award_correct_100"},
            {"⚡","На одном дыхании","Серия из 5 верных ответов","quiz_award_streak_5"},
            {"🚀","Без остановки","Серия из 10 верных ответов","quiz_award_streak_10"},
            {"🥇","Сотня очков","Набрать 100 очков за забег","quiz_award_score_100"},
            {"🏆","Большая победа","Набрать 250 очков за забег","quiz_award_score_250"},
            {"🔥","Эксперт","Набрать 100 очков на тяжёлой сложности","quiz_award_hard"}
        };
        for(String[] a:awards){
            boolean unlocked=prefs.getBoolean(a[3],false);
            LinearLayout item=card();item.setOrientation(LinearLayout.HORIZONTAL);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(13),dp(11),dp(13),dp(11));
            TextView icon=text(unlocked?a[0]:"🔒",27,unlocked?PINK:MUTED,true);item.addView(icon,lp(46,48,0,0,10,0));
            LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
            addText(words,a[1],14,unlocked?DARK:MUTED,true);addText(words,unlocked?"Открыто · "+a[2]:a[2],11,MUTED,false);
            item.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
            TextView state=text(unlocked?"✓":"···",17,unlocked?PINK:MUTED,true);item.addView(state);
            root.addView(item,lp(-1,-2,0,0,0,7));
        }
        Button back=button("← Назад",true);back.setOnClickListener(v->handleBackNavigation());root.addView(back,lp(-1,50,0,10,0,0));
    }

    private int countQuizAwards(){
        String[] keys={"quiz_award_correct_1","quiz_award_correct_5","quiz_award_correct_10","quiz_award_correct_25","quiz_award_correct_50","quiz_award_correct_100","quiz_award_streak_5","quiz_award_streak_10","quiz_award_score_100","quiz_award_score_250","quiz_award_hard"};
        int n=0;for(String key:keys)if(prefs.getBoolean(key,false))n++;return n;
    }

    private String dailyMotivation() {String last=prefs.getString("last_study_date","");if(!last.isEmpty()&&!todayKey().equals(last)&&!isYesterday(last))return "♡  Давно не занимались? Ничего страшного — начни с 5 минут. Ты справишься.";return "✧  "+messages[Math.floorMod(todayKey().hashCode(),messages.length)]; }
     private String word(int n) { int x=n%10, y=n%100; if(x==1 && y!=11)return "урок"; if(x>=2&&x<=4&&(y<12||y>14))return "урока"; return "уроков"; }
    private void showChapters() {
        currentScreen = "chapters";
        base("Уроки 📚", "Выбери предмет или создай свой урок");
        LinearLayout lessonHead=new LinearLayout(this);lessonHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView orderLabel=text("МОИ ПРЕДМЕТЫ",11,PINK,true);lessonHead.addView(orderLabel,new LinearLayout.LayoutParams(0,dp(38),1f));
        TextView search=text("⌕",24,PINK,true);search.setGravity(Gravity.CENTER);search.setBackground(shape(0xFFFFF0F4,14,0xFFF2DDE4));search.setElevation(dp(1));search.setContentDescription("Поиск уроков и тем");lessonHead.addView(search,lp(48,48,0,0,7,0));search.setOnClickListener(v->showSearchDialog());
        Button orderButton=button("⋮",false);orderButton.setTextSize(22);orderButton.setPadding(0,0,0,0);lessonHead.addView(orderButton,lp(48,48,0,0,0,0));root.addView(lessonHead,lp(-1, 48,0,0,0,8));
        orderButton.setOnClickListener(v->{android.widget.PopupMenu menu=new android.widget.PopupMenu(this,orderButton);menu.getMenu().add(0,1,0,"По умолчанию");menu.getMenu().add(0,2,1,"По названию А—Я");menu.getMenu().add(0,3,2,"Сначала новые");menu.getMenu().add(0,4,3,"По прогрессу");menu.setOnMenuItemClickListener(mi->{prefs.edit().putString("lesson_sort",mi.getItemId()==2?"alpha":mi.getItemId()==3?"new":mi.getItemId()==4?"progress":"default").apply();showChapters();return true;});menu.show();});
        Button weak=button("🧠  Мои слабые места · "+countMistakes(),false);weak.setOnClickListener(v->showWeaknessTrainer());root.addView(weak,lp(-1,48,0,0,0,10));
        List<String> mathTopics=chapters.get("1️⃣ Натуральные числа и действия с ними");int total=0,done=0;for(List<String> topics:chapters.values()){total+=topics.size();done+=countCompleted(topics);}
        int mathTotal=0,mathDone=0;for(Map.Entry<String,List<String>> e:chapters.entrySet())if(!e.getKey().startsWith("CUSTOM:")){mathTotal+=e.getValue().size();mathDone+=countCompleted(e.getValue());}
        LinearLayout math=card();math.setOrientation(LinearLayout.VERTICAL);math.setPadding(dp(15),dp(13),dp(15),dp(12));math.setOnClickListener(v->showMathChapters());
        LinearLayout mathTop=new LinearLayout(this);mathTop.setGravity(Gravity.CENTER_VERTICAL);TextView mathIcon=text("📐",27,PINK,true);mathTop.addView(mathIcon,lp(42,42,0,0,10,0));LinearLayout mathWords=new LinearLayout(this);mathWords.setOrientation(LinearLayout.VERTICAL);addText(mathWords,"Математика",16,DARK,true);addText(mathWords,"Все разделы и темы по математике",11,MUTED,false);mathTop.addView(mathWords,new LinearLayout.LayoutParams(0,-2,1f));TextView arrow=text("›",25,PINK,false);mathTop.addView(arrow);math.addView(mathTop,lp(-1,-2,0,0,0,7));
        addText(math,mathDone+" из "+mathTotal+" тем пройдено",10,MUTED,true);ProgressBar mathBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);mathBar.setMax(Math.max(1,mathTotal));mathBar.setProgress(mathDone);mathBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));mathBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF3E5EA));math.addView(mathBar,lp(-1,6,0,3,0,0));root.addView(math,lp(-1,-2,0,0,0,8));
        Set<String> ids=prefs.getStringSet("custom_lesson_ids",new TreeSet<String>());List<String> lessonOrder=new ArrayList<>(ids);String sort=prefs.getString("lesson_sort","default");if("alpha".equals(sort))java.util.Collections.sort(lessonOrder,(a,b)->prefs.getString("custom_lesson_name_"+a,"").compareToIgnoreCase(prefs.getString("custom_lesson_name_"+b,"")));else if("new".equals(sort))java.util.Collections.sort(lessonOrder,java.util.Collections.reverseOrder());else if("progress".equals(sort))java.util.Collections.sort(lessonOrder,(a,b)->{String na=prefs.getString("custom_lesson_name_"+a,""),nb=prefs.getString("custom_lesson_name_"+b,"");List<String> ta=chapters.get("CUSTOM:"+a+":"+na),tb=chapters.get("CUSTOM:"+b+":"+nb);int ca=ta==null?0:countCompleted(ta),cb=tb==null?0:countCompleted(tb);int sa=ta==null?0:ta.size(),sb=tb==null?0:tb.size();double pa=sa==0?0:(double)ca/sa,pb=sb==0?0:(double)cb/sb;return Double.compare(pb,pa);});
        for(String id:lessonOrder){String name=prefs.getString("custom_lesson_name_"+id,"Мой урок");List<String> topics=chapters.get("CUSTOM:"+id+":"+name);if(topics==null)topics=new ArrayList<>();int c=countCompleted("CUSTOM:"+id+":"+name,topics),n=topics.size();LinearLayout item=card();item.setOrientation(LinearLayout.VERTICAL);item.setPadding(dp(14),dp(11),dp(14),dp(11));LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView emoji=text(prefs.getString("custom_lesson_emoji_"+id,"📚"),25,PINK,true);row.addView(emoji,lp(40,40,0,0,9,0));LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);addText(words,name,15,DARK,true);addText(words,c+" из "+n+" тем пройдено",11,MUTED,false);row.addView(words,new LinearLayout.LayoutParams(0,-2,1f));TextView go=text("›",24,PINK,false);row.addView(go);item.addView(row);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,n));bar.setProgress(c);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF3E5EA));item.addView(bar,lp(-1,5,0,7,0,0));item.setOnClickListener(v->{selectedChapter="CUSTOM:"+id+":"+name;showTopics(selectedChapter);});item.setOnLongClickListener(v->{confirmDeleteCustomLesson(id,name);return true;});root.addView(item,lp(-1,-2,0,0,0,7));}
        Button add=button("＋  Добавить свой урок",true);add.setOnClickListener(v->addCustomLesson());root.addView(add,lp(-1,49,0,7,0,12));
        LinearLayout stats=card();stats.setPadding(dp(14),dp(12),dp(14),dp(12));addText(stats,"ТВОЙ УЧЕБНЫЙ РИТМ",11,PINK,true);addText(stats,done+" из "+total+" тем завершено",17,DARK,true);addText(stats,"Продолжай в своём темпе — каждый пройденный раздел приближает к цели ♡",11,MUTED,false);ProgressBar allBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);allBar.setMax(Math.max(1,total));allBar.setProgress(done);allBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));stats.addView(allBar,lp(-1,6,0,7,0,0));root.addView(stats,lp(-1,-2,0,0,0,10));
        String recent=prefs.getString("recent_topic","");String recentChapter=prefs.getString("recent_chapter","");if(!recent.isEmpty()&&!recentChapter.isEmpty()&&chapters.containsKey(recentChapter)){Button last=button("↗  Продолжить: "+recent,false);last.setOnClickListener(v->{selectedChapter=recentChapter;selectedTopic=recent;showTopic(recent);});root.addView(last,lp(-1,48,0,0,0,4));}
    }

    private void showMathChapters() {
        currentScreen = "math_chapters"; base("Математика 📐", "Выбери раздел и продолжай обучение");
        int number = 1;
        for (String chapter : chapters.keySet()) {
            if (chapter.startsWith("CUSTOM:")) continue;
            final String selected = chapter; String pretty = chapter.replaceFirst("^[^ ]+\\s*", "");
            actionRegular("РАЗДЕЛ " + number + "\n" + pretty, chapters.get(chapter).size() + " учебных тем", () -> { selectedChapter = selected; showTopics(selected); }); number++;
        }
        Button back = button("← К урокам", false); back.setOnClickListener(v -> handleBackNavigation()); root.addView(back, lp(-1, 50, 0, 12, 0, 0));
    }
    private void addCustomLesson() {
        final String[] chosenEmoji={"📚"};String[] emojis={"📚","📐","🌸","🧪","🌍","🎨","🎵","📝","💡","⭐","📖","✏️","🧠","🔬","🧬","🧮","🗺️","🏛️","🎭","🎹","🪴","🦋","☀️","🌙","💗","💜","🩵","🍀","🌷","🪻","🧸","🐱","🐰","🍓","🍒","🍉","🍰","🧁","☕","🫧","✨","🔥","🏆","🎯","💫","🪄","🧩","🪐","🌈","🦄"};
        LinearLayout dialogBody=new LinearLayout(this);dialogBody.setOrientation(LinearLayout.VERTICAL);dialogBody.setPadding(dp(20),dp(8),dp(20),dp(4));TextView hint=text("Создай отдельный предмет и добавляй в него свои темы. Например: английский, история или биология.",13,MUTED,false);hint.setLineSpacing(dp(3),1f);dialogBody.addView(hint,lp(-1,-2,0,0,0,10));
        LinearLayout emojiRow=new LinearLayout(this);emojiRow.setGravity(Gravity.CENTER_VERTICAL);String[] basicEmojis={"📚","📐","🌸","🧪"};TextView[] basicViews=new TextView[basicEmojis.length];
        Runnable refreshBasic=()->{for(int i=0;i<basicViews.length;i++){boolean on=chosenEmoji[0].equals(basicEmojis[i]);basicViews[i].setBackground(shape(on?0xFFFFE3EC:PALE,13,on?PINK:0));}};
        for(int i=0;i<basicEmojis.length;i++){final String emoji=basicEmojis[i];TextView option=text(emoji,24,DARK,false);option.setGravity(Gravity.CENTER);basicViews[i]=option;LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(48),1f);ep.setMargins(dp(2),0,dp(2),0);emojiRow.addView(option,ep);option.setOnClickListener(v->{chosenEmoji[0]=emoji;refreshBasic.run();});}
        TextView more=text("＋",25,PINK,true);more.setGravity(Gravity.CENTER);more.setContentDescription("Добавить эмодзи к теме");more.setBackground(shape(0xFFFFF0F4,13,0xFFF2DDE4));emojiRow.addView(more,lp(48,48,3,0,0,0));more.setOnClickListener(v->{android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(5);for(String emoji:emojis){TextView option=text(emoji,23,DARK,false);option.setGravity(Gravity.CENTER);option.setBackground(shape(chosenEmoji[0].equals(emoji)?0xFFFFE3EC:PALE,12,chosenEmoji[0].equals(emoji)?PINK:0));android.widget.GridLayout.LayoutParams ep=new android.widget.GridLayout.LayoutParams();ep.width=dp(48);ep.height=dp(44);ep.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(option,ep);option.setOnClickListener(q->{chosenEmoji[0]=emoji;refreshBasic.run();android.widget.Toast.makeText(this,"Эмодзи выбрано",Toast.LENGTH_SHORT).show();((android.app.Dialog)grid.getTag()).dismiss();});}android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.setPadding(dp(12),dp(8),dp(12),dp(8));scroll.addView(grid);android.app.AlertDialog picker=new android.app.AlertDialog.Builder(this).setTitle("Выбери эмодзи").setView(scroll).setNegativeButton("Закрыть",null).create();grid.setTag(picker);picker.show();});
        dialogBody.addView(emojiRow,lp(-1,52,0,0,0,10));EditText input = new EditText(this); input.setSingleLine(true); input.setTextSize(16);input.setPadding(dp(14),dp(8),dp(14),dp(8));input.setTextColor(DARK);input.setHint("Как назовём урок?");input.setBackground(shape(0xFFFFF9F7,15,0xFFEADDE2));dialogBody.addView(input,lp(-1,52,0,0,0,0));
        new android.app.AlertDialog.Builder(this).setTitle("♡  Добавить свой урок").setView(dialogBody)
            .setNegativeButton("Отмена", null).setPositiveButton("Создать урок", (d,w) -> {
                String name=input.getText().toString().trim(); if(name.isEmpty()){Toast.makeText(this,"Введи название урока",Toast.LENGTH_SHORT).show();return;}
                String id=String.valueOf(System.currentTimeMillis()); Set<String> ids=new TreeSet<>(prefs.getStringSet("custom_lesson_ids",new TreeSet<String>()));ids.add(id);
                prefs.edit().putStringSet("custom_lesson_ids",ids).putString("custom_lesson_name_"+id,name).putString("custom_lesson_emoji_"+id,chosenEmoji[0]).putStringSet("custom_lesson_topics_"+id,new TreeSet<String>()).apply();
                selectedChapter="CUSTOM:"+id+":"+name;chapters.put(selectedChapter,new ArrayList<String>());showTopics(selectedChapter);
            }).show();
    }
    private String topicContentKey(String lessonId,String topic){return "custom_topic_content_"+lessonId+"_"+android.util.Base64.encodeToString(topic.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);}
    private void openCustomTopicDialog(String chapter,String oldName,String oldContent){
        String[] parts=chapter.split(":",3);if(parts.length<3)return;String lessonId=parts[1];
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(8),dp(20),dp(4));
        TextView note=text("Название и материал темы можно будет изменить позже.",12,MUTED,false);body.addView(note,lp(-1,-2,0,0,0,8));
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextSize(16);name.setTextColor(DARK);name.setHint("Название темы");name.setText(oldName==null?"":oldName);name.setPadding(dp(13),dp(8),dp(13),dp(8));name.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));body.addView(name,lp(-1,50,0,0,0,9));
        EditText content=new EditText(this);content.setTextSize(15);content.setTextColor(DARK);content.setHint("Добавь конспект, объяснение или заметки…");content.setGravity(Gravity.TOP);content.setMinLines(4);content.setText(oldContent);content.setPadding(dp(13),dp(12),dp(13),dp(12));content.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));body.addView(content,lp(-1,118,0,0,0,0));
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle(oldName==null?"♡  Новая тема":"✎  Изменить тему").setView(body).setNegativeButton("Отмена",null).setPositiveButton(oldName==null?"Добавить":"Сохранить",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(q->{
            String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Введи название");name.requestFocus();return;}
            Set<String> topics=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+lessonId,new TreeSet<String>()));
            if(topics.contains(n)&&(oldName==null||!oldName.equals(n))){name.setError("Такая тема уже существует");name.requestFocus();return;}
            SharedPreferences.Editor e=prefs.edit();
            if(oldName!=null&&!oldName.equals(n)){topics.remove(oldName);topics.add(n);int oldProgress=prefs.getInt("progress_"+oldName,0);e.remove("progress_"+oldName).remove("done_"+oldName);if(oldProgress>0)e.putInt("progress_"+n,oldProgress);e.remove(topicContentKey(lessonId,oldName));}else topics.add(n);
            e.putStringSet("custom_lesson_topics_"+lessonId,topics).putString(topicContentKey(lessonId,n),content.getText().toString().trim()).apply();chapters.put(chapter,new ArrayList<>(topics));if(oldName!=null&&oldName.equals(selectedTopic))selectedTopic=n;dialog.dismiss();showTopics(chapter);
        }));
        dialog.show();
    }
    private void addCustomTopic(String chapter){openCustomTopicDialog(chapter,null,"");}

    private void showTopics(String chapter) {
        currentScreen = "topics";
        boolean customLesson = chapter.startsWith("CUSTOM:");
        base(customLesson ? chapter.split(":", 3)[2] : chapter, "Выбери тему, которую хочешь изучить:");
        List<String> topicOrder=new ArrayList<>(chapters.get(chapter));
        if(customLesson&&"alpha".equals(prefs.getString("lesson_sort","default")))java.util.Collections.sort(topicOrder,String.CASE_INSENSITIVE_ORDER);
        for(String topic:topicOrder){
            int percent=topicProgressForChapter(chapter,topic);
            LinearLayout topicItem=action((percent==100?"✓  ":"♡  ")+topic,"Изучено на "+percent+"%",()->{selectedTopic=topic;showTopic(topic);});
            if(customLesson)topicItem.setOnLongClickListener(v->{confirmDeleteCustomTopic(chapter,topic);return true;});
        }
        if(customLesson){Button addTopic=button("＋  Добавить тему",true);addTopic.setOnClickListener(v->addCustomTopic(chapter));root.addView(addTopic,lp(-1,50,0,10,0,8));}
        Button back=button(customLesson?"← К урокам":"← К разделам",false);back.setOnClickListener(v->handleBackNavigation());root.addView(back,lp(-1,52,0,12,0,0));
    }

    private void confirmDeleteCustomLesson(String id,String name){
        new android.app.AlertDialog.Builder(this).setTitle("Удалить предмет?")
            .setMessage("«"+name+"» и все его темы будут удалены. Это действие нельзя отменить.")
            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                Set<String> ids=new TreeSet<>(prefs.getStringSet("custom_lesson_ids",new TreeSet<String>()));ids.remove(id);
                Set<String> topics=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+id,new TreeSet<String>()));
                SharedPreferences.Editor e=prefs.edit().putStringSet("custom_lesson_ids",ids).remove("custom_lesson_name_"+id).remove("custom_lesson_emoji_"+id).remove("custom_lesson_topics_"+id);
                int removedCompleted=0;for(String t:topics){if(prefs.getInt("progress_"+t,0)>=100||prefs.getBoolean("done_"+t,false))removedCompleted++;e.remove(topicContentKey(id,t)).remove("progress_"+t).remove("done_"+t);}if(removedCompleted>0)e.putInt("done_count",Math.max(0,prefs.getInt("done_count",0)-removedCompleted));
                e.apply();chapters.remove("CUSTOM:"+id+":"+name);
                if(selectedChapter!=null&&selectedChapter.startsWith("CUSTOM:"+id+":")){selectedChapter=null;selectedTopic=null;}
                showChapters();Toast.makeText(this,"Предмет удалён",Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void confirmDeleteCustomTopic(String chapter,String topic){
        String[] parts=chapter.split(":",3);if(parts.length<2)return;String id=parts[1];
        new android.app.AlertDialog.Builder(this).setTitle("Удалить тему?")
            .setMessage("Тема «"+topic+"» и её учебный материал будут удалены.")
            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                Set<String> topics=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+id,new TreeSet<String>()));topics.remove(topic);
                prefs.edit().putStringSet("custom_lesson_topics_"+id,topics).remove(topicContentKey(id,topic)).remove("progress_"+topic).remove("done_"+topic).apply();
                chapters.put(chapter,new ArrayList<>(topics));showTopics(chapter);Toast.makeText(this,"Тема удалена",Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void showTopic(String topic) {
        currentScreen = "topic";
        if(selectedChapter!=null)prefs.edit().putString("recent_chapter",selectedChapter).putString("recent_topic",topic).apply();
        selectedTopic=topic;
        base("Твоя тема 📖", "Учись в своём темпе — каждый шаг важен");
        int percent = topicProgress(topic);
        LinearLayout box = card();
        addText(box, topic, 20, DARK, true);
        String customBody="";if(selectedChapter!=null&&selectedChapter.startsWith("CUSTOM:")){String[] customParts=selectedChapter.split(":",3);if(customParts.length>=2)customBody=prefs.getString(topicContentKey(customParts[1],topic),"");}addText(box,customBody.isEmpty()?"Открой соответствующий урок в учебнике, изучи материал и реши задания. Отмечай свой прогресс — он сохранится автоматически.":customBody,15,MUTED,false);
        Button mini=button("📝  Мини-тест по теме · 5 вопросов",true);mini.setOnClickListener(v->startMiniTest(topic));root.addView(mini,lp(-1,50,0,0,0,12));
        topicProgressLabel = text("ТВОЙ ПРОГРЕСС: " + percent + "%", 13, PINK, true); box.addView(topicProgressLabel, lp(-1, -2, 0, 0, 0, 5));
        topicProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); topicProgressBar.setMax(100); topicProgressBar.setProgress(percent);
        if (Build.VERSION.SDK_INT >= 21) { topicProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK)); topicProgressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA)); }
        box.addView(topicProgressBar, lp(-1, 8, 0, 0, 0, 5));
        root.addView(box, lp(-1, -2, 0, 0, 0, 14));
        addTopicStudyCard(topic);
        addTopicReflection(topic);
        addOwnQuestionCard(topic);
        addTopicPhoto(topic);
        if(selectedChapter!=null&&selectedChapter.startsWith("CUSTOM:")){String[] cp=selectedChapter.split(":",3);if(cp.length>=2){LinearLayout manage=new LinearLayout(this);Button editTopic=button("✎  Изменить содержание",false),deleteTopic=button("Удалить тему",false);manage.addView(editTopic,new LinearLayout.LayoutParams(0,dp(48),1f));manage.addView(deleteTopic,new LinearLayout.LayoutParams(0,dp(48),1f));editTopic.setOnClickListener(v->openCustomTopicDialog(selectedChapter,topic,prefs.getString(topicContentKey(cp[1],topic),"")));deleteTopic.setOnClickListener(v->{new android.app.AlertDialog.Builder(this).setTitle("Удалить тему?").setMessage("Тема и её содержание будут удалены.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{Set<String> ts=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+cp[1],new TreeSet<String>()));ts.remove(topic);prefs.edit().putStringSet("custom_lesson_topics_"+cp[1],ts).remove(topicContentKey(cp[1],topic)).remove("progress_"+topic).remove("done_"+topic).apply();chapters.put(selectedChapter,new ArrayList<>(ts));showTopics(selectedChapter);}).show();});root.addView(manage,lp(-1,48,0,0,0,12));}}

        addText(root, "ОТМЕТИТЬ ПРОГРЕСС", 12, PINK, true);
        LinearLayout levels = new LinearLayout(this);
        levels.setOrientation(LinearLayout.HORIZONTAL);
        int[] values = {25, 50, 75, 100};
        for (int index = 0; index < values.length; index++) {
            final int value = values[index];
            Button b = button(value + "%", percent >= value);
            progressButtons[index] = b;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            params.setMargins(dp(2), dp(4), dp(2), dp(10));
            levels.addView(b, params);
            b.setOnClickListener(v -> {
                int oldProgress=topicProgress(topic); saveTopicProgress(topic,value);
                if(value==100)recordStudyDay(); if(oldProgress<100&&value==100)playTopicCompleteSound();
                refreshTopicProgress(value);
                if (value == 100) showPraise();
            });
        }
        root.addView(levels, lp(-1, -2, 0, 0, 0, 8));

        Button done = button("✅  "+(g("Я позанималась сегодня","Я позанимался сегодня")), true);
        done.setOnClickListener(v -> markDone());
        root.addView(done, lp(-1, 54, 0, 4, 0, 10));
        Button back = button("← К темам", false);
        back.setOnClickListener(v -> handleBackNavigation());
        root.addView(back, lp(-1, 50, 0, 0, 0, 0));
    }

    private String topicRule(String topic){
        String t=topic.toLowerCase(new Locale("ru"));
        if(t.contains("процент"))return "1% — это одна сотая часть числа. Чтобы найти p% от числа, умножь число на p/100. Чтобы найти число по его проценту, раздели известную часть на p/100.";
        if(t.contains("пропорц")||t.contains("отношение"))return "В пропорции a/b = c/d произведение крайних членов равно произведению средних: a · d = b · c. В отношении сохраняй один и тот же множитель для обеих величин.";
        if(t.contains("десятич")||t.contains("округлен")||t.contains("среднее"))return "Записывай числа так, чтобы запятые стояли друг под другом. При умножении учитывай общее число знаков после запятой, при делении перенеси запятую в делителе и делимом одинаково.";
        if(t.contains("сокращ"))return "Раздели числитель и знаменатель на одно и то же общее число, отличное от нуля. Значение дроби при этом не меняется.";
        if(t.contains("знаменател"))return "Чтобы сложить или вычесть дроби, сначала приведи их к общему знаменателю. Затем выполни действие с числителями.";
        if(t.contains("сравнен"))return "Сначала приведи числа к сопоставимому виду. Для дробей с одинаковым знаменателем сравни числители; для десятичных дробей сравни разряды слева направо.";
        if(t.contains("смешан"))return "Для сложения или вычитания смешанных чисел отдельно работай с целыми и дробными частями. Если нужно, преобразуй смешанное число в неправильную дробь.";
        if(t.contains("умнож"))return "При умножении дробей перемножь числители и знаменатели. Перед вычислением можно сократить общий множитель по диагонали.";
        if(t.contains("делен"))return "Деление на дробь замени умножением на обратную дробь. Переверни вторую дробь, затем перемножь.";
        if(t.contains("части числа"))return "Чтобы найти часть числа, раздели число на знаменатель и умножь на числитель. Чтобы найти целое по части, выполни обратные действия.";
        if(t.contains("округлен"))return "Посмотри на цифру справа от нужного разряда: 0–4 — оставь разряд без изменения, 5–9 — увеличь его на единицу.";
        if(t.contains("делим")||t.contains("делител")||t.contains("кратн"))return "Делитель делит число без остатка. Кратные получаются умножением числа на натуральные множители.";
        if(t.contains("прост")||t.contains("состав"))return "Простое число имеет ровно два натуральных делителя: 1 и само число. У составного делителей больше двух.";
        if(t.contains("квадрат")||t.contains("куб"))return "Квадрат числа — произведение числа на себя. Куб — произведение трёх одинаковых множителей.";
        if(t.contains("выражен"))return "Соблюдай порядок действий: сначала скобки, затем умножение и деление, после этого сложение и вычитание.";
        if(t.contains("сложен")||t.contains("вычит"))return "При сложении и вычитании выравнивай разряды. Проверяй вычитание обратным сложением.";
        if(t.contains("умножен")||t.contains("делен"))return "Умножение можно проверить делением, а деление — умножением. Следи за разрядами и остатком.";
        return "Прочитай условие, выдели известные данные, выбери подходящее действие и проверь результат обратным действием.";
    }
    private String topicExample(String topic){
        String t=topic.toLowerCase(new Locale("ru"));
        if(t.contains("процент"))return "20% от 150: 150 · 20/100 = 30.";
        if(t.contains("пропорц")||t.contains("отношение"))return "2 : 3 = 8 : 12, потому что обе части первого отношения умножили на 4.";
        if(t.contains("десятич"))return "2,4 + 0,35 = 2,75. Запятые записываем друг под другом.";
        if(t.contains("сокращ"))return "6/8 = 3/4: делим числитель и знаменатель на 2.";
        if(t.contains("знаменател"))return "1/3 + 1/4 = 4/12 + 3/12 = 7/12.";
        if(t.contains("сравнен"))return "3/8 меньше 5/8, потому что знаменатели одинаковы, а 3 меньше 5.";
        if(t.contains("смешан"))return "2 1/3 = (2 · 3 + 1)/3 = 7/3.";
        if(t.contains("умнож"))return "2/3 · 3/5 = 6/15 = 2/5.";
        if(t.contains("делен"))return "2/3 ÷ 4/5 = 2/3 · 5/4 = 10/12 = 5/6.";
        if(t.contains("части числа"))return "3/4 от 20: 20 ÷ 4 · 3 = 15.";
        if(t.contains("округлен"))return "7,46 до десятых = 7,5.";
        if(t.contains("квадрат"))return "6² = 6 · 6 = 36; 6³ = 6 · 6 · 6 = 216.";
        if(t.contains("выражен"))return "8 + 3 · 4 = 8 + 12 = 20.";
        return "Если 24 разделить на 6, получится 4; проверка: 4 · 6 = 24.";
    }
    private void addTopicStudyCard(String topic){
        LinearLayout c=card();addText(c,"💡  КАРТОЧКА ТЕМЫ",11,PINK,true);addText(c,"Главное правило",15,DARK,true);addText(c,topicRule(topic),13,MUTED,false);addText(c,"Пример решения",14,DARK,true);addText(c,topicExample(topic),14,DARK,false);
        addText(c,"ШАГИ РЕШЕНИЯ",10,PINK,true);String lower=topic.toLowerCase(new Locale("ru"));String[] steps=lower.contains("знаменател")?new String[]{"Найди общий знаменатель для дробей","Преобразуй дроби, сохранив их значения","Выполни действие с числителями и сократи ответ"}:lower.contains("процент")?new String[]{"Переведи процент в дробь со знаменателем 100","Умножь исходное число на эту дробь","Проверь, соответствует ли результат условию"}:lower.contains("десятич")?new String[]{"Выравняй запятые и разряды","Выполни действие по разрядам","Проверь положение запятой в результате"}:lower.contains("смешан")?new String[]{"Преобразуй смешанные числа в неправильные дроби","Выполни действие с дробями","Сократи ответ и при необходимости верни смешанное число"}:lower.contains("делен")?new String[]{"Запиши обратную дробь для делителя","Замени деление умножением","Перемножь и сократи результат"}:new String[]{"Прочитай условие и выдели известные данные","Выбери подходящее правило и выполни действие","Проверь ответ обратным действием"};
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<steps.length;i++){TextView step=text((i+1)+".  "+steps[i],12,DARK,false);step.setPadding(dp(10),dp(8),dp(10),dp(8));step.setBackground(shape(0xFFFFF4F7,13,0));line.addView(step,lp(-1,-2,0,0,0,5));step.setAlpha(0f);step.setTranslationY(dp(8));final TextView animated=step;new Handler(Looper.getMainLooper()).postDelayed(()->animated.animate().alpha(1f).translationY(0).setDuration(220).start(),i*180L);}
        c.addView(line);root.addView(c,lp(-1,-2,0,0,0,12));
    }
    private void addTopicReflection(String topic){
        LinearLayout c=card();addText(c,"✍️  ОБЪЯСНИ СВОИМИ СЛОВАМИ",11,PINK,true);addText(c,"Представь, что объясняешь правило другу. Что самое важное нужно запомнить?",13,MUTED,false);
        EditText explanation=new EditText(this);explanation.setHint("Я понял(а), что...");explanation.setTextSize(14);explanation.setTextColor(DARK);explanation.setGravity(Gravity.TOP);explanation.setMinLines(3);explanation.setPadding(dp(12),dp(9),dp(12),dp(9));explanation.setBackground(shape(0xFFFFFBFC,14,0xFFEADDE2));String saved=prefs.getString("topic_explanation_"+topicStorageSuffix(selectedChapter,topic),"");explanation.setText(saved);c.addView(explanation,lp(-1,96,0,7,0,8));
        Button save=button("Сохранить объяснение в дневник",false);save.setOnClickListener(v->{String value=explanation.getText().toString().trim();if(value.isEmpty()){explanation.setError("Напиши объяснение");return;}String id=String.valueOf(System.currentTimeMillis());String entry="Моё объяснение темы «"+topic+"»:\\n"+value;Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));ids.add(id);prefs.edit().putString("topic_explanation_"+topicStorageSuffix(selectedChapter,topic),value).putString("diary_"+id,entry).putString("diary_tag_"+id,"Учёба").putStringSet("diary_ids",ids).apply();Toast.makeText(this,"Объяснение сохранено в дневник ♡",Toast.LENGTH_SHORT).show();});c.addView(save,lp(-1, 48,0,0,0,0));root.addView(c,lp(-1,-2,0,0,0,12));
    }
    private void addTopicPhoto(String topic){
        String suffix=topicStorageSuffix(selectedChapter,topic),key="topic_photo_"+suffix,uri=prefs.getString(key,""),file=prefs.getString("topic_photo_file_"+suffix,"");LinearLayout c=card();addText(c,"📷  ФОТО ЗАДАЧИ",11,PINK,true);addText(c,"Сохрани снимок задания из учебника, чтобы вернуться к нему позже.",12,MUTED,false);
        if(!uri.isEmpty()||!file.isEmpty()){try{ImageView image=new ImageView(this);if(!file.isEmpty())image.setImageBitmap(android.graphics.BitmapFactory.decodeFile(file));else image.setImageURI(android.net.Uri.parse(uri));image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);c.addView(image,lp(-1,190,0,6,0,7));}catch(Exception ignored){}}
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button camera=button("📷  Камера",false),gallery=button("Из галереи",false);actions.addView(camera,new LinearLayout.LayoutParams(0,dp(48),1f));actions.addView(gallery,new LinearLayout.LayoutParams(0,dp(48),1f));camera.setOnClickListener(v->{Intent take=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);if(take.resolveActivity(getPackageManager())!=null)startActivityForResult(take,PICK_TOPIC_CAMERA);else Toast.makeText(this,"Камера недоступна",Toast.LENGTH_SHORT).show();});gallery.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,PICK_TOPIC_PHOTO);});c.addView(actions,lp(-1,48,0,5,0,0));root.addView(c,lp(-1,-2,0,0,0,12));
    }
    private void loadQuestionBank(){
        questionBank.clear();
        try(java.io.InputStream in=getAssets().open("question_bank.json")){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))>0)out.write(buffer,0,n);
            org.json.JSONObject data=new org.json.JSONObject(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
            org.json.JSONArray all=data.getJSONArray("questions");
            for(int i=0;i<all.length();i++){
                org.json.JSONObject item=all.getJSONObject(i);String topic=item.getString("topic"),type=item.optString("type","choice"),answer=item.optString("answer","");
                org.json.JSONArray arr=item.optJSONArray("options");String[] options=null;int correct=-1;
                if(arr!=null){options=new String[arr.length()];for(int k=0;k<arr.length();k++){options[k]=arr.getString(k);if(options[k].trim().equalsIgnoreCase(answer.trim()))correct=k;}}
                MiniQuestion q=new MiniQuestion(item.getString("prompt"),options,correct,item.optString("explanation",""),type,answer);q.topic=topic;
                List<MiniQuestion> list=questionBank.get(topic);if(list==null){list=new ArrayList<>();questionBank.put(topic,list);}list.add(q);
            }
        }catch(Exception e){android.util.Log.e("StudyWithLove","Question bank load failed",e);}
        Set<String> customCards=prefs.getStringSet("custom_question_cards",new TreeSet<String>());
        for(String payload:customCards){try{org.json.JSONObject item=new org.json.JSONObject(payload);String topic=item.optString("topic",""),prompt=item.optString("prompt","").trim(),answer=item.optString("answer","").trim();if(topic.isEmpty()||prompt.isEmpty()||answer.isEmpty())continue;List<MiniQuestion> list=questionBank.get(topic);if(list==null){list=new ArrayList<>();questionBank.put(topic,list);}boolean duplicate=false;for(MiniQuestion existing:list)if(normalizeAnswer(existing.prompt).equals(normalizeAnswer(prompt))){duplicate=true;break;}if(!duplicate){MiniQuestion q=new MiniQuestion(prompt,null,-1,item.optString("note",""),"input",answer);q.topic=topic;list.add(q);}}catch(Exception ignored){}}
    }
    private void addOwnQuestionCard(String topic){
        Set<String> saved=prefs.getStringSet("custom_question_cards",new TreeSet<String>());int count=0;for(String payload:saved)try{if(topic.equals(new org.json.JSONObject(payload).optString("topic","")))count++;}catch(Exception ignored){}
        LinearLayout section=card();addText(section,"✎  МОИ КАРТОЧКИ И ЗАМЕТКИ",11,PINK,true);addText(section,"Добавляй свои вопросы прямо во время учёбы. Они сохраняются на устройстве и попадут в мини-тесты этой темы.",12,MUTED,false);addText(section,"Сохранено карточек: "+count,12,DARK,true);
        Button add=button("＋  Быстро добавить карточку",true);section.addView(add,lp(-1, 48,0,5,0,0));
        add.setOnClickListener(v->{
            LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(4),dp(4),dp(4),dp(2));
            EditText prompt=new EditText(this);prompt.setHint("Вопрос или формулировка правила");prompt.setMinLines(2);prompt.setGravity(Gravity.TOP);prompt.setTextSize(14);prompt.setTextColor(DARK);form.addView(prompt,lp(-1,78,0,0,0,8));
            EditText answer=new EditText(this);answer.setHint("Краткий правильный ответ");answer.setSingleLine(true);answer.setTextSize(14);answer.setTextColor(DARK);form.addView(answer,lp(-1,48,0,0,0,8));
            EditText note=new EditText(this);note.setHint("Пояснение или заметка (необязательно)");note.setMinLines(2);note.setGravity(Gravity.TOP);note.setTextSize(13);note.setTextColor(DARK);form.addView(note,lp(-1,68,0,0,0,0));
            android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Новая карточка").setView(form).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v2->{
                String p=prompt.getText().toString().trim(),a=answer.getText().toString().trim(),n=note.getText().toString().trim();
                if(p.length()<5||p.length()>240){prompt.setError("Вопрос должен содержать от 5 до 240 символов");return;}
                if(a.isEmpty()||a.length()>120){answer.setError("Ответ должен содержать от 1 до 120 символов");return;}
                if(n.length()>400){note.setError("Пояснение — не более 400 символов");return;}
                List<MiniQuestion> existing=questionBank.get(topic);if(existing!=null)for(MiniQuestion q:existing)if(normalizeAnswer(q.prompt).equals(normalizeAnswer(p))){prompt.setError("Такой вопрос уже есть в этой теме");return;}
                int ownCount=0;for(String raw:prefs.getStringSet("custom_question_cards",new TreeSet<String>()))try{if(topic.equals(new org.json.JSONObject(raw).optString("topic","")))ownCount++;}catch(Exception ignored){}
                if(ownCount>=100){Toast.makeText(this,"В одной теме можно сохранить не более 100 карточек",Toast.LENGTH_LONG).show();return;}
                try{org.json.JSONObject item=new org.json.JSONObject();item.put("topic",topic);item.put("prompt",p);item.put("answer",a);item.put("note",n);Set<String> cards=new TreeSet<>(prefs.getStringSet("custom_question_cards",new TreeSet<String>()));cards.add(item.toString());prefs.edit().putStringSet("custom_question_cards",cards).apply();loadQuestionBank();dialog.dismiss();showTopic(topic);Toast.makeText(this,"Карточка сохранена ♡",Toast.LENGTH_SHORT).show();}catch(Exception ex){Toast.makeText(this,"Не удалось сохранить карточку",Toast.LENGTH_SHORT).show();}
            }));dialog.show();
        });
        Button manage=button("Мои карточки · посмотреть и удалить",false);section.addView(manage,lp(-1, 48,0,6,0,0));
        manage.setOnClickListener(v->{List<String> payloads=new ArrayList<>(),labels=new ArrayList<>();for(String raw:prefs.getStringSet("custom_question_cards",new TreeSet<String>()))try{org.json.JSONObject item=new org.json.JSONObject(raw);if(topic.equals(item.optString("topic",""))){payloads.add(raw);labels.add(item.optString("prompt","Карточка"));}}catch(Exception ignored){}if(payloads.isEmpty()){Toast.makeText(this,"В этой теме пока нет своих карточек",Toast.LENGTH_SHORT).show();return;}new android.app.AlertDialog.Builder(this).setTitle("Мои карточки").setItems(labels.toArray(new String[0]),(dlg,index)->{String raw=payloads.get(index);try{org.json.JSONObject item=new org.json.JSONObject(raw);new android.app.AlertDialog.Builder(this).setTitle(item.optString("prompt","Карточка")).setMessage("Ответ: "+item.optString("answer","")+(item.optString("note","").isEmpty()?"":"\\n\\nЗаметка: "+item.optString("note",""))).setNegativeButton("Закрыть",null).setPositiveButton("Удалить",(d,w)->{Set<String> cards=new TreeSet<>(prefs.getStringSet("custom_question_cards",new TreeSet<String>()));cards.remove(raw);prefs.edit().putStringSet("custom_question_cards",cards).apply();loadQuestionBank();showTopic(topic);}).show();}catch(Exception ignored){}}).show();});
        root.addView(section,lp(-1,-2,0,0,0,12));
    }
    private void startMiniTest(String topic){
        miniTopic=topic;miniQuestions.clear();miniWrongQuestions.clear();miniScore=0;weaknessTrainingMode=false;miniResultSaved=false;
        List<MiniQuestion> bank=questionBank.get(topic);
        if(bank!=null&&!bank.isEmpty()){
            List<MiniQuestion> shuffled=new ArrayList<>(bank);java.util.Collections.shuffle(shuffled,quizRandom);
            miniQuestions.addAll(shuffled.subList(0,Math.min(5,shuffled.size())));
        }else for(int i=0;i<5;i++)miniQuestions.add(makeMiniQuestion(topic));
        renderMiniTest(0,false);
    }
    private void startWeaknessTraining(){
        miniTopic="Мои слабые места";miniQuestions.clear();miniWrongQuestions.clear();miniScore=0;weaknessTrainingMode=true;miniResultSaved=false;
        mistakeStore().migrateLegacy(prefs);
        for(String payload:mistakeStore().newestPayloads(10)){try{org.json.JSONObject o=new org.json.JSONObject(payload);org.json.JSONArray a=o.optJSONArray("options");String[] opts=null;int correct=-1;if(a!=null){opts=new String[a.length()];for(int i=0;i<a.length();i++){opts[i]=a.getString(i);if(opts[i].equalsIgnoreCase(o.optString("answer")))correct=i;}}MiniQuestion restored=new MiniQuestion(o.optString("prompt"),opts,correct,o.optString("explanation"),o.optString("type","choice"),o.optString("answer"));restored.topic=o.optString("topic","");miniQuestions.add(restored);}catch(Exception ignored){}}
        if(miniQuestions.isEmpty()){showWeaknessTrainer();return;}renderMiniTest(0,false);
    }
    private String classifyMistake(MiniQuestion q,String userAnswer){String t=(q.topic+" "+q.prompt).toLowerCase(new Locale("ru"));if(t.contains("сокращ"))return "Сокращение";if(t.contains("знаменател"))return "Общий знаменатель";if(t.contains("округлен")||t.contains("десятич")||t.contains("запят"))return "Запятая или разряд";if(t.contains("вычит")||q.prompt.contains("−")||q.prompt.contains(" - "))return "Знак действия";if(t.contains("делен"))return "Деление";return "Вычисление";}
    private void recordMistake(MiniQuestion q,String userAnswer){
        try{
            org.json.JSONObject o=new org.json.JSONObject();o.put("topic",q.topic==null||q.topic.isEmpty()?miniTopic:q.topic);o.put("kind",classifyMistake(q,userAnswer));o.put("type",q.type);o.put("prompt",q.prompt);o.put("answer",q.answer);o.put("user_answer",userAnswer);o.put("explanation",q.explanation);
            org.json.JSONArray a=new org.json.JSONArray();if(q.options!=null)for(String option:q.options)a.put(option);o.put("options",a);
            String topic=q.topic==null||q.topic.isEmpty()?miniTopic:q.topic;mistakeStore().migrateLegacy(prefs);mistakeStore().save(topic,q.prompt,o.toString());
        }catch(Exception ignored){}
    }
    private void clearMistake(MiniQuestion q){
        try{mistakeStore().migrateLegacy(prefs);String topic=q.topic==null?"":q.topic;mistakeStore().delete(topic,q.prompt);}catch(Exception ignored){}
    }
    private String normalizeAnswer(String value){return value==null?"":value.trim().toLowerCase(new Locale("ru")).replace(" ","").replace(",",".").replace("−","-");}
    private void acceptMiniAnswer(MiniQuestion q,String userAnswer,int index,boolean wrongOnly,View source){
        boolean correct=normalizeAnswer(userAnswer).equals(normalizeAnswer(q.answer));
        if(correct){miniScore++;clearMistake(q);haptic(source,true);}else{miniWrongQuestions.add(q);recordMistake(q,userAnswer);haptic(source,false);if(source!=null)source.animate().translationX(dp(8)).setDuration(45).withEndAction(()->source.animate().translationX(-dp(8)).setDuration(45).withEndAction(()->source.animate().translationX(0).setDuration(45).start()).start()).start();}
        if(source instanceof Button){Button chosen=(Button)source;chosen.setBackground(shape(correct?0xFFE4F4E8:0xFFFFE8E8,22,correct?0xFF75B987:0xFFE58A8A));chosen.setTextColor(correct?0xFF287A43:0xFFB64242);new Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()){chosen.setBackground(shape(0xFFFFFFFF,22,0xFFEADDE2));chosen.setTextColor(DARK);}},1050);}
        TextView feedback=text((correct?"✓  Верно! ":"✦  Правильный ответ: "+q.answer+" · ")+q.explanation,13,correct?0xFF287A43:0xFF9B4A58,true);feedback.setPadding(dp(12),dp(10),dp(12),dp(10));feedback.setBackground(shape(correct?0xFFF0F8F1:0xFFFFF2F2,14,0));root.addView(feedback,lp(-1,-2,0,7,0,4));
        new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing())renderMiniTest(index+1,wrongOnly);},1050);
    }
    private MiniQuestion makeMiniQuestion(String topic){
        String t=topic.toLowerCase(new Locale("ru"));
        boolean pct=t.contains("процент")||t.contains("отношение")||t.contains("пропорц");
        boolean dec=t.contains("десятич")||t.contains("округлен")||t.contains("среднее арифмет");
        boolean frac=t.contains("дроб")||t.contains("смешан")||t.contains("часть числа")||t.contains("числа по его части");
        int answer;String prompt,explain;boolean decimalAnswer=false;
        if(pct){int a=20+quizRandom.nextInt(181);if(t.contains("пропорц")||t.contains("отношение")){int unitPrice=2+quizRandom.nextInt(6),baseCount=2+quizRandom.nextInt(5);answer=unitPrice*2;prompt=baseCount+" одинаковых предмета стоят "+(unitPrice*baseCount)+". Сколько стоят "+(baseCount*2)+" таких предметов?";explain="Количество увеличилось в 2 раза, значит стоимость тоже: "+(unitPrice*baseCount)+" · 2 = "+answer+".";}else{int p=10*(1+quizRandom.nextInt(8));answer=a*p/100;prompt="Найди "+p+"% от "+a;explain=p+"% от "+a+" = "+answer+".";}}
        else if(dec){decimalAnswer=true;int a=12+quizRandom.nextInt(80),b=11+quizRandom.nextInt(70);answer=a+b;prompt="Вычисли "+String.format(Locale.GERMANY,"%.1f",a/10.0)+" + "+String.format(Locale.GERMANY,"%.1f",b/10.0);explain=String.format(Locale.GERMANY,"%.1f + %.1f = %.1f",a/10.0,b/10.0,answer/10.0);}
        else if(frac){int d=2+quizRandom.nextInt(8),n=2+quizRandom.nextInt(9);answer=n;prompt="Найди 1/"+d+" от "+(d*n);explain="Одна "+1+"/"+d+" часть от "+(d*n)+" равна "+answer+".";}
        else{int a=17+quizRandom.nextInt(70),b=6+quizRandom.nextInt(30);answer=a+b;prompt="Вычисли "+a+" + "+b;explain=a+" + "+b+" = "+answer+".";}
        List<Integer> order=new ArrayList<>();order.add(answer);int attempts=0;while(order.size()<4&&attempts++<100){int delta=1+quizRandom.nextInt(9);int candidate=answer+(quizRandom.nextBoolean()?delta:-delta);if(candidate>=0&&!order.contains(candidate))order.add(candidate);}while(order.size()<4)order.add(answer+order.size()+attempts++);
        java.util.Collections.shuffle(order,quizRandom);String[] opts=new String[4];int correct=0;for(int i=0;i<4;i++){opts[i]=decimalAnswer?String.format(Locale.GERMANY,"%.1f",order.get(i)/10.0):String.valueOf(order.get(i));if(order.get(i)==answer)correct=i;}
        MiniQuestion generated=new MiniQuestion(prompt,opts,correct,explain,"choice",opts[correct]);generated.topic=topic;return generated;
    }
    private void renderMiniTest(int index,boolean wrongOnly){
        List<MiniQuestion> list=wrongOnly?miniWrongQuestions:miniQuestions;
        if(index>=list.size()){showMiniTestResult(wrongOnly);return;}
        currentScreen="mini_test";base((weaknessTrainingMode?"Мои слабые места":"Мини-тест")+" · "+(index+1)+"/"+list.size(),"Отвечай в своём темпе — после ответа будет короткое объяснение");
        MiniQuestion original=list.get(index);MiniQuestion q=new MiniQuestion(original.prompt,original.options==null?null:original.options.clone(),original.correct,original.explanation,original.type,original.answer);q.topic=original.topic;if(q.options!=null)java.util.Collections.shuffle(java.util.Arrays.asList(q.options),quizRandom);LinearLayout panel=card();panel.setPadding(dp(16),dp(18),dp(16),dp(18));addText(panel,"ВОПРОС "+(index+1)+" ИЗ "+list.size(),11,PINK,true);addText(panel,q.type.equals("input")?"✍️  ВПИШИ ОТВЕТ":q.type.equals("true_false")?"⚖️  ВЕРНО ИЛИ НЕВЕРНО":q.type.equals("find_error")?"🔎  НАЙДИ ОШИБКУ":"ВЫБЕРИ ОТВЕТ",10,MUTED,true);addText(panel,q.prompt,20,DARK,true);root.addView(panel,lp(-1,-2,0,0,0,14));
        final boolean[] answered={false};
        if("input".equals(q.type)){
            EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(19);input.setTextColor(DARK);input.setHint("Твой ответ");input.setPadding(dp(14),dp(8),dp(14),dp(8));input.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));root.addView(input,lp(-1,52,0,0,0,9));
            Button submit=button("Проверить ответ",true);root.addView(submit,lp(-1,48,0,0,0,8));submit.setOnClickListener(v->{if(answered[0])return;String value=input.getText().toString();if(value.trim().isEmpty()){input.setError("Введи ответ");return;}answered[0]=true;acceptMiniAnswer(q,value,index,wrongOnly,submit);});
        }else{
            String[] opts=q.options==null?new String[]{"Верно","Неверно"}:q.options;Button[] answerButtons=new Button[opts.length];
            for(int i=0;i<opts.length;i++){final String answer=opts[i];Button b=button(answer,false);answerButtons[i]=b;b.setTextSize(17);root.addView(b,lp(-1,50,0,0,0,8));b.setOnClickListener(v->{if(answered[0])return;answered[0]=true;for(int k=0;k<opts.length;k++){Button choice=answerButtons[k];boolean isRight=normalizeAnswer(opts[k]).equals(normalizeAnswer(q.answer));if(isRight){choice.setBackground(shape(0xFFE4F4E8,22,0xFF75B987));choice.setTextColor(0xFF287A43);}else if(choice==b){choice.setBackground(shape(0xFFFFE8E8,22,0xFFE58A8A));choice.setTextColor(0xFFB64242);}}acceptMiniAnswer(q,answer,index,wrongOnly,b);new Handler(android.os.Looper.getMainLooper()).postDelayed(()->{for(Button option:answerButtons){option.setBackground(shape(0xFFFFFFFF,22,0xFFEADDE2));option.setTextColor(DARK);}},260);});}
        }
        if(!q.explanation.isEmpty()){TextView hint=text("После ответа появится разбор решения",11,MUTED,false);root.addView(hint,lp(-1,-2,0,3,0,0));}
        Button quit=button("Завершить тест",false);quit.setOnClickListener(v->showMiniTestResult(wrongOnly));root.addView(quit,lp(-1,48,0,8,0,0));
    }
    private void saveMiniTestResult(){try{org.json.JSONObject o=new org.json.JSONObject();o.put("topic",miniTopic);o.put("score",miniScore);o.put("total",miniQuestions.size());o.put("wrong",miniWrongQuestions.size());o.put("date",todayKey());String key="test_result_"+System.currentTimeMillis();prefs.edit().putString(key,o.toString()).apply();List<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("test_result_"))keys.add(k);java.util.Collections.sort(keys);for(int i=0;i<keys.size()-100;i++)prefs.edit().remove(keys.get(i)).apply();}catch(Exception ignored){}}
    private void showMiniTestResult(boolean wrongOnly){
        currentScreen="mini_test_result";base(weaknessTrainingMode?"Результат тренажёра":"Результат мини-теста","Ошибки — это подсказка, что повторить");if(!miniResultSaved){saveMiniTestResult();miniResultSaved=true;}
        int total=miniQuestions.size();LinearLayout resultCard=card();resultCard.setGravity(Gravity.CENTER);addText(resultCard,"ТВОЙ РЕЗУЛЬТАТ",11,PINK,true);TextView score=text(miniScore+" / "+total,38,DARK,true);score.setGravity(Gravity.CENTER);resultCard.addView(score,lp(-1,60,0,6,0,4));addText(resultCard,miniWrongQuestions.isEmpty()?"Все ответы верные — отлично!":"Ошибок: "+miniWrongQuestions.size()+". Они сохранены в журнале.",13,MUTED,false);root.addView(resultCard,lp(-1,-2,0,0,0,14));
        if(miniWrongQuestions.isEmpty()&&!pendingReviewKey.isEmpty()){prefs.edit().putBoolean(pendingReviewKey,true).apply();pendingReviewKey="";}
        if(!miniWrongQuestions.isEmpty()){Button repeat=button("↻  Повторить ошибки",true);repeat.setOnClickListener(v->{miniQuestions.clear();miniQuestions.addAll(miniWrongQuestions);miniWrongQuestions.clear();miniScore=0;miniResultSaved=false;renderMiniTest(0,false);});root.addView(repeat,lp(-1,50,0,0,0,8));}
        Button again=button(weaknessTrainingMode?"Пройти тренажёр ещё раз":"Пройти тест заново",false);again.setOnClickListener(v->{if(weaknessTrainingMode)startWeaknessTraining();else startMiniTest(miniTopic);});root.addView(again,lp(-1,48,0,0,0,8));backHome();
    }
    private int countMistakes(){int n=0;for(String k:prefs.getAll().keySet())if(k.startsWith("mistake_"))n++;return n;}
    private void showWeaknessTrainer(){
        currentScreen="weakness_trainer";base("Мои слабые места","Повторяй именно те задания, в которых ошибалась");
        Map<String,Integer> byTopic=new TreeMap<>();Map<String,Map<String,Integer>> kinds=new TreeMap<>();List<String> keys=new ArrayList<>();
        for(String key:prefs.getAll().keySet())if(key.startsWith("mistake_")){keys.add(key);try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(key,"{}"));String topic=o.optString("topic","Другая тема"),kind=o.optString("kind","Вычисление");byTopic.put(topic,byTopic.containsKey(topic)?byTopic.get(topic)+1:1);Map<String,Integer> group=kinds.get(topic);if(group==null){group=new TreeMap<>();kinds.put(topic,group);}group.put(kind,group.containsKey(kind)?group.get(kind)+1:1);}catch(Exception ignored){}}
        LinearLayout summary=card();addText(summary,"ЗАПИСЕЙ В ЖУРНАЛЕ",11,PINK,true);addText(summary,String.valueOf(keys.size()),34,DARK,true);addText(summary,"Сохраняются неверные ответы и тип затруднения.",12,MUTED,false);root.addView(summary,lp(-1,-2,0,0,0,12));
        if(keys.isEmpty())addText(root,"Пока ошибок нет. Решай мини-тесты — если что-то не получится, задание появится здесь.",14,MUTED,false);
        else for(Map.Entry<String,Integer> e:byTopic.entrySet()){LinearLayout item=card();addText(item,e.getKey(),14,DARK,true);addText(item,e.getValue()+" ошибок в журнале",12,MUTED,false);Map<String,Integer> group=kinds.get(e.getKey());if(group!=null){StringBuilder types=new StringBuilder("Типы: ");for(Map.Entry<String,Integer> kind:group.entrySet()){if(types.length()>6)types.append(" · ");types.append(kind.getKey()).append(" (").append(kind.getValue()).append(")");}addText(item,types.toString(),11,PINK,false);}root.addView(item,lp(-1,-2,0,0,0,6));}
        Button train=button("🧠  Тренировать мои ошибки",true);train.setEnabled(!keys.isEmpty());train.setAlpha(keys.isEmpty()?.45f:1f);train.setOnClickListener(v->startWeaknessTraining());root.addView(train,lp(-1,50,0,10,0,8));
        Button back=button("← К урокам",false);back.setOnClickListener(v->showChapters());root.addView(back,lp(-1,48,0,0,0,0));
    }
    private void addSpacedRepetitionSection(){
        LinearLayout section=card();addText(section,"🔁  ПОРА ПОВТОРИТЬ",12,PINK,true);int count=0;long now=System.currentTimeMillis();
        for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue()){
            String suffix=topicStorageSuffix(entry.getKey(),topic);long completed=prefs.getLong("topic_completed_at_"+suffix,0);if(completed<=0)continue;long age=(now-completed)/86400000L;
            for(int interval:new int[]{3,7,14})if(age>=interval&&!prefs.getBoolean("review_done_"+suffix+"_"+interval,false)){
                final int dueInterval=interval;count++;TextView item=text("•  "+topic+"  ·  "+interval+" "+dayWord(interval),13,DARK,true);section.addView(item,lp(-1,34,0,1,0,1));Button review=button("Повторить тему",false);review.setOnClickListener(v->{selectedChapter=entry.getKey();selectedTopic=topic;pendingReviewKey="review_done_"+suffix+"_"+dueInterval;startMiniTest(topic);});section.addView(review,lp(-1, 48,0,0,0,8));
            }
        }
        if(count==0)addText(section,"Пока всё повторено. Следующая проверка появится через 3, 7 и 14 дней после завершения темы ♡",12,MUTED,false);
        root.addView(section,lp(-1,-2,0,0,0,12));
    }

    private void refreshTopicProgress(int value) {
        if (topicProgressLabel != null) topicProgressLabel.setText("ТВОЙ ПРОГРЕСС: " + value + "%");
        if (topicProgressBar != null) topicProgressBar.setProgress(value);
        int[] values = {25, 50, 75, 100};
        for (int i = 0; i < progressButtons.length; i++) {
            if (progressButtons[i] != null) progressButtons[i].setBackground(shape(value >= values[i] ? PINK : PALE, 18, value >= values[i] ? PINK : 0xFFFFDCE6));
            if (progressButtons[i] != null) progressButtons[i].setTextColor(value >= values[i] ? 0xFFFFFFFF : PINK);
        }
    }

    private void playTopicCompleteSound(){try{android.media.ToneGenerator tone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,55);tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK,135);new Handler(Looper.getMainLooper()).postDelayed(tone::release,220);}catch(Exception ignored){}}

    private void showPraise() {
        Toast.makeText(this, g("Умничка! ❤️", "Молодец! ❤️"), Toast.LENGTH_SHORT).show();
    }

    private String topicStorageSuffix(String chapter,String topic) {
        if(chapter!=null&&chapter.startsWith("CUSTOM:")){String[] p=chapter.split(":",3);if(p.length>=2)return "custom_"+p[1]+"_"+android.util.Base64.encodeToString(topic.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);}
        return topic;
    }
    private int topicProgressForChapter(String chapter,String topic) {
        String suffix=topicStorageSuffix(chapter,topic),pk="progress_"+suffix,dk="done_"+suffix;
        if(chapter!=null&&chapter.startsWith("CUSTOM:")&&!prefs.contains(pk)&&!prefs.contains(dk)){int old=prefs.getInt("progress_"+topic,0);boolean wasDone=prefs.getBoolean("done_"+topic,false);if(old>0||wasDone)prefs.edit().putInt(pk,old).putBoolean(dk,wasDone).remove("progress_"+topic).remove("done_"+topic).apply();}
        if(prefs.getBoolean(dk,false))return 100;return Math.max(0,Math.min(100,prefs.getInt(pk,0)));
    }
    private int topicProgress(String topic){return topicProgressForChapter(selectedChapter,topic);}
    private void saveTopicProgressForChapter(String chapter,String topic,int value){
        int old=topicProgressForChapter(chapter,topic),next=Math.max(0,Math.min(100,value));String suffix=topicStorageSuffix(chapter,topic);
        SharedPreferences.Editor e=prefs.edit().putInt("progress_"+suffix,next).putBoolean("done_"+suffix,next==100);
        if(old<100&&next==100){e.putInt("done_count",prefs.getInt("done_count",0)+1).putLong("topic_completed_at_"+suffix,System.currentTimeMillis());haptic(root,true);celebrate();}if(old==100&&next<100)e.putInt("done_count",Math.max(0,prefs.getInt("done_count",0)-1)).remove("topic_completed_at_"+suffix);e.apply();updateAchievementDates();
    }
    private void saveTopicProgress(String topic,int value){saveTopicProgressForChapter(selectedChapter,topic,value);}

    private void recordStudyDay() {
        String today = todayKey();
        String last = prefs.getString("last_study_date", "");
        Set<String> history = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        history.add(today);
        if (!today.equals(last)) {
            int streak = prefs.getInt("current_streak", 0);
            if (isYesterday(last)) streak++; else if(isOneDayGap(last)&&!weekKey().equals(prefs.getString("streak_freeze_week",""))){streak++;prefs.edit().putString("streak_freeze_week",weekKey()).putString("streak_freeze_used_date",today).apply();}else streak = 1;
            prefs.edit().putInt("current_streak", streak)
                    .putInt("longest_streak", Math.max(streak, prefs.getInt("longest_streak", 0)))
                    .putString("last_study_date", today)
                    .putStringSet("study_days", history).apply();
            StudyWidgetProvider.updateAll(this);updateAchievementDates();
        } else {
            prefs.edit().putStringSet("study_days", history).apply();
        }
    }

    private void markDone() {
        recordStudyDay();updateAchievementDates();
        Toast.makeText(this,"Сегодняшнее занятие отмечено ♡",Toast.LENGTH_SHORT).show();
    }


    private void renderProgressCalendar() {
        if(progressCalendarHost==null)return;progressCalendarHost.removeAllViews();
        LinearLayout calendarCard = card();
        addText(calendarCard, "📅  ДНИ ЗАНЯТИЙ", 12, PINK, true);
        Calendar month = Calendar.getInstance(AppTime.zone());
        month.add(Calendar.MONTH, progressMonthOffset);
        int todayDay = month.get(Calendar.DAY_OF_MONTH);
        int monthNumber = month.get(Calendar.MONTH);
        int yearNumber = month.get(Calendar.YEAR);
        String monthLabel = new SimpleDateFormat("LLLL yyyy", new Locale("ru")).format(month.getTime());
        LinearLayout monthNav = new LinearLayout(this); monthNav.setGravity(Gravity.CENTER_VERTICAL);
        Button prevMonth = button("‹", false); prevMonth.setTextSize(22);
        monthNav.addView(prevMonth, lp(48, 48, 0, 0, 8, 0));
        TextView monthTitle = text(monthLabel.substring(0, 1).toUpperCase(new Locale("ru")) + monthLabel.substring(1), 17, DARK, true);
        monthTitle.setGravity(Gravity.CENTER);
        monthNav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button nextMonth = button("›", false); nextMonth.setTextSize(22);
        nextMonth.setEnabled(progressMonthOffset < 0);
        monthNav.addView(nextMonth, lp(48, 48, 8, 0, 0, 0));
        calendarCard.addView(monthNav, lp(-1, 48, 0, 5, 0, 2));
        prevMonth.setOnClickListener(v -> { progressMonthOffset--; renderProgressCalendar(); });
        nextMonth.setOnClickListener(v -> { if (progressMonthOffset < 0) { progressMonthOffset++; renderProgressCalendar(); } });
        Set<String> studiedDays = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        Set<String> visitedDays = new TreeSet<>(prefs.getStringSet("visit_days", new TreeSet<String>()));
        Calendar first = (Calendar) month.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        LinearLayout weekHeader = new LinearLayout(this);
        weekHeader.setOrientation(LinearLayout.HORIZONTAL);
        String[] shortDays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String dayLabel : shortDays) {
            TextView cell = text(dayLabel, 11, MUTED, true);
            cell.setGravity(Gravity.CENTER);
            weekHeader.addView(cell, new LinearLayout.LayoutParams(0, dp(30), 1f));
        }
        calendarCard.addView(weekHeader, lp(-1, 30, 0, 4, 0, 2));
        LinearLayout week = null;
        for (int slot = 0; slot < 42; slot++) {
            if (slot % 7 == 0) { week = new LinearLayout(this); week.setOrientation(LinearLayout.HORIZONTAL); week.setGravity(Gravity.CENTER_VERTICAL); calendarCard.addView(week, lp(-1, 36, 0, 1, 0, 1)); }
            TextView cell = text("", 11, DARK, true); cell.setGravity(Gravity.CENTER); cell.setIncludeFontPadding(false);
            if (slot >= offset && slot < offset + daysInMonth) {
                int day = slot - offset + 1;
                String dateKey = String.format(Locale.US, "%04d-%02d-%02d", yearNumber, monthNumber + 1, day);
                boolean studied = studiedDays.contains(dateKey), visited = visitedDays.contains(dateKey), isToday = progressMonthOffset == 0 && day == todayDay;
                cell.setText(String.valueOf(day)); int minutes=prefs.getInt("focus_minutes_"+dateKey,0);int heatColor=minutes>=60?0xFFFFC4D5:minutes>=30?0xFFFFD8E3:minutes>=15?0xFFFFE8EE:studied?0xFFFFF2F5:(isToday?PALE:(visited?0xFFF1F0F2:0xFFFFFFFF));cell.setTextColor(studied?PINK:DARK);
                cell.setBackground(shape(heatColor,11,isToday?PINK:(studied?0xFFFFC6D7:0x00FFFFFF)));
                cell.setElevation(dp(1)); if (isToday) cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
            LinearLayout.LayoutParams dayCell=new LinearLayout.LayoutParams(0,dp(30),1f); dayCell.gravity=Gravity.CENTER; dayCell.setMargins(dp(2),0,dp(2),0); week.addView(cell,dayCell);
        }
        addText(calendarCard, "♥ Учебные дни: " + countStudyDaysThisMonth(studiedDays, yearNumber, monthNumber) + "   ·   Заходила: " + countStudyDaysThisMonth(visitedDays, yearNumber, monthNumber), 12, MUTED, false);
        progressCalendarHost.addView(calendarCard, lp(-1, -2, 0, 0, 0, 0));

    }
    private void showProgress() {
        saveForegroundUsage();
        currentScreen = "progress";
        base("Мой прогресс 📊", "Здесь видно, сколько уже изучено по каждой теме");
        int totalTopics = 0, completedTopics = 0;
        for (String chapter : chapters.keySet()) {
            for (String topic : chapters.get(chapter)) {
                if(!isTopicInExamScope(chapter,topic))continue;
                totalTopics++;
                if (topicProgressForChapter(chapter,topic) == 100) completedTopics++;
            }
        }
        int overall = totalTopics == 0 ? 0 : completedTopics * 100 / totalTopics;

        progressCalendarHost=new LinearLayout(this);progressCalendarHost.setOrientation(LinearLayout.VERTICAL);renderProgressCalendar();root.addView(progressCalendarHost,lp(-1,-2,0,0,0,14));

        LinearLayout summary = card();
        addText(summary, "🌸  ОБЩИЙ РЕЗУЛЬТАТ", 12, PINK, true);
        addText(summary, overall + "% изучено", 27, DARK, true);
        addText(summary, completedTopics + " из " + totalTopics + " тем завершено", 14, MUTED, false);
        addText(summary, "⏱ Сегодня: " + prefs.getInt("focus_minutes_" + todayKey(), 0) + " минут по таймеру", 14, PINK, true);
        addText(summary, "Всего: " + totalStudyMinutes() + " минут", 13, MUTED, false);
        addProgressLine(summary, "Весь учебный план", overall);
        root.addView(summary, lp(-1, -2, 0, 0, 0, 16));

        for (String chapter : chapters.keySet()) {
            List<String> topics = new ArrayList<>();
            for(String topic:chapters.get(chapter))if(isTopicInExamScope(chapter,topic))topics.add(topic);
            if(topics.isEmpty())continue;
            int sum = 0;
            for (String topic : topics) sum += topicProgressForChapter(chapter,topic);
            int chapterPercent = sum / topics.size();
            LinearLayout section = card();
            TextView chapterBadge = text("✿  РАЗДЕЛ " + (new ArrayList<>(chapters.keySet()).indexOf(chapter) + 1), 12, PINK, true);
            chapterBadge.setBackground(shape(0xFFFFEEF3, 16, 0xFFFFD5E1));
            chapterBadge.setPadding(dp(10), dp(7), dp(10), dp(7));
            section.addView(chapterBadge, lp(-2, -2, 0, 0, 0, 8));
            addText(section, chapter.replaceFirst("^[^ ]+\\s*", ""), 17, DARK, true);
            addText(section, chapterPercent + "% · " + countCompleted(chapter,topics) + " из " + topics.size() + " тем полностью изучено", 13, MUTED, false);
            addProgressLine(section, "Прогресс раздела", chapterPercent);
            for (String topic : topics) {
                int value = topicProgressForChapter(chapter,topic);
                addProgressLine(section, topic + " — " + value + "%", value);
            }
            root.addView(section, lp(-1, -2, 0, 0, 0, 12));
        }
        Button back = button("← Назад", true);
        back.setOnClickListener(v -> handleBackNavigation());
        root.addView(back, lp(-1, 52, 0, 4, 0, 0));
    }

    private int totalStudyMinutes() { return prefs.getInt("focus_total_minutes",0); }

    private void showPomodoro() {
        currentScreen="pomodoro";base("Таймер фокуса","Настрой время и занимайся в своём ритме");
        LinearLayout wrap=new LinearLayout(this);wrap.setGravity(Gravity.CENTER);wrap.setPadding(0,dp(14),0,dp(18));
        timerDisplay=text(formatTimer(timerRemainingMs>0?timerRemainingMs:selectedStudyMinutes*60000L),42,DARK,true);timerDisplay.setGravity(Gravity.CENTER);
        GradientDrawable face=new GradientDrawable();face.setShape(GradientDrawable.OVAL);face.setColor(PALE);face.setStroke(dp(5),PINK);timerDisplay.setBackground(face);timerDisplay.setElevation(dp(5));
        wrap.addView(timerDisplay,lp(208,208,0,0,0,0));root.addView(wrap,lp(-1,246,0,0,0,8));
        timerStatus=text(timerIsBreak?"🌷  Время отдохнуть":"♡  Время для учёбы",15,PINK,true);timerStatus.setGravity(Gravity.CENTER);root.addView(timerStatus,lp(-1,32,0,0,0,12));
        TextView timerPlant=text(timerRunning?"🌱  Твой цветок растёт вместе с фокусом":"🌷  Начни занятие — и цветок начнёт расти",14,PINK,true);timerPlant.setGravity(Gravity.CENTER);root.addView(timerPlant,lp(-1,34,0,0,0,8));
        LinearLayout settings=card();addText(settings,"ВРЕМЯ УЧЁБЫ",11,PINK,true);
        LinearLayout study=new LinearLayout(this);study.setGravity(Gravity.CENTER);Button minus=button("−",false),plus=button("+",false);
        study.addView(minus,lp(48,48,3,0,6,0));studyTimeValue=text(selectedStudyMinutes+" минут",16,DARK,true);studyTimeValue.setGravity(Gravity.CENTER);study.addView(studyTimeValue,new LinearLayout.LayoutParams(0,dp(48),1f));study.addView(plus,lp(48,48,6,0,3,0));settings.addView(study,lp(-1,48,0,3,0,10));
        minus.setOnClickListener(v->{if(!timerRunning){selectedStudyMinutes=Math.max(1,selectedStudyMinutes-5);if(!timerIsBreak)timerRemainingMs=selectedStudyMinutes*60000L;studyTimeValue.setText(selectedStudyMinutes+" минут");timerDisplay.setText(formatTimer(timerRemainingMs>0?timerRemainingMs:selectedStudyMinutes*60000L));}});
        plus.setOnClickListener(v->{if(!timerRunning){selectedStudyMinutes=Math.min(600,selectedStudyMinutes+5);if(!timerIsBreak)timerRemainingMs=selectedStudyMinutes*60000L;studyTimeValue.setText(selectedStudyMinutes+" минут");timerDisplay.setText(formatTimer(timerRemainingMs>0?timerRemainingMs:selectedStudyMinutes*60000L));}});
        studyTimeValue.setOnClickListener(v->{if(!timerRunning)new TimePickerDialog(this,(p,h,m)->{selectedStudyMinutes=Math.min(600,Math.max(1,h*60+m));if(!timerIsBreak)timerRemainingMs=selectedStudyMinutes*60000L;studyTimeValue.setText(selectedStudyMinutes+" минут");timerDisplay.setText(formatTimer(timerRemainingMs>0?timerRemainingMs:selectedStudyMinutes*60000L));},selectedStudyMinutes/60,selectedStudyMinutes%60,true).show();});
        addText(settings,"ВРЕМЯ ПЕРЕРЫВА",11,PINK,true);LinearLayout breaksRow=new LinearLayout(this);breaksRow.setGravity(Gravity.CENTER);
        Button breakMinus=button("−",false),breakPlus=button("+",false);breakTimeValue=text(selectedBreakMinutes+" минут",16,DARK,true);breakTimeValue.setGravity(Gravity.CENTER);breaksRow.addView(breakMinus,lp(48,48,3,0,6,0));breaksRow.addView(breakTimeValue,new LinearLayout.LayoutParams(0,dp(48),1f));breaksRow.addView(breakPlus,lp(48,48,6,0,3,0));breakMinus.setOnClickListener(v->{if(!timerRunning){selectedBreakMinutes=Math.max(1,selectedBreakMinutes-1);if(timerIsBreak)timerRemainingMs=selectedBreakMinutes*60000L;breakTimeValue.setText(selectedBreakMinutes+" минут");if(timerIsBreak)timerDisplay.setText(formatTimer(timerRemainingMs));}});breakPlus.setOnClickListener(v->{if(!timerRunning){selectedBreakMinutes=Math.min(600,selectedBreakMinutes+1);if(timerIsBreak)timerRemainingMs=selectedBreakMinutes*60000L;breakTimeValue.setText(selectedBreakMinutes+" минут");if(timerIsBreak)timerDisplay.setText(formatTimer(timerRemainingMs));}});breakTimeValue.setOnClickListener(v->{if(!timerRunning)new TimePickerDialog(this,(p,h,m)->{selectedBreakMinutes=Math.max(1,h*60+m);if(timerIsBreak)timerRemainingMs=selectedBreakMinutes*60000L;breakTimeValue.setText(selectedBreakMinutes+" минут");if(timerIsBreak)timerDisplay.setText(formatTimer(timerRemainingMs));},selectedBreakMinutes/60,selectedBreakMinutes%60,true).show();});
        settings.addView(breaksRow,lp(-1,48,0,3,0,0));root.addView(settings,lp(-1,-2,0,0,0,14));
        LinearLayout timerControls = new LinearLayout(this); timerControls.setOrientation(LinearLayout.HORIZONTAL); timerControls.setGravity(Gravity.CENTER);
        timerStartButton=button(timerRunning?"Идёт":"▶  Начать",true);timerStartButton.setEnabled(!timerRunning);timerStartButton.setOnClickListener(v->{if(!timerIsBreak&&timerRemainingMs>0&&timerRemainingMs>=selectedStudyMinutes*60000L-1000L)showStudyRitual();else startTimer();});
        timerPauseButton=button("Ⅱ  Пауза",false);timerPauseButton.setEnabled(timerRunning);timerPauseButton.setOnClickListener(v->pauseTimer());
        Button reset=button("↺  Сброс",false);reset.setOnClickListener(v->{long elapsed=timerRunning&&!timerIsBreak?Math.max(0,SystemClock.elapsedRealtime()-timerSessionStarted):0;timerHandler.removeCallbacks(timerTick);timerRunning=false;cancelTimerFinishAlarm();try{startService(new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_STOP).putExtra("elapsed_ms",elapsed).putExtra("break",timerIsBreak));}catch(Exception ignored){}getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}if(timerDisplay!=null)timerDisplay.animate().cancel();timerIsBreak=false;timerRemainingMs=selectedStudyMinutes*60000L;showPomodoro();});
        Button[] controls={timerStartButton,timerPauseButton,reset};
        for(Button control:controls){
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1f); cp.setMargins(dp(3),0,dp(3),0);
            control.setPadding(dp(3),dp(4),dp(3),dp(4)); control.setTextSize(12); control.setMinHeight(0);
            int normal=control==timerStartButton?PINK:PALE, pressed=control==timerStartButton?0xFF9F526B:0xFFD9C5CC;
            android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed},shape(pressed,14,pressed));
            states.addState(new int[]{android.R.attr.state_enabled},shape(normal,14,control==timerStartButton?PINK:0));
            states.addState(new int[]{},shape(0xFFE7E1E4,14,0));
            control.setBackground(states);
            control.setOnTouchListener((view,event)->{
                if(event.getAction()==android.view.MotionEvent.ACTION_DOWN)view.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.82f).setDuration(75).start();
                else if(event.getAction()==android.view.MotionEvent.ACTION_UP||event.getAction()==android.view.MotionEvent.ACTION_CANCEL)view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(130).start();
                return false;
            });
            timerControls.addView(control,cp);
        }
        root.addView(timerControls,lp(-1,52,0,0,0,0));
        if(timerRemainingMs<=0)timerRemainingMs=(timerIsBreak?selectedBreakMinutes:selectedStudyMinutes)*60000L;
        if(timerRunning){updateTimerDisplay();timerStartButton.setEnabled(false);timerPauseButton.setEnabled(true);if(timerTick==null){timerTick=new Runnable(){public void run(){updateTimerDisplay();long left=timerEndElapsed-SystemClock.elapsedRealtime();if(left<=0)finishTimerPhase();else timerHandler.postDelayed(this,250);}};timerHandler.post(timerTick);}}
    }

    private String formatTimer(long ms) {
        long sec = Math.max(0, ms / 1000); return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
    }

    private void startTimer() {
        if (timerRunning) return;
        if (timerRemainingMs <= 0) timerRemainingMs = (timerIsBreak ? selectedBreakMinutes : selectedStudyMinutes) * 60000L;
        timerRunning = true; timerSessionStarted = SystemClock.elapsedRealtime(); timerEndElapsed = timerSessionStarted + timerRemainingMs;
        if(!timerIsBreak)prefs.edit().putLong("focus_started_wall",System.currentTimeMillis()).putBoolean("focus_left_app",false).apply();
        try{Intent service=new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_START).putExtra("duration_ms",timerRemainingMs).putExtra("break",timerIsBreak);if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);}catch(Exception ignored){}
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timerTick = new Runnable() { @Override public void run() { updateTimerDisplay(); long left = timerEndElapsed - SystemClock.elapsedRealtime(); if (left <= 0) finishTimerPhase(); else timerHandler.postDelayed(this, 250); } };
        if(timerPulseAnimator!=null)timerPulseAnimator.cancel();if(timerDisplay!=null){timerPulseAnimator=android.animation.ValueAnimator.ofFloat(1f,1.045f);timerPulseAnimator.setDuration(1050);timerPulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);timerPulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);timerPulseAnimator.addUpdateListener(a->{if(timerDisplay!=null){float s=(float)a.getAnimatedValue();timerDisplay.setScaleX(s);timerDisplay.setScaleY(s);}});timerPulseAnimator.start();}
        if (timerStartButton != null) timerStartButton.setEnabled(false);
        if (timerPauseButton != null) timerPauseButton.setEnabled(true);
        timerHandler.post(timerTick);
    }

    private void pauseTimer() {
        if (!timerRunning) return;
        timerRemainingMs = Math.max(0, timerEndElapsed - SystemClock.elapsedRealtime());
        timerRunning = false; timerHandler.removeCallbacks(timerTick); cancelTimerFinishAlarm(); try{Intent pause=new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_PAUSE).putExtra("elapsed_ms",Math.max(0,SystemClock.elapsedRealtime()-timerSessionStarted)).putExtra("break",timerIsBreak);startService(pause);}catch(Exception ignored){} getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}if (timerDisplay != null) { timerDisplay.setText(formatTimer(timerRemainingMs)); timerDisplay.animate().scaleX(1f).scaleY(1f).setDuration(220).start(); }
        if (timerStartButton != null) { timerStartButton.setEnabled(true); timerStartButton.setText("▶  Продолжить"); }
        if (timerPauseButton != null) timerPauseButton.setEnabled(false);
    }

    private boolean isMaleProfile(){return prefs!=null && "male".equals(prefs.getString("profile_gender","female"));}
    private String g(String feminine,String masculine){return isMaleProfile()?masculine:feminine;}
    private void updateTimerDisplay() {
        if (timerDisplay != null) timerDisplay.setText(formatTimer(Math.max(0, timerEndElapsed - SystemClock.elapsedRealtime())));
    }

    private void cancelTimerFinishAlarm() {
        try { Intent intent=new Intent(this,ReminderReceiver.class).setAction("study_timer_finish");PendingIntent pi=PendingIntent.getBroadcast(this,910,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);pi.cancel(); } catch(Exception ignored) { }
    }

    private void showTimerNotification(String title,String message) {
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String channel="study_timer_finished";
        android.app.NotificationManager manager=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){android.app.NotificationChannel c=new android.app.NotificationChannel(channel,"Окончание таймера",android.app.NotificationManager.IMPORTANCE_DEFAULT);c.setDescription("Уведомления о завершении учёбы и перерыва");manager.createNotificationChannel(c);}
        Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,910,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(this,channel):new android.app.Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(message).setAutoCancel(true).setContentIntent(pi);
        manager.notify(910,builder.build());
    }

    private void finishTimerPhase() {
        boolean focusFinished=!timerIsBreak;
        timerHandler.removeCallbacks(timerTick); timerRunning = false; cancelTimerFinishAlarm(); getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;} timerRemainingMs = 0;
        if (!timerIsBreak) { timerIsBreak = true; timerRemainingMs = selectedBreakMinutes * 60000L; Toast.makeText(this, g("Ты большая умничка! Время отдохнуть 🌸","Ты большой молодец! Время отдохнуть 🌸"), Toast.LENGTH_LONG).show(); }
        else { timerIsBreak = false; timerRemainingMs = selectedStudyMinutes * 60000L; Toast.makeText(this, g("Перерыв окончен. Ты готова продолжать! 💗","Перерыв окончен. Ты готов продолжать! 💗"), Toast.LENGTH_LONG).show(); }
        if ("pomodoro".equals(currentScreen)) showPomodoro();
        if(focusFinished){prefs.edit().putBoolean("diary_prefill_timer_win",true).putBoolean("timer_finish_prompt_pending",false).apply();showTimerFinishPrompt();}
    }

    private void showTimerFinishPrompt(){new android.app.AlertDialog.Builder(this).setTitle("Фокус-сессия завершена 🌸").setMessage("Хочешь записать эту маленькую победу в дневник?").setNegativeButton("Позже",null).setPositiveButton("Записать",(d,w)->showDiary()).show();}

    // App usage is tracked separately; only an explicit study action records a study day.
    private void saveForegroundUsage() { appUsageStartedElapsed=0L; }

    @Override protected void onResume() { super.onResume(); activityVisible=true; appUsageStartedElapsed = SystemClock.elapsedRealtime(); if (prefs != null) {updateAchievementDates();scheduleReminders(this);rescheduleFutureLetterReminders(this);WeeklyReportReceiver.schedule(this);StudyWidgetProvider.updateAll(this);if(prefs.getBoolean("timer_finish_prompt_pending",false)){prefs.edit().putBoolean("timer_finish_prompt_pending",false).apply();showTimerFinishPrompt();}} }
    @Override protected void onPause() { activityVisible=false; saveForegroundUsage(); appUsageStartedElapsed = 0L; super.onPause(); }

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

    private void showExam() {
        currentScreen="exam";base("Мой экзамен ✨","Выбери главную цель и следи за оставшимися днями");
        Set<String> savedIds=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));
        if(savedIds.isEmpty()&&!prefs.getString("exam_title","").isEmpty()&&!prefs.getString("exam_date","").isEmpty()){
            String legacy="legacy_exam";savedIds.add(legacy);
            prefs.edit().putString("exam_goal_title_"+legacy,prefs.getString("exam_title","")).putString("exam_goal_date_"+legacy,prefs.getString("exam_date","")).putStringSet("exam_goal_ids",savedIds).apply();
        }
        LinearLayout hero=card();hero.setBackground(shape(0xFFFFF0F4,24,0xFFF4DCE5));hero.setPadding(dp(18),dp(16),dp(18),dp(16));
        addText(hero,"ГЛАВНЫЙ ЭКЗАМЕН",11,PINK,true);
        LinearLayout heroRow=new LinearLayout(this);heroRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heroWords=new LinearLayout(this);heroWords.setOrientation(LinearLayout.VERTICAL);
        TextView heroName=text("Главная цель пока не выбрана",15,DARK,true);heroWords.addView(heroName);
        TextView heroDate=text("Выбери её в списке ниже",12,MUTED,false);heroWords.addView(heroDate);
        heroRow.addView(heroWords,new LinearLayout.LayoutParams(0,-2,1f));
        LinearLayout countdown=new LinearLayout(this);countdown.setOrientation(LinearLayout.VERTICAL);countdown.setGravity(Gravity.CENTER);
        TextView heroDays=text("—",38,PINK,true);heroDays.setGravity(Gravity.CENTER);
        TextView heroCaption=text("ДНЕЙ",10,MUTED,true);heroCaption.setGravity(Gravity.CENTER);
        countdown.addView(heroDays);countdown.addView(heroCaption);heroRow.addView(countdown,lp(88,-2,8,0,0,0));
        hero.addView(heroRow,lp(-1,-2,0,5,0,0));root.addView(hero,lp(-1,-2,0,0,0,14));
        final String[] editing={null};
        LinearLayout form=card();addText(form,"НАЗВАНИЕ ЦЕЛИ",12,PINK,true);
        EditText title=new EditText(this);title.setSingleLine(true);title.setTextSize(16);title.setTextColor(DARK);title.setHint("Например, экзамен по математике");title.setPadding(dp(13),dp(8),dp(13),dp(8));title.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));form.addView(title,lp(-1,50,0,7,0,12));
        addText(form,"ДАТА",12,PINK,true);
        EditText date=new EditText(this);date.setSingleLine(true);date.setFocusable(false);date.setClickable(true);date.setTextSize(15);date.setTextColor(DARK);date.setHint("Выбери дату");date.setPadding(dp(13),dp(8),dp(13),dp(8));date.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));form.addView(date,lp(-1,50,0,7,0,12));
        date.setOnClickListener(v->{Calendar now=Calendar.getInstance(AppTime.zone());Calendar min=(Calendar)now.clone();min.set(Calendar.HOUR_OF_DAY,0);min.set(Calendar.MINUTE,0);min.set(Calendar.SECOND,0);min.set(Calendar.MILLISECOND,0);android.app.DatePickerDialog dialog=new android.app.DatePickerDialog(this,(p,y,m,d)->date.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH));dialog.getDatePicker().setMinDate(min.getTimeInMillis());dialog.show();});
        Button save=button("＋  Добавить цель",true);form.addView(save,lp(-1,50,0,4,0,0));root.addView(form,lp(-1,-2,0,0,0,14));
        LinearLayout goals=new LinearLayout(this);goals.setOrientation(LinearLayout.VERTICAL);root.addView(goals,lp(-1,-2,0,0,0,0));
        final Runnable[] render={null};
        render[0]=()->{
            goals.removeAllViews();
            Set<String> ids=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));
            List<String> order=new ArrayList<>(ids);java.util.Collections.sort(order,java.util.Collections.reverseOrder());
            String mainId=prefs.getString("exam_main_goal_id","");
            if(!ids.contains(mainId))mainId="";
            if(mainId.isEmpty()&&!order.isEmpty())mainId=order.get(0);
            final String selectedMain=mainId;
            if(!selectedMain.isEmpty()){
                String mainTitle=prefs.getString("exam_goal_title_"+selectedMain,""),mainDate=prefs.getString("exam_goal_date_"+selectedMain,"");
                long remain=daysUntil(mainDate);heroName.setText(mainTitle);heroDate.setText("Дата экзамена: "+mainDate);
                heroDays.setText(remain<0?"—":String.valueOf(remain));
                heroCaption.setText(remain==0?"СЕГОДНЯ":"ДНЕЙ ОСТАЛОСЬ");
                prefs.edit().putString("exam_main_goal_id",selectedMain).apply();
            }else{heroName.setText("Главная цель пока не выбрана");heroDate.setText("Добавь цель и отметь её главной");heroDays.setText("—");heroCaption.setText("ДНЕЙ");}
            if(order.isEmpty()){LinearLayout empty=card();empty.setGravity(Gravity.CENTER);addText(empty,"Пока нет целей",15,DARK,true);addText(empty,"Добавь первую дату экзамена или важную цель.",13,MUTED,false);goals.addView(empty,lp(-1,-2,0,0,0,10));}
            for(String id:order){
                String goalTitle=prefs.getString("exam_goal_title_"+id,""),goalDate=prefs.getString("exam_goal_date_"+id,"");if(goalTitle.isEmpty()||goalDate.isEmpty())continue;
                long remain=daysUntil(goalDate);boolean isMain=id.equals(selectedMain);
                LinearLayout item=card();item.setOrientation(LinearLayout.VERTICAL);
                LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
                addText(words,isMain?"ГЛАВНАЯ ЦЕЛЬ":"ТВОЯ ЦЕЛЬ",11,isMain?PINK:MUTED,true);addText(words,goalTitle,17,DARK,true);addText(words,"Экзамен: "+goalDate,12,MUTED,false);addText(words,"Добавлена: "+prefs.getString("exam_goal_created_"+id,"—"),11,MUTED,false);
                top.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
                LinearLayout number=new LinearLayout(this);number.setOrientation(LinearLayout.VERTICAL);number.setGravity(Gravity.CENTER);number.setBackground(shape(isMain?0xFFFFF0F4:0xFFF7F4F2,18,0));TextView big=text(remain<0?"—":String.valueOf(remain),30,PINK,true);big.setGravity(Gravity.CENTER);TextView unit=text(remain==0?"СЕГОДНЯ":"ДНЕЙ",9,MUTED,true);unit.setGravity(Gravity.CENTER);number.addView(big);number.addView(unit);top.addView(number,lp(82,78,8,0,0,0));item.addView(top);
                LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
                Button main=button(isMain?"★  Главная цель":"☆  Сделать главной",isMain);main.setGravity(Gravity.CENTER);main.setIncludeFontPadding(false);main.setPadding(dp(2),0,dp(2),0);Button edit=button("Изменить",false),delete=button("Удалить",false);
                main.setOnClickListener(v->{prefs.edit().putString("exam_main_goal_id",id).apply();render[0].run();});
                edit.setOnClickListener(v->{editing[0]=id;title.setText(goalTitle);date.setText(goalDate);save.setText("Сохранить изменения");form.requestFocus();});
                delete.setOnClickListener(v->{TextView sparkle=text("✦   ✧   ✦",25,PINK,true);sparkle.setGravity(Gravity.CENTER);item.addView(sparkle,0,lp(-1,34,0,0,0,0));sparkle.setAlpha(0f);sparkle.animate().alpha(1f).translationY(-dp(8)).setDuration(180).start();item.animate().alpha(0f).scaleX(.82f).scaleY(.82f).translationX(dp(28)).setDuration(300).withEndAction(()->{Set<String> updated=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));updated.remove(id);SharedPreferences.Editor e=prefs.edit().remove("exam_goal_title_"+id).remove("exam_goal_date_"+id).remove("exam_goal_created_"+id).putStringSet("exam_goal_ids",updated);if(id.equals(prefs.getString("exam_main_goal_id","")))e.remove("exam_main_goal_id");e.apply();if(id.equals(editing[0])){editing[0]=null;title.setText("");date.setText("");save.setText("＋  Добавить цель");}render[0].run();});});
                LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(48),1f);mp.setMargins(0,dp(8),dp(3),0);actions.addView(main,mp);
                LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(48),.75f);ep.setMargins(dp(3),dp(8),dp(3),0);actions.addView(edit,ep);
                LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(0,dp(48),.65f);dpv.setMargins(dp(3),dp(8),0,0);actions.addView(delete,dpv);
                item.addView(actions);goals.addView(item,lp(-1,-2,0,0,0,10));
            }
        };
        save.setOnClickListener(v->{String n=title.getText().toString().trim(),d=date.getText().toString().trim();if(n.isEmpty()){title.setError("Введи название цели");return;}if(d.isEmpty()){showSavedBanner("Выбери дату","Укажи день экзамена ♡");return;}if(daysUntil(d)<0){showSavedBanner("Неверная дата","Выбери сегодняшнюю или будущую дату");return;}boolean isNew=editing[0]==null;String id=isNew?String.valueOf(System.currentTimeMillis()):editing[0];Set<String> ids=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));ids.add(id);SharedPreferences.Editor e=prefs.edit().putString("exam_goal_title_"+id,n).putString("exam_goal_date_"+id,d).putStringSet("exam_goal_ids",ids);if(isNew)e.putString("exam_goal_created_"+id,todayKey());if(prefs.getString("exam_main_goal_id","").isEmpty())e.putString("exam_main_goal_id",id);e.apply();StudyWidgetProvider.updateAll(this);editing[0]=null;title.setText("");date.setText("");save.setText("＋  Добавить цель");render[0].run();showSavedBanner("Цель сохранена","Она появилась в твоём списке ♡");});
        render[0].run();
        addExamTopicSelector();
    }
    private String examTopicKey(String chapter,String topic){return topicStorageSuffix(chapter,topic);}
    private boolean isTopicInExamScope(String chapter,String topic){
        if(!prefs.getBoolean("exam_topic_filter_enabled",false))return true;
        return prefs.getStringSet("exam_topic_ids",new TreeSet<String>()).contains(examTopicKey(chapter,topic));
    }
    private void addExamTopicSelector(){
        LinearLayout section=card();addText(section,"📚  ТЕМЫ ДЛЯ ЭКЗАМЕНА",12,PINK,true);
        addText(section,"Отметь только те темы, которые входят в твой экзамен. При включённом фильтре общий прогресс и планировщик будут учитывать именно их.",12,MUTED,false);
        CheckBox enabled=new CheckBox(this);enabled.setText("Считать прогресс и план только по отмеченным темам");enabled.setTextColor(DARK);enabled.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));enabled.setChecked(prefs.getBoolean("exam_topic_filter_enabled",false));section.addView(enabled,lp(-1,-2,0,5,0,6));
        enabled.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("exam_topic_filter_enabled",on).apply();Toast.makeText(this,on?"Фильтр тем экзамена включён":"Весь учебный план снова учитывается",Toast.LENGTH_SHORT).show();});
        LinearLayout actions=new LinearLayout(this);Button all=button("Выбрать все",false),none=button("Снять выбор",false);actions.addView(all,new LinearLayout.LayoutParams(0,dp(48),1f));actions.addView(none,new LinearLayout.LayoutParams(0,dp(48),1f));section.addView(actions,lp(-1, 48,0,0,0,6));
        all.setOnClickListener(v->{Set<String> ids=new TreeSet<>();for(Map.Entry<String,List<String>> e:chapters.entrySet())for(String topic:e.getValue())ids.add(examTopicKey(e.getKey(),topic));prefs.edit().putStringSet("exam_topic_ids",ids).apply();showExam();});
        none.setOnClickListener(v->{prefs.edit().putStringSet("exam_topic_ids",new TreeSet<String>()).apply();showExam();});
        Set<String> selected=new TreeSet<>(prefs.getStringSet("exam_topic_ids",new TreeSet<String>()));
        int allTopicCount=0;for(List<String> topics:chapters.values())allTopicCount+=topics.size();final int totalTopicCount=allTopicCount;
        TextView selectionSummary=text("Отмечено тем: "+selected.size()+" из "+totalTopicCount,12,MUTED,false);section.addView(selectionSummary,lp(-1,24,0,0,0,4));
        if(enabled.isChecked()&&selected.isEmpty())addText(section,"Сначала отметь хотя бы одну тему — иначе экзаменационный прогресс будет равен 0%.",12,PINK,true);
        for(Map.Entry<String,List<String>> entry:chapters.entrySet()){
            addText(section,entry.getKey().replaceFirst("^[^ ]+\\s*",""),13,DARK,true);
            for(String topic:entry.getValue()){
                String key=examTopicKey(entry.getKey(),topic);CheckBox check=new CheckBox(this);check.setText(topic);check.setTextSize(12);check.setTextColor(DARK);check.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));check.setChecked(selected.contains(key));section.addView(check,lp(-1,-2,0,0,0,1));
                check.setOnCheckedChangeListener((v,checked)->{Set<String> ids=new TreeSet<>(prefs.getStringSet("exam_topic_ids",new TreeSet<String>()));if(checked)ids.add(key);else ids.remove(key);prefs.edit().putStringSet("exam_topic_ids",ids).apply();selectionSummary.setText("Отмечено тем: "+ids.size()+" из "+totalTopicCount);});
            }
        }
        root.addView(section,lp(-1,-2,0,0,0,14));
    }
    private String createBuddyCode(String sticker){
        try{org.json.JSONObject o=new org.json.JSONObject();o.put("version",1);o.put("alias",prefs.getString("buddy_alias",prefs.getString("display_name","Напарник")));o.put("weekly_goal",prefs.getInt("weekly_goal_minutes",300));o.put("weekly_done",weeklyMinutesLast7Days());o.put("sticker",sticker==null?"🌷":sticker);o.put("message","Рада учиться рядом, даже если мы занимаемся в разное время ♡");o.put("date",todayKey());return "SWL_BUDDY_V1:"+android.util.Base64.encodeToString(o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);}catch(Exception e){return "";}}
    private boolean importBuddyCode(String raw){
        try{String code=raw.trim();if(code.startsWith("SWL_BUDDY_V1:"))code=code.substring("SWL_BUDDY_V1:".length());byte[] bytes=android.util.Base64.decode(code,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);org.json.JSONObject o=new org.json.JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));if(o.optInt("version",0)!=1)return false;String alias=o.optString("alias","Напарник").trim();int goal=o.optInt("weekly_goal",0),done=o.optInt("weekly_done",0);if(alias.isEmpty()||alias.length()>30||goal<30||goal>10080||done<0||done>10080)return false;prefs.edit().putString("buddy_partner_alias",alias).putInt("buddy_partner_goal",goal).putInt("buddy_partner_done",done).putString("buddy_partner_sticker",o.optString("sticker","🌷")).putString("buddy_partner_message",o.optString("message","")).putString("buddy_partner_date",o.optString("date","")).apply();return true;}catch(Exception e){return false;}}
    private void showStudyBuddy(){
        currentScreen="study_buddy";base("Напарник по учёбе ♡","Поддерживайте друг друга без соревнования и спешки");
        LinearLayout own=card();addText(own,"ТВОЯ НЕДЕЛЯ",11,PINK,true);int ownGoal=prefs.getInt("weekly_goal_minutes",300),ownDone=weeklyMinutesLast7Days();addText(own,ownDone+" из "+ownGoal+" минут",22,DARK,true);addProgressLine(own,"Твой вклад в общую цель",ownGoal<=0?0:Math.min(100,ownDone*100/ownGoal));EditText alias=new EditText(this);alias.setSingleLine(true);alias.setText(prefs.getString("buddy_alias",prefs.getString("display_name","")));alias.setHint("Как тебя называть напарнику");alias.setTextColor(DARK);alias.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));own.addView(alias,lp(-1,48,0,7,0,6));Button saveAlias=button("Сохранить имя для обмена",false);saveAlias.setOnClickListener(v->{String n=alias.getText().toString().trim();if(n.isEmpty()||n.length()>30){alias.setError("До 30 символов");return;}prefs.edit().putString("buddy_alias",n).apply();Toast.makeText(this,"Имя сохранено",Toast.LENGTH_SHORT).show();});own.addView(saveAlias,lp(-1, 48,0,0,0,0));root.addView(own,lp(-1,-2,0,0,0,10));
        String partner=prefs.getString("buddy_partner_alias","");if(!partner.isEmpty()){LinearLayout shared=card();addText(shared,"ВАШ ОБЩИЙ РИТМ",11,PINK,true);int partnerDone=prefs.getInt("buddy_partner_done",0),partnerGoal=prefs.getInt("buddy_partner_goal",300),combinedGoal=ownGoal+partnerGoal,combinedDone=ownDone+partnerDone;addText(shared,"Ты + "+partner,18,DARK,true);addText(shared,combinedDone+" из "+combinedGoal+" минут за неделю",15,DARK,true);addProgressLine(shared,"Общая цель",combinedGoal<=0?0:Math.min(100,combinedDone*100/combinedGoal));String sticker=prefs.getString("buddy_partner_sticker","🌷");addText(shared,"Стикер от напарника: "+sticker,14,PINK,true);addText(shared,prefs.getString("buddy_partner_message",""),12,MUTED,false);addText(shared,"Последнее обновление: "+prefs.getString("buddy_partner_date","—"),11,MUTED,false);root.addView(shared,lp(-1,-2,0,0,0,10));}
        LinearLayout exchange=card();addText(exchange,"ОБМЕН ОБНОВЛЕНИЯМИ",11,PINK,true);addText(exchange,"Создай короткий код и отправь его напарнику любым удобным способом. Он сможет прислать ответный код — синхронизация асинхронная, без аккаунтов и публичного профиля.",12,MUTED,false);
        Button share=button("↗  Отправить недельное обновление",true);exchange.addView(share,lp(-1,48,0,7,0,6));share.setOnClickListener(v->{String code=createBuddyCode("🌷");if(code.isEmpty())return;Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_SUBJECT,"Недельное обновление");send.putExtra(Intent.EXTRA_TEXT,code);startActivity(Intent.createChooser(send,"Отправить напарнику"));});
        Button sticker=button("♡  Отправить тёплый стикер",false);exchange.addView(sticker,lp(-1, 48,0,0,0,8));sticker.setOnClickListener(v->{String[] options={"🌷 Поддерживаю!","✨ У тебя получится!","☕ Время маленького перерыва","💗 Спасибо за совместную учёбу"};new android.app.AlertDialog.Builder(this).setTitle("Выбери стикер").setItems(options,(d,w)->{String code=createBuddyCode(options[w].substring(0,2));Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,code);startActivity(Intent.createChooser(send,"Отправить стикер"));}).show();});
        EditText incoming=new EditText(this);incoming.setHint("Вставь код, который прислал напарник");incoming.setMinLines(2);incoming.setGravity(Gravity.TOP);incoming.setTextSize(12);incoming.setTextColor(DARK);incoming.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));exchange.addView(incoming,lp(-1,76,0,0,0,6));Button accept=button("Получить обновление",false);exchange.addView(accept,lp(-1, 48,0,0,0,0));accept.setOnClickListener(v->{if(importBuddyCode(incoming.getText().toString())){Toast.makeText(this,"Обновление напарника принято ♡",Toast.LENGTH_SHORT).show();showStudyBuddy();}else incoming.setError("Код не распознан. Проверь, что скопирован весь текст.");});root.addView(exchange,lp(-1,-2,0,0,0,10));
        Button back=button("← На главную",true);back.setOnClickListener(v->showHome());root.addView(back,lp(-1,48,0,0,0,0));
    }

    private long daysUntil(String isoDate) {
        try {SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);f.setTimeZone(AppTime.zone());Date target=f.parse(isoDate);Calendar a=Calendar.getInstance(AppTime.zone());a.set(Calendar.HOUR_OF_DAY,0);a.set(Calendar.MINUTE,0);a.set(Calendar.SECOND,0);a.set(Calendar.MILLISECOND,0);Calendar b=Calendar.getInstance(AppTime.zone());b.setTime(target);b.set(Calendar.HOUR_OF_DAY,0);b.set(Calendar.MINUTE,0);b.set(Calendar.SECOND,0);b.set(Calendar.MILLISECOND,0);return (b.getTimeInMillis()-a.getTimeInMillis())/86400000L;}catch(Exception e){return -1;}
    }

    private void showDiary() {
        currentScreen="diary";
        base("Дневник успехов ♡","Сохраняй моменты, которыми ты гордишься");
        addText(root,"МАЛЕНЬКАЯ ПОБЕДА СЕГОДНЯ",11,PINK,true);
        EditText note=new EditText(this);note.setHint("Сегодня у меня получилось...");if(prefs.getBoolean("diary_prefill_timer_win",false)){note.setText(g("Я завершила фокус-сессию и уделила время своей цели. ♡","Я завершил фокус-сессию и уделил время своей цели. ♡"));prefs.edit().remove("diary_prefill_timer_win").apply();}note.setTextSize(17);note.setTextColor(DARK);note.setHintTextColor(MUTED);note.setGravity(Gravity.TOP);note.setMinLines(3);note.setPadding(dp(2),dp(10),dp(2),dp(10));note.setBackgroundColor(0x00000000);
        root.addView(note,lp(-1,104,0,0,0,7));
        final String[] selectedTag={"Учёба"}, editingId={null}, activeFilter={"Все"}, queryValue={""};
        Set<String> customTags=new TreeSet<>(prefs.getStringSet("diary_custom_tags",new TreeSet<String>()));
        LinearLayout tags=new LinearLayout(this);tags.setOrientation(LinearLayout.HORIZONTAL);tags.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView tagScroll=new HorizontalScrollView(this);tagScroll.setHorizontalScrollBarEnabled(false);tagScroll.addView(tags);root.addView(tagScroll,lp(-1,48,0,0,0,7));
        Runnable[] render={null};
        Runnable[] refreshTags=new Runnable[1];
        refreshTags[0]=()->{
            tags.removeAllViews();
            List<String> names=new ArrayList<>();names.add("Учёба");names.add("Дисциплина");names.add("Отдых");names.add("Прогресс");for(String t:customTags)if(!names.contains(t))names.add(t);
            for(String tag:names){TextView chip=text("#"+tag,12,tag.equals(selectedTag[0])?PINK:MUTED,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(11),dp(7),dp(11),dp(7));chip.setBackground(shape(tag.equals(selectedTag[0])?0xFFFFE8EF:0xFFF7F3F4,18,tag.equals(selectedTag[0])?PINK:0xFFEDE3E7));tags.addView(chip,lp(-2,48,0,0,6,0));chip.setOnClickListener(v->{selectedTag[0]=tag;for(int k=0;k<tags.getChildCount();k++){TextView q=(TextView)tags.getChildAt(k);boolean on=q.getText().toString().equals("#"+tag);q.setTextColor(on?PINK:MUTED);q.setBackground(shape(on?0xFFFFE8EF:0xFFF7F3F4,18,on?PINK:0xFFEDE3E7));}});}
            TextView addTag=text("＋ Свой тег",11,PINK,true);addTag.setGravity(Gravity.CENTER);addTag.setPadding(dp(11),dp(7),dp(11),dp(7));addTag.setBackground(shape(PALE,18,0xFFEADDE2));tags.addView(addTag,lp(-2,48,0,0,0,0));
            addTag.setOnClickListener(v->{EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(15);input.setTextColor(DARK);input.setHint("Например: Книги");input.setPadding(dp(12),dp(8),dp(12),dp(8));input.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));LinearLayout wrap=new LinearLayout(this);wrap.setPadding(dp(20),dp(8),dp(20),0);wrap.addView(input,lp(-1,48,0,0,0,0));new android.app.AlertDialog.Builder(this).setTitle("Новый хэштег").setView(wrap).setNegativeButton("Отмена",null).setPositiveButton("Добавить",(d,w)->{String tag=input.getText().toString().trim().replace("#","").replaceAll("\\s+","");if(tag.isEmpty()){Toast.makeText(this,"Введи название тега",Toast.LENGTH_SHORT).show();return;}customTags.add(tag);prefs.edit().putStringSet("diary_custom_tags",customTags).apply();selectedTag[0]=tag;refreshTags[0].run();if(render[0]!=null)render[0].run();}).show();});
        };
        refreshTags[0].run();
        Button save=button("✦  Сохранить победу",true);save.setTextSize(15);save.setElevation(dp(4));root.addView(save,lp(-1,51,0,4,0,15));
        TextView memoryTitle=text("Вспомнить успех ✨",16,DARK,true);root.addView(memoryTitle,lp(-1,-2,0,0,0,7));
        Set<String> all=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));List<String> old=new ArrayList<>(all);java.util.Collections.sort(old,java.util.Collections.reverseOrder());
        if(old.size()>1){String pick=old.get(1);LinearLayout memory=new LinearLayout(this);memory.setOrientation(LinearLayout.VERTICAL);memory.setPadding(dp(13),dp(10),dp(13),dp(10));memory.setBackground(shape(0xFFFFF2F6,17,0));addText(memory,prefs.getString("diary_"+pick,""),13,DARK,false);addText(memory,"Твоя прошлая маленькая победа ♡",10,PINK,true);root.addView(memory,lp(-1,-2,0,0,0,14));}
        else addText(root,"Здесь появится одна из твоих прошлых записей — пока сохрани первую победу.",11,MUTED,false);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("ТВОИ ЗАПИСИ",11,PINK,true);heading.addView(title,new LinearLayout.LayoutParams(0,dp(30),1f));
        TextView counter=text("",11,MUTED,true);heading.addView(counter,lp(-2,30,0,0,0,0));
        root.addView(heading,lp(-1,34,0,0,0,4));
        LinearLayout searchWrap=new LinearLayout(this);searchWrap.setGravity(Gravity.CENTER_VERTICAL);searchWrap.setPadding(dp(11),0,dp(8),0);searchWrap.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));
        TextView magnify=text("⌕",23,PINK,true);magnify.setGravity(Gravity.CENTER);searchWrap.addView(magnify,lp(27,42,0,0,5,0));
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(13);search.setTextColor(DARK);search.setHintTextColor(MUTED);search.setHint("Поиск по победам...");search.setPadding(0,0,0,0);search.setBackgroundColor(0x00000000);searchWrap.addView(search,new LinearLayout.LayoutParams(0,dp(48),1f));
        TextView clear=text("×",22,MUTED,true);clear.setGravity(Gravity.CENTER);clear.setVisibility(View.GONE);searchWrap.addView(clear,lp(48,48,0,0,0,0));root.addView(searchWrap,lp(-1, 48,0,0,0,7));
        LinearLayout filterRow=new LinearLayout(this);filterRow.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView filterScroll=new HorizontalScrollView(this);filterScroll.setHorizontalScrollBarEnabled(false);filterScroll.addView(filterRow);root.addView(filterScroll,lp(-1, 48,0,0,0,4));
        LinearLayout entries=new LinearLayout(this);entries.setOrientation(LinearLayout.VERTICAL);root.addView(entries,lp(-1,-2,0,0,0,0));
        render[0]=()->{
            entries.removeAllViews();filterRow.removeAllViews();if(heading.getChildCount()>2)heading.removeViews(2,heading.getChildCount()-2);
            List<String> filters=new ArrayList<>();filters.add("Все");filters.add("Учёба");filters.add("Дисциплина");filters.add("Отдых");filters.add("Прогресс");for(String t:customTags)if(!filters.contains(t))filters.add(t);
            for(String filter:filters){TextView chip=text(filter.equals("Все")?"Все":"#"+filter,12,activeFilter[0].equals(filter)?PINK:MUTED,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(10),dp(6),dp(10),dp(6));chip.setBackground(shape(activeFilter[0].equals(filter)?0xFFFFE8EF:0xFFF7F3F4,16,activeFilter[0].equals(filter)?PINK:0xFFEDE3E7));filterRow.addView(chip,lp(-2,48,0,0,5,0));chip.setOnClickListener(v->{activeFilter[0]=filter;render[0].run();});}
            String q=queryValue[0].trim().toLowerCase(new Locale("ru"));
            Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));List<String> sorted=new ArrayList<>(ids);java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());
            List<String> matches=new ArrayList<>();
            for(String id:sorted){String value=prefs.getString("diary_"+id,"");if(value.isEmpty())continue;String tag=prefs.getString("diary_tag_"+id,"Учёба");String date=new SimpleDateFormat("d MMMM yyyy · HH:mm",new Locale("ru")).format(new Date(Long.parseLong(id)));String iso=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(Long.parseLong(id)));if(!activeFilter[0].equals("Все")&&!activeFilter[0].equals(tag))continue;if(!q.isEmpty()&&!value.toLowerCase(new Locale("ru")).contains(q)&&!date.toLowerCase(new Locale("ru")).contains(q)&&!iso.contains(q))continue;matches.add(id);}
            boolean filtering=!q.isEmpty()||!activeFilter[0].equals("Все");
            counter.setText(filtering?"Найдено: "+matches.size():"Всего: "+ids.size());
            if(filtering){TextView reset=text("Сбросить",10,PINK,true);reset.setGravity(Gravity.CENTER);reset.setPadding(dp(8),0,0,0);heading.addView(reset,lp(-2,30,5,0,0,0));reset.setOnClickListener(v->{queryValue[0]="";search.setText("");activeFilter[0]="Все";render[0].run();});}
            if(matches.isEmpty()){addText(entries,filtering?"Ничего не найдено. Попробуй изменить запрос или фильтр.":"🌷 Здесь появятся твои маленькие победы. Сохрани первую запись — и возвращайся к ней в трудные дни.",13,MUTED,false);return;}
            String currentMonth="";
            Set<String> collapsed=new TreeSet<>(prefs.getStringSet("diary_collapsed_months",new TreeSet<String>()));
            for(String id:matches){long stamp=Long.parseLong(id);Calendar cal=Calendar.getInstance();cal.setTimeInMillis(stamp);String monthKey=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date(stamp));
                if(!monthKey.equals(currentMonth)){currentMonth=monthKey;String monthTitle=new SimpleDateFormat("LLLL yyyy",new Locale("ru")).format(new Date(stamp));monthTitle=monthTitle.substring(0,1).toUpperCase(new Locale("ru"))+monthTitle.substring(1);LinearLayout monthHead=new LinearLayout(this);monthHead.setGravity(Gravity.CENTER_VERTICAL);TextView month=text(monthTitle,12,PINK,true);monthHead.addView(month,new LinearLayout.LayoutParams(0,dp(36),1f));TextView arrow=text(collapsed.contains(monthKey)?"＋":"−",17,MUTED,true);arrow.setGravity(Gravity.CENTER);monthHead.addView(arrow,lp(30,34,0,0,0,0));entries.addView(monthHead,lp(-1,38,0,5,0,2));monthHead.setOnClickListener(v->{Set<String> state=new TreeSet<>(prefs.getStringSet("diary_collapsed_months",new TreeSet<String>()));if(state.contains(monthKey))state.remove(monthKey);else state.add(monthKey);prefs.edit().putStringSet("diary_collapsed_months",state).apply();render[0].run();});}
                if(collapsed.contains(monthKey))continue;
                String value=prefs.getString("diary_"+id,""),tag=prefs.getString("diary_tag_"+id,"Учёба");
                LinearLayout entry=new LinearLayout(this);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(2),dp(7),dp(2),dp(10));
                LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView date=text("✿  "+new SimpleDateFormat("d MMMM yyyy · HH:mm",new Locale("ru")).format(new Date(stamp)),10,PINK,true);top.addView(date,new LinearLayout.LayoutParams(0,dp(32),1f));
                TextView edit=text("✎",19,MUTED,true);edit.setGravity(Gravity.CENTER);top.addView(edit,lp(34,32,0,0,2,0));TextView del=text("⌫",18,MUTED,true);del.setGravity(Gravity.CENTER);top.addView(del,lp(34,32,0,0,0,0));entry.addView(top,lp(-1,34,0,0,0,2));
                TextView tagView=text("#"+tag,10,PINK,true);entry.addView(tagView,lp(-1,19,0,0,0,2));TextView body=text(value,14,DARK,false);body.setGravity(Gravity.TOP);body.setPadding(0,dp(2),0,dp(7));entry.addView(body,lp(-1,-2,0,0,0,0));
                LinearLayout editArea=new LinearLayout(this);editArea.setOrientation(LinearLayout.VERTICAL);editArea.setVisibility(View.GONE);EditText editText=new EditText(this);editText.setText(value);editText.setTextSize(14);editText.setTextColor(DARK);editText.setGravity(Gravity.TOP);editText.setMinLines(3);editText.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));editArea.addView(editText,lp(-1,92,0,0,0,6));
                Button saveInline=button("Сохранить",true),cancel=button("Отмена",false);editArea.addView(saveInline,lp(-1, 48,0,0,0,3));editArea.addView(cancel,lp(-1,38,0,0,0,0));entry.addView(editArea,lp(-1,-2,0,0,0,0));
                edit.setOnClickListener(v->{body.setVisibility(View.GONE);tagView.setVisibility(View.GONE);editArea.setVisibility(View.VISIBLE);});cancel.setOnClickListener(v->{editArea.setVisibility(View.GONE);body.setVisibility(View.VISIBLE);tagView.setVisibility(View.VISIBLE);});saveInline.setOnClickListener(v->{String updated=editText.getText().toString().trim();if(updated.isEmpty()){editText.setError("Напиши пару слов");return;}prefs.edit().putString("diary_"+id,updated).apply();render[0].run();});
                del.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("Удалить эту запись?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{Set<String> latest=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));latest.remove(id);prefs.edit().remove("diary_"+id).remove("diary_tag_"+id).putStringSet("diary_ids",latest).apply();render[0].run();}).show());
                View line=new View(this);line.setBackgroundColor(0xFFEFE5E8);entry.addView(line,lp(-1,1,0,5,0,0));entries.addView(entry,lp(-1,-2,0,0,0,5));
            }
        };
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){queryValue[0]=s.toString();clear.setVisibility(s.length()>0?View.VISIBLE:View.GONE);render[0].run();}public void afterTextChanged(android.text.Editable e){}});
        clear.setOnClickListener(v->search.setText(""));
        save.setOnClickListener(v->{String value=note.getText().toString().trim();if(value.isEmpty()){note.setError("Напиши хотя бы пару слов");return;}Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));String id=editingId[0]==null?String.valueOf(System.currentTimeMillis()):editingId[0];ids.add(id);prefs.edit().putString("diary_"+id,value).putString("diary_tag_"+id,selectedTag[0]).putStringSet("diary_ids",ids).apply();editingId[0]=null;note.setText("");save.setText("✦  Сохранить победу");render[0].run();showSavedBanner("Запись сохранена","Твоя маленькая победа осталась в дневнике ♡");});
        render[0].run();
    }

    private void showSavedBanner(String title, String subtitle) {
        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(16), dp(11), dp(18), dp(11));
        banner.setBackground(shape(PALE, 20, 0));
        banner.setElevation(dp(8));
        TextView icon = text("♡", 25, PINK, true);
        icon.setGravity(Gravity.CENTER);
        banner.addView(icon, lp(34, 36, 0, 0, 10, 0));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        addText(words, title, 14, DARK, true);
        addText(words, subtitle, 12, MUTED, false);
        banner.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        overlay.addView(banner, new android.widget.FrameLayout.LayoutParams(-1, -2));
        android.view.ViewGroup decor = (android.view.ViewGroup) getWindow().getDecorView();
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420)), -2,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.setMargins(dp(16), 0, dp(16), dp(28));
        decor.addView(overlay, params);
        overlay.setAlpha(0f);
        overlay.setTranslationY(dp(18));
        overlay.animate().alpha(1f).translationY(0).setDuration(220).start();
        overlay.postDelayed(() -> {
            if (overlay.getParent() != null) {
                overlay.animate().alpha(0f).translationY(dp(12)).setDuration(180)
                        .withEndAction(() -> { if (overlay.getParent() != null) decor.removeView(overlay); }).start();
            }
        }, 2400);
    }
    private void renderDiaryEntries(LinearLayout entries,EditText note,Button save,String[] editingId){
        entries.removeAllViews();Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));List<String> sorted=new ArrayList<>(ids);java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());
        if(sorted.isEmpty()){addText(entries,"🌷 Здесь появятся твои маленькие победы. Сохрани первую запись — и возвращайся к ней в трудные дни.",14,MUTED,false);}
        for(String id:sorted){String value=prefs.getString("diary_"+id,"");if(value.isEmpty())continue;
            LinearLayout entry=new LinearLayout(this);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(2),dp(9),dp(2),dp(12));
            LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
            TextView date=text("✿  "+new SimpleDateFormat("d MMMM yyyy · HH:mm",new Locale("ru")).format(new Date(Long.parseLong(id))),11,PINK,true);top.addView(date,new LinearLayout.LayoutParams(0,dp(34),1f));
            TextView edit=text("✎",21,MUTED,true);edit.setGravity(Gravity.CENTER);top.addView(edit,lp(38,34,0,0,2,0));
            TextView del=text("⌫",20,MUTED,true);del.setGravity(Gravity.CENTER);top.addView(del,lp(38,34,0,0,0,0));
            entry.addView(top,lp(-1,36,0,0,0,3));
            String tag=prefs.getString("diary_tag_"+id,"Учёба");TextView tagView=text("#"+tag,11,PINK,true);entry.addView(tagView,lp(-1,22,0,0,0,4));
            TextView body=text(value,15,DARK,false);body.setGravity(Gravity.TOP);body.setPadding(0,dp(3),0,dp(8));entry.addView(body,lp(-1,-2,0,0,0,0));
            LinearLayout editArea=new LinearLayout(this);editArea.setOrientation(LinearLayout.VERTICAL);editArea.setVisibility(View.GONE);EditText editText=new EditText(this);editText.setText(value);editText.setTextSize(15);editText.setTextColor(DARK);editText.setGravity(Gravity.TOP);editText.setMinLines(3);editText.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));editArea.addView(editText,lp(-1,100,0,0,0,7));Button saveInline=button("Сохранить",true),cancel=button("Отмена",false);editArea.addView(saveInline,lp(-1, 48,0,0,0,4));editArea.addView(cancel,lp(-1, 48,0,0,0,0));entry.addView(editArea,lp(-1,-2,0,0,0,0));
            edit.setOnClickListener(v->{body.setVisibility(View.GONE);tagView.setVisibility(View.GONE);editArea.setVisibility(View.VISIBLE);});cancel.setOnClickListener(v->{editArea.setVisibility(View.GONE);body.setVisibility(View.VISIBLE);tagView.setVisibility(View.VISIBLE);});saveInline.setOnClickListener(v->{String updated=editText.getText().toString().trim();if(updated.isEmpty()){editText.setError("Напиши пару слов");return;}prefs.edit().putString("diary_"+id,updated).apply();renderDiaryEntries(entries,note,save,editingId);});
            del.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("Удалить эту запись?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{Set<String> latest=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));latest.remove(id);prefs.edit().remove("diary_"+id).remove("diary_tag_"+id).putStringSet("diary_ids",latest).apply();renderDiaryEntries(entries,note,save,editingId);}).show());
            View line=new View(this);line.setBackgroundColor(0xFFEFE5E8);entry.addView(line,lp(-1,1,0,5,0,0));entries.addView(entry,lp(-1,-2,0,0,0,7));
        }
    }


    private String mascotMessage(){
        int hour=Calendar.getInstance(AppTime.zone()).get(Calendar.HOUR_OF_DAY);
        if(currentStreak()>=3)return "🐰 Я горжусь твоей серией! Каждый шаг важен.";
        if(hour<12)return "🐰 Доброе утро! Начнём день с маленькой победы?";
        if(hour<18)return "🐰 Я рядом, если захочешь немного позаниматься.";
        return "🐰 Вечер — хорошее время спокойно повторить пройденное.";
    }
    private void showKnowledgeGarden(){
        currentScreen="garden";base("Сад знаний","Каждая тема — отдельное растение. Повторение помогает ему расцветать.");
        int total=0,done=0;
        for(Map.Entry<String,List<String>> e:chapters.entrySet())for(String topic:e.getValue()){total++;if(topicProgressForChapter(e.getKey(),topic)==100)done++;}
        LinearLayout summary=card();summary.setGravity(Gravity.CENTER);addText(summary,"🌼  "+done+" из "+total+" растений расцвели",18,PINK,true);addText(summary,"Продолжай учиться в своём темпе — сад будет расти.",13,MUTED,false);root.addView(summary,lp(-1,-2,0,0,0,12));
        for(Map.Entry<String,List<String>> e:chapters.entrySet()){
            LinearLayout group=card();addText(group,e.getKey(),15,DARK,true);
            for(String topic:e.getValue()){
                int p=topicProgressForChapter(e.getKey(),topic);LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
                TextView plant=text(p==100?"🌸":p>0?"🌿":"🌱",25,PINK,true);line.addView(plant,lp(42,42,0,0,8,0));
                LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.addView(text(topic,13,DARK,true));words.addView(text(p==100?"Цветёт · тема завершена":p>0?"Растёт · прогресс "+p+"%":"Росток · начни изучение",11,MUTED,false));line.addView(words,new LinearLayout.LayoutParams(0,-2,1f));group.addView(line,lp(-1,-2,0,3,0,3));
            }root.addView(group,lp(-1,-2,0,0,0,9));
        }
        Button water=button("💧  Полить сад повторением",true);water.setOnClickListener(v->{prefs.edit().putInt("garden_water_count",prefs.getInt("garden_water_count",0)+1).putLong("garden_last_water",System.currentTimeMillis()).apply();Toast.makeText(this,"Сад полит! Повторение помогает знаниям 🌿",Toast.LENGTH_SHORT).show();});root.addView(water,lp(-1,50,0,4,0,10));
    }
    private void showPostcards(){
        currentScreen="postcards";base("Коллекция открыток","Открытки сохраняют важные учебные моменты.");
        int days=prefs.getStringSet("study_days",new TreeSet<String>()).size(),completed=prefs.getInt("done_count",0),sessions=prefs.getInt("postcard_focus_count",0);
        String[][] cards={{"🌱","Первый шаг","За первый учебный день",days>=1?"1":"0"},{"🌷","Неделя заботы","За 7 дней занятий",days>=7?"1":"0"},{"🌸","Цветущий сад","За первую завершённую тему",completed>=1?"1":"0"},{"✨","Сила фокуса","За первую завершённую фокус-сессию",sessions>=1?"1":"0"},{"💐","Постоянство","За 30 дней занятий",days>=30?"1":"0"}};
        for(String[] item:cards){boolean unlocked="1".equals(item[3]);LinearLayout c=card();c.setGravity(Gravity.CENTER);c.setPadding(dp(18),dp(20),dp(18),dp(20));c.setBackground(shape(unlocked?0xFFFFF0F4:0xFFF1ECEE,22,0));addText(c,item[0],34,PINK,true);addText(c,item[1],18,DARK,true);addText(c,item[2],12,MUTED,false);addText(c,unlocked?"Открытка открыта ♡":"Пока закрыта",12,unlocked?PINK:MUTED,true);
            if(unlocked){Button share=button("Поделиться открыткой",false);share.setOnClickListener(v->{Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,item[0]+" "+item[1]+" — "+item[2]+" ♡");startActivity(Intent.createChooser(send,"Поделиться открыткой"));});c.addView(share,lp(-1, 48,0,8,0,0));}root.addView(c,lp(-1,-2,0,0,0,10));
        }
    }


    private void showStudyRitual(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(4),dp(8),dp(4));
        TextView breathing=text("🌬  Сделай спокойный вдох и выдох",16,PINK,true);breathing.setGravity(Gravity.CENTER);panel.addView(breathing,lp(-1,48,0,0,0,8));
        EditText goal=new EditText(this);goal.setSingleLine(true);goal.setHint("Моя цель на эту сессию");goal.setText(prefs.getString("focus_goal",""));panel.addView(goal,lp(-1,52,0,0,0,8));
        new android.app.AlertDialog.Builder(this).setTitle("Ритуал начала занятия").setMessage("Первые 30 секунд — спокойное дыхание. Затем сформулируй цель и переходи к фокусу.").setView(panel).setNegativeButton("Позже",null).setPositiveButton("Начать фокус",(d,w)->{prefs.edit().putString("focus_goal",goal.getText().toString().trim()).apply();startTimer();}).show();
    }
    @Override public void onUserLeaveHint(){super.onUserLeaveHint();if(timerRunning&&!timerIsBreak)prefs.edit().putBoolean("focus_left_app",true).apply();}
    private void showFlower() {
        currentScreen="flower";
        Set<String> days=new TreeSet<>(prefs.getStringSet("study_days",new TreeSet<String>()));
        int count=days.size(),waterCount=prefs.getInt("flower_water_count",0),petals=prefs.getInt("flower_petals",0);
        int flowerIndex=Math.floorMod(prefs.getInt("flower_type",0),5);int streak=currentStreak();
        String stage=count>=30?"Пышное цветение":count>=14?"Цветочек окреп":count>=7?"Появились бутоны":count>=3?"Первые листочки":"Маленький росток";
        base("Мой цветочек","Он растёт вместе с твоими учебными победами");
        LinearLayout c=card();c.setGravity(Gravity.CENTER);
        android.widget.FrameLayout garden=new android.widget.FrameLayout(this);garden.setClipChildren(false);garden.setClipToPadding(false);
        FlowerPotView pot=new FlowerPotView(this);android.widget.FrameLayout.LayoutParams potLp=new android.widget.FrameLayout.LayoutParams(dp(142),dp(112),Gravity.CENTER);potLp.topMargin=dp(70);garden.addView(pot,potLp);
        FlowerArtView art=new FlowerArtView(this,flowerIndex,count,streak);android.widget.FrameLayout.LayoutParams flowerLp=new android.widget.FrameLayout.LayoutParams(dp(150),dp(150),Gravity.CENTER);flowerLp.topMargin=-dp(27);garden.addView(art,flowerLp);
        // Draw the pot rim and soil in front of the lower stem so the flower visibly grows from inside the pot.
        android.widget.FrameLayout.LayoutParams frontPotLp=new android.widget.FrameLayout.LayoutParams(dp(142),dp(112),Gravity.CENTER);frontPotLp.topMargin=dp(70);garden.addView(new FlowerPotView(this),frontPotLp);
        WaterCanView can=new WaterCanView(this);can.setVisibility(View.GONE);android.widget.FrameLayout.LayoutParams canLp=new android.widget.FrameLayout.LayoutParams(dp(66),dp(58),Gravity.RIGHT|Gravity.TOP);canLp.setMargins(0,dp(20),dp(12),0);garden.addView(can,canLp);
        WaterStreamView stream=new WaterStreamView(this);stream.setVisibility(View.GONE);android.widget.FrameLayout.LayoutParams streamLp=new android.widget.FrameLayout.LayoutParams(dp(74),dp(92),Gravity.RIGHT|Gravity.TOP);streamLp.setMargins(0,dp(65),dp(57),0);garden.addView(stream,streamLp);
        if(flowerBreatheAnimator!=null){flowerBreatheAnimator.cancel();flowerBreatheAnimator=null;} flowerBreatheAnimator=android.animation.ObjectAnimator.ofFloat(art,View.SCALE_Y,.985f,1.025f,.985f);flowerBreatheAnimator.setDuration(2400);flowerBreatheAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);flowerBreatheAnimator.start();
        c.addView(garden,lp(-1,230,0,0,0,8));addText(c,stage,21,DARK,true);addText(c,count+" дней занятий · "+totalStudyMinutes()+" минут вместе",13,MUTED,false);addText(c,"✿  "+petals+" лепестков заботы",14,PINK,true);root.addView(c,lp(-1,-2,0,0,0,12));
        LinearLayout care=card();addText(care,"УХОД ЗА ЦВЕТОЧКОМ",12,PINK,true);addText(care,"Поливай цветочек раз в день. Вода появится только после нажатия.",13,MUTED,false);
        boolean watered=prefs.getString("flower_water_date","").equals(todayKey());Button waterButton=button(watered?"Уже полит сегодня ✓":"Полить цветочек",true);waterButton.setEnabled(!watered);
        waterButton.setOnClickListener(v->{
            haptic(waterButton,true);
            waterButton.setEnabled(false);waterButton.setText("Цветочек пьёт воду…");
            can.setVisibility(View.VISIBLE);stream.setVisibility(View.VISIBLE);
            can.setPivotX(dp(55));can.setPivotY(dp(45));
            android.animation.ObjectAnimator pour=android.animation.ObjectAnimator.ofFloat(can,"rotation",0f,-28f,-18f,0f);pour.setDuration(1150);
            android.animation.ObjectAnimator streamMove=android.animation.ObjectAnimator.ofFloat(stream,View.TRANSLATION_Y,0f,dp(24));streamMove.setDuration(950);
            android.animation.ObjectAnimator streamFade=android.animation.ObjectAnimator.ofFloat(stream,View.ALPHA,0f,1f,.9f,0f);streamFade.setDuration(1050);
            android.animation.ObjectAnimator bloom=android.animation.ObjectAnimator.ofFloat(art,View.SCALE_X,1f,1.09f,1f);bloom.setDuration(900);
            pour.start();streamMove.start();streamFade.start();bloom.start();
            prefs.edit().putString("flower_water_date",todayKey()).putInt("flower_water_count",waterCount+1).putInt("flower_petals",petals+1).apply();
            new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()&&"flower".equals(currentScreen))showFlower();},1450);
        });
        care.addView(waterButton,lp(-1,48,0,8,0,8));
        Button change=button("Сменить вид цветка",false);change.setOnClickListener(v->{prefs.edit().putInt("flower_type",(flowerIndex+1)%5).apply();showFlower();});care.addView(change,lp(-1,48,0,0,0,0));root.addView(care,lp(-1,-2,0,0,0,12));
        addText(root,"Твой цветочек растёт в своём горшочке — так же, как твои маленькие победы ♡",14,MUTED,false);backHome();
    }

    private class FlowerPotView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        FlowerPotView(android.content.Context c){super(c);}
        @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();p.setShader(new android.graphics.LinearGradient(0,0,w,h,0xFFD58A68,0xFF9D5948,android.graphics.Shader.TileMode.CLAMP));android.graphics.Path pot=new android.graphics.Path();pot.moveTo(w*.12f,h*.12f);pot.lineTo(w*.88f,h*.12f);pot.lineTo(w*.76f,h*.94f);pot.quadTo(w*.5f,h*1.04f,w*.24f,h*.94f);pot.close();canvas.drawPath(pot,p);p.setShader(null);p.setColor(0xFFE8B08D);canvas.drawRoundRect(w*.08f,h*.08f,w*.92f,h*.24f,h*.08f,h*.08f,p);p.setColor(0xFF704638);canvas.drawOval(w*.18f,h*.13f,w*.82f,h*.23f,p);p.setColor(0xFF5D4933);canvas.drawOval(w*.19f,h*.145f,w*.81f,h*.225f,p);p.setColor(0xFF6D9B4C);for(int i=0;i<13;i++){float x=w*(.22f+i*.047f);float y=h*(.17f+(i%3)*.018f);canvas.drawLine(x,y,x-3,y-8,p);canvas.drawLine(x,y,x+4,y-7,p);}}
    }
    private class FlowerArtView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);private final int type,days,streak;
        FlowerArtView(android.content.Context c,int t,int d,int st){super(c);type=t;days=d;streak=st;}
        private void petal(android.graphics.Canvas c,float x,float y,float rx,float ry,int light,int mid,int dark){
            p.setStyle(android.graphics.Paint.Style.FILL);p.setShader(new android.graphics.LinearGradient(x-rx,y-ry,x+rx,y+ry,light,mid,android.graphics.Shader.TileMode.CLAMP));c.drawOval(x-rx,y-ry,x+rx,y+ry,p);p.setShader(null);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(dark);c.drawLine(x,y-ry*.65f,x,y+ry*.55f,p);p.setStyle(android.graphics.Paint.Style.FILL);
        }
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),stemX=w*.5f,baseY=h*.91f,headY=h*.29f;int[][] palettes={{0xFFFFD6E1,0xFFE879A0,0xFFB94B70},{0xFFFFE4EE,0xFFF3A9C1,0xFFC86C8A},{0xFFFFF1B8,0xFFEABF45,0xFFB77C20},{0xFFF3D9F4,0xFFC78BCB,0xFF89558F},{0xFFFFDCCB,0xFFEB9875,0xFFB95F48}};int[] col=palettes[type%palettes.length];
            p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setColor(0xFF397A45);android.graphics.Path stem=new android.graphics.Path();stem.moveTo(stemX,baseY);stem.cubicTo(stemX-4,baseY-55,stemX+8,headY+55,stemX,headY+8);c.drawPath(stem,p);p.setStyle(android.graphics.Paint.Style.FILL);
            android.graphics.Path left=new android.graphics.Path();left.moveTo(stemX,baseY*.79f);left.cubicTo(stemX-42,baseY*.69f,stemX-48,baseY*.59f,stemX-12,baseY*.64f);left.cubicTo(stemX-4,baseY*.67f,stemX-1,baseY*.73f,stemX,baseY*.79f);p.setShader(new android.graphics.LinearGradient(stemX-45,baseY*.6f,stemX+3,baseY*.8f,0xFF9BCB70,0xFF347A43,android.graphics.Shader.TileMode.CLAMP));c.drawPath(left,p);p.setShader(null);p.setColor(0xFFDBE9A4);p.setStrokeWidth(dp(1));c.drawLine(stemX-5,baseY*.73f,stemX-37,baseY*.65f,p);
            android.graphics.Path right=new android.graphics.Path();right.moveTo(stemX,baseY*.69f);right.cubicTo(stemX+40,baseY*.56f,stemX+49,baseY*.49f,stemX+13,baseY*.55f);right.cubicTo(stemX+4,baseY*.59f,stemX+1,baseY*.64f,stemX,baseY*.69f);p.setShader(new android.graphics.LinearGradient(stemX,baseY*.5f,stemX+45,baseY*.7f,0xFFB5D77C,0xFF397D48,android.graphics.Shader.TileMode.CLAMP));c.drawPath(right,p);p.setShader(null);p.setColor(0xFFDDEBA7);c.drawLine(stemX+5,baseY*.63f,stemX+38,baseY*.55f,p);
            float fx=stemX,fy=headY;float openness=.58f+.42f*Math.min(7,Math.max(0,streak))/7f;c.save();c.scale(openness,openness,fx,fy);for(int ring=0;ring<2;ring++){int count=ring==0?12:10;for(int j=0;j<count;j++){c.save();c.rotate(j*360f/count+(ring==0?0:18),fx,fy);float yy=ring==0?-23:-18;petal(c,fx,fy+yy,ring==0?8.5f:7f,ring==0?27:21,ring==0?col[0]:col[1],col[1],col[2]);c.restore();}}
            p.setShader(new android.graphics.RadialGradient(fx-3,fy-4,14,new int[]{0xFFFFF4B2,0xFFFFC84D,0xFFB66A22},null,android.graphics.Shader.TileMode.CLAMP));c.drawCircle(fx,fy,13,p);p.setShader(null);p.setColor(0xFF8F5523);for(int k=0;k<18;k++){double a=k*2.399;float r=3+(k%4)*2;c.drawCircle(fx+(float)Math.cos(a)*r,fy+(float)Math.sin(a)*r,1.15f,p);}
            c.restore();if(days<3){p.setColor(0xFF6C9D4F);c.drawOval(fx-8,fy+20,fx+8,fy+43,p);}
        }
    }

    private class WaterCanView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        WaterCanView(android.content.Context c){super(c);}
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setColor(0xFF9AB9C8);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(5);c.drawArc(w*.02f,h*.15f,w*.55f,h*.92f,80,200,false,p);p.setStyle(android.graphics.Paint.Style.FILL);p.setColor(0xFF82AFC0);c.drawRoundRect(w*.2f,h*.35f,w*.82f,h*.85f,10,10,p);p.setColor(0xFFAED0DA);c.drawRoundRect(w*.17f,h*.27f,w*.83f,h*.42f,8,8,p);p.setColor(0xFF82AFC0);android.graphics.Path spout=new android.graphics.Path();spout.moveTo(w*.72f,h*.42f);spout.lineTo(w*.98f,h*.16f);spout.lineTo(w*.88f,h*.12f);spout.lineTo(w*.62f,h*.36f);spout.close();c.drawPath(spout,p);}
    }
    private class WaterStreamView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        WaterStreamView(android.content.Context c){super(c);}
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);p.setColor(0xFF70BDE0);p.setStrokeWidth(4);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);for(int i=0;i<5;i++){float x=dp(10+i*11);c.drawLine(x,dp(i%2*8),x-5,dp(48+i%3*9),p);p.setStyle(android.graphics.Paint.Style.FILL);c.drawCircle(x-5,dp(53+i%3*9),dp(3),p);p.setStyle(android.graphics.Paint.Style.STROKE);}}
    }

    private void updateAchievementDates(){
        int days=prefs.getStringSet("study_days",new TreeSet<String>()).size(),minutes=totalStudyMinutes(),completed=0;for(List<String> topics:chapters.values())completed+=countCompleted(topics);int sessions=prefs.getInt("done_count",0);
        String[][] values={{"Первые шаги",String.valueOf(days>=1)},{"Неделя стараний",String.valueOf(days>=7)},{"Две недели",String.valueOf(days>=14)},{"Месяц успеха",String.valueOf(days>=30)},{"Два месяца",String.valueOf(days>=60)},{"Сто дней",String.valueOf(days>=100)},{"Маленький математик",String.valueOf(completed>=1)},{"Счёт — это просто",String.valueOf(completed>=5)},{"Знаток математики",String.valueOf(completed>=10)},{"Мастер знаний",String.valueOf(completed>=41)},{"Первый час",String.valueOf(minutes>=60)},{"10 часов учёбы",String.valueOf(minutes>=600)},{"Настойчивость",String.valueOf(sessions>=25)},{"Пятьдесят побед",String.valueOf(completed+sessions>=50)},{"Возвращаюсь к цели",String.valueOf(days>=3)},{"Ритм учёбы",String.valueOf(days>=5)}};
        SharedPreferences.Editor e=prefs.edit();String date=new SimpleDateFormat("d MMMM yyyy",new Locale("ru")).format(new Date());boolean changed=false;
        for(String[] v:values)if(Boolean.parseBoolean(v[1])&&!prefs.contains("achievement_date_"+v[0])){e.putString("achievement_date_"+v[0],date);changed=true;}if(changed)e.apply();
    }

    private void showAchievements() {
        currentScreen="achievements";base("Коллекция достижений","Каждый маленький шаг достоин похвалы");
        int days=prefs.getStringSet("study_days",new TreeSet<String>()).size(),minutes=totalStudyMinutes(),completed=0;for(List<String> topics:chapters.values())completed+=countCompleted(topics);int sessions=prefs.getInt("done_count",0);
        String[][] a={{"🌱","Первые шаги","Первый день занятий",String.valueOf(days>=1),"1"},{"🌷","Неделя стараний","7 дней занятий",String.valueOf(days>=7),"7"},{"🌸","Две недели","14 дней занятий",String.valueOf(days>=14),"14"},{"💐","Месяц успеха","30 дней занятий",String.valueOf(days>=30),"30"},{"🏵️","Два месяца","60 дней занятий",String.valueOf(days>=60),"60"},{"🌻","Сто дней","100 дней занятий",String.valueOf(days>=100),"100"},{"📚","Маленький математик","Завершена первая тема",String.valueOf(completed>=1),"1"},{"🧮","Счёт — это просто","Завершено 5 тем",String.valueOf(completed>=5),"5"},{"🎓","Знаток математики","Завершено 10 тем",String.valueOf(completed>=10),"10"},{"🏆","Мастер знаний","Завершены все 41 темы",String.valueOf(completed>=41),"41"},{"⏳","Первый час","60 минут занятий",String.valueOf(minutes>=60),"60"},{"⌛","10 часов учёбы","600 минут занятий",String.valueOf(minutes>=600),"600"},{"🚀","Настойчивость","25 учебных занятий",String.valueOf(sessions>=25),"25"},{"💎","Пятьдесят побед","50 тем или занятий",String.valueOf(completed+sessions>=50),"50"},{"💗","Возвращаюсь к цели","Занятия в 3 разных дня",String.valueOf(days>=3),"3"},{"✨","Ритм учёбы","Занятия в 5 разных дней",String.valueOf(days>=5),"5"}};
        int unlocked=0;for(String[] x:a)if(Boolean.parseBoolean(x[3]))unlocked++;
        
        addText(root,unlocked+" / "+a.length+" открыто",15,DARK,true);
        ProgressBar overall=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);overall.setMax(a.length);overall.setProgress(unlocked);overall.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));overall.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE9D4DE));root.addView(overall,lp(-1,10,0,7,0,9));
        int nextIndex=-1;for(int i=0;i<a.length;i++)if(!Boolean.parseBoolean(a[i][3])){nextIndex=i;break;}
        if(nextIndex<0)addText(root,"Все достижения открыты — невероятно! ♡",12,PINK,true);else{String nextName=a[nextIndex][1];int target=Integer.parseInt(a[nextIndex][4]);int current=achievementCurrent(nextIndex,days,minutes,completed,sessions);addText(root,"До достижения «"+nextName+"»: осталось "+Math.max(0,target-current)+(nextIndex>=10&&nextIndex<=11?" мин":(nextIndex<=5||nextIndex>=14)?" дней":" шагов"),12,DARK,true);}
        android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(3);grid.setUseDefaultMargins(false);
        for(int ai=0;ai<a.length;ai++){String[] x=a[ai];boolean open=Boolean.parseBoolean(x[3]);int target=Integer.parseInt(x[4]);int current=achievementCurrent(ai,days,minutes,completed,sessions);
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(7),dp(7),dp(7),dp(7));tile.setBackground(shape(open?0xFFFFF0F5:0xFFF7F5F5,16,open?PINK:0xFFE4DDE0));
            TextView icon=text(x[0],23,open?PINK:0xFF8F8790,true);icon.setGravity(Gravity.CENTER);tile.addView(icon,lp(-1,34,0,0,0,3));
            if(!open){TextView lock=text("🔒",11,MUTED,true);lock.setGravity(Gravity.CENTER);lock.setTranslationY(dp(-27));lock.setElevation(dp(3));tile.addView(lock,lp(-1,12,0,0,0,-1));}
            TextView name=text(x[1],10,open?DARK:0xFF514A50,true);name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setMinHeight(dp(29));tile.addView(name,lp(-1,-2,0,0,0,2));
            String progress=target>=100?String.format(Locale.US,"%.0f%%",Math.min(100,current*100f/target)):Math.min(current,target)+"/"+target;
            TextView state=text(open?"Открыто ✓":progress,10,open?PINK:0xFF625A61,true);state.setGravity(Gravity.CENTER);tile.addView(state,lp(-1,18,0,0,0,2));
            if(!open){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,target));bar.setProgress(Math.min(current,target));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE6DFE2));tile.addView(bar,lp(-1,5,0,2,0,0));}
            android.widget.GridLayout.LayoutParams gp=new android.widget.GridLayout.LayoutParams();gp.width=0;gp.height=dp(116);gp.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);gp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(tile,gp);
            tile.setOnClickListener(v->{String detail=x[2];if(open){String date=prefs.getString("achievement_date_"+x[1],"");if(date.isEmpty())date="Дата получения не сохранена";detail+="\n\nОткрыто ✓ · "+date;}else detail+="\n\nПрогресс: "+Math.min(current,target)+" из "+target;new android.app.AlertDialog.Builder(this).setTitle(x[0]+"  "+x[1]).setMessage(detail).setPositiveButton("Понятно",null).show();});
        }
        root.addView(grid,lp(-1,-2,0,8,0,8));backHome();
    }

    private int achievementCurrent(int index,int days,int minutes,int completed,int sessions){if(index<=5||index>=14)return days;if(index<=9)return completed;if(index<=11)return minutes;if(index==12)return sessions;if(index==13)return completed+sessions;return 0;}

    private void scheduleFutureLetterReminder(String id,String date) {
        try {
            java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);
            f.setTimeZone(AppTime.zone());
            Date parsed=f.parse(date);if(parsed==null)return;
            Calendar when=Calendar.getInstance(AppTime.zone());when.setTime(parsed);when.set(Calendar.HOUR_OF_DAY,9);when.set(Calendar.MINUTE,0);when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);
            if(when.getTimeInMillis()<=System.currentTimeMillis())return;
            Intent intent=new Intent(this,ReminderReceiver.class).setAction("future_letter_"+id)
                    .putExtra("message","Твоё письмо себе уже можно открыть ✉").putExtra("id",Math.abs(id.hashCode())+10000);
            PendingIntent pi=PendingIntent.getBroadcast(this,Math.abs(id.hashCode())+10000,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);
        } catch(Exception ignored) { }
    }

    public static void rescheduleFutureLetterReminders(Context context){
        SharedPreferences p=context.getSharedPreferences(PREFS,MODE_PRIVATE);Set<String> ids=p.getStringSet("future_letter_ids",new TreeSet<String>());AlarmManager am=(AlarmManager)context.getSystemService(ALARM_SERVICE);
        for(String id:ids){if(!p.getBoolean("future_remind_"+id,false))continue;String date=p.getString("future_date_"+id,"");try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(AppTime.zone());Date parsed=f.parse(date);if(parsed==null)continue;Calendar when=Calendar.getInstance(AppTime.zone());when.setTime(parsed);when.set(Calendar.HOUR_OF_DAY,9);when.set(Calendar.MINUTE,0);when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);if(when.getTimeInMillis()<=System.currentTimeMillis())continue;int code=Math.abs(id.hashCode())+10000;Intent intent=new Intent(context,ReminderReceiver.class).setAction("future_letter_"+id).putExtra("message","Твоё письмо себе уже можно открыть ✉").putExtra("id",code);PendingIntent pi=PendingIntent.getBroadcast(context,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);}catch(Exception ignored){}}
    }

    private void showFutureLetters() {
        currentScreen="letters";base("✉ Письмо себе в будущее",g("Запечатай важные мысли для себя будущей","Запечатай важные мысли для себя будущего"));
        LinearLayout steps=new LinearLayout(this);steps.setOrientation(LinearLayout.HORIZONTAL);String[] stepLabels={"1. Напиши","2. Выбери дату","3. Отправь"};for(int i=0;i<3;i++){TextView st=text(stepLabels[i],10,i==0?PINK:MUTED,true);st.setGravity(Gravity.CENTER);st.setPadding(dp(5),dp(9),dp(5),dp(9));st.setBackground(shape(i==0?0xFFFFE8EF:0xFFF7F3F4,14,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(38),1f);p.setMargins(dp(2),0,dp(2),0);steps.addView(st,p);}root.addView(steps,lp(-1, 48,0,0,0,18));
        TextView prompt=text("✒  Напиши письмо",17,DARK,true);root.addView(prompt,lp(-1,-2,0,0,0,5));
        EditText body=new EditText(this);body.setHint("Дорогая я, хочу сказать тебе...");body.setMinLines(5);body.setGravity(Gravity.TOP);body.setTextSize(16);body.setTextColor(DARK);body.setHintTextColor(MUTED);body.setPadding(dp(2),dp(12),dp(2),dp(12));body.setBackgroundColor(0x00000000);root.addView(body,lp(-1,145,0,0,0,18));
        TextView dateLabel=text("📅  Когда открыть письмо?",15,DARK,true);root.addView(dateLabel,lp(-1,-2,0,0,0,7));
        EditText date=new EditText(this);date.setHint("Например, 14 июня 2027");date.setSingleLine(true);date.setFocusable(false);date.setClickable(true);date.setTextSize(15);date.setTextColor(DARK);date.setPadding(dp(14),dp(8),dp(14),dp(8));date.setBackground(shape(0xFFFFFFFF,16,0xFFEADDE2));root.addView(date,lp(-1,50,0,0,0,5));
        TextView countdown=text("Выбери дату — я посчитаю, сколько осталось дней",12,MUTED,false);root.addView(countdown,lp(-1,26,0,0,0,8));
        final Calendar[] chosen={null};Runnable updateCountdown=()->{if(chosen[0]==null)return;Calendar now=Calendar.getInstance(AppTime.zone());Calendar target=(Calendar)chosen[0].clone();long daysLeft=Math.max(0,(target.getTimeInMillis()-now.getTimeInMillis())/86400000L);countdown.setText("До открытия: через "+daysLeft+" дней");date.setText(new SimpleDateFormat("d MMMM yyyy",new Locale("ru")).format(target.getTime()));};
        date.setOnClickListener(v->{Calendar now=Calendar.getInstance(AppTime.zone());Calendar initial=chosen[0]==null?(Calendar)now.clone():(Calendar)chosen[0].clone();android.app.DatePickerDialog picker=new android.app.DatePickerDialog(this,(p,y,m,d)->{Calendar c=Calendar.getInstance(AppTime.zone());c.set(y,m,d,9,0,0);c.set(Calendar.MILLISECOND,0);chosen[0]=c;updateCountdown.run();},initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH));picker.getDatePicker().setMinDate(now.getTimeInMillis());picker.show();});
        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);String[] quickNames={"Через месяц","Через полгода","Через год","В конце уч. года"};int[] months={1,6,12,7};for(int i=0;i<quickNames.length;i++){final int index=i;TextView chip=text(quickNames[i],12,PINK,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(7),dp(8),dp(7),dp(8));chip.setBackground(shape(0xFFFFF0F4,14,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1f);p.setMargins(dp(2),0,dp(2),0);p.height=dp(48);quick.addView(chip,p);chip.setOnClickListener(v->{Calendar c=Calendar.getInstance(AppTime.zone());if(index==3){int y=c.get(Calendar.YEAR);int m=c.get(Calendar.MONTH);c.set(y,m<=5?Calendar.JUNE:Calendar.JUNE,m<=5?30:30);if(m>5)c.add(Calendar.YEAR,1);}else c.add(Calendar.MONTH,months[index]);chosen[0]=c;updateCountdown.run();});}root.addView(quick,lp(-1,48,0,0,0,15));
        CheckBox hidden=new CheckBox(this);hidden.setText("🔒  Скрыть письмо до даты открытия");hidden.setTextColor(DARK);hidden.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));hidden.setChecked(true);root.addView(hidden,lp(-1, 48,0,0,0,2));
        CheckBox remind=new CheckBox(this);remind.setText("🔔  Напомнить, когда придёт время");remind.setTextColor(DARK);remind.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));root.addView(remind,lp(-1, 48,0,0,0,14));
        Button save=button("✉  Запечатать письмо",true);save.setTextSize(15);root.addView(save,lp(-1,54,0,0,0,18));
        save.setOnClickListener(v->{String msg=body.getText().toString().trim();if(msg.isEmpty()||chosen[0]==null){Toast.makeText(this,"Напиши письмо и выбери дату",Toast.LENGTH_SHORT).show();return;}String id=String.valueOf(System.currentTimeMillis());String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(chosen[0].getTime());Set<String> ids=new TreeSet<>(prefs.getStringSet("future_letter_ids",new TreeSet<String>()));ids.add(id);prefs.edit().putString("future_letter_"+id,msg).putString("future_date_"+id,d).putBoolean("future_hidden_"+id,hidden.isChecked()).putBoolean("future_remind_"+id,remind.isChecked()).putStringSet("future_letter_ids",ids).apply();if(remind.isChecked())scheduleFutureLetterReminder(id,d);body.setText("");chosen[0]=null;date.setText("");countdown.setText("Выбери дату — я посчитаю, сколько осталось дней");showSavedBanner("Письмо запечатано","Оно будет ждать тебя в назначенный день ✉");showFutureLetters();});
        Set<String> ids=new TreeSet<>(prefs.getStringSet("future_letter_ids",new TreeSet<String>()));List<String> sorted=new ArrayList<>(ids);java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());addText(root,"📬 ТВОИ ЗАПЕЧАТАННЫЕ ПИСЬМА",11,PINK,true);
        for(String id:sorted){String d=prefs.getString("future_date_"+id,"");String msg=prefs.getString("future_letter_"+id,"");boolean open=d.compareTo(todayKey())<=0;boolean concealed=prefs.getBoolean("future_hidden_"+id,true)&&!open;LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(dp(13),dp(12),dp(13),dp(12));item.setBackground(shape(0xFFFFF5F8,18,0));addText(item,(open?"✉️  Можно открыть":"🔒  Открывается "+d),13,PINK,true);if(!concealed)addText(item,msg,14,DARK,false);else addText(item,"Твоё письмо ждёт назначенного дня ♡",12,MUTED,false);root.addView(item,lp(-1,-2,0,0,0,8));}
        addText(root,"📬 У тебя уже есть "+ids.size()+" запечатанное письмо"+(ids.size()==1?"":"(а)")+".",12,MUTED,false);backHome();
    }

    
    
    private String createBackupJson() throws Exception {
        org.json.JSONObject json=new org.json.JSONObject();json.put("app","Study with Love");json.put("version",2);org.json.JSONObject data=new org.json.JSONObject(),photos=new org.json.JSONObject();
        Map<String,?> all=prefs.getAll();
        for(Map.Entry<String,?> entry:all.entrySet()){
            String key=entry.getKey();if(key.startsWith("timer_service_")||key.startsWith("topic_photo_"))continue;
            Object value=entry.getValue();if(value instanceof Set){org.json.JSONArray arr=new org.json.JSONArray();for(Object item:(Set<?>)value)arr.put(String.valueOf(item));data.put(key,arr);}else data.put(key,value);
        }
        Set<String> suffixes=new TreeSet<>();for(String key:all.keySet()){if(key.startsWith("topic_photo_file_"))suffixes.add(key.substring("topic_photo_file_".length()));else if(key.startsWith("topic_photo_"))suffixes.add(key.substring("topic_photo_".length()));}
        for(String suffix:suffixes){
            String path=(String)all.get("topic_photo_file_"+suffix),uri=(String)all.get("topic_photo_"+suffix);java.io.InputStream in=null;
            try{if(path!=null&&!path.isEmpty())in=new java.io.FileInputStream(path);else if(uri!=null&&!uri.isEmpty())in=getContentResolver().openInputStream(android.net.Uri.parse(uri));
                if(in==null)throw new Exception("Фото темы недоступно: "+suffix);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);photos.put(suffix,android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));
            }finally{if(in!=null)try{in.close();}catch(Exception ignored){}}
        }
        json.put("data",data);json.put("topic_photos",photos);return json.toString(2);
    }
    private void restoreBackupJson(String raw) throws Exception {
        org.json.JSONObject json=new org.json.JSONObject(raw);if(!"Study with Love".equals(json.optString("app")))throw new Exception("Wrong backup");
        org.json.JSONObject data=json.getJSONObject("data"),photos=json.optJSONObject("topic_photos");SharedPreferences.Editor editor=prefs.edit().clear();
        java.util.Iterator<String> keys=data.keys();while(keys.hasNext()){
            String key=keys.next();Object value=data.get(key);
            if(value instanceof org.json.JSONArray){org.json.JSONArray arr=(org.json.JSONArray)value;Set<String> set=new TreeSet<>();for(int i=0;i<arr.length();i++)set.add(arr.getString(i));editor.putStringSet(key,set);}
            else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
            else if(value instanceof Number){Number n=(Number)value;if(key.startsWith("app_usage_remainder_")||key.startsWith("focus_remainder_ms_")||key.endsWith("_elapsed_ms")||key.endsWith("_time_ms"))editor.putLong(key,n.longValue());else if(value instanceof Integer)editor.putInt(key,n.intValue());else if(value instanceof Long)editor.putLong(key,n.longValue());else editor.putFloat(key,n.floatValue());}
            else if(value instanceof String)editor.putString(key,(String)value);
        }
        if(photos!=null){java.util.Iterator<String> photoKeys=photos.keys();while(photoKeys.hasNext()){String suffix=photoKeys.next();byte[] bytes=android.util.Base64.decode(photos.getString(suffix),android.util.Base64.DEFAULT);java.io.File file=new java.io.File(getFilesDir(),"topic_photo_"+Math.abs(suffix.hashCode())+".jpg");try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){out.write(bytes);out.flush();}editor.putString("topic_photo_file_"+suffix,file.getAbsolutePath()).remove("topic_photo_"+suffix);}}
        editor.commit();scheduleReminders(this);
    }
    private void showSearchDialog() {
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(10),dp(18),dp(6));
        LinearLayout field=new LinearLayout(this);field.setGravity(Gravity.CENTER_VERTICAL);field.setPadding(dp(12),0,dp(8),0);field.setBackground(shape(0xFFFFF5F7,17,0xFFF0DCE4));
        TextView icon=text("⌕",23,PINK,true);field.addView(icon,lp(28,48,0,0,7,0));
        EditText query=new EditText(this);query.setSingleLine(true);query.setTextSize(15);query.setTextColor(DARK);query.setHintTextColor(MUTED);query.setHint("Найти предмет или тему");query.setBackgroundColor(0x00000000);query.setPadding(0,0,0,0);field.addView(query,new LinearLayout.LayoutParams(0,dp(48),1f));
        TextView clear=text("×",22,MUTED,true);clear.setGravity(Gravity.CENTER);clear.setVisibility(View.GONE);field.addView(clear,lp(28,44,0,0,0,0));clear.setOnClickListener(v->query.setText(""));
        body.addView(field,lp(-1,50,0,0,0,8));addText(body,"Поиск по названию предмета или любой теме",11,MUTED,false);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Поиск уроков").setView(body).setNegativeButton("Закрыть",null).setPositiveButton("Найти",(d,w)->{
            String q=query.getText().toString().trim().toLowerCase(Locale.ROOT);if(q.isEmpty())return;List<String> matches=new ArrayList<>();
            for(Map.Entry<String,List<String>> e:chapters.entrySet()){
                String ch=e.getKey();String display=ch.startsWith("CUSTOM:")?ch.split(":",3)[2]:ch.replaceFirst("^[^ ]+\\s*","");
                if(display.toLowerCase(Locale.ROOT).contains(q))matches.add(display+"\t"+ch);
                for(String topic:e.getValue())if(topic.toLowerCase(Locale.ROOT).contains(q))matches.add(display+"  •  "+topic+"\t"+ch+"\n"+topic);
            }
            if(matches.isEmpty()){new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Ничего не найдено").setMessage("Попробуй изменить запрос ♡").setPositiveButton("Понятно",null).show();return;}
            String[] rawItems=matches.toArray(new String[0]);String[] items=rawItems.clone();
            for(int i=0;i<items.length;i++){String raw=items[i];int tab=raw.indexOf("\t");int nl=raw.indexOf("\n");items[i]=raw.substring(0,tab>=0?tab:(nl>=0?nl:raw.length()));}
            new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Результаты поиска").setItems(items,(dlg,which)->{
                String[] parts=rawItems[which].split("\\t",2);if(parts.length<2)return;selectedChapter=parts[1];
                if(rawItems[which].contains("\n")){selectedTopic=rawItems[which].substring(rawItems[which].indexOf("\n")+1);showTopic(selectedTopic);}else showTopics(selectedChapter);
            }).show();
        }).create();
        query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){clear.setVisibility(s.length()>0?View.VISIBLE:View.GONE);}public void afterTextChanged(android.text.Editable e){}});
        dialog.show();
    }
    private String mainExamDate(){
        String id=prefs.getString("exam_main_goal_id","");Set<String> ids=prefs.getStringSet("exam_goal_ids",new TreeSet<String>());if(!ids.contains(id)){id="";for(String candidate:ids){if(id.isEmpty()||candidate.compareTo(id)>0)id=candidate;}}
        return id.isEmpty()?"":prefs.getString("exam_goal_date_"+id,"");
    }
    private int ensureExamPlan(){
        String examDate=mainExamDate();long daysUntilExam=examDate.isEmpty()?-1:daysUntil(examDate);if(examDate.isEmpty()||daysUntilExam<0){SharedPreferences.Editor cleanup=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("planner_tasks_")){Set<String> old=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));boolean changed=false;java.util.Iterator<String> taskIt=old.iterator();while(taskIt.hasNext())if(taskIt.next().startsWith("📘 Экзамен:")){taskIt.remove();changed=true;}if(changed)cleanup.putStringSet(key,old);}cleanup.apply();return 0;}int days=(int)Math.min(3650L,Math.max(1L,daysUntilExam+1));
        List<String> pending=new ArrayList<>();for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue())if(isTopicInExamScope(entry.getKey(),topic)&&topicProgressForChapter(entry.getKey(),topic)<100)pending.add(topic);
        Map<String,Set<String>> planned=new LinkedHashMap<>();Calendar start=Calendar.getInstance(AppTime.zone());
        for(int i=0;i<pending.size();i++){int offset=Math.min(days-1,(int)(((long)i*days)/Math.max(1,pending.size())));Calendar day=(Calendar)start.clone();day.add(Calendar.DAY_OF_YEAR,offset);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());Set<String> tasks=planned.get(date);if(tasks==null){tasks=new TreeSet<>();planned.put(date,tasks);}tasks.add("📘 Экзамен: "+pending.get(i)+" · 20 мин · Повторить правило и решить задачи");}
        SharedPreferences.Editor edit=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("planner_tasks_")){Set<String> old=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));boolean changed=false;java.util.Iterator<String> it=old.iterator();while(it.hasNext())if(it.next().startsWith("📘 Экзамен:")){it.remove();changed=true;}if(changed)edit.putStringSet(key,old);}
        for(Map.Entry<String,Set<String>> e:planned.entrySet()){String key="planner_tasks_"+e.getKey();Set<String> merged=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));merged.addAll(e.getValue());edit.putStringSet(key,merged);}
        edit.putString("exam_plan_last_built",todayKey()).apply();return pending.size();
    }
    private void showPlanner() {
        currentScreen="planner";base("Умный планировщик","Небольшой план — уже хороший шаг");
        String[] selectedDate={todayKey()},duration={""},action={""};
        String[] durations={"15 мин","30 мин","1 час"},actions={"Повторить тему","Решить 5 задач","Сделать конспект"};
        LinearLayout switcher=new LinearLayout(this);switcher.setGravity(Gravity.CENTER);switcher.setBackground(shape(PALE,16,0));
        TextView todayChip=text("Сегодня",13,PINK,true),tomorrowChip=text("Завтра",13,MUTED,true);todayChip.setGravity(Gravity.CENTER);tomorrowChip.setGravity(Gravity.CENTER);
        switcher.addView(todayChip,new LinearLayout.LayoutParams(0,dp(48),1f));switcher.addView(tomorrowChip,new LinearLayout.LayoutParams(0,dp(48),1f));root.addView(switcher,lp(-1, 48,0,0,0,10));
        addSpacedRepetitionSection();
        int examPlanTopics=ensureExamPlan();if(!mainExamDate().isEmpty()){boolean noExamTopics=prefs.getBoolean("exam_topic_filter_enabled",false)&&prefs.getStringSet("exam_topic_ids",new TreeSet<String>()).isEmpty();String planMessage=daysUntil(mainExamDate())<0?"Дата экзамена уже прошла.":noExamTopics?"Отметь темы для экзамена в разделе «Мой экзамен» — тогда появится план.":examPlanTopics==0?"Все выбранные темы уже завершены.":"План автоматически распределил "+examPlanTopics+" незавершённых тем до "+mainExamDate()+". При пропуске план пересчитывается при следующем открытии.";LinearLayout planCard=card();addText(planCard,"🎯  ПЛАН ДО ЭКЗАМЕНА",11,PINK,true);addText(planCard,planMessage,13,DARK,false);root.addView(planCard,lp(-1,-2,0,0,0,10));}
        LinearLayout form=card();addText(form,"НОВАЯ ЗАДАЧА",12,PINK,true);TextView counter=text("0 / 3 важных дела",11,MUTED,true);form.addView(counter,lp(-1,22,0,2,0,0));
        EditText task=new EditText(this);task.setSingleLine(true);task.setHint("Например: Дроби");task.setTextColor(DARK);task.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));form.addView(task,lp(-1,48,0,7,0,8));
        addText(form,"СКОЛЬКО ВРЕМЕНИ?",10,MUTED,true);
        LinearLayout durationRow=new LinearLayout(this);durationRow.setGravity(Gravity.CENTER_VERTICAL);form.addView(durationRow,lp(-1,38,0,3,0,7));
        TextView[] durationViews=new TextView[durations.length];
        Runnable refreshDuration=()->{for(int i=0;i<durations.length;i++){boolean on=duration[0].equals(durations[i]);durationViews[i].setTextColor(on?0xFFFFFFFF:PINK);durationViews[i].setBackground(shape(on?PINK:0xFFFFF0F4,15,on?PINK:0));}};
        for(int i=0;i<durations.length;i++){final String value=durations[i];TextView chip=text(value,11,PINK,true);chip.setGravity(Gravity.CENTER);durationViews[i]=chip;LinearLayout.LayoutParams durationLp=new LinearLayout.LayoutParams(0,dp(32),1f);durationLp.setMargins(0,0,dp(6),0);durationRow.addView(chip,durationLp);chip.setOnClickListener(v->{duration[0]=value;refreshDuration.run();});}
        addText(form,"ЧТО ДЕЛАЕМ?",10,MUTED,true);
        LinearLayout actionRow=new LinearLayout(this);actionRow.setGravity(Gravity.CENTER_VERTICAL);form.addView(actionRow,lp(-1,38,0,3,0,7));
        TextView[] actionViews=new TextView[actions.length];
        Runnable refreshAction=()->{for(int i=0;i<actions.length;i++){boolean on=action[0].equals(actions[i]);actionViews[i].setTextColor(on?0xFFFFFFFF:PINK);actionViews[i].setBackground(shape(on?PINK:0xFFFFF0F4,14,on?PINK:0));}};
        for(int i=0;i<actions.length;i++){final String value=actions[i];TextView chip=text(value,10,PINK,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(3),0,dp(3),0);actionViews[i]=chip;actionRow.addView(chip,new LinearLayout.LayoutParams(0,dp(32),1f));chip.setOnClickListener(v->{action[0]=value;refreshAction.run();});}
        Button add=button("＋  Добавить в план",true);add.setEnabled(false);add.setAlpha(.45f);form.addView(add,lp(-1,48,0,4,0,0));root.addView(form,lp(-1,-2,0,0,0,10));
        task.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){boolean valid=s.toString().trim().length()>0;add.setEnabled(valid);add.setAlpha(valid?1f:.45f);}public void afterTextChanged(android.text.Editable e){}});
        TextView overload=text("",11,PINK,true);root.addView(overload,lp(-1,-2,0,0,0,5));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list,lp(-1,-2,0,0,0,8));
        Button move=button("↗  Перенести незавершённое на завтра",false);root.addView(move,lp(-1, 48,0,4,0,8));Runnable[] render=new Runnable[1];
        Runnable updateDate=()->{boolean isToday=selectedDate[0].equals(todayKey());todayChip.setTextColor(isToday?PINK:MUTED);tomorrowChip.setTextColor(isToday?MUTED:PINK);todayChip.setBackground(shape(isToday?0xFFFFFFFF:PALE,13,isToday?PINK:0));tomorrowChip.setBackground(shape(isToday?PALE:0xFFFFFFFF,13,isToday?0:PINK));};
        todayChip.setOnClickListener(v->{selectedDate[0]=todayKey();updateDate.run();render[0].run();});
        tomorrowChip.setOnClickListener(v->{Calendar cal=Calendar.getInstance(AppTime.zone());cal.add(Calendar.DAY_OF_YEAR,1);selectedDate[0]=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.getTime());updateDate.run();render[0].run();});
        render[0]=()->{list.removeAllViews();String key="planner_tasks_"+selectedDate[0];Set<String> tasks=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));counter.setText(tasks.size()+" / 3 важных дела");overload.setText(tasks.size()>=3?"Уже "+tasks.size()+" задач — точно хватит сил на ещё одну? ♡":"");
            move.setVisibility(tasks.isEmpty()||!selectedDate[0].equals(todayKey())?View.GONE:View.VISIBLE);
            if(tasks.isEmpty()){addText(list,"Можно добавить одно важное дело, если есть силы ♡",12,MUTED,false);LinearLayout suggestions=card();addText(suggestions,"ИДЕИ НА СЕГОДНЯ",11,PINK,true);for(String ex:actions){TextView suggestion=text("＋  "+ex,13,DARK,true);suggestions.addView(suggestion,lp(-1,38,0,2,0,0));suggestion.setOnClickListener(v->{action[0]=ex;refreshAction.run();task.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(task,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});}list.addView(suggestions,lp(-1,-2,0,8,0,0));}
            for(String item:tasks){LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);CheckBox done=new CheckBox(this);done.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));done.setText(item);done.setTextColor(DARK);row.addView(done,new LinearLayout.LayoutParams(0,dp(48),1f));Button del=button("×",false);del.setMinWidth(dp(38));row.addView(del);
                done.setOnCheckedChangeListener((b,on)->{if(on){Set<String> latest=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));latest.remove(item);prefs.edit().putStringSet(key,latest).putInt("planner_completed_"+selectedDate[0],prefs.getInt("planner_completed_"+selectedDate[0],0)+1).apply();row.animate().alpha(0).setDuration(180).withEndAction(render[0]::run).start();}});
                del.setOnClickListener(v->{Set<String> latest=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));latest.remove(item);prefs.edit().putStringSet(key,latest).apply();render[0].run();});
                final float[] down={0};row.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){down[0]=event.getX();return true;}if(event.getAction()==android.view.MotionEvent.ACTION_UP){float dx=event.getX()-down[0];if(Math.abs(dx)>dp(55)){Set<String> latest=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));latest.remove(item);if(dx>0)prefs.edit().putInt("planner_completed_"+selectedDate[0],prefs.getInt("planner_completed_"+selectedDate[0],0)+1).apply();prefs.edit().putStringSet(key,latest).apply();row.animate().alpha(0).setDuration(160).withEndAction(render[0]::run).start();return true;}return false;}return true;});
                list.addView(row,lp(-1,-2,0,0,0,7));}
        };
        move.setOnClickListener(v->{if(selectedDate[0].equals(todayKey())){Set<String> pending=new TreeSet<>(prefs.getStringSet("planner_tasks_"+selectedDate[0],new TreeSet<String>()));if(pending.isEmpty())return;Calendar cal=Calendar.getInstance(AppTime.zone());cal.add(Calendar.DAY_OF_YEAR,1);String tomorrow=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.getTime());Set<String> next=new TreeSet<>(prefs.getStringSet("planner_tasks_"+tomorrow,new TreeSet<String>()));next.addAll(pending);prefs.edit().putStringSet("planner_tasks_"+tomorrow,next).putStringSet("planner_tasks_"+selectedDate[0],new TreeSet<String>()).apply();showSavedBanner("План обновлён","Незавершённые задачи перенесены на завтра ♡");render[0].run();}});
        add.setOnClickListener(v->{String value=task.getText().toString().trim();if(value.isEmpty())return;StringBuilder entry=new StringBuilder(value);if(!action[0].isEmpty())entry.append(" · ").append(action[0]);if(!duration[0].isEmpty())entry.append(" · ").append(duration[0]);Set<String> tasks=new TreeSet<>(prefs.getStringSet("planner_tasks_"+selectedDate[0],new TreeSet<String>()));tasks.add(entry.toString());prefs.edit().putStringSet("planner_tasks_"+selectedDate[0],tasks).apply();task.setText("");duration[0]="";action[0]="";refreshDuration.run();refreshAction.run();render[0].run();});
        updateDate.run();render[0].run();
    }
    private int weeklyMinutesLast7Days(){int total=0;Calendar c=Calendar.getInstance(AppTime.zone());for(int i=0;i<7;i++){String key=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());total+=prefs.getInt("focus_minutes_"+key,0);c.add(Calendar.DAY_OF_YEAR,-1);}return total;}
    private class WeeklyGoalRing extends View{private final android.graphics.Paint paint=new android.graphics.Paint(3);private final int done,goal;WeeklyGoalRing(Context c,int d,int g){super(c);done=d;goal=Math.max(1,g);}@Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.38f;paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(8));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setColor(isDarkAppearance()?0xFF49404B:PALE);canvas.drawCircle(cx,cy,r,paint);paint.setColor(PINK);canvas.drawArc(new android.graphics.RectF(cx-r,cy-r,cx+r,cy+r),-90,360f*Math.min(1f,(float)done/goal),false,paint);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(DARK);paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTextSize(dp(15));canvas.drawText(Math.min(100,done*100/goal)+"%",cx,cy+dp(5),paint);}}
    private void exportWeeklySummaryImage(){int minutes=weeklyMinutesLast7Days(),goal=prefs.getInt("weekly_goal_minutes",300);android.graphics.Bitmap bmp=android.graphics.Bitmap.createBitmap(1080,1350,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(bmp);c.drawColor(BG);android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(PINK);p.setTextSize(54);p.setTypeface(android.graphics.Typeface.create(selectedFontFamily(),android.graphics.Typeface.BOLD));c.drawText("Моя учебная неделя",72,150,p);p.setColor(DARK);p.setTextSize(42);c.drawText("Моя неделя учёбы",72,245,p);p.setTextSize(32);c.drawText("Фокус-время: "+minutes+" минут",72,360,p);c.drawText("Цель: "+goal+" минут",72,420,p);c.drawText("Выполнено: "+Math.min(100,minutes*100/Math.max(1,goal))+"%",72,480,p);p.setColor(PALE);c.drawRoundRect(72,540,1008,580,20,20,p);p.setColor(PINK);c.drawRoundRect(72,540,72+936*Math.min(1f,(float)minutes/Math.max(1,goal)),580,20,20,p);int y=700;Calendar day=Calendar.getInstance(AppTime.zone());for(int i=0;i<7;i++){String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());int m=prefs.getInt("focus_minutes_"+date,0);p.setColor(DARK);p.setTextSize(28);c.drawText(new SimpleDateFormat("EEE, d MMM",new Locale("ru")).format(day.getTime())+"  ·  "+m+" мин",90,y,p);y+=68;day.add(Calendar.DAY_OF_YEAR,-1);}p.setColor(PINK);p.setTextSize(25);c.drawText("Маленькие шаги складываются в большой результат ♡",72,1240,p);pendingWeeklyBitmap=bmp;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/png");intent.putExtra(Intent.EXTRA_TITLE,"study_with_love_week.png");startActivityForResult(intent,WEEKLY_IMAGE_EXPORT);}
    private void exportProgressPdf(){Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,"study_with_love_report.pdf");startActivityForResult(intent,PDF_REPORT_EXPORT);}
    private void writeProgressPdf(android.net.Uri uri)throws Exception{
        android.graphics.pdf.PdfDocument doc=new android.graphics.pdf.PdfDocument();int pageNo=1;
        android.graphics.pdf.PdfDocument.Page page=doc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create());
        android.graphics.Canvas c=page.getCanvas();android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(0xFF3A3036);p.setTextSize(22);p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.BOLD));c.drawText("Мой учебный отчёт",40,55,p);p.setTypeface(android.graphics.Typeface.DEFAULT);p.setTextSize(12);
        List<String> lines=new ArrayList<>();lines.add("Дата отчёта: "+todayKey());lines.add("Учебных дней: "+prefs.getStringSet("study_days",new TreeSet<String>()).size());lines.add("Всего минут фокуса: "+totalStudyMinutes());lines.add("За последние 7 дней: "+weeklyMinutesLast7Days()+" минут");lines.add("Недельная цель: "+prefs.getInt("weekly_goal_minutes",300)+" минут");lines.add("");lines.add("ЗАВЕРШЁННЫЕ ТЕМЫ:");
        for(Map.Entry<String,List<String>> e:chapters.entrySet())for(String topic:e.getValue())if(topicProgressForChapter(e.getKey(),topic)==100)lines.add("• "+topic);
        lines.add("");lines.add("ПОСЛЕДНИЕ РЕЗУЛЬТАТЫ ТЕСТОВ:");
        List<String> results=new ArrayList<>();for(String key:prefs.getAll().keySet())if(key.startsWith("test_result_"))results.add(key);java.util.Collections.sort(results,java.util.Collections.reverseOrder());int limit=Math.min(15,results.size());
        for(int i=0;i<limit;i++){try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(results.get(i),"{}"));lines.add(o.optString("date","")+" · "+o.optString("topic","Тест")+" — "+o.optInt("score",0)+"/"+o.optInt("total",0)+", ошибок: "+o.optInt("wrong",0));}catch(Exception ignored){}}
        int y=90;for(String line:lines){if(y>790){doc.finishPage(page);pageNo++;page=doc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create());c=page.getCanvas();y=50;}c.drawText(line.length()>78?line.substring(0,77)+"…":line,40,y,p);y+=19;}
        doc.finishPage(page);java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null)throw new Exception("output");doc.writeTo(out);out.close();doc.close();
    }
    private void showDetailedStats() {
        currentScreen="stats";saveForegroundUsage();base("Подробная статистика","Посмотри, как растёт твой учебный путь");int completed=0,total=0;
        for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue()){if(!isTopicInExamScope(entry.getKey(),topic))continue;total++;if(topicProgressForChapter(entry.getKey(),topic)==100)completed++;}LinearLayout summary=card();addText(summary,"📚  ИЗУЧЕНИЕ",12,PINK,true);addText(summary,completed+" из "+total+" тем завершено",22,DARK,true);
        addProgressLine(summary,"Общий прогресс",total==0?0:completed*100/total);Set<String> days=prefs.getStringSet("study_days",new TreeSet<String>());TextView studyDays=text("Учебных дней: 0",14,MUTED,false);summary.addView(studyDays);animateNumber(studyDays,days.size()," учебных дней");
        TextView totalTime=text("Всего времени: 0 мин",14,MUTED,false);summary.addView(totalTime);animateNumber(totalTime,totalStudyMinutes()," мин всего");addText(summary,"Завершённых занятий: "+(prefs.getBoolean("exam_topic_filter_enabled",false)?completed:prefs.getInt("done_count",0)),14,MUTED,false);root.addView(summary,lp(-1,-2,0,0,0,12));
        int tests=0,correctAnswers=0,totalAnswers=0;List<String> testKeys=new ArrayList<>();for(String key:prefs.getAll().keySet())if(key.startsWith("test_result_"))testKeys.add(key);java.util.Collections.sort(testKeys,java.util.Collections.reverseOrder());for(String key:testKeys){try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(key,"{}"));tests++;correctAnswers+=o.optInt("score",0);totalAnswers+=o.optInt("total",0);}catch(Exception ignored){}}
        LinearLayout testStats=card();addText(testStats,"📝  РЕЗУЛЬТАТЫ МИНИ-ТЕСТОВ",11,PINK,true);addText(testStats,"Пройдено: "+tests+" · Верных ответов: "+correctAnswers+" из "+totalAnswers,14,DARK,true);addProgressLine(testStats,"Точность ответов",totalAnswers==0?0:correctAnswers*100/totalAnswers);for(int i=0;i<Math.min(3,testKeys.size());i++){try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(testKeys.get(i),"{}"));addText(testStats,o.optString("topic","Тест")+" · "+o.optInt("score",0)+"/"+o.optInt("total",0),11,MUTED,false);}catch(Exception ignored){}}root.addView(testStats,lp(-1,-2,0,0,0,12));
        Calendar c=Calendar.getInstance(AppTime.zone());LinearLayout week=card();addText(week,"ПОСЛЕДНИЕ 7 ДНЕЙ",12,PINK,true);
        for(int i=6;i>=0;i--){Calendar day=(Calendar)c.clone();day.add(Calendar.DAY_OF_YEAR,-i);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());int mins=prefs.getInt("focus_minutes_"+date,0);addProgressLine(week,new SimpleDateFormat("EEE, d MMM",new Locale("ru")).format(day.getTime())+" · "+mins+" мин",Math.min(100,mins*2));}
        root.addView(week,lp(-1,-2,0,0,0,12));
        int goal=prefs.getInt("weekly_goal_minutes",300),doneMinutes=weeklyMinutesLast7Days();LinearLayout target=card();addText(target,"🎯  НЕДЕЛЬНАЯ ЦЕЛЬ",11,PINK,true);addText(target,doneMinutes+" из "+goal+" минут",20,DARK,true);addProgressLine(target,"Выполнение цели",goal<=0?0:Math.min(100,doneMinutes*100/goal));root.addView(target,lp(-1,-2,0,0,0,10));Button image=button("🖼  Итоги недели картинкой",true);image.setOnClickListener(v->exportWeeklySummaryImage());root.addView(image,lp(-1,48,0,0,0,7));Button pdf=button("📄  Создать PDF-отчёт",false);pdf.setOnClickListener(v->exportProgressPdf());root.addView(pdf,lp(-1,48,0,0,0,7));backHome();
    }

    private void startAmbience() {
        stopAmbience();
        String mode=prefs.getString("study_atmosphere","🌧️  Дождь за окном");
        ambienceAudioManager=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);
        if(Build.VERSION.SDK_INT>=26){android.media.AudioAttributes attrs=new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build();android.media.AudioFocusRequest request=new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(ambienceFocusListener).setWillPauseWhenDucked(true).build();ambienceFocusRequest=request;if(ambienceAudioManager.requestAudioFocus(request)!=android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED){Toast.makeText(this,"Звук сейчас недоступен: занят аудиофокус",Toast.LENGTH_SHORT).show();return;}}else if(ambienceAudioManager.requestAudioFocus(ambienceFocusListener,android.media.AudioManager.STREAM_MUSIC,android.media.AudioManager.AUDIOFOCUS_GAIN)!=android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return;
        ambienceLoading=true;ambiencePlaying=false;
        String url=mode.contains("Дождь")?"https://upload.wikimedia.org/wikipedia/commons/3/3d/Rain.ogg":
                mode.contains("кафе")?"https://upload.wikimedia.org/wikipedia/commons/5/54/Cafe_ambiance.ogg":
                mode.contains("природы")?"https://upload.wikimedia.org/wikipedia/commons/3/38/Birds_forest.ogg":
                "https://upload.wikimedia.org/wikipedia/commons/f/fb/20121112_TU_Delft_Library%2C_quiet_study_room_-_general_ambience_-_SoundCloud_-_el_mar.ogg";
        try{
            android.media.MediaPlayer player=new android.media.MediaPlayer();ambiencePlayer=player;
            float volume=prefs.getInt("ambience_volume",35)/100f;player.setVolume(volume,volume);player.setLooping(true);if(Build.VERSION.SDK_INT>=21)player.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnPreparedListener(mp->{if(ambiencePlayer==mp){ambienceLoading=false;mp.start();ambiencePlaying=true;}});
            player.setOnErrorListener((mp,what,extra)->{if(ambiencePlayer==mp)stopAmbience();runOnUiThread(()->Toast.makeText(this,"Не удалось загрузить звук. Проверь интернет.",Toast.LENGTH_SHORT).show());return true;});
            java.util.Map<String,String> headers=new java.util.HashMap<>();headers.put("User-Agent","StudyWithLove/4.1 (Android; ambience player)");player.setDataSource(this,android.net.Uri.parse(url),headers);player.prepareAsync();
        }catch(Exception e){stopAmbience();Toast.makeText(this,"Не удалось загрузить звук атмосферы",Toast.LENGTH_SHORT).show();}
    }
    private void stopAmbience(){ambiencePlaying=false;ambienceLoading=false;android.media.MediaPlayer player=ambiencePlayer;ambiencePlayer=null;if(player!=null){try{if(player.isPlaying())player.stop();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}}if(ambienceAudioManager!=null){try{if(Build.VERSION.SDK_INT>=26&&ambienceFocusRequest!=null)ambienceAudioManager.abandonAudioFocusRequest((android.media.AudioFocusRequest)ambienceFocusRequest);else ambienceAudioManager.abandonAudioFocus(ambienceFocusListener);}catch(Exception ignored){}}}
    @Override protected void onDestroy(){if(flowerBreatheAnimator!=null){flowerBreatheAnimator.cancel();flowerBreatheAnimator=null;}if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}stopAmbience();super.onDestroy();}

    private void showAtmosphere() {
        currentScreen = "atmosphere";
        base("Атмосфера учёбы", "Выбери настроение — звук включится сразу");
        String[] names={"🌧️  Дождь за окном","☕  Уютное кафе","🌿  Звуки природы","📚  Тихая библиотека"};
        String[] descriptions={"Мягкий дождливый настрой","Тёплая атмосфера кофейни","Спокойствие и свежесть","Минимум отвлечений"};
        String chosen=prefs.getString("study_atmosphere",names[0]);
        for(int i=0;i<names.length;i++){final String n=names[i];boolean selected=chosen.equals(n);LinearLayout c=card();addText(c,n,18,DARK,true);addText(c,descriptions[i],13,MUTED,false);TextView state=text(selected?(ambienceLoading?"Загрузка звука…":ambiencePlaying?"Играет сейчас  •  ✓":"Выбрано  •  ▶"):"Нажми, чтобы включить",12,selected?PINK:MUTED,true);c.addView(state,lp(-1,30,0,5,0,0));c.setOnClickListener(v->{prefs.edit().putString("study_atmosphere",n).apply();if(selected&&ambiencePlaying)stopAmbience();else if(!ambiencePlaying)startAmbience();else{stopAmbience();startAmbience();}showAtmosphere();});root.addView(c,lp(-1,-2,0,0,0,9));}
        android.widget.SeekBar volume=new android.widget.SeekBar(this);volume.setMax(100);volume.setProgress(prefs.getInt("ambience_volume",35));if(Build.VERSION.SDK_INT>=21)volume.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));addText(root,"ГРОМКОСТЬ",11,PINK,true);root.addView(volume,lp(-1,36,0,0,0,8));volume.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int value,boolean user){if(user){prefs.edit().putInt("ambience_volume",value).apply();if(ambiencePlayer!=null)ambiencePlayer.setVolume(value/100f,value/100f);}}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        if(ambiencePlaying){Button stop=button("Остановить звук",false);stop.setOnClickListener(v->{stopAmbience();showAtmosphere();});root.addView(stop,lp(-1, 48,0,4,0,8));}
        Button credits=button("ⓘ  Источники звуков и лицензии",false);credits.setOnClickListener(v->showAudioCredits());root.addView(credits,lp(-1, 48,0,6,0,0));
        backHome();
    }
    private void showAudioCredits(){new android.app.AlertDialog.Builder(this).setTitle("Источники звуков").setMessage("Дождь — ジダネ, Wikimedia Commons, общественное достояние.\n\nКафе — Marble Toast, Wikimedia Commons, CC0 1.0.\n\nПрирода — Barracuda1983, Wikimedia Commons, общественное достояние.\n\nТихая библиотека — “Quiet study room ambience”, el_mar, Wikimedia Commons, CC BY 3.0. Запись используется без изменений.\n\nПодробности: AUDIO_CREDITS.md в репозитории проекта.").setPositiveButton("Понятно",null).show();}

    private int countStudyDaysThisMonth(Set<String> days, int year, int month) {
        int count = 0;
        String prefix = String.format(Locale.US, "%04d-%02d-", year, month + 1);
        for (String day : days) if (day.startsWith(prefix)) count++;
        return count;
    }

    private void handleBackNavigation() {
        if (!screenHistory.isEmpty()) {
            String target = screenHistory.remove(screenHistory.size() - 1);
            navigatingBack = true;
            if (currentScreen.startsWith("math_quiz")) { quizActive = false; stopQuizTimer(); }
            showScreenByKey(target);
            return;
        }
        if (!"home".equals(currentScreen)) { showHome(); return; }
        long now=SystemClock.elapsedRealtime();
        if(now-lastBackPressedAt<2000L){finish();return;}
        lastBackPressedAt=now;
        Toast.makeText(this, "Нажми ещё раз, чтобы выйти", Toast.LENGTH_SHORT).show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() { handleBackNavigation(); }

    private void showScreenByKey(String key) {
        switch (key) {
            case "home": showHome(); break;
            case "chapters": showChapters(); break;
            case "math_chapters": showMathChapters(); break;
            case "topics": if(selectedChapter!=null)showTopics(selectedChapter);else showChapters(); break;
            case "topic": if(selectedTopic!=null)showTopic(selectedTopic);else if(selectedChapter!=null)showTopics(selectedChapter);else showChapters(); break;
            case "settings": showSettings(); break;
            case "progress": showProgress(); break;
            case "pomodoro": showPomodoro(); break;
            case "exam": showExam(); break;
            case "diary": showDiary(); break;
            case "flower": showFlower(); break;
            case "garden": showKnowledgeGarden(); break;
            case "postcards": showPostcards(); break;
            case "achievements": showAchievements(); break;
            case "letters": showFutureLetters(); break;
            case "planner": showPlanner(); break;
            case "stats": showDetailedStats(); break;
            case "atmosphere": showAtmosphere(); break;
            case "reminders": showReminderSettings(); break;
            case "message": showMessage(); break;
            case "math_quiz": showMathQuiz(); break;
            case "math_quiz_result": showMathQuiz(); break;
            case "math_quiz_awards": showMathQuizAchievements(); break;
            default: showHome(); break;
        }
    }

    private int countCompleted(List<String> topics){return countCompleted(selectedChapter,topics);}
    private int countCompleted(String chapter,List<String> topics){int count=0;for(String topic:topics)if(topicProgressForChapter(chapter,topic)==100)count++;return count;}

    private void addProgressLine(LinearLayout parent, String label, int percent) {
        boolean sliderMode=prefs!=null&&"slider".equals(prefs.getString("progress_display","percent"));
        TextView caption = text(sliderMode?label:label+"  ·  "+Math.max(0,Math.min(100,percent))+"%", 12, MUTED, false);
        parent.addView(caption, lp(-1, -2, 0, 5, 0, 3));
        if(!sliderMode)return;
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
            f.setTimeZone(AppTime.zone());
            java.util.Date d = f.parse(date);
            Calendar c = Calendar.getInstance(AppTime.zone());
            c.add(Calendar.DAY_OF_YEAR, -1);
            return d != null && f.format(c.getTime()).equals(f.format(d));
        } catch (Exception e) { return false; }
    }

    private int currentStreak() {
        String last = prefs.getString("last_study_date", "");
        if (todayKey().equals(last) || isYesterday(last) || (isOneDayGap(last)&&!weekKey().equals(prefs.getString("streak_freeze_week","")))) return prefs.getInt("current_streak", 0);
        return 0;
    }

    private boolean isOneDayGap(String date){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(AppTime.zone());Date d=f.parse(date);Calendar c=Calendar.getInstance(AppTime.zone());c.setTime(d);c.add(Calendar.DAY_OF_YEAR,2);return todayKey().equals(f.format(c.getTime()));}catch(Exception e){return false;}}
    private String weekKey(){Calendar c=Calendar.getInstance(AppTime.zone());c.setFirstDayOfWeek(Calendar.MONDAY);c.setMinimalDaysInFirstWeek(4);int week=c.get(Calendar.WEEK_OF_YEAR),year=c.get(Calendar.YEAR),month=c.get(Calendar.MONTH);if(week==1&&month==Calendar.DECEMBER)year++;else if(week>=52&&month==Calendar.JANUARY)year--;return year+"-"+week;}
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
        return android.text.TextUtils.join(", ", labels) + "  ·  " + android.text.TextUtils.join(", ", times) + " (местное время)";
    }

    private void showReminderSettings() {
        currentScreen = "reminders";
        base("Напоминания 🔔", "Выбери дни и время. Часовой пояс устройства.");
        addText(root, "ДНИ НЕДЕЛИ", 12, PINK, true);
        dayChecks.clear();
        Set<String> savedDays = selectedDays();
        LinearLayout daysRow = new LinearLayout(this); daysRow.setOrientation(LinearLayout.HORIZONTAL); daysRow.setGravity(Gravity.CENTER);
        String[] dayShort = {"Пн","Вт","Ср","Чт","Пт","Сб","Вс"};
        int[] calendarDays = {2,3,4,5,6,7,1};
        for (int d = 0; d < 7; d++) {
            CheckBox cb = new CheckBox(this); cb.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK)); cb.setText(dayShort[d]); cb.setSingleLine(true); cb.setMinWidth(0); cb.setTextSize(9); cb.setTypeface(Typeface.DEFAULT,Typeface.BOLD); cb.setTextColor(DARK); cb.setGravity(Gravity.CENTER); cb.setPadding(dp(2),0,dp(2),0); cb.setBackground(shape(PALE,14,0xFFF0DCE4));
            cb.setChecked(savedDays.contains(String.valueOf(calendarDays[d])));
            cb.setBackground(shape(cb.isChecked()?0xFFF7DCE6:PALE,14,cb.isChecked()?PINK:0xFFF0DCE4));
            cb.setOnCheckedChangeListener((button,checked)->cb.setBackground(shape(checked?0xFFF7DCE6:PALE,14,checked?PINK:0xFFF0DCE4)));
            dayChecks.add(cb); LinearLayout.LayoutParams dayLp=new LinearLayout.LayoutParams(0,dp(36),1f);dayLp.setMargins(dp(2),0,dp(2),0);daysRow.addView(cb,dayLp);
        }
        LinearLayout daysCard = card(); daysCard.addView(daysRow, lp(-1, 38, 0, 0, 0, 0)); root.addView(daysCard, lp(-1, -2, 0, 2, 0, 6));
        LinearLayout presets=new LinearLayout(this);presets.setGravity(Gravity.CENTER_VERTICAL);
        String[] presetNames={"Каждый день","Будни","Выходные"};
        for(String preset:presetNames){TextView chip=text(preset,10,PINK,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(10),dp(7),dp(10),dp(7));chip.setBackground(shape(0xFFFFF0F4,15,0));presets.addView(chip,lp(-2,32,0,0,5,0));chip.setOnClickListener(v->{for(int k=0;k<dayChecks.size();k++){boolean checked=preset.equals("Каждый день")||(preset.equals("Будни")&&k<5)||(preset.equals("Выходные")&&k>=5);dayChecks.get(k).setChecked(checked);}});}
        root.addView(presets,lp(-1,36,0,0,0,8));
        addText(root, "ВРЕМЯ НАПОМИНАНИЙ", 12, PINK, true);
        reminderTimes.clear(); reminderTimes.addAll(getSavedTimes());
        LinearLayout timeList = new LinearLayout(this); timeList.setOrientation(LinearLayout.VERTICAL); root.addView(timeList, lp(-1, -2, 0, 4, 0, 8));
        final Runnable[] renderTimes = new Runnable[1];
        renderTimes[0] = () -> {
            timeList.removeAllViews();
            if (reminderTimes.isEmpty()) addText(timeList, "Время не выбрано — напоминания выключены", 14, MUTED, false);
            for (String time : new ArrayList<>(reminderTimes)) {
                LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setBackground(shape(PALE, 16, 0)); row.setPadding(dp(12),0,dp(6),0);
                LinearLayout timeInfo=new LinearLayout(this);timeInfo.setOrientation(LinearLayout.VERTICAL);timeInfo.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = text("◷  " + time, 16, DARK, true);timeInfo.addView(label,lp(-1,24,0,0,0,0)); label.setOnClickListener(v->editReminderTime(time,renderTimes[0]));
                String slot=time.startsWith("10:")?"Утро":time.startsWith("15:")?"День":time.startsWith("19:")?"Вечер":time.startsWith("21:")?"Перед сном":"Напоминание";
                TextView sub=text(slot,10,MUTED,false);timeInfo.addView(sub,lp(-1,16,0,0,0,0));row.addView(timeInfo,new LinearLayout.LayoutParams(0,dp(46),1));
                Button edit = button("✎", false); edit.setTextSize(14); edit.setMinWidth(dp(30)); edit.setPadding(0,0,0,0); edit.setOnClickListener(v -> editReminderTime(time,renderTimes[0]));
                Button remove = button("×", false); remove.setTextSize(16); remove.setMinWidth(dp(30)); remove.setPadding(0,0,0,0); remove.setOnClickListener(v -> { reminderTimes.remove(time); renderTimes[0].run(); });
                row.addView(edit,lp(32,34,2,0,2,0)); row.addView(remove,lp(32,34,2,0,0,0)); timeList.addView(row, lp(-1, 52, 0, 3, 0, 3));
            }
        };
        renderTimes[0].run();
        Button addTime = button("＋  Добавить время", false);
        addTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance(AppTime.zone());
            new TimePickerDialog(this, (TimePicker picker, int hour, int minute) -> {
                String value = String.format(Locale.US, "%02d:%02d", hour, minute);
                if (!reminderTimes.contains(value)) { reminderTimes.add(value); java.util.Collections.sort(reminderTimes); renderTimes[0].run(); }
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        });
        root.addView(addTime, lp(-1, 48, 0, 4, 0, 14));
        TextView summary=text(selectedDays().size()+" дней · "+reminderTimes.size()+" напоминания в день",11,MUTED,true);summary.setGravity(Gravity.CENTER);root.addView(summary,lp(-1,28,0,2,0,2));
        Runnable updateReminderSummary=()->{int count=0;for(CheckBox cb:dayChecks)if(cb.isChecked())count++;summary.setText(count+" "+dayWord(count)+" · "+reminderTimes.size()+" напоминаний в день");};
        for(CheckBox cb:dayChecks)cb.setOnCheckedChangeListener((button,checked)->{cb.setBackground(shape(checked?0xFFF7DCE6:PALE,14,checked?PINK:0xFFF0DCE4));updateReminderSummary.run();});
        Button save = button("💗  Сохранить расписание", true);
        save.setOnClickListener(v -> {
            Set<String> days = new TreeSet<>();
            for (int i = 0; i < dayChecks.size(); i++) if (dayChecks.get(i).isChecked()) days.add(String.valueOf(i == 6 ? 1 : i + 2));
            Set<String> times = new TreeSet<>(reminderTimes);
            SharedPreferences.Editor e = prefs.edit().putStringSet("reminder_days", days).putStringSet("reminder_times", times);
            for (int d = 1; d <= 7; d++) e.putBoolean("day_" + d, days.contains(String.valueOf(d)));
            e.apply(); scheduleReminders(this);
            if (!days.isEmpty() && !times.isEmpty() && Build.VERSION.SDK_INT >= 31) {
                AlarmManager alarms=(AlarmManager)getSystemService(ALARM_SERVICE);
                if (!alarms.canScheduleExactAlarms()) {
                    try { startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:"+getPackageName()))); }
                    catch (Exception ignored) { }
                }
            }
            Toast.makeText(this, days.isEmpty() || times.isEmpty() ? "Расписание очищено. Напоминания выключены." : "Готово! Расписание сохранено по местному времени. Разреши точные будильники и уведомления, чтобы получать напоминания без системной задержки.", Toast.LENGTH_LONG).show();
            showHome();
        });
        root.addView(save, lp(-1, 54, 0, 4, 0, 8));

    }
    private void editReminderTime(String oldTime,Runnable refresh){
        try{String[] p=oldTime.split(":");int h=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]);
            new TimePickerDialog(this,(picker,hour,minute)->{String value=String.format(Locale.US,"%02d:%02d",hour,minute);
                if(!oldTime.equals(value)&&reminderTimes.contains(value)){Toast.makeText(this,"Такое время уже добавлено",Toast.LENGTH_SHORT).show();return;}
                int index=reminderTimes.indexOf(oldTime);if(index>=0){reminderTimes.set(index,value);java.util.Collections.sort(reminderTimes);refresh.run();}
            },h,m,true).show();
        }catch(Exception ignored){Toast.makeText(this,"Не удалось изменить время",Toast.LENGTH_SHORT).show();}
    }

    private void backHome() { Button b = button("← Назад", false); b.setOnClickListener(v -> handleBackNavigation()); root.addView(b, lp(-1, 50, 0, 18, 0, 0)); }
    private void ritualTile(LinearLayout row, String icon, String title, String subtitle, OnAction run) {
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_VERTICAL);
            tile.setPadding(dp(14), dp(14), dp(12), dp(13));
            tile.setBackground(shape(0xFFFFFFFF, 22, 0xFFF0E4E8));
            tile.setElevation(dp(1));
            TextView symbol = text(icon, 25, PINK, true);
            symbol.setGravity(Gravity.CENTER);
            symbol.setBackground(shape(0xFFFFF0F4, 15, 0));
            tile.addView(symbol, lp(43, 43, 0, 0, 0, 11));
            TextView heading = text(title, 15, DARK, true);
            heading.setMaxLines(2);
            tile.addView(heading, lp(-1, -2, 0, 0, 0, 4));
            TextView detail = text(subtitle, 12, MUTED, false);
            detail.setMaxLines(2);
            tile.addView(detail, lp(-1, -2, 0, 0, 0, 0));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(139), 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            row.addView(tile, p);
            tile.setOnClickListener(v -> {
                symbol.animate().rotationBy(24f).scaleX(1.16f).scaleY(1.16f).setDuration(180)
                    .withEndAction(() -> symbol.animate().rotationBy(-24f).scaleX(1f).scaleY(1f).setDuration(180).start()).start();
                tile.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.88f).setDuration(110)
                    .withEndAction(() -> { tile.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start(); run.go(); }).start();
            });
        }

    private void actionRegular(String title, String sub, OnAction run) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.HORIZONTAL); c.setGravity(Gravity.CENTER_VERTICAL); c.setPadding(dp(16), dp(14), dp(14), dp(14));
        c.setBackground(shape(0xFFFFFFFF, 22, 0xFFF3E9EC));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        TextView heading = text(title, 17, DARK, false);
        if (title.startsWith("РАЗДЕЛ ")) {
            android.text.SpannableString styled = new android.text.SpannableString(title);
            styled.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, 6, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            heading.setText(styled);
        }
        words.addView(heading, lp(-1, -2, 0, 0, 0, 4));
        TextView detail = text(sub, 13, MUTED, false); words.addView(detail, lp(-1, -2, 0, 0, 0, 0));
        c.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 28, PINK, false); arrow.setGravity(Gravity.CENTER); c.addView(arrow, lp(24, 34, 8, 0, 0, 0));
        c.setOnClickListener(v -> { c.animate().scaleX(0.985f).scaleY(0.985f).setDuration(100).withEndAction(() -> { c.animate().scaleX(1f).scaleY(1f).setDuration(150).start(); run.go(); }).start(); });
        root.addView(c, lp(-1, -2, 0, 0, 0, 9));
    }

    private LinearLayout action(String title, String sub, OnAction run) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.HORIZONTAL); c.setGravity(Gravity.CENTER_VERTICAL); c.setPadding(dp(16), dp(14), dp(14), dp(14));
        c.setBackground(shape(0xFFFFFFFF, 22, 0xFFF3E9EC)); c.setElevation(dp(2));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 17, DARK, true); heading.setMaxLines(2); words.addView(heading, lp(-1, -2, 0, 0, 0, 4));
        TextView detail = text(sub, 13, MUTED, false); detail.setMaxLines(2); words.addView(detail, lp(-1, -2, 0, 0, 0, 0));
        c.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 28, PINK, false); arrow.setGravity(Gravity.CENTER); c.addView(arrow, lp(24, 34, 8, 0, 0, 0));
        c.setOnClickListener(v -> { c.animate().scaleX(0.985f).scaleY(0.985f).setDuration(100).withEndAction(() -> { c.animate().scaleX(1f).scaleY(1f).setDuration(150).start(); run.go(); }).start(); });
        root.addView(c, lp(-1, -2, 0, 0, 0, 9));
        return c;
    }
    private String selectedFontFamily(){
        String key=prefs==null?"sans":prefs.getString("app_font","sans");
        if("rounded".equals(key))return "sans-serif-rounded";
        if("serif".equals(key))return "serif";
        if("mono".equals(key))return "monospace";
        if("condensed".equals(key))return "sans-serif-condensed";
        if("medium".equals(key))return "sans-serif-medium";
        return "sans-serif";
    }
    private void installThemeFontListener(android.view.ViewGroup group){
        group.setOnHierarchyChangeListener(new android.view.ViewGroup.OnHierarchyChangeListener(){
            public void onChildViewAdded(android.view.View parent,android.view.View child){applyThemeFont(child);}
            public void onChildViewRemoved(android.view.View parent,android.view.View child){}
        });
        for(int i=0;i<group.getChildCount();i++)applyThemeFont(group.getChildAt(i));
    }
    private void applyThemeFont(View view){
        if(view instanceof TextView){
            TextView t=(TextView)view;boolean bold=t.getTypeface()!=null&&t.getTypeface().isBold();
            t.setTypeface(Typeface.create(selectedFontFamily(),bold?Typeface.BOLD:Typeface.NORMAL));
            int c=t.getCurrentTextColor();
            if(c==0xFFC97891||c==0xFFD96C91)t.setTextColor(PINK);
            else if(c==0xFF3F3439||c==0xFF3A3036||c==0xFF382F34||c==0xFF49382E)t.setTextColor(DARK);
            else if(c==0xFF95878C||c==0xFF8E879E||c==0xFF9B8778)t.setTextColor(MUTED);
        }
        if(view instanceof android.widget.ProgressBar&&Build.VERSION.SDK_INT>=21){
            android.widget.ProgressBar bar=(android.widget.ProgressBar)view;
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(isDarkAppearance()?0xFF49404B:PALE));
            if(view instanceof android.widget.SeekBar)((android.widget.SeekBar)view).setThumbTintList(android.content.res.ColorStateList.valueOf(PINK));
        }
        if(view instanceof CheckBox&&Build.VERSION.SDK_INT>=21)((CheckBox)view).setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));
        if(view instanceof android.view.ViewGroup)installThemeFontListener((android.view.ViewGroup)view);
    }
    private boolean isDarkAppearance(){String t=prefs==null?"pink":prefs.getString("app_theme","pink");return "night".equals(t)||"luxury".equals(t)||("system".equals(t)&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);}
    private LinearLayout card() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(17), dp(16), dp(17), dp(16)); boolean night = isDarkAppearance(); String theme=prefs==null?"pink":prefs.getString("app_theme","pink");int surface=night?("luxury".equals(theme)?0xFF30251F:0xFF25212B):0xFFFFFFFF;int border=night?("luxury".equals(theme)?0xFF78613D:0xFF403541):0xFFF3E9EC;android.graphics.drawable.Drawable bg=shape(surface,23,border);if(Build.VERSION.SDK_INT>=21)l.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x227D5264),bg,shape(0xFFFFFFFF,23,0)));else l.setBackground(bg);l.setElevation(dp(2)); return l; }
    private void addText(LinearLayout parent, String value, int size, int color, boolean bold) { TextView t = text(value, size, color, bold); parent.addView(t, lp(-1, -2, 0, 0, 0, 5)); }
    private TextView text(String value, int size, int color, boolean bold) {
        if(color==0xFFC97891||color==0xFFD96C91)color=PINK;
        else if(color==0xFF3F3439||color==0xFF3A3036||color==0xFF382F34||color==0xFF49382E||color==0xFF34443D||color==0xFF303844)color=DARK;
        else if(color==0xFF95878C||color==0xFF8E879E||color==0xFF9B8778||color==0xFF84958C)color=MUTED;
        value=translate(value); TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create(selectedFontFamily(),bold?Typeface.BOLD:Typeface.NORMAL)); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private void applyPressFeedback(View view) {
        view.setOnTouchListener((v,event)->{
            if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                v.animate().scaleX(.97f).scaleY(.97f).setDuration(85).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            }else if(event.getAction()==android.view.MotionEvent.ACTION_UP||event.getAction()==android.view.MotionEvent.ACTION_CANCEL){
                v.animate().scaleX(1f).scaleY(1f).setDuration(145).setInterpolator(new android.view.animation.OvershootInterpolator(1.15f)).start();
            }
            return false;
        });
    }
    private void animateNumber(TextView view,int target,String suffix) {
        android.animation.ValueAnimator animator=android.animation.ValueAnimator.ofInt(0,Math.max(0,target));
        animator.setDuration(720);animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(a->view.setText(a.getAnimatedValue()+suffix));animator.start();
    }
    private void animateProgress(ProgressBar bar,int target) {
        bar.setProgress(0);android.animation.ObjectAnimator.ofInt(bar,"progress",0,Math.max(0,target)).setDuration(850).start();
    }
    private void animateHomeCascade() {
        if(root==null)return;
        for(int i=0;i<root.getChildCount();i++){View child=root.getChildAt(i);child.setAlpha(0f);child.setTranslationY(dp(12));child.animate().alpha(1f).translationY(0f).setStartDelay(i*40L).setDuration(300).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
    }
    private void haptic(View view, boolean success) {
        if(view!=null)view.performHapticFeedback(success?android.view.HapticFeedbackConstants.CONFIRM:android.view.HapticFeedbackConstants.REJECT);
    }
    private void celebrate() {
        if(root==null)return;
        final CelebrationView fx=new CelebrationView(this);
        root.getOverlay().add(fx);fx.layout(0,0,root.getWidth(),root.getHeight());
        android.animation.ValueAnimator animator=android.animation.ValueAnimator.ofFloat(0f,1f);
        animator.setDuration(1250);
        animator.addUpdateListener(a->{fx.progress=(float)a.getAnimatedValue();fx.invalidate();});
        animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(root!=null)root.getOverlay().remove(fx);}});
        animator.start();
    }
    private class CelebrationView extends View {
        float progress=0f; final android.graphics.Paint paint=new android.graphics.Paint(3);
        final float[] x=new float[34],y=new float[34],speed=new float[34],size=new float[34],rotation=new float[34];
        final int[] colors={0xFFFF8FB1,0xFFFFC66D,0xFFB7DCA5,0xFFC7A4E8,0xFFFFD9E4};
        CelebrationView(android.content.Context c){super(c);java.util.Random r=new java.util.Random();for(int i=0;i<x.length;i++){x[i]=r.nextFloat();y[i]=-.12f-r.nextFloat()*.7f;speed[i]=.55f+r.nextFloat()*.9f;size[i]=dp(3+r.nextInt(4));rotation[i]=r.nextFloat()*360f;}}
        @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();for(int i=0;i<x.length;i++){float yy=(y[i]+progress*speed[i]*1.5f)*h;if(yy<0||yy>h)continue;canvas.save();canvas.rotate(rotation[i]+progress*360f,x[i]*w,yy);paint.setColor(colors[i%colors.length]);paint.setStyle(android.graphics.Paint.Style.FILL);canvas.drawOval(x[i]*w-size[i],yy-size[i]*1.5f,x[i]*w+size[i],yy+size[i]*1.5f,paint);canvas.restore();}}
    }
    private Button button(String label, boolean primary) { label=translate(label); Button b = new Button(this); b.setText(label); b.setTextSize(14); b.setTypeface(Typeface.create(selectedFontFamily(), Typeface.BOLD)); b.setAllCaps(false); b.setLetterSpacing(0.01f); b.setMinHeight(dp(48)); b.setPadding(dp(14), dp(8), dp(14), dp(8)); b.setElevation(primary ? dp(2) : 0); b.setTextColor(primary ? 0xFFFFFFFF : PINK); android.graphics.drawable.Drawable bg=shape(primary ? PINK : PALE, 22, primary ? PINK : 0xFFF2DDE4); if(Build.VERSION.SDK_INT>=21){GradientDrawable mask=shape(0xFFFFFFFF,22,0);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x337D5264),bg,mask));}else b.setBackground(bg);applyPressFeedback(b);return b; }
    private String translate(String value){
        String lang=prefs==null?"ru":prefs.getString("app_language","ru");if("ru".equals(lang)||value==null)return value;
        String[][] pairs={
            {"Настройки ⚙","Settings ⚙","Parametrlər ⚙"},{"Настрой приложение под себя","Make the app yours","Tətbiqi özünə uyğunlaşdır"},
            {"Главная","Home","Ana səhifə"},{"Уроки","Lessons","Dərslər"},{"Прогресс","Progress","İrəliləyiş"},{"Экзамен","Exam","İmtahan"},{"Дневник","Diary","Gündəlik"},{"Таймер","Timer","Taymer"},
            {"Таймер фокуса","Focus timer","Fokus taymeri"},{"Настрой время и занимайся в своём ритме","Set your time and study at your own pace","Vaxtı seç və öz ritmində oxu"},
            {"ВРЕМЯ УЧЁБЫ","STUDY TIME","DƏRS VAXTI"},{"ВРЕМЯ ПЕРЕРЫВА","BREAK TIME","FASİLƏ VAXTI"},{"минут","minutes","dəqiqə"},{"Начать","Start","Başla"},{"Продолжить","Resume","Davam et"},{"Пауза","Pause","Fasilə"},{"Сброс","Reset","Sıfırla"},
            {"Мой цветочек","My flower","Mənim çiçəyim"},{"Мой экзамен ✨","My exam ✨","Mənim imtahanım ✨"},{"Главная цель","Main goal","Əsas məqsəd"},{"ТВОЯ ЦЕЛЬ","YOUR GOAL","SƏNİN MƏQSƏDİN"},{"ГЛАВНАЯ ЦЕЛЬ","MAIN GOAL","ƏSAS MƏQSƏD"},{"ДНЕЙ ОСТАЛОСЬ","DAYS LEFT","QALAN GÜN"},{"СЕГОДНЯ","TODAY","BU GÜN"},{"Добавить цель","Add goal","Məqsəd əlavə et"},{"Изменить","Edit","Dəyiş"},{"Удалить","Delete","Sil"},{"Сохранить изменения","Save changes","Dəyişiklikləri saxla"},
            {"Добавить свой урок","Add a custom lesson","Öz dərsini əlavə et"},{"Добавить тему","Add topic","Mövzu əlavə et"},{"Новая тема","New topic","Yeni mövzu"},{"Название темы","Topic title","Mövzunun adı"},{"Создать урок","Create lesson","Dərs yarat"},{"Отмена","Cancel","Ləğv et"},{"Сохранить","Save","Yadda saxla"},
            {"Напоминания 🔔","Reminders 🔔","Xatırlatmalar 🔔"},{"Сохранить расписание","Save schedule","Cədvəli saxla"},{"Настроить дни и время","Set days and time","Günləri və vaxtı seç"},{"УВЕДОМЛЕНИЯ","NOTIFICATIONS","BİLDİRİŞLƏR"},
            {"Дневник успехов ♡","Success diary ♡","Uğur gündəliyi ♡"},{"Сохранить запись","Save entry","Qeydi saxla"},{"Сохранить имя","Save name","Adı yadda saxla"},{"ТЕМА ОФОРМЛЕНИЯ","APP THEME","TƏRTİBAT MÖVZUSU"},{"ЯЗЫК ПРИЛОЖЕНИЯ","APP LANGUAGE","TƏTBİQİN DİLİ"},
            {"Русский","Russian","Rus dili"},{"Выбери тему, которую хочешь изучить:","Choose a topic to study:","Öyrənmək istədiyin mövzunu seç:"},{"← К урокам","← Lessons","← Dərslərə"},{"← К разделам","← Sections","← Bölmələr"},{"← Главное меню","← Home","← Ana səhifə"},
            {"Полить цветочек","Water the flower","Çiçəyi sulamaq"},{"Уже полит сегодня ✓","Watered today ✓","Bu gün sulanıb ✓"},{"Сменить вид цветка","Change flower","Çiçəyi dəyiş"},{"УХОД ЗА ЦВЕТОЧКОМ","FLOWER CARE","ÇİÇƏYƏ QULLUQ"},
            {"СВОЙ ФОН","CUSTOM BACKGROUND","ÖZ FONUN"},{"Выбрать своё фото","Choose a photo","Şəkil seç"},{"Убрать своё фото","Remove photo","Şəkli sil"},{"ТВОЁ ИМЯ","YOUR NAME","SƏNİN ADIN"},
            {"Выбери настроение приложения","Choose your app mood","Tətbiqin görünüşünü seç"},
            {"КАК К ТЕБЕ ОБРАЩАТЬСЯ?","WHAT SHOULD WE CALL YOU?","SƏNƏ NECƏ MÜRACİƏT EDƏK?"},{"Выбери обращение","Choose how to address you","Müraciət formasını seç"},{"Установи любимое фото как нежный фон приложения","Set your favorite photo as the app background","Sevdiyin şəkli tətbiqin fonu et"},{"Выбери язык интерфейса","Choose interface language","İnterfeys dilini seç"},
            {"НАПОМИНАНИЯ","REMINDERS","XATIRLATMALAR"},{"ОТМЕТИТЬ ПРОГРЕСС","TRACK PROGRESS","İRƏLİLƏYİŞİ QEYD ET"},{"ГЛАВНЫЙ ЭКЗАМЕН","MAIN EXAM","ƏSAS İMTAHAN"},{"НАЗВАНИЕ ЦЕЛИ","GOAL NAME","MƏQSƏDİN ADI"},{"ДАТА","DATE","TARİX"},{"Пока нет целей","No goals yet","Hələ məqsəd yoxdur"},{"Добавь первую дату экзамена или важную цель.","Add your first exam date or goal.","İlk imtahan tarixini və ya məqsədini əlavə et."},
            {"МАЛЕНЬКАЯ ПОБЕДА СЕГОДНЯ","TODAY'S LITTLE WIN","BU GÜNÜN KİÇİK QƏLƏBƏSİ"},{"Здесь появятся твои маленькие победы","Your little wins will appear here","Kiçik qələbələrin burada görünəcək"},{"Сохрани первую запись — тебе будет приятно перечитать её позже.","Save your first entry — you'll enjoy reading it later.","İlk qeydi saxla — sonra oxumaq xoş olacaq."},
            {"Поливай цветочек раз в день. Вода появится только после нажатия.","Water your flower once a day. Water appears when you tap.","Çiçəyini gündə bir dəfə sulа. Su yalnız toxunduqda görünəcək."},{"Твой цветочек растёт в своём горшочке — так же, как твои маленькие победы ♡","Your flower grows in its pot, just like your little wins ♡","Çiçəyin dibçəkdə böyüyür, kiçik uğurların kimi ♡"},
            {"ДНИ НЕДЕЛИ","DAYS OF THE WEEK","HƏFTƏNİN GÜNLƏRİ"},{"ВРЕМЯ НАПОМИНАНИЙ","REMINDER TIMES","XATIRLATMA VAXTLARI"},{"Время не выбрано — напоминания выключены","No times selected — reminders are off","Vaxt seçilməyib — xatırlatmalar sönülüdür"},
            {"Сохраняй моменты, которыми ты гордишься","Save the moments you're proud of","Qürur duyduğun anları saxla"},{"Выбери главную цель и следи за оставшимися днями","Choose your main goal and track the days remaining","Əsas məqsədini seç və qalan günləri izlə"},{"Он растёт вместе с твоими учебными победами","It grows with your study achievements","O, dərs uğurlarınla birlikdə böyüyür"},
            {"Привет, ","Hello, ","Salam, "},{"Сегодня — хороший день, чтобы сделать маленький шаг к своей цели.","Today is a good day to take a small step toward your goal.","Bu gün məqsədinə doğru kiçik addım atmaq üçün yaxşı gündür."},
            {"ТВОЙ ПРОГРЕСС","YOUR PROGRESS","SƏNİN İRƏLİLƏYİŞİN"},{"Ты справляешься.\nПродолжай в своём темпе.","You're doing great.\nKeep going at your own pace.","Sən bacarırsan.\nÖz tempində davam et."},{"Каждое занятие — это забота о себе будущей.","Every study session is care for your future self.","Hər dərs gələcək özünə qayğıdır."},
            {"Выбери дни и время. Часовой пояс устройства.","Choose days and times. Time zone — Baku.","Günləri və vaxtı seç. Saat qurşağı — Bakı."},{"Сохраняй моменты, которыми ты гордишься","Save moments you're proud of","Qürur duyduğun anları saxla"},
            {"Как к тебе обращаться?","What should we call you?","Sənə necə müraciət edək?"},{"Например: Английский, История, Биология","For example: English, History, Biology","Məsələn: İngilis dili, Tarix, Biologiya"},{"Название урока","Lesson name","Dərsin adı"},{"Название темы","Topic name","Mövzunun adı"},{"Сегодня у меня получилось...","Today I managed to...","Bu gün bacardım ki..."},{"Например, экзамен по математике","For example, math exam","Məsələn, riyaziyyat imtahanı"},
            {"Всё нужное — в одном месте","Everything you need in one place","Lazım olan hər şey bir yerdə"},
            {"ПРОФИЛЬ","PROFILE","PROFİL"},{"ВНЕШНИЙ ВИД","APPEARANCE","GÖRÜNÜŞ"},{"Тема приложения","App theme","Tətbiqin mövzusu"},{"Нажми на палитру, чтобы сразу применить оформление.","Tap a palette to apply a look instantly.","Görünüşü tətbiq etmək üçün palitraya toxun."},
            {"Язык приложения","App language","Tətbiqin dili"},{"После выбора язык применяется ко всем экранам приложения.","The selected language is used throughout the app.","Seçilmiş dil tətbiqin bütün ekranlarında istifadə olunur."},
            {"ОТОБРАЖЕНИЕ ПРОГРЕССА","PROGRESS DISPLAY","İRƏLİLƏYİŞİN GÖRÜNÜŞÜ"},{"Выбери удобный формат отображения учебных результатов.","Choose how your study results are displayed.","Nəticələrin göstərilmə formasını seç."},{"С процентами","With percentages","Faizlərlə"},{"Полосы прогресса","Progress bars","İrəliləyiş zolaqları"},
            {"НАПОМИНАНИЯ","REMINDERS","XATIRLATMALAR"},{"Включить напоминания о занятиях","Enable study reminders","Dərs xatırlatmalarını aktiv et"},{"РЕЖИМ ФОКУСА","FOCUS MODE","FOKUS REJİMİ"},{"Временно отключает учебные напоминания.","Temporarily disables study reminders.","Dərs xatırlatmalarını müvəqqəti söndürür."},
            {"РЕЗЕРВНАЯ КОПИЯ","BACKUP","EHTİYAT NÜSXƏSİ"},{"Сохрани уроки, цели, дневник и прогресс в один файл.","Save lessons, goals, diary and progress in one file.","Dərsləri, məqsədləri, gündəliyi və irəliləyişi bir faylda saxla."},
            {"Атмосфера учёбы","Study atmosphere","Dərs atmosferi"},{"Выбери настроение — звук включится сразу","Choose a mood — sound starts immediately","Əhval seç — səs dərhal başlayacaq"},{"Мягкий дождливый настрой","Soft rainy ambience","Yumşaq yağış atmosferi"},{"Тёплая атмосфера кофейни","Cozy café ambience","Rahat kafe atmosferi"},{"Спокойствие и свежесть","Calm and freshness","Sakitlik və təravət"},{"Минимум отвлечений","Fewer distractions","Daha az diqqət yayındıran amil"},{"Нажми, чтобы включить","Tap to play","Başlatmaq üçün toxun"},{"Играет сейчас  •  ✓","Playing now  •  ✓","İndi səslənir  •  ✓"},{"Остановить звук","Stop sound","Səsi dayandır"},
            {"ДНИ НЕДЕЛИ","DAYS OF THE WEEK","HƏFTƏNİN GÜNLƏRİ"},{"ВРЕМЯ НАПОМИНАНИЙ","REMINDER TIMES","XATIRLATMA VAXTLARI"},{"Время не выбрано — напоминания выключены","No time selected — reminders are off","Vaxt seçilməyib — xatırlatmalar sönülüdür"},{"Сохранить расписание","Save schedule","Cədvəli yadda saxla"},
            {"Поиск уроков и тем","Search lessons and topics","Dərs və mövzu axtarışı"},
            {"Математика 📐","Mathematics 📐","Riyaziyyat 📐"},{"Все разделы и темы по математике","All math sections and topics","Riyaziyyatın bütün bölmələri və mövzuları"},
            {"Добавить свой урок","Add your own lesson","Öz dərsini əlavə et"},{"Создать урок","Create lesson","Dərs yarat"},{"Выбери предмет или создай свой урок","Choose a subject or create your own lesson","Fənn seç və ya öz dərsini yarat"}
        };
        for(String[] p:pairs){String from=p[0],to="en".equals(lang)?p[1]:p[2];if(value.equals(from))return to;if(value.startsWith(from+" "))return to+value.substring(from.length());}
        return value;
    }

    private interface OnAction { void go(); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        String theme = prefs == null ? "pink" : prefs.getString("app_theme", "pink");
        if(color==0xFFC97891||color==0xFFD96C91)color=PINK;
        if(stroke==0xFFC97891||stroke==0xFFD96C91)stroke=PINK;
        boolean white = color == 0xFFFFFFFF || color == 0xFFFFF9F7 || color == 0xFFFFFBFC || color == 0xFFFFF5F8 || color == 0xFFFFF4F7 || color == 0xFFFFF5F7 || color == 0xFFFFF8F6 || color == 0xFFF7F4F2 || color == 0xFFFFFCF7 || color == 0xFFFFFCFC || color == 0xFFFFF9EF;
        boolean soft = color == 0xFFFCEEF2 || color == 0xFFFFF0F4 || color == 0xFFFFF0F5 || color == 0xFFFFEEF3 || color == 0xFFFFF1F5 || color == 0xFFF8E8EE || color == 0xFFFFF4F7 || color == 0xFFFFF5F8 || color == 0xFFFFF5F7 || color == 0xFFFFF5F4;
        boolean pale = color == 0xFFF3F0FF || color == 0xFFF1F0FF || color == 0xFFEFF7F1 || color == 0xFFF5F2F1 || color == 0xFFF0E4E8 || color == 0xFFF1F0F2 || color == 0xFFF7F4F2 || color == 0xFFF7F4F5;
        boolean border = stroke == 0xFFF3E9EC || stroke == 0xFFF0E4E8 || stroke == 0xFFEADDE2 || stroke == 0xFFFFD5E1 || stroke == 0xFFF4DCE5 || stroke == 0xFFF2DDE4 || stroke == 0xFFEEDFE4;
        if ("system".equals(theme)) {
            if(white)color=color==0xFFFFF9F7?BG:(isDarkAppearance()?PALE:0xFFFFFFFF);
            else if(soft||pale)color=PALE;
            if(border)stroke=isDarkAppearance()?0xFF49404B:0xFFE0D8DF;
        } else if ("night".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFF17151D : 0xFF25212B;
            else if (soft) color = 0xFF302735; else if (pale) color = 0xFF342E39;
            if (border) stroke = 0xFF403541;
        } else if ("lavender".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF8F6FF : 0xFFFEFDFF;
            else if (soft || pale) color = 0xFFF0ECFA;
            if (border) stroke = 0xFFE0D9F0;
        } else if ("mint".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF5FAF7 : 0xFFFEFFFE;
            else if (soft || pale) color = 0xFFEAF5EF;
            if (border) stroke = 0xFFDCEBE2;
        } else if ("white".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF7F8FA : 0xFFFFFFFF; else if (soft || pale) color = 0xFFE9EDF2;
            if (border) stroke = 0xFFDCE1E8;
        } else if ("brown".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF7F1EA : 0xFFFFFCF7; else if (soft || pale) color = 0xFFEDE0D2;
            if (border) stroke = 0xFFE1D0BE;
        } else if ("gold".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF5D0 : 0xFFFFFBEA; else if (soft || pale) color = 0xFFF5E2A6;
            if (border) stroke = 0xFFD9B64B;
        } else if ("luxury".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFF211B18 : 0xFF30251F; else if (soft || pale) color = 0xFF3A3028;
            if (border) stroke = 0xFF78613D;
        } else if ("sky".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF1F8FC : 0xFFFFFFFF; else if (soft || pale) color = 0xFFE1EFF8;
            if (border) stroke = 0xFFD4E5F0;
        } else if ("red".equals(theme)) { if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF5F4 : 0xFFFFFCFC; else if (soft || pale) color = 0xFFF8E2E3; if (border) stroke = 0xFFECC9CC;
        } else if ("lv".equals(theme)) { if (white) color = color == 0xFFFFF9F7 ? 0xFFF4E8D5 : 0xFFFFF9EF; else if (soft || pale) color = 0xFFE7D2B3; if (border) stroke = 0xFFC6A779;
        } else if ("peach".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF5EF : 0xFFFFFCF9; else if (soft || pale) color = 0xFFFBE5D9;
            if (border) stroke = 0xFFF0D2C4;
        }
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
}
